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
fun SavePasswordScreen(
    generatedPassword: String,
    onBackClick: () -> Unit,
    onSavePassword: (String, String) -> Unit // description, encryptedPassword
) {
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(text = "Сохранить пароль") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Сгенерированный пароль:",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = generatedPassword,
                onValueChange = {}, // Не изменяем
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание пароля") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (description.isNotEmpty()) {
                        onSavePassword(description, generatedPassword) // Пока без шифрования
                        onBackClick()
                    }
                },
                enabled = description.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Подтвердить и сохранить")
            }
        }
    }
}