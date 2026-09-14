package io.btq.wallet

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import kotlin.random.Random

data class WalletState(
    val address: String = "",
    val balance: Double = 0.0,
    val isGenesis: Boolean = false,
    val logs: List<String> = emptyList(),
    val airdrops: Map<String, Int> = emptyMap()
)

class WalletEngine(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(WalletState())
    val state = _state.asStateFlow()

    fun initialize(referrer: String? = null) {
        scope.launch {
            // 1. Generate Quantum Identity (Simulated)
            val dummyPubKey = "PQC-DILITHIUM-M3-" + Random.nextInt(10000, 99999)
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(dummyPubKey.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val address = "BTQ1G" + hash.take(24).uppercase()

            _state.value = _state.value.copy(address = address)
            addLog("🔐 Sovereign Identity Generated: $address")

            // 2. Sentinel Activation
            val isGenesis = SentinelGateway.checkGenesisStatus(address, referrer)
            _state.value = _state.value.copy(
                isGenesis = isGenesis,
                balance = if (isGenesis) 100.0 else 0.0
            )
            
            if (isGenesis) {
                addLog("🔥 Genesis Activated. 100 BTQ Welcome Gift received.")
                startDaemons()
            }
        }
    }

    private fun startDaemons() {
        // AI Miner
        scope.launch {
            while (isActive) {
                delay(10000)
                val reward = 50.0
                _state.value = _state.value.copy(balance = _state.value.balance + reward)
                addLog("⛏️ AI Miner: Found block! +$reward BTQ")
            }
        }

        // AI Airdrop
        scope.launch {
            val coins = listOf("HOMIE", "POOKIE", "CGOD")
            while (isActive) {
                delay(15000)
                val coin = coins.random()
                val amount = 500
                val currentAirdrops = _state.value.airdrops.toMutableMap()
                currentAirdrops[coin] = (currentAirdrops[coin] ?: 0) + amount
                _state.value = _state.value.copy(airdrops = currentAirdrops)
                addLog("🪂 AI Airdrop: Captured $amount \$$coin")
            }
        }

        // Omni-Faucet
        scope.launch {
            while (isActive) {
                delay(20000)
                val claim = 100.0
                _state.value = _state.value.copy(balance = _state.value.balance + claim)
                addLog("💧 Omni-Faucet: Auto-claimed $claim BTQ")
            }
        }
    }

    private fun addLog(message: String) {
        val currentLogs = _state.value.logs.toMutableList()
        currentLogs.add(0, message) // Newest at top
        _state.value = _state.value.copy(logs = currentLogs.take(50))
    }
}
