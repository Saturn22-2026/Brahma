package com.brahmnetwork.wallet.sentinel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySession
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.util.concurrent.ConcurrentHashMap

/**
 * Wi-Fi Aware transport — imported from MY NETWORK
 * (`com.sentinal.sdk.transport.WifiAwareTransport`).
 */
@RequiresApi(Build.VERSION_CODES.O)
class WifiAwareTransport(
    private val context: Context,
    private val gson: Gson = Gson()
) : Transport {
    override val name: String = "WiFiAware"
    override var onPacketReceived: ((MeshPacket) -> Unit)? = null

    private var wifiAwareManager: WifiAwareManager? =
        context.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
    private var attachSession: WifiAwareSession? = null
    private var discoverySession: DiscoverySession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredPeers = ConcurrentHashMap<String, PeerHandle>()
    private val assembler = MeshAssembler()

    override fun isAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            (wifiAwareManager?.isAvailable == true)
    }

    override fun start() {
        if (!isAvailable() || !hasRequiredPermissions()) return
        try {
            wifiAwareManager?.attach(object : AttachCallback() {
                override fun onAttached(session: WifiAwareSession) {
                    attachSession = session
                    startDiscovery(session)
                }
            }, handler)
        } catch (_: SecurityException) {
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startDiscovery(session: WifiAwareSession) {
        val pubConfig = PublishConfig.Builder()
            .setServiceName("BrahSentinel")
            .build()
        try {
            session.publish(pubConfig, object : DiscoverySessionCallback() {
                override fun onPublishStarted(session: PublishDiscoverySession) {
                    discoverySession = session
                }

                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    try {
                        discoveredPeers["aware-${peerHandle.hashCode()}"] = peerHandle
                        val json = assembler.accept(message) ?: return
                        val packet = gson.fromJson(json, MeshPacket::class.java) ?: return
                        discoveredPeers[packet.senderId] = peerHandle
                        onPacketReceived?.invoke(packet)
                    } catch (_: Exception) {
                    }
                }
            }, handler)

            val subConfig = SubscribeConfig.Builder()
                .setServiceName("BrahSentinel")
                .build()
            session.subscribe(subConfig, object : DiscoverySessionCallback() {
                override fun onSubscribeStarted(session: SubscribeDiscoverySession) {}
                override fun onServiceDiscovered(
                    peerHandle: PeerHandle,
                    serviceSpecificInfo: ByteArray?,
                    matchFilter: MutableList<ByteArray>?
                ) {
                    discoveredPeers["aware-${peerHandle.hashCode()}"] = peerHandle
                }
            }, handler)
        } catch (_: SecurityException) {
        }
    }

    override fun stop() {
        discoverySession?.close()
        attachSession?.close()
        discoveredPeers.clear()
    }

    override fun send(packet: MeshPacket, destinationNodeId: String?) {
        val session = discoverySession ?: return
        val frames = MeshFramer.encode(gson.toJson(packet))
        val targets = if (destinationNodeId == null) {
            discoveredPeers.values
        } else {
            listOfNotNull(discoveredPeers[destinationNodeId])
        }
        targets.forEach { peer ->
            frames.forEach { frame ->
                try {
                    session.sendMessage(peer, 0, frame)
                } catch (_: Exception) {
                }
            }
        }
    }
}
