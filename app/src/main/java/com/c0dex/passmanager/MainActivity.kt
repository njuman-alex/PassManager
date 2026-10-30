package com.c0dex.passmanager

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {
    private lateinit var generatedPasswordText: TextView
    private lateinit var useLettersCheckBox: CheckBox
    private lateinit var useDigitsCheckBox: CheckBox
    private lateinit var useSpecialCheckBox: CheckBox
    private lateinit var passwordLengthEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var copyButton: Button
    private lateinit var saveButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var masterPasswordStatusText: TextView
    private lateinit var versionText: TextView
    private lateinit var editPasswordButton: MaterialButton
    private lateinit var editablePasswordText: EditText

    // 👇 ДОБАВЛЕНО: храним текущее состояние пароля и режима редактирования
    private var currentPassword: String = ""
    private var isEditMode: Boolean = false

    private val masterPasswordLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            checkMasterPasswordStatus()
        }
    }

    private val savePasswordLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            generatedPasswordText.text = ""
            currentPassword = "" // 👈 сброс при сохранении
            checkMasterPasswordStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupToolbar()
        setupDrawer()
        setupClickListeners()
        setVersion()

        // 👇 ВОССТАНАВЛИВАЕМ состояние после поворота
        if (savedInstanceState != null) {
            currentPassword = savedInstanceState.getString("current_password", "")
            isEditMode = savedInstanceState.getBoolean("is_edit_mode", false)

            if (isEditMode) {
                editablePasswordText.setText(currentPassword)
                editablePasswordText.visibility = View.VISIBLE
                generatedPasswordText.visibility = View.GONE
                editPasswordButton.text = "✓"
            } else {
                generatedPasswordText.text = currentPassword
                generatedPasswordText.visibility = if (currentPassword.isNotEmpty()) View.VISIBLE else View.GONE
                editablePasswordText.visibility = View.GONE
                editPasswordButton.text = "✎"
            }
        }

        checkMasterPasswordStatus()
        runMigrationsIfNeeded()
    }

    // 👇 ДОБАВЛЕНО: сохраняем состояние перед уничтожением
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Сохраняем текущий пароль (из нужного поля)
        currentPassword = if (isEditMode) {
            editablePasswordText.text.toString()
        } else {
            generatedPasswordText.text.toString()
        }
        outState.putString("current_password", currentPassword)
        outState.putBoolean("is_edit_mode", isEditMode)
    }

    private fun runMigrationsIfNeeded() {
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Мастер: plaintext -> EncryptedSharedPreferences (каждый запуск, идемпотентно)
            PasswordMigrator.migrateMasterPasswordIfNeeded(this@MainActivity)

            // 2. База: v1 -> v2 (одноразово, по флагу)
            val prefs = getSharedPreferences("password_prefs", MODE_PRIVATE)
            val hasMaster = prefs.getBoolean("master_password_set", false)
            val alreadyMigrated = prefs.getBoolean("db_migrated_v2", false)
            if (hasMaster && !alreadyMigrated) {
                val master = SecurePrefs.getMaster(this@MainActivity) ?: return@launch
                val backupFile = PasswordMigrator.backupDatabase(this@MainActivity)
                val count = PasswordMigrator.migrateDatabase(this@MainActivity, master)
                prefs.edit().putBoolean("db_migrated_v2", true).apply()
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "База переведена на новый шифр: записей обновлено: $count\nБэкап: ${backupFile.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun initViews() {
        generatedPasswordText = findViewById(R.id.generatedPasswordText)
        useLettersCheckBox = findViewById(R.id.useLettersCheckBox)
        useDigitsCheckBox = findViewById(R.id.useDigitsCheckBox)
        useSpecialCheckBox = findViewById(R.id.useSpecialCheckBox)
        passwordLengthEditText = findViewById(R.id.passwordLengthEditText)
        generateButton = findViewById(R.id.generateButton)
        copyButton = findViewById(R.id.copyButton)
        saveButton = findViewById(R.id.saveButton)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        masterPasswordStatusText = findViewById(R.id.masterPasswordStatusText)
        versionText = findViewById(R.id.versionText)
        editPasswordButton = findViewById(R.id.editPasswordButton)
        editablePasswordText = findViewById(R.id.editablePasswordText)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

    private fun setupDrawer() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_password_list -> {
                    startActivity(Intent(this, PasswordListActivity::class.java))
                }
                R.id.nav_master_password -> {
                    masterPasswordLauncher.launch(Intent(this, MasterPasswordActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setVersion() {
        val versionName = BuildConfig.VERSION_NAME
        versionText.text = "ver. $versionName"
    }

    private fun setupClickListeners() {
        generateButton.setOnClickListener {
            generatePassword()
        }

        copyButton.setOnClickListener {
            try {
                val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("password", generatedPasswordText.text.toString())
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this, "Пароль скопирован", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка копирования", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        editPasswordButton.setOnClickListener {
            togglePasswordEdit()
        }

        saveButton.setOnClickListener {
            val password = generatedPasswordText.text.toString()
            if (password.isNotEmpty()) {
                val intent = Intent(this, SavePasswordActivity::class.java)
                intent.putExtra("GENERATED_PASSWORD", password)

                val prefs = getSharedPreferences("password_prefs", MODE_PRIVATE)
                val hasMasterPassword = prefs.getBoolean("master_password_set", false)

                if (hasMasterPassword) {
                    savePasswordLauncher.launch(intent)
                } else {
                    Toast.makeText(this, "Мастер-пароль не установлен", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Сначала сгенерируйте пароль", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkMasterPasswordStatus() {
        val prefs = getSharedPreferences("password_prefs", MODE_PRIVATE)
        val hasMasterPassword = prefs.getBoolean("master_password_set", false)

        val isPasswordAvailable = generatedPasswordText.text.isNotEmpty() ||
                (editablePasswordText.isVisible && editablePasswordText.text.isNotEmpty())

        if (!hasMasterPassword) {
            masterPasswordStatusText.visibility = View.VISIBLE
            saveButton.isEnabled = false
        } else {
            masterPasswordStatusText.visibility = View.GONE
            saveButton.isEnabled = isPasswordAvailable
        }

        copyButton.isEnabled = isPasswordAvailable
        editPasswordButton.isEnabled = generatedPasswordText.text.isNotEmpty()
    }

    private fun generatePassword() {
        val length = passwordLengthEditText.text.toString().toIntOrNull() ?: 8
        if (length !in 6..16) {
            Toast.makeText(this, "Некорректная длина пароля (6 - 16 символов)", Toast.LENGTH_SHORT).show()
            return
        }

        val password = generatePasswordString(length, useLettersCheckBox.isChecked, useDigitsCheckBox.isChecked, useSpecialCheckBox.isChecked)
        generatedPasswordText.text = password
        currentPassword = password // 👈 обновляем текущий пароль

        // Выходим из режима редактирования
        if (editablePasswordText.isVisible) {
            editablePasswordText.visibility = View.GONE
            generatedPasswordText.visibility = View.VISIBLE
            editPasswordButton.text = "✎"
            isEditMode = false // 👈 сбрасываем флаг
        }

        // Анимация появления
        generatedPasswordText.alpha = 0f
        generatedPasswordText.visibility = View.VISIBLE
        generatedPasswordText.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        checkMasterPasswordStatus()
    }

    private fun generatePasswordString(
        length: Int,
        useLetters: Boolean,
        useDigits: Boolean,
        useSpecial: Boolean
    ): String {
        if (length <= 0) return ""

        val chars = buildString {
            append("abcdefghijkmnpqrstuvwxyz")
            if (useLetters) append("ABCDEFGHJKMNPQRSTUVWXYZ")
            if (useDigits) append("23456789")
            if (useSpecial) append("!@#$%^&*()_+-=[]{}|;:,.<>?")
        }

        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun togglePasswordEdit() {
        isEditMode = !isEditMode // 👈 переключаем флаг

        if (isEditMode) {
            val currentText = generatedPasswordText.text.toString()
            editablePasswordText.setText(currentText)
            editablePasswordText.visibility = View.VISIBLE
            generatedPasswordText.visibility = View.GONE
            editPasswordButton.text = "✓"
        } else {
            val editedText = editablePasswordText.text.toString()
            generatedPasswordText.text = editedText
            currentPassword = editedText // 👈 сохраняем отредактированный пароль
            editablePasswordText.visibility = View.GONE
            generatedPasswordText.visibility = View.VISIBLE
            editPasswordButton.text = "✎"
            checkMasterPasswordStatus()
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(navigationView)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}