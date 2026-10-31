package com.c0dex.passmanager.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [PasswordItem::class],  // Убрали MasterPasswordItem
    version = 3,  // Увеличили с 2 до 3
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    // masterPasswordDao() удалён

    companion object {
        @Volatile
        private var INSTANCE: PasswordDatabase? = null

        // Миграция 2 -> 3: удаляем неиспользуемую таблицу master_password
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS master_password")
            }
        }

        fun getDatabase(context: Context): PasswordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    "password_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}