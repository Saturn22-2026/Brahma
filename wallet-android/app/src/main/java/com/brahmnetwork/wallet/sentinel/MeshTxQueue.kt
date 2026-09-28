package com.brahmnetwork.wallet.sentinel

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/** Signed txs waiting for a node (HTTP or mesh) when the radio is dark. */
class MeshTxQueue(private val prefs: SharedPreferences) {
    @Synchronized
    fun enqueue(txJson: String) {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        if (arr.length() >= 32) return
        for (i in 0 until arr.length()) {
            if (arr.optString(i) == txJson) return
        }
        arr.put(txJson)
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    @Synchronized
    fun snapshot(): List<String> {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
    }

    @Synchronized
    fun remove(txJson: String) {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val next = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.optString(i)
            if (item != txJson) next.put(item)
        }
        prefs.edit().putString(KEY, next.toString()).apply()
    }

    fun size(): Int = snapshot().size

    companion object {
        private const val KEY = "mesh_tx_queue"
        fun from(context: Context): MeshTxQueue {
            return MeshTxQueue(context.getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE))
        }
    }
}
