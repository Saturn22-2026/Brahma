"""Sign a BRAHMNETWORK L1 tx with a key from genesis_secrets.json (operator tool)."""
from __future__ import annotations

import argparse
import json
import sys

from crypto_mldsa import canonical_message, sign_dilithium3
from generate_genesis_keys import secrets_path
from l1_mainnet import account, load_or_init, mine_nonce, sha256_hex


def _row(data: dict, label: str) -> dict:
    row = next((k for k in data["keys"] if k["label"] == label), None)
    if row is None:
        raise SystemExit(f"unknown label {label}")
    return row


def main() -> int:
    parser = argparse.ArgumentParser(description="Sign a Dilithium3 Brahma Coin tx from genesis_secrets.json")
    parser.add_argument("--label", required=True, help="Key label, e.g. TREASURY_FOUNDER")
    parser.add_argument("--kind", default="send")
    parser.add_argument("--to", default="")
    parser.add_argument("--amount-units", type=int, default=0)
    parser.add_argument("--memo", default="")
    parser.add_argument("--asset", default="0")
    parser.add_argument(
        "--cosign-label",
        default="",
        help="Second key for donate (TREASURY_EMPOWER)",
    )
    args = parser.parse_args()
    sec = secrets_path()
    if not sec.exists():
        print("missing genesis_secrets.json — run generate_genesis_keys.py", file=sys.stderr)
        return 1
    data = json.loads(sec.read_text(encoding="utf-8"))
    row = _row(data, args.label)
    if args.kind == "donate" and not args.cosign_label:
        print("donate requires --cosign-label TREASURY_EMPOWER", file=sys.stderr)
        return 1
    state = load_or_init()
    addr = row["address"]
    nonce = int(account(state, addr).get("nonce", 0))
    msg = canonical_message(
        int(state["chain_id"]), nonce, args.kind, addr, args.to, args.asset, args.amount_units, args.memo
    )
    sig = sign_dilithium3(bytes.fromhex(row["secret_key"]), msg)
    tx_hash = sha256_hex(msg.decode("utf-8"))
    root = sha256_hex(tx_hash)
    reward = int(state["block_reward"]) if args.kind == "mine" else 0
    header = f"{state['chain_height']}|{state['tip_hash']}|{addr}|{reward}|{root}"
    pow_nonce, _ = mine_nonce(header, int(state["pow_bits"]))
    out = {
        "kind": args.kind,
        "from": addr,
        "to": args.to,
        "asset": args.asset,
        "amount_units": str(args.amount_units),
        "memo": args.memo,
        "nonce": nonce,
        "public_key": row["public_key"],
        "signature": sig.hex(),
        "algo": "Dilithium3",
        "pow_nonce": pow_nonce,
    }
    if args.kind == "donate":
        crow = _row(data, args.cosign_label)
        out["cosign_public_key"] = crow["public_key"]
        out["cosign_signature"] = sign_dilithium3(bytes.fromhex(crow["secret_key"]), msg).hex()
    print(json.dumps(out, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
