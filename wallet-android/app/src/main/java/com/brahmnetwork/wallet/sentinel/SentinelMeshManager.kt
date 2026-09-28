package com.brahmnetwork.wallet.sentinel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sentinel DTN mesh manager — imported from MY NETWORK
 * (`com.sentinal.network.mesh.SentinalMeshManager`).
 *
 * Store-and-forward: buffer, gossip across every available transport,
 * re-broadcast every 60s so chain data is not stuck on one server.
 */
class SentinelMeshManager(
    private val transports: List<Transport>,
    private val nodeId: String,
    private val rateLimiter: RateLimiter = RateLimiter(),
    private val onDeliver: (MeshPacket) -> Unit = {}
) {
    private val packetBuffer = ConcurrentHashMap<String, MeshPacket>()
    private val seenPackets = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var loop: Job? = null
    val peerHint = AtomicInteger(0)

    fun start() {
        transports.forEach {
            it.onPacketReceived = { packet -> receivePacket(packet) }
            if (it.isAvailable()) it.start()
        }
        loop = scope.launch {
            while (isActive) {
                packetBuffer.values.forEach { forwardPacket(it) }
                delay(60_000)
            }
        }
    }

    fun stop() {
        loop?.cancel()
        transports.forEach { it.stop() }
    }

    fun inject(type: String, payloadJson: String) {
        val packet = MeshPacket(
            id = UUID.randomUUID().toString(),
            senderId = nodeId,
            payload = payloadJson,
            type = type
        )
        seenPackets[packet.id] = packet.timestamp
        packetBuffer[packet.id] = packet
        trim()
        forwardPacket(packet)
    }

    fun receivePacket(packet: MeshPacket) {
        if (seenPackets.containsKey(packet.id)) return
        if (packet.senderId == nodeId) return
        if (!rateLimiter.isAllowed(packet.senderId)) return
        if (packet.payload.toByteArray().size > MeshPacket.MAX_BYTES) return
        if (packet.type.equals("INVITE", ignoreCase = true)) {
            if (packet.payload.toByteArray().size > 8192) return
            if (!InviteGuard.accept(packet.payload)) return
        }
        seenPackets[packet.id] = System.currentTimeMillis()
        packetBuffer[packet.id] = packet
        trim()
        peerHint.incrementAndGet()
        onDeliver(packet)
        forwardPacket(packet)
    }

    private fun forwardPacket(packet: MeshPacket) {
        if (packet.ttl <= 0) return
        val forwarding = packet.copy(ttl = packet.ttl - 1)
        transports.filter { it.isAvailable() }.forEach { transport ->
            transport.send(forwarding, null)
        }
    }

    private fun trim() {
        if (packetBuffer.size <= 200) return
        val oldest = packetBuffer.minByOrNull { it.value.timestamp }?.key ?: return
        packetBuffer.remove(oldest)
    }
}
