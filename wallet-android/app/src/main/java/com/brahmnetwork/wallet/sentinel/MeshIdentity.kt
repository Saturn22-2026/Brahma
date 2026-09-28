package com.brahmnetwork.wallet.sentinel

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.brahmnetwork.wallet.CryptoManager
import com.brahmnetwork.wallet.KeyPair
import kotlin.math.abs

object MeshHello {
    const val MAX_SKEW_SEC = 90L

    fun canonical(nodeId: String, tcpPort: Int, ts: Long, prefix: String = "BRAHHELLO"): ByteArray =
        "$prefix|$nodeId|$tcpPort|$ts".toByteArray(Charsets.UTF_8)
}

class MeshIdentity private constructor(
    private val crypto: CryptoManager,
    private val pair: KeyPair
) {
    fun fields(nodeId: String, tcpPort: Int): Map<String, Any> {
        val ts = System.currentTimeMillis() / 1000
        val sig = crypto.signMessage(pair.secretKey, MeshHello.canonical(nodeId, tcpPort, ts), pair.publicKey)
        return mapOf(
            "tcpPort" to tcpPort,
            "node" to nodeId,
            "senderId" to nodeId,
            "ts" to ts,
            "public_key" to CryptoManager.toHex(pair.publicKey),
            "signature" to CryptoManager.toHex(sig),
            "algo" to "Dilithium3"
        )
    }

    fun verify(nodeId: String, tcpPort: Int, publicKeyHex: String, signatureHex: String, ts: Long): Boolean {
        if (nodeId.isBlank() || tcpPort <= 0 || publicKeyHex.isBlank() || signatureHex.isBlank() || ts <= 0L) {
            return false
        }
        val now = System.currentTimeMillis() / 1000
        if (abs(now - ts) > MeshHello.MAX_SKEW_SEC) {
            return false
        }
        return try {
            val pk = CryptoManager.fromHex(publicKeyHex)
            val sig = CryptoManager.fromHex(signatureHex)
            crypto.verify(pk, MeshHello.canonical(nodeId, tcpPort, ts), sig)
                || crypto.verify(pk, MeshHello.canonical(nodeId, tcpPort, ts, "BTQHELLO"), sig)
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        fun load(context: Context): MeshIdentity {
            val crypto = CryptoManager()
            val prefs = openPrefs(context)
            var pkHex = prefs.getString("mesh_pk", "") ?: ""
            var skHex = prefs.getString("mesh_sk", "") ?: ""
            if (pkHex.isBlank() || skHex.isBlank()) {
                val mnemonic = crypto.generateMnemonic(true)
                val pair = crypto.deriveKeyPairFromMnemonic(mnemonic)
                pkHex = CryptoManager.toHex(pair.publicKey)
                skHex = CryptoManager.toHex(pair.secretKey)
                prefs.edit()
                    .putString("mesh_pk", pkHex)
                    .putString("mesh_sk", skHex)
                    .apply()
            }
            return MeshIdentity(
                crypto,
                KeyPair(CryptoManager.fromHex(pkHex), CryptoManager.fromHex(skHex))
            )
        }

        private fun openPrefs(context: Context): android.content.SharedPreferences {
            val plain = context.getSharedPreferences("mesh_vault", Context.MODE_PRIVATE)
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "mesh_vault_enc",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                plain
            }
        }
    }
}
