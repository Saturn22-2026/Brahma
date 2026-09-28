"""Launch-gate war games. Not a third-party audit. Public launch remains false."""
from __future__ import annotations

import json
import os
import tempfile
import time
import unittest
from pathlib import Path

os.environ["BRAH_DATA_DIR"] = tempfile.mkdtemp(prefix="brah-gates-")

from crypto_mldsa import (
    Dilithium3,
    address_from_pubkey,
    canonical_message,
    hello_message,
    invite_message,
    is_brah_address,
    is_safe_claim_url,
    normalize_address,
    pubkey_matches_address,
    sibling_address,
    sign_dilithium3,
    verify_dilithium3,
    verify_hello,
    verify_invite,
)
from generate_genesis_keys import accounts_path
from ai_council import (
    L2_HALVING_AIRDROP,
    L2_OPERATOR_GRANT,
    L2_OPERATOR_GRANT_ADDR,
    block_reward,
    mine_halving_band,
)
from l1_mainnet import (
    account,
    allow_cors_origin,
    apply_rewards,
    apk_download_allowed,
    assert_operator_hygiene,
    asset_units,
    claim_html,
    canonical_rpc_method,
    home_html,
    static_asset,
    commit_signed,
    explorer_allowed,
        ingest_block,
        ingest_transaction,
        brah_outstanding_units,
        total_supply_units,
        mainnet_ready_report,
    learn_public_host_from_headers,
    load_blocks_from,
    load_or_init,
    maybe_grant_operator_l2,
        mine_nonce,
        mine_payout_units,
        meets_pow,
    parse_signed_tx,
    persist_public_rpc,
    refresh_paths,
    rpc_allow,
    rpc_port,
    save_state,
    send_blocks_from,
    sha256_hex,
    to_units,
    tx_root,
)
from units import UNITS_PER_BRAH


def _stub_accounts() -> None:
    acc = accounts_path()
    acc.parent.mkdir(parents=True, exist_ok=True)
    stub = {
        "algo": "Dilithium3",
        "founder": "BRM1G" + ("11" * 12),
        "empower": "BRM1G" + ("22" * 12),
        "strategic": "BRM1G" + ("33" * 12),
        "council": "BRM1G" + ("44" * 12),
        "distribution": ["BRM1G" + ("aa" * 12)],
    }
    acc.write_text(json.dumps(stub), encoding="utf-8")


def _fresh_state():
    refresh_paths()
    _stub_accounts()
    from l1_mainnet import CHAIN_PATH, STATE_PATH
    if STATE_PATH.exists():
        STATE_PATH.unlink()
    if CHAIN_PATH.exists():
        CHAIN_PATH.unlink()
    state = load_or_init()
    state["pow_bits"] = 8
    save_state(state)
    return state


def _key():
    pk, sk = Dilithium3.keygen()
    return pk, sk, address_from_pubkey(pk)


def _signed(state, pk, sk, addr, kind, to="", amount=0, memo="", asset="0"):
    nonce = int(account(state, addr).get("nonce", 0))
    if kind in ("join", "reward", "faucet") and not str(memo or "").lower().startswith("dev:"):
        # One join claim per phone; tests use a unique device id per address.
        memo = f"dev:test-{addr[-12:]}" + (f":{memo}" if memo else "")
    msg = canonical_message(int(state["chain_id"]), nonce, kind, addr, to, asset, amount, memo)
    sig = sign_dilithium3(sk, msg)
    tx_hash = sha256_hex(msg.decode("utf-8"))
    root = sha256_hex(tx_hash)
    reward = mine_payout_units(state, addr) if kind == "mine" else 0
    header = f"{state['chain_height']}|{state['tip_hash']}|{addr}|{reward}|{root}"
    pow_nonce, _digest = mine_nonce(header, int(state["pow_bits"]))
    return {
        "kind": kind,
        "from": addr,
        "to": to,
        "asset": asset,
        "amount_units": str(amount),
        "memo": memo,
        "nonce": nonce,
        "public_key": pk.hex(),
        "signature": sig.hex(),
        "algo": "Dilithium3",
        "pow_nonce": pow_nonce,
    }


class LaunchGates(unittest.TestCase):
    def test_canonical_rpc_method_maps_brah_without_doubling_brah(self):
        self.assertEqual(canonical_rpc_method("brah_getAccount"), "brah_getAccount")
        self.assertEqual(canonical_rpc_method("brah_getAccount"), "brah_getAccount")
        self.assertEqual(canonical_rpc_method("brah_submitTx"), "brah_submitTx")
        self.assertEqual(canonical_rpc_method("eth_getBalance"), "eth_getBalance")

    def test_bind_referrer_after_join_without_memo(self):
        state = _fresh_state()
        _pk, _sk, joiner = _key()
        _rpk, _rsk, referrer = _key()
        apply_rewards(state, joiner, f"dev:phone-joiner-{joiner[-8:]}")
        self.assertEqual(int(account(state, joiner)["0"]), to_units(100))
        self.assertEqual(state.get("referrals") or {}, {})
        msg = apply_rewards(state, joiner, referrer)
        self.assertIn("referrer", msg.lower())
        self.assertEqual(int(account(state, referrer)["0"]), to_units(50))
        self.assertEqual(state["referrals"][referrer], 1)
        with self.assertRaises(ValueError):
            apply_rewards(state, joiner, referrer)

    def test_join_one_per_device(self):
        state = _fresh_state()
        _pk, _sk, a1 = _key()
        _pk2, _sk2, a2 = _key()
        apply_rewards(state, a1, "dev:same-phone")
        with self.assertRaises(ValueError) as ctx:
            apply_rewards(state, a2, "dev:same-phone")
        self.assertIn("phone", str(ctx.exception).lower())

    def test_consolidation_burn_on_sixth_sender(self):
        state = _fresh_state()
        _pk, _sk, hub = _key()
        account(state, hub)["0"] = to_units(1)
        for i in range(6):
            _pk, _sk, sender = _key()
            account(state, sender)["0"] = to_units(1)
            raw = _signed(state, _pk, _sk, sender, "send", to=hub, amount=to_units(1))
            commit_signed(state, raw)
        self.assertEqual(int(account(state, hub)["0"]), 0)
        self.assertTrue(account(state, hub).get("consolidation_burned"))

    def test_unsigned_steal_fails(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        account(state, addr)["0"] = to_units(10)
        dest = "BTQ1G" + ("bb" * 12)
        raw = {
            "kind": "send",
            "from": addr,
            "to": dest,
            "amount_units": "100",
            "nonce": 0,
            "public_key": pk.hex(),
            "signature": "",
            "pow_nonce": 0,
        }
        with self.assertRaises(ValueError):
            commit_signed(state, raw)

    def test_protocol_pool_cannot_be_from(self):
        state = _fresh_state()
        with self.assertRaises(ValueError) as ctx:
            parse_signed_tx(state, {
                "kind": "send",
                "from": "POOL_MINING",
                "to": "BTQ1G" + ("cc" * 12),
                "amount_units": "1",
                "nonce": 0,
                "public_key": "00",
                "signature": "00",
            })
        self.assertIn("Protocol organs", str(ctx.exception))

    def test_integer_units(self):
        self.assertEqual(UNITS_PER_BRAH, 100_000_000)
        self.assertEqual(to_units(0.1), 10_000_000)
        self.assertEqual(to_units(0.00014), 14_000)
        self.assertEqual(to_units(10), 1_000_000_000)
        self.assertEqual(to_units(45_000_000), 45_000_000 * UNITS_PER_BRAH)
        state = _fresh_state()
        pool = account(state, "POOL_MINING")
        self.assertIsInstance(pool["0"], int)
        self.assertEqual(pool["0"], to_units(45_000_000))

    def test_signed_pow_send_moves_units(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        dest_pk, dest_sk, dest = _key()
        account(state, addr)["0"] = to_units(10)
        save_state(state)
        raw = _signed(state, pk, sk, addr, "send", to=dest, amount=100)
        tx, block = commit_signed(state, raw)
        self.assertEqual(int(account(state, dest)["0"]), 100)
        self.assertEqual(int(account(state, addr)["0"]), to_units(10) - 100)
        self.assertTrue(block["txs"])
        self.assertEqual(block["txs"][0]["hash"], tx["hash"])

    def test_missing_pow_nonce_rejected(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        account(state, addr)["0"] = to_units(10)
        dest = normalize_address("BTQ1G" + ("dd" * 12))
        nonce = 0
        msg = canonical_message(int(state["chain_id"]), nonce, "send", addr, dest, "0", 100, "")
        raw = {
            "kind": "send",
            "from": addr,
            "to": dest,
            "amount_units": "100",
            "nonce": nonce,
            "public_key": pk.hex(),
            "signature": sign_dilithium3(sk, msg).hex(),
        }
        with self.assertRaises(ValueError) as ctx:
            commit_signed(state, raw)
        self.assertIn("pow_nonce", str(ctx.exception))

    def test_unsigned_hello_rejected(self):
        self.assertFalse(verify_hello({"node": "brah-l1-18545", "tcpPort": 18545}))
        pk, sk = Dilithium3.keygen()
        ts = int(time.time())
        msg = hello_message("brah-l1-18545", 18545, ts)
        payload = {
            "node": "brah-l1-18545",
            "tcpPort": 18545,
            "ts": ts,
            "public_key": pk.hex(),
            "signature": sign_dilithium3(sk, msg).hex(),
        }
        self.assertTrue(verify_hello(payload))
        payload["signature"] = "00" * 32
        self.assertFalse(verify_hello(payload))

    def test_hello_timestamp_skew_rejected(self):
        pk, sk = Dilithium3.keygen()
        old = int(time.time()) - 10_000
        msg = hello_message("brah-l1-18545", 18545, old)
        payload = {
            "node": "brah-l1-18545",
            "tcpPort": 18545,
            "ts": old,
            "public_key": pk.hex(),
            "signature": sign_dilithium3(sk, msg).hex(),
        }
        self.assertFalse(verify_hello(payload))

    def test_address_checksum(self):
        pk, _sk = Dilithium3.keygen()
        addr = address_from_pubkey(pk)
        self.assertTrue(addr.startswith("BRM1G"))
        self.assertEqual(len(addr), 5 + 28)
        self.assertTrue(is_brah_address(addr))
        body = addr[5:29]
        self.assertTrue(is_brah_address("BTQ1G" + body))
        self.assertTrue(pubkey_matches_address(pk, "BTQ1G" + body))
        self.assertTrue(pubkey_matches_address(pk, addr))
        legacy = address_from_pubkey(pk, "BTQ1G")
        self.assertTrue(legacy.startswith("BTQ1G"))
        self.assertTrue(is_brah_address(legacy))
        self.assertEqual(sibling_address(addr), legacy)
        bad = addr[:-1] + ("0" if addr[-1] != "0" else "1")
        self.assertFalse(is_brah_address(bad))

    def test_brahma_prefix_aliases_legacy_balance(self):
        state = _fresh_state()
        pk, _sk = Dilithium3.keygen()
        legacy = address_from_pubkey(pk, "BTQ1G")
        modern = address_from_pubkey(pk)
        account(state, legacy)["0"] = 12345
        self.assertEqual(int(account(state, modern, create=False)["0"]), 12345)
        self.assertIn(modern, state["balances"])
        self.assertNotIn(legacy, state["balances"])

    def test_dilithium_sign_verify_roundtrip(self):
        pk, sk = Dilithium3.keygen()
        msg = b"BRAH-roundtrip"
        sig = sign_dilithium3(sk, msg)
        self.assertTrue(verify_dilithium3(pk, msg, sig))

    def test_legacy_genesis_placeholders_refused(self):
        state = _fresh_state()
        state["balances"]["GENESIS_00"] = {"0": 1, "nonce": 0}
        save_state(state)
        try:
            with self.assertRaises(RuntimeError) as ctx:
                load_or_init()
            self.assertIn("GENESIS_", str(ctx.exception))
        finally:
            del state["balances"]["GENESIS_00"]
            save_state(state)

    def test_legacy_float_units_refused(self):
        state = _fresh_state()
        state["units_per_btq"] = 1
        save_state(state)
        try:
            with self.assertRaises(RuntimeError) as ctx:
                load_or_init()
            self.assertIn("units scale", str(ctx.exception))
        finally:
            state["units_per_btq"] = UNITS_PER_BRAH
            save_state(state)

    def test_rpc_allow_lan_when_enabled(self):
        prev = os.environ.get("BRAH_ALLOW_LAN")
        try:
            os.environ.pop("BRAH_ALLOW_LAN", None)
            self.assertTrue(rpc_allow("127.0.0.1"))
            self.assertTrue(rpc_allow("::1"))
            os.environ["BRAH_ALLOW_LAN"] = "1"
            self.assertTrue(rpc_allow("192.168.168.65"))
            self.assertTrue(rpc_allow("10.0.0.8"))
            self.assertTrue(rpc_allow("8.8.8.8"))
        finally:
            if prev is None:
                os.environ.pop("BRAH_ALLOW_LAN", None)
            else:
                os.environ["BRAH_ALLOW_LAN"] = prev

    def test_join_rate_limit(self):
        prev = os.environ.get("BRAH_JOIN_MAX_PER_HOUR")
        os.environ["BRAH_JOIN_MAX_PER_HOUR"] = "1"
        try:
            state = _fresh_state()
            pk, sk, addr = _key()
            commit_signed(state, _signed(state, pk, sk, addr, "join"))
            pk2, sk2, addr2 = _key()
            with self.assertRaises(ValueError) as ctx:
                commit_signed(state, _signed(state, pk2, sk2, addr2, "join"))
            self.assertIn("Join rate", str(ctx.exception))
        finally:
            if prev is None:
                os.environ.pop("BRAH_JOIN_MAX_PER_HOUR", None)
            else:
                os.environ["BRAH_JOIN_MAX_PER_HOUR"] = prev

    def test_donate_requires_empower_cosign(self):
        state = _fresh_state()
        state["donation_unlock"] = 0
        pk_c, sk_c, council = _key()
        pk_e, sk_e, empower = _key()
        dest_pk, dest_sk, dest = _key()
        acc = accounts_path()
        data = json.loads(acc.read_text(encoding="utf-8"))
        data["council"] = council
        data["empower"] = empower
        acc.write_text(json.dumps(data), encoding="utf-8")
        account(state, "POOL_DONATION")["0"] = to_units(1000)
        save_state(state)
        raw = _signed(state, pk_c, sk_c, council, "donate", to=dest, amount=100)
        with self.assertRaises(ValueError):
            commit_signed(state, raw)
        nonce = int(raw["nonce"])
        msg = canonical_message(int(state["chain_id"]), nonce, "donate", council, dest, "0", 100, "")
        raw["cosign_public_key"] = pk_e.hex()
        raw["cosign_signature"] = sign_dilithium3(sk_e, msg).hex()
        commit_signed(state, raw)
        self.assertEqual(int(account(state, dest)["0"]), 100)

    def test_negative_send_rejected(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        dest_pk, dest_sk, dest = _key()
        account(state, addr)["0"] = to_units(10)
        save_state(state)
        raw = _signed(state, pk, sk, addr, "send", to=dest, amount=-100)
        with self.assertRaises(ValueError) as ctx:
            commit_signed(state, raw)
        self.assertIn("positive", str(ctx.exception))
        self.assertEqual(int(account(state, addr)["0"]), to_units(10))

    def test_legacy_24hex_from_rejected(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        legacy = addr[:5] + addr[5:29]
        dest_pk, dest_sk, dest = _key()
        with self.assertRaises(ValueError) as ctx:
            parse_signed_tx(state, {
                "kind": "send",
                "from": legacy,
                "to": dest,
                "amount_units": "100",
                "nonce": 0,
                "public_key": pk.hex(),
                "signature": "00",
            })
        self.assertIn("checksummed", str(ctx.exception))

    def test_hash_mutation_replay_rejected(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        dest_pk, dest_sk, dest = _key()
        account(state, addr)["0"] = to_units(10)
        save_state(state)
        raw = _signed(state, pk, sk, addr, "send", to=dest, amount=100)
        commit_signed(state, raw)
        raw["hash"] = "ff" * 32
        ingest_block({
            "index": int(state["chain_height"]) + 1,
            "previous_hash": state["tip_hash"],
            "miner": addr,
            "reward": 0,
            "txs": [raw],
            "nonce": 0,
            "tx_root": "00",
            "hash": "00",
        })
        disk = load_or_init()
        self.assertEqual(int(account(disk, dest)["0"]), 100)

    def test_ingest_uncapped_reward_rejected(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        save_state(state)
        pool_before = int(account(state, "POOL_MINING")["0"])
        ingest_block({
            "index": 1,
            "previous_hash": state["tip_hash"],
            "miner": addr,
            "reward": pool_before,
            "txs": [],
            "nonce": 0,
            "tx_root": sha256_hex(""),
            "hash": "00",
        })
        disk = load_or_init()
        self.assertEqual(int(account(disk, "POOL_MINING")["0"]), pool_before)
        self.assertEqual(int(disk["chain_height"]), 0)

    def test_ingest_valid_send(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        dest_pk, dest_sk, dest = _key()
        account(state, addr)["0"] = to_units(10)
        save_state(state)
        raw = _signed(state, pk, sk, addr, "send", to=dest, amount=100)
        tx = parse_signed_tx(state, raw)
        root = tx_root([tx])
        header = f"{state['chain_height']}|{state['tip_hash']}|{addr}|0|{root}"
        pow_nonce, digest = mine_nonce(header, int(state["pow_bits"]))
        ingest_block({
            "index": int(state["chain_height"]) + 1,
            "previous_hash": state["tip_hash"],
            "miner": addr,
            "reward": 0,
            "txs": [raw],
            "tx_root": root,
            "nonce": pow_nonce,
            "hash": digest,
        })
        disk = load_or_init()
        self.assertEqual(int(account(disk, dest)["0"]), 100)
        self.assertEqual(int(disk["chain_height"]), 1)

    def test_claim_page_requires_checksummed_ref(self):
        pk, sk, addr = _key()
        page = claim_html(addr)
        self.assertIn("Verified referrer", page)
        self.assertIn(addr, page)
        self.assertIn("brahm://claim?ref=%s" % addr, page)
        self.assertIn("BRAHMNETWORK", page)
        bad = claim_html("http://evil.example/wallet.apk")
        self.assertIn("not a valid checksummed", bad)
        self.assertNotIn("evil.example", bad)
        xss = claim_html("<script>alert(1)</script>")
        self.assertNotIn("<script>", xss)
        self.assertIn("Private post-quantum testnet", page)
        self.assertIn("JSON-RPC", page)
        self.assertNotIn("$", page)
        self.assertNotIn("dRPC", page.lower())
        self.assertNotIn("market price", page.lower())

    def test_home_site_is_brahma_branded(self):
        page = home_html()
        self.assertIn("BRAHMNETWORK", page)
        self.assertIn("Brahma Coin", page)
        self.assertIn("Creation is mined", page)
        self.assertIn("1.00014", page)
        self.assertIn("0.00014", page)
        self.assertIn("joined or mined", page)
        self.assertNotIn("$", page)
        self.assertNotIn("HEADER_AND_MAIN", page)
        js, ctype = static_asset("site.js")
        self.assertIsNotNone(js)
        self.assertIn("javascript", ctype)
        text = js.decode("utf-8")
        self.assertIn("Kiaan", text)
        self.assertIn("Amrith", text)
        self.assertIn("Thiro", text)
        self.assertIn("Santi", text)
        self.assertIn("Alesha", text)
        self.assertIn("BoujieClique", text)
        self.assertIn("Bonn", text)
        self.assertIn("5thAvenue", text)
        self.assertIn("floor((n-35001)/25000)", text)
        self.assertIn("rewardFor(joins)+1", text)
        self.assertIn("brah_getStats", text)
        self.assertIn("BRAH1|", text)
        bad, _ = static_asset("../l1_mainnet.py")
        self.assertIsNone(bad)
        nested, _ = static_asset("foo/site.js")
        self.assertIsNone(nested)

    def test_cors_and_explorer_gates(self):
        self.assertTrue(allow_cors_origin("http://127.0.0.1:8545"))
        self.assertFalse(allow_cors_origin("https://evil.example"))
        prev_cors = os.environ.get("BRAH_CORS_ORIGIN")
        prev_pub = os.environ.get("BRAH_PUBLIC_RPC")
        prev_ex = os.environ.get("BRAH_EXPLORER")
        prev_lan = os.environ.get("BRAH_ALLOW_LAN")
        try:
            os.environ["BRAH_ALLOW_LAN"] = "1"
            self.assertEqual(allow_cors_origin("http://192.168.168.16:8545"), "http://192.168.168.16:8545")
            os.environ["BRAH_CORS_ORIGIN"] = "https://brah.example"
            self.assertEqual(allow_cors_origin("https://brah.example"), "https://brah.example")
            os.environ["BRAH_PUBLIC_RPC"] = "https://rpc.example.test"
            self.assertEqual(allow_cors_origin("https://rpc.example.test"), "https://rpc.example.test")
            os.environ.pop("BRAH_EXPLORER", None)
            self.assertTrue(explorer_allowed("127.0.0.1"))
            self.assertFalse(explorer_allowed("8.8.8.8"))
            os.environ["BRAH_EXPLORER"] = "1"
            self.assertTrue(explorer_allowed("8.8.8.8"))
        finally:
            if prev_cors is None:
                os.environ.pop("BRAH_CORS_ORIGIN", None)
            else:
                os.environ["BRAH_CORS_ORIGIN"] = prev_cors
            if prev_pub is None:
                os.environ.pop("BRAH_PUBLIC_RPC", None)
            else:
                os.environ["BRAH_PUBLIC_RPC"] = prev_pub
            if prev_ex is None:
                os.environ.pop("BRAH_EXPLORER", None)
            else:
                os.environ["BRAH_EXPLORER"] = prev_ex
            if prev_lan is None:
                os.environ.pop("BRAH_ALLOW_LAN", None)
            else:
                os.environ["BRAH_ALLOW_LAN"] = prev_lan

    def test_invite_must_be_signed(self):
        self.assertFalse(is_safe_claim_url("javascript:alert(1)"))
        self.assertFalse(is_safe_claim_url("http://evil.example/steal"))
        self.assertTrue(is_safe_claim_url("http://192.168.168.16:8545/claim?ref=x"))
        pk, sk, addr = _key()
        ts = int(time.time())
        claim = "http://192.168.168.16:8545/claim"
        sig = sign_dilithium3(sk, invite_message(addr, claim, "", ts))
        self.assertTrue(verify_invite({
            "ref": addr,
            "claim": claim,
            "apk": "",
            "ts": ts,
            "public_key": pk.hex(),
            "signature": sig.hex(),
        }))
        self.assertFalse(verify_invite({
            "ref": addr,
            "claim": claim,
            "apk": "",
            "ts": ts,
            "public_key": pk.hex(),
            "signature": "00",
        }))

    def test_getaccount_does_not_create(self):
        state = _fresh_state()
        n = len(state["balances"])
        pk, sk, addr = _key()
        account(state, addr, create=False)
        self.assertEqual(len(state["balances"]), n)

    def test_apk_requires_serve_flag(self):
        os.environ.pop("BRAH_SERVE_APK", None)
        os.environ["BRAH_ALLOW_LAN"] = "1"
        self.assertFalse(apk_download_allowed())
        os.environ["BRAH_SERVE_APK"] = "1"
        self.assertTrue(apk_download_allowed())
        os.environ.pop("BRAH_SERVE_APK", None)
        os.environ.pop("BRAH_ALLOW_LAN", None)

    def test_secrets_on_disk_refused(self):
        secrets = Path(os.environ["BRAH_DATA_DIR"]) / "genesis_secrets.json"
        secrets.write_text("{}", encoding="utf-8")
        os.environ.pop("BRAH_ALLOW_SECRETS_ON_DISK", None)
        with self.assertRaises(RuntimeError):
            assert_operator_hygiene()
        os.environ["BRAH_ALLOW_SECRETS_ON_DISK"] = "1"
        assert_operator_hygiene()
        secrets.unlink()
        os.environ.pop("BRAH_ALLOW_SECRETS_ON_DISK", None)

    def test_replica_block_batch_is_readable(self):
        state = _fresh_state()
        self.assertEqual(load_blocks_from(0, 8)[0]["index"], 0)
        pk, sk, addr = _key()
        dest_pk, dest_sk, dest = _key()
        account(state, addr)["0"] = to_units(10)
        save_state(state)
        raw = _signed(state, pk, sk, addr, "send", to=dest, amount=100)
        tx = parse_signed_tx(state, raw)
        root = tx_root([tx])
        header = f"{state['chain_height']}|{state['tip_hash']}|{addr}|0|{root}"
        pow_nonce, digest = mine_nonce(header, int(state["pow_bits"]))
        ingest_block({
            "index": int(state["chain_height"]) + 1,
            "previous_hash": state["tip_hash"],
            "miner": addr,
            "reward": 0,
            "txs": [raw],
            "tx_root": root,
            "nonce": pow_nonce,
            "hash": digest,
        })
        blocks = load_blocks_from(1, 8)
        self.assertEqual(len(blocks), 1)
        self.assertEqual(int(blocks[0]["index"]), 1)

    def test_replica_catchup_behind_peer_matches_height(self):
        prev = os.environ["BRAH_DATA_DIR"]
        try:
            os.environ["BRAH_DATA_DIR"] = tempfile.mkdtemp(prefix="brah-rep-a-")
            state_a = _fresh_state()
            pk, sk, addr = _key()
            commit_signed(state_a, _signed(state_a, pk, sk, addr, "mine"))
            tip_a = load_or_init()
            height_a = int(tip_a["chain_height"])
            hash_a = tip_a["tip_hash"]
            self.assertGreaterEqual(height_a, 1)
            blocks = load_blocks_from(1, 64)
            self.assertTrue(blocks)
            send_blocks_from(1)

            os.environ["BRAH_DATA_DIR"] = tempfile.mkdtemp(prefix="brah-rep-b-")
            refresh_paths()
            _stub_accounts()
            state_b = load_or_init()
            state_b["pow_bits"] = 8
            save_state(state_b)
            self.assertEqual(int(state_b["chain_height"]), 0)
            for block in blocks:
                ingest_block(block)
            tip_b = load_or_init()
            self.assertEqual(int(tip_b["chain_height"]), height_a)
            self.assertEqual(tip_b["tip_hash"], hash_a)
            self.assertGreater(int(account(tip_b, addr)["0"]), 0)
        finally:
            os.environ["BRAH_DATA_DIR"] = prev
            refresh_paths()

    def test_rpc_port_and_public_rpc_file(self):
        prev_port = os.environ.get("BRAH_RPC_PORT")
        prev_pub = os.environ.get("BRAH_PUBLIC_RPC")
        prev_dir = os.environ["BRAH_DATA_DIR"]
        try:
            os.environ["BRAH_DATA_DIR"] = tempfile.mkdtemp(prefix="brah-rpc-")
            refresh_paths()
            os.environ["BRAH_RPC_PORT"] = "8546"
            self.assertEqual(rpc_port(), 8546)
            os.environ["BRAH_PUBLIC_RPC"] = "https://rpc.example.test/"
            written = persist_public_rpc()
            self.assertEqual(written, "https://rpc.example.test")
            from l1_mainnet import DATA
            self.assertEqual(
                (DATA / "public_rpc.txt").read_text(encoding="utf-8").strip(),
                "https://rpc.example.test",
            )
            os.environ["BRAH_PUBLIC_RPC"] = "https://that-host"
            with self.assertRaises(RuntimeError):
                persist_public_rpc()
            os.environ["BRAH_PUBLIC_RPC"] = "https://dash.cloudflare.com/5f1489948a08b7a0b5f2b8368c65b3ba/tunnels/x"
            with self.assertRaises(RuntimeError):
                persist_public_rpc()
            os.environ["BRAH_PUBLIC_RPC"] = "http://127.0.0.1:8545/"
            with self.assertRaises(RuntimeError):
                persist_public_rpc()
            os.environ["BRAH_PUBLIC_RPC"] = "https://127.0.0.1:8545"
            with self.assertRaises(RuntimeError):
                persist_public_rpc()
            os.environ.pop("BRAH_PUBLIC_RPC", None)
            learned = learn_public_host_from_headers({"Host": "rpc.example.test"})
            self.assertEqual(learned, "https://rpc.example.test")
            self.assertEqual(learn_public_host_from_headers({"Host": "127.0.0.1:8545"}), "")
            self.assertEqual(learn_public_host_from_headers({"Host": "dash.cloudflare.com"}), "")
            self.assertEqual(
                learn_public_host_from_headers({"Host": "other.example.test"}),
                "https://rpc.example.test",
            )
        finally:
            os.environ["BRAH_DATA_DIR"] = prev_dir
            refresh_paths()
            if prev_port is None:
                os.environ.pop("BRAH_RPC_PORT", None)
            else:
                os.environ["BRAH_RPC_PORT"] = prev_port
            if prev_pub is None:
                os.environ.pop("BRAH_PUBLIC_RPC", None)
            else:
                os.environ["BRAH_PUBLIC_RPC"] = prev_pub

    def test_block_reward_schedule(self):
        self.assertEqual(to_units(block_reward(0)), 14_000)
        self.assertEqual(to_units(block_reward(10_000)), 14_000)
        self.assertEqual(to_units(block_reward(10_001)), 7_000)
        self.assertEqual(to_units(block_reward(110_000)), 7_000)
        self.assertEqual(to_units(block_reward(110_001)), 3_500)
        self.assertEqual(to_units(block_reward(210_001)), 1_750)
        self.assertEqual(mine_halving_band(0), -1)
        self.assertEqual(mine_halving_band(10_000), -1)
        self.assertEqual(mine_halving_band(10_001), 0)
        self.assertEqual(mine_halving_band(110_001), 1)

    def test_memecoins_retired(self):
        state = _fresh_state()
        self.assertTrue(state.get("l2_retired"))
        self.assertEqual(state.get("l2_assets"), {})
        self.assertFalse(maybe_grant_operator_l2(state))
        self.assertFalse(maybe_l2_halving_airdrop(state))
        for asset_id in range(1, 10):
            self.assertEqual(asset_units(account(state, L2_OPERATOR_GRANT_ADDR), str(asset_id)), 0)

    def test_active_burn(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        account(state, addr)["0"] = to_units(10)
        raw = _signed(state, pk, sk, addr, "burn", amount=to_units(3))
        commit_signed(state, raw)
        self.assertEqual(int(account(state, addr)["0"]), to_units(7))
        self.assertGreaterEqual(int(state.get("burned_brah") or 0), to_units(3))
        with self.assertRaises(ValueError):
            commit_signed(state, _signed(state, pk, sk, addr, "launch_l2", memo="Nope|NOPE"))

    def test_first_mine_bonus_once(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        save_state(state)
        self.assertEqual(mine_payout_units(state, addr), to_units(1.00014))
        raw = _signed(state, pk, sk, addr, "mine")
        _tx, block = commit_signed(state, raw)
        self.assertEqual(int(block["reward"]), to_units(1.00014))
        self.assertEqual(int(account(state, addr)["0"]), to_units(1.00014))
        self.assertTrue((state.get("first_mined") or {}).get(addr))
        self.assertEqual(mine_payout_units(state, addr), to_units(0.00014))
        raw2 = _signed(state, pk, sk, addr, "mine")
        _tx2, block2 = commit_signed(state, raw2)
        self.assertEqual(int(block2["reward"]), to_units(0.00014))
        self.assertEqual(int(account(state, addr)["0"]), to_units(1.00028))

    def test_mesh_ingest_mine_block(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        raw = _signed(state, pk, sk, addr, "mine")
        reward = mine_payout_units(state, addr)
        parsed = parse_signed_tx(state, raw)
        root = tx_root([parsed])
        header = f"{state['chain_height']}|{state['tip_hash']}|{addr}|{reward}|{root}"
        nonce = int(raw["pow_nonce"])
        block = {
            "index": int(state["chain_height"]) + 1,
            "previous_hash": state["tip_hash"],
            "miner": addr,
            "nonce": nonce,
            "hash": sha256_hex(f"{header}:{nonce}"),
            "tx_root": root,
            "txs": [raw],
            "reward": reward,
            "pow_bits": int(state["pow_bits"]),
        }
        ingest_block(block)
        disk = load_or_init()
        self.assertEqual(int(disk["chain_height"]), 1)
        self.assertEqual(int(account(disk, addr)["0"]), reward)

    def test_supply_cap_is_100m(self):
        state = _fresh_state()
        self.assertEqual(total_supply_units(), 100_000_000 * UNITS_PER_BRAH)
        self.assertLessEqual(brah_outstanding_units(state), total_supply_units())

    def test_mesh_transaction_seals(self):
        state = _fresh_state()
        pk, sk, addr = _key()
        raw = _signed(state, pk, sk, addr, "mine")
        self.assertTrue(ingest_transaction(raw))
        disk = load_or_init()
        self.assertEqual(int(disk["chain_height"]), 1)
        self.assertGreater(int(account(disk, addr)["0"]), 0)

    def test_heavier_sibling_replaces_tip(self):
        _fresh_state()
        pk1, sk1, a1 = _key()
        pk2, sk2, a2 = _key()
        state = load_or_init()
        bits = int(state["pow_bits"])
        genesis_tip = state["tip_hash"]
        raw1 = _signed(state, pk1, sk1, a1, "mine")
        parsed1 = parse_signed_tx(state, raw1)
        reward1 = mine_payout_units(state, a1)
        root1 = tx_root([parsed1])
        header1 = f"0|{genesis_tip}|{a1}|{reward1}|{root1}"
        n1 = int(raw1["pow_nonce"])
        h1 = sha256_hex(f"{header1}:{n1}")
        block1 = {
            "index": 1,
            "previous_hash": genesis_tip,
            "miner": a1,
            "nonce": n1,
            "hash": h1,
            "tx_root": root1,
            "txs": [raw1],
            "reward": reward1,
            "pow_bits": bits,
        }
        ingest_block(block1)
        self.assertEqual(int(load_or_init()["chain_height"]), 1)

        nonce2 = int(account(state, a2).get("nonce", 0))
        from crypto_mldsa import canonical_message, sign_dilithium3
        msg = canonical_message(int(state["chain_id"]), nonce2, "mine", a2, "", "0", 0, "")
        sig = sign_dilithium3(sk2, msg)
        raw2 = {
            "kind": "mine",
            "from": a2,
            "to": "",
            "asset": "0",
            "amount_units": "0",
            "memo": "",
            "nonce": nonce2,
            "public_key": pk2.hex(),
            "signature": sig.hex(),
            "algo": "Dilithium3",
            "pow_nonce": 0,
        }
        parsed2 = parse_signed_tx(state, raw2)
        reward2 = mine_payout_units(state, a2)
        root2 = tx_root([parsed2])
        header2 = f"0|{genesis_tip}|{a2}|{reward2}|{root2}"
        n2, _ = mine_nonce(header2, bits)
        h2 = sha256_hex(f"{header2}:{n2}")
        guard = 0
        while guard < 2_000_000 and not (meets_pow(header2, n2, bits) and h2 < h1):
            n2 += 1
            guard += 1
            h2 = sha256_hex(f"{header2}:{n2}")
        self.assertTrue(meets_pow(header2, n2, bits))
        self.assertLess(h2, h1)
        raw2["pow_nonce"] = n2
        block2 = {
            "index": 1,
            "previous_hash": genesis_tip,
            "miner": a2,
            "nonce": n2,
            "hash": h2,
            "tx_root": root2,
            "txs": [raw2],
            "reward": reward2,
            "pow_bits": bits,
        }
        ingest_block(block2)
        disk = load_or_init()
        self.assertEqual(int(disk["chain_height"]), 1)
        self.assertEqual(str(disk["tip_hash"]), h2)
        self.assertEqual(int(account(disk, a2)["0"]), reward2)
        self.assertEqual(int(account(disk, a1)["0"]), 0)

    def test_mainnet_ready_needs_peer_and_named_host(self):
        report = mainnet_ready_report()
        self.assertFalse(report["public_launch"])
        self.assertFalse(report["mainnet_ready"])
        self.assertTrue(report["protocol_locked"])


if __name__ == "__main__":
    unittest.main()
