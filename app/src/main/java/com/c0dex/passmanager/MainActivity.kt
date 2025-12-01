package com.c0dex.passmanager

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
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var generatedPasswordText: TextView
    private lateinit var useLettersCheckBox: CheckBox
    private lateinit var useDigitsCheckBox: CheckBox
    private lateinit var useSpecialCheckBox: CheckBox
    private lateinit var passwordLengthEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var saveButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var masterPasswordStatusText: TextView
    private lateinit var versionText: TextView

    private val masterPasswordLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            checkMasterPasswordStatus() // Обновляем статус
        }
    }

    private val savePasswordLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Очищаем поле пароля
            generatedPasswordText.text = ""
            // Обновляем статус
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
        checkMasterPasswordStatus() // Проверяем при запуске
        setVersion()
    }

    private fun initViews() {
        generatedPasswordText = findViewById(R.id.generatedPasswordText)
        useLettersCheckBox = findViewById(R.id.useLettersCheckBox)
        useDigitsCheckBox = findViewById(R.id.useDigitsCheckBox)
        useSpecialCheckBox = findViewById(R.id.useSpecialCheckBox)
        passwordLengthEditText = findViewById(R.id.passwordLengthEditText)
        generateButton = findViewById(R.id.generateButton)
        saveButton = findViewById(R.id.saveButton)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        masterPasswordStatusText = findViewById(R.id.masterPasswordStatusText)
        versionText = findViewById(R.id.versionText)
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
                    // Используем launcher
                    masterPasswordLauncher.launch(Intent(this, MasterPasswordActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
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

        // В setupClickListeners(), в onClick saveButton:

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

        if (!hasMasterPassword) {
            masterPasswordStatusText.visibility = View.VISIBLE
            saveButton.isEnabled = false
        } else {
            masterPasswordStatusText.visibility = View.GONE
            saveButton.isEnabled = generatedPasswordText.text.toString().isNotEmpty()
        }
    }

    private fun generatePassword() {
        val length = passwordLengthEditText.text.toString().toIntOrNull() ?: 8
        val useLetters = useLettersCheckBox.isChecked
        val useDigits = useDigitsCheckBox.isChecked
        val useSpecial = useSpecialCheckBox.isChecked

        val password = generatePasswordString(length, useLetters, useDigits, useSpecial)
        generatedPasswordText.text = password

        // Обновляем статус кнопки
        checkMasterPasswordStatus()
    }

    private fun generatePasswordString(
        length: Int,
        useLetters: Boolean,
        useDigits: Boolean,
        useSpecial: Boolean
    ): String {
        if (length <= 0) return ""

        var chars = "abcdefghijkmnpqrstuvwxyz"
        if (useLetters) {
            chars += "ABCDEFGHJKMNPQRSTUVWXYZ"
        }
        if (useDigits) {
            chars += "23456789"
        }
        if (useSpecial) {
            chars += "!@#$%^&*()_+-=[]{}|;:,.<>?"
        }

        if (chars.isEmpty()) return ""

        return buildString {
            repeat(length) {
                append(chars.random())
            }
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