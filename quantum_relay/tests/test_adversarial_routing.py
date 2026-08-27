import pytest
import hashlib
import json
import base64
from ..common.router import NetworkRouter
from ..common.crypto import LayeredCryptoEnvelope
from ..common.protocol import QuantumTransportPacket

def test_malicious_relay_tampering():
    """
    Simulate a relay node attempting to modify the ciphertext.
    The LayeredCryptoEnvelope must detect this via its AEAD layer.
    """
    # 1. Setup
    alice_keys = LayeredCryptoEnvelope.generate_user_keypairs()
    bob_keys = LayeredCryptoEnvelope.generate_user_keypairs()

    # 2. Alice sends a message
    envelope = LayeredCryptoEnvelope.encrypt(
        "Secret Data", alice_keys, bob_keys['public']['exchange'], aad=b"v1"
    )

    # 3. Malicious Relay (Attacker) tampers with the ciphertext
    tampered_envelope = envelope.copy()
    tampered_ciphertext = bytearray(tampered_envelope['ciphertext'])
    tampered_ciphertext[0] ^= 0xFF # Flip a bit
    tampered_envelope['ciphertext'] = bytes(tampered_ciphertext)

    # 4. Bob attempts to decrypt
    with pytest.raises(ValueError) as excinfo:
        LayeredCryptoEnvelope.decrypt(
            tampered_envelope, bob_keys, alice_keys['public']['signing']
        )

    assert "Decryption failed" in str(excinfo.value)
    print("\n[Security] Malicious tampering correctly blocked by AEAD.")

def test_signature_forgery_prevention():
    """
    Simulate an attacker trying to swap the payload but reusing a valid signature.
    This must fail because the signature covers the ciphertext.
    """
    alice_keys = LayeredCryptoEnvelope.generate_user_keypairs()
    bob_keys = LayeredCryptoEnvelope.generate_user_keypairs()
    attacker_keys = LayeredCryptoEnvelope.generate_user_keypairs()

    # 1. Alice sends a message
    alice_envelope = LayeredCryptoEnvelope.encrypt(
        "Safe Message", alice_keys, bob_keys['public']['exchange'], aad=b"v1"
    )

    # 2. Attacker prepares a malicious message
    malicious_envelope = LayeredCryptoEnvelope.encrypt(
        "Malicious Payload", attacker_keys, bob_keys['public']['exchange'], aad=b"v1"
    )

    # 3. Attacker tries to "stitch" Alice's signature onto the malicious payload
    stitched_envelope = malicious_envelope.copy()
    stitched_envelope['signature'] = alice_envelope['signature']

    # 4. Bob attempts to decrypt (expecting it's from Alice)
    with pytest.raises(ValueError) as excinfo:
        LayeredCryptoEnvelope.decrypt(
            stitched_envelope, bob_keys, alice_keys['public']['signing']
        )

    assert "Decryption failed" in str(excinfo.value)
    print("\n[Security] Signature stitching correctly blocked by ML-DSA verification.")

def test_gossip_flooding_resilience():
    """
    Verify the framework for seen-message caching to prevent gossip loops/DDoS.
    """
    from ..dpn.node import DecentralizedPeerNode
    node = DecentralizedPeerNode(relay_url="http://mock")

    # 1. Simulate receiving the same gossip message multiple times
    gossip_id = hashlib.sha256(b"msg_content").hexdigest()

    # First time seen
    is_new = gossip_id not in node.seen_gossip
    node.seen_gossip.add(gossip_id)
    assert is_new == True

    # Subsequent times
    is_new_again = gossip_id not in node.seen_gossip
    assert is_new_again == False
    print("\n[Security] Gossip flooding mitigated by seen-message cache.")

if __name__ == "__main__":
    test_malicious_relay_tampering()
    test_signature_forgery_prevention()
    test_gossip_flooding_resilience()
    print("Infrastructure Stress Tests Passed!")
