"""Wallet signatures are BouncyCastle Dilithium3 (3309 bytes), not dilithium-py (3293)."""
from __future__ import annotations

import unittest
from pathlib import Path

from dilithium_py.dilithium import Dilithium3

from crypto_mldsa import (
    address_from_pubkey,
    canonical_message,
    matching_canonical_message,
    verify_dilithium3,
)


ROOT = Path(__file__).resolve().parent
VECTOR = ROOT / "testdata" / "bc_dilithium3_mine.txt"


def _load_vector():
    msg, pk, sig = VECTOR.read_text(encoding="ascii").splitlines()
    return msg.encode("utf-8"), bytes.fromhex(pk), bytes.fromhex(sig)


class BcSignatureTests(unittest.TestCase):
    def test_bouncy_castle_mine_signature_verifies(self):
        msg, pk, sig = _load_vector()
        self.assertEqual(len(pk), 1952)
        self.assertEqual(len(sig), 3309)
        self.assertTrue(verify_dilithium3(pk, msg, sig))
        broken = bytearray(sig)
        broken[80] ^= 0x01
        self.assertFalse(verify_dilithium3(pk, msg, bytes(broken)))
        self.assertFalse(verify_dilithium3(pk, msg + b"x", sig))

    def test_canonical_mine_message_accepts_wallet_signature(self):
        msg, pk, sig = _load_vector()
        text = msg.decode("utf-8")
        addr = text.split("|")[4]
        self.assertEqual(addr, address_from_pubkey(pk))
        got = matching_canonical_message(
            2026,
            0,
            "mine",
            addr,
            "",
            "0",
            0,
            "",
            public_key_hex=pk.hex(),
            signature_hex=sig.hex(),
            require_sig=True,
        )
        self.assertEqual(got, msg)

    def test_public_key_must_match_from_address(self):
        _msg, pk, sig = _load_vector()
        other_pk, _sk = Dilithium3.keygen()
        other = address_from_pubkey(other_pk)
        with self.assertRaises(ValueError) as ctx:
            matching_canonical_message(
                2026,
                0,
                "mine",
                other,
                "",
                "0",
                0,
                "",
                public_key_hex=pk.hex(),
                signature_hex=sig.hex(),
                require_sig=True,
            )
        self.assertIn("Public key does not match", str(ctx.exception))

    def test_dilithium_py_signatures_still_verify(self):
        pk, sk = Dilithium3.keygen()
        addr = address_from_pubkey(pk)
        msg = canonical_message(2026, 1, "mine", addr, "", "0", 0, "")
        sig = Dilithium3.sign(sk, msg)
        self.assertEqual(len(sig), 3293)
        self.assertTrue(verify_dilithium3(pk, msg, sig))
        got = matching_canonical_message(
            2026,
            1,
            "mine",
            addr,
            "",
            "0",
            0,
            "",
            public_key_hex=pk.hex(),
            signature_hex=sig.hex(),
            require_sig=True,
        )
        self.assertEqual(got, msg)


if __name__ == "__main__":
    unittest.main()
