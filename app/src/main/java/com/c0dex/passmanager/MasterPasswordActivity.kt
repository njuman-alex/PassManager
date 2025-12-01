package com.c0dex.passmanager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope

class MasterPasswordActivity : AppCompatActivity() {
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var setButton: Button
    private lateinit var resetButton: Button
    private lateinit var statusText: TextView
    private lateinit var toolbar: Toolbar

    private val PREFS_NAME = "password_prefs"
    private val MASTER_PASSWORD_SET_KEY = "master_password_set"
    private val MASTER_PASSWORD_KEY = "master_password"

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
        resetButton = findViewById(R.id.resetButton)
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
        val hasMasterPassword = prefs.getBoolean(MASTER_PASSWORD_SET_KEY, false)

        if (hasMasterPassword) {
            passwordEditText.isEnabled = false
            confirmPasswordEditText.isEnabled = false
            setButton.isEnabled = false
            statusText.text = "Мастер-пароль установлен"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            resetButton.isEnabled = true
        } else {
            statusText.text = "Мастер-пароль еще не установлен"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            resetButton.isEnabled = false
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
        resetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Сброс мастер-пароля")
                .setMessage("Вы действительно хотите сбросить мастер-пароль? " +
                        "Функция сохранения будет недоступна! " +
                        "Для просмотра сохраненных паролей вам все-равно придется ввести прежний мастер-пароль!")
                .setPositiveButton("Да") { _, _ ->
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    prefs.edit()
                        .remove(MASTER_PASSWORD_SET_KEY)
                        .remove(MASTER_PASSWORD_KEY)
                        .apply()

                    Toast.makeText(this, "Мастер-пароль сброшен!", Toast.LENGTH_SHORT).show()
                    checkMasterPasswordStatus()

                    passwordEditText.isEnabled = true
                    confirmPasswordEditText.isEnabled = true
                    setButton.isEnabled = true
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }

    private fun setMasterPassword(password: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putBoolean(MASTER_PASSWORD_SET_KEY, true)
            .putString(MASTER_PASSWORD_KEY, password)
            .apply()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}