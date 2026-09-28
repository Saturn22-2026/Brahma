package com.brahmnetwork.wallet.sentinel

import com.brahmnetwork.wallet.CryptoManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlin.math.abs

object InviteGuard {
    private val gson = Gson()
    private val crypto = CryptoManager()
    @Volatile
    var allowedRpc: String = ""

    fun accept(payloadJson: String): Boolean {
        return try {
            val o = gson.fromJson(payloadJson, JsonObject::class.java) ?: return false
            val ref = o.get("ref")?.asString ?: return false
            val claim = o.get("claim")?.asString ?: return false
            val apk = o.get("apk")?.asString.orEmpty()
            val ts = o.get("ts")?.asLong ?: return false
            val pkHex = o.get("public_key")?.asString ?: return false
            val sigHex = o.get("signature")?.asString ?: return false
            if (!CryptoManager.isChecksummedAddress(ref)) return false
            if (!CryptoManager.isSafeClaimUrl(claim) || !CryptoManager.isSafeApkUrl(apk)) return false
            if (abs(System.currentTimeMillis() / 1000 - ts) > 90) return false
            if (allowedRpc.isNotBlank()
                && !CryptoManager.claimHostMatchesRpc(claim, allowedRpc)
                && !CryptoManager.claimHostMatchesRpc(claim, com.brahmnetwork.wallet.Constants.LAN_RPC_URL)
                && !(com.brahmnetwork.wallet.Constants.publicHttps().isNotBlank()
                    && CryptoManager.claimHostMatchesRpc(claim, com.brahmnetwork.wallet.Constants.publicHttps()))
            ) return false
            val pk = CryptoManager.fromHex(pkHex)
            if (!CryptoManager.sameWalletAddress(crypto.deriveAddress(pk), ref)) return false
            val sig = CryptoManager.fromHex(sigHex)
            crypto.verify(pk, CryptoManager.inviteCanonical(ref, claim, apk, ts), sig)
                || crypto.verify(pk, "BTQINVITE|$ref|$claim|$apk|$ts".toByteArray(Charsets.UTF_8), sig)
        } catch (_: Exception) {
            false
        }
    }
}
