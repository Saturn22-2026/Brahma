package com.brahmnetwork.wallet

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * One P-256 key in the Android keystore. The private key cannot be exported.
 * The join memo proves this phone, so a made-up device string is not enough.
 */
object DeviceJoinKey {
    private const val ALIAS = "brahm_join_device"

    data class Proof(val compressedPub: String, val signatureHex: String)

    fun proof(address: String): Proof? {
        return try {
            val pub = ensure()
            val hex = compressed(pub)
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey())
            signature.update("BRAHDEV|$address|$hex".toByteArray(Charsets.UTF_8))
            Proof(hex, signature.sign().joinToString("") { "%02x".format(it) })
        } catch (_: Exception) {
            null
        }
    }

    private fun ensure(): ECPublicKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(ALIAS)) {
            val generator = java.security.KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
            generator.initialize(spec)
            generator.generateKeyPair()
        }
        val key = store.getCertificate(ALIAS).publicKey
        return key as ECPublicKey
    }

    private fun privateKey(): java.security.PrivateKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return store.getKey(ALIAS, null) as java.security.PrivateKey
    }

    private fun compressed(pub: ECPublicKey): String {
        val x = pub.w.affineX.toByteArray().let { trim32(it) }
        val prefix = if (pub.w.affineY.testBit(0)) "03" else "02"
        return prefix + x.joinToString("") { "%02x".format(it) }
    }

    private fun trim32(raw: ByteArray): ByteArray {
        if (raw.size == 32) return raw
        if (raw.size > 32) return raw.copyOfRange(raw.size - 32, raw.size)
        return ByteArray(32 - raw.size) + raw
    }
}
