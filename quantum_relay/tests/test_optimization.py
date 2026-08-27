import pytest
import hashlib
import json
from ..common.optimization import MerkleBatcher, RollupCompressor, TransactionOptimizationEngine, Transaction

def test_merkle_batcher():
    """Verify Merkle tree root and proof generation."""
    txs = [b"tx1", b"tx2", b"tx3", b"tx4"]
    root, proofs = MerkleBatcher.build_root(txs)

    assert len(root) == 32
    assert len(proofs) == 4

    # Verify a proof manually
    tx_idx = 1
    tx_data = txs[tx_idx]
    current_hash = MerkleBatcher.hash_leaf(tx_data)

    for direction, sibling in proofs[tx_idx]:
        if direction == "R":
            current_hash = MerkleBatcher.hash_node(current_hash, sibling)
        else:
            current_hash = MerkleBatcher.hash_node(sibling, current_hash)

    assert current_hash == root

def test_rollup_compressor():
    """Verify compression ratio calculation and selector substitution."""
    # Dummy ERC-20 transfer
    selector = b"\xa9\x05\x9c\xbb"
    txs = [selector + b"A" * 64 for _ in range(10)]

    compressed = RollupCompressor.compress(txs)
    original_size = sum(len(t) for t in txs)

    ratio = RollupCompressor.compression_ratio(original_size, compressed)
    assert ratio > 0
    assert len(compressed) < original_size

def test_sequencer_logic():
    """Verify engine holds transactions until conditions are met."""
    engine = TransactionOptimizationEngine()

    # Low fee environment
    engine.feed_block_data(10.0, [1.0, 1.2, 1.5])

    tx = Transaction(b"hash", b"data", 21000, 0)
    engine.submit_transaction(tx)

    # With only 1 TX and normal fee, it should HOLD (min_batch is 10)
    result = engine.attempt_submission(current_base_fee=10.0)
    assert result["action"] == "HOLD"

    # Inject more TXs to hit min_batch
    for i in range(10):
        engine.submit_transaction(Transaction(str(i).encode(), b"data", 21000, 0))

    result = engine.attempt_submission(current_base_fee=8.0) # Favorable fee
    assert result["action"] == "SUBMIT"
    assert result["batch"]["tx_count"] >= 10

if __name__ == "__main__":
    test_merkle_batcher()
    test_rollup_compressor()
    test_sequencer_logic()
    print("Optimization Tests Passed!")
