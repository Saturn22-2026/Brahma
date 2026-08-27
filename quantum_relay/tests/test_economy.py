import pytest
from ..common.economy import SovereignEconomicEngine

def test_initial_allocation():
    """Verify the 11/11/11/67 allocation rule."""
    total_supply = 1_000_000
    engine = SovereignEconomicEngine(total_supply=total_supply)
    stats = engine.get_market_stats()

    assert stats["sovereign_wealth"] == total_supply * 0.11
    assert stats["empowerment_funds"] == total_supply * 0.11
    assert stats["sovereign_reserve"] == total_supply * 0.11
    assert stats["tradeable_float"] == total_supply * 0.67

def test_dynamic_pricing_buy():
    """Verify price appreciation on buy."""
    engine = SovereignEconomicEngine(total_supply=1_000_000, initial_price=1.0)
    initial_price = engine.market.current_price

    # Buy tokens worth $100,000
    tokens, new_price = engine.buy_tokens(100_000)

    assert tokens > 0
    assert new_price > initial_price
    assert engine.market.collateral_pool == 100_000

def test_dynamic_pricing_sell():
    """Verify price depreciation on sell."""
    engine = SovereignEconomicEngine(total_supply=1_000_000, initial_price=1.0)

    # Initial buy to build collateral
    engine.buy_tokens(100_000)
    price_after_buy = engine.market.current_price

    # Sell half the bought tokens
    fiat, new_price = engine.sell_tokens(25_000)

    assert fiat > 0
    assert new_price < price_after_buy

def test_reserve_intervention():
    """Verify the reserve intervenes on extreme dump."""
    # Low total supply to make price movement extreme
    engine = SovereignEconomicEngine(total_supply=100_000, initial_price=1.0, price_elasticity=0.8)

    # 1. Build a collateral pool
    engine.buy_tokens(10_000)

    # 2. Extreme sell to crash price below $0.80
    # Current float is approx 67,000 - 10,000 = 57,000.
    # Selling 30,000 should crash it.
    fiat, crash_price = engine.sell_tokens(30_000)

    # The sell_tokens method calls _check_sovereign_intervention() internally.
    # If successful, the price should be stabilized (higher than crash_price).
    final_stats = engine.get_market_stats()

    assert final_stats["current_price"] > crash_price
    assert final_stats["sovereign_wealth"] > 11_000 # Wealth grew from buyback

if __name__ == "__main__":
    test_initial_allocation()
    test_dynamic_pricing_buy()
    test_dynamic_pricing_sell()
    test_reserve_intervention()
    print("Economic Engine Tests Passed!")
