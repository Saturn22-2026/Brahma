"""
Economics / tokenomics tests for the reworked Brahma Coin supply.

These intentionally import only the pure modules (genesis.json, ai_council, units)
so they run without the full node (which depends on operator-only p2p/rail modules).

Run: python test_economics.py   (from btq-node/)
"""
from __future__ import annotations

import json
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

import ai_council as C
from units import UNITS_PER_BRAH, from_units, to_units

GENESIS = json.loads((Path(__file__).resolve().parent / "genesis.json").read_text())
ECO = GENESIS["economics"]
TREASURY = GENESIS["treasury"]


class SupplyAllocation(unittest.TestCase):
    def test_total_supply_is_100m(self):
        self.assertEqual(ECO["total_supply"], 100_000_000)

    def test_pool_sizes(self):
        self.assertEqual(ECO["mining_pool"], 50_000_000)
        self.assertEqual(ECO["airdrop_pool"], 30_000_000)
        self.assertEqual(ECO["rewards"]["pool"], 30_000_000)
        self.assertEqual(ECO["empower"], 3_000_000)
        self.assertEqual(ECO["strategic"], 3_000_000)
        self.assertEqual(ECO["founder"], 2_000_000)
        self.assertEqual(ECO["referral"], 2_000_000)
        self.assertEqual(ECO["genesis_wallets_total"], 10_000_000)
        self.assertEqual(
            ECO["genesis_wallet_count"] * ECO["genesis_wallet_each"],
            ECO["genesis_wallets_total"],
        )

    def test_treasury_matches_economics(self):
        self.assertEqual(TREASURY["POOL_MINING"], ECO["mining_pool"])
        self.assertEqual(TREASURY["POOL_AIRDROP"], ECO["airdrop_pool"])
        self.assertEqual(TREASURY["POOL_EMPOWER"], ECO["empower"])
        self.assertEqual(TREASURY["POOL_STRATEGIC"], ECO["strategic"])
        self.assertEqual(TREASURY["POOL_FOUNDER"], ECO["founder"])
        self.assertEqual(TREASURY["POOL_REFERRAL"], ECO["referral"])

    def test_everything_sums_to_total_supply(self):
        allocated = sum(TREASURY.values()) + ECO["genesis_wallets_total"]
        self.assertEqual(allocated, ECO["total_supply"])

    def test_donation_split_into_mining_and_rewards(self):
        # The old 10M donation pool became +5M mining (45->50) and +5M rewards (25->30).
        self.assertEqual(ECO["mining_pool"], 45_000_000 + 5_000_000)
        self.assertEqual(ECO["airdrop_pool"], 25_000_000 + 5_000_000)


class ProtocolsRemoved(unittest.TestCase):
    def test_no_donation_anywhere(self):
        self.assertNotIn("donation_pool", ECO)
        self.assertNotIn("donation_unlock_days", ECO)
        self.assertNotIn("POOL_DONATION", TREASURY)
        self.assertFalse(hasattr(C, "DONATION_UNLOCK_DAYS"))
        self.assertFalse(hasattr(C, "donations_unlocked"))

    def test_no_l2_memecoins(self):
        self.assertNotIn("l2_launch_burn_btq", ECO)
        self.assertNotIn("l2_launch_burn", ECO)
        for attr in (
            "L2_OPERATOR_GRANT",
            "L2_OPERATOR_GRANT_ADDR",
            "L2_HALVING_AIRDROP",
            "L2_HALVING_AIRDROP_SHARE",
            "L2_BOOTSTRAP_IDS",
        ):
            self.assertFalse(hasattr(C, attr), f"{attr} should be gone")


class EmpowerPool(unittest.TestCase):
    def test_first_100_wallets_split_3m(self):
        self.assertEqual(C.EMPOWER_WALLET_COUNT, 100)
        self.assertEqual(C.EMPOWER_POOL, 3_000_000)
        self.assertEqual(C.empower_each(), 30_000)
        self.assertEqual(C.empower_each() * C.EMPOWER_WALLET_COUNT, C.EMPOWER_POOL)


class FounderGrant(unittest.TestCase):
    def test_founder_targets_second_wallet(self):
        self.assertEqual(C.FOUNDER_POOL, 2_000_000)
        self.assertEqual(C.FOUNDER_WALLET_INDEX, 2)
        self.assertEqual(ECO["founder_wallet_index"], 2)


class StrategicBurn(unittest.TestCase):
    def test_monthly_tranche_and_duration(self):
        self.assertEqual(C.STRATEGIC_POOL, 3_000_000)
        self.assertEqual(C.STRATEGIC_BURN_MONTHLY, 100_000)
        # 3,000,000 / 100,000 = 30 monthly burns
        self.assertEqual(C.strategic_burn_months(), 30)

    def test_burn_amount_clamps_to_remaining(self):
        self.assertEqual(C.strategic_burn_amount(0), 100_000)
        self.assertEqual(C.strategic_burn_amount(2_950_000), 50_000)
        self.assertEqual(C.strategic_burn_amount(3_000_000), 0)

    def test_cumulative_burn_hits_exactly_3m(self):
        burned = 0.0
        tranches = 0
        while True:
            amt = C.strategic_burn_amount(burned)
            if amt <= 0:
                break
            burned += amt
            tranches += 1
            self.assertLessEqual(tranches, 1000)  # guard against runaway
        self.assertEqual(burned, 3_000_000)
        self.assertEqual(tranches, 30)

    def test_first_friday_detection(self):
        # Exactly one first-Friday per month across 2026 (12 total), each a Friday on day 1-7.
        start = datetime(2026, 1, 1, 12, 0, tzinfo=timezone.utc)
        firsts = []
        for i in range(365):
            day = start + timedelta(days=i)
            ts = int(day.timestamp())
            if C.is_first_friday(ts):
                firsts.append(day)
                self.assertEqual(day.weekday(), 4)  # Friday
                self.assertLessEqual(day.day, 7)
        self.assertEqual(len(firsts), 12)

    def test_month_key_stable(self):
        ts = int(datetime(2026, 2, 6, 9, 30, tzinfo=timezone.utc).timestamp())
        self.assertEqual(C.month_key(ts), "2026-02")


class ReferralPool(unittest.TestCase):
    def test_per_halving_payout(self):
        self.assertEqual(C.REFERRAL_POOL, 2_000_000)
        self.assertEqual(C.REFERRAL_HALVING_PAYOUT, 500_000)
        self.assertEqual(C.referral_top_n(), 10_000)
        # 2M / 500k = 4 fully-funded halving payouts
        self.assertEqual(C.referral_halving_payout(0), 500_000)
        self.assertEqual(C.referral_halving_payout(1_500_000), 500_000)
        self.assertEqual(C.referral_halving_payout(2_000_000), 0)

    def test_pool_funds_exactly_four_halvings(self):
        paid = 0.0
        events = 0
        while True:
            amt = C.referral_halving_payout(paid)
            if amt <= 0:
                break
            paid += amt
            events += 1
            self.assertLessEqual(events, 100)
        self.assertEqual(paid, 2_000_000)
        self.assertEqual(events, 4)

    def test_share_split_across_top_10k(self):
        # Full field: 500,000 / 10,000 = 50 each
        self.assertEqual(C.referral_share_each(10_000), 50)
        self.assertEqual(C.referral_share_each(50_000), 50)  # capped at top 10k
        # Thin field: split only across the referrers that exist
        self.assertEqual(C.referral_share_each(5), 100_000)
        self.assertEqual(C.referral_share_each(0), 0)


class Units(unittest.TestCase):
    def test_round_trip(self):
        self.assertEqual(to_units(1), UNITS_PER_BRAH)
        self.assertEqual(to_units(ECO["empower"]), 3_000_000 * UNITS_PER_BRAH)
        self.assertEqual(from_units(to_units(30_000)), "30000")
        self.assertEqual(from_units(to_units(0.5)), "0.5")


if __name__ == "__main__":
    unittest.main(verbosity=2)
