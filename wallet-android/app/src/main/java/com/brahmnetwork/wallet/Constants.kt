package com.brahmnetwork.wallet

import android.os.Build

/**
 * Wallet parameters for BRAHMNETWORK (Brahma Coin). Private testnet.
 * Debug builds may use LAN HTTP. Release builds require TLS.
 */
object Constants {
    private const val USB_RPC_URL = "http://127.0.0.1:8545/"
    const val LAN_RPC_URL = "http://192.168.168.16:8545/"
    const val PUBLIC_HTTPS_URL = "https://www.brahmnetwork.com/"
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
        val https = publicHttps()
        val raw = if (!BuildConfig.DEBUG) {
            listOf(https)
        } else if (isEmulator) {
            listOf(EMULATOR_RPC_URL, USB_RPC_URL)
        } else if (preferLan) {
            listOf(LAN_RPC_URL, https, USB_RPC_URL)
        } else {
            listOf(https)
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

    val L2_SYMBOLS = mapOf(
        1 to "AMRITH",
        2 to "KIAAN",
        3 to "ALESHA",
        4 to "BOUJIE",
        5 to "QMILE",
        6 to "BONN",
        7 to "5AVE",
        8 to "THIRO",
        9 to "SANTI"
    )

    fun nodeBase(customRpc: String = ""): String {
        val raw = customRpc.trim().ifBlank { RPC_URL }
        return raw.trimEnd('/')
    }

    /**
     * Operator LAN: keep 192.168.168.16. GSM: public HTTPS.
     * Never emit USB/emulator loopback or trycloudflare.
     */
    fun shareableNodeBase(customRpc: String = "", onOperatorLan: Boolean = true): String {
        val https = publicHttps().trimEnd('/')
        val fallback = if (onOperatorLan) LAN_RPC_URL.trimEnd('/') else https
        val raw = customRpc.trim().ifBlank { fallback }
        val base = raw.trimEnd('/')
        val host = try {
            java.net.URI(if (base.endsWith("/")) base else "$base/").host
        } catch (_: Exception) {
            null
        }
        if (host.isNullOrBlank()
            || host == "127.0.0.1"
            || host == "localhost"
            || host == "10.0.2.2"
            || host.endsWith(".trycloudflare.com")
        ) {
            return fallback
        }
        if (!onOperatorLan && isPrivateLanRpc(base)) {
            return https
        }
        return base
    }

    fun shareClaimUrl(address: String, customRpc: String = ""): String =
        "${shareableNodeBase(customRpc)}/claim?ref=$address"

    fun shareDownloadUrl(customRpc: String = ""): String =
        "${shareableNodeBase(customRpc)}/wallet.apk"

    const val MESH_PORT = 18545
    const val L2_LAUNCH_BURN = 100.0
    const val CHAIN_ID = 2026L
    const val UNITS_PER_BRAH = 100_000_000L
}
