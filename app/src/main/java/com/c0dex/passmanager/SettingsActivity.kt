package com.c0dex.passmanager

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.c0dex.passmanager.data.PasswordDatabase
import kotlinx.coroutines.launch

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
            Toast.makeText(this, "Экспорт базы паролей", Toast.LENGTH_SHORT).show()
            // TODO: Реализовать экспорт
        }

        importButton.setOnClickListener {
            Toast.makeText(this, "Импорт базы паролей", Toast.LENGTH_SHORT).show()
            // TODO: Реализовать импорт
        }

        deleteAllButton.setOnClickListener {
            showDeleteAllAlert()
        }
    }

    private fun showDeleteAllAlert() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_all, null)
        val checkBox = dialogView.findViewById<CheckBox>(R.id.confirmCheckBox)

        AlertDialog.Builder(this)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}