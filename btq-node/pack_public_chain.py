"""Copy the public chain files. Stops if a secrets file would be published."""
from __future__ import annotations

import json
import sys
from pathlib import Path

PUBLIC_FILES = ("l1_chain.jsonl", "l1_state.json")
SECRET_NAMES = {
    "genesis_secrets.json",
    "secrets.json",
    "node_identity.json",
}
SECRET_KEYS = {"secret", "seed", "private_key", "mnemonic"}


def _bad_key(obj) -> str:
    if isinstance(obj, dict):
        for key, value in obj.items():
            if str(key).lower() in SECRET_KEYS:
                return str(key)
            found = _bad_key(value)
            if found:
                return found
    elif isinstance(obj, list):
        for item in obj:
            found = _bad_key(item)
            if found:
                return found
    return ""


def pack(source: Path, dest: Path) -> None:
    source = source.resolve()
    dest = dest.resolve()
    for path in source.rglob("*"):
        if path.is_file() and path.name.lower() in SECRET_NAMES:
            raise SystemExit(f"refusing to publish {path.name} from {source}")
    dest.mkdir(parents=True, exist_ok=True)
    for name in PUBLIC_FILES:
        src = source / name
        if not src.is_file():
            raise SystemExit(f"missing {src}")
        if name.endswith(".json"):
            found = _bad_key(json.loads(src.read_text(encoding="utf-8")))
            if found:
                raise SystemExit(f"refusing to publish {src.name}: contains {found}")
        (dest / name).write_bytes(src.read_bytes())
    print(f"packed {' '.join(PUBLIC_FILES)} -> {dest}")


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: pack_public_chain.py <data-dir> <output-dir>")
    pack(Path(sys.argv[1]), Path(sys.argv[2]))


if __name__ == "__main__":
    main()
