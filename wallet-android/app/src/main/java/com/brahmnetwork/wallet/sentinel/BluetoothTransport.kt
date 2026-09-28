package com.brahmnetwork.wallet.sentinel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.thread

/**
 * Bluetooth Classic RFCOMM transport for DTN when Wi‑Fi and cellular are gone.
 * Dilithium txs are too large for BLE; RFCOMM carries newline JSON like TCP LAN.
 */
class BluetoothTransport(
    private val context: Context,
    private val gson: Gson = Gson()
) : Transport {
    override val name: String = "Bluetooth"
    override var onPacketReceived: ((MeshPacket) -> Unit)? = null

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    @Volatile
    private var running = false
    private val sockets = CopyOnWriteArraySet<BluetoothSocket>()
    private var server: BluetoothServerSocket? = null

    override fun isAvailable(): Boolean {
        return adapter?.isEnabled == true && hasPermission()
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (!isAvailable() || running) return
        running = true
        thread(name = "brah-bt-listen", isDaemon = true) { listen() }
        thread(name = "brah-bt-dial", isDaemon = true) { dialBonded() }
    }

    override fun stop() {
        running = false
        try {
            server?.close()
        } catch (_: Exception) {
        }
        sockets.forEach { sock ->
            try {
                sock.close()
            } catch (_: Exception) {
            }
        }
        sockets.clear()
    }

    @SuppressLint("MissingPermission")
    override fun send(packet: MeshPacket, destinationNodeId: String?) {
        if (!running || !hasPermission()) return
        val line = gson.toJson(packet) + "\n"
        val bytes = line.toByteArray(Charsets.UTF_8)
        sockets.toList().forEach { sock ->
            try {
                sock.outputStream.write(bytes)
                sock.outputStream.flush()
            } catch (_: Exception) {
                sockets.remove(sock)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun listen() {
        try {
            server = adapter?.listenUsingInsecureRfcommWithServiceRecord("BrahSentinel", SPP_UUID)
            while (running) {
                val sock = server?.accept() ?: break
                attach(sock)
            }
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun dialBonded() {
        val bonded = try {
            adapter?.bondedDevices.orEmpty()
        } catch (_: SecurityException) {
            emptySet()
        }
        for (device in bonded) {
            if (!running) return
            try {
                val sock = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                sock.connect()
                attach(sock)
            } catch (_: Exception) {
            }
        }
    }

    private fun attach(sock: BluetoothSocket) {
        sockets.add(sock)
        thread(name = "brah-bt-read", isDaemon = true) {
            try {
                val reader = BufferedReader(InputStreamReader(sock.inputStream, Charsets.UTF_8))
                while (running) {
                    val line = reader.readLine() ?: break
                    val packet = gson.fromJson(line, MeshPacket::class.java) ?: continue
                    onPacketReceived?.invoke(packet)
                }
            } catch (_: Exception) {
            } finally {
                sockets.remove(sock)
                try {
                    sock.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("62747173-656e-7469-6e65-6c0000000001")
    }
}
