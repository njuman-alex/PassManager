package com.c0dex.passmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var generatedPasswordText: TextView
    private lateinit var useLettersCheckBox: CheckBox
    private lateinit var useDigitsAndSpecialCheckBox: CheckBox
    private lateinit var passwordLengthEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var saveButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var masterPasswordStatusText: TextView

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
    }

    private fun initViews() {
        generatedPasswordText = findViewById(R.id.generatedPasswordText)
        useLettersCheckBox = findViewById(R.id.useLettersCheckBox)
        useDigitsAndSpecialCheckBox = findViewById(R.id.useDigitsAndSpecialCheckBox)
        passwordLengthEditText = findViewById(R.id.passwordLengthEditText)
        generateButton = findViewById(R.id.generateButton)
        saveButton = findViewById(R.id.saveButton)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        masterPasswordStatusText = findViewById(R.id.masterPasswordStatusText)
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
                    //startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupClickListeners() {
        generateButton.setOnClickListener {
            generatePassword()
        }

        saveButton.setOnClickListener {
            val intent = Intent(this, SavePasswordActivity::class.java)
            intent.putExtra("GENERATED_PASSWORD", generatedPasswordText.text.toString())
            savePasswordLauncher.launch(intent) // Используем launcher
        }
    }

    private fun checkMasterPasswordStatus() {
        val prefs = getSharedPreferences("password_prefs", MODE_PRIVATE)
        val hasMasterPassword = prefs.contains("master_password_hash")

        if (!hasMasterPassword) {
            // Мастер-пароль не установлен
            masterPasswordStatusText.visibility = View.VISIBLE
            saveButton.isEnabled = false
        } else {
            // Мастер-пароль установлен
            masterPasswordStatusText.visibility = View.GONE
            // Проверяем, есть ли сгенерированный пароль
            saveButton.isEnabled = generatedPasswordText.text.toString().isNotEmpty()
        }
    }

    private fun generatePassword() {
        val length = passwordLengthEditText.text.toString().toIntOrNull() ?: 12
        val useLetters = useLettersCheckBox.isChecked
        val useDigitsAndSpecial = useDigitsAndSpecialCheckBox.isChecked

        val password = generatePasswordString(length, useLetters, useDigitsAndSpecial)
        generatedPasswordText.text = password

        // Обновляем статус кнопки
        checkMasterPasswordStatus()
    }

    private fun generatePasswordString(
        length: Int,
        useLetters: Boolean,
        useDigitsAndSpecial: Boolean
    ): String {
        if (length <= 0) return ""

        var chars = "abcdefghijkmnpqrstuvwxyz"
        if (useLetters) {
            chars += "ABCDEFGHJKMNPQRSTUVWXYZ"
        }
        if (useDigitsAndSpecial) {
            chars += "23456789"
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