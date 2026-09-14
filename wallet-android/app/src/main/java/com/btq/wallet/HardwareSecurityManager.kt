package com.btq.wallet

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class HardwareSecurityManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun ensureHardwareKey(alias: String) {
        if (!keyStore.containsAlias(alias)) {
            try {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
                )
                val specBuilder = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    try {
                        specBuilder.setIsStrongBoxBacked(true)
                        keyGenerator.init(specBuilder.build())
                        keyGenerator.generateKey()
                        return
                    } catch (e: Exception) {
                        specBuilder.setIsStrongBoxBacked(false)
                    }
                }
                
                keyGenerator.init(specBuilder.build())
                keyGenerator.generateKey()
            } catch (e: Exception) {
                android.util.Log.e("HardwareSecurityManager", "Failed to generate key", e)
            }
        }
    }

    fun signWithHardware(alias: String, data: ByteArray): ByteArray {
        ensureHardwareKey(alias)
        val key = keyStore.getKey(alias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun wipeHardwareKeys(alias: String) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }
}
