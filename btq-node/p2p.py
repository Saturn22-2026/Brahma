"""TCP + UDP gossip using Sentinel DAO DTN mesh (not a single-server store)."""
from __future__ import annotations

import hashlib
import json
import socket
import threading
import time
from pathlib import Path
from typing import Callable

from crypto_mldsa import sign_dilithium3, hello_message, verify_hello
from sentinel_mesh import SentinelMesh

UDP_PORT = 18547
HELLO_LIVE_SEC = 90
# Bootstrap only. Learned HELLO peers are kept on disk and dialed after this.
BUILTIN_SEEDS = ("84.12.123.117:18545",)


def parse_peer(seed: str) -> tuple[str, int] | None:
    host, sep, port = (seed or "").strip().partition(":")
    if not sep or not host or not port.isdigit():
        return None
    number = int(port)
    if number <= 0 or number > 65535:
        return None
    return host, number


def protocol_id() -> str:
    """Short hash of the locked rule file. Advertised so a different rule set is visible."""
    raw = hashlib.sha256((Path(__file__).with_name("ai_council.py")).read_bytes()).hexdigest()
    return raw[:16]


def is_local_peer(host: str) -> bool:
    h = (host or "").strip().lower()
    return h in ("127.0.0.1", "::1", "localhost", "0.0.0.0") or h.startswith("fe80:") or h.startswith("169.254.")


class PeerNet:
    def __init__(
        self,
        listen_port: int,
        seeds: list[str],
        node_id: str,
        on_packet: Callable[[dict], None],
        identity: tuple[bytes, bytes] | None = None,
        listen_host: str = "127.0.0.1",
        rpc_port: int = 8545,
        udp_port: int | None = None,
        store_path: str | None = None,
    ):
        self.listen_port = listen_port
        self.rpc_port = int(rpc_port)
        self.udp_port = int(udp_port) if udp_port else listen_port + 2
        self.listen_host = "127.0.0.1" if listen_host in ("localhost",) else listen_host
        self.node_id = node_id
        self._on_packet = on_packet
        self._pk, self._sk = identity if identity else (b"", b"")
        self.seeds: set[tuple[str, int]] = set()
        self.peers: set[tuple[str, int]] = set()
        self.live: dict[tuple[str, int], float] = {}
        self.store_path = store_path
        self._add_seeds(list(BUILTIN_SEEDS) + list(seeds))
        self._load_store()
        self.mesh = SentinelMesh(node_id, self._wire_send, self._handle_mesh)

    def _add_seeds(self, seeds: list[str]) -> None:
        for seed in seeds:
            pair = parse_peer(seed)
            if not pair:
                continue
            self.seeds.add(pair)
            self.peers.add(pair)

    def start(self) -> None:
        self.mesh.start()
        threading.Thread(target=self._listen_tcp, daemon=True).start()
        threading.Thread(target=self._listen_udp, daemon=True).start()
        threading.Thread(target=self._dial_loop, daemon=True).start()

    def seed_count(self) -> int:
        return len(self.seeds)

    def peer_count(self) -> int:
        """Authenticated HELLO peers still inside the 90s window. Seeds do not count."""
        now = time.time()
        expired = [pair for pair, ts in self.live.items() if now - ts > HELLO_LIVE_SEC]
        for pair in expired:
            self.live.pop(pair, None)
        return len(self.live)

    def independent_peer_count(self) -> int:
        """Live HELLO peers that are not loopback / link-local (not a same-box fake peer)."""
        self.peer_count()
        n = 0
        for host, _port in self.live:
            h = (host or "").strip().lower()
            if h in ("127.0.0.1", "::1", "localhost", "0.0.0.0"):
                continue
            if h.startswith("fe80:") or h.startswith("169.254."):
                continue
            n += 1
        return n

    def broadcast(self, packet_type: str, payload: dict) -> None:
        self.mesh.inject(packet_type, payload)

    def _hello_fields(self) -> dict:
        ts = int(time.time())
        fields = {
            "tcpPort": self.listen_port,
            "rpcPort": self.rpc_port,
            "node": self.node_id,
            "senderId": self.node_id,
            "ts": ts,
            "protocol": protocol_id(),
        }
        if self._pk and self._sk:
            sig = sign_dilithium3(self._sk, hello_message(self.node_id, self.listen_port, ts))
            fields["public_key"] = self._pk.hex()
            fields["signature"] = sig.hex()
            fields["algo"] = "Dilithium3"
        return fields

    def _accept_hello(self, payload: dict, host: str, *, persist: bool = True) -> bool:
        if not verify_hello(payload):
            return False
        node = str(payload.get("node") or payload.get("senderId") or "")
        if node == self.node_id:
            return False
        port = int(payload.get("tcpPort") or payload.get("port") or 0)
        if port <= 0 or port > 65535:
            return False
        if len(self.peers) >= 64 and (host, port) not in self.peers:
            return False
        pair = (host, port)
        fresh = pair not in self.peers and not is_local_peer(host)
        self.peers.add(pair)
        self.live[pair] = time.time()
        advertised = str(payload.get("protocol") or "")
        if advertised and advertised != protocol_id():
            print(f"peer {host}:{port} protocol {advertised} != {protocol_id()}", flush=True)
        if fresh and persist:
            self._save_store()
        return True

    def _load_store(self) -> None:
        if not self.store_path:
            return
        try:
            with open(self.store_path, "r", encoding="utf-8") as fh:
                lines = fh.read().splitlines()
        except OSError:
            return
        for line in lines:
            pair = parse_peer(line)
            if pair and not is_local_peer(pair[0]):
                self.peers.add(pair)

    def _save_store(self) -> None:
        if not self.store_path:
            return
        kept = [f"{host}:{port}" for host, port in sorted(self.peers) if not is_local_peer(host)]
        try:
            with open(self.store_path, "w", encoding="utf-8") as fh:
                fh.write("\n".join(kept) + ("\n" if kept else ""))
        except OSError:
            return

    def _handle_mesh(self, packet: dict) -> None:
        self._on_packet(packet)

    def _wire_send(self, packet: dict) -> None:
        line = json.dumps(packet).encode("utf-8") + b"\n"
        for host, port in list(self.peers):
            try:
                with socket.create_connection((host, port), timeout=2) as sock:
                    sock.sendall(line)
            except OSError:
                pass

    def _udp_hello(self) -> None:
        msg = json.dumps({"type": "HELLO", **self._hello_fields()}).encode("utf-8")
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            sock.sendto(msg, ("255.255.255.255", self.udp_port))
            sock.close()
        except OSError:
            pass

    def _listen_tcp(self) -> None:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((self.listen_host, self.listen_port))
        sock.listen(64)
        while True:
            conn, addr = sock.accept()
            threading.Thread(target=self._handle_tcp, args=(conn, addr), daemon=True).start()

    def _extract_payload(self, msg: dict):
        payload = msg.get("payloadJson")
        if isinstance(payload, dict):
            return payload
        raw = msg.get("payload")
        if isinstance(raw, dict):
            return raw
        if isinstance(raw, str) and raw.startswith("{"):
            try:
                parsed = json.loads(raw)
                if isinstance(parsed, dict):
                    return parsed
            except json.JSONDecodeError:
                return None
        return None

    def _handle_tcp(self, conn: socket.socket, addr: tuple) -> None:
        try:
            data = b""
            while True:
                chunk = conn.recv(65536)
                if not chunk:
                    break
                data += chunk
                if len(data) > 256 * 1024:
                    return
                if b"\n" in data:
                    break
            if not data:
                return
            msg = json.loads(data.decode("utf-8").split("\n", 1)[0])
            ptype = str(msg.get("type", "")).upper()
            if "senderId" in msg and "type" in msg and ("payload" in msg or "payloadJson" in msg):
                if ptype == "HELLO":
                    payload = self._extract_payload(msg)
                    if isinstance(payload, dict):
                        self._accept_hello(payload, addr[0])
                    return
                self.mesh.receive(msg)
            else:
                if ptype == "BLOCK" and "block" in msg:
                    self.mesh.receive({
                        "id": msg.get("id") or f"legacy-{time.time()}",
                        "senderId": msg.get("senderId") or addr[0],
                        "payloadJson": msg["block"],
                        "payload": json.dumps(msg["block"]),
                        "ttl": 10,
                        "timestamp": int(time.time() * 1000),
                        "type": "BLOCK",
                    })
                elif ptype == "HELLO":
                    self._accept_hello(msg, addr[0])
        except Exception:
            pass
        finally:
            conn.close()

    def _listen_udp(self) -> None:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind((self.listen_host, self.udp_port))
        except OSError:
            return
        while True:
            try:
                data, addr = sock.recvfrom(4096)
                msg = json.loads(data.decode("utf-8"))
                self._accept_hello(msg, addr[0], persist=False)
            except Exception:
                pass

    def _dial_loop(self) -> None:
        while True:
            self.mesh.inject("HELLO", self._hello_fields())
            self._udp_hello()
            time.sleep(15)
