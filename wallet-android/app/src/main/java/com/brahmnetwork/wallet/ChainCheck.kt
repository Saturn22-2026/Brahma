package com.brahmnetwork.wallet

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.brahmnetwork.wallet.network.BrahRpc

/**
 * A node is this chain only when every header links back to the shipped genesis
 * and each mined block meets the proof of work it declares (18–24 bits).
 */
object ChainCheck {
    const val GENESIS_HASH = "34ba978506a51f42077f23573801045b7ec9b82a43420ffe4b999deb5377eaf8"

    suspend fun score(client: BrahRpc): Long? {
        var start = 0
        var prev: String? = null
        var sawGenesis = false
        var work = 0L
        var height = 0
        while (start < 100_000) {
            val page = try {
                client.call("brah_getBlocks", listOf(start, 64))?.asJsonObject
            } catch (_: Exception) {
                return null
            } ?: return null
            val blocks = page.getAsJsonArray("blocks") ?: return null
            if (blocks.size() == 0) {
                if (!sawGenesis || height > 0 && work == 0L) return null
                return work
            }
            for (el in blocks) {
                val block = el.asJsonObject
                val index = block.get("index")?.asInt ?: return null
                val hash = block.get("hash")?.asString ?: return null
                if (index == 0) {
                    if (!hash.equals(GENESIS_HASH, ignoreCase = true)) return null
                    sawGenesis = true
                } else {
                    val previous = block.get("previous_hash")?.asString ?: return null
                    if (prev != null && previous != prev) return null
                    if (!headerOk(block)) return null
                    work += provenWork(block)
                }
                prev = hash
                height = index
            }
            val tip = page.get("tip")?.asInt ?: return if (sawGenesis && (height == 0 || work > 0L)) work else null
            val last = blocks.last().asJsonObject.get("index")?.asInt ?: return null
            if (last >= tip) {
                if (!sawGenesis || height > 0 && work == 0L) return null
                return work
            }
            start = last + 1
        }
        return null
    }

    fun provenWork(block: JsonObject): Long {
        val bits = block.get("pow_bits")?.asInt ?: 0
        if (bits !in 18..24) return 0L
        if (!headerOk(block)) return 0L
        return 1L shl bits
    }

    fun headerOk(block: JsonObject): Boolean {
        val index = block.get("index")?.asInt ?: return false
        val previous = block.get("previous_hash")?.asString ?: return false
        val miner = block.get("miner")?.asString ?: return false
        val reward = block.get("reward")?.asLong ?: 0L
        val root = block.get("tx_root")?.asString ?: return false
        val nonce = block.get("nonce")?.asLong ?: return false
        val hash = block.get("hash")?.asString ?: return false
        val bits = block.get("pow_bits")?.asInt ?: 0
        val instant = block.get("instant")?.asBoolean == true || bits == 0
        val header = "${index - 1}|$previous|$miner|$reward|$root"
        val expect = CryptoManager.sha256Hex("$header:$nonce")
        if (!expect.equals(hash, ignoreCase = true)) return false
        if (instant) return nonce == 0L && bits == 0
        if (bits !in 18..24) return false
        return CryptoManager.meetsPow(header, nonce, bits)
    }

    fun meshPayloadOk(payload: String): Boolean {
        val obj = try {
            JsonParser.parseString(payload).asJsonObject
        } catch (_: Exception) {
            return false
        }
        val sig = obj.get("signature")?.asString.orEmpty()
        val pk = obj.get("public_key")?.asString.orEmpty()
        if (sig.length < 64 || pk.length < 64) return false
        val header = obj.get("_header")?.asString
        val nonce = obj.get("pow_nonce")?.asLong
        val bits = obj.get("pow_bits")?.asInt ?: obj.get("_bits")?.asInt
        if (header != null && nonce != null && bits != null) {
            return bits in 18..24 && CryptoManager.meetsPow(header, nonce, bits)
        }
        return true
    }
}
