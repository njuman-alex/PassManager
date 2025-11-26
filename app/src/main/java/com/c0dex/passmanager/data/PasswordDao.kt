package com.c0dex.passmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PasswordDao {
    @Insert
    suspend fun insertPassword(passwordItem: PasswordItem)

    @Query("SELECT * FROM passwords")
    suspend fun getAllPasswords(): List<PasswordItem>

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePassword(id: Long)
}