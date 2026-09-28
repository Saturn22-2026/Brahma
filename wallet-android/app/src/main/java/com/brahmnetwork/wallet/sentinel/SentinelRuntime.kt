package com.brahmnetwork.wallet.sentinel

import android.content.Context
import android.os.Build
import com.brahmnetwork.wallet.Constants
import com.brahmnetwork.wallet.CryptoManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicReference

data class MeshInvite(
    val ref: String,
    val claim: String,
    val apk: String
)

/** Starts Sentinel DTN + DAO (Wi-Fi Aware) so invite links gossip off a single phone. */
object SentinelRuntime {
    private val gson = Gson()
    private val managerRef = AtomicReference<SentinelMeshManager?>(null)
    private val tcpRef = AtomicReference<TcpLanTransport?>(null)
    private val discoveredRpc = CopyOnWriteArraySet<String>()
    private val _lastInvite = MutableStateFlow<MeshInvite?>(null)
    val lastInvite: StateFlow<MeshInvite?> = _lastInvite.asStateFlow()
    private val _queued = MutableStateFlow(0)
    val queuedTx: StateFlow<Int> = _queued.asStateFlow()
    @Volatile
    var onTransaction: ((String) -> Unit)? = null
    private var queue: MeshTxQueue? = null

    fun discoveredRpcUrls(): List<String> = discoveredRpc.toList()

    fun rememberRpcHost(host: String, rpcPort: Int = 8545) {
        if (host.isBlank() || rpcPort !in 1..65535) return
        if (host == "127.0.0.1" || host == "localhost") return
        val url = "http://$host:$rpcPort/"
        if (CryptoManager.isAllowedRpcUrl(url, true)) {
            discoveredRpc.add(url)
        }
    }

    val manager: SentinelMeshManager?
        get() = managerRef.get()

    fun start(context: Context, nodeId: String) {
        InviteGuard.allowedRpc = prefsRpc(context).ifBlank { Constants.RPC_URL }
        if (managerRef.get() != null) {
            addRpcSeed(prefsRpc(context))
            return
        }
        val urls = (listOf(prefsRpc(context)) + Constants.rpcUrls).filter { it.isNotBlank() }
        val seeds = urls.mapNotNull { url ->
            try {
                val host = URI(url).host ?: return@mapNotNull null
                host to Constants.MESH_PORT
            } catch (_: Exception) {
                null
            }
        }.distinct()
        val identity = MeshIdentity.load(context.applicationContext)
        queue = MeshTxQueue.from(context.applicationContext)
        _queued.value = queue?.size() ?: 0
        val tcp = TcpLanTransport(nodeId, Constants.MESH_PORT, seeds, identity)
        tcp.onRpcDiscovered = { host, rpcPort -> rememberRpcHost(host, rpcPort) }
        try {
            val lanHost = URI(Constants.LAN_RPC_URL).host
            if (!lanHost.isNullOrBlank()) tcp.addPeer(lanHost, Constants.MESH_PORT)
        } catch (_: Exception) {
        }
        val transports = mutableListOf<Transport>(tcp)
        transports.add(BluetoothTransport(context.applicationContext))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            transports.add(WifiAwareTransport(context.applicationContext))
        }
        val mesh = SentinelMeshManager(transports, nodeId, onDeliver = ::onDeliver)
        if (managerRef.compareAndSet(null, mesh)) {
            tcpRef.set(tcp)
            mesh.start()
        }
    }

    fun addRpcSeed(rpcUrl: String) {
        if (rpcUrl.isNotBlank()) {
            InviteGuard.allowedRpc = rpcUrl
        }
        val host = try {
            URI(rpcUrl.trim()).host
        } catch (_: Exception) {
            null
        } ?: return
        tcpRef.get()?.addPeer(host, Constants.MESH_PORT)
    }

    fun gossipTransaction(txJson: String): Boolean {
        val mgr = managerRef.get() ?: return false
        mgr.inject("TRANSACTION", txJson)
        queue?.enqueue(txJson)
        _queued.value = queue?.size() ?: 0
        return true
    }

    fun queuedTransactions(): List<String> = queue?.snapshot().orEmpty()

    fun dropQueued(txJson: String) {
        queue?.remove(txJson)
        _queued.value = queue?.size() ?: 0
    }

    fun gossipBlock(block: Map<String, Any>): Boolean {
        val mgr = managerRef.get() ?: return false
        mgr.inject("BLOCK", gson.toJson(block))
        return true
    }

    fun gossipInvite(fields: Map<String, String>): Boolean {
        val mgr = managerRef.get() ?: return false
        val payload = gson.toJson(fields)
        if (!InviteGuard.accept(payload)) return false
        mgr.inject("INVITE", payload)
        return true
    }

    fun stop() {
        managerRef.getAndSet(null)?.stop()
        tcpRef.set(null)
    }

    private fun onDeliver(packet: MeshPacket) {
        when {
            packet.type.equals("INVITE", ignoreCase = true) -> {
                if (!InviteGuard.accept(packet.payload)) return
                try {
                    val o = gson.fromJson(packet.payload, JsonObject::class.java) ?: return
                    _lastInvite.value = MeshInvite(
                        o.get("ref").asString,
                        o.get("claim").asString,
                        o.get("apk")?.asString.orEmpty()
                    )
                } catch (_: Exception) {
                }
            }
            packet.type.equals("TRANSACTION", ignoreCase = true) -> {
                queue?.enqueue(packet.payload)
                _queued.value = queue?.size() ?: 0
                onTransaction?.invoke(packet.payload)
            }
        }
    }

    private fun prefsRpc(context: Context): String {
        return context.getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
            .getString("rpc_url", "")
            .orEmpty()
    }
}
