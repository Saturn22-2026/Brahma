"""Generate real Dilithium3 keys for founder / empower / strategic / 50 + council.

Secrets stay in genesis_secrets.json (gitignored). Addresses go in genesis_accounts.json.
"""
from __future__ import annotations

from envutil import env

ROOT = Path(__file__).resolve().parent


def _data_dir() -> Path:
    return Path(env("DATA_DIR") or str(ROOT))


def accounts_path() -> Path:
    return _data_dir() / "genesis_accounts.json"


def secrets_path() -> Path:
    return _data_dir() / "genesis_secrets.json"


# Back-compat names (resolved at import; tests should call accounts_path()).
ACCOUNTS_PATH = ROOT / "genesis_accounts.json"
SECRETS_PATH = ROOT / "genesis_secrets.json"


def _one(label: str) -> dict:
    if Dilithium3 is None:
        raise RuntimeError("pip install dilithium-py")
    pk, sk = Dilithium3.keygen()
    addr = address_from_pubkey(pk)
    return {
        "label": label,
        "address": addr,
        "public_key": pk.hex(),
        "secret_key": sk.hex(),
        "entropy": secrets.token_hex(32),
    }


def generate(force: bool = False) -> dict:
    acc = accounts_path()
    sec = secrets_path()
    if acc.exists() and not force:
        with open(acc, "r", encoding="utf-8") as fh:
            return json.load(fh)
    rows = [
        _one("TREASURY_FOUNDER"),
        _one("TREASURY_EMPOWER"),
        _one("TREASURY_STRATEGIC"),
        _one("COUNCIL"),
    ]
    for i in range(50):
        rows.append(_one(f"DIST_{i:02d}"))
    secrets_out = {
        "algo": "Dilithium3",
        "warning": "Operator-held genesis keys. Not a wallet mnemonic. Keep offline.",
        "keys": rows,
    }
    public = {
        "algo": "Dilithium3",
        "founder": rows[0]["address"],
        "empower": rows[1]["address"],
        "strategic": rows[2]["address"],
        "council": rows[3]["address"],
        "distribution": [r["address"] for r in rows[4:]],
        "public_keys": {r["label"]: r["public_key"] for r in rows},
    }
    acc.parent.mkdir(parents=True, exist_ok=True)
    with open(sec, "w", encoding="utf-8") as fh:
        json.dump(secrets_out, fh, indent=2)
    with open(acc, "w", encoding="utf-8") as fh:
        json.dump(public, fh, indent=2)
    return public


if __name__ == "__main__":
    acc = generate()
    print("wrote", accounts_path())
    print("founder", acc["founder"])
    print("council", acc["council"])
    print("distribution", len(acc["distribution"]))
