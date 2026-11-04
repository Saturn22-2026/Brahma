package com.brahmnetwork.wallet

import android.os.Build

/**
 * Wallet parameters for BRAHMNETWORK (Brahma Coin).
 * Release builds try the shipped public nodes over TLS, in order.
 */
object Constants {
    private const val USB_RPC_URL = "http://127.0.0.1:8545/"
    const val LAN_RPC_URL = "http://192.168.168.16:8545/"
    const val PUBLIC_HTTPS_URL = "https://www.brahmnetwork.com/"
    /** Shipped public nodes. The phone tries these in order, then any HTTPS node it has learned. */
    val SEED_RPC_URLS = listOf(PUBLIC_HTTPS_URL)
    /** P2P seed. Phones dial this on Sentinel even when the HTTPS name is down. */
    const val SEED_P2P_HOST = "84.12.123.117"
    /** Live operator grant (103 Brahma). Opening this identity makes it first. */
    const val OPERATOR_GRANT_ADDR = "BRM1Ge19737c9aa6f097f76cb609a84a6"
    const val OPERATOR_GRANT_ADDRESS = "BRM1Ge19737c9aa6f097f76cb609a84a6"
    private const val EMULATOR_RPC_URL = "http://10.0.2.2:8545/"

    /** Named host for GSM. Bake-in wins; otherwise www.brahmnetwork.com. Never trycloudflare. */
    fun publicHttps(): String {
        val baked = BuildConfig.PUBLIC_RPC_URL.trim()
        val raw = when {
            baked.isBlank() -> PUBLIC_HTTPS_URL
            baked.contains("trycloudflare.com", ignoreCase = true) -> PUBLIC_HTTPS_URL
            baked.contains("drpc", ignoreCase = true) -> PUBLIC_HTTPS_URL
            else -> baked
        }
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    fun isPrivateLanRpc(url: String): Boolean {
        val host = try {
            java.net.URI(if (url.endsWith("/")) url else "$url/").host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: return false
        if (host == "127.0.0.1" || host == "localhost" || host == "10.0.2.2") return true
        if (host.endsWith(".trycloudflare.com")) return true
        return host.startsWith("192.168.") || host.startsWith("10.")
    }

    val RPC_URL: String
        get() = rpcUrls.firstOrNull().orEmpty().ifBlank { LAN_RPC_URL }

    val rpcUrls: List<String>
        get() = rpcUrlCandidates(preferLan = false)

    fun rpcUrlCandidates(preferLan: Boolean): List<String> {
        val https = (listOf(publicHttps()) + SEED_RPC_URLS).distinct()
        val raw = if (!BuildConfig.DEBUG) {
            https
        } else if (isEmulator) {
            https + listOf(EMULATOR_RPC_URL, USB_RPC_URL)
        } else if (preferLan) {
            https + listOf(LAN_RPC_URL, USB_RPC_URL)
        } else {
            https
        }
        return raw.filter { it.isNotBlank() }
    }

    private val isEmulator: Boolean
        get() {
            val fingerprint = Build.FINGERPRINT
            val model = Build.MODEL
            val hardware = Build.HARDWARE
            val product = Build.PRODUCT
            val manufacturer = Build.MANUFACTURER
            return fingerprint.startsWith("generic")
                || fingerprint.contains("unknown")
                || model.contains("Emulator", ignoreCase = true)
                || model.contains("Android SDK", ignoreCase = true)
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || product.contains("sdk")
                || product.contains("emulator")
                || manufacturer.contains("Genymotion", ignoreCase = true)
        }

    data class L2Asset(val id: Int, val name: String, val symbol: String)

    val L2_ASSETS = listOf(
        L2Asset(1, "Amrith", "AMRITH"),
        L2Asset(2, "Kiaan", "KIAAN"),
        L2Asset(3, "Alesha", "ALESHA"),
        L2Asset(4, "BoujieClique", "BOUJIE"),
        L2Asset(5, "QuarterMile", "QMILE"),
        L2Asset(6, "Bonn", "BONN"),
        L2Asset(7, "5thAvenue", "5AVE"),
        L2Asset(8, "Thiro", "THIRO"),
        L2Asset(9, "Santi", "SANTI"),
    )

    val L2_SYMBOLS = L2_ASSETS.associate { it.id to it.symbol }

    fun nodeBase(customRpc: String = ""): String {
        val raw = customRpc.trim().ifBlank { RPC_URL }
        return raw.trimEnd('/')
    }

    /**
     * Wallet RPC may stay on the operator LAN. Invite and APK links always use the
     * named public host. A private address such as 192.168.168.18 is never shareable.
     * [onOperatorLan] cannot select a LAN host.
     */
    fun shareableNodeBase(customRpc: String = "", onOperatorLan: Boolean = false): String {
        val https = publicHttps().trimEnd('/')
        val raw = customRpc.trim()
        if (onOperatorLan && raw.isBlank()) return https
        if (raw.isBlank()) return https
        val base = raw.trimEnd('/')
        val host = try {
            java.net.URI(if (base.endsWith("/")) base else "$base/").host?.lowercase()
        } catch (_: Exception) {
            null
        }
        if (host.isNullOrBlank()
            || host == "127.0.0.1"
            || host == "localhost"
            || host == "10.0.2.2"
            || host.endsWith(".trycloudflare.com")
            || isPrivateLanRpc(base)
            || isPrivateOctetHost(host)
        ) {
            return https
        }
        return base
    }

    private fun isPrivateOctetHost(host: String): Boolean {
        val parts = host.split(".")
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets[0] == 192 && octets[1] == 168) return true
        if (octets[0] == 10) return true
        if (octets[0] == 172 && octets[1] in 16..31) return true
        return false
    }

    /** Invite/APK URLs for other phones — always the public host, never LAN/trycloudflare. */
    fun shareClaimUrl(address: String, customRpc: String = ""): String =
        "${shareableNodeBase(customRpc.ifBlank { publicHttps() }, onOperatorLan = false)}/claim?ref=$address"

    fun shareDownloadUrl(customRpc: String = ""): String =
        "${shareableNodeBase(customRpc.ifBlank { publicHttps() }, onOperatorLan = false)}/wallet.apk"

    const val MESH_PORT = 18545
    const val L2_LAUNCH_BURN = 0.0
    const val CHAIN_ID = 2026L
    const val UNITS_PER_BRAH = 100_000_000L
}
