"""
Integration tests for the reworked protocol distributions in l1_mainnet.py:
empower (first 100 wallets at mine completion), founder (2nd wallet), strategic
reserve monthly burn, referral per-halving payout, and the removal of the
donation protocol and L2 memecoins.

The full node depends on operator-only `p2p` and `brahm_rail` modules that are not
in this repository, so this test installs minimal in-memory stubs for them and a
throwaway genesis_accounts.json before importing l1_mainnet. Only networking/rail
is stubbed; all ledger/economics logic under test is the real code.

Run: python test_distributions.py   (from btq-node/)
"""
from __future__ import annotations

import json
import os
import sys
import tempfile
import types
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

_DATA = tempfile.mkdtemp(prefix="brah-dist-")
os.environ["BRAH_DATA_DIR"] = _DATA

# --- stub the operator-only networking + payments-rail modules ---
_rail = types.ModuleType("brahm_rail")
_rail.RAIL_RPC_METHODS = frozenset()
_rail.enqueue_webhook_events = lambda *a, **k: None
_rail.ensure_rail = lambda state: None
_rail.handle_v1 = lambda *a, **k: (404, {})
_rail.on_sealed_send = lambda *a, **k: []
_rail.openapi_html = lambda *a, **k: ""
_rail.rpc_rail_dispatch = lambda *a, **k: None
_rail.verify_api_key = lambda *a, **k: None
sys.modules["brahm_rail"] = _rail

_p2p = types.ModuleType("p2p")


class _PeerNetStub:
    def __init__(self, *a, **k):
        pass

    def start(self):
        pass

    def broadcast(self, *a, **k):
        pass

    def peer_count(self):
        return 0


_p2p.PeerNet = _PeerNetStub
sys.modules["p2p"] = _p2p

from crypto_mldsa import address_from_pubkey, normalize_address  # noqa: E402
from units import to_units  # noqa: E402


def mint(tag: str) -> str:
    return normalize_address(address_from_pubkey(("btq-" + tag).encode()))


# throwaway genesis accounts (addresses only; no secrets, no signing needed here)
_accounts = {
    "algo": "Dilithium3",
    "founder": mint("founder-key"),
    "empower": mint("empower-key"),
    "strategic": mint("strategic-key"),
    "council": mint("council-key"),
    "distribution": [mint(f"dist-{i:02d}") for i in range(50)],
    "public_keys": {},
}
Path(_DATA, "genesis_accounts.json").write_text(json.dumps(_accounts))

import l1_mainnet as L  # noqa: E402


def first_friday_ts(year: int, month: int) -> int:
    d = datetime(year, month, 1, 12, 0, tzinfo=timezone.utc)
    while d.weekday() != 4:
        d += timedelta(days=1)
    return int(d.timestamp())


def fresh_state() -> dict:
    # build_initial_state persists to the temp data dir on first call; reload a clean copy.
    return L.load_or_init()


class GenesisPools(unittest.TestCase):
    def test_pool_seed_amounts(self):
        s = fresh_state()
        self.assertEqual(int(L.account(s, "POOL_MINING")["0"]), to_units(50_000_000))
        self.assertEqual(int(L.account(s, "POOL_AIRDROP")["0"]), to_units(30_000_000))
        self.assertEqual(int(L.account(s, "POOL_EMPOWER")["0"]), to_units(3_000_000))
        self.assertEqual(int(L.account(s, "POOL_STRATEGIC")["0"]), to_units(3_000_000))
        self.assertEqual(int(L.account(s, "POOL_FOUNDER")["0"]), to_units(2_000_000))
        self.assertEqual(int(L.account(s, "POOL_REFERRAL")["0"]), to_units(2_000_000))

    def test_outstanding_equals_100m_cap(self):
        s = fresh_state()
        self.assertEqual(L.brah_outstanding_units(s), to_units(100_000_000))
        # must not raise
        L.assert_supply_cap(s)

    def test_no_donation_pool(self):
        s = fresh_state()
        self.assertNotIn("POOL_DONATION", s["balances"])
        self.assertNotIn("donation_unlock", s)


class FounderGrant(unittest.TestCase):
    def test_second_wallet_receives_2m(self):
        s = fresh_state()
        w1, w2 = mint("w1"), mint("w2")
        s["wallet_order"] = [w1, w2]
        self.assertTrue(L.maybe_grant_founder(s))
        self.assertEqual(int(L.account(s, w2)["0"]), to_units(2_000_000))
        self.assertEqual(int(L.account(s, w1)["0"]), 0)
        self.assertEqual(int(L.account(s, "POOL_FOUNDER")["0"]), 0)
        self.assertTrue(s["founder_paid"])
        # idempotent
        self.assertFalse(L.maybe_grant_founder(s))

    def test_not_paid_before_second_wallet_exists(self):
        s = fresh_state()
        s["wallet_order"] = [mint("only")]
        self.assertFalse(L.maybe_grant_founder(s))
        self.assertEqual(int(L.account(s, "POOL_FOUNDER")["0"]), to_units(2_000_000))


class EmpowerDistribution(unittest.TestCase):
    def test_requires_mining_complete(self):
        s = fresh_state()
        s["wallet_order"] = [mint(f"e{i}") for i in range(100)]
        self.assertFalse(L.maybe_distribute_empower(s))  # mining not done

    def test_first_100_each_get_30k(self):
        s = fresh_state()
        wallets = [mint(f"e{i}") for i in range(120)]
        s["wallet_order"] = wallets
        s["total_mined"] = int(s["mining_cap"])  # all coins mined
        self.assertTrue(L.maybe_distribute_empower(s))
        each = to_units(3_000_000) // 100
        self.assertEqual(each, to_units(30_000))
        for w in wallets[:100]:
            self.assertEqual(int(L.account(s, w)["0"]), each)
        for w in wallets[100:]:
            self.assertEqual(int(L.account(s, w)["0"]), 0)  # only the first 100
        self.assertEqual(int(L.account(s, "POOL_EMPOWER")["0"]), 0)
        self.assertTrue(L.maybe_distribute_empower(s) is False)  # idempotent


class StrategicBurn(unittest.TestCase):
    def test_first_friday_after_mining_burns_100k(self):
        s = fresh_state()
        s["total_mined"] = int(s["mining_cap"])
        ts = first_friday_ts(2027, 1)
        before = int(L.account(s, "POOL_STRATEGIC")["0"])
        self.assertTrue(L.maybe_strategic_monthly_burn(s, ts))
        self.assertEqual(int(L.account(s, "POOL_STRATEGIC")["0"]), before - to_units(100_000))
        self.assertEqual(int(s["burned_brah"]), to_units(100_000))
        # same month, no double burn
        self.assertFalse(L.maybe_strategic_monthly_burn(s, ts + 3600))

    def test_not_before_mining_complete(self):
        s = fresh_state()
        self.assertFalse(L.maybe_strategic_monthly_burn(s, first_friday_ts(2027, 1)))

    def test_exhausts_reserve_in_30_months(self):
        s = fresh_state()
        s["total_mined"] = int(s["mining_cap"])
        burns = 0
        y, m = 2027, 1
        for _ in range(40):
            if L.maybe_strategic_monthly_burn(s, first_friday_ts(y, m)):
                burns += 1
            m += 1
            if m > 12:
                m = 1
                y += 1
        self.assertEqual(burns, 30)
        self.assertEqual(int(L.account(s, "POOL_STRATEGIC")["0"]), 0)
        self.assertEqual(int(s["burned_brah"]), to_units(3_000_000))


class ReferralHalving(unittest.TestCase):
    def test_payout_split_across_referrers(self):
        s = fresh_state()
        refs = {mint(f"ref{i}"): (i + 1) for i in range(20)}
        s["referrals"] = refs
        s["joined_count"] = 10_001  # first mine-schedule halving band (0)
        self.assertEqual(L.mine_halving_band(10_001), 0)
        pool_before = int(L.account(s, "POOL_REFERRAL")["0"])
        self.assertTrue(L.maybe_pay_referral_halving(s))
        each = to_units(500_000) // 20
        distributed = each * 20
        self.assertEqual(int(L.account(s, "POOL_REFERRAL")["0"]), pool_before - distributed)
        # one of the referrers got `each`
        any_ref = next(iter(refs))
        self.assertEqual(int(L.account(s, any_ref)["0"]), each)
        # same band: no repeat
        self.assertFalse(L.maybe_pay_referral_halving(s))

    def test_no_payout_before_first_halving(self):
        s = fresh_state()
        s["referrals"] = {mint("r"): 5}
        s["joined_count"] = 500  # band -1
        self.assertFalse(L.maybe_pay_referral_halving(s))


class ProtocolsRemoved(unittest.TestCase):
    def _tx(self, **kw):
        base = {
            "from": mint("a"),
            "to": mint("b"),
            "amount": 1,
            "nonce": 0,
            "asset": "0",
            "memo": "",
            "chain_id": fresh_state()["chain_id"],
        }
        base.update(kw)
        return base

    def test_donate_rejected(self):
        s = fresh_state()
        with self.assertRaises(ValueError) as ctx:
            L.apply_recorded_tx(s, self._tx(kind="donate"))
        self.assertIn("Donation", str(ctx.exception))

    def test_launch_l2_rejected(self):
        s = fresh_state()
        with self.assertRaises(ValueError) as ctx:
            L.apply_recorded_tx(s, self._tx(kind="launch_l2"))
        self.assertIn("Memecoins removed", str(ctx.exception))

    def test_sending_non_brahma_asset_rejected(self):
        s = fresh_state()
        with self.assertRaises(ValueError) as ctx:
            L.apply_recorded_tx(s, self._tx(kind="send", asset="5"))
        self.assertIn("Memecoins removed", str(ctx.exception))

    def test_protocol_pools_are_the_new_set(self):
        self.assertEqual(
            set(L.PROTOCOL_POOLS),
            {
                "POOL_MINING",
                "POOL_AIRDROP",
                "POOL_EMPOWER",
                "POOL_STRATEGIC",
                "POOL_FOUNDER",
                "POOL_REFERRAL",
            },
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
