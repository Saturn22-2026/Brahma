package com.btq.wallet

actual class CryptoManager actual constructor() {
    init {
        System.loadLibrary("mldsa_jni")
    }

    actual fun generateKeyPair(): MultiplatformKeyPair {
        val kp = nativeGenerateKeyPair()
        return MultiplatformKeyPair(kp.publicKey, kp.secretKey)
    }

    actual fun signMessage(secretKey: ByteArray, message: ByteArray): ByteArray {
        return nativeSignMessage(secretKey, message)
    }

    actual fun deriveAddress(publicKey: ByteArray): String {
        return nativeDeriveAddress(publicKey)
    }

    private external fun nativeGenerateKeyPair(): KeyPair
    private external fun nativeSignMessage(secretKey: ByteArray, message: ByteArray): ByteArray
    private external fun nativeDeriveAddress(publicKey: ByteArray): String
}

// Helper for JNI mapping
data class KeyPair(val publicKey: ByteArray, val secretKey: ByteArray)
