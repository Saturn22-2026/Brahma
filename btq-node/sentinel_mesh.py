"""Sentinel DAO mesh protocols (from MY NETWORK / Sentinal).

Delay-tolerant store-and-forward: buffer packets, gossip across transports,
re-broadcast on a timer, drop duplicates / oversized / rate-limited senders.

Packet types match sentinal-mesh MeshPacket.kt:
  DATA, BLOCK, TRANSACTION, ROUTING_UPDATE, HELLO
plus BRAHMNETWORK sync:
  STATE, SYNC
plus wallet invite links:
  INVITE
"""
from __future__ import annotations

import json
import threading
import time
import uuid
from typing import Callable

MAX_PAYLOAD_BYTES = 2 * 1024 * 1024
DEFAULT_TTL = 10
REGOSSIP_SECONDS = 60
RATE_MAX_PACKETS = 100
RATE_WINDOW_MS = 60_000


class RateLimiter:
    """Sliding window limiter from sentinal-mesh RateLimiter.kt."""

    def __init__(self, max_packets: int = RATE_MAX_PACKETS, window_ms: int = RATE_WINDOW_MS):
        self.max_packets = max_packets
        self.window_ms = window_ms
        self._windows: dict[str, tuple[int, int]] = {}
        self._lock = threading.Lock()

    def is_allowed(self, node_id: str) -> bool:
        now = int(time.time() * 1000)
        with self._lock:
            start, count = self._windows.get(node_id, (now, 0))
            if now - start > self.window_ms:
                start, count = now, 0
            if count >= self.max_packets:
                self._windows[node_id] = (start, count)
                return False
            self._windows[node_id] = (start, count + 1)
            return True


def new_packet(sender_id: str, packet_type: str, payload: dict | bytes, ttl: int = DEFAULT_TTL) -> dict:
    if isinstance(payload, dict):
        raw = json.dumps(payload, separators=(",", ":")).encode("utf-8")
    else:
        raw = payload
    return {
        "id": str(uuid.uuid4()),
        "senderId": sender_id,
        "payload": raw.decode("utf-8") if isinstance(payload, dict) else raw.hex(),
        "payloadJson": payload if isinstance(payload, dict) else None,
        "ttl": ttl,
        "timestamp": int(time.time() * 1000),
        "type": packet_type,
    }


class SentinelMesh:
    def __init__(
        self,
        node_id: str,
        send_fn: Callable[[dict], None],
        on_packet: Callable[[dict], None],
        rate_limiter: RateLimiter | None = None,
    ):
        self.node_id = node_id
        self._send_fn = send_fn
        self._on_packet = on_packet
        self.rate_limiter = rate_limiter or RateLimiter()
        self.packet_buffer: dict[str, dict] = {}
        self.seen: dict[str, int] = {}
        self._lock = threading.Lock()

    def start(self) -> None:
        threading.Thread(target=self._regossip_loop, daemon=True).start()

    def receive(self, packet: dict) -> None:
        pid = str(packet.get("id") or "")
        sender = str(packet.get("senderId") or "")
        if not pid or sender == self.node_id:
            return
        with self._lock:
            if pid in self.seen:
                return
            if not self.rate_limiter.is_allowed(sender):
                return
            payload = packet.get("payloadJson")
            if payload is None:
                raw = packet.get("payload", "")
                size = len(raw.encode("utf-8") if isinstance(raw, str) else raw)
            else:
                size = len(json.dumps(payload).encode("utf-8"))
            if size > MAX_PAYLOAD_BYTES:
                return
            ptype = str(packet.get("type") or "").upper()
            if ptype == "INVITE" and size > 8192:
                return
            self.seen[pid] = int(time.time() * 1000)
            self.packet_buffer[pid] = packet
            self._trim_buffer()
        accept = True
        try:
            if self._on_packet(packet) is False:
                accept = False
        except Exception:
            accept = False
        if accept:
            self.forward(packet)
        else:
            with self._lock:
                self.packet_buffer.pop(pid, None)

    def inject(self, packet_type: str, payload: dict) -> dict:
        packet = new_packet(self.node_id, packet_type, payload)
        with self._lock:
            self.seen[packet["id"]] = packet["timestamp"]
            self.packet_buffer[packet["id"]] = packet
            self._trim_buffer()
        self.forward(packet)
        return packet

    def forward(self, packet: dict) -> None:
        ttl = int(packet.get("ttl", 0))
        if ttl <= 0:
            return
        forwarded = dict(packet)
        forwarded["ttl"] = ttl - 1
        try:
            self._send_fn(forwarded)
        except Exception:
            pass

    def _trim_buffer(self) -> None:
        while len(self.packet_buffer) > 200:
            oldest = min(self.packet_buffer, key=lambda k: int(self.packet_buffer[k].get("timestamp") or 0))
            self.packet_buffer.pop(oldest, None)

    def _regossip_loop(self) -> None:
        while True:
            time.sleep(REGOSSIP_SECONDS)
            with self._lock:
                packets = list(self.packet_buffer.values())
            for packet in packets:
                self.forward(packet)
