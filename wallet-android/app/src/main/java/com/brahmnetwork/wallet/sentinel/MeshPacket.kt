package com.brahmnetwork.wallet.sentinel

/**
 * Sentinel DAO mesh packet — imported from MY NETWORK
 * (`com.sentinal.network.mesh.MeshPacket`).
 */
data class MeshPacket(
    val id: String = "",
    val senderId: String = "",
    val payload: String = "",
    var ttl: Int = 10,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "DATA"
) {
    companion object {
        const val MAX_BYTES = 2 * 1024 * 1024
        val TYPES = setOf(
            "DATA", "BLOCK", "TRANSACTION", "ROUTING_UPDATE", "HELLO", "STATE", "SYNC", "INVITE"
        )
    }
}

interface Transport {
    val name: String
    var onPacketReceived: ((MeshPacket) -> Unit)?
    fun start()
    fun stop()
    fun send(packet: MeshPacket, destinationNodeId: String?)
    fun isAvailable(): Boolean
}

enum class TransportType {
    WIFI_AWARE,
    BLUETOOTH_MESH,
    TCP_LAN,
    MICROWAVE,
    PLC,
    SATELLITE
}
