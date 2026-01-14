package com.c0dex.passmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.c0dex.passmanager.MasterPasswordItem

@Dao
interface MasterPasswordDao {
    @Insert
    suspend fun insertMasterPassword(masterPasswordItem: MasterPasswordItem)

    @Query("SELECT * FROM master_password LIMIT 1")
    suspend fun getMasterPassword(): MasterPasswordItem?

    @Query("DELETE FROM master_password")
    suspend fun deleteMasterPassword()
}