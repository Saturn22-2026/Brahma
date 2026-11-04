package com.brahmnetwork.wallet

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * One wallet password. It is never stored; only a PBKDF2-HMAC-SHA256 AES-GCM verifier is kept.
 * The same password also seals every extra wallet on the device except the first.
 */
object VaultPassword {
    const val LOCK_MARK = "locked:"
    const val CHECK_PLAIN = "brahma-vault-ok"
    private const val ITERATIONS = 120_000
    private const val IV_LEN = 12

    fun isLocked(stored: String): Boolean = stored.startsWith(LOCK_MARK)

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun verifier(password: CharArray, salt: ByteArray): String = lock(CHECK_PLAIN, password, salt)

    fun matches(password: CharArray, salt: ByteArray, verifier: String): Boolean {
        return try {
            unlock(verifier, password, salt) == CHECK_PLAIN
        } catch (_: Exception) {
            false
        }
    }

    fun lock(plain: String, password: CharArray, salt: ByteArray): String {
        return lockWithKey(plain, derive(password, salt))
    }

    fun unlock(stored: String, password: CharArray, salt: ByteArray): String {
        return unlockWithKey(stored, derive(password, salt))
    }

    fun lockWithKey(plain: String, key: SecretKey): String {
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(iv.size + enc.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(enc, 0, packed, iv.size, enc.size)
        return LOCK_MARK + Base64.getEncoder().withoutPadding().encodeToString(packed)
    }

    fun unlockWithKey(stored: String, key: SecretKey): String {
        if (!isLocked(stored)) throw IllegalArgumentException("not locked")
        val packed = Base64.getDecoder().decode(stored.removePrefix(LOCK_MARK))
        if (packed.size <= IV_LEN) throw IllegalArgumentException("bad lock")
        val iv = packed.copyOfRange(0, IV_LEN)
        val enc = packed.copyOfRange(IV_LEN, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(enc), Charsets.UTF_8)
    }

    fun derive(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val bytes = factory.generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
