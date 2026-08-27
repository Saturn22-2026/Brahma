import pytest
import asyncio
import json
import base64
from httpx import AsyncClient
from ..crn.main import app
from ..dpn.node import DecentralizedPeerNode
from ..common.payments import Currency, TransactionStatus

@pytest.mark.asyncio
async def test_end_to_end_payment_flow():
    """
    Test: Alice (Customer) sends a Payment Request to Bob (Merchant).
    Bob processes it and Alice receives the SUCCESS result.
    """
    async with AsyncClient(app=app, base_url="http://test") as client:
        # 1. Setup Alice and Bob
        alice = DecentralizedPeerNode(relay_url="http://test")
        bob = DecentralizedPeerNode(relay_url="http://test")

        # 2. Register both on Relay
        await client.post("/register", json={
            "public_key": base64.b64encode(alice.keys['public']['exchange']).decode('utf-8'),
            "signing_public_key": base64.b64encode(alice.keys['public']['signing']).decode('utf-8')
        })
        await client.post("/register", json={
            "public_key": base64.b64encode(bob.keys['public']['exchange']).decode('utf-8'),
            "signing_public_key": base64.b64encode(bob.keys['public']['signing']).decode('utf-8')
        })

        # 3. Simulate Alice learning Bob's keys (via Relay PKI)
        resp = await client.get("/pki")
        relay_pki = resp.json()
        alice.pki[bob.id] = {
            'exchange_pub': base64.b64decode(relay_pki[bob.id]['exchange_pub']),
            'signing_pub': base64.b64decode(relay_pki[bob.id]['signing_pub'])
        }
        bob.pki[alice.id] = {
            'exchange_pub': base64.b64decode(relay_pki[alice.id]['exchange_pub']),
            'signing_pub': base64.b64decode(relay_pki[alice.id]['signing_pub'])
        }

        # 4. Alice sends a Payment Request for 50.00 USD
        await alice.send_payment_request(bob.id, 5000, "USD", "Test Purchase")

        # 5. Bob polls messages and processes the payment
        resp = await client.get(f"/messages/{bob.id}")
        data = resp.json()
        assert data["count"] == 1

        # Bob handles incoming message (this internally calls _handle_incoming_envelope)
        packet_json = json.dumps(data["packets"][0])
        await bob._dispatch_routed_packet(packet_json)

        # 6. Alice polls messages to receive the result
        resp = await client.get(f"/messages/{alice.id}")
        data = resp.json()
        assert data["count"] == 1

        # Mock Alice processing the result
        packet = data["packets"][0]
        envelope = packet["payload"]

        plaintext = alice.decrypt_raw_envelope(envelope, bob.id)
        result_cmd = json.loads(plaintext)

        assert result_cmd["type"] == "PAYMENT_RESULT"
        assert result_cmd["data"]["status"] == "SUCCESS"
        assert result_cmd["data"]["provider"] == "Stripe-Mock"
        print(f"\n[Test] Payment Successful! ID: {result_cmd['data']['provider_transaction_id']}")

# Monkeypatch for testing decryption easily in Alice's context
def decrypt_raw_envelope(self, envelope, sender_id):
    from ..common.crypto import LayeredCryptoEnvelope
    return LayeredCryptoEnvelope.decrypt(
        payload=envelope,
        recipient_keys=self.keys,
        sender_public_signing=self.pki[sender_id]['signing_pub']
    )

DecentralizedPeerNode.decrypt_raw_envelope = decrypt_raw_envelope

if __name__ == "__main__":
    asyncio.run(test_end_to_end_payment_flow())
