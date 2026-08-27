import hashlib

def get_pubkey_hash(pubkey: bytes) -> bytes:
    """Computes the SHA-256 hash of a public key."""
    return hashlib.sha256(pubkey).digest()
