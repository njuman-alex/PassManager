package com.c0dex.passmanager

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.c0dex.passmanager.data.PasswordDatabase
import com.c0dex.passmanager.data.PasswordItem
import com.c0dex.passmanager.ui.theme.PassManagerTheme
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : ComponentActivity() {

    // Uri выбранного файла импорта — наблюдает Compose
    var pendingImportUri by mutableStateOf<Uri?>(null)

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) performExportToUri(uri) }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImportUri = uri }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PassManagerTheme {
                SettingsScreen(
                    onBack = { finish() },
                    onExportClick = { startExport() },
                    onImportClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun startExport() {
        lifecycleScope.launch {
            val db = PasswordDatabase.getDatabase(this@SettingsActivity)
            val passwords = db.passwordDao().getAllPasswords()
            if (passwords.isEmpty()) {
                toast("Нет паролей для экспорта")
                return@launch
            }
            val master = SecurePrefs.getMaster(this@SettingsActivity) ?: ""
            if (master.isEmpty()) {
                toast("Мастер-пароль не установлен")
                return@launch
            }
            val sdf = SimpleDateFormat("dd.MM.yyyy_HH:mm", Locale.getDefault())
            exportLauncher.launch("PassManager_passwords_${sdf.format(Date())}.json")
        }
    }

    fun performExportToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val db = PasswordDatabase.getDatabase(this@SettingsActivity)
                val passwords = db.passwordDao().getAllPasswords()
                val master = SecurePrefs.getMaster(this@SettingsActivity) ?: return@launch
                val json = Gson().toJson(passwords)
                val encrypted = EncryptionHelper.encrypt(json, master)
                contentResolver.openOutputStream(uri)?.use { it.write(encrypted.toByteArray()) }
                toast("Пароли экспортированы")
            } catch (e: Exception) {
                toast("Ошибка экспорта: ${e.message}")
            }
        }
    }

    fun performImport(uri: Uri, masterPassword: String) {
        lifecycleScope.launch {
            try {
                val encryptedJson = contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                if (encryptedJson.isNullOrEmpty()) {
                    toast("Файл пуст")
                    return@launch
                }
                val json = EncryptionHelper.decrypt(encryptedJson, masterPassword)
                if (json == null) {
                    toast("Неверный мастер-пароль")
                    return@launch
                }
                val items = Gson().fromJson(json, Array<PasswordItem>::class.java).toList()
                val db = PasswordDatabase.getDatabase(this@SettingsActivity)
                db.passwordDao().deleteAllPasswords()
                db.passwordDao().insertPasswords(items)
                toast("Пароли импортированы")
            } catch (e: Exception) {
                toast("Ошибка импорта: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? SettingsActivity
    val scope = rememberCoroutineScope()

    var toastMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var checkboxChecked by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showMasterInput by remember { mutableStateOf(false) }
    var masterInput by remember { mutableStateOf("") }

    // Реагируем на выбранный файл импорта
    val pendingUri = activity?.pendingImportUri
    LaunchedEffect(pendingUri) {
        if (pendingUri != null) showImportConfirm = true
    }

    // Показ Toast
    toastMsg?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            toastMsg = null
        }
    }

    // ===== Диалог подтверждения импорта =====
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                activity?.pendingImportUri = null
            },
            title = { Text("Импортировать?") },
            text = { Text("Все текущие пароли будут заменены!") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    showMasterInput = true
                }) { Text("Импорт") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    activity?.pendingImportUri = null
                }) { Text("Отмена") }
            }
        )
    }

    // ===== Диалог ввода мастера для импорта =====
    if (showMasterInput) {
        AlertDialog(
            onDismissRequest = {
                showMasterInput = false
                masterInput = ""
                activity?.pendingImportUri = null
            },
            title = { Text("Введите мастер-пароль") },
            text = {
                OutlinedTextField(
                    value = masterInput,
                    onValueChange = { masterInput = it },
                    label = { Text("Мастер-пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = activity?.pendingImportUri
                    if (uri != null) activity.performImport(uri, masterInput)
                    showMasterInput = false
                    masterInput = ""
                    activity?.pendingImportUri = null
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMasterInput = false
                    masterInput = ""
                    activity?.pendingImportUri = null
                }) { Text("Отмена") }
            }
        )
    }

    // ===== Диалог удаления всех паролей с чекбоксом =====
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                checkboxChecked = false
            },
            title = { Text("Удалить все пароли") },
            text = {
                Column {
                    Text("Вы действительно хотите удалить все сохранённые пароли? Это действие нельзя отменить.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checkboxChecked,
                            onCheckedChange = { checkboxChecked = it }
                        )
                        Text("Я подтверждаю удаление всех паролей")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val db = PasswordDatabase.getDatabase(context)
                                db.passwordDao().deleteAllPasswords()
                                toastMsg = "Все пароли удалены"
                            } catch (e: Exception) {
                                toastMsg = "Ошибка удаления"
                            } finally {
                                showDeleteDialog = false
                                checkboxChecked = false
                            }
                        }
                    },
                    enabled = checkboxChecked
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    checkboxChecked = false
                }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
                    // Верхние кнопки (weight толкает "удалить" вниз, импорт НЕ нужен!)
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = onExportClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Экспорт базы паролей")
                        }

                        Button(
                            onClick = onImportClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text("Импорт базы паролей")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Красная кнопка удаления
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val db = PasswordDatabase.getDatabase(context)
                                val passwords = db.passwordDao().getAllPasswords()
                                if (passwords.isEmpty()) {
                                    toastMsg = "Нечего удалять"
                                } else {
                                    showDeleteDialog = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить все пароли")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    PassManagerTheme {
        SettingsScreen(
            onBack = {},
            onExportClick = {},
            onImportClick = {}
        )
    }
}