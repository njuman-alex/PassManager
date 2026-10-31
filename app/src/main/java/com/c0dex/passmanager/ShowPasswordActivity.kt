package com.c0dex.passmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.c0dex.passmanager.data.PasswordDatabase
import com.c0dex.passmanager.ui.theme.PassManagerTheme
import kotlinx.coroutines.launch

class ShowPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val passwordId = intent.getLongExtra("PASSWORD_ID", -1)
        val encryptedPassword = intent.getStringExtra("ENCRYPTED_PASSWORD") ?: ""

        if (passwordId == -1L || encryptedPassword.isEmpty()) {
            Toast.makeText(this, "Ошибка: пароль не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            PassManagerTheme {
                ShowPasswordScreen(
                    passwordId = passwordId,
                    encryptedPassword = encryptedPassword,
                    onBack = {
                        setResult(RESULT_OK)
                        finish()
                    },
                    onDeleteSuccess = {
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
fun ShowPasswordScreen(
    passwordId: Long,
    encryptedPassword: String,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var displayedPassword by remember { mutableStateOf("Расшифруйте пароль") }
    var masterPasswordInput by remember { mutableStateOf("") }

    var isDecrypted by remember { mutableStateOf(false) }
    var isEditingDescription by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Загрузка описания при старте
    LaunchedEffect(passwordId) {
        try {
            val db = PasswordDatabase.getDatabase(context)
            val item = db.passwordDao().getPasswordById(passwordId)
            if (item != null) {
                description = item.description
            } else {
                showToast = "Пароль не найден"
                onBack()
            }
        } catch (e: Exception) {
            showToast = "Ошибка загрузки"
            onBack()
        }
    }

    // Показ Toast
    showToast?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            showToast = null
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить пароль?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isLoading = true
                        val activity = context as? ComponentActivity
                        activity?.lifecycleScope?.launch {
                            try {
                                val db = PasswordDatabase.getDatabase(context)
                                db.passwordDao().deletePassword(passwordId)
                                showToast = "Пароль удалён!"
                                onDeleteSuccess()
                            } catch (e: Exception) {
                                showToast = "Ошибка удаления"
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Удаление..." else "Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isLoading) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просмотр пароля") },
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
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Верхняя часть с полями
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // 1. Описание
                        Text(
                            text = "Описание пароля:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TextField(
                            value = description,
                            onValueChange = { if (isEditingDescription) description = it },
                            enabled = isEditingDescription,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                disabledIndicatorColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // 2. Пароль
                        Text(
                            text = "Пароль:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SelectionContainer {
                            Text(
                                text = displayedPassword,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .padding(bottom = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. Мастер-пароль
                        Text(
                            text = "Мастер-пароль:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = masterPasswordInput,
                            onValueChange = { masterPasswordInput = it },
                            label = { Text("Введите мастер-пароль для расшифровки") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            enabled = !isDecrypted,
                            singleLine = true
                        )

                        // 4. Кнопки действий
                        Button(
                            onClick = {
                                if (masterPasswordInput.isNotEmpty()) {
                                    val decrypted = EncryptionHelper.decrypt(encryptedPassword, masterPasswordInput)
                                    if (decrypted != null) {
                                        displayedPassword = decrypted
                                        isDecrypted = true
                                        masterPasswordInput = "" // Очищаем сразу
                                        showToast = "Пароль расшифрован!"
                                    } else {
                                        showToast = "Неверный мастер-пароль"
                                    }
                                } else {
                                    showToast = "Введите мастер-пароль"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            enabled = !isDecrypted && !isLoading
                        ) {
                            Text("Расшифровать")
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("password", displayedPassword))
                                showToast = "Пароль скопирован"
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            enabled = isDecrypted && !isLoading
                        ) {
                            Text("Скопировать в буфер")
                        }

                        Button(
                            onClick = {
                                if (!isEditingDescription) {
                                    isEditingDescription = true
                                } else {
                                    if (description.isNotBlank()) {
                                        isLoading = true
                                        val activity = context as? ComponentActivity
                                        activity?.lifecycleScope?.launch {
                                            try {
                                                val db = PasswordDatabase.getDatabase(context)
                                                db.passwordDao().updateDescription(passwordId, description.trim())
                                                isEditingDescription = false
                                                showToast = "Описание обновлено!"
                                            } catch (e: Exception) {
                                                showToast = "Ошибка обновления"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        showToast = "Описание не может быть пустым"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            enabled = isDecrypted && !isLoading
                        ) {
                            Text(if (isEditingDescription) "Сохранить описание" else "Изменить описание")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Кнопка удаления (снизу, красная)
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isDecrypted && !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = if (isDecrypted) 1f else 0.38f)
                        )
                    ) {
                        Text(if (isLoading) "Удаление..." else "Удалить пароль")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShowPasswordScreenPreview() {
    PassManagerTheme {
        ShowPasswordScreen(
            passwordId = 1L,
            encryptedPassword = "encrypted_data_here",
            onBack = {},
            onDeleteSuccess = {}
        )
    }
}