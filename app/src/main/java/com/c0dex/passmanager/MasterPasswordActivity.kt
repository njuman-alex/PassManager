package com.c0dex.passmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0dex.passmanager.ui.theme.PassManagerTheme

class MasterPasswordActivity : ComponentActivity() {

    private val PREFS_NAME = "password_prefs"
    private val MASTER_PASSWORD_SET_KEY = "master_password_set"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем статус при запуске
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isMasterSet = prefs.getBoolean(MASTER_PASSWORD_SET_KEY, false)

        setContent {
            PassManagerTheme {
                MasterPasswordScreen(
                    isMasterSet = isMasterSet,
                    onBack = { finish() },
                    onSetPassword = { password ->
                        SecurePrefs.setMaster(this, password)
                        prefs.edit().putBoolean(MASTER_PASSWORD_SET_KEY, true).apply()
                        Toast.makeText(this, "Мастер-пароль установлен!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    },
                    onResetPassword = { enteredPassword ->
                        val currentMaster = SecurePrefs.getMaster(this) ?: ""
                        if (enteredPassword == currentMaster) {
                            SecurePrefs.clearMaster(this)
                            prefs.edit().remove(MASTER_PASSWORD_SET_KEY).apply()
                            Toast.makeText(this, "Мастер-пароль сброшен!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this, "Неверный мастер-пароль", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterPasswordScreen(
    isMasterSet: Boolean,
    onBack: () -> Unit,
    onSetPassword: (String) -> Unit,
    onResetPassword: (String) -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Состояния для диалогов
    var showConfirmResetDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var verifyInput by remember { mutableStateOf("") }

    // Показ ошибок через Toast
    errorMessage?.let { msg ->
        androidx.compose.runtime.LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            errorMessage = null
        }
    }

    // Диалог подтверждения сброса
    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text("Сброс мастер-пароля") },
            text = {
                Text("Вы действительно хотите сбросить мастер-пароль?\n" +
                        "Функции сохранения и экспорта будут недоступны!\n" +
                        "Для просмотра сохранённых паролей вам всё равно придётся ввести прежний мастер-пароль!")
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmResetDialog = false
                    showVerifyDialog = true
                }) {
                    Text("Да", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDialog = false }) {
                    Text("Нет")
                }
            }
        )
    }

    // Диалог ввода старого пароля для подтверждения сброса
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Введите мастер-пароль") },
            text = {
                OutlinedTextField(
                    value = verifyInput,
                    onValueChange = { verifyInput = it },
                    label = { Text("Мастер-пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onResetPassword(verifyInput)
                    showVerifyDialog = false
                }) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мастер-пароль") },
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
            // Карточка (аналог MaterialCardView)
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxSize(),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Верхняя часть с полями ввода (занимает всё доступное место, толкая кнопку сброса вниз)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isMasterSet,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Подтвердите пароль") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isMasterSet,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (password != confirmPassword) {
                                    errorMessage = "Пароли не совпадают"
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Пароль должен быть не менее 6 символов"
                                    return@Button
                                }
                                onSetPassword(password)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isMasterSet
                        ) {
                            Text("Установить")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Статус
                        Text(
                            text = if (isMasterSet) "Мастер-пароль установлен" else "Мастер-пароль еще не установлен",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMasterSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопка сброса (Outlined, красная)
                    OutlinedButton(
                        onClick = { showConfirmResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isMasterSet,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = if (isMasterSet) 1f else 0.38f)
                        )
                    ) {
                        Text("Сбросить мастер-пароль")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MasterPasswordScreenPreview() {
    PassManagerTheme {
        // Превью для состояния "НЕ установлен"
        MasterPasswordScreen(
            isMasterSet = false,
            onBack = {},
            onSetPassword = {},
            onResetPassword = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MasterPasswordScreenSetPreview() {
    PassManagerTheme {
        // Превью для состояния "Установлен"
        MasterPasswordScreen(
            isMasterSet = true,
            onBack = {},
            onSetPassword = {},
            onResetPassword = {}
        )
    }
}