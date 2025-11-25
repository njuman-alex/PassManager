package com.c0dex.passmanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.c0dex.passmanager.ui.theme.PassManagerTheme
import com.c0dex.passmanager.ui.theme.SettingsScreen
import kotlinx.coroutines.launch
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    private fun isMasterPasswordSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences("password_prefs", MODE_PRIVATE)
        return prefs.getBoolean("is_master_set", false)
    }

    private fun setMasterPasswordSet(context: Context, isSet: Boolean) {
        val prefs = context.getSharedPreferences("password_prefs", MODE_PRIVATE)
        prefs.edit { putBoolean("is_master_set", isSet) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PassManagerTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onMasterPasswordClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("master_password")
                            },
                            onSettingsClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("settings_screen")
                            }
                        )
                    }
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            // Внутри будет Scaffold
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    TopAppBar(
                                        title = { Text(text = "Менеджер паролей") },
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(
                                                    imageVector = Icons.Default.Menu,
                                                    contentDescription = "Меню"
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor = MaterialTheme.colorScheme.primary,
                                        )
                                    )
                                }
                            ) { innerPadding ->  // ← Это content у Scaffold
                                MainContent(
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                        composable("master_password") {
                            val context = LocalContext.current
                            val isSet = isMasterPasswordSet(context)

                            MasterPasswordScreen(
                                onBackClick = { navController.popBackStack() },
                                onMasterPasswordSet = {
                                    setMasterPasswordSet(this@MainActivity, true)
                                },
                                isMasterPasswordSet = isSet
                            )
                        }
                        composable ( "settings_screen" ) {
                            SettingsScreen { navController.popBackStack() }
                        }

                    }
                }
            }
        }
    }

    @Composable
    fun DrawerContent(
        onMasterPasswordClick: () -> Unit,
        onSettingsClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = "Меню",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Здесь можно добавить пункты меню
            ListItem(
                headlineContent = { Text(text = "Список паролей") },
                modifier = Modifier
                    .clickable {}
                    .padding(vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text(text = "Мастер-пароль") },
                modifier = Modifier
                    .clickable {onMasterPasswordClick()}
                    .padding(vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text(text = "Настройки") },
                modifier = Modifier
                    .clickable {onSettingsClick()}
                    .padding(vertical = 8.dp)
            )
        }
    }

    @Composable
    fun MainContent(modifier: Modifier = Modifier) {
        val context = LocalContext.current

        var generatedPassword by remember { mutableStateOf("") }
        var useLetters by remember { mutableStateOf(false) }
        var useDigitsAndSpecial by remember { mutableStateOf(false) }
        var passwordLength by remember { mutableStateOf("12") }
        var isMasterPasswordSet by remember(context) { mutableStateOf(isMasterPasswordSet(context)) }

        fun generatePassword(
            length: Int,
            useLetters: Boolean,
            useDigitsAndSpecial: Boolean
        ): String {
            if (length <= 0) return ""

            var chars = "abcdefghijkmnpqrstuvwxyz" // без o, l
            if (useLetters) {
                chars += "ABCDEFGHJKMNPQRSTUVWXYZ" // без O, L
            }
            if (useDigitsAndSpecial) {
                chars += "23456789" // без 0 и 1
                chars += "!@#$%^&*()_+-=[]{}|;:,.<>?" // без ` и ~ и других похожих
            }

            if (chars.isEmpty()) return ""

            return buildString {
                repeat(length) {
                    append(chars.random())
                }
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Генератор паролей",
                style = MaterialTheme.typography.headlineMedium
            )

            // Текстовое поле для отображения пароля
            OutlinedTextField(
                value = generatedPassword,
                onValueChange = {}, // Не изменяем вручную
                label = { Text("Сгенерированный пароль") },
                readOnly = true, // Только для чтения
                modifier = Modifier.fillMaxWidth()
            )

            // Чекбокс: использовать буквы
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = useLetters,
                    onCheckedChange = { useLetters = it }
                )
                Text(
                    text = "Использовать большие и малые буквы",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Чекбокс: использовать цифры и спецсимволы
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = useDigitsAndSpecial,
                    onCheckedChange = { useDigitsAndSpecial = it }
                )
                Text(
                    text = "Использовать цифры и спецсимволы",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Поле для длины пароля
            OutlinedTextField(
                value = passwordLength,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) { // Проверка: только цифры
                        passwordLength = newValue
                    }
                },
                label = { Text("Длина пароля") },
                modifier = Modifier.fillMaxWidth()
            )

            // Кнопка "Генерировать"
            Button(
                onClick = {
                    val length = passwordLength.toIntOrNull() ?: 12 // Если не число — 12
                    generatedPassword = if (length > 0) {
                        generatePassword(length, useLetters, useDigitsAndSpecial)
                    } else {
                        ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Генерировать")
            }
            // Кнопка "Сохранить"
            Button(
                onClick = {

                },
                enabled = generatedPassword.isNotEmpty() && isMasterPasswordSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }

// Показываем текст, если мастер-пароль не установлен
            if (!isMasterPasswordSet) {
                Text(
                    text = "Мастер-пароль не задан. Сохранение не доступно!",
                    color = MaterialTheme.colorScheme.error, // Красный цвет (ошибка)
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MainContentPreview() {
        PassManagerTheme {
            MainContent()
        }
    }
}