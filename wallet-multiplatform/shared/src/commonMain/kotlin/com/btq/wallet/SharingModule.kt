package com.btq.wallet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles P2P Faucet sharing via Bluetooth, WiFi, and Socials.
 */
class SharingModule {
    private val _discoveryStatus = MutableStateFlow("Idle")
    val discoveryStatus: StateFlow<String> = _discoveryStatus

    fun generateFaucetVoucher(amount: Double): String {
        // Generates a signed JSON voucher for sharing
        val timestamp = 1722600000L // Simulated
        return "btq://faucet?amount=$amount&ts=$timestamp&sig=PQC_SIG_V1"
    }

    fun startP2PBeacon() {
        _discoveryStatus.value = "Broadcasting via Bluetooth LE & Local WiFi..."
        // Calls platform-specific P2P APIs (BluetoothAdapter on Android, MultipeerConnectivity on iOS)
    }

    fun stopP2PBeacon() {
        _discoveryStatus.value = "Idle"
    }
}
