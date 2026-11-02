package com.brahmnetwork.wallet.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brahmnetwork.wallet.Constants
import com.brahmnetwork.wallet.CryptoManager
import com.brahmnetwork.wallet.DeviceWallets
import com.brahmnetwork.wallet.KeyPair
import com.brahmnetwork.wallet.VaultPassword
import com.brahmnetwork.wallet.VaultWallet
import java.util.Base64
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
    val name: String,
    val symbol: String,
    val amount: Double
)

sealed class AuthRequest {
    data class SendTokens(val receiver: String, val amount: Double, val assetId: Int) : AuthRequest()
    data class Burn(val amount: Double) : AuthRequest()
    data class LaunchL2(val name: String, val symbol: String) : AuthRequest()
    object ShowMnemonic : AuthRequest()
}

enum class PasswordPurpose {
    Vault,
    AppUnlock,
    Send,
    Burn,
    LaunchL2,
    ShowMnemonic,
    EnableAppLock,
    EnableSendLock,
    DisableAppLock,
    DisableSendLock,
}

sealed class VaultPasswordPrompt {
    data class Create(
        val address: String = "",
        val purpose: PasswordPurpose = PasswordPurpose.Vault,
    ) : VaultPasswordPrompt()
    data class Enter(
        val address: String = "",
        val purpose: PasswordPurpose = PasswordPurpose.Vault,
    ) : VaultPasswordPrompt()
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoManager = CryptoManager()
    private val prefs = application.getSharedPreferences("sovereign_prefs", Context.MODE_PRIVATE)
    private val devicePrefs = application.getSharedPreferences("device_wallet_identity", Context.MODE_PRIVATE)
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

    private val _hasWalletPassword = MutableStateFlow(false)
    val hasWalletPassword: StateFlow<Boolean> = _hasWalletPassword.asStateFlow()

    private val _lockOnOpen = MutableStateFlow(false)
    val lockOnOpen: StateFlow<Boolean> = _lockOnOpen.asStateFlow()

    private val _sendLock = MutableStateFlow(prefs.getBoolean("send_lock", true))
    val sendLock: StateFlow<Boolean> = _sendLock.asStateFlow()

    private val _sessionUnlocked = MutableStateFlow(true)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()

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

    private val _walletSlots = MutableStateFlow<List<String>>(emptyList())
    val walletSlots: StateFlow<List<String>> = _walletSlots.asStateFlow()

    private val _hostsManyWallets = MutableStateFlow(false)
    val hostsManyWallets: StateFlow<Boolean> = _hostsManyWallets.asStateFlow()

    /** Only the first wallet on this device may add or recover into the 60-slot vault. */
    private val _canManageVault = MutableStateFlow(false)
    val canManageVault: StateFlow<Boolean> = _canManageVault.asStateFlow()

    private val _canReturnToPrimary = MutableStateFlow(false)
    val canReturnToPrimary: StateFlow<Boolean> = _canReturnToPrimary.asStateFlow()

    private val _passwordPrompt = MutableStateFlow<VaultPasswordPrompt?>(null)
    val passwordPrompt: StateFlow<VaultPasswordPrompt?> = _passwordPrompt.asStateFlow()

    private val _separateWalletsLocked = MutableStateFlow(false)
    val separateWalletsLocked: StateFlow<Boolean> = _separateWalletsLocked.asStateFlow()
    private var pendingAddAfterPassword = false
    private var pendingRecoverMnemonic: String? = null
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
        if (mnemonic.isBlank()) {
            throw IllegalStateException("No signing key for this wallet. Open it from Wallets on this device.")
        }
        if (VaultPassword.isLocked(mnemonic)) {
            throw IllegalStateException("This wallet is locked. Open it with the password before mining.")
        }
        val pair = keysForStored(mnemonic)
        val derived = cryptoManager.deriveAddress(pair.publicKey)
        if (addr.isNotEmpty() && !CryptoManager.sameWalletAddress(derived, addr)) {
            throw IllegalStateException("Key does not match wallet address")
        }
        cachedKeyPair = pair
        cachedKeyAddress = addr.ifEmpty { derived }
        return pair
    }

    private fun mineRpc(): BrahRpc {
        return BrahRpc { rpcUrlList() }.also { wireRpc(it) }
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
        add(Constants.publicHttps())
        val last = prefs.getString("last_good_rpc", "").orEmpty()
        if (!Constants.isPrivateLanRpc(last)) add(last)
        val custom = prefs.getString("rpc_url", "").orEmpty()
        if (!Constants.isPrivateLanRpc(custom)) add(custom)
        com.brahmnetwork.wallet.sentinel.SentinelRuntime.discoveredRpcUrls().forEach { url ->
            if (!Constants.isPrivateLanRpc(url)) add(url)
        }
        Constants.rpcUrlCandidates(preferLan = false).forEach { add(it) }
        if (debug && onOperatorLan()) {
            add(Constants.LAN_RPC_URL)
            if (Constants.isPrivateLanRpc(custom)) add(custom)
            com.brahmnetwork.wallet.sentinel.SentinelRuntime.discoveredRpcUrls().forEach { url ->
                if (Constants.isPrivateLanRpc(url)) add(url)
            }
        }
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
            if (!Constants.isPrivateLanRpc(url)) {
                prefs.edit().putString("last_good_rpc", url).apply()
            }
            val host = CryptoManager.rpcHost(url)
            if (!host.isNullOrBlank()) {
                com.brahmnetwork.wallet.sentinel.SentinelRuntime.rememberRpcHost(host)
            }
        }
    }

    private fun shareBase(): String =
        Constants.shareableNodeBase(Constants.publicHttps(), onOperatorLan = false)

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
        client: BrahRpc = rpc,
        signingAddress: String = "",
        signingKeys: KeyPair? = null,
    ): String = submitMutex.withLock {
        val addr = signingAddress.ifBlank { _address.value }
        if (addr.isEmpty()) throw IllegalStateException("No Wallet")
        if (signingAddress.isNotBlank() && signingKeys == null) {
            throw IllegalStateException("No signing key for $signingAddress")
        }
        if (kind == "mine") _mineStatus.value = "Loading keys…"
        val keyPair = signingKeys ?: withContext(Dispatchers.Default) { cachedKeys() }
        if (kind == "mine") {
            client.onAttempt = { url -> _mineStatus.value = "Trying $url…" }
            _mineStatus.value = "Reading account…"
        }
        val acct = try {
            client.call("brah_getAccount", listOf(addr)).obj().also {
                if (signingAddress.isBlank()) cacheAccount(it)
            }
        } catch (e: Exception) {
            if (signingAddress.isNotBlank()) {
                throw IllegalStateException("Need the node online to claim for this wallet (${e.message ?: "no RPC"})")
            }
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
        val powFrom = CryptoManager.toBrahAddress(addr)
        val powTo = if (to.isEmpty()) "" else CryptoManager.toBrahAddress(to)
        suspend fun signedTx(nonceNow: Long, heightNow: Long, tipNow: String, rewardNow: Long): Map<String, Any> {
            val message = CryptoManager.canonicalMessage(
                Constants.CHAIN_ID, nonceNow, kind, powFrom, powTo, asset, units, memo
            )
            if (kind == "mine") _mineStatus.value = "Signing…"
            val signature = withContext(Dispatchers.Default) {
                cryptoManager.signMessage(keyPair.secretKey, message, keyPair.publicKey)
            }
            val powMessage = message
            val txHash = CryptoManager.sha256Hex(String(powMessage, Charsets.UTF_8))
            val txRoot = CryptoManager.sha256Hex(txHash)
            val header = "$heightNow|$tipNow|$powFrom|$rewardNow|$txRoot"
            val bits = acct.longOr("pow_bits", 20L).toInt()
            val body = mutableMapOf<String, Any>(
                "kind" to kind,
                "from" to powFrom,
                "to" to powTo,
                "asset" to asset,
                "amount_units" to units.toString(),
                "memo" to memo,
                "nonce" to nonceNow,
                "chain_id" to Constants.CHAIN_ID,
                "public_key" to CryptoManager.toHex(keyPair.publicKey),
                "signature" to CryptoManager.toHex(signature),
                "algo" to CryptoManager.ALGO,
                "_height" to heightNow,
                "_tip" to tipNow,
                "_reward" to rewardNow,
                "_root" to txRoot,
                "_bits" to bits,
                "_header" to header
            )
            if (kind == "mine") {
                _mineStatus.value = "Solving PoW ($bits bits)…"
                val powNonce = withContext(Dispatchers.Default) {
                    CryptoManager.mineNonce(header, bits) { tried ->
                        _mineStatus.value = "Solving PoW ($bits bits)… $tried hashes"
                    }
                }
                body["pow_nonce"] = powNonce
            }
            return body
        }
        var height = acct.longOr("chain_height")
        var tip = acct.stringOr("tip_hash")
        var reward = if (kind == "mine") acct.longOr("block_reward_units") else 0L
        var packed = signedTx(nonce, height, tip, reward)
        val powNonce = packed["pow_nonce"] as? Long ?: 0L
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
            if (!CryptoManager.nodeCouldNotCheckSignature(e.message)) throw e
            if (kind == "mine") _mineStatus.value = "Retrying the older signature…"
            val legacyMessage = CryptoManager.canonicalMessage(
                Constants.CHAIN_ID, nonce, kind, powFrom, powTo, asset, units, memo,
                CryptoManager.LEGACY_MSG_PREFIX
            )
            val legacySig = withContext(Dispatchers.Default) {
                cryptoManager.signMessage(keyPair.secretKey, legacyMessage, keyPair.publicKey)
            }
            val legacyTx = tx.toMutableMap()
            legacyTx["signature"] = CryptoManager.toHex(legacySig)
            return try {
                val result = client.call("brah_submitTx", listOf(legacyTx)).obj()
                result.get("hash")?.asString ?: result.toString()
            } catch (again: JsonRpcException) {
                if (CryptoManager.nodeCouldNotCheckSignature(again.message)) {
                    throw IllegalStateException(
                        "The node could not check the Dilithium3 signature. This phrase matches the address. The live node signature checker needs a restart."
                    )
                }
                throw again
            }
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
        client: BrahRpc = rpc,
        signingAddress: String = "",
        signingKeys: KeyPair? = null,
    ): String {
        return try {
            signSubmit(kind, to, asset, amountBrah, memo, client, signingAddress, signingKeys)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if ("Bad nonce" in msg) {
                signSubmit(kind, to, asset, amountBrah, memo, client, signingAddress, signingKeys)
            } else {
                throw e
            }
        }
    }

    /** Sign a join for [address] with its own key. The open wallet stays put. */
    private fun claimJoin(address: String, keys: KeyPair?, prefix: String) {
        viewModelScope.launch {
            val shown = CryptoManager.toBrahAddress(address)
            try {
                val pair = keys ?: withContext(Dispatchers.Default) { cachedKeys() }
                val result = signSubmitOnce(
                    kind = "join",
                    memo = joinMemo(_referrerAddress.value),
                    signingAddress = shown,
                    signingKeys = pair,
                )
                _status.value = "$prefix · $result"
                if (CryptoManager.sameWalletAddress(shown, _address.value)) refreshBalances()
            } catch (e: Exception) {
                _status.value = "$prefix · Claim failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    init {
        SentinelRuntime.onTransaction = { json ->
            viewModelScope.launch { relayMeshTx(json) }
        }
        ensureDeviceVault()
        importDormantWallets()
        _hasWalletPassword.value = hasVaultPassword()
        val lockPref = prefs.getBoolean("lock_on_open", hasVaultPassword())
        _lockOnOpen.value = lockPref
        _sessionUnlocked.value = !_lockOnOpen.value || !hasVaultPassword()
        if (separateWalletsNeedPassword()) {
            _passwordPrompt.value = VaultPasswordPrompt.Create(purpose = PasswordPurpose.Vault)
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

            val loaded = mutableListOf<TokenBalance>()
            for (asset in Constants.L2_ASSETS) {
                val amount = try {
                    rpc.call("eth_getBalance", listOf(addr, asset.id.toString())).obj().doubleOr("amount")
                } catch (_: Exception) {
                    0.0
                }
                loaded.add(TokenBalance(asset.id, asset.name, asset.symbol, amount))
            }
            _tokens.value = loaded

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
                val stored = securePrefs.getString("mnemonic", "") ?: ""
                if (!storedKeyMatches(addr, stored)) {
                    val msg = "Mining failed: no signing key for $addr. Open that wallet from Wallets on this device."
                    _mineStatus.value = msg
                    _status.value = msg
                    return@launch
                }
                val client = mineRpc()
                client.resetActiveUrl()
                _mineStatus.value = "Preparing mine for $addr…"
                _status.value = "Mining $addr…"
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
        // Save referrer only — do not auto-submit join from a deep link.
        _status.value = if (_address.value.isBlank()) {
            "Referrer saved. Create a wallet, then tap Join to claim."
        } else {
            "Referrer saved. Tap Join to claim with this invite."
        }
    }

    fun burnBrahma(amount: Double) {
        if (_address.value.isEmpty()) {
            _status.value = "No Wallet"
            return
        }
        if (amount <= 0.0 || amount.isNaN()) {
            _status.value = "Enter an amount to burn"
            return
        }
        requestSpendAuth(AuthRequest.Burn(amount), PasswordPurpose.Burn)
    }

    fun launchL2(name: String, symbol: String) {
        _status.value = "New L2 launches are closed"
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
        requestSpendAuth(AuthRequest.SendTokens(to, amount, assetId), PasswordPurpose.Send)
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
            is AuthRequest.Burn -> executeBurnAfterAuth()
            is AuthRequest.LaunchL2 -> executeLaunchAfterAuth()
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
                _status.value = "Sealed: $result"
                refreshBalances()
            } catch (e: Exception) {
                _status.value = "Send failed: ${e.message ?: "unknown error"}"
            } finally {
                pendingAuthRequest = null
            }
        }
    }

    private fun executeBurnAfterAuth() {
        val request = pendingAuthRequest as? AuthRequest.Burn ?: return
        viewModelScope.launch {
            try {
                _status.value = "Burning ${request.amount} Brahma Coin..."
                val result = signSubmit("burn", amountBrah = request.amount)
                _status.value = "Burned ${request.amount} Brahma Coin $result"
                refreshBalances()
            } catch (e: Exception) {
                _status.value = "Burn failed: ${e.message ?: "unknown error"}"
            } finally {
                pendingAuthRequest = null
            }
        }
    }

    private fun executeLaunchAfterAuth() {
        pendingAuthRequest = null
        _status.value = "New L2 launches are closed"
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
        if (managesDeviceWallets()) {
            addDeviceWallet()
            return
        }
        if (readVault().isNotEmpty() || firstWalletAddress().isNotBlank()) {
            _status.value = "Only the first wallet on this device can add wallets"
            return
        }
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
            rememberFirstWallet(addr)
            if (managesDeviceWallets()) {
                storeVaultWallet(addr, mnemonic)
            }
            _status.value = "Wallet created"
            if (!hasVaultPassword()) {
                _passwordPrompt.value = VaultPasswordPrompt.Create(purpose = PasswordPurpose.EnableAppLock)
            }
            claimJoin(addr, keyPair, "Wallet created")
        } catch (e: Exception) {
            _status.value = "Create failed: ${e.message ?: "unknown error"}"
        }
    }

    fun recoverWallet(mnemonic: String, isHardware: Boolean) {
        try {
            val normalized = CryptoManager.normalizeMnemonic(mnemonic)
            val keyPair = cryptoManager.deriveKeyPairFromMnemonic(normalized)
            val addr = cryptoManager.deriveAddress(keyPair.publicKey)
            val vaultAddrs = readVault().map { it.address }
            if (DeviceWallets.recoverShouldAdd(_address.value, firstWalletAddress(), vaultAddrs)) {
                reconcilePlaceholderIdentity()
                if (!managesDeviceWallets()) {
                    _status.value = "Only the first wallet on this device can add wallets"
                    return
                }
                if (hasVaultPassword() && !CryptoManager.sameWalletAddress(addr, firstWalletAddress())) {
                    pendingRecoverMnemonic = normalized
                    _passwordPrompt.value = VaultPasswordPrompt.Enter(purpose = PasswordPurpose.Vault)
                    return
                }
                finishRecoverAdd(addr, normalized, null)
                return
            }
            if (_address.value.isNotBlank() && readVault().any { CryptoManager.sameWalletAddress(it.address, addr) }) {
                openVaultAddress(addr)
                return
            }
            if (_address.value.isNotBlank()) {
                _status.value = "Return to the first wallet before adding another phrase"
                return
            }

            _address.value = addr
            _isHardwareColdStorage.value = isHardware
            prefs.edit().apply {
                putString("wallet_address", addr)
                putBoolean("cold_storage", isHardware)
                apply()
            }
            securePrefs.edit().putString("mnemonic", normalized).apply()
            cachedKeyPair = keyPair
            cachedKeyAddress = addr
            devicePrefs.edit().putString("first_wallet_address", CryptoManager.toBrahAddress(addr)).apply()
            storeVaultWallet(addr, normalized)
            publishDeviceWallets()
            _status.value = "Wallet recovered"
            if (!hasVaultPassword()) {
                _passwordPrompt.value = VaultPasswordPrompt.Create(purpose = PasswordPurpose.EnableAppLock)
            }
            claimJoin(addr, keyPair, "Wallet recovered")
        } catch (e: Exception) {
            _status.value = "Recover failed: ${e.message ?: "unknown error"}"
        }
    }

    private fun finishRecoverAdd(addr: String, mnemonic: String, password: CharArray?) {
        val added = storeVaultWallet(addr, mnemonic, password)
        val shown = CryptoManager.toBrahAddress(addr)
        if (!added && readVault().size >= DeviceWallets.MAX
            && readVault().none { CryptoManager.sameWalletAddress(it.address, addr) }
        ) {
            _status.value = "60 wallets already on this device"
            return
        }
        promoteOperatorGrant()
        activatePlain(shown, mnemonic)
        val opened = if (added) {
            "Opened $shown (${readVault().size} of ${DeviceWallets.MAX})"
        } else {
            "Opened $shown"
        }
        if (added) {
            claimJoin(shown, keysForStored(mnemonic), opened)
        } else {
            _status.value = opened
        }
    }

    private fun promoteOperatorGrant() {
        val grant = Constants.OPERATOR_GRANT_ADDR
        if (readVault().none { CryptoManager.sameWalletAddress(it.address, grant) }) return
        if (!CryptoManager.sameWalletAddress(firstWalletAddress(), grant)) {
            devicePrefs.edit().putString("first_wallet_address", CryptoManager.toBrahAddress(grant)).apply()
        }
    }

    private fun openVaultAddress(address: String) {
        val slot = readVault().firstOrNull { CryptoManager.sameWalletAddress(it.address, address) } ?: return
        if (VaultPassword.isLocked(slot.mnemonic)) {
            _passwordPrompt.value = if (hasVaultPassword()) {
                VaultPasswordPrompt.Enter(address, PasswordPurpose.Vault)
            } else {
                VaultPasswordPrompt.Create(address, PasswordPurpose.Vault)
            }
            return
        }
        activateStoredWallet(slot)
    }

    private fun activatePlain(address: String, mnemonic: String) {
        val pair = keysForStored(mnemonic)
        val derived = cryptoManager.deriveAddress(pair.publicKey)
        if (!CryptoManager.sameWalletAddress(derived, address)) {
            _status.value = "Stored key does not match that wallet"
            return
        }
        _address.value = derived
        prefs.edit().putString("wallet_address", derived).apply()
        securePrefs.edit().putString("mnemonic", mnemonic).apply()
        cachedKeyPair = pair
        cachedKeyAddress = derived
        publishDeviceWallets()
        refreshBalances()
    }

    fun requestShowMnemonic() {
        pendingAuthRequest = AuthRequest.ShowMnemonic
        _authFallback.value = null
        _authGate.value = null
        _passwordPrompt.value = if (hasVaultPassword()) {
            VaultPasswordPrompt.Enter(purpose = PasswordPurpose.ShowMnemonic)
        } else {
            VaultPasswordPrompt.Create(purpose = PasswordPurpose.ShowMnemonic)
        }
    }

    private fun revealMnemonic() {
        val stored = securePrefs.getString("mnemonic", "") ?: ""
        _mnemonic.value = when {
            DeviceWallets.isStoredKey(stored) -> "This wallet has no word phrase."
            stored.isBlank() || VaultPassword.isLocked(stored) -> "This wallet is locked with a password."
            else -> stored
        }
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
        publishDeviceWallets()
        _hasWalletPassword.value = false
        _lockOnOpen.value = false
        _sendLock.value = true
        _sessionUnlocked.value = true
        _status.value = "Wallet Wiped"
    }

    fun primaryWalletAddress(): String = firstWalletAddress()

    fun switchDeviceWallet(address: String) {
        val slot = readVault().firstOrNull { CryptoManager.sameWalletAddress(it.address, address) }
        if (slot == null) {
            _status.value = "That wallet is not on this device"
            return
        }
        val returning = CryptoManager.sameWalletAddress(address, firstWalletAddress())
        if (returning) {
            pendingAddAfterPassword = false
            pendingRecoverMnemonic = null
            activateStoredWallet(slot)
            return
        }
        pendingAddAfterPassword = false
        pendingRecoverMnemonic = null
        _passwordPrompt.value = if (hasVaultPassword()) {
            VaultPasswordPrompt.Enter(address, PasswordPurpose.Vault)
        } else {
            VaultPasswordPrompt.Create(address, PasswordPurpose.Vault)
        }
    }

    fun dismissPasswordPrompt() {
        pendingAddAfterPassword = false
        pendingRecoverMnemonic = null
        val prompt = _passwordPrompt.value
        _passwordPrompt.value = null
        val purpose = when (prompt) {
            is VaultPasswordPrompt.Create -> prompt.purpose
            is VaultPasswordPrompt.Enter -> prompt.purpose
            null -> null
        }
        if (isSpendOrRevealPurpose(purpose)) {
            cancelAuth()
        }
    }

    fun requestVaultPassword() {
        pendingAddAfterPassword = false
        pendingRecoverMnemonic = null
        _passwordPrompt.value = if (hasVaultPassword()) {
            VaultPasswordPrompt.Enter(purpose = PasswordPurpose.Vault)
        } else {
            VaultPasswordPrompt.Create(purpose = PasswordPurpose.Vault)
        }
    }

    fun setLockOnOpen(enabled: Boolean) {
        if (enabled) {
            if (!hasVaultPassword()) {
                _passwordPrompt.value = VaultPasswordPrompt.Create(purpose = PasswordPurpose.EnableAppLock)
                return
            }
            writeLockOnOpen(true)
            return
        }
        if (!hasVaultPassword()) {
            writeLockOnOpen(false)
            _sessionUnlocked.value = true
            return
        }
        _passwordPrompt.value = VaultPasswordPrompt.Enter(purpose = PasswordPurpose.DisableAppLock)
    }

    fun setSendLock(enabled: Boolean) {
        if (enabled) {
            if (!hasVaultPassword()) {
                _passwordPrompt.value = VaultPasswordPrompt.Create(purpose = PasswordPurpose.EnableSendLock)
                return
            }
            writeSendLock(true)
            return
        }
        if (!hasVaultPassword()) {
            writeSendLock(false)
            return
        }
        _passwordPrompt.value = VaultPasswordPrompt.Enter(purpose = PasswordPurpose.DisableSendLock)
    }

    fun lockSession() {
        if (_lockOnOpen.value && hasVaultPassword()) {
            _sessionUnlocked.value = false
            hideMnemonic()
            val purpose = when (val prompt = _passwordPrompt.value) {
                is VaultPasswordPrompt.Create -> prompt.purpose
                is VaultPasswordPrompt.Enter -> prompt.purpose
                null -> null
            }
            if (isSpendOrRevealPurpose(purpose)) {
                dismissPasswordPrompt()
            }
        }
    }

    fun unlockWithPassword(password: String): Boolean {
        val chars = password.toCharArray()
        return try {
            if (!passwordMatches(chars)) {
                _status.value = "Wrong password"
                false
            } else {
                _sessionUnlocked.value = true
                _status.value = "Unlocked"
                true
            }
        } finally {
            chars.fill('\u0000')
        }
    }

    fun needsAppUnlock(): Boolean {
        return _address.value.isNotBlank() && _lockOnOpen.value && hasVaultPassword() && !_sessionUnlocked.value
    }

    fun submitVaultPassword(password: String, confirm: String) {
        val prompt = _passwordPrompt.value ?: return
        val purpose = when (prompt) {
            is VaultPasswordPrompt.Create -> prompt.purpose
            is VaultPasswordPrompt.Enter -> prompt.purpose
        }
        if (prompt is VaultPasswordPrompt.Create) {
            if (password.length < 8) {
                _status.value = "Use at least 8 characters"
                return
            }
            if (password != confirm) {
                _status.value = "Passwords do not match"
                return
            }
        }
        val chars = password.toCharArray()
        try {
            if (prompt is VaultPasswordPrompt.Enter && !passwordMatches(chars)) {
                _status.value = "Wrong password"
                return
            }
            if (prompt is VaultPasswordPrompt.Create) {
                sealSeparateWallets(chars)
                _hasWalletPassword.value = true
                writeLockOnOpen(true)
                writeSendLock(true)
                _sessionUnlocked.value = true
            }
            _passwordPrompt.value = null
            when (purpose) {
                PasswordPurpose.Vault -> {
                    val openAddress = when (prompt) {
                        is VaultPasswordPrompt.Create -> prompt.address
                        is VaultPasswordPrompt.Enter -> prompt.address
                    }
                    val pendingPhrase = pendingRecoverMnemonic
                    pendingRecoverMnemonic = null
                    if (pendingPhrase != null) {
                        val recovered = cryptoManager.deriveKeyPairFromMnemonic(pendingPhrase)
                        finishRecoverAdd(
                            cryptoManager.deriveAddress(recovered.publicKey),
                            pendingPhrase,
                            chars
                        )
                    } else if (pendingAddAfterPassword) {
                        pendingAddAfterPassword = false
                        addDeviceWalletWithPassword(chars)
                    } else if (openAddress.isNotBlank()) {
                        openSeparateWallet(openAddress, chars)
                    } else {
                        _status.value = "Password saved"
                    }
                }
                PasswordPurpose.AppUnlock -> {
                    _sessionUnlocked.value = true
                    _status.value = "Unlocked"
                }
                PasswordPurpose.Send, PasswordPurpose.Burn, PasswordPurpose.LaunchL2 -> executePendingAuth()
                PasswordPurpose.ShowMnemonic -> revealMnemonic()
                PasswordPurpose.EnableAppLock -> {
                    writeLockOnOpen(true)
                    _sessionUnlocked.value = true
                }
                PasswordPurpose.EnableSendLock -> writeSendLock(true)
                PasswordPurpose.DisableAppLock -> {
                    writeLockOnOpen(false)
                    _sessionUnlocked.value = true
                }
                PasswordPurpose.DisableSendLock -> writeSendLock(false)
            }
        } catch (_: Exception) {
            _status.value = "Wrong password"
        } finally {
            chars.fill('\u0000')
        }
    }

    private fun addDeviceWallet() {
        if (!managesDeviceWallets()) {
            _status.value = "Only the first wallet on this device can add wallets"
            return
        }
        val current = _address.value
        val phrase = securePrefs.getString("mnemonic", "") ?: ""
        if (phrase.isNotBlank() && !readVault().any { CryptoManager.sameWalletAddress(it.address, current) }) {
            storeVaultWallet(current, phrase)
        }
        val slots = readVault()
        if (!slots.any { CryptoManager.sameWalletAddress(it.address, current) }) {
            _status.value = "This wallet has no phrase stored, so other wallets cannot be added"
            return
        }
        if (slots.size >= DeviceWallets.MAX) {
            _status.value = "60 wallets already on this device"
            return
        }
        if (hasVaultPassword()) {
            pendingAddAfterPassword = true
            _passwordPrompt.value = VaultPasswordPrompt.Enter(purpose = PasswordPurpose.Vault)
            return
        }
        addDeviceWalletWithPassword(null)
    }

    private fun addDeviceWalletWithPassword(password: CharArray?) {
        try {
            val mnemonic = cryptoManager.generateMnemonic(true)
            val keyPair = cryptoManager.deriveKeyPairFromMnemonic(mnemonic)
            val addr = cryptoManager.deriveAddress(keyPair.publicKey)
            if (!storeVaultWallet(addr, mnemonic, password)) {
                _status.value = "Add wallet failed"
                return
            }
            val shown = CryptoManager.toBrahAddress(addr)
            claimJoin(
                shown,
                keyPair,
                "Added $shown (${readVault().size} of ${DeviceWallets.MAX}). Primary wallet stays active",
            )
        } catch (e: Exception) {
            _status.value = "Add wallet failed: ${e.message ?: "unknown error"}"
        }
    }

    private fun importDormantWallets() {
        val dirs = listOfNotNull(
            getApplication<Application>().getExternalFilesDir(null),
            getApplication<Application>().filesDir
        )
        val file = dirs.map { java.io.File(it, "dormant-wallets.tsv") }.firstOrNull { it.isFile } ?: return
        var added = 0
        try {
            file.readLines().forEach { line ->
                try {
                    val parts = line.trim().split('\t')
                    if (parts.size != 3) return@forEach
                    val material = DeviceWallets.signingMaterial(parts[1], parts[2])
                    val pair = cryptoManager.keyPairFromEncoded(parts[1], parts[2])
                    val derived = cryptoManager.deriveAddress(pair.publicKey)
                    if (!CryptoManager.sameWalletAddress(derived, parts[0])) return@forEach
                    if (storeVaultWallet(derived, material)) added += 1
                } catch (_: Exception) {
                    // Skip a bad row. The import file is removed below.
                }
            }
            if (added > 0) {
                _status.value = "Loaded $added wallets. ${readVault().size} of ${DeviceWallets.MAX} on this device."
            }
        } catch (_: Exception) {
            _status.value = "Wallet import failed"
        } finally {
            file.delete()
        }
    }

    private fun ensureDeviceVault() {
        reconcilePlaceholderIdentity()
        val addr = _address.value
        var mnemonic = securePrefs.getString("mnemonic", "") ?: ""
        if (mnemonic.isBlank() && addr.isNotEmpty()) {
            val slot = readVault().firstOrNull { CryptoManager.sameWalletAddress(it.address, addr) }
            if (slot != null && !VaultPassword.isLocked(slot.mnemonic)) {
                mnemonic = slot.mnemonic
                securePrefs.edit().putString("mnemonic", mnemonic).apply()
            }
        }
        if (addr.isNotEmpty() && mnemonic.isNotBlank() && storedKeyMatches(addr, mnemonic)) {
            storeVaultWallet(addr, mnemonic)
        } else {
            publishDeviceWallets()
        }
    }

    private fun reconcilePlaceholderIdentity() {
        val next = DeviceWallets.promotePrimary(
            firstWalletAddress(),
            _address.value,
            readVault().map { it.address },
            Constants.OPERATOR_GRANT_ADDR
        )
        if (next.isNotBlank() && !CryptoManager.sameWalletAddress(next, firstWalletAddress())) {
            devicePrefs.edit().putString("first_wallet_address", next).apply()
        } else if (next.isNotBlank() && firstWalletAddress().isBlank()) {
            devicePrefs.edit().putString("first_wallet_address", next).apply()
        }
    }

    private fun storedKeyMatches(address: String, stored: String): Boolean {
        if (address.isBlank() || stored.isBlank() || VaultPassword.isLocked(stored)) return false
        return try {
            val pair = keysForStored(stored)
            CryptoManager.sameWalletAddress(cryptoManager.deriveAddress(pair.publicKey), address)
        } catch (_: Exception) {
            false
        }
    }

    private fun rememberFirstWallet(address: String) {
        if (address.isBlank()) return
        val existing = firstWalletAddress()
        if (existing.isBlank()) {
            devicePrefs.edit().putString("first_wallet_address", CryptoManager.toBrahAddress(address)).apply()
        }
    }

    private fun firstWalletAddress(): String {
        return devicePrefs.getString("first_wallet_address", "") ?: ""
    }

    private fun managesDeviceWallets(): Boolean {
        return DeviceWallets.canManage(_address.value, firstWalletAddress())
    }

    private fun readVault(): List<VaultWallet> {
        return try {
            DeviceWallets.decode(securePrefs.getString("wallet_vault", "") ?: "")
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun storeVaultWallet(address: String, mnemonic: String, password: CharArray? = null): Boolean {
        val slots = readVault()
        if (slots.any { CryptoManager.sameWalletAddress(it.address, address) }) {
            publishDeviceWallets()
            return false
        }
        if (slots.size >= DeviceWallets.MAX) {
            publishDeviceWallets()
            return false
        }
        val stored = lockIfSeparate(address, mnemonic, password)
        val next = DeviceWallets.add(slots, VaultWallet(CryptoManager.toBrahAddress(address), stored))
        securePrefs.edit().putString("wallet_vault", DeviceWallets.encode(next)).apply()
        publishDeviceWallets()
        return true
    }

    private fun lockIfSeparate(address: String, mnemonic: String, password: CharArray?): String {
        if (CryptoManager.sameWalletAddress(address, firstWalletAddress())) return mnemonic
        // Dilithium key material from dormant import is not a phrase; do not wrap it.
        if (DeviceWallets.isStoredKey(mnemonic)) return mnemonic
        if (!hasVaultPassword()) return mnemonic
        val pass = password ?: throw IllegalStateException("Password required")
        val salt = vaultSalt() ?: throw IllegalStateException("Password required")
        return VaultPassword.lock(mnemonic, pass, salt)
    }

    private fun requestSpendAuth(req: AuthRequest, purpose: PasswordPurpose) {
        pendingAuthRequest = req
        _authFallback.value = null
        _authGate.value = null
        if (!_sendLock.value) {
            executePendingAuth()
            return
        }
        promptWalletPassword(purpose)
    }

    private fun isSpendOrRevealPurpose(purpose: PasswordPurpose?): Boolean {
        return purpose == PasswordPurpose.Send ||
            purpose == PasswordPurpose.Burn ||
            purpose == PasswordPurpose.LaunchL2 ||
            purpose == PasswordPurpose.ShowMnemonic
    }

    private fun promptWalletPassword(purpose: PasswordPurpose) {
        _passwordPrompt.value = if (hasVaultPassword()) {
            VaultPasswordPrompt.Enter(purpose = purpose)
        } else {
            VaultPasswordPrompt.Create(purpose = purpose)
        }
    }

    private fun writeLockOnOpen(enabled: Boolean) {
        _lockOnOpen.value = enabled
        prefs.edit().putBoolean("lock_on_open", enabled).apply()
    }

    private fun writeSendLock(enabled: Boolean) {
        _sendLock.value = enabled
        prefs.edit().putBoolean("send_lock", enabled).apply()
    }

    private fun hasVaultPassword(): Boolean {
        return !securePrefs.getString("vault_password_salt", "").isNullOrBlank()
            && !securePrefs.getString("vault_password_check", "").isNullOrBlank()
    }

    private fun separateWalletsNeedPassword(): Boolean {
        if (hasVaultPassword()) return false
        val first = firstWalletAddress()
        return readVault().any { !CryptoManager.sameWalletAddress(it.address, first) }
    }

    private fun vaultSalt(): ByteArray? {
        val raw = securePrefs.getString("vault_password_salt", "") ?: ""
        if (raw.isBlank()) return null
        return try {
            Base64.getDecoder().decode(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun passwordMatches(password: CharArray): Boolean {
        val salt = vaultSalt() ?: return false
        val check = securePrefs.getString("vault_password_check", "") ?: ""
        if (check.isBlank()) return false
        return VaultPassword.matches(password, salt, check)
    }

    private fun sealSeparateWallets(password: CharArray) {
        val salt = vaultSalt() ?: VaultPassword.newSalt()
        val key = VaultPassword.derive(password, salt)
        val first = firstWalletAddress()
        val next = readVault().map { slot ->
            if (CryptoManager.sameWalletAddress(slot.address, first) || VaultPassword.isLocked(slot.mnemonic)) {
                slot
            } else {
                slot.copy(mnemonic = VaultPassword.lockWithKey(slot.mnemonic, key))
            }
        }
        securePrefs.edit()
            .putString("wallet_vault", DeviceWallets.encode(next))
            .putString("vault_password_salt", Base64.getEncoder().withoutPadding().encodeToString(salt))
            .putString("vault_password_check", VaultPassword.verifier(password, salt))
            .apply()
        publishDeviceWallets()
    }

    private fun openSeparateWallet(address: String, password: CharArray) {
        val slot = readVault().firstOrNull { CryptoManager.sameWalletAddress(it.address, address) } ?: return
        val salt = vaultSalt() ?: return
        val plain = if (VaultPassword.isLocked(slot.mnemonic)) {
            VaultPassword.unlock(slot.mnemonic, password, salt)
        } else {
            slot.mnemonic
        }
        activateStoredWallet(slot.copy(mnemonic = plain))
    }

    private fun keysForStored(stored: String): KeyPair {
        val parsed = DeviceWallets.parseStoredKey(stored)
        if (parsed != null) {
            return cryptoManager.keyPairFromEncoded(parsed.first, parsed.second)
        }
        return cryptoManager.deriveKeyPairFromMnemonic(stored)
    }

    private fun activateStoredWallet(slot: VaultWallet) {
        val keyPair = keysForStored(slot.mnemonic)
        val addr = cryptoManager.deriveAddress(keyPair.publicKey)
        if (!CryptoManager.sameWalletAddress(addr, slot.address)) {
            _status.value = "Stored key does not match that wallet"
            return
        }
        _address.value = addr
        prefs.edit().putString("wallet_address", addr).apply()
        securePrefs.edit().putString("mnemonic", slot.mnemonic).apply()
        cachedKeyPair = keyPair
        cachedKeyAddress = addr
        publishDeviceWallets()
        _status.value = "Using $addr"
        refreshBalances()
    }

    private fun publishDeviceWallets() {
        val slots = readVault()
        _walletSlots.value = slots.map { it.address }
        val manage = managesDeviceWallets()
        _canManageVault.value = manage
        _hostsManyWallets.value = slots.size > 1 || DeviceWallets.recoverShouldAdd(
            _address.value,
            firstWalletAddress(),
            slots.map { it.address }
        )
        _separateWalletsLocked.value = hasVaultPassword()
        val first = firstWalletAddress()
        _canReturnToPrimary.value = first.isNotBlank()
            && _address.value.isNotBlank()
            && !manage
            && slots.any { CryptoManager.sameWalletAddress(it.address, first) }
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
