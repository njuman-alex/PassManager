package com.c0dex.passmanager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import java.security.SecureRandom
import android.util.Base64

class MasterPasswordActivity : AppCompatActivity() {
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var setButton: Button
    private lateinit var statusText: TextView
    private lateinit var toolbar: Toolbar

    private val PREFS_NAME = "password_prefs"
    private val MASTER_PASSWORD_KEY = "master_password_hash"
    private val MASTER_PASSWORD_HASH_KEY = "master_password_hash"
    private val MASTER_PASSWORD_SALT_KEY = "master_password_salt"
    private val MASTER_PASSWORD_KEY_KEY = "master_password_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_master_password)

        initViews()
        setupToolbar()
        checkMasterPasswordStatus()
        setupClickListeners()
    }

    private fun initViews() {
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        setButton = findViewById(R.id.setButton)
        statusText = findViewById(R.id.statusText)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun checkMasterPasswordStatus() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hasMasterPassword = prefs.contains(MASTER_PASSWORD_KEY)

        if (hasMasterPassword) {
            passwordEditText.isEnabled = false
            confirmPasswordEditText.isEnabled = false
            setButton.isEnabled = false
            statusText.text = "Мастер-пароль установлен"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            statusText.text = ""
        }
    }

    private fun setupClickListeners() {
        setButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (password == confirmPassword && password.length >= 6) {
                setMasterPassword(password)
                Toast.makeText(this, "Мастер-пароль установлен!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Пароли не совпадают или короткие", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setMasterPassword(password: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        // Создаём ключ из пароля
        val key = EncryptionHelper.generateKey(password, salt)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(MASTER_PASSWORD_HASH_KEY, password.hashCode().toString())
            .putString(MASTER_PASSWORD_SALT_KEY, Base64.encodeToString(salt, Base64.DEFAULT))
            .putString(MASTER_PASSWORD_KEY_KEY, Base64.encodeToString(key.encoded, Base64.DEFAULT)) // <-- Сохраняем ключ
            .apply()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}