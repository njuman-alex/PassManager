package com.c0dex.passmanager

import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.c0dex.passmanager.data.PasswordDatabase
import kotlinx.coroutines.launch
import javax.crypto.spec.SecretKeySpec

class SavePasswordActivity : AppCompatActivity() {
    private lateinit var generatedPasswordText: TextView
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var toolbar: Toolbar
    companion object {private const val ALGORITHM = "AES"}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_password)

        initViews()
        setupToolbar()
        setupClickListeners()

        // Получаем сгенерированный пароль из Intent
        val generatedPassword = intent.getStringExtra("GENERATED_PASSWORD")
        generatedPasswordText.text = generatedPassword
    }

    private fun initViews() {
        generatedPasswordText = findViewById(R.id.generatedPasswordText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        saveButton = findViewById(R.id.saveButton)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() { // <-- Добавь
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            val description = descriptionEditText.text.toString()
            val password = generatedPasswordText.text.toString()

            if (description.isNotEmpty() && password.isNotEmpty()) {
                val prefs = getSharedPreferences("password_prefs", MODE_PRIVATE)
                val masterPasswordHash = prefs.getString("master_password_hash", "")
                val keyString = prefs.getString("master_password_key", "") // <-- Получаем ключ

                if (masterPasswordHash.isNullOrEmpty() || keyString.isNullOrEmpty()) {
                    Toast.makeText(this, "Мастер-пароль не установлен!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
                val key = SecretKeySpec(keyBytes, ALGORITHM) // <-- Создай SecretKey
                val encryptedPassword = EncryptionHelper.encrypt(password, key) // <-- Передай ключ

                // Сохраняем в базу
                lifecycleScope.launch {
                    try {
                        val db = PasswordDatabase.getDatabase(this@SavePasswordActivity)
                        val passwordItem = com.c0dex.passmanager.data.PasswordItem(
                            description = description,
                            encryptedPassword = encryptedPassword
                        )
                        db.passwordDao().insertPassword(passwordItem)

                        Toast.makeText(this@SavePasswordActivity, "Пароль сохранён!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish() // Закрываем Activity
                    } catch (e: Exception) {
                        Toast.makeText(this@SavePasswordActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            } else {
                Toast.makeText(this, "Введите описание", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}