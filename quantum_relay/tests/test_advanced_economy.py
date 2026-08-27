import pytest
from ..common.economy import SovereignEconomicEngine

def test_smooth_emission():
    """Verify emission decays continuously and doesn't hit a cliff."""
    engine = SovereignEconomicEngine(pre_mined_supply=1_000_000, mineable_supply=100_000)

    rewards = []
    for _ in range(50):
        block = engine.mining.mine_block(miner_hashpower=1.0)
        rewards.append(block["reward_minted"])

    # Check that rewards are strictly decreasing (smooth decay)
    for i in range(1, len(rewards)):
        assert rewards[i] < rewards[i-1]

    # Verify no sudden 50% drops (no halvings)
    for i in range(1, len(rewards)):
        ratio = rewards[i] / rewards[i-1]
        assert ratio > 0.95

def test_whale_tax_triggers():
    """Verify whale tax engages at 2.5% threshold."""
    engine = SovereignEconomicEngine(pre_mined_supply=1_000_000, mineable_supply=0)
    # Float is 670,000. 2.5% is 16,750.

    initial_reserve = engine.wallets.reserve_wallet

    # 1. Normal sell (under 2.5%)
    engine.buy_tokens(100_000) # Build collateral
    engine.sell_tokens(10_000)
    assert engine.wallets.reserve_wallet == initial_reserve

    # 2. Whale sell (over 2.5%)
    # Current float is roughly 670k - 100k + 10k = 580k.
    # 2.5% of 580k is 14,500. Selling 30,000 should trigger tax.
    engine.sell_tokens(30_000)
    assert engine.wallets.reserve_wallet > initial_reserve

def test_difficulty_adjustment():
    """Verify difficulty responds to hashpower changes."""
    engine = SovereignEconomicEngine(pre_mined_supply=1_000_000, mineable_supply=100_000)

    # 1. High hashpower -> Difficulty should rise
    initial_diff = engine.mining.difficulty
    for _ in range(5):
        engine.mining.mine_block(miner_hashpower=10.0)

    assert engine.mining.difficulty > initial_diff

    # 2. Low hashpower -> Difficulty should fall
    high_diff = engine.mining.difficulty
    for _ in range(5):
        engine.mining.mine_block(miner_hashpower=0.1)

    assert engine.mining.difficulty < high_diff

if __name__ == "__main__":
    test_smooth_emission()
    test_whale_tax_triggers()
    test_difficulty_adjustment()
    print("Advanced Economic Engine Tests Passed!")
