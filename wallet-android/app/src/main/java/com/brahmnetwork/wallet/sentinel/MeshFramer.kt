package com.brahmnetwork.wallet.sentinel

import android.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/** Split mesh JSON into Wi-Fi Aware-sized frames and reassemble. */
object MeshFramer {
    private const val CHUNK = 120

    fun encode(json: String): List<ByteArray> {
        val id = UUID.randomUUID().toString().take(8)
        val b64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val n = (b64.length + CHUNK - 1) / CHUNK
        return (0 until n).map { i ->
            val chunk = b64.substring(i * CHUNK, min((i + 1) * CHUNK, b64.length))
            "BRAHF|$id|$i|$n|$chunk".toByteArray(Charsets.UTF_8)
        }
    }
}

class MeshAssembler {
    private val pending = ConcurrentHashMap<String, Array<String?>>()

    fun accept(message: ByteArray): String? {
        val text = String(message, Charsets.UTF_8)
        if (!text.startsWith("BRAHF|") && !text.startsWith("BTQF|")) {
            return text
        }
        val parts = text.split("|", limit = 5)
        if (parts.size < 5) return null
        val id = parts[1]
        val index = parts[2].toIntOrNull() ?: return null
        val total = parts[3].toIntOrNull() ?: return null
        val chunk = parts[4]
        if (total <= 0 || index !in 0 until total || total > 256) return null
        val slots = pending.getOrPut(id) { arrayOfNulls(total) }
        if (slots.size != total) {
            pending[id] = arrayOfNulls<String>(total).also { it[index] = chunk }
            return null
        }
        slots[index] = chunk
        if (slots.any { it == null }) return null
        pending.remove(id)
        val b64 = slots.joinToString("") { it ?: "" }
        return String(Base64.decode(b64, Base64.NO_WRAP), Charsets.UTF_8)
    }
}
