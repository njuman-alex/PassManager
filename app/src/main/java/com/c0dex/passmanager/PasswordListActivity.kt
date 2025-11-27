package com.c0dex.passmanager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.c0dex.passmanager.data.PasswordDatabase
import kotlinx.coroutines.launch

class PasswordListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PasswordListAdapter
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_list)

        initViews()
        setupToolbar()
        setupRecyclerView()
        loadPasswords()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupRecyclerView() {
        adapter = PasswordListAdapter { item ->
            // Переход в ShowPasswordActivity
            val intent = Intent(this, ShowPasswordActivity::class.java)
            intent.putExtra("PASSWORD_ID", item.id) // <-- Передаём id
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadPasswords() {
        lifecycleScope.launch {
            try {
                val db = PasswordDatabase.getDatabase(this@PasswordListActivity)
                val passwords = db.passwordDao().getAllPasswords()
                adapter.updatePasswords(passwords)
            } catch (e: Exception) {
                Toast.makeText(this@PasswordListActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}