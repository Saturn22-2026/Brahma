import time
import heapq
import zlib
import hashlib
import math
import random
from dataclasses import dataclass, field
from typing import List, Optional, Dict, Tuple

# ============================================================================
# LAYER 1: EIP-1559 ADAPTIVE FEE ESTIMATION ORACLE
# ============================================================================
@dataclass
class EIP1559FeeOracle:
    """
    Estimates optimal base_fee + priority_fee using an Exponential Moving Average (EMA).
    """
    alpha: float = 0.3                  # EMA smoothing factor
    ema_base_fee: float = 10.0          # Gwei
    elasticity_multiplier: float = 1.125 # EIP-1559 default
    p75_priority_fee: float = 1.5       # 75th percentile priority fee (Gwei)
    min_priority_fee: float = 1.0
    recent_base_fees: List[float] = field(default_factory=list)
    window: int = 20                    # Blocks to retain for percentile calc

    def update(self, current_base_fee: float, priority_fees: List[float]):
        """Feed new block data into the oracle."""
        self.ema_base_fee = (
            self.alpha * current_base_fee +
            (1 - self.alpha) * self.ema_base_fee
        )
        self.recent_base_fees.append(current_base_fee)
        if len(self.recent_base_fees) > self.window:
            self.recent_base_fees.pop(0)
        if priority_fees:
            sorted_pf = sorted(priority_fees)
            idx = int(0.75 * (len(sorted_pf) - 1))
            self.p75_priority_fee = sorted_pf[idx]

    def estimate(self, urgency: str = "normal") -> dict:
        """Returns recommended max_fee_per_gas and max_priority_fee_per_gas."""
        if len(self.recent_base_fees) >= 2:
            demand_delta = (
                self.recent_base_fees[-1] - self.recent_base_fees[-2]
            ) / max(self.recent_base_fees[-2], 1.0)
            projected_base = self.ema_base_fee * (
                1 + self.elasticity_multiplier * demand_delta
            )
        else:
            projected_base = self.ema_base_fee * self.elasticity_multiplier

        volatility = self._stddev(self.recent_base_fees) if len(self.recent_base_fees) > 1 else 0
        upper_band = projected_base * (1 + 0.15 + volatility)

        if urgency == "high":
            priority = max(self.p75_priority_fee * 1.5, self.min_priority_fee * 3)
            max_fee = upper_band + priority
        elif urgency == "low":
            priority = self.min_priority_fee
            max_fee = projected_base * 0.9 + priority
        else:
            priority = max(self.p75_priority_fee, self.min_priority_fee)
            max_fee = upper_band + priority

        return {
            "max_fee_per_gas": max_fee,
            "max_priority_fee_per_gas": priority,
            "projected_base_fee": projected_base,
            "submission_threshold": projected_base * 0.92
        }

    @staticmethod
    def _stddev(data: List[float]) -> float:
        if len(data) < 2:
            return 0.0
        mean = sum(data) / len(data)
        var = sum((x - mean) ** 2 for x in data) / (len(data) - 1)
        return math.sqrt(var) / mean if mean > 0 else 0.0


# ============================================================================
# LAYER 2: MERKLE BATCH AGGREGATION
# ============================================================================
class MerkleBatcher:
    """Aggregates N transactions into a single merkle root."""
    @staticmethod
    def hash_leaf(tx_data: bytes) -> bytes:
        return hashlib.sha256(b"\x00" + tx_data).digest()

    @staticmethod
    def hash_node(left: bytes, right: bytes) -> bytes:
        return hashlib.sha256(b"\x01" + left + right).digest()

    @classmethod
    def build_root(cls, txs: List[bytes]) -> Tuple[bytes, Dict[int, List[Tuple[str, bytes]]]]:
        if not txs:
            return b"\x00" * 32, {}

        padded = [cls.hash_leaf(tx) for tx in txs]
        n = len(padded)
        next_pow2 = 1 << (n - 1).bit_length()
        padded += [b"\x00" * 32] * (next_pow2 - n)

        proofs = {i: [] for i in range(len(txs))}
        level = padded[:]
        while len(level) > 1:
            next_level = []
            for i in range(0, len(level), 2):
                left, right = level[i], level[i + 1]
                next_level.append(cls.hash_node(left, right))
                for idx in range(len(txs)):
                    if i // 2 * 2 <= idx < i // 2 * 2 + 2:
                        sibling = right if idx == i else left
                        direction = "R" if idx == i else "L"
                        proofs[idx].append((direction, sibling))
            level = next_level

        return level[0], proofs


# ============================================================================
# LAYER 3: ROLLUP-STYLE COMPRESSION
# ============================================================================
class RollupCompressor:
    """Compresses batched calldata before posting."""
    COMMON_SELECTORS = {
        b"\xa9\x05\x9c\xbb": b"\x00\x01",  # transfer(address,uint256)
        b"\x09\x5e\xa7\xb3": b"\x00\x02",  # approve(address,uint256)
        b"\x23\xb8\x72\xdd": b"\x00\x03",  # transferFrom
    }

    @classmethod
    def compress(cls, txs: List[bytes]) -> bytes:
        substituted = []
        for tx in txs:
            for full, short in cls.COMMON_SELECTORS.items():
                if tx.startswith(full):
                    tx = short + tx[len(full):]
                    break
            substituted.append(tx)

        blob = b""
        for tx in substituted:
            blob += len(tx).to_bytes(2, "big") + tx

        return zlib.compress(blob, level=9)

    @staticmethod
    def compression_ratio(original_size: int, compressed: bytes) -> float:
        if original_size == 0:
            return 0.0
        return (1 - len(compressed) / original_size) * 100


# ============================================================================
# LAYER 4: MEV-AWARE ROUTING & TIMING
# ============================================================================
@dataclass
class Transaction:
    tx_hash: bytes
    data: bytes
    gas_limit: int
    value: int
    timestamp: float = field(default_factory=time.time)
    urgency: str = "normal"

class MEVAwareSequencer:
    """Decides WHEN and HOW to submit the batch."""
    def __init__(self, oracle: EIP1559FeeOracle,
                 min_batch_size: int = 10,
                 max_batch_size: int = 500,
                 max_wait_seconds: float = 12.0):
        self.oracle = oracle
        self.mempool: List[Tuple[float, int, Transaction]] = []
        self.min_batch = min_batch_size
        self.max_batch = max_batch_size
        self.max_wait = max_wait_seconds
        self.consecutive_rejections = 0

    def add_transaction(self, tx: Transaction):
        heapq.heappush(self.mempool, (tx.timestamp, len(self.mempool), tx))

    def _should_submit(self, current_base_fee: float) -> Tuple[bool, dict]:
        estimate = self.oracle.estimate()
        threshold = estimate["submission_threshold"]

        fee_ok = current_base_fee <= threshold
        size_ok = len(self.mempool) >= self.min_batch
        oldest_ts = self.mempool[0][0] if self.mempool else time.time()
        time_pressure = (time.time() - oldest_ts) >= self.max_wait
        cap_reached = len(self.mempool) >= self.max_batch

        submit = (fee_ok and size_ok) or time_pressure or cap_reached
        return submit, estimate

    def build_batch(self) -> Optional[dict]:
        if not self.mempool:
            return None

        txs = []
        while self.mempool and len(txs) < self.max_batch:
            _, _, tx = heapq.heappop(self.mempool)
            txs.append(tx)

        raw_payloads = [tx.data for tx in txs]
        merkle_root, proofs = MerkleBatcher.build_root(raw_payloads)
        compressed = RollupCompressor.compress(raw_payloads)
        original_size = sum(len(t) for t in raw_payloads)
        ratio = RollupCompressor.compression_ratio(original_size, compressed)
        urgency = "high" if any(t.urgency == "high" for t in txs) else "normal"

        return {
            "merkle_root": merkle_root.hex(),
            "compressed_data": compressed,
            "tx_count": len(txs),
            "total_gas_limit": sum(t.gas_limit for t in txs),
            "compression_ratio": ratio,
            "urgency": urgency,
            "proofs": proofs
        }


# ============================================================================
# ORCHESTRATION: THE OPTIMIZATION ENGINE
# ============================================================================
class TransactionOptimizationEngine:
    """Combines all 4 layers into a single submission engine."""
    def __init__(self):
        self.oracle = EIP1559FeeOracle()
        self.sequencer = MEVAwareSequencer(self.oracle)
        self.submitted_batches = []

    def feed_block_data(self, base_fee: float, priority_fees: List[float]):
        self.oracle.update(base_fee, priority_fees)

    def submit_transaction(self, tx: Transaction):
        self.sequencer.add_transaction(tx)

    def attempt_submission(self, current_base_fee: float) -> Optional[dict]:
        should_submit, estimate = self.sequencer._should_submit(current_base_fee)

        if not should_submit:
            self.sequencer.consecutive_rejections += 1
            backoff = min(2 ** self.sequencer.consecutive_rejections, 60)
            return {
                "action": "HOLD",
                "reason": "base_fee_above_threshold",
                "backoff_seconds": backoff,
                "current_fee": current_base_fee,
                "threshold": estimate["submission_threshold"]
            }

        batch = self.sequencer.build_batch()
        if batch is None:
            return None

        self.sequencer.consecutive_rejections = 0
        batch_gas_overhead = 21000 + 16 * len(batch["compressed_data"])
        per_tx_gas = batch_gas_overhead / batch["tx_count"]

        result = {
            "action": "SUBMIT",
            "batch": batch,
            "fee_estimate": estimate,
            "effective_gas_per_tx": per_tx_gas,
            "savings_vs_individual": (1 - per_tx_gas / 21000) * 100
        }
        self.submitted_batches.append(result)
        return result
