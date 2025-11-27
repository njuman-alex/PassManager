package com.c0dex.passmanager

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.SecretKeyFactory
import java.security.SecureRandom
import javax.crypto.SecretKey

object EncryptionHelper {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256

    fun generateKey(password: String, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val key = keyFactory.generateSecret(keySpec)
        return SecretKeySpec(key.encoded, ALGORITHM)
    }

    fun encrypt(data: String, key: SecretKey): String {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Объединяем iv + encryptedData
        val result = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)

        return Base64.encodeToString(result, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String, password: String, salt: ByteArray): String? {
        try {
            val data = Base64.decode(encryptedData, Base64.DEFAULT)

            val iv = ByteArray(16)
            val encryptedBytes = ByteArray(data.size - 16)

            System.arraycopy(data, 0, iv, 0, iv.size)
            System.arraycopy(data, iv.size, encryptedBytes, 0, encryptedBytes.size)

            val key = generateKey(password, salt)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            val decryptedData = cipher.doFinal(encryptedBytes)
            return String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}