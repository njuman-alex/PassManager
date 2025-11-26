package com.c0dex.passmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val encryptedPassword: String
)