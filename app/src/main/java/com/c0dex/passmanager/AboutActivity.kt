package com.c0dex.passmanager

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar


class AboutActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var versionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        initViews()
        setupToolbar()
        setVersion()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        versionText = findViewById(R.id.versionText)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setVersion() {
        val versionName = BuildConfig.VERSION_NAME
        versionText.text = "ver. $versionName"
    }


    override fun onSupportNavigateUp(): Boolean {
        setResult(RESULT_OK)
        finish()
        return true
    }
}