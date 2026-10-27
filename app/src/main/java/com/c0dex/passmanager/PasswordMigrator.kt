package com.c0dex.passmanager

import android.content.Context
import android.util.Log
import com.c0dex.passmanager.data.PasswordDatabase
import com.c0dex.passmanager.data.PasswordItem
import com.google.gson.Gson
import java.io.File

/**
 * Фаза 2а: одноразовая миграция legacy-записей (v1) на v2 + бэкап перед миграцией.
 */
object PasswordMigrator {

    private const val TAG = "PasswordMigrator"

    /** Логический бэкап таблицы в JSON в приватной папке приложения. */
    suspend fun backupDatabase(context: Context): File {
        val db = PasswordDatabase.getDatabase(context)
        val all: List<PasswordItem> = db.passwordDao().getAllPasswords()
        val json = Gson().toJson(all)
        val file = File(context.filesDir, "backup_passwords_${System.currentTimeMillis()}.json")
        file.writeText(json)
        Log.i(TAG, "Backup created: ${file.absolutePath} (${all.size} records)")
        return file
    }

    /** Перешифровывает все записи v1 в v2. Возвращает число мигрированных. */
    suspend fun migrateDatabase(context: Context, masterPassword: String): Int {
        val db = PasswordDatabase.getDatabase(context)
        val legacy = db.passwordDao().getAllPasswords()
            .filter { EncryptionHelper.isLegacy(it.encryptedPassword) }

        var migrated = 0
        for (item in legacy) {
            val plain = EncryptionHelper.decrypt(item.encryptedPassword, masterPassword)
            if (plain != null) {
                val v2 = EncryptionHelper.encrypt(plain, masterPassword)
                db.passwordDao().updateEncryptedPassword(item.id, v2)
                migrated++
            } else {
                Log.w(TAG, "Record id=${item.id} (${item.description}) не расшифровалась, пропущена")
            }
        }
        Log.i(TAG, "Migration done: $migrated / ${legacy.size}")
        return migrated
    }
}