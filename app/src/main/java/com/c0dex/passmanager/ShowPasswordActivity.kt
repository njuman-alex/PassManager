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

class ShowPasswordActivity : AppCompatActivity() {
    private lateinit var descriptionText: TextView
    private lateinit var passwordText: TextView
    private lateinit var masterPasswordEditText: EditText
    private lateinit var decryptButton: Button
    private lateinit var editDescriptionButton: Button
    private lateinit var deletePasswordButton: Button
    private lateinit var toolbar: Toolbar

    private var currentPasswordId: Long = -1
    private var currentEncryptedPassword: String = ""
    private var currentDescription: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_password)

        currentPasswordId = intent.getLongExtra("PASSWORD_ID", -1)
        if (currentPasswordId == -1L) {
            Toast.makeText(this, "Ошибка: пароль не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupClickListeners()
        loadPassword()
    }

    private fun initViews() {
        descriptionText = findViewById(R.id.descriptionText)
        passwordText = findViewById(R.id.passwordText)
        masterPasswordEditText = findViewById(R.id.masterPasswordEditText)
        decryptButton = findViewById(R.id.decryptButton)
        editDescriptionButton = findViewById(R.id.editDescriptionButton)
        deletePasswordButton = findViewById(R.id.deletePasswordButton)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupClickListeners() {
        decryptButton.setOnClickListener {
            val enteredMasterPassword = masterPasswordEditText.text.toString()

            if (enteredMasterPassword.isNotEmpty()) {
                val prefs = getSharedPreferences("password_prefs", MODE_PRIVATE)
                val masterPasswordHash = prefs.getString("master_password_hash", "")
                val saltString = prefs.getString("master_password_salt", "")

                if (masterPasswordHash.isNullOrEmpty() || saltString.isNullOrEmpty()) {
                    Toast.makeText(this, "Мастер-пароль не установлен!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Проверяем, правильный ли мастер-пароль
                if (enteredMasterPassword.hashCode().toString() != masterPasswordHash) {
                    Toast.makeText(this, "Неверный мастер-пароль", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val salt = Base64.decode(saltString, Base64.DEFAULT)
                val decryptedPassword = EncryptionHelper.decrypt(currentEncryptedPassword, enteredMasterPassword, salt) // <-- Введённый пароль и соль

                if (decryptedPassword != null) {
                    // Успешно расшифровано
                    descriptionText.text = currentDescription
                    passwordText.text = decryptedPassword
                    //passwordText.visibility = android.view.View.VISIBLE
                    passwordText.text = decryptedPassword
                    editDescriptionButton.isEnabled = true
                    deletePasswordButton.isEnabled = true
                    descriptionText.isEnabled = true
                    Toast.makeText(this, "Пароль расшифрован!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ошибка расшифровки", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Введите мастер-пароль", Toast.LENGTH_SHORT).show()
            }
        }
        editDescriptionButton.setOnClickListener {
            val newDescription = descriptionText.text.toString()

            if (newDescription.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        val db = PasswordDatabase.getDatabase(this@ShowPasswordActivity)
                        db.passwordDao().updateDescription(currentPasswordId, newDescription)

                        Toast.makeText(this@ShowPasswordActivity, "Описание обновлено!", Toast.LENGTH_SHORT).show()
                        currentDescription = newDescription // Обновляем локально
                    } catch (e: Exception) {
                        Toast.makeText(this@ShowPasswordActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            } else {
                Toast.makeText(this, "Введите описание", Toast.LENGTH_SHORT).show()
            }
        }
        deletePasswordButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val db = PasswordDatabase.getDatabase(this@ShowPasswordActivity)
                    db.passwordDao().deletePassword(currentPasswordId)
                    Toast.makeText(this@ShowPasswordActivity, "Пароль удалён!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // <-- Уведомляем, что были изменения
                    finish() // <-- Закрываем активити
                } catch (e: Exception) {
                    Toast.makeText(this@ShowPasswordActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadPassword() {
        lifecycleScope.launch {
            try {
                val db = PasswordDatabase.getDatabase(this@ShowPasswordActivity)
                val passwordItem = db.passwordDao().getPasswordById(currentPasswordId)

                if (passwordItem != null) {
                    currentEncryptedPassword = passwordItem.encryptedPassword
                    currentDescription = passwordItem.description
                    // Показываем только описание, пока не расшифровали
                    descriptionText.text = currentDescription
                    //passwordText.visibility = android.view.View.GONE // <-- Скрыто до расшифровки
                    passwordText.text = "Расшифруйте пароль"
                } else {
                    Toast.makeText(this@ShowPasswordActivity, "Пароль не найден", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ShowPasswordActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        setResult(RESULT_OK)
        onBackPressed()
        return true
    }
}