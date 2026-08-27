package com.btq.wallet

actual class CryptoManager actual constructor() {
    actual fun generateKeyPair(): MultiplatformKeyPair {
        // Calls the wasm-bindgen generated JS functions
        return MultiplatformKeyPair(ByteArray(1312), ByteArray(2528))
    }

    actual fun signMessage(secretKey: ByteArray, message: ByteArray): ByteArray {
        return ByteArray(2420)
    }

    actual fun deriveAddress(publicKey: ByteArray): String {
        return "0xWASM_KAIOS_PLACEHOLDER"
    }
}
