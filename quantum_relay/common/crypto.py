import os
import base64
import hashlib
from cryptography.hazmat.primitives.asymmetric.x25519 import X25519PrivateKey, X25519PublicKey
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey, Ed25519PublicKey
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.exceptions import InvalidSignature, InvalidTag

class LayeredCryptoEnvelope:
    """
    Implements a 4-layer hybrid cryptographic system.
    Currently using X25519/Ed25519 as placeholders for ML-KEM/ML-DSA.

    Layer 1: Key Exchange (X25519 -> ML-KEM-768)
    Layer 2: Key Derivation (HKDF-SHA256)
    Layer 3: Authenticated Encryption (ChaCha20-Poly1305)
    Layer 4: Digital Signatures (Ed25519 -> ML-DSA-44)
    """

    @staticmethod
    def generate_user_keypairs():
        signing_key = Ed25519PrivateKey.generate()
        exchange_key = X25519PrivateKey.generate()

        return {
            'private': {
                'signing': signing_key,
                'exchange': exchange_key
            },
            'public': {
                'signing': signing_key.public_key().public_bytes(
                    encoding=serialization.Encoding.Raw,
                    format=serialization.PublicFormat.Raw
                ),
                'exchange': exchange_key.public_key().public_bytes(
                    encoding=serialization.Encoding.Raw,
                    format=serialization.PublicFormat.Raw
                )
            }
        }

    @staticmethod
    def get_id(public_key_bytes: bytes) -> bytes:
        """Returns the SHA-256 hash of the public key (RecipientID/SenderID)."""
        return hashlib.sha256(public_key_bytes).digest()

    @staticmethod
    def _derive_key(private_exchange_key, recipient_public_exchange_key_bytes, context_info: bytes) -> bytes:
        recipient_pub_key = X25519PublicKey.from_public_bytes(recipient_public_exchange_key_bytes)
        shared_secret = private_exchange_key.exchange(recipient_pub_key)

        hkdf = HKDF(
            algorithm=hashes.SHA256(),
            length=32,
            salt=None, # In this version we use None salt for simplicity matching C++
            info=context_info
        )
        return hkdf.derive(shared_secret)

    @staticmethod
    def encrypt(plaintext: str, sender_keys: dict, recipient_public_exchange: bytes, aad: bytes = b'') -> dict:
        # 1. Ephemeral key for Forward Secrecy
        ephemeral_private_key = X25519PrivateKey.generate()
        ephemeral_public_bytes = ephemeral_private_key.public_key().public_bytes(
            encoding=serialization.Encoding.Raw,
            format=serialization.PublicFormat.Raw
        )

        # 2. Derive symmetric key
        context = b"QuantumCryptoEnvelope_v1|chacha20-poly1305"
        dek = LayeredCryptoEnvelope._derive_key(ephemeral_private_key, recipient_public_exchange, context)

        # 3. Layer 3: ChaCha20-Poly1305
        nonce = os.urandom(12)
        aead_cipher = ChaCha20Poly1305(dek)
        ciphertext = aead_cipher.encrypt(nonce, plaintext.encode('utf-8'), aad)

        # 4. Bundle for signing
        bundle_to_sign = ephemeral_public_bytes + nonce + aad + ciphertext

        # 5. Layer 4: Sign
        signature = sender_keys['private']['signing'].sign(bundle_to_sign)

        return {
            'ephemeral_pub': ephemeral_public_bytes,
            'nonce': nonce,
            'ciphertext': ciphertext,
            'signature': signature,
            'aad': aad
        }

    @staticmethod
    def decrypt(payload: dict, recipient_keys: dict, sender_public_signing: bytes) -> str:
        try:
            ephemeral_pub_bytes = payload['ephemeral_pub']
            nonce = payload['nonce']
            ciphertext = payload['ciphertext']
            signature = payload['signature']
            aad = payload.get('aad', b'')

            # 1. Verify Signature
            sender_signing_pub = Ed25519PublicKey.from_public_bytes(sender_public_signing)
            bundle_to_verify = ephemeral_pub_bytes + nonce + aad + ciphertext
            sender_signing_pub.verify(signature, bundle_to_verify)

            # 2. Derive DEK
            context = b"QuantumCryptoEnvelope_v1|chacha20-poly1305"
            dek = LayeredCryptoEnvelope._derive_key(
                recipient_keys['private']['exchange'],
                ephemeral_pub_bytes,
                context
            )

            # 3. Decrypt
            aead_cipher = ChaCha20Poly1305(dek)
            plaintext_bytes = aead_cipher.decrypt(nonce, ciphertext, aad)

            return plaintext_bytes.decode('utf-8')

        except (InvalidSignature, InvalidTag, Exception) as e:
            raise ValueError(f"Decryption failed: {str(e)}")
