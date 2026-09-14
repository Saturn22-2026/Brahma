"""
Locked protocol rules. After genesis these numbers cannot change.
Handles adoption rewards, whale tax, mine schedule, and donation release timing.
"""
from __future__ import annotations

TIER1_USERS = 10_000
TIER2_USERS = 25_000
HALVE_BAND = 25_000
TIER1_JOIN = 100.0
TIER1_REF = 50.0
TIER2_JOIN = 50.0
TIER2_REF = 25.0
POST_JOIN = 25.0
REWARDS_POOL = 25_000_000.0
WHALE_TAX_BPS = 2500
WHALE_THRESHOLD_BPS = 250
DONATION_UNLOCK_DAYS = 730
FIRST_MINE_BONUS = 1.0
FIRST_MINE_BONUS_BTQ = FIRST_MINE_BONUS
MINE_FULL_JOINS = 10_000
MINE_BASE_BLOCK = 0.1
MINE_HALVE_EVERY_JOINS = 100_000
YEARLY_MINE_CAP = 5_000_000.0
L2_OPERATOR_GRANT_ADDR = "BRM1Ge19737c9aa6f097f76cb609a84a6"
L2_OPERATOR_GRANT = 5_000_000_000
L2_HALVING_AIRDROP = 750_000_000
L2_BOOTSTRAP_IDS = tuple(str(i) for i in range(1, 9))


def mine_halving_band(joined_count: int) -> int:
    """-1 before the first mine-schedule halving; 0 at 10_001 joins; 1 at 110_001; …"""
    n = max(0, int(joined_count))
    if n <= MINE_FULL_JOINS:
        return -1
    return (n - MINE_FULL_JOINS - 1) // MINE_HALVE_EVERY_JOINS


def block_reward(joined_count: int) -> float:
    """0.1 for the first 10_000 joins, then half every 100_000 joins. ~8s blocks stay."""
    n = max(0, int(joined_count))
    if n <= MINE_FULL_JOINS:
        return MINE_BASE_BLOCK
    band = mine_halving_band(n)
    if band >= 40:
        return 0.0
    return (MINE_BASE_BLOCK / 2.0) / (2 ** band)


block_reward_btq = block_reward


def reward_rates(joined_count: int) -> tuple[float, float]:
    n = joined_count
    if n < TIER1_USERS:
        return TIER1_JOIN, TIER1_REF
    if n < TIER1_USERS + TIER2_USERS:
        return TIER2_JOIN, TIER2_REF
    band = (n - TIER1_USERS - TIER2_USERS) // HALVE_BAND
    if band >= 40:
        return 0.0, 0.0
    join = POST_JOIN / (2 ** band)
    return join, join / 2.0


def whale_tax(amount: float, circulating: float) -> float:
    if circulating <= 0:
        return 0.0
    if (amount * 10000.0) / circulating > WHALE_THRESHOLD_BPS:
        return amount * (WHALE_TAX_BPS / 10000.0)
    return 0.0


def whale_tax_units(amount: int, circulating: int) -> int:
    if circulating <= 0:
        return 0
    if (int(amount) * 10000) // int(circulating) > WHALE_THRESHOLD_BPS:
        return (int(amount) * WHALE_TAX_BPS) // 10000
    return 0


def donations_unlocked(unlock_ts: int, now_ts: int) -> bool:
    return now_ts >= unlock_ts
