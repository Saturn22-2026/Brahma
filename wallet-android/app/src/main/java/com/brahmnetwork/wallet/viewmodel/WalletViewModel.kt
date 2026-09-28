package com.brahmnetwork.wallet.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brahmnetwork.wallet.Constants
import com.brahmnetwork.wallet.CryptoManager
import com.brahmnetwork.wallet.KeyPair
import com.brahmnetwork.wallet.sentinel.SentinelRuntime
import com.brahmnetwork.wallet.network.BrahRpc
import com.brahmnetwork.wallet.network.JsonRpcException
import com.brahmnetwork.wallet.network.boolOr
import com.brahmnetwork.wallet.network.doubleOr
import com.brahmnetwork.wallet.network.longOr
import com.brahmnetwork.wallet.network.obj
import com.brahmnetwork.wallet.network.stringOr
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean

data class TokenBalance(
    val id: Int,
    val symbol: String,
    val amount: Double
)

sealed class AuthRequest {
    data class SendTokens(val receiver: String, val amount: Double, val assetId: Int) : AuthRequest()
    object ShowMnemonic : AuthRequest()
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoManager = CryptoManager()
    private val prefs = application.getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
    private val securePrefs = openSecurePrefs(application)
    private var rpc = BrahRpc { rpcUrlList() }.also { wireRpc(it) }
    private val gson = Gson()
    private val submitMutex = Mutex()

    // --- State Flows ---
    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _tokens = MutableStateFlow(emptyList<TokenBalance>())
    val tokens: StateFlow<List<TokenBalance>> = _tokens.asStateFlow()

    private val _chainHeight = MutableStateFlow(0L)
    val chainHeight: StateFlow<Long> = _chainHeight.asStateFlow()

    private val _difficulty = MutableStateFlow(0L)
    val difficulty: StateFlow<Long> = _difficulty.asStateFlow()

    private val _totalMined = MutableStateFlow(0.0)
    val totalMined: StateFlow<Double> = _totalMined.asStateFlow()

    private val _hashRate = MutableStateFlow(0.0)
    val hashRate: StateFlow<Double> = _hashRate.asStateFlow()

    private val _blockReward = MutableStateFlow(0.00014)
    val blockReward: StateFlow<Double> = _blockReward.asStateFlow()

    private val _firstMinePending = MutableStateFlow(true)
    val firstMinePending: StateFlow<Boolean> = _firstMinePending.asStateFlow()

    private val _joinedCount = MutableStateFlow(0L)
    val joinedCount: StateFlow<Long> = _joinedCount.asStateFlow()

    private val _status = MutableStateFlow("Disconnected")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _mineStatus = MutableStateFlow("")
    val mineStatus: StateFlow<String> = _mineStatus.asStateFlow()

    private val _mining = MutableStateFlow(false)
    val mining: StateFlow<Boolean> = _mining.asStateFlow()

    private val _address = MutableStateFlow(
        CryptoManager.toBrahAddress(prefs.getString("wallet_address", "") ?: "")
    )
    val address: StateFlow<String> = _address.asStateFlow()

    private val _isHardwareColdStorage = MutableStateFlow(prefs.getBoolean("cold_storage", false))
    val isHardwareColdStorage: StateFlow<Boolean> = _isHardwareColdStorage.asStateFlow()

    private val _hideL1Balances = MutableStateFlow(
        prefs.getBoolean("hide_l1_balances", prefs.getBoolean("hide_balances", false))
    )
    val hideL1Balances: StateFlow<Boolean> = _hideL1Balances.asStateFlow()

    private val _hideL2Balances = MutableStateFlow(
        prefs.getBoolean("hide_l2_balances", prefs.getBoolean("hide_balances", false))
    )
    val hideL2Balances: StateFlow<Boolean> = _hideL2Balances.asStateFlow()

    /** @deprecated Prefer [hideL1Balances] / [hideL2Balances]. True only when both are hidden. */
    val hideBalances: StateFlow<Boolean> = _hideL1Balances

    private val _totalUsers = MutableStateFlow(0L)
    val totalUsers: StateFlow<Long> = _totalUsers.asStateFlow()

    private val _mnemonic = MutableStateFlow("")
    val mnemonic: StateFlow<String> = _mnemonic.asStateFlow()

    private val _isMnemonicVisible = MutableStateFlow(false)
    val isMnemonicVisible: StateFlow<Boolean> = _isMnemonicVisible.asStateFlow()

    private val _autoLockMinutes = MutableStateFlow(prefs.getInt("auto_lock", 5))
    val autoLockMinutes: StateFlow<Int> = _autoLockMinutes.asStateFlow()

    private val _isBiometricsEnabled = MutableStateFlow(prefs.getBoolean("biometrics", false))
    val isBiometricsEnabled: StateFlow<Boolean> = _isBiometricsEnabled.asStateFlow()

    // --- Phase 2.0 & 2.1 State Flows ---
    private val _genesisRank = MutableStateFlow(0L)
    val genesisRank: StateFlow<Long> = _genesisRank.asStateFlow()

    private val _referrerAddress = MutableStateFlow(
        CryptoManager.toBrahAddress(prefs.getString("referrer_address", "") ?: "")
    )
    val referrerAddress: StateFlow<String> = _referrerAddress.asStateFlow()

    private val _referralTreeCount = MutableStateFlow(prefs.getInt("referral_tree_count", 0))
    val referralTreeCount: StateFlow<Int> = _referralTreeCount.asStateFlow()

    private val _meshPeers = MutableStateFlow(0L)
    val meshPeers: StateFlow<Long> = _meshPeers.asStateFlow()

    private var pendingAuthRequest: AuthRequest? = null
    private val _authGate = MutableStateFlow<AuthRequest?>(null)
    val authGate: StateFlow<AuthRequest?> = _authGate.asStateFlow()
    private val _authFallback = MutableStateFlow<AuthRequest?>(null)
    val authFallback: StateFlow<AuthRequest?> = _authFallback.asStateFlow()
    private val _rpcUrl = MutableStateFlow(prefs.getString("rpc_url", "") ?: "")
    val rpcUrl: StateFlow<String> = _rpcUrl.asStateFlow()
    private val miningBusy = AtomicBoolean(false)
    @Volatile private var cachedKeyPair: KeyPair? = null
    @Volatile private var cachedKeyAddress: String? = null

    private fun clearCachedKeys() {
        cachedKeyPair = null
        cachedKeyAddress = null
    }

    private fun cachedKeys(): KeyPair {
        val addr = _address.value
        cachedKeyPair?.let { if (cachedKeyAddress == addr && addr.isNotEmpty()) return it }
        val mnemonic = securePrefs.getString("mnemonic", "") ?: ""
        if (mnemonic.isBlank()) throw IllegalStateException("No signing key")
        val pair = cryptoManager.deriveKeyPairFromMnemonic(mnemonic)
        val derived = cryptoManager.deriveAddress(pair.publicKey)
        if (addr.isNotEmpty() && !CryptoManager.sameWalletAddress(derived, addr)) {
            throw IllegalStateException("Key does not match wallet address")
        }
        cachedKeyPair = pair
        cachedKeyAddress = addr.ifEmpty { derived }
        return pair
    }

    private fun mineRpc(): BrahRpc {
        val debug = com.brahmnetwork.wallet.BuildConfig.DEBUG
        val urls = mutableListOf<String>()
        fun add(url: String) {
            val raw = url.trim()
            if (raw.isBlank()) return
            val normalized = if (raw.endsWith("/")) raw else "$raw/"
            if (!CryptoManager.isAllowedRpcUrl(normalized, debug)) return
            if (normalized !in urls) urls.add(normalized)
        }
        if (onOperatorLan()) {
            add(Constants.LAN_RPC_URL)
            add(prefs.getString("rpc_url", "").orEmpty())
            if (urls.isEmpty()) add(Constants.LAN_RPC_URL)
            return BrahRpc { urls }.also { wireRpc(it) }
        }
        add(Constants.publicHttps())
        val custom = prefs.getString("rpc_url", "").orEmpty()
        if (!Constants.isPrivateLanRpc(custom)) add(custom)
        val last = prefs.getString("last_good_rpc", "").orEmpty()
        if (!Constants.isPrivateLanRpc(last)) add(last)
        if (urls.isEmpty()) add(Constants.publicHttps())
        return BrahRpc { urls }.also { wireRpc(it) }
    }

    private fun onOperatorLan(): Boolean {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address) {
                        val parts = addr.hostAddress?.split(".") ?: continue
                        if (parts.size == 4 && parts[0] == "192" && parts[1] == "168" && parts[2] == "168") {
                            return true
                        }
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun rpcUrlList(): List<String> {
        val debug = com.brahmnetwork.wallet.BuildConfig.DEBUG
        val extras = mutableListOf<String>()
        fun add(url: String) {
            val raw = url.trim()
            if (raw.isBlank()) return
            val normalized = if (raw.endsWith("/")) raw else "$raw/"
            if (CryptoManager.isAllowedRpcUrl(normalized, debug)) extras.add(normalized)
        }
        val custom = prefs.getString("rpc_url", "").orEmpty()
        if (onOperatorLan() || !Constants.isPrivateLanRpc(custom)) {
            add(custom)
        }
        if (onOperatorLan()) {
            add(Constants.LAN_RPC_URL)
        }
        val last = prefs.getString("last_good_rpc", "").orEmpty()
        if (onOperatorLan() || !Constants.isPrivateLanRpc(last)) {
            add(last)
        }
        com.brahmnetwork.wallet.sentinel.SentinelRuntime.discoveredRpcUrls().forEach { url ->
            if (onOperatorLan() || !Constants.isPrivateLanRpc(url)) add(url)
        }
        Constants.rpcUrlCandidates(onOperatorLan()).forEach { add(it) }
        val unique = extras.distinct()
        val loopback = unique.filter { CryptoManager.isLoopbackRpc(it) }
        return unique.filter { it !in loopback } + loopback
    }

    private fun cacheAccount(acct: JsonObject) {
        prefs.edit().putString("cached_account", acct.toString()).apply()
    }

    private fun cachedAccount(): JsonObject? {
        val raw = prefs.getString("cached_account", "").orEmpty()
        if (raw.isBlank()) return null
        return try {
            JsonParser.parseString(raw).asJsonObject
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun relayMeshTx(txJson: String) {
        try {
            rpc.call("brah_submitTx", listOf(JsonParser.parseString(txJson).asJsonObject))
            SentinelRuntime.dropQueued(txJson)
        } catch (e: JsonRpcException) {
            SentinelRuntime.dropQueued(txJson)
        } catch (_: Exception) {
        }
    }

    private suspend fun flushMeshQueue() {
        if (miningBusy.get()) return
        for (txJson in SentinelRuntime.queuedTransactions()) {
            relayMeshTx(txJson)
        }
    }

    private fun wireRpc(client: BrahRpc) {
        client.onSuccess = { url ->
            prefs.edit().putString("last_good_rpc", url).apply()
            val host = CryptoManager.rpcHost(url)
            if (!host.isNullOrBlank()) {
                com.brahmnetwork.wallet.sentinel.SentinelRuntime.rememberRpcHost(host)
            }
        }
    }

    private fun shareBase(): String {
        val onLan = onOperatorLan()
        if (!onLan) {
            return Constants.shareableNodeBase(Constants.publicHttps(), onOperatorLan = false)
        }
        val preferred = _rpcUrl.value.ifBlank {
            prefs.getString("last_good_rpc", "").orEmpty().ifBlank {
                com.brahmnetwork.wallet.sentinel.SentinelRuntime.discoveredRpcUrls().firstOrNull().orEmpty()
            }
        }
        return Constants.shareableNodeBase(
            preferred.ifBlank { Constants.LAN_RPC_URL },
            onOperatorLan = true
        )
    }

    fun shareNodeBase(): String = shareBase()

    fun setRpcUrl(url: String) {
        val cleaned = url.trim()
        if (!CryptoManager.isAllowedRpcUrl(cleaned, com.brahmnetwork.wallet.BuildConfig.DEBUG)) {
            _status.value = "RPC URL refused (HTTPS, or debug LAN/localhost only)"
            return
        }
        prefs.edit().putString("rpc_url", cleaned).apply()
        _rpcUrl.value = cleaned
        rpc = BrahRpc { rpcUrlList() }.also { wireRpc(it) }
        com.brahmnetwork.wallet.sentinel.SentinelRuntime.addRpcSeed(cleaned)
        refreshBalances()
    }

    fun shareInvite(context: android.content.Context) {
        val shareBase = shareBase()
        com.brahmnetwork.wallet.ShareActions.shareInvite(
            context,
            _address.value,
            shareBase,
            signedFields = signInviteFields()
        )
    }

    private fun signInviteFields(): Map<String, String>? {
        val addr = _address.value
        val shareBase = shareBase()
        val link = com.brahmnetwork.wallet.ShareActions.inviteMessage(addr, shareBase) ?: return null
        val apk = Constants.shareDownloadUrl(shareBase)
        if (!CryptoManager.isSafeClaimUrl(link) || !CryptoManager.isSafeApkUrl(apk)) return null
        if (!CryptoManager.claimHostMatchesRpc(link, shareBase)) return null
        val ts = System.currentTimeMillis() / 1000
        val keyPair = try {
            cachedKeys()
        } catch (_: Exception) {
            return null
        }
        if (!CryptoManager.sameWalletAddress(cryptoManager.deriveAddress(keyPair.publicKey), addr)) return null
        val sig = cryptoManager.signMessage(
            keyPair.secretKey,
            CryptoManager.inviteCanonical(addr, link, apk, ts),
            keyPair.publicKey
        )
        return mapOf(
            "kind" to "invite",
            "ref" to addr,
            "claim" to link,
            "apk" to apk,
            "ts" to ts.toString(),
            "public_key" to CryptoManager.toHex(keyPair.publicKey),
            "signature" to CryptoManager.toHex(sig)
        )
    }

    private fun openSecurePrefs(application: Application): android.content.SharedPreferences {
        val plain = application.getSharedPreferences("secure_vault", Context.MODE_PRIVATE)
        return try {
            val masterKey = MasterKey.Builder(application)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val enc = EncryptedSharedPreferences.create(
                application,
                "secure_vault_enc",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val old = plain.getString("mnemonic", null)
            if (!old.isNullOrBlank() && enc.getString("mnemonic", null).isNullOrBlank()) {
                enc.edit().putString("mnemonic", old).apply()
                plain.edit().remove("mnemonic").apply()
            }
            enc
        } catch (_: Exception) {
            plain
        }
    }

    private suspend fun signSubmit(
        kind: String,
        to: String = "",
        asset: String = "0",
        amountBrah: Double = 0.0,
        memo: String = "",
        client: BrahRpc = rpc
    ): String = submitMutex.withLock {
        val addr = _address.value
        if (addr.isEmpty()) throw IllegalStateException("No Wallet")
        if (kind == "mine") _mineStatus.value = "Loading keys…"
        val keyPair = withContext(Dispatchers.Default) { cachedKeys() }
        if (kind == "mine") {
            client.onAttempt = { url -> _mineStatus.value = "Trying $url…" }
            _mineStatus.value = "Reading account…"
        }
        val acct = try {
            client.call("brah_getAccount", listOf(addr)).obj().also { cacheAccount(it) }
        } catch (e: Exception) {
            cachedAccount() ?: throw IllegalStateException("Need one online sync before offline mine (${e.message ?: "no RPC"})")
        }
        if (kind == "mine") {
            val via = client.activeUrl.ifBlank { "node" }
            _mineStatus.value = "Connected: $via"
        }
        var nonce = acct.longOr("nonce")
        val units = if (asset == "0") {
            CryptoManager.toUnits(amountBrah)
        } else {
            amountBrah.toLong()
        }
        suspend fun signedTx(nonceNow: Long, heightNow: Long, tipNow: String, rewardNow: Long): Map<String, Any> {
            val message = CryptoManager.canonicalMessage(
                Constants.CHAIN_ID, nonceNow, kind, addr, to, asset, units, memo
            )
            if (kind == "mine") _mineStatus.value = "Signing…"
            val signature = withContext(Dispatchers.Default) {
                cryptoManager.signMessage(keyPair.secretKey, message, keyPair.publicKey)
            }
            val powFrom = CryptoManager.toBrahAddress(addr)
            val powTo = if (to.isEmpty()) "" else CryptoManager.toBrahAddress(to)
            val powMessage = CryptoManager.canonicalMessage(
                Constants.CHAIN_ID, nonceNow, kind, powFrom, powTo, asset, units, memo
            )
            val txHash = CryptoManager.sha256Hex(String(powMessage, Charsets.UTF_8))
            val txRoot = CryptoManager.sha256Hex(txHash)
            val header = "$heightNow|$tipNow|$powFrom|$rewardNow|$txRoot"
            val bits = acct.longOr("pow_bits", 20L).toInt()
            if (kind == "mine") _mineStatus.value = "Solving PoW ($bits bits)…"
            val powNonce = withContext(Dispatchers.Default) {
                CryptoManager.mineNonce(header, bits) { tried ->
                    _mineStatus.value = "Solving PoW ($bits bits)… $tried hashes"
                }
            }
            return mapOf(
                "kind" to kind,
                "from" to addr,
                "to" to to,
                "asset" to asset,
                "amount_units" to units.toString(),
                "memo" to memo,
                "nonce" to nonceNow,
                "chain_id" to Constants.CHAIN_ID,
                "public_key" to CryptoManager.toHex(keyPair.publicKey),
                "signature" to CryptoManager.toHex(signature),
                "algo" to CryptoManager.ALGO,
                "pow_nonce" to powNonce,
                "_height" to heightNow,
                "_tip" to tipNow,
                "_reward" to rewardNow,
                "_root" to txRoot,
                "_bits" to bits,
                "_header" to header
            )
        }
        var height = acct.longOr("chain_height")
        var tip = acct.stringOr("tip_hash")
        var reward = if (kind == "mine") acct.longOr("block_reward_units") else 0L
        var packed = signedTx(nonce, height, tip, reward)
        val powNonce = packed["pow_nonce"] as Long
        val txRoot = packed["_root"] as String
        val bits = packed["_bits"] as Int
        val header = packed["_header"] as String
        val tx = packed.filterKeys { !it.startsWith("_") }
        val blockHash = CryptoManager.sha256Hex("$header:$powNonce")
        val via = client.activeUrl.ifBlank { "node" }
        if (kind == "mine") _mineStatus.value = "Submitting via $via…"
        return try {
            val result = client.call("brah_submitTx", listOf(tx)).obj()
            if (kind == "mine") {
                try {
                    SentinelRuntime.gossipBlock(
                        mapOf(
                            "index" to (height + 1),
                            "timestamp" to (System.currentTimeMillis() / 1000),
                            "previous_hash" to tip,
                            "miner" to CryptoManager.toBrahAddress(addr),
                            "nonce" to powNonce,
                            "hash" to blockHash,
                            "tx_root" to txRoot,
                            "txs" to listOf(tx),
                            "reward" to reward,
                            "pow_bits" to bits
                        )
                    )
                } catch (_: Exception) {
                }
            }
            result.get("hash")?.asString ?: result.toString()
        } catch (e: JsonRpcException) {
            throw e
        } catch (e: Exception) {
            val txJson = gson.toJson(tx)
            SentinelRuntime.gossipTransaction(txJson)
            if (kind == "mine") {
                try {
                    SentinelRuntime.gossipBlock(
                        mapOf(
                            "index" to (height + 1),
                            "timestamp" to (System.currentTimeMillis() / 1000),
                            "previous_hash" to tip,
                            "miner" to CryptoManager.toBrahAddress(addr),
                            "nonce" to powNonce,
                            "hash" to blockHash,
                            "tx_root" to txRoot,
                            "txs" to listOf(tx),
                            "reward" to reward,
                            "pow_bits" to bits
                        )
                    )
                } catch (_: Exception) {
                }
                repeat(4) {
                    delay(400)
                    try {
                        val after = client.call("brah_getStats", listOf(addr)).obj()
                        if (after.longOr("chain_height") > height) {
                            SentinelRuntime.dropQueued(txJson)
                            return blockHash
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            return "Queued on mesh — seals when a node is in range"
        }
    }

    private suspend fun signSubmitOnce(
        kind: String,
        to: String = "",
        asset: String = "0",
        amountBrah: Double = 0.0,
        memo: String = "",
        client: BrahRpc = rpc
    ): String {
        return try {
            signSubmit(kind, to, asset, amountBrah, memo, client)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if ("Bad nonce" in msg) {
                signSubmit(kind, to, asset, amountBrah, memo, client)
            } else {
                throw e
            }
        }
    }

    init {
        SentinelRuntime.onTransaction = { json ->
            viewModelScope.launch { relayMeshTx(json) }
        }
        refreshBalances()
        if (_address.value.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.Default) {
                runCatching { cachedKeys() }
            }
        }
    }

    fun refreshBalances() {
        viewModelScope.launch {
            val addr = _address.value
            if (addr.isEmpty()) {
                _status.value = "No Wallet"
                return@launch
            }
            if (!miningBusy.get()) {
                _status.value = "Syncing..."
            }
            var live = false
            var lastError: String? = null

            try {
                val balance = rpc.call("eth_getBalance", listOf(addr, "latest")).obj()
                _balance.value = balance.doubleOr(
                    "Brahma Coin",
                    balance.doubleOr(
                        "Brahma",
                        balance.doubleOr("BRAH", balance.doubleOr("BRAHMA", balance.doubleOr("Brahma Coin", balance.doubleOr("0"))))
                    )
                )
                live = true
            } catch (e: Exception) {
                lastError = e.message
            }

            try {
                    val stats = rpc.call("brah_getStats", listOf(addr)).obj()
                _totalUsers.value = stats.longOr("total_users")
                _genesisRank.value = stats.longOr("genesis_rank")
                _chainHeight.value = stats.longOr("chain_height")
                _difficulty.value = stats.longOr("pow_bits", stats.longOr("difficulty"))
                _totalMined.value = stats.doubleOr("total_mined")
                val nextMine = stats.doubleOr("next_mine_reward")
                val baseMine = stats.doubleOr("block_reward")
                _blockReward.value = when {
                    nextMine > 0.0 -> nextMine
                    baseMine > 0.0 -> baseMine
                    else -> 0.00014
                }
                _firstMinePending.value = stats.boolOr("first_mine_pending")
                _joinedCount.value = stats.longOr("joined_count")
                _hashRate.value = 0.0
                _referralTreeCount.value = stats.longOr("referral_tree").toInt()
                prefs.edit().putInt("referral_tree_count", _referralTreeCount.value).apply()
                _meshPeers.value = stats.longOr("p2p_peers")
                live = true
            } catch (e: Exception) {
                lastError = e.message ?: lastError
            }

            _tokens.value = emptyList()

            if (!miningBusy.get()) {
                _status.value = if (live) {
                    "Live"
                } else {
                    "Offline: ${lastError ?: "cannot reach node"} · mesh will carry mine/join"
                }
            }
            if (live) {
                flushMeshQueue()
            }
        }
    }

    fun mineBrahma() {
        if (!miningBusy.compareAndSet(false, true)) {
            _mineStatus.value = "Already mining — wait for the current block"
            return
        }
        _mining.value = true
        viewModelScope.launch {
            try {
                val addr = _address.value
                if (addr.isEmpty()) {
                    _mineStatus.value = "No Wallet"
                    _status.value = "No Wallet"
                    return@launch
                }
                val client = mineRpc()
                client.resetActiveUrl()
                _mineStatus.value = "Preparing mine…"
                _status.value = "Mining..."
                val result = withTimeout(600_000) {
                    signSubmitOnce("mine", client = client)
                }
                val msg = if (result.startsWith("Queued")) result else "Block sealed: $result"
                _mineStatus.value = msg
                _status.value = msg
                refreshBalances()
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                val msg = "Mining failed: timed out. Tap Mine again."
                _mineStatus.value = msg
                _status.value = msg
            } catch (e: Exception) {
                val msg = "Mining failed: ${e.message ?: "unknown error"}"
                _mineStatus.value = msg
                _status.value = msg
            } finally {
                miningBusy.set(false)
                _mining.value = false
            }
        }
    }

    fun setInactivitySweep(enabled: Boolean) {
        _isHardwareColdStorage.value = !enabled
        prefs.edit().putBoolean("cold_storage", !enabled).apply()
    }

    fun setHideBalances(hidden: Boolean) {
        setHideL1Balances(hidden)
        setHideL2Balances(hidden)
    }

    fun setHideL1Balances(hidden: Boolean) {
        _hideL1Balances.value = hidden
        prefs.edit().putBoolean("hide_l1_balances", hidden).apply()
    }

    fun setHideL2Balances(hidden: Boolean) {
        _hideL2Balances.value = hidden
        prefs.edit().putBoolean("hide_l2_balances", hidden).apply()
    }

    /** Stable phone id for one-join-per-device (ANDROID_ID). */
    fun deviceJoinId(): String {
        val cached = prefs.getString("join_device_id", null)
        if (!cached.isNullOrBlank()) return cached
        val androidId = try {
            android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
        } catch (_: Exception) {
            null
        }
        val id = (androidId?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            ?: java.util.UUID.randomUUID().toString()).take(64)
        prefs.edit().putString("join_device_id", id).apply()
        return id
    }

    fun joinMemo(referrer: String = _referrerAddress.value): String {
        val ref = CryptoManager.toBrahAddress(referrer.trim())
        val dev = deviceJoinId()
        return if (ref.isNotEmpty()) "dev:$dev:$ref" else "dev:$dev"
    }

    fun setReferrer(address: String) {
        val canonical = CryptoManager.toBrahAddress(address.trim())
        _referrerAddress.value = canonical
        prefs.edit().putString("referrer_address", canonical).apply()
    }

    fun applyClaimLink(url: String) {
        val ref = CryptoManager.referrerFromClaimUrl(url) ?: return
        setReferrer(ref)
        _status.value = "Referrer saved from invite"
        if (_address.value.isBlank()) return
        viewModelScope.launch {
            try {
                val result = signSubmitOnce("join", memo = joinMemo(_referrerAddress.value))
                _status.value = "Claim sealed: $result"
                refreshBalances()
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                _status.value = if ("Already claimed" in msg && "Referral" !in msg) {
                    "Referrer saved"
                } else {
                    "Claim failed: ${e.message ?: "unknown error"}"
                }
            }
        }
    }

    fun burnBrahma(amount: Double) {
        viewModelScope.launch {
            try {
                if (_address.value.isEmpty()) {
                    _status.value = "No Wallet"
                    return@launch
                }
                if (amount <= 0.0) {
                    _status.value = "Enter an amount to burn"
                    return@launch
                }
                _status.value = "Burning $amount Brahma Coin..."
                val result = signSubmit("burn", amountBrah = amount)
                _status.value = "Burned $amount Brahma Coin $result"
                refreshBalances()
            } catch (e: Exception) {
                _status.value = "Burn failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun launchL2(name: String, symbol: String) {
        viewModelScope.launch {
            try {
                val addr = _address.value
                if (addr.isEmpty()) {
                    _status.value = "No Wallet"
                    return@launch
                }
                _status.value = "Burning ${Constants.L2_LAUNCH_BURN.toInt()} Brahma Coin..."
                val result = signSubmit("launch_l2", memo = "$name|$symbol")
                _status.value = "L2 launched (100 Brahma Coin burned) $result"
                refreshBalances()
            } catch (e: Exception) {
                _status.value = "L2 launch failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun requestSendTokens(receiver: String, amount: Double, assetId: Int) {
        if (amount <= 0.0 || amount.isNaN()) {
            _status.value = "Amount must be positive"
            return
        }
        val to = receiver.trim()
        if (!CryptoManager.isChecksummedAddress(to)) {
            _status.value = "Recipient must be a checksummed BRM1G address"
            return
        }
        val req = AuthRequest.SendTokens(to, amount, assetId)
        pendingAuthRequest = req
        _authFallback.value = null
        _authGate.value = req
    }

    fun clearAuthGate() {
        _authGate.value = null
    }

    fun showAuthFallback() {
        _authFallback.value = pendingAuthRequest
    }

    fun executePendingAuth() {
        _authGate.value = null
        _authFallback.value = null
        when (pendingAuthRequest) {
            is AuthRequest.SendTokens -> executeSendAfterAuth()
            AuthRequest.ShowMnemonic -> revealMnemonic()
            null -> {}
        }
    }

    fun cancelAuth() {
        pendingAuthRequest = null
        _authGate.value = null
        _authFallback.value = null
    }

    fun reportAuthError(message: String) {
        _status.value = message
        cancelAuth()
    }

    private fun executeSendAfterAuth() {
        val request = pendingAuthRequest as? AuthRequest.SendTokens ?: return
        viewModelScope.launch {
            try {
                val addr = _address.value
                if (addr.isEmpty()) {
                    _status.value = "No Wallet"
                    return@launch
                }
                _status.value = "Signing ML-DSA..."
                val result = signSubmit(
                    kind = "send",
                    to = request.receiver,
                    asset = request.assetId.toString(),
                    amountBrah = request.amount
                )
                _status.value = "Sent: $result"
                refreshBalances()
            } catch (e: Exception) {
                _status.value = "Send failed: ${e.message ?: "unknown error"}"
            } finally {
                pendingAuthRequest = null
            }
        }
    }

    fun claimFaucet() {
        viewModelScope.launch {
            try {
                val address = _address.value.trim()
                if (address.isEmpty()) {
                    _status.value = "No Wallet — create one first"
                    return@launch
                }
                _status.value = "Signing claim..."
                val ref = CryptoManager.toBrahAddress(_referrerAddress.value.trim())
                if (ref.isNotEmpty()) setReferrer(ref)
                val result = signSubmitOnce("join", memo = joinMemo(_referrerAddress.value))
                _status.value = "Claim sealed: $result"
                refreshBalances()
            } catch (e: Exception) {
                _status.value = "Faucet failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun createWallet(is24Words: Boolean, isHardware: Boolean) {
        try {
            val mnemonic = cryptoManager.generateMnemonic(true)
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
            cachedKeyPair = keyPair
            cachedKeyAddress = addr
            _status.value = "Wallet created"
            viewModelScope.launch {
                val ref = _referrerAddress.value.trim()
                if (ref.isNotEmpty()) {
                    try {
                        signSubmitOnce("join", memo = joinMemo(ref))
                    } catch (_: Exception) {
                    }
                }
                refreshBalances()
            }
        } catch (e: Exception) {
            _status.value = "Create failed: ${e.message ?: "unknown error"}"
        }
    }

    fun recoverWallet(mnemonic: String, isHardware: Boolean) {
        try {
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
            cachedKeyPair = keyPair
            cachedKeyAddress = addr
            _status.value = "Wallet recovered"
            refreshBalances()
        } catch (e: Exception) {
            _status.value = "Recover failed: ${e.message ?: "unknown error"}"
        }
    }

    fun requestShowMnemonic() {
        pendingAuthRequest = AuthRequest.ShowMnemonic
        _authFallback.value = null
        _authGate.value = AuthRequest.ShowMnemonic
    }

    private fun revealMnemonic() {
        _mnemonic.value = securePrefs.getString("mnemonic", "Not found") ?: ""
        _isMnemonicVisible.value = true
        pendingAuthRequest = null
    }

    fun hideMnemonic() {
        _isMnemonicVisible.value = false
        _mnemonic.value = ""
    }

    fun wipeWallet() {
        prefs.edit().clear().apply()
        securePrefs.edit().clear().apply()
        clearCachedKeys()
        _address.value = ""
        _balance.value = 0.0
        _tokens.value = emptyList()
        _status.value = "Wallet Wiped"
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
