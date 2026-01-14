package com.c0dex.passmanager

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_password")
data class MasterPasswordItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val encryptedPassword: String
)