import pytest
import asyncio
import base64
import pickle
from httpx import AsyncClient
from ..crn.main import app
from ..dpn.node import DecentralizedPeerNode
from ..common.protocol import QuantumTransportPacket
from ..common.crypto import LayeredCryptoEnvelope

@pytest.mark.asyncio
async def test_quantum_relay_e2e():
    """
    Test End-to-End: Alice sends a message to Bob via the Relay (CRN).
    """
    async with AsyncClient(app=app, base_url="http://test") as client:
        # 1. Setup Alice and Bob (DPNs)
        alice = DecentralizedPeerNode(relay_url="http://test")
        bob = DecentralizedPeerNode(relay_url="http://test")

        # 2. Bob registers with the relay
        pub_b64 = base64.b64encode(bob.keys['public']['exchange']).decode('utf-8')
        await client.post("/register", json={"public_key": pub_b64})

        # 3. Alice "knows" Bob's IDs and public keys (out-of-band or lookup)
        bob_id = bob.id
        bob_exchange_pub = bob.keys['public']['exchange']

        # 4. Alice sends message
        message = "Quantum checkmate!"
        # Encrypt
        envelope_dict = LayeredCryptoEnvelope.encrypt(
            message, alice.keys, bob_exchange_pub, aad=b"v1"
        )
        payload = pickle.dumps(envelope_dict)
        packet = QuantumTransportPacket(
            msg_type=QuantumTransportPacket.TYPE_DIRECT_MESSAGE,
            recipient_id=bob_id,
            sender_id=alice.id,
            payload=payload
        )

        # Send packet to relay
        resp = await client.post("/send", content=packet.pack())
        assert resp.status_code == 200
        assert resp.json()["status"] == "delivered_to_queue"

        # 5. Bob polls messages from relay
        resp = await client.get(f"/messages/{bob.id.hex()}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["count"] == 1

        # 6. Bob decrypts
        raw_packet = base64.b64decode(data["packets"][0])
        recv_packet = QuantumTransportPacket.unpack(raw_packet)
        assert recv_packet.sender_id == alice.id

        recv_envelope = pickle.loads(recv_packet.payload)

        # Bob needs Alice's signing pub to verify
        decrypted = LayeredCryptoEnvelope.decrypt(
            recv_envelope, bob.keys, alice.keys['public']['signing']
        )

        assert decrypted == message
        print(f"\n[Test] Alice said: '{decrypted}' - Quantum Integrity Verified.")

if __name__ == "__main__":
    asyncio.run(test_quantum_relay_e2e())
