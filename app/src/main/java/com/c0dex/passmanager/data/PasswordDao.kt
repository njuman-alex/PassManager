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

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordItem?
    @Query("UPDATE passwords SET description = :newDescription WHERE id = :id")
    suspend fun updateDescription(id: Long, newDescription: String)

    @Query("SELECT COUNT(*) FROM passwords WHERE description = :description")
    suspend fun getDescriptionCount(description: String): Int
}