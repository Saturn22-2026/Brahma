package com.brahmnetwork.wallet.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class BrahRpc(
    private val urlsProvider: () -> List<String>
) {
    constructor(urls: List<String>) : this({ urls })

    private val urls: List<String>
        get() = urlsProvider().filter { it.isNotBlank() }.distinct()
    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val loopbackClient = client.newBuilder()
        .connectTimeout(400, TimeUnit.MILLISECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val lanClient = client.newBuilder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun clientFor(url: String): OkHttpClient {
        val host = try {
            java.net.URI(url).host.orEmpty()
        } catch (_: Exception) {
            ""
        }
        return when {
            host == "127.0.0.1" || host == "localhost" -> loopbackClient
            host.startsWith("192.168.") || host.startsWith("10.") -> lanClient
            else -> client
        }
    }

    @Volatile
    var activeUrl: String = ""
        private set

    var onSuccess: ((String) -> Unit)? = null
    var onAttempt: ((String) -> Unit)? = null

    fun resetActiveUrl() {
        activeUrl = ""
    }

    suspend fun call(method: String, params: List<Any> = emptyList()): JsonElement? {
        return withContext(Dispatchers.IO) {
            val payload = JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                addProperty("method", method)
                add("params", gson.toJsonTree(params))
                addProperty("id", 1)
            }
            val body = payload.toString().toRequestBody(jsonMedia)
            var lastError: Exception? = null
            val candidates = urls
            val ordered = (listOf(activeUrl) + candidates).filter { it.isNotBlank() }.distinct()
            for (url in ordered) {
                try {
                    onAttempt?.invoke(url)
                    val request = Request.Builder()
                        .url(url)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build()
                    clientFor(url).newCall(request).execute().use { response ->
                        val text = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            throw IllegalStateException("HTTP ${response.code} from $url")
                        }
                        val root = JsonParser.parseString(text).asJsonObject
                        if (root.has("error") && !root.get("error").isJsonNull) {
                            val err = root.getAsJsonObject("error")
                            val message = err.get("message")?.asString ?: err.toString()
                            throw JsonRpcException(message)
                        }
                        activeUrl = url
                        onSuccess?.invoke(url)
                        Log.i(TAG, "$method ok via $url")
                        return@withContext if (root.has("result") && !root.get("result").isJsonNull) {
                            root.get("result")
                        } else {
                            null
                        }
                    }
                } catch (e: JsonRpcException) {
                    Log.w(TAG, "$method rejected by $url: ${e.message}")
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "$method failed on $url: ${e.message}")
                    lastError = e
                }
            }
            throw lastError ?: IllegalStateException("No RPC endpoint reachable")
        }
    }

    suspend fun callString(method: String, params: List<Any> = emptyList()): String {
        val result = call(method, params)
        return when {
            result == null || result.isJsonNull -> ""
            result.isJsonPrimitive -> result.asString
            else -> result.toString()
        }
    }

    companion object {
        private const val TAG = "BrahRpc"
    }
}

class JsonRpcException(message: String) : IllegalStateException(message)

fun JsonElement?.obj(): JsonObject = this?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()

fun JsonObject.doubleOr(key: String, fallback: Double = 0.0): Double {
    val el = get(key) ?: return fallback
    return try {
        if (el.isJsonPrimitive) el.asDouble else fallback
    } catch (_: Exception) {
        fallback
    }
}

fun JsonObject.longOr(key: String, fallback: Long = 0L): Long {
    val el = get(key) ?: return fallback
    return try {
        if (!el.isJsonPrimitive) return fallback
        val p = el.asJsonPrimitive
        if (p.isNumber) p.asLong else p.asString.toLongOrNull() ?: fallback
    } catch (_: Exception) {
        fallback
    }
}

fun JsonObject.boolOr(key: String, fallback: Boolean = false): Boolean {
    val el = get(key) ?: return fallback
    return try {
        if (el.isJsonPrimitive) el.asBoolean else fallback
    } catch (_: Exception) {
        fallback
    }
}

fun JsonObject.stringOr(key: String, fallback: String = ""): String {
    val el = get(key) ?: return fallback
    return try {
        if (el.isJsonPrimitive) el.asString else fallback
    } catch (_: Exception) {
        fallback
    }
}
