import pytest
import json
import base64
import time
from ..common.router import NetworkRouter
from ..common.crypto import LayeredCryptoEnvelope

def test_onion_routing_logic():
    """
    Test that the NetworkRouter can wrap and unwrap onion layers correctly.
    """
    # 1. Setup Keys and IDs
    alice_keys = LayeredCryptoEnvelope.generate_user_keypairs()
    bob_keys = LayeredCryptoEnvelope.generate_user_keypairs()
    relay_peer_keys = LayeredCryptoEnvelope.generate_user_keypairs()

    alice_id = NetworkRouter.get_node_id(alice_keys['public']['signing'])
    bob_id = NetworkRouter.get_node_id(bob_keys['public']['signing'])
    relay_peer_id = NetworkRouter.get_node_id(relay_peer_keys['public']['signing'])

    # 2. Alice creates a message for Bob
    envelope = LayeredCryptoEnvelope.encrypt(
        "Hello Bob!", alice_keys, bob_keys['public']['exchange'], aad=b"v1"
    )

    base_packet = NetworkRouter.package_for_transport(
        alice_keys['public']['signing'],
        bob_keys['public']['signing'],
        envelope
    )

    # 3. Alice wraps it in an onion layer for RelayPeer
    onion_packet_json = NetworkRouter.wrap_onion_layer(relay_peer_id, base_packet)

    packet = json.loads(onion_packet_json)
    assert packet["header"]["type"] == "ONION_PACKET"
    assert packet["header"]["recipient_id"] == relay_peer_id
    assert packet["header"]["sender_id"] == "ANONYMOUS"

    # 4. RelayPeer receives and routes (unwraps)
    known_nodes = {"p2p_mesh": {}, "central_relay_url": "http://relay"}
    action, payload = NetworkRouter.route_packet(onion_packet_json, known_nodes, relay_peer_id)

    assert action == "UNWRAP_ONION"

    # 5. Route the unwrapped packet
    inner_packet_json = json.dumps(payload)
    action2, target = NetworkRouter.route_packet(inner_packet_json, known_nodes, bob_id)

    assert action2 == "LOCAL_DELIVERY"
    assert target["ciphertext"] == envelope["ciphertext"]

if __name__ == "__main__":
    test_onion_routing_logic()
    print("Onion Routing Test Passed!")
