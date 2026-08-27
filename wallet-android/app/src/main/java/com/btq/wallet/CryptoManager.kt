package com.btq.wallet

class CryptoManager {
    companion object {
        init {
            System.loadLibrary("mldsa_jni")
        }
    }

    external fun generateKeyPair(): KeyPair
    external fun signMessage(secretKey: ByteArray, message: ByteArray): ByteArray
    external fun deriveAddress(publicKey: ByteArray): String
}

data class KeyPair(
    val publicKey: ByteArray,
    val secretKey: ByteArray
)
