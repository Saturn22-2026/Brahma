package com.brahmnetwork.wallet

import android.util.Log
import org.bouncycastle.crypto.prng.FixedSecureRandom
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyGenerationParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner
import org.bouncycastle.crypto.digests.SHAKEDigest
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.security.SecureRandom

class CryptoManager {
    private val hardwareManager = HardwareSecurityManager()

    companion object {
        const val ALGO = "ML-DSA-65"
        private val wordlist = listOf(
            "quantum", "sovereign", "secure", "asset", "digital", "future", "private", "key",
            "recovery", "phrase", "stability", "mission", "empower", "citizen", "network", "block",
            "chain", "wallet", "ledger", "hash", "proof", "stake", "node", "peer",
            "signal", "orbit", "cipher", "vault", "anchor", "pulse", "forge", "ember",
            "cedar", "river", "granite", "aurora", "cobalt", "onyx", "quartz", "silver",
            "copper", "nickel", "helium", "neon", "argon", "xenon", "plasma", "photon",
            "neutron", "proton", "vector", "matrix", "kernel", "relay", "beacon", "harbor",
            "summit", "canyon", "meadow", "forest", "glacier", "tundra", "delta", "omega"
        )

        fun isReady() = true

        fun toHex(data: ByteArray): String = data.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

        fun fromHex(hex: String): ByteArray {
            val clean = hex.removePrefix("0x")
            return ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }

        fun toUnits(amount: Double): Long {
            return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(Constants.UNITS_PER_BRAH))
                .setScale(0, RoundingMode.DOWN)
                .longValueExact()
        }

        fun canonicalMessage(
            chainId: Long,
            nonce: Long,
            kind: String,
            from: String,
            to: String,
            asset: String,
            amountUnits: Long,
            memo: String
        ): ByteArray {
            return "BRAH1|$chainId|$nonce|$kind|$from|$to|$asset|$amountUnits|$memo"
                .toByteArray(Charsets.UTF_8)
        }

        fun sha256Hex(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray(Charsets.UTF_8))
            return toHex(digest)
        }

        fun meetsPow(header: String, nonce: Long, bits: Int): Boolean {
            if (bits <= 0) return false
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("$header:$nonce".toByteArray(Charsets.UTF_8))
            return leadingZeroBits(digest, bits)
        }

        fun mineNonce(
            header: String,
            bits: Int,
            maxTries: Long = 16_000_000L,
            onProgress: ((Long) -> Unit)? = null
        ): Long {
            if (bits <= 0) throw IllegalStateException("PoW bits must be positive")
            val md = MessageDigest.getInstance("SHA-256")
            val prefix = "$header:".toByteArray(Charsets.UTF_8)
            var nonce = 0L
            while (nonce < maxTries) {
                md.reset()
                md.update(prefix)
                md.update(nonce.toString().toByteArray(Charsets.UTF_8))
                if (leadingZeroBits(md.digest(), bits)) return nonce
                nonce++
                if (nonce and 16383L == 0L) {
                    onProgress?.invoke(nonce)
                    Thread.yield()
                }
            }
            throw IllegalStateException("PoW search exhausted")
        }

        private fun leadingZeroBits(digest: ByteArray, bits: Int): Boolean {
            val fullBytes = bits / 8
            val rem = bits % 8
            if (fullBytes > digest.size) return false
            for (i in 0 until fullBytes) {
                if (digest[i].toInt() != 0) return false
            }
            if (rem != 0) {
                val mask = (0xFF shl (8 - rem)) and 0xFF
                if (digest[fullBytes].toInt() and 0xFF and mask != 0) return false
            }
            return true
        }

        fun checksumWord(words: List<String>): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(words.joinToString(" ").toByteArray(Charsets.UTF_8))
            val idx = digest[0].toInt() and 0xff
            return wordlist[idx % wordlist.size]
        }

        private val addrPrefixes = listOf("BRM1G", "BTQ1G")

        fun addrPrefix(addr: String): String? =
            addrPrefixes.firstOrNull { addr.startsWith(it) }

        fun sameWalletAddress(a: String, b: String): Boolean {
            if (!isBrahAddress(a) || !isBrahAddress(b)) return false
            if (a.length < 29 || b.length < 29) return false
            return a.substring(5, 29).lowercase() == b.substring(5, 29).lowercase()
        }

        fun isBrahAddress(addr: String): Boolean {
            val prefix = addrPrefix(addr) ?: return false
            val rest = addr.substring(5)
            if (rest.length != 24 && rest.length != 28) return false
            if (rest.any { it !in "0123456789abcdefABCDEF" }) return false
            if (rest.length == 24) return true
            val body = rest.substring(0, 24)
            val chkMaterial = MessageDigest.getInstance("SHA-256")
                .digest("$prefix$body".toByteArray(Charsets.UTF_8))
            val chk = chkMaterial.take(2).joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
            return rest.substring(24).lowercase() == chk
        }

        fun toBrahAddress(addr: String): String {
            if (addr.isBlank() || !isBrahAddress(addr) || addr.length < 29) return addr
            val body = addr.substring(5, 29).lowercase()
            val chkMaterial = MessageDigest.getInstance("SHA-256")
                .digest("BRM1G$body".toByteArray(Charsets.UTF_8))
            val chk = chkMaterial.take(2).joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
            return "BRM1G$body$chk"
        }

        fun isChecksummedAddress(addr: String): Boolean =
            isBrahAddress(addr) && addr.length == 33

        fun inviteCanonical(ref: String, claim: String, apk: String, ts: Long): ByteArray =
            "BRAHINVITE|$ref|$claim|$apk|$ts".toByteArray(Charsets.UTF_8)

        fun isSafeHttpUrl(url: String, allowedPath: String, allowEmpty: Boolean = false): Boolean {
            if (url.isEmpty()) return allowEmpty
            if (url.length > 256) return false
            return try {
                val parsed = java.net.URI(url)
                val scheme = parsed.scheme?.lowercase()
                val path = (parsed.path ?: "/").trimEnd('/').ifEmpty { "/" }
                scheme in setOf("http", "https")
                    && parsed.userInfo.isNullOrEmpty()
                    && parsed.fragment.isNullOrEmpty()
                    && path == allowedPath
                    && !parsed.host.isNullOrBlank()
                    && ".." !in (parsed.host ?: "")
            } catch (_: Exception) {
                false
            }
        }

        fun isSafeClaimUrl(url: String): Boolean = isSafeHttpUrl(url, "/claim")

        fun referrerFromClaimUrl(url: String): String? {
            return try {
                val parsed = java.net.URI(url.trim())
                val query = parsed.query ?: return null
                val ref = query.split("&").mapNotNull { part ->
                    val pair = part.split("=", limit = 2)
                    if (pair.size == 2 && pair[0] == "ref") {
                        java.net.URLDecoder.decode(pair[1], "UTF-8")
                    } else {
                        null
                    }
                }.firstOrNull() ?: return null
                if (isChecksummedAddress(ref)) toBrahAddress(ref) else null
            } catch (_: Exception) {
                null
            }
        }

        fun isSafeApkUrl(url: String): Boolean = isSafeHttpUrl(url, "/wallet.apk", allowEmpty = true)

        fun rpcHost(url: String): String? = try {
            java.net.URI(url.trim().ifBlank { "http://invalid" }).host
        } catch (_: Exception) {
            null
        }

        fun isLoopbackRpc(url: String): Boolean {
            val host = rpcHost(url) ?: return false
            return host == "127.0.0.1" || host == "localhost"
        }

        fun claimHostMatchesRpc(claim: String, rpcUrl: String): Boolean {
            val claimHost = rpcHost(claim) ?: return false
            val nodeHost = rpcHost(Constants.nodeBase(rpcUrl)) ?: return false
            return claimHost.equals(nodeHost, ignoreCase = true)
        }

        fun isAllowedRpcUrl(url: String, debug: Boolean): Boolean {
            val raw = url.trim()
            if (raw.isEmpty()) return true
            return try {
                val parsed = java.net.URI(if (raw.endsWith("/")) raw else "$raw/")
                val host = parsed.host ?: return false
                when (parsed.scheme?.lowercase()) {
                    "https" -> true
                    "http" -> debug && host.isNotBlank() && host != "0.0.0.0"
                    else -> false
                }
            } catch (_: Exception) {
                false
            }
        }

        fun normalizeMnemonic(mnemonic: String): String {
            val parts = mnemonic.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.size == 13 || parts.size == 25) {
                val body = parts.dropLast(1)
                val expected = checksumWord(body)
                require(parts.last() == expected) { "Invalid recovery checksum" }
                return body.joinToString(" ")
            }
            return parts.joinToString(" ")
        }
    }

    fun generateMnemonic(is24Words: Boolean = true): String {
        val rng = SecureRandom()
        val pool = wordlist.toMutableList()
        val words = MutableList(24) {
            pool.removeAt(rng.nextInt(pool.size))
        }
        return (words + checksumWord(words)).joinToString(" ")
    }

    fun deriveKeyPairFromMnemonic(mnemonic: String): KeyPair {
        val seed = shake256(normalizeMnemonic(mnemonic).toByteArray(Charsets.UTF_8), 2048)
        val random = FixedSecureRandom(seed)
        val generator = DilithiumKeyPairGenerator()
        generator.init(DilithiumKeyGenerationParameters(random, DilithiumParameters.dilithium3))
        val pair = generator.generateKeyPair()
        val pub = pair.public as DilithiumPublicKeyParameters
        val priv = pair.private as DilithiumPrivateKeyParameters
        return KeyPair(publicKey = pub.encoded, secretKey = priv.encoded)
    }

    fun signMessage(secretKey: ByteArray, message: ByteArray, publicKey: ByteArray): ByteArray {
        val pub = DilithiumPublicKeyParameters(DilithiumParameters.dilithium3, publicKey)
        val priv = DilithiumPrivateKeyParameters(DilithiumParameters.dilithium3, secretKey, pub)
        val signer = DilithiumSigner()
        signer.init(true, priv)
        return signer.generateSignature(message)
    }

    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return try {
            val signer = DilithiumSigner()
            signer.init(false, DilithiumPublicKeyParameters(DilithiumParameters.dilithium3, publicKey))
            signer.verifySignature(message, signature)
        } catch (e: Exception) {
            Log.w("CryptoManager", "ML-DSA verify failed: ${e.message}")
            false
        }
    }

    fun signMessageHardware(alias: String, message: ByteArray): ByteArray {
        return hardwareManager.signWithHardware(alias, message)
    }

    fun deriveAddress(publicKey: ByteArray): String {
        val material = sha256(publicKey)
        val body = material.take(12).joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
        val chkMaterial = sha256("BRM1G$body".toByteArray(Charsets.UTF_8))
        val chk = chkMaterial.take(2).joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
        return "BRM1G$body$chk"
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    private fun shake256(data: ByteArray, outLen: Int): ByteArray {
        val digest = SHAKEDigest(256)
        digest.update(data, 0, data.size)
        val out = ByteArray(outLen)
        digest.doFinal(out, 0, outLen)
        return out
    }
}

data class KeyPair(
    val publicKey: ByteArray,
    val secretKey: ByteArray
)
