"""ML-DSA / Dilithium3 verify for Brahma spends.

Wallet signs with BouncyCastle Dilithium3 (labeled ML-DSA-65 in the app).
The node refuses a spend unless the public key hashes to from-address and
the Dilithium3 signature checks over the canonical message.
Canonical addresses are BRM1G. Canonical message is BRAH1|.
Also accept BRM1| / BRMHELLO / BRMINVITE (brm rename aliases).
Older BTQ1G / BTQ1| / BTQHELLO / BTQINVITE forms still verify so existing
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
BRM_MSG_PREFIX = "BRM1|"
LEGACY_MSG_PREFIX = "BTQ1|"
MSG_PREFIXES = (MSG_PREFIX, BRM_MSG_PREFIX, LEGACY_MSG_PREFIX)
HELLO_PREFIX = "BRAHHELLO"
BRM_HELLO_PREFIX = "BRMHELLO"
LEGACY_HELLO_PREFIX = "BTQHELLO"
HELLO_PREFIXES = (HELLO_PREFIX, BRM_HELLO_PREFIX, LEGACY_HELLO_PREFIX)
INVITE_PREFIX = "BRAHINVITE"
BRM_INVITE_PREFIX = "BRMINVITE"
LEGACY_INVITE_PREFIX = "BTQINVITE"
INVITE_PREFIXES = (INVITE_PREFIX, BRM_INVITE_PREFIX, LEGACY_INVITE_PREFIX)


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


def assert_key_owns_address(from_addr: str, public_key_hex: str) -> None:
    """Refuse spends whose public key does not hash to from (either HRP)."""
    pk = _parse_hex(public_key_hex)
    if pubkey_matches_address(pk, from_addr):
        return
    try:
        if pubkey_matches_address(pk, normalize_address(from_addr)):
            return
        if pubkey_matches_address(pk, sibling_address(from_addr)):
            return
    except ValueError:
        pass
    raise ValueError("Public key does not match from address")


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
    """Prefer BRAH1|; also accept BRM1| and legacy BTQ1| signatures."""
    candidates = [
        _format_canonical_message(
            prefix, chain_id, nonce, kind, from_addr, to_addr, asset, amount_units, memo
        )
        for prefix in MSG_PREFIXES
    ]
    pk_hex = (public_key_hex or "").strip()
    sig_hex = (signature_hex or "").strip()
    if require_sig and (not pk_hex or not sig_hex):
        raise ValueError("signature required")
    if pk_hex and sig_hex:
        pk = _parse_hex(pk_hex)
        sig = _parse_hex(sig_hex)
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
_BC_JAVA_FALLBACK = r"C:\Users\Navesh\.jdks\jbr-17.0.14\bin\java.exe"
_BC_JAR_FALLBACKS = (
    str(_BC_ROOT / "bcprov-jdk18on-1.72.jar"),
    str(_BC_ROOT / "bcprov-jdk18on-1.85.jar"),
    r"C:\Users\Navesh\.gradle\caches\modules-2\files-2.1\org.bouncycastle\bcprov-jdk18on\1.72\d8dc62c28a3497d29c93fee3e71c00b27dff41b4\bcprov-jdk18on-1.72.jar",
    r"C:\Users\Navesh\.gradle\caches\modules-2\files-2.1\org.bouncycastle\bcprov-jdk18on\1.78.1\39e9e45359e20998eb79c1828751f94a818d25f8\bcprov-jdk18on-1.78.1.jar",
)


def bouncycastle_classpath(root, jar: str) -> str:
    """Classpath for the verifier class: node dir + jar, joined with the OS separator."""
    return os.pathsep.join([str(root), str(jar)])


def bouncycastle_java() -> str:
    """Prefer BRAH_JAVA / BRM_JAVA / BTQ_JAVA when the path exists; else JBR 17 or java on PATH."""
    preferred = env("JAVA")
    if preferred and Path(preferred).exists():
        return preferred
    if Path(_BC_JAVA_FALLBACK).exists():
        return _BC_JAVA_FALLBACK
    return "java"


def _first_jar(names: tuple[str, ...]) -> str | None:
    preferred = env("BCPROV_JAR")
    if preferred and Path(preferred).exists() and any(name in Path(preferred).name for name in names):
        return preferred
    for path in _BC_JAR_FALLBACKS:
        jar = Path(path)
        if jar.is_file() and any(name in jar.name for name in names):
            return str(jar)
    return None


def bouncycastle_jar() -> str | None:
    """Historical Dilithium3 checker. Phones already in use sign with 1.72 / 1.78.1."""
    return _first_jar(("1.72", "1.78.1"))


def bouncycastle_mldsa_jar() -> str | None:
    """FIPS 204 checker. BouncyCastle 1.85 verifies pure ML-DSA-87."""
    return _first_jar(("1.85",))


def _run_bc_verify(class_name: str, jar: str, pk: bytes, message: bytes, signature: bytes) -> bool:
    java = bouncycastle_java()
    if not jar or not Path(jar).is_file() or not Path(java).exists():
        return False
    if not (_BC_ROOT / f"{class_name}.class").is_file():
        return False
    try:
        with tempfile.TemporaryDirectory(prefix="brah-bc-") as tmp:
            t = Path(tmp)
            (t / "pk.hex").write_text(pk.hex(), encoding="ascii")
            (t / "sig.hex").write_text(signature.hex(), encoding="ascii")
            (t / "msg.hex").write_text(message.hex(), encoding="ascii")
            proc = subprocess.run(
                [
                    java,
                    "-cp",
                    bouncycastle_classpath(_BC_ROOT, jar),
                    class_name,
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


def _verify_bouncycastle(pk: bytes, message: bytes, signature: bytes) -> bool:
    """Dilithium3 uses the phone jar. A 2592-byte key is FIPS 204 ML-DSA-87 and uses 1.85."""
    if len(pk) == 2592:
        jar = bouncycastle_mldsa_jar()
        return bool(jar) and _run_bc_verify("bc_mldsa", jar, pk, message, signature)
    legacy = bouncycastle_jar()
    if legacy and _run_bc_verify("bc_verify", legacy, pk, message, signature):
        return True
    modern = bouncycastle_mldsa_jar()
    return bool(modern) and _run_bc_verify("bc_mldsa", modern, pk, message, signature)


def verify_dilithium3(public_key: bytes, message: bytes, signature: bytes) -> bool:
    """Accept existing Dilithium3 keys and FIPS 204 ML-DSA-87 keys. A 2592-byte key is ML-DSA-87 and is not sliced."""
    if not public_key or not signature:
        return False
    candidates = [public_key]
    if len(public_key) > 1952 and len(public_key) != 2592:
        candidates.append(public_key[-1952:])
    if len(public_key) != 2592 and Dilithium3 is not None and len(signature) != 3309:
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
    for prefix in INVITE_PREFIXES:
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
    for prefix in HELLO_PREFIXES:
        msg = f"{prefix}|{node}|{int(port)}|{int(ts)}".encode("utf-8")
        if verify_dilithium3(pk, msg, sig):
            return True
    return False


def device_message(address: str, pub_hex: str) -> bytes:
    return f"BRAHDEV|{address}|{pub_hex}".encode("utf-8")


def verify_device_key(pub_hex: str, message: bytes, sig_hex: str) -> bool:
    """Verify a non-exportable phone P-256 key. pub_hex is the compressed point."""
    try:
        raw = bytes.fromhex(pub_hex.strip())
        sig = bytes.fromhex(sig_hex.strip())
    except ValueError:
        return False
    if len(raw) != 33 or raw[0] not in (2, 3) or not sig:
        return False
    spki = bytes.fromhex("3039301306072a8648ce3d020106082a8648ce3d030107032200") + raw
    try:
        with tempfile.TemporaryDirectory() as folder:
            der = Path(folder) / "pub.der"
            pem = Path(folder) / "pub.pem"
            msg = Path(folder) / "msg.bin"
            signature = Path(folder) / "sig.bin"
            der.write_bytes(spki)
            msg.write_bytes(message)
            signature.write_bytes(sig)
            made = subprocess.run(
                ["openssl", "pkey", "-pubin", "-inform", "DER", "-in", str(der), "-out", str(pem)],
                capture_output=True,
            )
            if made.returncode != 0:
                return False
            checked = subprocess.run(
                ["openssl", "dgst", "-sha256", "-verify", str(pem), "-signature", str(signature), str(msg)],
                capture_output=True,
            )
            return checked.returncode == 0
    except OSError:
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
