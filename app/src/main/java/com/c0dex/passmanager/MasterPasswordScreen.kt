package com.c0dex.passmanager

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MasterPasswordScreen(
    onBackClick: () -> Unit,
    onMasterPasswordSet: () -> Unit,
    isMasterPasswordSet: Boolean
) {
    SetMasterPasswordScreen(
        onBackClick = onBackClick,
        onMasterPasswordSet = onMasterPasswordSet,
        isMasterPasswordSet = isMasterPasswordSet
    )
}

@Composable
fun SetMasterPasswordScreen(
    onBackClick: () -> Unit,
    onMasterPasswordSet: () -> Unit,
    isMasterPasswordSet: Boolean
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(text = "Мастер-пароль") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Установите мастер-пароль",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Подтвердите пароль") },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Кнопка "Установить"
            Button(
                onClick = {
                    if (password == confirmPassword && password.length >= 6) {
                        onMasterPasswordSet()
                        onBackClick()
                    }
                },
                enabled = !isMasterPasswordSet && password == confirmPassword && password.length >= 6, // <-- Неактивна, если установлен
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Установить")
            }

            // Показываем текст, если мастер-пароль уже установлен
            if (isMasterPasswordSet) {
                Text(
                    text = "Мастер-пароль установлен",
                    color = MaterialTheme.colorScheme.primary, // Синий цвет (или другой)
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}