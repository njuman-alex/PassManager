package com.c0dex.passmanager

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Крипто-модуль v2.
 *
 * Новая схема (v2): PBKDF2WithHmacSHA256 (120k итераций) + соль 16 байт + AES-256-GCM.
 * Формат строки: "v2:" + Base64(salt || iv || ciphertext+tag)
 *
 * Старая схема (v1, legacy): AES/CBC/PKCS5Padding, ключ = пароль + нули до 32 байт.
 * Формат строки: Base64(iv || ciphertext). Поддерживается ТОЛЬКО для чтения старых записей.
 */
object EncryptionHelper {

    private const val VERSION_PREFIX = "v2:"

    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val KEY_SIZE_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000
    private const val GCM_TAG_BITS = 128

    private const val LEGACY_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val LEGACY_IV_SIZE = 16

    // ================= НОВАЯ СХЕМА (v2) =================

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(data: String, password: String): String {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        val result = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, result, salt.size + iv.size, ciphertext.size)

        return VERSION_PREFIX + Base64.encodeToString(result, Base64.NO_WRAP)
    }

    /**
     * Универсальный decrypt: сам определяет схему по префиксу.
     * Без префикса -> старая запись, читаем legacy-методом (для миграции).
     */
    fun decrypt(encryptedData: String, password: String): String? {
        return if (isLegacy(encryptedData)) {
            decryptLegacy(encryptedData, password)
        } else {
            decryptV2(encryptedData.removePrefix(VERSION_PREFIX), password)
        }
    }

    fun isLegacy(encryptedData: String): Boolean = !encryptedData.startsWith(VERSION_PREFIX)

    private fun decryptV2(payload: String, password: String): String? {
        return try {
            val data = Base64.decode(payload, Base64.NO_WRAP)
            if (data.size < SALT_SIZE + IV_SIZE) return null

            val salt = data.copyOfRange(0, SALT_SIZE)
            val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
            val ciphertext = data.copyOfRange(SALT_SIZE + IV_SIZE, data.size)

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    // ================= СТАРАЯ СХЕМА (v1) — только чтение =================

    private fun createLegacyKey(password: String): SecretKeySpec {
        val keyBytes = password.toByteArray(Charsets.UTF_8)
        val key = ByteArray(32) // AES-256, остаток добьётся нулями сам
        if (keyBytes.size > 32) {
            System.arraycopy(keyBytes, 0, key, 0, 32)
        } else {
            System.arraycopy(keyBytes, 0, key, 0, keyBytes.size)
        }
        return SecretKeySpec(key, "AES")
    }

    private fun decryptLegacy(encryptedData: String, password: String): String? {
        return try {
            val data = Base64.decode(encryptedData, Base64.DEFAULT)
            if (data.size < LEGACY_IV_SIZE) return null

            val iv = data.copyOfRange(0, LEGACY_IV_SIZE)
            val ciphertext = data.copyOfRange(LEGACY_IV_SIZE, data.size)

            val cipher = Cipher.getInstance(LEGACY_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, createLegacyKey(password), IvParameterSpec(iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}