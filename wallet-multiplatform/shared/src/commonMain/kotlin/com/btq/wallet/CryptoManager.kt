package com.btq.wallet

expect class CryptoManager() {
    fun generateKeyPair(): MultiplatformKeyPair
    fun signMessage(secretKey: ByteArray, message: ByteArray): ByteArray
    fun deriveAddress(publicKey: ByteArray): String
}

data class MultiplatformKeyPair(
    val publicKey: ByteArray,
    val secretKey: ByteArray
)
