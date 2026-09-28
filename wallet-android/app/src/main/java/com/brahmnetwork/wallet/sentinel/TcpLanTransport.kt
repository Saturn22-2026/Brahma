package com.brahmnetwork.wallet.sentinel

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * LAN TCP + UDP discovery transport for Sentinel DTN.
 * HELLO packets must carry a Dilithium3 signature over BRAHHELLO|{node}|{port}|{ts}.
 */
class TcpLanTransport(
    private val nodeId: String,
    private val listenPort: Int,
    private val seeds: List<Pair<String, Int>>,
    private val identity: MeshIdentity,
    private val udpPort: Int = 18547,
    private val gson: Gson = Gson(),
    var onRpcDiscovered: ((host: String, rpcPort: Int) -> Unit)? = null
) : Transport {
    override val name: String = "TcpLan"
    override var onPacketReceived: ((MeshPacket) -> Unit)? = null

    @Volatile
    private var running = false
    private val peers = java.util.concurrent.CopyOnWriteArraySet<Pair<String, Int>>()

    override fun isAvailable(): Boolean = true

    override fun start() {
        running = true
        seeds.forEach { peers.add(it) }
        thread(name = "brah-mesh-tcp", isDaemon = true) { listenTcp() }
        thread(name = "brah-mesh-udp", isDaemon = true) { listenUdp() }
        thread(name = "brah-mesh-hello", isDaemon = true) {
            while (running) {
                sendUdpHello()
                Thread.sleep(15_000)
            }
        }
    }

    fun addPeer(host: String, port: Int) {
        if (host.isBlank() || port <= 0) return
        peers.add(host to port)
    }

    override fun stop() {
        running = false
    }

    override fun send(packet: MeshPacket, destinationNodeId: String?) {
        val line = gson.toJson(packet) + "\n"
        val bytes = line.toByteArray(Charsets.UTF_8)
        for ((host, port) in peers.toList()) {
            try {
                Socket().use { sock ->
                    sock.connect(InetSocketAddress(host, port), 2000)
                    sock.getOutputStream().write(bytes)
                    sock.getOutputStream().flush()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun listenTcp() {
        try {
            ServerSocket().use { server ->
                server.reuseAddress = true
                server.bind(InetSocketAddress(listenPort))
                while (running) {
                    val client = server.accept()
                    thread(isDaemon = true) { handleClient(client) }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun handleClient(sock: Socket) {
        try {
            sock.soTimeout = 8000
            val text = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8)).readLine() ?: return
            val packet = gson.fromJson(text, MeshPacket::class.java) ?: return
            val remote = sock.inetAddress.hostAddress ?: return
            if (packet.type.equals("HELLO", ignoreCase = true)) {
                acceptHelloJson(packet.payload, remote)
                return
            }
            onPacketReceived?.invoke(packet)
        } catch (_: Exception) {
        } finally {
            try {
                sock.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun listenUdp() {
        try {
            DatagramSocket(null).use { sock ->
                sock.reuseAddress = true
                sock.bind(InetSocketAddress(udpPort))
                val buf = ByteArray(4096)
                while (running) {
                    val dg = DatagramPacket(buf, buf.size)
                    sock.receive(dg)
                    val text = String(dg.data, 0, dg.length, Charsets.UTF_8)
                    acceptHelloJson(text, dg.address.hostAddress)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun acceptHelloJson(raw: String, host: String?) {
        if (host.isNullOrBlank()) return
        try {
            val obj = gson.fromJson(raw, JsonObject::class.java) ?: return
            val port = obj.get("tcpPort")?.asInt ?: obj.get("port")?.asInt ?: return
            val node = obj.get("node")?.asString ?: obj.get("senderId")?.asString ?: return
            val pk = obj.get("public_key")?.asString ?: return
            val sig = obj.get("signature")?.asString ?: return
            val ts = obj.get("ts")?.asLong ?: return
            if (node == nodeId) return
            if (identity.verify(node, port, pk, sig, ts)) {
                peers.add(host to port)
                val rpcPort = obj.get("rpcPort")?.asInt ?: 8545
                if (rpcPort in 1..65535) {
                    onRpcDiscovered?.invoke(host, rpcPort)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun sendUdpHello() {
        try {
            val body = identity.fields(nodeId, listenPort).toMutableMap()
            body["type"] = "HELLO"
            val msg = gson.toJson(body).toByteArray(Charsets.UTF_8)
            DatagramSocket().use { sock ->
                sock.broadcast = true
                sock.send(DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), udpPort))
            }
        } catch (_: Exception) {
        }
    }
}
