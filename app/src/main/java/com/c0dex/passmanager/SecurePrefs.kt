package com.c0dex.passmanager

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Защищённое хранилище секретов: ключи шифруются аппаратным Android Keystore.
 * На диске лежит крипто-каша, а не plaintext.
 */
object SecurePrefs {

    private const val FILE_NAME = "secure_prefs"
    private const val KEY_MASTER = "master_password"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setMaster(context: Context, password: String) {
        prefs(context).edit().putString(KEY_MASTER, password).apply()
    }

    fun getMaster(context: Context): String? {
        return prefs(context).getString(KEY_MASTER, null)
    }

    fun clearMaster(context: Context) {
        prefs(context).edit().remove(KEY_MASTER).apply()
    }
}