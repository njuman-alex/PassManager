package com.c0dex.passmanager

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.c0dex.passmanager.data.PasswordDatabase
import com.c0dex.passmanager.data.PasswordItem
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var deleteAllButton: Button
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        setupToolbar()
        setupClickListeners()
    }

    private fun initViews() {
        exportButton = findViewById(R.id.exportButton)
        importButton = findViewById(R.id.importButton)
        deleteAllButton = findViewById(R.id.deleteAllButton)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupClickListeners() {
        exportButton.setOnClickListener {
            //Toast.makeText(this, "Экспорт базы паролей", Toast.LENGTH_SHORT).show()
            // TODO: Реализовать экспорт
            exportPasswords()
        }

        importButton.setOnClickListener {
            //Toast.makeText(this, "Импорт базы паролей", Toast.LENGTH_SHORT).show()
            // TODO: Реализовать импорт
            importPasswords()
        }

        deleteAllButton.setOnClickListener {
            showDeleteAllAlert()
        }
    }

    private fun showDeleteAllAlert() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_all, null)
        val checkBox = dialogView.findViewById<CheckBox>(R.id.confirmCheckBox)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Удалить все пароли")
            .setMessage("Вы действительно хотите удалить все сохранённые пароли? Это действие нельзя отменить.")
            .setView(dialogView)
            .setPositiveButton("Удалить") { _, _ ->
                if (checkBox.isChecked) {
                    deleteAllPasswords()
                } else {
                    Toast.makeText(this, "Подтвердите удаление", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteAllPasswords() {
        lifecycleScope.launch {
            try {
                val db = PasswordDatabase.getDatabase(this@SettingsActivity)
                db.passwordDao().deleteAllPasswords()
                Toast.makeText(this@SettingsActivity, "Все пароли удалены", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun exportPasswords() {
        lifecycleScope.launch {
            try {
                val db = PasswordDatabase.getDatabase(this@SettingsActivity)
                val passwords = db.passwordDao().getAllPasswords()

                if (passwords.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "Нет паролей для экспорта", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val json = passwords.toJson()

                val prefs = getSharedPreferences("password_prefs", MODE_PRIVATE)
                val masterPassword = prefs.getString("master_password", "") ?: ""

                if (masterPassword.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "Мастер-пароль не установлен", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val encryptedJson = EncryptionHelper.encrypt(json, masterPassword)

                val sdf = SimpleDateFormat("dd.MM.yyyy_HH:mm", Locale.getDefault())
                val fileName = "PassManager_passwords_${sdf.format(Date())}.json"

                // Сохраняем в Downloads
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri).use { outputStream ->
                        outputStream?.write(encryptedJson.toByteArray())
                    }
                    Toast.makeText(this@SettingsActivity, "Пароли экспортированы", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Ошибка экспорта", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Ошибка экспорта", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun importPasswords() {
        //val dialogView = layoutInflater.inflate(R.layout.dialog_delete_all, null)
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        AlertDialog.Builder(this)
            .setTitle("Импортировать?")
            .setMessage("Все текущие пароли будут заменены!")
            //.setView(dialogView)
            .setPositiveButton("Импорт") { _, _ ->
                importFileLauncher.launch(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()

        //importFileLauncher.launch(intent)
    }

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                // Сначала запросим мастер-пароль
                requestMasterPasswordForImport(uri)
            }
        }
    }

    private fun requestMasterPasswordForImport(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_master_password, null)
        val input = dialogView.findViewById<EditText>(R.id.masterPasswordEditText)

        AlertDialog.Builder(this)
            .setTitle("Введите мастер-пароль")
            .setView(dialogView)
            .setPositiveButton("ОК") { _, _ ->
                val masterPassword = input.text.toString()
                importPasswordsFromFile(uri, masterPassword)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun importPasswordsFromFile(uri: Uri, masterPassword: String) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val encryptedJson = inputStream?.bufferedReader().use { it?.readText() }

                if (encryptedJson.isNullOrEmpty()) {
                    Toast.makeText(this@SettingsActivity, "Файл пуст", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val json = EncryptionHelper.decrypt(encryptedJson, masterPassword)
                if (json == null) {
                    Toast.makeText(this@SettingsActivity, "Неверный мастер-пароль", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val passwords = json.fromJson()

                val db = PasswordDatabase.getDatabase(this@SettingsActivity)
                db.passwordDao().deleteAllPasswords() // <-- Удаляем старые
                db.passwordDao().insertPasswords(passwords) // <-- Вставляем новые

                Toast.makeText(this@SettingsActivity, "Пароли импортированы", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Ошибка импорта", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun String.fromJson(): List<PasswordItem> {
        val gson = Gson()
        return try {
            gson.fromJson(this, Array<PasswordItem>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun List<PasswordItem>.toJson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}