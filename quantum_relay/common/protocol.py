import struct

class QuantumTransportPacket:
    """
    Standardized Network Transport Wrapper for Quantum Envelopes.

    Binary Format:
    - Version (1 byte)
    - Type (1 byte)
    - RecipientID (32 bytes, SHA-256 hash of public key)
    - SenderID (32 bytes, SHA-256 hash of public key)
    - TTL (1 byte)
    - PayloadLength (4 bytes, Big-Endian)
    - Payload (Variable length, Encrypted LayeredCryptoEnvelope)
    """
    HEADER_FORMAT = "!BB32s32sBI"
    HEADER_SIZE = struct.calcsize(HEADER_FORMAT)

    # Message Types
    TYPE_REGISTRATION = 0x01
    TYPE_DIRECT_MESSAGE = 0x02
    TYPE_GOSSIP = 0x03
    TYPE_ACK = 0x04

    def __init__(self, msg_type, recipient_id, sender_id, payload, ttl=64, version=1):
        self.version = version
        self.msg_type = msg_type
        self.recipient_id = recipient_id  # 32 bytes hash
        self.sender_id = sender_id        # 32 bytes hash
        self.ttl = ttl
        self.payload = payload

    def pack(self) -> bytes:
        """Serializes the packet into binary format."""
        header = struct.pack(
            self.HEADER_FORMAT,
            self.version,
            self.msg_type,
            self.recipient_id,
            self.sender_id,
            self.ttl,
            len(self.payload)
        )
        return header + self.payload

    @classmethod
    def unpack(cls, data: bytes):
        """Deserializes binary data into a QuantumTransportPacket object."""
        if len(data) < cls.HEADER_SIZE:
            raise ValueError("Data too short for header")

        version, msg_type, recipient_id, sender_id, ttl, payload_len = struct.unpack(
            cls.HEADER_FORMAT,
            data[:cls.HEADER_SIZE]
        )

        payload = data[cls.HEADER_SIZE : cls.HEADER_SIZE + payload_len]
        if len(payload) < payload_len:
            # We don't raise error here because we might be reading from a stream
            # but for this utility, we assume the buffer contains the full packet.
            raise ValueError(f"Payload length mismatch: expected {payload_len}, got {len(payload)}")

        return cls(msg_type, recipient_id, sender_id, payload, ttl, version)

    def __repr__(self):
        return f"<QuantumTransportPacket type={self.msg_type} recipient={self.recipient_id.hex()[:8]}... len={len(self.payload)}>"
