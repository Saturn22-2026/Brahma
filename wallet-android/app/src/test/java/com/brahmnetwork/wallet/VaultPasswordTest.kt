package com.brahmnetwork.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultPasswordTest {
    @Test
    fun locksAndUnlocksWithTheSamePassword() {
        val salt = VaultPassword.newSalt()
        val password = "correct horse".toCharArray()
        val plain = "key:abcd:ef01 phrase stays intact"
        val locked = VaultPassword.lock(plain, password, salt)
        assertTrue(VaultPassword.isLocked(locked))
        assertEquals(plain, VaultPassword.unlock(locked, password, salt))
        val check = VaultPassword.verifier(password, salt)
        assertTrue(VaultPassword.matches(password, salt, check))
        assertFalse(VaultPassword.matches("wrong horse".toCharArray(), salt, check))
    }

    @Test
    fun verifierDoesNotEmbedThePassword() {
        val salt = VaultPassword.newSalt()
        val password = "never-store-this".toCharArray()
        val check = VaultPassword.verifier(password, salt)
        assertTrue(VaultPassword.isLocked(check))
        assertFalse(String(check.toCharArray()).contains("never-store-this"))
        assertTrue(VaultPassword.matches(password, salt, check))
        assertFalse(VaultPassword.matches("never-store-that".toCharArray(), salt, check))
    }

    @Test
    fun twoLocksOfTheSameSecretDiffer() {
        val salt = VaultPassword.newSalt()
        val password = "another secret".toCharArray()
        val first = VaultPassword.lock("same", password, salt)
        val second = VaultPassword.lock("same", password, salt)
        assertNotEquals(first, second)
        assertEquals("same", VaultPassword.unlock(second, password, salt))
    }
}
