import math
import time
from dataclasses import dataclass, field
from typing import Tuple, Dict

@dataclass
class SovereignWallets:
    """Holds the 33% locked sovereign supply."""
    wealth_wallet: float = 0.0       # 11% - Long-term strategic holdings
    empowerment_wallet: float = 0.0  # 11% - Grants, community funding, airdrops
    reserve_wallet: float = 0.0      # 11% - Market stabilization & liquidity backstop

@dataclass
class MarketState:
    """Holds the 67% tradeable supply and market variables."""
    tradeable_float: float = 0.0     # 67% - Available for sale/trade
    collateral_pool: float = 0.0     # Fiat/Reserve currency backing the token
    current_price: float = 1.0       # Initial peg price (e.g., $1.00)

class SovereignEconomicEngine:
    """
    Manages sovereign allocation and dynamic supply/demand pricing.
    """
    def __init__(self, total_supply: float, initial_price: float = 1.0, price_elasticity: float = 0.5):
        self.total_supply = total_supply
        self.wallets = SovereignWallets()
        self.market = MarketState(current_price=initial_price)
        self.elasticity = price_elasticity  # Controls price volatility (lower = more stable)
        self.initial_tradeable_float = 0.0

        self._allocate_sovereign_funds()

    def _allocate_sovereign_funds(self):
        """Executes the strict 11% / 11% / 11% / 67% split."""
        self.wallets.wealth_wallet = self.total_supply * 0.11
        self.wallets.empowerment_wallet = self.total_supply * 0.11
        self.wallets.reserve_wallet = self.total_supply * 0.11
        self.market.tradeable_float = self.total_supply * 0.67
        self.initial_tradeable_float = self.market.tradeable_float

    def _calculate_dynamic_price(self, proposed_new_float: float) -> float:
        """
        Uses a logarithmic bonding curve to determine price based on supply scarcity.
        Formula: P_new = P_current * (Initial_Supply / Proposed_Supply)^Elasticity
        """
        # Prevent division by zero or negative supply
        safe_float = max(proposed_new_float, 1.0)

        # Ratio of initial supply to new supply.
        supply_ratio = self.initial_tradeable_float / safe_float

        new_price = self.market.current_price * (supply_ratio ** self.elasticity)
        return max(new_price, 0.01) # Hard floor to prevent zeroing out

    def buy_tokens(self, fiat_amount: float) -> Tuple[float, float]:
        """Processes a buy order from the tradeable float."""
        if fiat_amount <= 0:
            return 0.0, self.market.current_price

        # 1. Calculate how many tokens the user gets at the current price
        tokens_purchased = fiat_amount / self.market.current_price

        # 2. Ensure we don't exhaust the 67% float
        if tokens_purchased >= self.market.tradeable_float:
            tokens_purchased = self.market.tradeable_float * 0.99

        # 3. Update Float and Collateral
        self.market.tradeable_float -= tokens_purchased
        self.market.collateral_pool += fiat_amount

        # 4. Dynamically adjust price based on new (lower) supply
        self.market.current_price = self._calculate_dynamic_price(self.market.tradeable_float)

        return tokens_purchased, self.market.current_price

    def sell_tokens(self, token_amount: float) -> Tuple[float, float]:
        """Processes a sell order back into the tradeable float."""
        if token_amount <= 0:
            return 0.0, self.market.current_price

        # 1. Calculate fiat value at current price
        fiat_value = token_amount * self.market.current_price

        # 2. Ensure collateral pool can handle the sell
        if fiat_value > self.market.collateral_pool:
            fiat_value = self.market.collateral_pool
            token_amount = fiat_value / self.market.current_price

        # 3. Update Float and Collateral
        self.market.tradeable_float += token_amount
        self.market.collateral_pool -= fiat_value

        # 4. Dynamically adjust price based on new (higher) supply
        self.market.current_price = self._calculate_dynamic_price(self.market.tradeable_float)

        # 5. Trigger Sovereign Reserve Stabilization if price drops too low
        self._check_sovereign_intervention()

        return fiat_value, self.market.current_price

    def _check_sovereign_intervention(self):
        """
        If the market price drops below a critical threshold (e.g., 80% of initial),
        the Sovereign Reserve automatically buys tokens from the float.
        """
        price_floor = 0.80

        if self.market.current_price < price_floor and self.wallets.reserve_wallet > 0:
            intervention_amount = self.market.collateral_pool * 0.15

            # The reserve wallet buys tokens
            tokens_bought, new_price = self.buy_tokens(intervention_amount)

            # Move bought tokens to the wealth wallet
            self.wallets.wealth_wallet += tokens_bought
            self.wallets.reserve_wallet -= tokens_bought * 0.1

            print(f"[ RESERVE] Intervention: Price stabilized to ${new_price:.4f}")

    def get_market_stats(self) -> Dict:
        return {
            "current_price": self.market.current_price,
            "tradeable_float": self.market.tradeable_float,
            "collateral_pool": self.market.collateral_pool,
            "sovereign_wealth": self.wallets.wealth_wallet,
            "empowerment_funds": self.wallets.empowerment_wallet,
            "sovereign_reserve": self.wallets.reserve_wallet
        }
