import asyncio
import httpx
import base64
import json
import pickle
from ..common.crypto import LayeredCryptoEnvelope
from ..common.protocol import QuantumTransportPacket
from ..common.utils import get_pubkey_hash

class DecentralizedPeerNode:
    def __init__(self, relay_url: str):
        self.relay_url = relay_url
        self.keys = LayeredCryptoEnvelope.generate_user_keypairs()
        # Our ID is the hash of our exchange public key
        self.id = get_pubkey_hash(self.keys['public']['exchange'])
        self.signing_id = get_pubkey_hash(self.keys['public']['signing'])
        print(f"Initialized DPN. ID: {self.id.hex()[:16]}...")

    async def register(self):
        """Register our public keys with the CRN."""
        async with httpx.AsyncClient() as client:
            # We register our exchange key so others can encrypt for us
            pub_b64 = base64.b64encode(self.keys['public']['exchange']).decode('utf-8')
            resp = await client.post(f"{self.relay_url}/register", json={"public_key": pub_b64})
            if resp.status_code == 200:
                print(f"Registration successful. Registered ID: {resp.json()['recipient_id']}")
            else:
                print(f"Registration failed: {resp.text}")

    async def send_quantum_message(self, recipient_id: bytes, recipient_exchange_pub: bytes, message: str, sender_signing_pub: bytes):
        """Encrypt, wrap and send a message via the relay."""
        # 1. Create the Layered Envelope
        # We include our signing pub in the AAD or similar if needed,
        # but here we pass it as a separate param for the crypto logic.
        envelope_dict = LayeredCryptoEnvelope.encrypt(
            plaintext=message,
            sender_keys=self.keys,
            recipient_public_exchange=recipient_exchange_pub,
            aad=b"v1" # Version binding
        )

        # 2. Serialize the envelope
        # In a real system, we'd use a strict binary format for the envelope too.
        # For this phase, we use pickle for convenience.
        payload = pickle.dumps(envelope_dict)

        # 3. Create the Transport Packet
        packet = QuantumTransportPacket(
            msg_type=QuantumTransportPacket.TYPE_DIRECT_MESSAGE,
            recipient_id=recipient_id,
            sender_id=self.id,
            payload=payload
        )

        # 4. Dispatch to Relay
        async with httpx.AsyncClient() as client:
            resp = await client.post(f"{self.relay_url}/send", content=packet.pack())
            if resp.status_code == 200:
                print(f"Packet dispatched to relay for {recipient_id.hex()[:16]}...")
            else:
                print(f"Dispatch failed: {resp.text}")

    async def fetch_and_decrypt_messages(self, sender_signing_pubs: dict):
        """Poll the relay for messages and decrypt them."""
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{self.relay_url}/messages/{self.id.hex()}")
            if resp.status_code != 200:
                print("Failed to poll messages")
                return

            data = resp.json()
            if data['count'] == 0:
                return

            print(f"Fetched {data['count']} new packets.")
            for p_b64 in data['packets']:
                raw_packet = base64.b64decode(p_b64)
                packet = QuantumTransportPacket.unpack(raw_packet)

                # Recover the envelope
                envelope_dict = pickle.loads(packet.payload)

                # Find the sender's signing key (must be known or provided)
                sender_id = packet.sender_id
                if sender_id in sender_signing_pubs:
                    try:
                        plaintext = LayeredCryptoEnvelope.decrypt(
                            payload=envelope_dict,
                            recipient_keys=self.keys,
                            sender_public_signing=sender_signing_pubs[sender_id]
                        )
                        print(f"[*] Decrypted message from {sender_id.hex()[:8]}: {plaintext}")
                    except ValueError as e:
                        print(f"[!] Decryption failed for packet from {sender_id.hex()}: {e}")
                else:
                    print(f"[?] Received message from unknown sender {sender_id.hex()}. Missing signing key.")
