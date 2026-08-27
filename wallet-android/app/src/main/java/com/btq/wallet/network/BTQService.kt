package com.btq.wallet.network

import retrofit2.http.Body
import retrofit2.http.POST

data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: List<Any>,
    val id: Int = 1
)

data class JsonRpcResponse<T>(
    val jsonrpc: String,
    val result: T?,
    val error: JsonRpcError?,
    val id: Int
)

data class JsonRpcError(
    val code: Int,
    val message: String
)

data class NetworkStats(
    val chain_height: Long,
    val total_mined: Double,
    val difficulty: Int,
    val p2p_status: String
)

interface BTQService {
    @POST("/")
    suspend fun getNetworkStats(@Body request: JsonRpcRequest): JsonRpcResponse<NetworkStats>
    
    @POST("/")
    suspend fun sendTransaction(@Body request: JsonRpcRequest): JsonRpcResponse<String>
}
