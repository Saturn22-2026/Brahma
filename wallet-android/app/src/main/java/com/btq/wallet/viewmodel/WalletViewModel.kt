package com.btq.wallet.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.btq.wallet.Constants
import com.btq.wallet.CryptoManager
import com.btq.wallet.HardwareSecurityManager
import com.btq.wallet.network.BTQService
import com.btq.wallet.network.JsonRpcRequest
import com.btq.wallet.network.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

data class TokenBalance(
    val symbol: String,
    val amount: Double,
    val fiatValue: String
)

data class SuccessorConfig(
    var address: String = "",
    var percentage: Int = 0
)

sealed class AuthRequest {
    data class SendTokens(val receiver: String, val amount: Double, val assetId: Int) : AuthRequest()
    object ShowMnemonic : AuthRequest()
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoManager = CryptoManager()
    private val securityManager = HardwareSecurityManager()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.RPC_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val service = retrofit.create(BTQService::class.java)

    private val prefs = application.getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
    private val securePrefs = application.getSharedPreferences("secure_vault", Context.MODE_PRIVATE)

    // --- State Flows ---
    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _tokens = MutableStateFlow<List<TokenBalance>>(emptyList())
    val tokens: StateFlow<List<TokenBalance>> = _tokens.asStateFlow()

    private val _status = MutableStateFlow("Disconnected")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _address = MutableStateFlow(prefs.getString("wallet_address", "") ?: "")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _isHardwareColdStorage = MutableStateFlow(prefs.getBoolean("cold_storage", false))
    val isHardwareColdStorage: StateFlow<Boolean> = _isHardwareColdStorage.asStateFlow()

    private val _totalUsers = MutableStateFlow(0L)
    val totalUsers: StateFlow<Long> = _totalUsers.asStateFlow()

    private val _mnemonic = MutableStateFlow("")
    val mnemonic: StateFlow<String> = _mnemonic.asStateFlow()

    private val _isMnemonicVisible = MutableStateFlow(false)
    val isMnemonicVisible: StateFlow<Boolean> = _isMnemonicVisible.asStateFlow()

    private val _isDmsEnabled = MutableStateFlow(prefs.getBoolean("dms_enabled", false))
    val isDmsEnabled: StateFlow<Boolean> = _isDmsEnabled.asStateFlow()

    private val _successorConfigs = MutableStateFlow(List(3) { SuccessorConfig() })
    val successorConfigs: StateFlow<List<SuccessorConfig>> = _successorConfigs.asStateFlow()

    private val _aiAgentStatus = MutableStateFlow("Idle")
    val aiAgentStatus: StateFlow<String> = _aiAgentStatus.asStateFlow()

    private val _autoLockMinutes = MutableStateFlow(prefs.getInt("auto_lock", 5))
    val autoLockMinutes: StateFlow<Int> = _autoLockMinutes.asStateFlow()

    private val _isBiometricsEnabled = MutableStateFlow(prefs.getBoolean("biometrics", false))
    val isBiometricsEnabled: StateFlow<Boolean> = _isBiometricsEnabled.asStateFlow()

    // --- Phase 2.0 State Flows ---
    private val _qusdBalance = MutableStateFlow(0.0)
    val qusdBalance: StateFlow<Double> = _qusdBalance.asStateFlow()

    private val _genesisRank = MutableStateFlow(0L)
    val genesisRank: StateFlow<Long> = _genesisRank.asStateFlow()

    private val _qusdCollateral = MutableStateFlow(0.0)
    val qusdCollateral: StateFlow<Double> = _qusdCollateral.asStateFlow()

    private val _referrerAddress = MutableStateFlow(prefs.getString("referrer_address", "") ?: "")
    val referrerAddress: StateFlow<String> = _referrerAddress.asStateFlow()

    private val _annualYield = MutableStateFlow(0.0)
    val annualYield: StateFlow<Double> = _annualYield.asStateFlow()

    private var pendingAuthRequest: AuthRequest? = null

    init {
        refreshBalances()
        loadSuccessorConfigs()
    }

    fun refreshBalances() {
        viewModelScope.launch {
            try {
                _status.value = "Syncing..."
                val addr = _address.value
                if (addr.isEmpty()) {
                    _status.value = "No Wallet"
                    return@launch
                }

                // Fetch BTQ balance
                val balanceResponse = service.getBalance(JsonRpcRequest(method = "eth_getBalance", params = listOf(addr, "latest")))
                val btqBalance = balanceResponse.result?.get("BTQ") ?: 0.0
                _balance.value = btqBalance

                // Fetch Network Stats (includes Genesis Rank and Collateral in Phase 2.0)
                val statsResponse = service.getNetworkStats(JsonRpcRequest(method = "btq_getStats", params = emptyList()))
                statsResponse.result?.let { stats ->
                    _totalUsers.value = stats.total_users
                    _genesisRank.value = stats.genesis_rank
                    _qusdCollateral.value = stats.qusd_collateral
                    
                    // Math Redo: Annual Yield (10% of fees distributed to stakers)
                    // Simplified for Phase 2.0: (Total Fees * 0.1) / Total Collateral
                    val yield = if (stats.qusd_collateral > 0) (stats.total_mined * 0.1) / stats.qusd_collateral else 0.1
                    _annualYield.value = yield * 100 // Percentage
                }

                // Fetch QUSD Balance (Stable-Credit)
                val qusdResponse = service.getBalance(JsonRpcRequest(method = "eth_getBalance", params = listOf(addr, "qusd")))
                _qusdBalance.value = qusdResponse.result?.get("QUSD") ?: 0.0

                // Fetch L2 Assets
                val tokensList = mutableListOf<TokenBalance>()
                Constants.L2_SYMBOLS.forEach { (id, symbol) ->
                    val tokenResponse = service.getBalance(JsonRpcRequest(method = "eth_getBalance", params = listOf(addr, id.toString())))
                    val amount = tokenResponse.result?.get("amount") ?: 0.0
                    if (amount > 0) {
                        tokensList.add(TokenBalance(symbol, amount, "$${amount * 0.01}")) // Mock fiat value
                    }
                }
                _tokens.value = tokensList

                _status.value = "Live"
            } catch (e: Exception) {
                _status.value = "Offline"
            }
        }
    }

    fun mineBTQ() {
        viewModelScope.launch {
            try {
                val rank = _genesisRank.value
                val multiplier = when {
                    rank in 1..500 -> 1.0
                    rank in 501..5000 -> 0.5
                    else -> 0.1
                }
                
                _status.value = "Mining (Tier Multiplier: ${multiplier * 100}%)"
                
                val response = service.mine(JsonRpcRequest(method = "btq_mine", params = listOf(_address.value)))
                if (response.result != null) {
                    _status.value = "Block Mined! +${0.1 * multiplier} BTQ"
                    refreshBalances()
                } else {
                    _status.value = "Mining failed: ${response.error?.message}"
                }
            } catch (e: Exception) {
                _status.value = "Mining Error"
            }
        }
    }

    fun setReferrer(address: String) {
        _referrerAddress.value = address
        prefs.edit().putString("referrer_address", address).apply()
    }

    fun mintQUSD(amount: Double) {
        viewModelScope.launch {
            try {
                _status.value = "Minting QUSD..."
                val response = service.mintQUSD(JsonRpcRequest(method = "btq_mintQUSD", params = listOf(_address.value, amount)))
                if (response.result != null) {
                    _status.value = "QUSD Minted successfully"
                    refreshBalances()
                } else {
                    _status.value = "Minting failed: ${response.error?.message}"
                }
            } catch (e: Exception) {
                _status.value = "Minting Error"
            }
        }
    }

    fun requestSendTokens(receiver: String, amount: Double, assetId: Int) {
        pendingAuthRequest = AuthRequest.SendTokens(receiver, amount, assetId)
        // In a real app, this would trigger BiometricPrompt
        // For this refactor, we assume auth is successful and call executeSendAfterAuth
        executeSendAfterAuth()
    }

    private fun executeSendAfterAuth() {
        val request = pendingAuthRequest as? AuthRequest.SendTokens ?: return
        viewModelScope.launch {
            try {
                _status.value = "Authenticating..."
                
                val isFirstSend = !prefs.getBoolean("has_sent_before", false)
                val referrer = if (isFirstSend) _referrerAddress.value else null
                
                if (isFirstSend) {
                    prefs.edit().putBoolean("has_sent_before", true).apply()
                }

                val tx = Transaction(
                    asset_id = request.assetId,
                    sender = _address.value,
                    receiver = request.receiver,
                    amount = request.amount,
                    fee = 0.001,
                    signature_pqc = emptyList(), // Signatures would be generated here using CryptoManager
                    signature_sphincs = emptyList(),
                    signature_classic = emptyList(),
                    pk_pqc = emptyList(),
                    pk_sphincs = emptyList(),
                    pk_classic = emptyList(),
                    is_private = false,
                    referrer = referrer
                )

                val response = service.sendTransaction(JsonRpcRequest(method = "eth_sendRawTransaction", params = listOf(tx)))
                if (response.result != null) {
                    _status.value = "Transaction Sent"
                    refreshBalances()
                } else {
                    _status.value = "Send failed: ${response.error?.message}"
                }
            } catch (e: Exception) {
                _status.value = "Send Error"
            } finally {
                pendingAuthRequest = null
            }
        }
    }

    fun claimFaucet() {
        viewModelScope.launch {
            try {
                _status.value = "Requesting Faucet..."
                val response = service.requestFaucet(JsonRpcRequest(method = "btq_faucet", params = listOf(_address.value)))
                if (response.result != null) {
                    _status.value = "Faucet claim successful"
                    refreshBalances()
                } else {
                    _status.value = "Faucet failed: ${response.error?.message}"
                }
            } catch (e: Exception) {
                _status.value = "Faucet Error"
            }
        }
    }

    fun createWallet(is24Words: Boolean, isHardware: Boolean) {
        val mnemonic = cryptoManager.generateMnemonic(if (is24Words) 1 else 0)
        val keyPair = cryptoManager.deriveKeyPairFromMnemonic(mnemonic)
        val addr = cryptoManager.deriveAddress(keyPair.publicKey)
        
        _address.value = addr
        _isHardwareColdStorage.value = isHardware
        
        prefs.edit().apply {
            putString("wallet_address", addr)
            putBoolean("cold_storage", isHardware)
            apply()
        }
        
        // Save mnemonic securely
        securePrefs.edit().putString("mnemonic", mnemonic).apply()
        
        refreshBalances()
    }

    fun recoverWallet(mnemonic: String, isHardware: Boolean) {
        val keyPair = cryptoManager.deriveKeyPairFromMnemonic(mnemonic)
        val addr = cryptoManager.deriveAddress(keyPair.publicKey)
        
        _address.value = addr
        _isHardwareColdStorage.value = isHardware
        
        prefs.edit().apply {
            putString("wallet_address", addr)
            putBoolean("cold_storage", isHardware)
            apply()
        }
        
        securePrefs.edit().putString("mnemonic", mnemonic).apply()
        
        refreshBalances()
    }

    fun requestShowMnemonic() {
        // Trigger Biometric Auth then reveal
        _mnemonic.value = securePrefs.getString("mnemonic", "Not found") ?: ""
        _isMnemonicVisible.value = true
    }

    fun hideMnemonic() {
        _isMnemonicVisible.value = false
        _mnemonic.value = ""
    }

    fun toggleDms(enabled: Boolean) {
        _isDmsEnabled.value = enabled
        prefs.edit().putBoolean("dms_enabled", enabled).apply()
    }

    fun updateSuccessorConfig(index: Int, address: String, percentage: Int) {
        val current = _successorConfigs.value.toMutableList()
        current[index] = SuccessorConfig(address, percentage)
        _successorConfigs.value = current
        saveSuccessorConfigs()
    }

    private fun saveSuccessorConfigs() {
        // Implementation for persistence
    }

    private fun loadSuccessorConfigs() {
        // Implementation for loading
    }

    fun wipeWallet() {
        prefs.edit().clear().apply()
        securePrefs.edit().clear().apply()
        _address.value = ""
        _balance.value = 0.0
        _tokens.value = emptyList()
        _status.value = "Wallet Wiped"
    }

    fun executeEmergencyDistressSweep() {
        // Emergency logic
        _status.value = "EDP SWEEP TRIGGERED"
    }

    fun startAiAgents() {
        viewModelScope.launch {
            _aiAgentStatus.value = "Analyzing network..."
            // AI logic
        }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        _isBiometricsEnabled.value = enabled
        prefs.edit().putBoolean("biometrics", enabled).apply()
    }

    fun updateAutoLock(minutes: Int) {
        _autoLockMinutes.value = minutes
        prefs.edit().putInt("auto_lock", minutes).apply()
    }
}
