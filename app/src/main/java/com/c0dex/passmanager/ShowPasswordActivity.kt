package com.c0dex.passmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
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
    private lateinit var descriptionText: EditText // <-- теперь EditText!
    private lateinit var passwordText: TextView
    private lateinit var masterPasswordEditText: EditText
    private lateinit var decryptButton: Button
    private lateinit var copyButton: Button
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
        currentEncryptedPassword = intent.getStringExtra("ENCRYPTED_PASSWORD") ?: ""

        if (currentPasswordId == -1L || currentEncryptedPassword.isEmpty()) {
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
        copyButton = findViewById(R.id.copyButton)
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
            val enteredMasterPassword = masterPasswordEditText.text.toString().trim()

            if (enteredMasterPassword.isNotEmpty()) {
                val decryptedPassword = EncryptionHelper.decrypt(currentEncryptedPassword, enteredMasterPassword)

                if (decryptedPassword != null) {
                    passwordText.text = decryptedPassword
                    decryptButton.isEnabled = false
                    masterPasswordEditText.setText("") // 🔒 очищаем сразу!
                    editDescriptionButton.isEnabled = true
                    copyButton.isEnabled = true
                    deletePasswordButton.isEnabled = true

                    // Разрешаем редактировать описание (но не активируем сразу)
                    descriptionText.isEnabled = true
                    descriptionText.isFocusable = true
                    descriptionText.isFocusableInTouchMode = true

                    Toast.makeText(this, "Пароль расшифрован!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Неверный мастер-пароль", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Введите мастер-пароль", Toast.LENGTH_SHORT).show()
            }
        }

        copyButton.setOnClickListener {
            try {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("password", passwordText.text))
                Toast.makeText(this, "Пароль скопирован", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка копирования", Toast.LENGTH_SHORT).show()
            }
        }

        editDescriptionButton.setOnClickListener {
            if (editDescriptionButton.text == "Изменить описание") {
                // Переключаемся в режим редактирования
                descriptionText.isEnabled = true
                descriptionText.isFocusable = true
                descriptionText.isFocusableInTouchMode = true
                descriptionText.requestFocus()
                editDescriptionButton.text = "Сохранить описание"
            } else {
                // Сохраняем
                val newDesc = descriptionText.text.toString().trim()
                if (newDesc.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            val db = PasswordDatabase.getDatabase(this@ShowPasswordActivity)
                            db.passwordDao().updateDescription(currentPasswordId, newDesc)
                            currentDescription = newDesc
                            Toast.makeText(this@ShowPasswordActivity, "Описание обновлено!", Toast.LENGTH_SHORT).show()

                            // Блокируем поле после сохранения
                            descriptionText.isEnabled = false
                            descriptionText.isFocusable = false
                            editDescriptionButton.text = "Изменить описание"
                        } catch (e: Exception) {
                            Toast.makeText(this@ShowPasswordActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Описание не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
        }

        deletePasswordButton.setOnClickListener {
            deletePasswordButton.isEnabled = false // блокируем повтор
            lifecycleScope.launch {
                try {
                    val db = PasswordDatabase.getDatabase(this@ShowPasswordActivity)
                    db.passwordDao().deletePassword(currentPasswordId)
                    Toast.makeText(this@ShowPasswordActivity, "Пароль удалён!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@ShowPasswordActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                    deletePasswordButton.isEnabled = true // разблокируем при ошибке
                }
            }
        }
    }

    private fun loadPassword() {
        lifecycleScope.launch {
            try {
                val db = PasswordDatabase.getDatabase(this@ShowPasswordActivity)
                val item = db.passwordDao().getPasswordById(currentPasswordId)
                if (item != null) {
                    currentDescription = item.description
                    descriptionText.setText(currentDescription)
                    passwordText.text = "Расшифруйте пароль"

                    // Блокируем описание до расшифровки
                    descriptionText.isEnabled = false
                    descriptionText.isFocusable = false
                } else {
                    Toast.makeText(this@ShowPasswordActivity, "Пароль не найден", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ShowPasswordActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        setResult(RESULT_OK)
        finish()
        return true
    }
}