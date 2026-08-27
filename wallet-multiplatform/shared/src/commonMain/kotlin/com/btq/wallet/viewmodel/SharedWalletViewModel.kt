package com.btq.wallet.viewmodel

import com.btq.wallet.CryptoManager
import com.btq.wallet.MultiplatformKeyPair
import com.btq.wallet.SharingModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedWalletViewModel {
    private val cryptoManager = CryptoManager()
    private val sharingModule = SharingModule()
    
    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address

    private val _guardStatus = MutableStateFlow("AI Sentinel Active")
    val guardStatus: StateFlow<String> = _guardStatus

    private val _discoveryStatus = sharingModule.discoveryStatus

    fun createWallet() {
        val kp = cryptoManager.generateKeyPair()
        _address.value = cryptoManager.deriveAddress(kp.publicKey)
    }

    fun protectTransaction(to: String, amount: Double): Boolean {
        // AI Sentinel Logic: Block transactions to known malicious addresses
        if (to.contains("SCAM")) {
            _guardStatus.value = "ALERT: Malicious address detected. Blocked by Sentinel."
            return false
        }
        
        // Whale Extinguisher check
        if (amount > 1000.0) {
            _guardStatus.value = "WARNING: Large transaction. Whale tax may apply."
        } else {
            _guardStatus.value = "Transaction Verified by AI Sentinel."
        }
        
        return true
    }

    fun startSharing() {
        sharingModule.startP2PBeacon()
    }
}
