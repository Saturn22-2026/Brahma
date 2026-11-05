package com.brahmnetwork.wallet

/**
 * Only the device holding [Constants.OPERATOR_GRANT_ADDR] may keep up to [MAX] wallets.
 * Every other device keeps [SINGLE] wallet.
 */
data class VaultWallet(val address: String, val mnemonic: String)

object DeviceWallets {
    const val MAX = 60
    const val SINGLE = 1

    fun isMultiWalletDevice(vaultAddresses: List<String>): Boolean {
        return vaultAddresses.any { CryptoManager.sameWalletAddress(it, Constants.OPERATOR_GRANT_ADDR) }
    }

    fun maxFor(vaultAddresses: List<String>): Int = if (isMultiWalletDevice(vaultAddresses)) MAX else SINGLE
    const val KEY_MARK = "key:"

    fun isStoredKey(stored: String): Boolean = stored.startsWith(KEY_MARK)

    fun signingMaterial(secretHex: String, publicHex: String): String {
        require(secretHex.length == 8000 && secretHex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })
        require(publicHex.length == 3904 && publicHex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })
        return KEY_MARK + secretHex.lowercase() + ":" + publicHex.lowercase()
    }

    fun parseStoredKey(stored: String): Pair<String, String>? {
        if (!isStoredKey(stored)) return null
        val body = stored.removePrefix(KEY_MARK)
        val split = body.indexOf(':')
        if (split <= 0 || split >= body.lastIndex) return null
        return body.substring(0, split) to body.substring(split + 1)
    }

    fun canManage(activeAddress: String, firstWalletAddress: String): Boolean {
        if (firstWalletAddress.isBlank() || activeAddress.isBlank()) return false
        return CryptoManager.sameWalletAddress(activeAddress, firstWalletAddress)
    }

    /**
     * Recovering a phrase must add a vault slot when Home already has an identity.
     * Leftover first-id still counts as the on-screen wallet, not a replace.
     * An extra wallet that is not first cannot add.
     */
    fun recoverShouldAdd(
        activeAddress: String,
        firstWalletAddress: String,
        vaultAddresses: List<String>
    ): Boolean {
        if (activeAddress.isBlank()) return false
        val first = placeholderAddress(firstWalletAddress, activeAddress, vaultAddresses)
        return canManage(activeAddress, first)
    }

    fun isPreferredFirst(address: String, preferred: String): Boolean {
        return preferred.isNotBlank() && CryptoManager.sameWalletAddress(address, preferred)
    }

    fun firstAddress(
        storedFirst: String,
        activeAddress: String,
        vaultAddresses: List<String>,
        preferred: String
    ): String {
        if (preferred.isNotBlank() && vaultAddresses.any { CryptoManager.sameWalletAddress(it, preferred) }) {
            return CryptoManager.toBrahAddress(preferred)
        }
        return placeholderAddress(storedFirst, activeAddress, vaultAddresses)
    }

    fun canOpen(target: String, vaultAddresses: List<String>): Boolean {
        return target.isNotBlank() && vaultAddresses.any { CryptoManager.sameWalletAddress(it, target) }
    }

    fun shouldOpenRecovered(address: String, alreadyInVault: Boolean, preferred: String): Boolean {
        return alreadyInVault || isPreferredFirst(address, preferred)
    }

    /**
     * The wallet on screen is first when the stored first-id is leftover
     * (recover/create overwrote Home, vault is empty or only the old row).
     * A real multi-wallet vault keeps the stored first identity.
     */
    fun placeholderAddress(
        storedFirst: String,
        activeAddress: String,
        vaultAddresses: List<String>
    ): String {
        val active = activeAddress.trim()
        val first = storedFirst.trim()
        if (active.isBlank()) {
            return if (first.isBlank()) "" else CryptoManager.toBrahAddress(first)
        }
        val canonActive = CryptoManager.toBrahAddress(active)
        fun inVault(addr: String): Boolean =
            addr.isNotBlank() && vaultAddresses.any { CryptoManager.sameWalletAddress(it, addr) }
        if (vaultAddresses.isEmpty()) return canonActive
        if (vaultAddresses.size == 1) {
            return if (inVault(canonActive)) CryptoManager.toBrahAddress(vaultAddresses[0]) else canonActive
        }
        if (first.isBlank() || !inVault(first)) return canonActive
        return CryptoManager.toBrahAddress(first)
    }

    /**
     * The live operator grant becomes first when it is already in the vault.
     * Otherwise keep the leftover-first placeholder.
     */
    fun promotePrimary(
        storedFirst: String,
        activeAddress: String,
        vaultAddresses: List<String>,
        preferred: String
    ): String {
        val want = preferred.trim()
        if (want.isNotBlank() && vaultAddresses.any { CryptoManager.sameWalletAddress(it, want) }) {
            return CryptoManager.toBrahAddress(want)
        }
        return placeholderAddress(storedFirst, activeAddress, vaultAddresses)
    }

    fun add(existing: List<VaultWallet>, wallet: VaultWallet): List<VaultWallet> {
        if (existing.any { CryptoManager.sameWalletAddress(it.address, wallet.address) }) {
            return existing
        }
        val cap = maxFor(existing.map { it.address } + wallet.address)
        if (existing.size >= cap) {
            throw IllegalStateException("$cap wallet limit")
        }
        return existing + wallet
    }

    fun encode(slots: List<VaultWallet>): String {
        return slots.joinToString("\n") { slot ->
            slot.address.trim() + "\t" + slot.mnemonic.trim().replace("\n", " ")
        }
    }

    fun decode(raw: String?): List<VaultWallet> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
            val tab = line.indexOf('\t')
            if (tab <= 0 || tab == line.lastIndex) {
                throw IllegalArgumentException("bad vault row")
            }
            VaultWallet(line.substring(0, tab), line.substring(tab + 1))
        }.toList()
    }
}
