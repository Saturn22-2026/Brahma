package com.brahmnetwork.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceWalletsTest {
    private fun addr(body: String): String {
        val prefix = "BRM1G"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest("$prefix$body".toByteArray(Charsets.UTF_8))
        val chk = digest.take(2).joinToString("") { b -> "%02x".format(b.toInt() and 0xFF) }
        return "$prefix$body$chk"
    }

    @Test
    fun onlyFirstWalletManagesTheSet() {
        val first = addr("ab".repeat(12))
        val other = addr("cd".repeat(12))
        assertTrue(DeviceWallets.canManage(first, first))
        assertFalse(DeviceWallets.canManage(other, first))
        assertFalse(DeviceWallets.canManage("", first))
    }

    @Test
    fun otherDevicesKeepOneWallet() {
        val mine = listOf(VaultWallet(addr("ab".repeat(12)), "word 1"))
        assertEquals(1, DeviceWallets.maxFor(mine.map { it.address }))
        try {
            DeviceWallets.add(mine, VaultWallet(addr("cd".repeat(12)), "word 2"))
            throw AssertionError("expected limit")
        } catch (e: IllegalStateException) {
            assertEquals("1 wallet limit", e.message)
        }
        assertEquals(1, DeviceWallets.add(emptyList(), mine.first()).size)
    }

    @Test
    fun stopsAtSixtyAndKeepsDuplicatesOut() {
        val slots = listOf(VaultWallet(Constants.OPERATOR_GRANT_ADDR, "word 0")) +
            (1..59).map { VaultWallet(addr("%024x".format(it)), "word $it") }
        assertEquals(60, slots.size)
        try {
            DeviceWallets.add(slots, VaultWallet(addr("%024x".format(61)), "word 61"))
            throw AssertionError("expected limit")
        } catch (e: IllegalStateException) {
            assertEquals("60 wallet limit", e.message)
        }
        val again = DeviceWallets.add(slots, slots.first())
        assertEquals(60, again.size)
    }

    @Test
    fun leftoverFirstIdYieldsToTheWalletOnScreen() {
        val oldFirst = addr("ab".repeat(12))
        val onScreen = addr("cd".repeat(12))
        assertEquals(onScreen, DeviceWallets.placeholderAddress(oldFirst, onScreen, emptyList()))
        assertEquals(onScreen, DeviceWallets.placeholderAddress(oldFirst, onScreen, listOf(oldFirst)))
        assertEquals(oldFirst, DeviceWallets.placeholderAddress(oldFirst, oldFirst, listOf(oldFirst)))
    }

    @Test
    fun leftoverFirstIdAddsPhrasesInsteadOfReplacing() {
        val leftover = addr("ab".repeat(12))
        val onScreen = addr("cd".repeat(12))
        assertTrue(DeviceWallets.recoverShouldAdd(onScreen, leftover, emptyList()))
        assertTrue(DeviceWallets.recoverShouldAdd(onScreen, leftover, listOf(leftover)))
        assertFalse(DeviceWallets.recoverShouldAdd("", leftover, emptyList()))
    }

    @Test
    fun extraWalletCannotAddRecoveredPhrases() {
        val first = addr("ab".repeat(12))
        val extra = addr("cd".repeat(12))
        assertFalse(DeviceWallets.recoverShouldAdd(extra, first, listOf(first, extra)))
        assertTrue(DeviceWallets.recoverShouldAdd(first, first, listOf(first, extra)))
    }

    @Test
    fun operatorGrantBecomesFirstWhenItIsInTheVault() {
        val leftover = addr("ab".repeat(12))
        val grant = Constants.OPERATOR_GRANT_ADDR
        assertEquals(grant, DeviceWallets.promotePrimary(leftover, leftover, listOf(leftover, grant), grant))
        assertEquals(leftover, DeviceWallets.promotePrimary(leftover, leftover, listOf(leftover), grant))
    }

    @Test
    fun multiWalletVaultKeepsStoredFirst() {
        val first = addr("ab".repeat(12))
        val other = addr("cd".repeat(12))
        assertEquals(first, DeviceWallets.placeholderAddress(first, other, listOf(first, other)))
    }

    @Test
    fun roundTripsVaultRows() {
        val slots = listOf(
            VaultWallet(addr("11".repeat(12)), "alpha beta gamma"),
            VaultWallet(addr("22".repeat(12)), "delta epsilon")
        )
        assertEquals(slots, DeviceWallets.decode(DeviceWallets.encode(slots)))
    }
}
