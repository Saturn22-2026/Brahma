"""ML-DSA / Dilithium3 verify for Brahma spends.

Wallet signs with BouncyCastle Dilithium3 (labeled ML-DSA-65 in the app).
The node refuses a spend unless the public key hashes to from-address and
the Dilithium3 signature checks over the canonical message.
Canonical addresses are BRM1G. Canonical message is BRAH1|.
Older BTQ1G / BTQ1| / BTQHELLO / BTQINVITE forms still verify so height-8
history and old phones do not fork.
"""
from __future__ import annotations

import hashlib
import json
import os
import subprocess
import tempfile
import threading
import time
from pathlib import Path

from envutil import env

try:
    from dilithium_py.dilithium import Dilithium3
except ImportError:  # pragma: no cover
    Dilithium3 = None

# dilithium-py is not safe to overlap sign/verify across threads (HTTP miners).
_MLDSA_LOCK = threading.Lock()


def sha256(data: bytes) -> bytes:
    return hashlib.sha256(data).digest()


ADDR_PREFIX = "BRM1G"
LEGACY_ADDR_PREFIX = "BTQ1G"
ADDR_PREFIXES = (ADDR_PREFIX, LEGACY_ADDR_PREFIX)
MSG_PREFIX = "BRAH1|"
LEGACY_MSG_PREFIX = "BTQ1|"
HELLO_PREFIX = "BRAHHELLO"
LEGACY_HELLO_PREFIX = "BTQHELLO"
INVITE_PREFIX = "BRAHINVITE"
LEGACY_INVITE_PREFIX = "BTQINVITE"


def _addr_body(pk: bytes) -> str:
    return sha256(pk)[:12].hex()


def _addr_prefix(addr: str) -> str | None:
    if not isinstance(addr, str):
        return None
    for prefix in ADDR_PREFIXES:
        if addr.startswith(prefix):
            return prefix
    return None


def _addr_checksum(body: str, prefix: str = ADDR_PREFIX) -> str:
    return sha256(f"{prefix}{body}".encode("utf-8"))[:2].hex()


def address_from_pubkey(pk: bytes, prefix: str = ADDR_PREFIX) -> str:
    body = _addr_body(pk)
    return prefix + body + _addr_checksum(body, prefix)


def sibling_address(addr: str) -> str:
    """Same body under the other HRP, checksummed."""
    normalized = normalize_address(addr)
    prefix = _addr_prefix(normalized)
    other = LEGACY_ADDR_PREFIX if prefix == ADDR_PREFIX else ADDR_PREFIX
    body = normalized[5:29]
    return other + body + _addr_checksum(body, other)


def pubkey_matches_address(pk: bytes, addr: str) -> bool:
    body = _addr_body(pk)
    prefix = _addr_prefix(addr)
    if prefix is None:
        return False
    if addr == prefix + body:
        return True
    return addr == prefix + body + _addr_checksum(body, prefix)


def is_brah_address(addr: str) -> bool:
    prefix = _addr_prefix(addr)
    if prefix is None:
        return False
    rest = addr[5:]
    if len(rest) not in (24, 28):
        return False
    try:
        bytes.fromhex(rest)
    except ValueError:
        return False
    if len(rest) == 24:
        return True
    return rest[24:] == _addr_checksum(rest[:24], prefix)


is_brah_address = is_brah_address


def checksum_address(addr: str) -> str:
    """Checksummed form that keeps the HRP the caller used (needed for old signatures)."""
    if not is_brah_address(addr):
        raise ValueError("invalid Brahma address")
    prefix = _addr_prefix(addr)
    body = addr[5:29].lower()
    return prefix + body + _addr_checksum(body, prefix)


def normalize_address(addr: str) -> str:
    """Checksummed BRM1G. Older BTQ1G input maps to the same 24-hex body."""
    checked = checksum_address(addr)
    body = checked[5:29]
    return ADDR_PREFIX + body + _addr_checksum(body, ADDR_PREFIX)


def _format_canonical_message(
    prefix: str,
    chain_id: int,
    nonce: int,
    kind: str,
    from_addr: str,
    to_addr: str,
    asset: str,
    amount_units: int,
    memo: str = "",
) -> bytes:
    return (
        f"{prefix}{int(chain_id)}|{int(nonce)}|{kind}|{from_addr}|{to_addr}|"
        f"{asset}|{int(amount_units)}|{memo}"
    ).encode("utf-8")


def canonical_message(
    chain_id: int,
    nonce: int,
    kind: str,
    from_addr: str,
    to_addr: str,
    asset: str,
    amount_units: int,
    memo: str = "",
) -> bytes:
    return _format_canonical_message(
        MSG_PREFIX, chain_id, nonce, kind, from_addr, to_addr, asset, amount_units, memo
    )


def matching_canonical_message(
    chain_id: int,
    nonce: int,
    kind: str,
    from_addr: str,
    to_addr: str,
    asset: str,
    amount_units: int,
    memo: str = "",
    public_key_hex: str = "",
    signature_hex: str = "",
    require_sig: bool = False,
) -> bytes:
    """Prefer BRAH1|; accept a BTQ1| signature so existing blocks still ingest."""
    candidates = [
        _format_canonical_message(
            MSG_PREFIX, chain_id, nonce, kind, from_addr, to_addr, asset, amount_units, memo
        ),
        _format_canonical_message(
            LEGACY_MSG_PREFIX, chain_id, nonce, kind, from_addr, to_addr, asset, amount_units, memo
        ),
    ]
    if public_key_hex and signature_hex:
        pk = _parse_hex(public_key_hex)
        sig = _parse_hex(signature_hex)
        pk_ok = pubkey_matches_address(pk, from_addr)
        if not pk_ok and len(pk) > 1952:
            pk_ok = pubkey_matches_address(pk[-1952:], from_addr)
        if require_sig and not pk_ok:
            raise ValueError("Public key does not match from address")
        for msg in candidates:
            if verify_dilithium3(pk, msg, sig):
                return msg
        if require_sig:
            raise ValueError("ML-DSA / Dilithium3 signature invalid")
    return candidates[0]


def _parse_hex(value: str) -> bytes:
    clean = (value or "").strip().replace("0x", "").replace("0X", "")
    if len(clean) % 2:
        raise ValueError("invalid hex")
    return bytes.fromhex(clean)


_BC_ROOT = Path(__file__).resolve().parent
_BC_JAR = env(
    "BCPROV_JAR",
    r"C:\Users\Navesh\.gradle\caches\modules-2\files-2.1\org.bouncycastle\bcprov-jdk18on\1.78.1\39e9e45359e20998eb79c1828751f94a818d25f8\bcprov-jdk18on-1.78.1.jar",
)
_BC_JAVA = env(
    "JAVA",
    r"C:\Users\Navesh\.jdks\jbr-17.0.14\bin\java.exe",
)


def _verify_bc_mldsa65(public_key: bytes, message: bytes, signature: bytes) -> bool:
    """Verify a BouncyCastle 1.78 Dilithium3 signature.

    The wallet's bcprov 1.78.1 signs ML-DSA-65 (1952-byte key, 3309-byte sig).
    dilithium-py's Dilithium3 check is the older 3293-byte scheme and rejects these.
    BC samples the challenge from the first 32 bytes of c~, then compares the full
    48-byte c~. FIPS verify uses all 48 bytes as the challenge seed, so it also
    rejects a valid wallet signature.
    """
    if len(public_key) != 1952 or len(signature) != 3309:
        return False
    try:
        from dilithium_py.ml_dsa import ML_DSA_65
    except ImportError:
        return False
    d = ML_DSA_65
    try:
        rho, t1 = d._unpack_pk(public_key)
        c_tilde, z, h = d._unpack_sig(signature)
    except Exception:
        return False
    if h.sum_hint() > d.omega or z.check_norm_bound(d.gamma_1 - d.beta):
        return False
    A_hat = d._expand_matrix_from_seed(rho)
    tr = d._h(public_key, 64)
    mu = d._h(tr + message, 64)
    c = d.R.sample_in_ball(c_tilde[:32], d.tau).to_ntt()
    t1n = t1.scale(1 << d.d).to_ntt()
    acc = ((A_hat @ z.to_ntt()) - t1n.scale(c)).from_ntt()
    w_prime = h.use_hint(acc, 2 * d.gamma_2)
    got = d._h(mu + w_prime.bit_pack_w(d.gamma_2), d.c_tilde_bytes)
    return got == c_tilde


def _verify_bouncycastle(pk: bytes, message: bytes, signature: bytes) -> bool:
    """Java fallback when dilithium-py has no ML-DSA module. Linux classpath uses ':'."""
    cls = _BC_ROOT / "bc_verify.class"
    if not cls.exists() or not Path(_BC_JAVA).exists() or not Path(_BC_JAR).exists():
        return False
    try:
        with tempfile.TemporaryDirectory(prefix="brah-bc-") as tmp:
            t = Path(tmp)
            (t / "pk.hex").write_text(pk.hex(), encoding="ascii")
            (t / "sig.hex").write_text(signature.hex(), encoding="ascii")
            (t / "msg.hex").write_text(message.hex(), encoding="ascii")
            cp = os.pathsep.join((str(_BC_ROOT), _BC_JAR))
            proc = subprocess.run(
                [
                    _BC_JAVA,
                    "-cp",
                    cp,
                    "bc_verify",
                    str(t / "pk.hex"),
                    str(t / "sig.hex"),
                    str(t / "msg.hex"),
                ],
                check=False,
                timeout=90,
                capture_output=True,
            )
            return proc.returncode == 0
    except Exception:
        return False


def verify_dilithium3(public_key: bytes, message: bytes, signature: bytes) -> bool:
    if not public_key or not signature:
        return False
    candidates = [public_key]
    if len(public_key) > 1952:
        candidates.append(public_key[-1952:])
    if len(signature) == 3309:
        with _MLDSA_LOCK:
            for pk in candidates:
                try:
                    if _verify_bc_mldsa65(pk, message, signature):
                        return True
                except Exception:
                    continue
    elif Dilithium3 is not None:
        with _MLDSA_LOCK:
            for pk in candidates:
                try:
                    if Dilithium3.verify(pk, message, signature):
                        return True
                except Exception:
                    pass
                try:
                    if Dilithium3.verify(pk, signature, message):
                        return True
                except Exception:
                    continue
    for pk in candidates:
        if _verify_bouncycastle(pk, message, signature):
            return True
    return False


def addresses_equal(a: str, b: str) -> bool:
    if a == b:
        return True
    try:
        return normalize_address(a)[5:29] == normalize_address(b)[5:29]
    except ValueError:
        return False


def verify_spend(from_addr: str, public_key_hex: str, signature_hex: str, message: bytes) -> None:
    pk = _parse_hex(public_key_hex)
    sig = _parse_hex(signature_hex)
    if not pubkey_matches_address(pk, from_addr) and not pubkey_matches_address(pk, normalize_address(from_addr)):
        raise ValueError("Public key does not match from address")
    if not verify_dilithium3(pk, message, sig):
        raise ValueError("ML-DSA / Dilithium3 signature invalid")


def sign_dilithium3(secret_key: bytes, message: bytes) -> bytes:
    if Dilithium3 is None:
        raise ValueError("Dilithium3 signer not installed (pip install dilithium-py)")
    with _MLDSA_LOCK:
        return Dilithium3.sign(secret_key, message)


HELLO_MAX_SKEW_SEC = 90
INVITE_MAX_SKEW_SEC = 90
INVITE_MAX_URL = 256


def invite_message(ref: str, claim: str, apk: str, ts: int) -> bytes:
    return f"{INVITE_PREFIX}|{ref}|{claim}|{apk}|{int(ts)}".encode("utf-8")


def _safe_http_url(url: str, allowed_paths: set[str]) -> bool:
    if not isinstance(url, str) or not url or len(url) > INVITE_MAX_URL:
        return False
    from urllib.parse import urlparse
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https"):
        return False
    if parsed.username or parsed.password or parsed.params or parsed.fragment:
        return False
    path = (parsed.path or "/").rstrip("/") or "/"
    if path not in allowed_paths:
        return False
    host = parsed.hostname or ""
    if not host or ".." in host or "/" in host or "\\" in host:
        return False
    return True


def is_safe_claim_url(url: str) -> bool:
    return _safe_http_url(url, {"/claim"})


def is_safe_apk_url(url: str) -> bool:
    if url == "":
        return True
    return _safe_http_url(url, {"/wallet.apk"})


def verify_invite(payload: dict, now: int | None = None) -> bool:
    if not isinstance(payload, dict):
        return False
    ref = str(payload.get("ref") or "")
    claim = str(payload.get("claim") or "")
    apk = str(payload.get("apk") or "")
    try:
        ts = int(payload.get("ts") or 0)
    except (TypeError, ValueError):
        return False
    if ts <= 0 or not is_brah_address(ref) or len(ref) != 33:
        return False
    if not is_safe_claim_url(claim) or not is_safe_apk_url(apk):
        return False
    clock = int(now if now is not None else time.time())
    if abs(clock - ts) > INVITE_MAX_SKEW_SEC:
        return False
    pk_hex = str(payload.get("public_key") or "")
    sig_hex = str(payload.get("signature") or "")
    for prefix in (INVITE_PREFIX, LEGACY_INVITE_PREFIX):
        msg = f"{prefix}|{ref}|{claim}|{apk}|{int(ts)}".encode("utf-8")
        try:
            verify_spend(ref, pk_hex, sig_hex, msg)
            return True
        except ValueError:
            continue
    return False


def hello_message(node_id: str, tcp_port: int, ts: int) -> bytes:
    return f"{HELLO_PREFIX}|{node_id}|{int(tcp_port)}|{int(ts)}".encode("utf-8")


def verify_hello(payload: dict, now: int | None = None) -> bool:
    node = str(payload.get("node") or payload.get("senderId") or "")
    try:
        port = int(payload.get("tcpPort") or payload.get("port") or 0)
        ts = int(payload.get("ts") or 0)
    except (TypeError, ValueError):
        return False
    if not node or port <= 0 or ts <= 0:
        return False
    clock = int(now if now is not None else time.time())
    if abs(clock - ts) > HELLO_MAX_SKEW_SEC:
        return False
    try:
        pk = _parse_hex(str(payload.get("public_key") or ""))
        sig = _parse_hex(str(payload.get("signature") or ""))
    except ValueError:
        return False
    for prefix in (HELLO_PREFIX, LEGACY_HELLO_PREFIX):
        msg = f"{prefix}|{node}|{int(port)}|{int(ts)}".encode("utf-8")
        if verify_dilithium3(pk, msg, sig):
            return True
    return False


def load_or_create_identity(path: Path) -> tuple[bytes, bytes]:
    if Dilithium3 is None:
        raise ValueError("Dilithium3 not installed (pip install dilithium-py)")
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
        return bytes.fromhex(data["public_key"]), bytes.fromhex(data["secret_key"])
    pk, sk = Dilithium3.keygen()
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps({"algo": "Dilithium3", "public_key": pk.hex(), "secret_key": sk.hex()}),
        encoding="utf-8",
    )
    return pk, sk
