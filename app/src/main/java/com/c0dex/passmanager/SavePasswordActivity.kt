package com.c0dex.passmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.c0dex.passmanager.data.PasswordDatabase
import com.c0dex.passmanager.data.PasswordItem
import com.c0dex.passmanager.ui.theme.PassManagerTheme
import kotlinx.coroutines.launch

class SavePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val generatedPassword = intent.getStringExtra("GENERATED_PASSWORD") ?: ""

        setContent {
            PassManagerTheme {
                SavePasswordScreen(
                    generatedPassword = generatedPassword,
                    onBack = { finish() },
                    onSaveSuccess = {
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePasswordScreen(
    generatedPassword: String,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Показываем ошибки через Toast
    errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сохранить пароль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Поле сгенерированного пароля (только для чтения)
            OutlinedTextField(
                value = generatedPassword,
                onValueChange = {},
                label = { Text("Сгенерированный пароль") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Поле описания
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка сохранения
            Button(
                onClick = {
                    if (description.isBlank()) {
                        errorMessage = "Введите описание"
                        return@Button
                    }
                    if (generatedPassword.isBlank()) {
                        errorMessage = "Пароль не сгенерирован"
                        return@Button
                    }

                    isSaving = true
                    val activity = context as? ComponentActivity
                    activity?.lifecycleScope?.launch {
                        try {
                            val db = PasswordDatabase.getDatabase(context)
                            val count = db.passwordDao().getDescriptionCount(description.trim())

                            if (count > 0) {
                                errorMessage = "Пароль с таким описанием уже существует!"
                                isSaving = false
                                return@launch
                            }

                            val masterPassword = SecurePrefs.getMaster(context) ?: ""
                            if (masterPassword.isEmpty()) {
                                errorMessage = "Мастер-пароль не установлен!"
                                isSaving = false
                                return@launch
                            }

                            val encryptedPassword = EncryptionHelper.encrypt(generatedPassword, masterPassword)
                            val passwordItem = PasswordItem(
                                description = description.trim(),
                                encryptedPassword = encryptedPassword
                            )
                            db.passwordDao().insertPassword(passwordItem)

                            onSaveSuccess()
                        } catch (e: Exception) {
                            errorMessage = "Ошибка сохранения"
                            isSaving = false
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Сохранение..." else "Сохранить")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SavePasswordScreenPreview() {
    PassManagerTheme {
        SavePasswordScreen(
            generatedPassword = "Kj8#mP2\$vL9@nQ4!",
            onBack = {},
            onSaveSuccess = {}
        )
    }
}