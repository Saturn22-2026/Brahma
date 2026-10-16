"""
Locked protocol rules. After genesis these numbers cannot change.
Handles adoption rewards, whale tax, the mine schedule, and the empower / founder /
strategic / referral distribution rules.

Supply (100,000,000 Brahma Coin total):
  - mining pool            50,000,000  (block rewards)
  - rewards / airdrop pool 30,000,000  (join + referrer adoption bounties)
  - genesis distribution   10,000,000  (50 wallets x 200,000)
  - empower pool            3,000,000  (split across the first 100 wallets once mining is done)
  - strategic reserve       3,000,000  (burned 100,000/month on the first Friday, after mining is done)
  - founder                 2,000,000  (to the 2nd wallet created)
  - referral pool           2,000,000  (500,000 per mine halving, split across the top 10,000 referrers)

There is no donation protocol and there are no L2 memecoins.
"""
from __future__ import annotations

from datetime import datetime, timezone

TIER1_USERS = 10_000
TIER2_USERS = 25_000
HALVE_BAND = 25_000
TIER1_JOIN = 100.0
TIER1_REF = 50.0
TIER2_JOIN = 50.0
TIER2_REF = 25.0
POST_JOIN = 25.0
REWARDS_POOL = 30_000_000.0
WHALE_TAX_BPS = 2500
# Dump tax starts when one send is more than 10% of circulating Brahma.
WHALE_THRESHOLD_BPS = 1000
FIRST_MINE_BONUS = 1.0
FIRST_MINE_BONUS_Brah = FIRST_MINE_BONUS
MINE_FULL_JOINS = 10_000
MINE_BASE_BLOCK = 0.00014
MINE_HALVE_EVERY_JOINS = 100_000
YEARLY_MINE_CAP = 5_000_000.0

# --- Empower pool: first N wallets, paid once mining is complete ---
EMPOWER_POOL = 3_000_000.0
EMPOWER_WALLET_COUNT = 100

# --- Founder grant: the Nth wallet created (2nd) ---
FOUNDER_POOL = 2_000_000.0
FOUNDER_WALLET_INDEX = 2

# --- Strategic reserve: burned in monthly tranches after mining completes ---
STRATEGIC_POOL = 3_000_000.0
STRATEGIC_BURN_MONTHLY = 100_000.0

# --- Referral pool: paid out each mine-schedule halving to the top referrers ---
REFERRAL_POOL = 2_000_000.0
REFERRAL_HALVING_PAYOUT = 500_000.0
REFERRAL_TOP_N = 10_000

# Distinct inbound senders before the hub address is wiped (anti scrap-phone farm).
CONSOLIDATION_BURN_SENDERS = 6
NEW_WALLET_JOIN_BRAH = 100.0


def mine_halving_band(joined_count: int) -> int:
    """-1 before the first mine-schedule halving; 0 at 10_001 joins; 1 at 110_001; …"""
    n = max(0, int(joined_count))
    if n <= MINE_FULL_JOINS:
        return -1
    return (n - MINE_FULL_JOINS - 1) // MINE_HALVE_EVERY_JOINS


def block_reward(joined_count: int) -> float:
    """0.00014 for the first 10_000 joins, then half every 100_000 joins. ~8s blocks stay."""
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


# --- Empower pool -----------------------------------------------------------

def empower_each() -> float:
    """Whole-coin share each of the first EMPOWER_WALLET_COUNT wallets receives."""
    if EMPOWER_WALLET_COUNT <= 0:
        return 0.0
    return EMPOWER_POOL / EMPOWER_WALLET_COUNT


# --- Strategic reserve monthly burn ----------------------------------------

def strategic_burn_months() -> int:
    """Number of monthly 100,000-coin tranches needed to burn the whole reserve."""
    if STRATEGIC_BURN_MONTHLY <= 0:
        return 0
    return int((STRATEGIC_POOL + STRATEGIC_BURN_MONTHLY - 1) // STRATEGIC_BURN_MONTHLY)


def is_first_friday(ts: int) -> bool:
    """True if the UTC date of `ts` is the first Friday of its month (Friday, day 1-7)."""
    dt = datetime.fromtimestamp(int(ts), tz=timezone.utc)
    return dt.weekday() == 4 and dt.day <= 7


def month_key(ts: int) -> str:
    """Stable 'YYYY-MM' bucket (UTC) used to pay each monthly burn at most once."""
    dt = datetime.fromtimestamp(int(ts), tz=timezone.utc)
    return f"{dt.year:04d}-{dt.month:02d}"


def strategic_burn_amount(already_burned: float) -> float:
    """Coins to burn this tranche, clamped so the reserve never goes negative."""
    remaining = STRATEGIC_POOL - max(0.0, float(already_burned))
    if remaining <= 0:
        return 0.0
    return min(STRATEGIC_BURN_MONTHLY, remaining)


# --- Referral pool per-halving payout --------------------------------------

def referral_top_n() -> int:
    return REFERRAL_TOP_N


def referral_halving_payout(already_paid: float) -> float:
    """Coins released from the referral pool at a halving, clamped to the remaining pool."""
    remaining = REFERRAL_POOL - max(0.0, float(already_paid))
    if remaining <= 0:
        return 0.0
    return min(REFERRAL_HALVING_PAYOUT, remaining)


def referral_share_each(num_referrers: int, payout: float = REFERRAL_HALVING_PAYOUT) -> float:
    """Even split of a halving payout across up to REFERRAL_TOP_N ranked referrers."""
    winners = min(max(0, int(num_referrers)), REFERRAL_TOP_N)
    if winners <= 0:
        return 0.0
    return float(payout) / winners
