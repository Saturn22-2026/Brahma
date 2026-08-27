package com.btq.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btq.wallet.CryptoManager
import com.btq.wallet.KeyPair
import com.btq.wallet.network.BTQService
import com.btq.wallet.network.JsonRpcRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WalletViewModel : ViewModel() {
    private val cryptoManager = CryptoManager()
    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance

    private val _status = MutableStateFlow("Disconnected")
    val status: StateFlow<String> = _status

    private var keyPair: KeyPair? = null

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080") // Android emulator localhost
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(BTQService::class.java)

    init {
        generateNewWallet()
        refreshNetworkStats()
    }

    fun generateNewWallet() {
        val kp = cryptoManager.generateKeyPair()
        keyPair = kp
        _address.value = cryptoManager.deriveAddress(kp.publicKey)
    }

    fun refreshNetworkStats() {
        viewModelScope.launch {
            try {
                val response = service.getNetworkStats(JsonRpcRequest(method = "btq_getNetworkStats", params = emptyList()))
                response.result?.let {
                    _status.value = "Connected (Height: ${it.chain_height})"
                }
            } catch (e: Exception) {
                _status.value = "Error: ${e.message}"
            }
        }
    }
}
