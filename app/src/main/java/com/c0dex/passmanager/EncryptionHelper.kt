package com.c0dex.passmanager

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.security.SecureRandom
import java.util.Arrays

object EncryptionHelper {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    private fun createKeyFromPassword(password: String): SecretKeySpec {
        val keyBytes = password.toByteArray(Charsets.UTF_8)
        val key = ByteArray(32) // AES-256
        if (keyBytes.size > 32) {
            // Обрезаем, если длиннее 32
            System.arraycopy(keyBytes, 0, key, 0, 32)
        } else {
            // Дополняем нулями, если короче
            System.arraycopy(keyBytes, 0, key, 0, keyBytes.size)
            Arrays.fill(key, keyBytes.size, 32, 0.toByte())
        }
        return SecretKeySpec(key, ALGORITHM)
    }

    fun encrypt(data: String, password: String): String {
        val key = createKeyFromPassword(password)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        val result = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)

        return Base64.encodeToString(result, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String, password: String): String? {
        try {
            val data = Base64.decode(encryptedData, Base64.DEFAULT)

            val iv = ByteArray(16)
            val encryptedBytes = ByteArray(data.size - 16)

            System.arraycopy(data, 0, iv, 0, iv.size)
            System.arraycopy(data, iv.size, encryptedBytes, 0, encryptedBytes.size)

            val key = createKeyFromPassword(password)

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