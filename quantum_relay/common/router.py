import json
import hashlib
import time

class NetworkRouter:
    """
    Wraps the cryptographic envelope in a routable transport layer.
    Centralized and Decentralized nodes read this header to route traffic,
    but CANNOT read the inner payload.
    """

    @staticmethod
    def get_node_id(public_signing_key: bytes) -> str:
        """Derives a deterministic Node ID from a public signing key using SHA3-256."""
        return hashlib.sha3_256(public_signing_key).hexdigest()[:16]

    @staticmethod
    def package_for_transport(sender_pub_signing: bytes, recipient_pub_signing: bytes, crypto_envelope: dict) -> str:
        """
        Creates the outer routing wrapper.
        """
        transport_header = {
            "version": "1.0",
            "sender_id": NetworkRouter.get_node_id(sender_pub_signing),
            "recipient_id": NetworkRouter.get_node_id(recipient_pub_signing),
            "type": "E2EE_PAYLOAD",
            "timestamp": int(time.time())
        }

        network_packet = {
            "header": transport_header,
            "payload": crypto_envelope # The output from LayeredCryptoEnvelope.encrypt()
        }

        return json.dumps(network_packet)

    @staticmethod
    def route_packet(network_packet_str: str, known_nodes: dict, local_node_id: str):
        """
        Logic used by any node (Central or Decentralized) to route a packet.
        """
        packet = json.loads(network_packet_str)
        recipient_id = packet["header"]["recipient_id"]

        # If this node is the intended recipient, drop to local decryption layer
        if recipient_id == local_node_id:
            # Note: logging instead of printing for cleaner production code in some contexts,
            # but keeping print as per provided snippet.
            print(f"[Router] Packet received for local node {local_node_id}. Passing to decryption layer.")
            return "LOCAL_DELIVERY", packet["payload"]

        # If we know the recipient in our P2P mesh, forward directly (Decentralized)
        elif recipient_id in known_nodes.get("p2p_mesh", {}):
            print(f"[Router] Routing directly to P2P peer {recipient_id}.")
            return "P2P_FORWARD", (known_nodes["p2p_mesh"][recipient_id], packet)

        # If not in P2P mesh, route through Central Relay (Centralized fallback)
        else:
            print(f"[Router] Peer {recipient_id} not in local mesh. Routing to Central Relay.")
            return "RELAY_FORWARD", (known_nodes["central_relay_url"], packet)
