package com.c0dex.passmanager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SavePasswordActivity : AppCompatActivity() {
    private lateinit var generatedPasswordText: TextView
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_password)

        initViews()
        setupClickListeners()

        // Получаем сгенерированный пароль из Intent
        val generatedPassword = intent.getStringExtra("GENERATED_PASSWORD")
        generatedPasswordText.text = generatedPassword
    }

    private fun initViews() {
        generatedPasswordText = findViewById(R.id.generatedPasswordText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            val description = descriptionEditText.text.toString()
            val password = generatedPasswordText.text.toString()

            if (description.isNotEmpty()) {
                // TODO: сохранить в базу
                Toast.makeText(this, "Пароль сохранён!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish() // Закрываем Activity
            } else {
                Toast.makeText(this, "Введите описание", Toast.LENGTH_SHORT).show()
            }
        }
    }
}