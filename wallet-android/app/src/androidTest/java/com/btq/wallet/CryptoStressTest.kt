package com.btq.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoStressTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setup() {
        cryptoManager = CryptoManager()
    }

    @Test
    fun stressTestMnemonicGeneration() {
        val iterations = 100
        for (i in 1..iterations) {
            val mnemonic = cryptoManager.generateMnemonic(1) // 24 words
            assertNotNull("Mnemonic should not be null at iteration $i", mnemonic)
            assertTrue("Mnemonic should not be empty at iteration $i", mnemonic.isNotEmpty())
            val words = mnemonic.split(" ")
            assertEquals("Mnemonic should have 24 words at iteration $i", 24, words.size)
        }
    }

    @Test
    fun stressTestKeyPairDerivation() {
        val iterations = 100
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art"
        for (i in 1..iterations) {
            val keyPair = cryptoManager.deriveKeyPairFromMnemonic(mnemonic)
            assertNotNull("KeyPair should not be null at iteration $i", keyPair)
            assertTrue("Public key should not be empty at iteration $i", keyPair.publicKey.isNotEmpty())
            assertTrue("Secret key should not be empty at iteration $i", keyPair.secretKey.isNotEmpty())
        }
    }

    @Test
    fun stressTestAddressDerivation() {
        val iterations = 100
        val publicKey = ByteArray(32) { 0 }
        for (i in 1..iterations) {
            val address = cryptoManager.deriveAddress(publicKey)
            assertNotNull("Address should not be null at iteration $i", address)
            assertTrue("Address should not be empty at iteration $i", address.isNotEmpty())
        }
    }

    @Test
    fun stressTestSigning() {
        val iterations = 100
        val secretKey = ByteArray(64) { 0 }
        val message = "Stress test message".toByteArray()
        for (i in 1..iterations) {
            val signature = cryptoManager.signMessage(secretKey, message)
            assertNotNull("Signature should not be null at iteration $i", signature)
            assertTrue("Signature should not be empty at iteration $i", signature.isNotEmpty())
        }
    }
}
