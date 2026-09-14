package com.btq.wallet

import android.util.Log

class CryptoManager {
    private val hardwareManager = HardwareSecurityManager()

    companion object {
        private var isLibLoaded = false
        init {
            try {
                System.loadLibrary("mldsa_jni")
                isLibLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e("CryptoManager", "Failed to load mldsa_jni", e)
            }
        }
        
        fun isReady() = isLibLoaded
    }

    external fun generateMnemonic(is24Words: Int): String
    external fun deriveKeyPairFromMnemonic(mnemonic: String): KeyPair
    external fun signMessage(secretKey: ByteArray, message: ByteArray): ByteArray
    
    fun signMessageHardware(alias: String, message: ByteArray): ByteArray {
        return hardwareManager.signWithHardware(alias, message)
    }

    external fun deriveAddress(publicKey: ByteArray): String
}

data class KeyPair(
    val publicKey: ByteArray,
    val secretKey: ByteArray
)
