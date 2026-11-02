package com.c0dex.passmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.c0dex.passmanager.ui.theme.PassManagerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val masterPasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Сигнал Compose обновить статус мастера
            triggerMasterCheck = true
        }
    }

    private val savePasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Сброс пароля после успешного сохранения
            generatedPassword = ""
            triggerMasterCheck = true
        }
    }

    // Триггеры для Compose
    var triggerMasterCheck by mutableStateOf(false)
    var generatedPassword by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PassManagerTheme {
                MainScreen(
                    activity = this,
                    generatedPassword = generatedPassword,
                    onGeneratedPasswordChange = { generatedPassword = it },
                    triggerMasterCheck = triggerMasterCheck,
                    onMasterCheckHandled = { triggerMasterCheck = false }
                )
            }
        }
    }

    fun launchMasterPasswordActivity() {
        masterPasswordLauncher.launch(Intent(this, MasterPasswordActivity::class.java))
    }

    fun launchSavePasswordActivity(password: String) {
        val intent = Intent(this, SavePasswordActivity::class.java).apply {
            putExtra("GENERATED_PASSWORD", password)
        }
        savePasswordLauncher.launch(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    activity: MainActivity,
    generatedPassword: String,
    onGeneratedPasswordChange: (String) -> Unit,
    triggerMasterCheck: Boolean,
    onMasterCheckHandled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Состояние с сохранением при повороте
    var useLetters by rememberSaveable { mutableStateOf(false) }
    var useDigits by rememberSaveable { mutableStateOf(false) }
    var useSpecial by rememberSaveable { mutableStateOf(false) }
    var passwordLength by rememberSaveable { mutableStateOf("8") }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var editedPassword by rememberSaveable { mutableStateOf("") }

    var hasMasterPassword by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf<String?>(null) }


    // Проверка статуса мастера
    LaunchedEffect(triggerMasterCheck) {
        val prefs = context.getSharedPreferences("password_prefs", Context.MODE_PRIVATE)
        hasMasterPassword = prefs.getBoolean("master_password_set", false)
        onMasterCheckHandled()
    }

    // Миграция при первом запуске
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            PasswordMigrator.migrateMasterPasswordIfNeeded(context)
            val prefs = context.getSharedPreferences("password_prefs", Context.MODE_PRIVATE)
            val hasMaster = prefs.getBoolean("master_password_set", false)
            val alreadyMigrated = prefs.getBoolean("db_migrated_v2", false)
            if (hasMaster && !alreadyMigrated) {
                val master = SecurePrefs.getMaster(context) ?: return@launch
                val backupFile = PasswordMigrator.backupDatabase(context)
                val count = PasswordMigrator.migrateDatabase(context, master)
                prefs.edit { putBoolean("db_migrated_v2", true) }
                withContext(Dispatchers.Main) {
                    showToast = "База переведена на новый шифр: записей обновлено: $count\nБэкап: ${backupFile.name}"
                }
            }
        }
        // Первичная проверка статуса
        val prefs = context.getSharedPreferences("password_prefs", Context.MODE_PRIVATE)
        hasMasterPassword = prefs.getBoolean("master_password_set", false)
    }

    // Показ Toast
    showToast?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            showToast = null
        }
    }

    // Функция генерации пароля (один в один из оригинала)
    fun generatePasswordString(length: Int, letters: Boolean, digits: Boolean, special: Boolean): String {
        if (length <= 0) return ""
        val chars = buildString {
            append("abcdefghijkmnpqrstuvwxyz")
            if (letters) append("ABCDEFGHJKMNPQRSTUVWXYZ")
            if (digits) append("23456789")
            if (special) append("!@#$%^&*()_+-=[]{}|;:,.<>?")
        }
        return (1..length).map { chars.random() }.joinToString("")
    }

    // Текущий пароль (из нужного поля)
    val currentPassword = if (isEditMode) editedPassword else generatedPassword

    // Вспомогательные переменные для UI
    val isPasswordAvailable = currentPassword.isNotEmpty()
    val copyEnabled = isPasswordAvailable
    val saveEnabled = hasMasterPassword && isPasswordAvailable
    val editEnabled = !isEditMode && generatedPassword.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "PassManager",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Список паролей") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            context.startActivity(Intent(context, PasswordListActivity::class.java))
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    label = { Text("Мастер-пароль") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            activity.launchMasterPasswordActivity()
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Управление базой паролей") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    label = { Text("Тема") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            //activity.launchThemeActivity()
                            context.startActivity(Intent(context, ThemeActivity::class.java))
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("О программе") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            context.startActivity(Intent(context, AboutActivity::class.java))
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Менеджер паролей") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
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
                // Карточка с генератором (вес 1, растягивается)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Заголовок
                        Text(
                            text = "Генератор паролей",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Поле пароля + кнопка редактирования
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditMode) {
                                OutlinedTextField(
                                    value = editedPassword,
                                    onValueChange = { editedPassword = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            } else {
                                Text(
                                    text = generatedPassword.ifEmpty { "Сгенерированный пароль" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(12.dp),
                                    fontSize = 16.sp,
                                    color = if (generatedPassword.isEmpty())
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (isEditMode) {
                                        // Сохраняем редактирование
                                        onGeneratedPasswordChange(editedPassword)
                                        isEditMode = false
                                    } else {
                                        // Включаем режим редактирования
                                        editedPassword = generatedPassword
                                        isEditMode = true
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                enabled = editEnabled || isEditMode
                            ) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
                                    contentDescription = if (isEditMode) "Подтвердить" else "Редактировать"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Чекбоксы
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = useLetters,
                                onCheckedChange = { useLetters = it }
                            )
                            Text("Использовать большие и малые буквы")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = useDigits,
                                onCheckedChange = { useDigits = it }
                            )
                            Text("Использовать цифры")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = useSpecial,
                                onCheckedChange = { useSpecial = it }
                            )
                            Text("Использовать спецсимволы")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Количество символов в пароле")

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = passwordLength,
                            onValueChange = { passwordLength = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Длина пароля") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Кнопки
                        Button(
                            onClick = {
                                val length = passwordLength.toIntOrNull() ?: 8
                                if (length !in 6..16) {
                                    showToast = "Некорректная длина пароля (6 - 16 символов)"
                                    return@Button
                                }
                                val password = generatePasswordString(length, useLetters, useDigits, useSpecial)
                                onGeneratedPasswordChange(password)
                                isEditMode = false // Выходим из режима редактирования
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Генерировать")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("password", currentPassword))
                                    showToast = "Пароль скопирован"
                                } catch (_: Exception) {
                                    showToast = "Ошибка копирования"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = copyEnabled
                        ) {
                            Text("Скопировать в буфер")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (currentPassword.isNotEmpty()) {
                                    if (hasMasterPassword) {
                                        activity.launchSavePasswordActivity(currentPassword)
                                    } else {
                                        showToast = "Мастер-пароль не установлен"
                                    }
                                } else {
                                    showToast = "Сначала сгенерируйте пароль"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = saveEnabled
                        ) {
                            Text("Сохранить пароль")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Статус мастера
                        if (!hasMasterPassword) {
                            Text(
                                text = "Мастер-пароль не задан. Сохранение недоступно!",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Версия
                Text(
                    text = "ver. ${BuildConfig.VERSION_NAME}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}