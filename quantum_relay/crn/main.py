from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from typing import List, Dict
import base64
from ..common.protocol import QuantumTransportPacket
from ..common.utils import get_pubkey_hash

app = FastAPI(title="Quantum Relay Node (CRN)")

# In-memory stores (to be replaced by Redis/PostgreSQL in Phase 3+)
public_keys: Dict[bytes, bytes] = {}          # RecipientID -> Public Key
message_queues: Dict[bytes, List[bytes]] = {} # RecipientID -> List of binary packets

class Registration(BaseModel):
    public_key: str # Base64 encoded public key

@app.get("/")
async def root():
    return {"status": "online", "nodes": len(public_keys)}

@app.post("/register")
async def register(reg: Registration):
    """Register a public key to receive messages."""
    try:
        pubkey = base64.b64decode(reg.public_key)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid base64 encoding")

    rid = get_pubkey_hash(pubkey)
    public_keys[rid] = pubkey

    if rid not in message_queues:
        message_queues[rid] = []

    return {"recipient_id": rid.hex(), "message": "Registered successfully"}

@app.post("/send")
async def send_packet(request: Request):
    """Broker an encrypted QuantumTransportPacket."""
    data = await request.body()
    try:
        packet = QuantumTransportPacket.unpack(data)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid packet format: {str(e)}")

    # Zero-Knowledge: The relay doesn't know the content, just the recipient ID.
    if packet.recipient_id not in message_queues:
        message_queues[packet.recipient_id] = []

    message_queues[packet.recipient_id].append(data)
    return {"status": "delivered_to_queue", "recipient": packet.recipient_id.hex()}

@app.get("/messages/{recipient_id_hex}")
async def get_messages(recipient_id_hex: str):
    """Retrieve offline messages for a recipient."""
    try:
        rid = bytes.fromhex(recipient_id_hex)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid hex recipient_id")

    messages = message_queues.get(rid, [])
    # In a real system, we'd wait for an ACK before clearing, but for now we clear on read.
    message_queues[rid] = []

    return {
        "count": len(messages),
        "packets": [base64.b64encode(m).decode('utf-8') for m in messages]
    }
