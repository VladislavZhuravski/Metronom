package com.example.metronom

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.metronom.ui.theme.MetronomTheme
// Предполагается, что AppState и AppSoundHolder доступны (возможно, определены в корневом пакете).
// R импортируется автоматически для доступа к ресурсам (R.raw.bell и т.д.).

/**
 * Sound — это активити (экран), который позволяет пользователю выбрать один из
 * доступных звуков метронома. Выбор сохраняется в AppState и AppSoundHolder.
 */
class Sound : ComponentActivity() {

    // 1. Список отображаемых названий звуков (для UI).
    private val soundTitles = listOf("Колокольчик", "Малый барабан", "Фонк", "Клавесин")
    // 2. Список соответствующих ID ресурсов (для MetronomeService).
    private val soundResIds = listOf(R.raw.bell, R.raw.smalldrum, R.raw.fonk, R.raw.claves)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Включаем режим отрисовки от края до края.
        enableEdgeToEdge()

        setContent {
            MetronomTheme {
                // Реактивное состояние, хранящее индекс (позицию) выбранного звука в списке.
                var selectedSoundIdx by remember {
                    // Инициализация: ищем индекс, соответствующий текущему названию в AppState.
                    // Если не находим (например, AppState пуст), используем индекс 0 (Колокольчик).
                    mutableStateOf(
                        soundTitles.indexOf(AppState.selectedSound).takeIf { it >= 0 } ?: 0
                    )
                }

                // Box используется для позиционирования нижней навигационной панели (BottomNavBar)
                // поверх основного контента.
                Box(modifier = Modifier.fillMaxSize()) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            // Отступ, чтобы контент не перекрывался навигационной панелью.
                            .padding(bottom = 70.dp)
                    ) {

                        // Верхний заголовок экрана.
                        TopBar("Звуки")

                        Spacer(modifier = Modifier.height(20.dp))

                        // Контейнер для списка кнопок выбора звука.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Расстояние между кнопками
                        ) {
                            // Заголовок секции.
                            Text(
                                text = "Выберите звук для метронома",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4D47FF),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Создание кнопок для каждого звука.
                            soundTitles.forEachIndexed { idx, sound ->
                                val isSelected = idx == selectedSoundIdx // Проверяем, выбрана ли эта кнопка.
                                Button(
                                    onClick = {
                                        selectedSoundIdx = idx // Обновляем локальное UI-состояние.

                                        // 1. Обновляем строковое имя в AppState для отображения в UI.
                                        AppState.selectedSound = soundTitles[idx]

                                        // 2. !!! ГЛАВНОЕ: Обновляем ресурс ID в AppSoundHolder.
                                        // Именно этот ID будет использоваться MetronomeService для загрузки и воспроизведения звука.
                                        AppSoundHolder.selectedSoundRes = soundResIds[idx]
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        // Цветовая схема для активной/неактивной кнопки.
                                        containerColor = if (isSelected)
                                            Color(0xFF635AFF) // Активный фон
                                        else
                                            Color(0xFFEAE9FF), // Неактивный фон
                                        contentColor = if (isSelected)
                                            Color.White // Активный текст
                                        else
                                            Color(0xFF4D47FF) // Неактивный текст
                                    )
                                ) {
                                    Text(
                                        text = sound,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Spacer с весом, чтобы вытолкнуть контент вверх, а нижнюю панель вниз.
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Позиционирование нижней навигационной панели.
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        BottomNavBarSound()
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// --- COMPOSE КОМПОНЕНТЫ ---
// ------------------------------------------------------------------------------------

/**
 * BottomNavBarSound — Компонент нижней навигационной панели для экрана "Звуки".
 */
@Composable
fun BottomNavBarSound() {
    val context = LocalContext.current
    val activeColor = Color(0xFF635AFF)
    val inactiveColor = Color(0xFFB5B3FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2FF)) // Фон панели.
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, // Распределение кнопок
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка 1: Метроном (MainActivity)
            BottomNavTextButton(
                label = "Метроном",
                isActive = false,
                onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            // Кнопка 2: Ритм (Rhythm)
            BottomNavTextButton(
                label = "Ритм",
                isActive = false,
                onClick = {
                    context.startActivity(Intent(context, Rhythm::class.java))
                },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            // Кнопка 3: Звуки (Текущий экран) - активна.
            BottomNavTextButton(
                label = "Звуки",
                isActive = true, // Показывает, что это текущий экран
                onClick = { /* Остаемся на этом экране */ },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            // Кнопка 4: Помощь (Help)
            BottomNavTextButton(
                label = "Помощь",
                isActive = false,
                onClick = {
                    context.startActivity(Intent(context, Help::class.java))
                },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
        }
    }
}