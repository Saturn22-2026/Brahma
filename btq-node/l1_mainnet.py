"""
BRAHMNETWORK L1 — canonical ledger for Brahma Coin (not the Solidity token, not the unfinished Rust node).

Rules enforced here:
- Spends require Dilithium3 (wallet ML-DSA-65 label) over a canonical message
- POOL_* protocol organs cannot be used as a user `from`
- Balances are integer base units (1 Brahma Coin = 100_000_000)
- Every accepted tx is sealed into a block (the chain is the record)
- Mesh gossips blocks; unsigned packets do not move coins
"""
from __future__ import annotations

import hashlib
import html
import ipaddress
import json
import os
import sys
import threading
import time
import ssl
from collections import deque
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from socketserver import ThreadingMixIn
from urllib.parse import parse_qs, urlparse

from envutil import env

from ai_council import (
    FIRST_MINE_BONUS,
    L2_BOOTSTRAP_IDS,
    L2_HALVING_AIRDROP,
    L2_OPERATOR_GRANT,
    L2_OPERATOR_GRANT_ADDR,
    block_reward,
    donations_unlocked,
    mine_halving_band,
    reward_rates,
    whale_tax_units,
)
from crypto_mldsa import (
    ADDR_PREFIXES,
    addresses_equal,
    canonical_message,
    checksum_address,
    is_brah_address,
    load_or_create_identity,
    matching_canonical_message,
    normalize_address,
    sibling_address,
    verify_invite,
    verify_spend,
)
from generate_genesis_keys import accounts_path
from p2p import PeerNet
from units import UNITS_PER_BRAH, from_units, from_units_float, to_units

L2_BOOTSTRAP = [
    (1, "Homie", "HOMIE"),
    (2, "SlumDog", "SLUM"),
    (3, "Crazy God", "CRAZY"),
    (4, "BoujieClique", "BOUJIE"),
    (5, "QuarterMile", "QMILE"),
    (6, "SoldiersOfFortune", "SOF"),
    (7, "5thAvenue", "5AVE"),
    (8, "Pookie", "POOKIE"),
]

MAX_RPC_BODY = 256 * 1024
MAX_MEMO_LEN = 256

PROTOCOL_POOLS = frozenset({
    "POOL_MINING",
    "POOL_AIRDROP",
    "POOL_DONATION",
})

ROOT = Path(__file__).resolve().parent
GENESIS_PATH = ROOT / "genesis.json"
DATA = Path(env("DATA_DIR") or str(ROOT))
STATE_PATH = DATA / "l1_state.json"
CHAIN_PATH = DATA / "l1_chain.jsonl"


def refresh_paths() -> None:
    global DATA, STATE_PATH, CHAIN_PATH
    DATA = Path(env("DATA_DIR") or str(ROOT))
    if env("DATA_DIR"):
        DATA.mkdir(parents=True, exist_ok=True)
    STATE_PATH = DATA / "l1_state.json"
    CHAIN_PATH = DATA / "l1_chain.jsonl"


def rpc_port() -> int:
    raw = env("RPC_PORT", "8545").strip() or "8545"
    try:
        port = int(raw)
    except ValueError as exc:
        raise RuntimeError("BRAH_RPC_PORT must be an integer") from exc
    if not (1 <= port <= 65535):
        raise RuntimeError("BRAH_RPC_PORT must be in 1..65535")
    return port


def persist_public_rpc() -> str:
    """Write BRAH_PUBLIC_RPC (or legacy BTQ_PUBLIC_RPC) to data/public_rpc.txt when set."""
    dest = DATA / "public_rpc.txt"
    url = (env("PUBLIC_RPC") or "").strip()
    if url:
        if not _usable_public_rpc(url):
            raise RuntimeError(
                "BRAH_PUBLIC_RPC must be an https hostname that forwards to this node, "
                "not 127.0.0.1, a Cloudflare dashboard URL, that-host, or dRPC. "
                "http://127.0.0.1:8545 is the tunnel origin, not the public URL."
            )
        dest.write_text(url.rstrip("/") + "\n", encoding="utf-8")
        return url.rstrip("/")
    return public_rpc_url()


def _usable_public_rpc(url: str) -> bool:
    raw = (url or "").strip().rstrip("/")
    low = raw.lower()
    if not raw:
        return False
    if "dash.cloudflare.com" in low or "that-host" in low:
        return False
    if low.startswith("https://lb.drpc.") or "/lambda/" in low:
        return False
    if not low.startswith("https://"):
        return False
    host = (urlparse(raw).hostname or "").lower()
    if not host:
        return False
    if host in ("127.0.0.1", "localhost", "::1"):
        return False
    if host.endswith(".trycloudflare.com"):
        return True
    try:
        ip = ipaddress.ip_address(host)
        if ip.is_loopback or ip.is_private or ip.is_link_local:
            return False
    except ValueError:
        pass
    return True


def learn_public_host_from_headers(headers) -> str:
    """If Cloudflare (or another proxy) hits localhost with a real Host, remember it."""
    raw = (headers.get("X-Forwarded-Host") or headers.get("Host") or "").split(",")[0].strip()
    hostname = raw.split("/")[0].split(":")[0].strip().lower()
    if not hostname:
        return ""
    if hostname in ("127.0.0.1", "localhost", "::1") or hostname.startswith(("192.168.", "10.")):
        return ""
    if hostname.endswith(".trycloudflare.com"):
        return ""
    url = f"https://{hostname}"
    if not _usable_public_rpc(url):
        return ""
    current = public_rpc_url()
    if current and _usable_public_rpc(current) and "trycloudflare.com" not in current.lower():
        return current
    dest = DATA / "public_rpc.txt"
    dest.write_text(url + "\n", encoding="utf-8")
    print(f"learned public_rpc={url}")
    return url


def public_rpc_url() -> str:
    url = (env("PUBLIC_RPC") or "").strip().rstrip("/")
    if url and _usable_public_rpc(url):
        return url
    dest = DATA / "public_rpc.txt"
    if dest.exists() and dest.stat().st_size:
        got = dest.read_text(encoding="utf-8").strip().splitlines()[0].strip().rstrip("/")
        if _usable_public_rpc(got):
            return got
    return ""


def allow_cors_origin(origin: str) -> str:
    origin = (origin or "").strip()
    if not origin:
        return ""
    if origin.startswith(
        ("http://127.0.0.1", "http://localhost", "https://127.0.0.1", "https://localhost")
    ):
        return origin
    extra = (env("CORS_ORIGIN") or "").strip().rstrip("/")
    if extra and origin.rstrip("/") == extra:
        return origin
    pub = public_rpc_url()
    if pub and origin.rstrip("/") == pub:
        return origin
    return ""


def explorer_allowed(ip: str) -> bool:
    if ip in ("127.0.0.1", "::1", "localhost"):
        return True
    parsed = _rpc_ip(ip)
    if parsed is not None and parsed.is_loopback:
        return True
    return env("EXPLORER") == "1"


def resolve_apk() -> Path | None:
    apk_root = ROOT.parent / "wallet-android" / "app" / "build" / "outputs" / "apk"
    for candidate in (
        DATA / "wallet.apk",
        ROOT / "wallet.apk",
        apk_root / "release" / "app-release.apk",
        apk_root / "debug" / "app-debug.apk",
    ):
        if candidate.exists():
            return candidate
    return None


def apk_download_allowed() -> bool:
    return env("SERVE_APK") == "1"


def home_html() -> str:
    path = ROOT / "static" / "index.html"
    try:
        return path.read_text(encoding="utf-8")
    except OSError:
        return claim_html()


def static_asset(rel: str) -> tuple[bytes | None, str]:
    name = Path(rel).name
    if not name or name != rel.replace("\\", "/").split("/")[-1]:
        return None, ""
    if name != rel:
        return None, ""
    path = (ROOT / "static" / name).resolve()
    root = (ROOT / "static").resolve()
    if path.parent != root or not path.is_file():
        return None, ""
    suffix = path.suffix.lower()
    types = {
        ".js": "application/javascript; charset=utf-8",
        ".css": "text/css; charset=utf-8",
        ".html": "text/html; charset=utf-8",
        ".svg": "image/svg+xml",
    }
    ctype = types.get(suffix)
    if ctype is None:
        return None, ""
    return path.read_bytes(), ctype


def claim_html(ref_raw: str = "") -> str:
    ref_block = "<p>No referral address in this link.</p>"
    raw = (ref_raw or "").strip()
    if raw:
        try:
            addr = normalize_address(raw)
            ref_block = (
                "<p>Verified referrer (checksummed BRM1G): "
                f"<code>{html.escape(addr)}</code></p>"
            )
        except ValueError:
            ref_block = (
                "<p>Referral is not a valid checksummed Brahma Coin address. "
                "Do not trust this invite.</p>"
            )
    apk = resolve_apk() if apk_download_allowed() else None
    open_wallet = ""
    if raw:
        try:
            addr = normalize_address(raw)
            open_wallet = (
                f'<p><a href="brahm://claim?ref={html.escape(addr)}">'
                "Open in Brahma Coin wallet (saves this referrer)</a> · "
                "</p>"
            )
        except ValueError:
            open_wallet = ""
    if apk is not None:
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        dl_block = (
            "<ol>"
            "<li><a href=\"/wallet.apk\">Download the Brahma Coin wallet APK</a> "
            "and allow install from this browser.</li>"
            "<li>Create a wallet in the app (or tap Open in Brahma Coin wallet first).</li>"
            "<li>Tap Claim adoption, then Mine. A mine pays only after the node seals it.</li>"
            "</ol>"
            f"<p>SHA-256: <code>{html.escape(digest)}</code></p>"
        )
    elif apk_download_allowed():
        dl_block = "<p>This node is allowed to serve the wallet, but no APK file was found.</p>"
    else:
        dl_block = (
            "<p>Wallet download is off. Operator: build the APK and set "
            "<code>BRAH_SERVE_APK=1</code>.</p>"
        )
    height, joined, mined = "—", "—", "—"
    try:
        with _lock:
            state = load_or_init()
            height = str(int(state["chain_height"]))
            joined = str(int(state.get("joined_count", 0)))
            mined = from_units(int(state["total_mined"]))
    except Exception:
        pass
    pub = html.escape(public_rpc_url() or "this host")
    return (
        "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>"
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>"
        "<title>BRAHMNETWORK</title>"
        "<style>body{font-family:sans-serif;background:#000;color:#fff;margin:24px;max-width:42rem}"
        "a{color:#00FF88}code{color:#00FF88;word-break:break-all}li{margin:10px 0}"
        ".muted{color:#9a9a9a}nav a{margin-right:1rem}</style>"
        "</head><body>"
        "<h1>BRAHMNETWORK</h1>"
        "<p>Private post-quantum testnet. Canonical ledger is this Python node. "
        "Not Bitcoin, not Ethereum, not a public mainnet. Coin: Brahma Coin.</p>"
        f"<p>Height <code>{html.escape(height)}</code> · joined <code>{html.escape(joined)}</code> · "
        f"mined <code>{html.escape(mined)}</code> Brahma Coin</p>"
        "<p class=\"muted\">Balances are Brahma Coin units (1 Brahma Coin = 100,000,000). "
        "Dilithium3 spends. <code>public_launch</code> is false.</p>"
        "<nav><a href=\"/\">Home</a><a href=\"/claim\">Claim</a>"
        "<a href=\"/explorer\">Explorer</a><a href=\"/rpc\">RPC</a></nav>"
        "<h2>JSON-RPC</h2>"
        "<p>POST JSON-RPC to <code>/</code> on this same host. "
        f"Public URL: <code>{pub}</code></p>"
        "<p class=\"muted\">Methods: <code>brah_submitTx</code>, <code>brah_getAccount</code>, "
        "<code>brah_getStats</code>, <code>brah_getBlocks</code>, <code>eth_getBalance</code>. "
        "Not an Ethereum RPC provider.</p>"
        f"{ref_block}{dl_block}{open_wallet}"
        "</body></html>"
    )

_lock = threading.Lock()
_peer_net: PeerNet | None = None
_rpc_hits_lock = threading.Lock()
_rpc_hits: dict[str, deque] = {}


class ThreadingL1Server(ThreadingMixIn, HTTPServer):
    daemon_threads = True
    allow_reuse_address = True
    timeout = 15
    request_queue_size = 16


def _rpc_ip(ip: str):
    try:
        return ipaddress.ip_address(ip.split("%")[0])
    except ValueError:
        return None


def rpc_allow(ip: str) -> bool:
    if ip in ("127.0.0.1", "::1", "localhost"):
        return True
    parsed = _rpc_ip(ip)
    if parsed is not None and parsed.is_loopback:
        return True
    # Phone may be on Public Wi-Fi, guest LAN, or a routed path. Do not 429 it.
    if env("ALLOW_LAN") == "1" and parsed is not None:
        return True
    now = time.time()
    with _rpc_hits_lock:
        q = _rpc_hits.setdefault(ip, deque())
        while q and now - q[0] > 10.0:
            q.popleft()
        if len(q) >= 30:
            return False
        q.append(now)
        return True


def sha256_hex(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def load_genesis() -> dict:
    with open(GENESIS_PATH, "r", encoding="utf-8") as fh:
        return json.load(fh)


def load_keyed_accounts() -> dict:
    path = accounts_path()
    if not path.exists():
        raise RuntimeError(
            "genesis_accounts.json missing. Run python generate_genesis_keys.py, "
            "then move genesis_secrets.json offline."
        )
    with open(path, "r", encoding="utf-8") as fh:
        data = json.load(fh)
    out = {}
    for key, value in data.items():
        if isinstance(value, str) and is_brah_address(value):
            try:
                out[key] = normalize_address(value)
            except ValueError:
                out[key] = value
        elif isinstance(value, list):
            mapped = []
            for item in value:
                if isinstance(item, str) and is_brah_address(item):
                    try:
                        mapped.append(normalize_address(item))
                    except ValueError:
                        mapped.append(item)
                else:
                    mapped.append(item)
            out[key] = mapped
        else:
            out[key] = value
    return out


def is_protocol(addr: str) -> bool:
    if addr in PROTOCOL_POOLS:
        return True
    if addr.startswith("POOL_") or addr.startswith("GENESIS_"):
        return True
    return False


def circulating(state: dict) -> int:
    skip = set(PROTOCOL_POOLS)
    total = 0
    for name, acct in state["balances"].items():
        if name in skip:
            continue
        total += int(acct.get("0", 0))
    return total


def empty_account() -> dict:
    acct = {"0": 0, "nonce": 0}
    for i in range(1, 9):
        acct[str(i)] = 0
    return acct


def flag_for(blob: dict, addr: str) -> bool:
    if not addr or not isinstance(blob, dict):
        return False
    if blob.get(addr):
        return True
    try:
        keyed = normalize_address(addr)
    except ValueError:
        return False
    if blob.get(keyed):
        return True
    try:
        return bool(blob.get(sibling_address(keyed)))
    except ValueError:
        return False


def migrate_address_map(blob, *, merge_accounts: bool = False):
    if not isinstance(blob, dict):
        return blob
    out = {}
    for name, value in blob.items():
        key = str(name)
        if is_protocol(key) or not is_brah_address(key):
            out[key] = value
            continue
        try:
            keyed = normalize_address(key)
        except ValueError:
            out[key] = value
            continue
        if keyed in out and merge_accounts and isinstance(out[keyed], dict) and isinstance(value, dict):
            merged = dict(out[keyed])
            for field, amount in value.items():
                if field == "nonce":
                    merged["nonce"] = max(int(merged.get("nonce") or 0), int(amount or 0))
                elif str(field).isdigit():
                    merged[str(field)] = int(merged.get(str(field)) or 0) + int(amount or 0)
                else:
                    merged[field] = amount
            out[keyed] = merged
        elif keyed not in out:
            out[keyed] = value
        elif value:
            out[keyed] = out[keyed] or value
    return out


def migrate_brand_state(state: dict) -> dict:
    if "units_per_brah" not in state and "units_per_btq" in state:
        state["units_per_brah"] = state.get("units_per_btq")
    if "burned_brah" not in state and "burned_btq" in state:
        state["burned_brah"] = state.get("burned_btq")
    state["balances"] = migrate_address_map(state.get("balances") or {}, merge_accounts=True)
    for field in ("claimed_rewards", "first_mined", "referrals"):
        state[field] = migrate_address_map(state.get(field) or {})
    return state


def resolve_balance_key(state: dict, addr: str) -> str:
    """Canonical BRM1G bucket; still finds a leftover sibling key."""
    if is_protocol(addr) or not is_brah_address(addr):
        return addr
    try:
        keyed = normalize_address(addr)
    except ValueError:
        return addr
    balances = state["balances"]
    if keyed in balances:
        return keyed
    body = keyed[5:29]
    for prefix in ADDR_PREFIXES:
        bare = prefix + body
        if bare in balances:
            return bare
        try:
            checked = normalize_address(bare)
        except ValueError:
            continue
        if checked in balances:
            return checked
    try:
        sib = sibling_address(keyed)
    except ValueError:
        return keyed
    if sib in balances:
        return sib
    return keyed


def account(state: dict, addr: str, *, create: bool = True) -> dict:
    balances = state["balances"]
    key = resolve_balance_key(state, addr)
    if key not in balances:
        if not create:
            return empty_account()
        balances[key] = empty_account()
    acct = balances[key]
    acct.pop("qusd", None)
    for key in ("0", "nonce"):
        if key not in acct:
            acct[key] = 0
        else:
            acct[key] = int(acct[key])
    return acct


def asset_units(acct: dict, asset_id: str) -> int:
    return int(acct.get(asset_id, 0))


def ensure_l2(state: dict) -> None:
    assets = state.setdefault("l2_assets", {})
    if not assets:
        for asset_id, name, symbol in L2_BOOTSTRAP:
            assets[str(asset_id)] = {
                "id": asset_id,
                "name": name,
                "symbol": symbol,
                "creator": "GENESIS",
                "burned": 0,
            }
        state["next_l2_id"] = 9
    state.setdefault("next_l2_id", 9)
    state.setdefault("burned_brah", 0)
    state.setdefault("applied_txs", {})
    state.setdefault("mempool", [])
    state.setdefault("units_per_brah", UNITS_PER_BRAH)
    state.setdefault("l2_operator_grant_done", False)
    state.setdefault("l2_halving_band_paid", -1)


def credit_bootstrap_l2(state: dict, addr: str, amount: int) -> None:
    acct = account(state, addr)
    qty = int(amount)
    if qty <= 0:
        return
    for asset_id in L2_BOOTSTRAP_IDS:
        acct[asset_id] = int(acct.get(asset_id, 0)) + qty


def active_l2_users(state: dict) -> list[str]:
    names: set[str] = set()
    for key in ("claimed_rewards", "first_mined"):
        blob = state.get(key) or {}
        if isinstance(blob, dict):
            names.update(str(name) for name in blob.keys())
    out: list[str] = []
    seen: set[str] = set()
    for addr in names:
        if not addr or is_protocol(addr) or not is_brah_address(addr):
            continue
        try:
            keyed = normalize_address(addr)
        except ValueError:
            continue
        body = keyed[5:29]
        if body in seen:
            continue
        seen.add(body)
        out.append(keyed)
    return out


def maybe_grant_operator_l2(state: dict) -> bool:
    """One-time operator-disk credit of 5B whole units of each genesis L2 coin."""
    ensure_l2(state)
    if state.get("l2_operator_grant_done"):
        return False
    credit_bootstrap_l2(state, L2_OPERATOR_GRANT_ADDR, L2_OPERATOR_GRANT)
    state["l2_operator_grant_done"] = True
    return True


def maybe_l2_halving_airdrop(state: dict) -> bool:
    """At each mine-schedule halving, credit 750M of each genesis L2 to joined/mined users."""
    ensure_l2(state)
    band = mine_halving_band(int(state.get("joined_count", 0)))
    last = int(state.get("l2_halving_band_paid", -1))
    if band <= last:
        return False
    users = active_l2_users(state)
    crossings = band - last
    payout = L2_HALVING_AIRDROP * crossings
    for addr in users:
        credit_bootstrap_l2(state, addr, payout)
    state["l2_halving_band_paid"] = band
    return True


def l2_burn_units() -> int:
    return to_units(load_genesis()["economics"].get("l2_launch_burn") or load_genesis()["economics"].get("l2_launch_burn_btq", 100))


def tx_hash(tx: dict) -> str:
    """Always the canonical message digest. Client-supplied hash is ignored."""
    msg = canonical_message(
        int(tx.get("chain_id") or 0),
        int(tx.get("nonce") or 0),
        str(tx.get("kind") or ""),
        str(tx.get("from") or ""),
        str(tx.get("to") or ""),
        str(tx.get("asset") or "0"),
        int(tx.get("amount") or 0),
        str(tx.get("memo") or ""),
    )
    return sha256_hex(msg.decode("utf-8"))


def tx_root(txs: list) -> str:
    if not txs:
        return sha256_hex("")
    return sha256_hex("|".join(tx_hash(t) for t in txs))


def meets_pow(header: str, nonce: int, bits: int) -> bool:
    digest = sha256_hex(f"{header}:{nonce}")
    return int(digest, 16) >> (256 - bits) == 0


def mine_nonce(header: str, bits: int, max_tries: int = 4_000_000) -> tuple[int, str]:
    for nonce in range(max_tries):
        if meets_pow(header, nonce, bits):
            return nonce, sha256_hex(f"{header}:{nonce}")
    raise RuntimeError("PoW search exhausted")


def block_header(state: dict, miner: str, txs: list, reward: int) -> tuple[str, str]:
    root = tx_root(txs)
    header = f"{state['chain_height']}|{state['tip_hash']}|{miner}|{reward}|{root}"
    return header, root


def maybe_retarget(state: dict) -> None:
    """Adjust bits toward ~8s blocks. Floor 18, ceiling 24. BRAHMNETWORK curve, not Bitcoin's."""
    height = int(state["chain_height"])
    window = 64
    if height == 0 or height % window != 0:
        return
    last_t = int(state.get("retarget_time", 0))
    now = int(time.time())
    if last_t <= 0:
        state["retarget_height"] = height
        state["retarget_time"] = now
        return
    elapsed = max(1, now - last_t)
    target = window * 8
    bits = int(state["pow_bits"])
    if elapsed < target // 2 and bits < 24:
        bits += 1
    elif elapsed > target * 2 and bits > 18:
        bits -= 1
    state["pow_bits"] = bits
    state["retarget_height"] = height
    state["retarget_time"] = now


def announce(kind: str, payload: dict) -> None:
    if _peer_net is not None:
        _peer_net.broadcast(kind, payload)


def total_supply_units() -> int:
    eco = load_genesis().get("economics") or {}
    return to_units(int(eco.get("total_supply") or 100_000_000))


def brah_outstanding_units(state: dict) -> int:
    total = int(state.get("burned_brah", 0) or 0)
    for acct in (state.get("balances") or {}).values():
        if isinstance(acct, dict):
            total += int(acct.get("0", 0) or 0)
    return total


def assert_supply_cap(state: dict) -> None:
    cap = total_supply_units()
    got = brah_outstanding_units(state)
    if got > cap:
        raise RuntimeError(f"Brahma Coin outstanding {got} exceeds 100M cap {cap}")


def save_state(state: dict) -> None:
    assert_supply_cap(state)
    tmp = STATE_PATH.with_suffix(".tmp")
    with open(tmp, "w", encoding="utf-8") as fh:
        json.dump(state, fh)
    tmp.replace(STATE_PATH)


def append_block(block: dict) -> None:
    with open(CHAIN_PATH, "a", encoding="utf-8") as fh:
        fh.write(json.dumps(block) + "\n")


def load_all_blocks() -> list:
    if not CHAIN_PATH.exists():
        return []
    out = []
    for line in CHAIN_PATH.read_text(encoding="utf-8").splitlines():
        if line.strip():
            out.append(json.loads(line))
    return out


def block_work(block: dict) -> int:
    bits = int(block.get("pow_bits") or 20)
    bits = max(1, min(bits, 62))
    return 1 << bits


def named_public_rpc() -> str:
    url = (public_rpc_url() or "").strip()
    if not url or "trycloudflare.com" in url.lower():
        return ""
    return url


def mainnet_ready_report() -> dict:
    peers = _peer_net.peer_count() if _peer_net is not None else 0
    named = named_public_rpc()
    ready = peers >= 1 and bool(named)
    launched = env("PUBLIC_LAUNCH") == "1" and ready
    return {
        "peers": peers,
        "named_public_rpc": bool(named),
        "protocol_locked": True,
        "mainnet_ready": ready,
        "public_launch": launched,
    }


def join_max_per_hour() -> int:
    try:
        return max(1, int(env("JOIN_MAX_PER_HOUR", "20") or "20"))
    except ValueError:
        return 20


def apply_rewards(state: dict, addr: str, referrer: str) -> str:
    claimed = state.setdefault("claimed_rewards", {})
    if claimed.get(addr):
        raise ValueError("Already claimed adoption reward")
    now = int(time.time())
    window = state.setdefault("join_hour", {"start": now, "count": 0})
    if now - int(window.get("start") or 0) >= 3600:
        window["start"] = now
        window["count"] = 0
    if int(window.get("count") or 0) >= join_max_per_hour():
        raise ValueError("Join rate limit")
    join_amt_coins, ref_amt_coins = reward_rates(int(state.get("joined_count", 0)))
    join_amt = to_units(join_amt_coins)
    ref_amt = to_units(ref_amt_coins)
    if join_amt <= 0:
        raise ValueError("Adoption rewards complete")
    ref = (referrer or "").strip()
    if ref and is_brah_address(ref) and not is_protocol(ref):
        ref = normalize_address(ref)
        pay_ref = ref_amt if ref != addr else 0
    else:
        pay_ref = 0
    total = join_amt + pay_ref
    pool = account(state, "POOL_AIRDROP")
    already = int(state.get("rewards_paid", 0))
    cap = to_units(25_000_000)
    if already + total > cap or int(pool["0"]) < total:
        raise ValueError("Rewards pool empty")
    pool["0"] = int(pool["0"]) - total
    acct = account(state, addr)
    acct["0"] = int(acct["0"]) + join_amt
    if pay_ref > 0:
        bonus = account(state, ref)
        bonus["0"] = int(bonus["0"]) + pay_ref
        state.setdefault("referrals", {})
        state["referrals"][ref] = int(state["referrals"].get(ref, 0)) + 1
    claimed[addr] = True
    window["count"] = int(window.get("count") or 0) + 1
    state["join_hour"] = window
    state["joined_count"] = int(state.get("joined_count", 0)) + 1
    state["rewards_paid"] = already + total
    maybe_l2_halving_airdrop(state)
    return f"Adoption reward {from_units(join_amt)} Brahma Coin" + (
        f" + {from_units(pay_ref)} to referrer" if pay_ref else ""
    )


def launch_l2(state: dict, creator: str, name: str, symbol: str) -> dict:
    ensure_l2(state)
    name = (name or "").strip()
    symbol = (symbol or "").strip().upper()
    if len(name) < 2 or len(symbol) < 2:
        raise ValueError("Name and symbol required")
    if not symbol.isalnum():
        raise ValueError("Symbol must be alphanumeric")
    for asset in state["l2_assets"].values():
        if str(asset.get("symbol", "")).upper() == symbol:
            raise ValueError("L2 symbol already exists")
    burn = l2_burn_units()
    acct = account(state, creator)
    if int(acct.get("0", 0)) < burn:
        raise ValueError(f"Need {from_units(burn)} Brahma Coin burn to launch an L2 memecoin")
    acct["0"] = int(acct["0"]) - burn
    state["burned_brah"] = int(state.get("burned_brah") or state.get("burned_btq") or 0) + burn
    asset_id = int(state.get("next_l2_id", 9))
    record = {
        "id": asset_id,
        "name": name,
        "symbol": symbol,
        "creator": creator,
        "burned": burn,
    }
    state["l2_assets"][str(asset_id)] = record
    state["next_l2_id"] = asset_id + 1
    return record


def apply_recorded_tx(state: dict, tx: dict) -> None:
    hid = tx_hash(tx)
    applied = state.setdefault("applied_txs", {})
    if applied.get(hid):
        return
    kind = str(tx.get("kind") or "")
    if kind == "send":
        sender = tx["from"]
        if is_protocol(sender):
            raise ValueError("Protocol organs cannot send")
        receiver = tx["to"]
        amount = int(tx["amount"])
        if amount <= 0:
            raise ValueError("amount must be positive")
        if not is_brah_address(receiver) or is_protocol(receiver):
            raise ValueError("invalid recipient")
        asset_id = str(tx.get("asset", "0"))
        src = account(state, sender)
        if asset_units(src, asset_id) < amount:
            raise ValueError("Insufficient balance")
        tax = whale_tax_units(amount, circulating(state)) if asset_id == "0" else 0
        src[asset_id] = asset_units(src, asset_id) - amount
        dst = account(state, receiver)
        dst[asset_id] = asset_units(dst, asset_id) + (amount - tax)
        if tax > 0:
            state["burned_brah"] = int(state.get("burned_brah") or state.get("burned_btq") or 0) + tax
    elif kind in ("join", "reward", "faucet"):
        apply_rewards(state, tx["from"], tx.get("memo") or "")
    elif kind == "donate":
        keyed = load_keyed_accounts()
        if not addresses_equal(tx["from"], str(keyed.get("council") or "")):
            raise ValueError("Only the council key may donate")
        if not donations_unlocked(int(state.get("donation_unlock", 0)), int(time.time())):
            raise ValueError("Donation lock (730 days) still active")
        amount = int(tx["amount"])
        if amount <= 0:
            raise ValueError("amount must be positive")
        if not is_brah_address(str(tx.get("to") or "")):
            raise ValueError("invalid recipient")
        pool = account(state, "POOL_DONATION")
        if int(pool["0"]) < amount:
            raise ValueError("Donation pool empty")
        pool["0"] = int(pool["0"]) - amount
        account(state, tx["to"])["0"] = int(account(state, tx["to"])["0"]) + amount
    elif kind == "mint_qusd":
        raise ValueError("QUSD removed")
    elif kind == "launch_l2":
        name, _, symbol = str(tx.get("memo") or "").partition("|")
        launch_l2(state, tx["from"], name, symbol)
    elif kind == "mine":
        pass
    else:
        raise ValueError(f"Unknown tx kind: {kind}")
    applied[hid] = True
    account(state, tx["from"])["nonce"] = int(account(state, tx["from"]).get("nonce", 0)) + 1


def parse_signed_tx(state: dict, raw: dict, *, verify: bool = True) -> dict:
    kind = str(raw.get("kind") or raw.get("type") or "send")
    from_addr = str(raw.get("from") or raw.get("sender") or raw.get("addr") or raw.get("creator") or "")
    to_addr = str(raw.get("to") or raw.get("receiver") or raw.get("recipient") or "")
    asset = str(raw.get("asset") or raw.get("asset_id") or "0")
    if asset.lower() in ("latest", "brah", "brahma", "brahma coin", "0"):
        asset = "0"
    memo = str(raw.get("memo") or raw.get("referrer") or "")
    if kind == "launch_l2" and not memo:
        memo = f"{raw.get('name', '')}|{raw.get('symbol', '')}"
    if kind in ("join", "faucet", "reward") and not memo:
        memo = str(raw.get("referrer") or "")
    amount_in = raw.get("amount_units", raw.get("amount", 0))
    if raw.get("amount_units") is None and kind != "mine":
        amount = to_units(amount_in)
    else:
        amount = int(amount_in or 0)
    if kind == "mine":
        amount = 0
        to_addr = ""
        asset = "0"
    if is_protocol(from_addr):
        raise ValueError("Protocol organs cannot originate a tx")
    if not is_brah_address(from_addr) or len(from_addr) != 33:
        raise ValueError("from must be a checksummed BRM1G address")
    signed_from = checksum_address(from_addr)
    from_addr = normalize_address(from_addr)
    if len(memo) > MAX_MEMO_LEN:
        raise ValueError("memo too long")
    signed_to = ""
    if kind in ("send", "donate"):
        if amount <= 0:
            raise ValueError("amount must be positive")
        if not is_brah_address(to_addr) or len(to_addr) != 33:
            raise ValueError("to must be a checksummed BRM1G address")
        if is_protocol(to_addr):
            raise ValueError("cannot send to a protocol organ")
        signed_to = checksum_address(to_addr)
        to_addr = normalize_address(to_addr)
    acct = account(state, from_addr)
    nonce = int(raw.get("nonce", acct.get("nonce", 0)))
    if nonce != int(acct.get("nonce", 0)):
        raise ValueError(f"Bad nonce (expected {acct.get('nonce', 0)})")
    msg = matching_canonical_message(
        int(state["chain_id"]),
        nonce,
        kind,
        signed_from,
        signed_to,
        asset,
        amount,
        memo,
        public_key_hex=str(raw.get("public_key") or ""),
        signature_hex=str(raw.get("signature") or ""),
        require_sig=verify,
    )
    if kind == "donate":
        keyed = load_keyed_accounts()
        if not addresses_equal(from_addr, str(keyed.get("council") or "")):
            raise ValueError("Only the council key may donate")
        empower = str(keyed.get("empower") or "")
        if not empower:
            raise ValueError("Empower key missing")
        if verify:
            verify_spend(
                checksum_address(empower) if is_brah_address(empower) else empower,
                str(raw.get("cosign_public_key") or ""),
                str(raw.get("cosign_signature") or ""),
                msg,
            )
    tx = {
        "kind": kind,
        "from": from_addr,
        "to": to_addr,
        "asset": asset,
        "amount": amount,
        "memo": memo,
        "nonce": nonce,
        "public_key": str(raw.get("public_key") or ""),
        "signature": str(raw.get("signature") or ""),
        "algo": "Dilithium3",
        "chain_id": int(state["chain_id"]),
    }
    if kind == "donate":
        tx["cosign_public_key"] = str(raw.get("cosign_public_key") or "")
        tx["cosign_signature"] = str(raw.get("cosign_signature") or "")
    tx["hash"] = sha256_hex(msg.decode("utf-8"))
    return tx


def mine_payout_units(state: dict, miner: str) -> int:
    payout = to_units(block_reward(int(state.get("joined_count", 0))))
    first = state.get("first_mined") or {}
    if miner and not flag_for(first, miner):
        payout += to_units(FIRST_MINE_BONUS)
    return payout


def mark_first_mined(state: dict, miner: str) -> None:
    if not miner:
        return
    key = resolve_balance_key(state, miner) if is_brah_address(miner) else miner
    state.setdefault("first_mined", {})[key] = True


def reset_year_if_needed(state: dict) -> None:
    now = int(time.time())
    if now >= int(state["year_start"]) + 365 * 86400:
        state["year_start"] = now
        state["mined_this_year"] = 0


def seal_block(state: dict, miner: str, txs: list, reward: int, pow_nonce: int) -> dict:
    reset_year_if_needed(state)
    if reward:
        if int(state["total_mined"]) + reward > int(state["mining_cap"]):
            raise ValueError("Mining reserve exhausted")
        if int(state["mined_this_year"]) + reward > int(state["yearly_cap"]):
            raise ValueError("Yearly mining cap reached")
        pool = account(state, "POOL_MINING")
        if int(pool["0"]) < reward:
            raise ValueError("Mining pool empty")
        pool["0"] = int(pool["0"]) - reward
        miner_acct = account(state, miner)
        miner_acct["0"] = int(miner_acct["0"]) + reward
        state["total_mined"] = int(state["total_mined"]) + reward
        state["mined_this_year"] = int(state["mined_this_year"]) + reward
        mark_first_mined(state, miner)
    header, root = block_header(state, miner, txs, reward)
    bits = int(state["pow_bits"])
    if not meets_pow(header, int(pow_nonce), bits):
        raise ValueError("PoW invalid — client must solve the header")
    block_hash = sha256_hex(f"{header}:{int(pow_nonce)}")
    state["chain_height"] = int(state["chain_height"]) + 1
    maybe_retarget(state)
    block = {
        "index": state["chain_height"],
        "timestamp": int(time.time()),
        "previous_hash": state["tip_hash"],
        "miner": miner,
        "nonce": int(pow_nonce),
        "hash": block_hash,
        "tx_root": root,
        "txs": txs,
        "reward": reward,
        "pow_bits": bits,
    }
    state["tip_hash"] = block_hash
    append_block(block)
    save_state(state)
    announce("BLOCK", block)
    announce("ROUTING_UPDATE", {"chain_height": state["chain_height"], "tip_hash": state["tip_hash"]})
    return block


def commit_signed(state: dict, raw: dict, *, verify: bool = True) -> tuple[dict, dict]:
    tx = parse_signed_tx(state, raw, verify=verify)
    reward = mine_payout_units(state, tx["from"]) if tx["kind"] == "mine" else 0
    if raw.get("pow_nonce") is None:
        raise ValueError("pow_nonce required (client PoW)")
    pow_nonce = int(raw["pow_nonce"])
    header, _root = block_header(state, tx["from"], [tx], reward)
    if not meets_pow(header, pow_nonce, int(state["pow_bits"])):
        raise ValueError("PoW invalid — client must solve the header")
    apply_recorded_tx(state, tx)
    block = seal_block(state, tx["from"], [tx], reward, pow_nonce)
    return tx, block


def load_blocks_from(start: int, limit: int = 64) -> list:
    start = max(0, int(start))
    limit = max(1, min(int(limit), 128))
    out = []
    for block in load_all_blocks():
        if int(block.get("index", -1)) >= start:
            out.append(block)
            if len(out) >= limit:
                break
    return out


def _apply_extension(state: dict, block: dict) -> bool:
    txs = list(block.get("txs") or [])
    if len(txs) != 1 or not isinstance(txs[0], dict):
        return False
    try:
        tx = parse_signed_tx(state, dict(txs[0]))
    except Exception:
        return False
    expected_reward = mine_payout_units(state, tx["from"]) if tx["kind"] == "mine" else 0
    reward = int(block.get("reward") or 0)
    miner = str(block.get("miner") or "")
    if reward != expected_reward or miner != tx["from"]:
        return False
    root = tx_root([tx])
    if str(block.get("tx_root") or "") != root:
        return False
    header = f"{int(block['index']) - 1}|{block['previous_hash']}|{miner}|{reward}|{root}"
    try:
        nonce = int(block["nonce"])
    except (KeyError, TypeError, ValueError):
        return False
    bits = int(block.get("pow_bits") or state["pow_bits"])
    if not meets_pow(header, nonce, bits):
        return False
    if block.get("hash") != sha256_hex(f"{header}:{nonce}"):
        return False
    if reward:
        reset_year_if_needed(state)
        if int(state["total_mined"]) + reward > int(state["mining_cap"]):
            return False
        if int(state["mined_this_year"]) + reward > int(state["yearly_cap"]):
            return False
        if int(account(state, "POOL_MINING")["0"]) < reward:
            return False
    try:
        apply_recorded_tx(state, tx)
    except Exception:
        return False
    if reward:
        pool = account(state, "POOL_MINING")
        pool["0"] = int(pool["0"]) - reward
        miner_acct = account(state, miner)
        miner_acct["0"] = int(miner_acct["0"]) + reward
        state["total_mined"] = int(state["total_mined"]) + reward
        state["mined_this_year"] = int(state["mined_this_year"]) + reward
        mark_first_mined(state, miner)
    state["chain_height"] = int(block["index"])
    state["tip_hash"] = block["hash"]
    stored = dict(block)
    stored["txs"] = [tx]
    stored["tx_root"] = root
    stored["reward"] = reward
    stored["miner"] = miner
    stored["pow_bits"] = int(block.get("pow_bits") or state["pow_bits"])
    append_block(stored)
    save_state(state)
    return True


def _replay_prefix(prefix: list) -> dict:
    if STATE_PATH.exists():
        STATE_PATH.unlink()
    if CHAIN_PATH.exists():
        CHAIN_PATH.unlink()
    state = load_or_init()
    applied = [b for b in prefix if int(b.get("index") or 0) > 0]
    if applied:
        state["pow_bits"] = int(applied[-1].get("pow_bits") or state["pow_bits"])
        save_state(state)
    for block in applied:
        if not _apply_extension(state, block):
            raise RuntimeError("replay failed")
        state = load_or_init()
    return state


def _prefer_block(challenger: dict, incumbent: dict) -> bool:
    cw, iw = block_work(challenger), block_work(incumbent)
    if cw != iw:
        return cw > iw
    return str(challenger.get("hash") or "") < str(incumbent.get("hash") or "")


def ingest_block(block: dict) -> None:
    with _lock:
        state = load_or_init()
        idx = int(block.get("index", -1))
        height = int(state["chain_height"])
        if idx > height + 1:
            announce("SYNC", {"want_from": height + 1})
            return
        tip_hash = str(state["tip_hash"])
        if idx == height and str(block.get("hash") or "") == tip_hash:
            return
        if idx == height and height >= 1:
            chain = load_all_blocks()
            if chain and str(block.get("previous_hash") or "") == str(chain[-1].get("previous_hash") or ""):
                if _prefer_block(block, chain[-1]):
                    try:
                        state = _replay_prefix(chain[:-1])
                        if not _apply_extension(state, block):
                            _replay_prefix(chain)
                    except Exception:
                        _replay_prefix(chain)
                return
        if idx != height + 1:
            return
        if block.get("previous_hash") != state["tip_hash"]:
            return
        _apply_extension(state, block)


def ingest_transaction(raw: dict) -> bool:
    if not isinstance(raw, dict):
        return False
    with _lock:
        state = load_or_init()
        try:
            commit_signed(state, raw)
            return True
        except Exception as exc:
            print(f"mesh tx rejected: {exc}", flush=True)
            return False


def packet_payload(packet: dict):
    payload = packet.get("payloadJson")
    if isinstance(payload, dict):
        return payload
    raw = packet.get("payload")
    if isinstance(raw, dict):
        return raw
    if isinstance(raw, str) and raw.startswith("{"):
        return json.loads(raw)
    return {}


def send_blocks_from(start: int) -> None:
    for block in load_blocks_from(start, 64):
        announce("BLOCK", block)


def on_mesh_packet(packet: dict):
    ptype = str(packet.get("type") or "").upper()
    try:
        payload = packet_payload(packet)
    except Exception:
        return False
    if ptype == "BLOCK":
        ingest_block(payload)
    elif ptype == "TRANSACTION":
        return ingest_transaction(payload)
    elif ptype == "SYNC":
        send_blocks_from(int(payload.get("want_from") or 0))
    elif ptype == "INVITE":
        return bool(verify_invite(payload))
    elif ptype == "ROUTING_UPDATE":
        with _lock:
            state = load_or_init()
            ours = int(state["chain_height"])
        theirs = int(payload.get("chain_height") or -1)
        if theirs > ours:
            announce("SYNC", {"want_from": ours + 1})
        elif theirs >= 0 and theirs < ours:
            send_blocks_from(theirs + 1)


def build_initial_state(genesis: dict) -> tuple[dict, dict]:
    eco = genesis["economics"]
    keyed = load_keyed_accounts()
    balances: dict[str, dict] = {}
    for name, amount_coins in genesis["treasury"].items():
        if name not in PROTOCOL_POOLS:
            continue
        acct = empty_account()
        acct["0"] = to_units(amount_coins)
        balances[name] = acct
    founder = empty_account()
    founder["0"] = to_units(eco["founder"])
    balances[keyed["founder"]] = founder
    empower = empty_account()
    empower["0"] = to_units(eco["empower"])
    balances[keyed["empower"]] = empower
    strategic = empty_account()
    strategic["0"] = to_units(eco["strategic"])
    balances[keyed["strategic"]] = strategic
    each = to_units(eco["genesis_wallet_each"])
    for addr in keyed.get("distribution") or []:
        acct = empty_account()
        acct["0"] = each
        balances[addr] = acct
    now = int(time.time())
    genesis_block = {
        "index": 0,
        "timestamp": genesis["genesis_block"]["timestamp"],
        "previous_hash": "0",
        "miner": "GENESIS",
        "nonce": 0,
        "hash": sha256_hex("BRAHMNETWORK-L1-GENESIS"),
        "tx_root": sha256_hex(""),
        "txs": [],
        "reward": 0,
    }
    return {
        "chain_id": genesis["chain_id"],
        "canonical_ledger": "python-l1",
        "units_per_brah": UNITS_PER_BRAH,
        "balances": balances,
        "referrals": {},
        "chain_height": 0,
        "total_mined": 0,
        "mined_this_year": 0,
        "retarget_height": 0,
        "retarget_time": now,
        "donation_unlock": now + int(eco["donation_unlock_days"]) * 86400,
        "pow_bits": int(eco["pow_difficulty_bits"]),
        "block_reward": to_units(eco.get("block_reward", 0.1)),
        "yearly_cap": to_units(eco["yearly_mine_cap"]),
        "mining_cap": to_units(eco["mining_pool"]),
        "tip_hash": genesis_block["hash"],
        "public_launch": False,
        "protocol_locked": True,
        "year_start": now,
        "joined_count": 0,
        "first_mined": {},
        "rewards_paid": 0,
        "claimed_rewards": {},
        "p2p_peers": 0,
        "burned_brah": 0,
        "applied_txs": {},
        "mempool": [],
        "next_l2_id": 9,
        "l2_operator_grant_done": False,
        "l2_halving_band_paid": -1,
        "council": keyed.get("council"),
        "l2_assets": {
            str(asset_id): {
                "id": asset_id,
                "name": name,
                "symbol": symbol,
                "creator": "GENESIS",
                "burned": 0,
            }
            for asset_id, name, symbol in L2_BOOTSTRAP
        },
    }, genesis_block


def refuse_legacy_state(state: dict) -> dict:
    migrate_brand_state(state)
    scale = int(state.get("units_per_brah") or state.get("units_per_btq") or 0)
    if scale != UNITS_PER_BRAH:
        raise RuntimeError(
            "Legacy state refused (units scale != 1e8). "
            "Set BRAH_DATA_DIR to a new directory and generate genesis keys."
        )
    for name in state.get("balances", {}):
        if str(name).startswith("GENESIS_"):
            raise RuntimeError(
                "Legacy GENESIS_* placeholders refused. "
                "Set BRAH_DATA_DIR to a new directory and generate genesis keys."
            )
    ensure_l2(state)
    state.setdefault("year_start", int(time.time()))
    state.setdefault("first_mined", {})
    return state


def apply_l2_credits(state: dict) -> bool:
    granted = maybe_grant_operator_l2(state)
    airdropped = maybe_l2_halving_airdrop(state)
    return granted or airdropped


def load_or_init():
    refresh_paths()
    genesis = load_genesis()
    if STATE_PATH.exists():
        with open(STATE_PATH, "r", encoding="utf-8") as fh:
            state = json.load(fh)
        state = refuse_legacy_state(state)
        if apply_l2_credits(state):
            save_state(state)
        return state
    state, genesis_block = build_initial_state(genesis)
    apply_l2_credits(state)
    save_state(state)
    with open(CHAIN_PATH, "w", encoding="utf-8") as fh:
        fh.write(json.dumps(genesis_block) + "\n")
    return state


def canonical_rpc_method(method) -> str:
    name = str(method or "")
    if name.startswith("brah_"):
        return "brah_" + name[4:]
    return name


SUBMIT_METHODS = frozenset({
    "brah_submitTx",
    "brah_sendTransaction",
    "eth_sendRawTransaction",
    "brah_requestFaucet",
    "brah_faucet",
    "brah_joinRewards",
    "brah_mine",
    "brah_councilDonate",
    "brah_launchL2",
    "brah_launchMemecoin",
})


class L1Handler(BaseHTTPRequestHandler):
    timeout = 120

    def _allow_origin(self) -> str:
        return allow_cors_origin(self.headers.get("Origin") or "")

    def _apply_cors(self) -> None:
        origin = self._allow_origin()
        if origin:
            self.send_header("Access-Control-Allow-Origin", origin)
            self.send_header("Vary", "Origin")

    def _send(self, code: int, content_type: str, body) -> None:
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self._apply_cors()
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if self.command == "HEAD":
            return
        try:
            self.wfile.write(body)
        except (ConnectionAbortedError, ConnectionResetError, BrokenPipeError, OSError):
            return

    def do_OPTIONS(self) -> None:
        self.send_response(200)
        self._apply_cors()
        self.send_header("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_HEAD(self) -> None:
        self.do_GET()

    def do_GET(self) -> None:
        if not rpc_allow(self.client_address[0]):
            self._send(429, "text/plain", "rate limited")
            return
        if self.client_address[0] in ("127.0.0.1", "::1"):
            learn_public_host_from_headers(self.headers)
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        if path in ("/wallet.apk", "/download"):
            if not apk_download_allowed():
                self._send(404, "text/plain", "APK download disabled (set BRAH_SERVE_APK=1)")
                return
            apk = resolve_apk()
            if apk is None:
                self._send(404, "text/plain", "No APK built")
                return
            data = apk.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="brahma-wallet.apk"')
            self.send_header("Content-Length", str(len(data)))
            self._apply_cors()
            self.end_headers()
            if self.command != "HEAD":
                try:
                    self.wfile.write(data)
                except (ConnectionAbortedError, ConnectionResetError, BrokenPipeError, OSError):
                    return
            return
        if path in ("/explorer", "/blocks"):
            if not explorer_allowed(self.client_address[0]):
                self._send(404, "text/plain", "explorer is localhost-only (set BRAH_EXPLORER=1)")
                return
            with _lock:
                page = explorer_html()
            self._send(200, "text/html; charset=utf-8", page)
            return
        if path in ("/rpc", "/rpc/"):
            self._send(200, "application/json", json.dumps({
                "network": "BRAHMNETWORK",
                "canonical_ledger": "python-l1",
                "signature": "Dilithium3",
                **mainnet_ready_report(),
                "jsonrpc": "POST /",
                "methods": [
                    "brah_submitTx",
                    "brah_getAccount",
                    "brah_getStats",
                    "brah_getBlocks",
                    "eth_getBalance",
                ],
                "public_rpc": public_rpc_url() or None,
            }))
            return
        if path == "/":
            self._send(200, "text/html; charset=utf-8", home_html())
            return
        if path.startswith("/static/"):
            data, ctype = static_asset(path[len("/static/") :])
            if data is None:
                self._send(404, "text/plain", "not found")
                return
            self._send(200, ctype, data)
            return
        if path in ("/claim", "/status"):
            refs = parse_qs(parsed.query).get("ref", [])
            ref = refs[0] if refs else ""
            self._send(200, "text/html; charset=utf-8", claim_html(ref))
            return
        self._send(404, "application/json", json.dumps({"error": "not found", "public_launch": False}))

    def do_POST(self) -> None:
        if not rpc_allow(self.client_address[0]):
            self._send(429, "application/json", json.dumps({"error": "rate limited"}))
            return
        if self.client_address[0] in ("127.0.0.1", "::1"):
            learn_public_host_from_headers(self.headers)
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except (TypeError, ValueError):
            self._send(400, "application/json", json.dumps({"error": "invalid content-length"}))
            return
        if length < 0 or length > MAX_RPC_BODY:
            self._send(413, "application/json", json.dumps({"error": "payload too large"}))
            return
        raw = self.rfile.read(length)
        try:
            request = json.loads(raw)
        except Exception:
            self._send(400, "application/json", json.dumps({"error": "invalid json"}))
            return
        method = canonical_rpc_method(request.get("method"))
        self._rpc_method = str(method or "")
        params = request.get("params", [])
        if not isinstance(params, list):
            self._send(400, "application/json", json.dumps({"error": "params must be a list"}))
            return
        result = None
        error = None
        try:
            if method in SUBMIT_METHODS and params and isinstance(params[0], dict):
                raw = params[0]
                with _lock:
                    state = load_or_init()
                    preview = parse_signed_tx(state, raw, verify=False)
                signed_from = checksum_address(str(raw.get("from") or preview["from"]))
                raw_to = str(raw.get("to") or preview.get("to") or "")
                signed_to = checksum_address(raw_to) if raw_to and is_brah_address(raw_to) else str(preview.get("to") or "")
                msg = matching_canonical_message(
                    int(preview["chain_id"]),
                    int(preview["nonce"]),
                    str(preview["kind"]),
                    signed_from,
                    signed_to,
                    str(preview["asset"]),
                    int(preview["amount"]),
                    str(preview["memo"]),
                    public_key_hex=preview["public_key"],
                    signature_hex=preview["signature"],
                    require_sig=True,
                )
                if preview["kind"] == "donate":
                    keyed = load_keyed_accounts()
                    empower = str(keyed.get("empower") or "")
                    if not empower:
                        raise ValueError("Empower key missing")
                    verify_spend(
                        checksum_address(empower) if is_brah_address(empower) else empower,
                        str(preview.get("cosign_public_key") or ""),
                        str(preview.get("cosign_signature") or ""),
                        msg,
                    )
                with _lock:
                    state = load_or_init()
                    result = self._dispatch(state, method, params, verify=False)
            else:
                with _lock:
                    state = load_or_init()
                    result = self._dispatch(state, method, params)
        except ValueError as exc:
            print(f"{self.client_address[0]} {method} ERR {exc}", flush=True)
            error = {"code": -32603, "message": str(exc)}
        except Exception as exc:
            print("internal error:", type(exc).__name__, exc, flush=True)
            error = {"code": -32603, "message": "internal error"}
        if error is not None:
            print(f"{self.client_address[0]} {method} ERR {error.get('message')}", flush=True)
        else:
            print(f"{self.client_address[0]} {method} ok", flush=True)
        response = {"jsonrpc": "2.0", "id": request.get("id", 1)}
        if error is not None:
            response["error"] = error
        else:
            response["result"] = result
        self._send(200, "application/json", json.dumps(response))

    def log_message(self, fmt: str, *args) -> None:
        host = self.headers.get("Host") or ""
        note = getattr(self, "_rpc_method", "")
        extra = ((" " + note) if note else "") + ((" host=" + host) if host else "")
        sys.stderr.write(
            "%s - [%s]%s %s\n"
            % (self.address_string(), self.log_date_time_string(), extra, fmt % args)
        )

    def _dispatch(self, state: dict, method: str, params: list, *, verify: bool = True):
        if method in ("brah_getNetworkStats", "brah_getStats"):
            req_addr = params[0] if params else ""
            referrals = state.get("referrals", {})
            return {
                "chain_height": state["chain_height"],
                "total_mined": from_units_float(int(state["total_mined"])),
                "total_users": max(0, len(state["balances"]) - 3),
                "difficulty": state["pow_bits"],
                "pow_bits": state["pow_bits"],
                "tip_hash": state["tip_hash"],
                "block_reward": from_units_float(to_units(block_reward(int(state.get("joined_count", 0))))),
                "next_mine_reward": from_units_float(mine_payout_units(state, req_addr)) if req_addr else 0,
                "first_mine_pending": bool(req_addr) and not flag_for(state.get("first_mined") or {}, req_addr),
                "joined_count": int(state.get("joined_count", 0)),
                "p2p_status": "P2P",
                "referral_tree": int(referrals.get(req_addr, 0)) if req_addr else 0,
                "live": True,
                "chain_id": state["chain_id"],
                **mainnet_ready_report(),
                "canonical_ledger": "python-l1",
                "signature_required": True,
                "units_per_brah": UNITS_PER_BRAH,
                "rewards_paid": from_units_float(int(state.get("rewards_paid", 0))),
                "rewards_remaining": from_units_float(int(account(state, "POOL_AIRDROP").get("0", 0))),
                "tip": state["tip_hash"][:16],
                "p2p_peers": _peer_net.peer_count() if _peer_net is not None else 0,
                "mesh": "sentinel-dtn",
                "l2_launch_burn": from_units_float(l2_burn_units()),
                "burned_brah": from_units_float(int(state.get("burned_brah") or state.get("burned_btq") or 0)),
                "l2_assets": list(state.get("l2_assets", {}).values()),
            }
        if method == "brah_getAccount":
            addr = params[0] if params else ""
            acct = account(state, addr, create=False)
            shown = addr
            try:
                if addr and is_brah_address(addr):
                    shown = normalize_address(addr)
            except ValueError:
                shown = addr
            return {
                "address": shown,
                "nonce": int(acct.get("nonce", 0)),
                "protocol": is_protocol(addr),
                "units": str(int(acct.get("0", 0))),
                "BRAH": from_units_float(int(acct.get("0", 0))),
                "Brahma": from_units_float(int(acct.get("0", 0))),
                "BRAHMA": from_units_float(int(acct.get("0", 0))),
                "Brahma Coin": from_units_float(int(acct.get("0", 0))),
                "chain_id": state["chain_id"],
                "pow_bits": int(state["pow_bits"]),
                "chain_height": int(state["chain_height"]),
                "tip_hash": state["tip_hash"],
                "block_reward_units": str(mine_payout_units(state, addr)),
            }
        if method in ("brah_getBalance", "eth_getBalance"):
            addr = params[0] if params else ""
            asset = str(params[1]) if len(params) > 1 else "latest"
            acct = account(state, addr, create=False)
            if asset.lower() in ("latest", "brah", "brahma", "brahma coin", "0"):
                return {
                    "BRAH": from_units_float(int(acct.get("0", 0))),
                    "Brahma": from_units_float(int(acct.get("0", 0))),
                    "BRAHMA": from_units_float(int(acct.get("0", 0))),
                    "Brahma Coin": from_units_float(int(acct.get("0", 0))),
                    "0": from_units_float(int(acct.get("0", 0))),
                    "units": str(int(acct.get("0", 0))),
                    "nonce": int(acct.get("nonce", 0)),
                }
            if str(asset).lower() == "qusd":
                raise ValueError("QUSD removed")
            return {"amount": float(int(acct.get(asset, 0)))}
        if method in ("brah_submitTx", "brah_sendTransaction", "eth_sendRawTransaction"):
            raw = params[0] if params else {}
            if method != "brah_submitTx" and not raw.get("kind"):
                raw = dict(raw)
                raw["kind"] = "send"
            tx, block = commit_signed(state, raw, verify=verify)
            return {"hash": tx["hash"], "height": block["index"]}
        if method in ("brah_requestFaucet", "brah_faucet", "brah_joinRewards"):
            raw = params[0] if params and isinstance(params[0], dict) else {
                "kind": "join",
                "from": params[0] if params else "",
                "memo": params[1] if len(params) > 1 else "",
                "public_key": "",
                "signature": "",
            }
            raw = dict(raw)
            raw["kind"] = "join"
            tx, _block = commit_signed(state, raw, verify=verify)
            return f"Adoption reward sealed in block (tx {tx['hash'][:16]})"
        if method == "brah_mine":
            raw = params[0] if params and isinstance(params[0], dict) else {
                "kind": "mine",
                "from": params[0] if params else "",
            }
            raw = dict(raw)
            raw["kind"] = "mine"
            tx, block = commit_signed(state, raw, verify=verify)
            return f"height={block['index']} nonce={block['nonce']} hash={block['hash'][:16]} tx={tx['hash'][:16]}"
        if method == "brah_councilDonate":
            raw = params[0] if params and isinstance(params[0], dict) else {}
            raw = dict(raw)
            raw["kind"] = "donate"
            tx, _block = commit_signed(state, raw, verify=verify)
            return f"Council donated {from_units(int(tx['amount']))} Brahma Coin"
        if method == "brah_mintQUSD":
            raise ValueError("QUSD removed")
        if method in ("brah_launchL2", "brah_launchMemecoin"):
            raw = params[0] if params and isinstance(params[0], dict) else {
                "kind": "launch_l2",
                "from": params[0] if params else "",
                "name": params[1] if len(params) > 1 else "",
                "symbol": params[2] if len(params) > 2 else "",
            }
            raw = dict(raw)
            raw["kind"] = "launch_l2"
            tx, _block = commit_signed(state, raw, verify=verify)
            name, _, symbol = tx["memo"].partition("|")
            return {"id": state["next_l2_id"] - 1, "name": name, "symbol": symbol, "hash": tx["hash"]}
        if method in ("brah_listL2", "brah_getL2Assets"):
            ensure_l2(state)
            return {
                "burn": from_units_float(l2_burn_units()),
                "assets": list(state.get("l2_assets", {}).values()),
            }
        if method == "brah_getBlocks":
            start = int(params[0]) if params else 0
            limit = int(params[1]) if len(params) > 1 else 64
            return {
                "from": start,
                "blocks": load_blocks_from(start, limit),
                "tip": int(state["chain_height"]),
            }
        raise ValueError("Method not found")


def explorer_html() -> str:
    state = load_or_init()
    join_amt, ref_amt = reward_rates(int(state.get("joined_count", 0)))
    rows = []
    if CHAIN_PATH.exists():
        lines = CHAIN_PATH.read_text(encoding="utf-8").strip().splitlines()[-20:]
        for line in lines:
            b = json.loads(line)
            ntx = len(b.get("txs") or [])
            rows.append(
                f"<tr><td>{html.escape(str(b.get('index')))}</td>"
                f"<td>{html.escape(str(b.get('miner')))}</td>"
                f"<td>{ntx}</td><td>{html.escape(str(b.get('hash', ''))[:16])}</td>"
                f"<td>{html.escape(from_units(int(b.get('reward') or 0)))}</td></tr>"
            )
    table = "".join(rows) or "<tr><td colspan=5>No blocks</td></tr>"
    return f"""<!DOCTYPE html><html><head><meta charset="utf-8"/>
<title>BRAHMNETWORK Explorer</title>
<style>body{{font-family:sans-serif;background:#111;color:#eee;margin:24px}}
td,th{{padding:6px 10px;text-align:left}} a{{color:#00FF88}}</style></head>
<body>
<h1>BRAHMNETWORK Explorer</h1>
<p>Canonical python-l1 · Dilithium3 spends · integer units · height {state['chain_height']} ·
mined {from_units(int(state['total_mined']))} Brahma Coin ·
joined {state.get('joined_count', 0)} · next join {join_amt} / referrer {ref_amt} ·
rewards left {from_units(int(account(state, 'POOL_AIRDROP').get('0', 0)))} · protocol locked · private testnet</p>
<p><a href="/">Home</a> · <a href="/rpc">JSON-RPC POST /</a></p>
<table><tr><th>Height</th><th>Miner</th><th>Txs</th><th>Hash</th><th>Reward</th></tr>{table}</table>
</body></html>
"""


def resolve_bind() -> str:
    bind = env("BIND", "127.0.0.1") or "127.0.0.1"
    local = bind in ("127.0.0.1", "localhost", "::1")
    if not local and env("ALLOW_LAN") != "1":
        raise RuntimeError(
            "Refusing to bind beyond localhost. "
            "Set BRAH_ALLOW_LAN=1 and BRAH_BIND=0.0.0.0 for phone LAN tests."
        )
    if env("REQUIRE_TLS") == "1":
        if not (env("TLS_CERT") and env("TLS_KEY")):
            raise RuntimeError("BRAH_REQUIRE_TLS=1 requires BRAH_TLS_CERT and BRAH_TLS_KEY")
    return bind


def assert_operator_hygiene() -> None:
    secrets = DATA / "genesis_secrets.json"
    if secrets.exists() and env("ALLOW_SECRETS_ON_DISK") != "1":
        raise RuntimeError(
            "Refusing to start: genesis_secrets.json is next to node data. "
            "Move operator secrets offline. Local-only exception: BRAH_ALLOW_SECRETS_ON_DISK=1."
        )


def run(port: int | None = None) -> None:
    refresh_paths()
    assert_operator_hygiene()
    load_or_init()
    bind = resolve_bind()
    if port is None:
        port = rpc_port()
    public = persist_public_rpc()
    p2p_port = int(env("P2P_PORT", "18545") or "18545")
    seeds = [s for s in (env("PEERS") or "").split(",") if s.strip()]
    global _peer_net
    identity = load_or_create_identity(DATA / "node_identity.json")
    udp_port = int(env("UDP_PORT") or str(p2p_port + 2))
    _peer_net = PeerNet(
        p2p_port,
        seeds,
        f"brah-l1-{p2p_port}",
        on_mesh_packet,
        identity=identity,
        listen_host=bind,
        rpc_port=port,
        udp_port=udp_port,
    )
    _peer_net.start()
    # Let seed HELLO land so BLOCK replies have a peer to send to.
    time.sleep(1.5)
    with _lock:
        tip_state = load_or_init()
        height = int(tip_state["chain_height"])
        announce("ROUTING_UPDATE", {
            "chain_height": height,
            "tip_hash": tip_state["tip_hash"],
        })
        announce("SYNC", {"want_from": height + 1})
    httpd = ThreadingL1Server((bind, port), L1Handler)
    cert = env("TLS_CERT") or ""
    key = env("TLS_KEY") or ""
    tls = False
    if cert and key:
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ctx.minimum_version = ssl.TLSVersion.TLSv1_2
        ctx.load_cert_chain(cert, key)
        httpd.socket = ctx.wrap_socket(httpd.socket, server_side=True)
        tls = True
    extra = f" public_rpc={public}" if public else ""
    ready = mainnet_ready_report()
    print(
        f"BRAHMNETWORK python-l1 bind={bind}:{port} tls={tls} Dilithium3 client-PoW sentinel p2p:{p2p_port} "
        f"l2_burn=100 protocol_locked=true public_launch={str(ready['public_launch']).lower()} "
        f"mainnet_ready={str(ready['mainnet_ready']).lower()}{extra}"
    )
    httpd.serve_forever()


if __name__ == "__main__":
    run()
