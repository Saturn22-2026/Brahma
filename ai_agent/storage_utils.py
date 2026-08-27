import os
import json
import base64
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

class EncryptedStorage:
    """
    Secure AES-256-GCM storage for AI Council sensitive data.
    """
    def __init__(self, master_key: str):
        # Derive a 256-bit key from the Master Sovereign Key
        kdf = PBKDF2HMAC(
            algorithm=hashes.SHA256(),
            length=32,
            salt=b"BTQ__SALT_v1", # Fixed salt for deterministic recovery
            iterations=100000,
        )
        self.key = kdf.derive(master_key.encode())
        self.aesgcm = AESGCM(self.key)

    def save(self, file_path: str, data: dict):
        plaintext = json.dumps(data).encode()
        nonce = os.urandom(12)
        ciphertext = self.aesgcm.encrypt(nonce, plaintext, None)

        # Store as base64(nonce + ciphertext)
        encoded_data = base64.b64encode(nonce + ciphertext).decode()
        with open(file_path, "w") as f:
            f.write(encoded_data)

    def load(self, file_path: str) -> dict:
        if not os.path.exists(file_path):
            return {}

        with open(file_path, "r") as f:
            encoded_data = f.read()

        raw_data = base64.b64decode(encoded_data)
        nonce = raw_data[:12]
        ciphertext = raw_data[12:]

        plaintext = self.aesgcm.decrypt(nonce, ciphertext, None)
        return json.loads(plaintext.decode())
