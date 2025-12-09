package com.example.metronom

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.metronom.ui.theme.MetronomTheme


/**
 * MainActivity - Главный экран приложения для управления метрономом.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем версию Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            MetronomTheme {

                // Локальное состояние BPM для UI. Используется remember для сохранения состояния
                // при перерисовках и инициализируется из глобального AppState.
                var bpm by remember { mutableStateOf(AppState.currentBpm.toFloat()) }
                val context = LocalContext.current // Получаем контекст для запуска сервисов/активити

                // Основной Compose-компонент, который строит весь UI.
                ColumnAndUI(
                    bpm = bpm,
                    isRunning = AppState.isRunning, // Для кнопки Старт/Стоп
                    // Лямбда-функция для обработки нажатия кнопки Старт/Стоп
                    onStartStop = {
                        val intent = Intent(context, MetronomeService::class.java)

                        if (!AppState.isRunning) {
                            // Логика СТАРТА метронома
                            // Передаем текущие параметры в сервис через Intent.
                            intent.putExtra("bpm", bpm.toInt())
                            intent.putExtra("rhythm", AppState.selectedRhythm)
                            // Передаем Int ID ресурса звука, который был выбран на экране Sound.
                            intent.putExtra("soundRes", AppSoundHolder.selectedSoundRes)
                            intent.putExtra("ACTION", "START") // Команда сервису: начать работу.
                            context.startService(intent) // Запускаем сервис.
                            AppState.isRunning = true    // Обновляем глобальное состояние для UI.
                            AppState.currentBpm = bpm.toInt() // Сохраняем актуальный BPM.
                        } else {
                            // Логика ОСТАНОВКИ метронома
                            // Отправляем команду на остановку сервиса.
                            context.stopService(Intent(context, MetronomeService::class.java))
                            AppState.isRunning = false // Обновляем глобальное состояние для UI.
                        }
                    },
                    // Лямбда-функция для обработки изменения BPM (слайдером или кнопками +/-)
                    onBpmChange = { newBpm ->
                        bpm = newBpm // Обновляем локальное UI-состояние.
                        AppState.currentBpm = newBpm.toInt() // Обновляем глобальное состояние.

                        // Если метроном уже запущен, отправляем сервису команду на обновление BPM.
                        if (AppState.isRunning) {
                            // ОБНОВЛЕНИЕ BPM
                            val intent = Intent(context, MetronomeService::class.java).apply {
                                putExtra("bpm", newBpm.toInt())
                                // Отправляем также ритм и звук на случай, если они менялись,
                                // но главное действие — обновить темп.
                                putExtra("rhythm", AppState.selectedRhythm)
                                putExtra("soundRes", AppSoundHolder.selectedSoundRes)
                                putExtra("ACTION", "UPDATE_BPM") // Команда сервису: изменить темп.
                            }
                            context.startService(intent)
                        }
                    }
                )
            }
        }
    }

    /**
     * Фоновый режим активити
     */
    override fun onPause() {
        super.onPause()
        // Сохраняем текущее состояние запуска во временную переменную.
        // Это нужно, чтобы знать, должен ли метроном работать, когда пользователь вернется.
        AppState.wasRunning = AppState.isRunning
    }

    /**
     * Вызывается, когда активити возвращается из фонового режима.
     */
    override fun onResume() {
        super.onResume()

        // Восстанавливаем состояние кнопки "Старт/Стоп" на основе сохраненного флага.
        AppState.isRunning = AppState.wasRunning

        // Если метроном должен быть запущен (был запущен до сворачивания),
        // отправляем команду сервису, чтобы он проверил, не изменились ли настройки.
        if (AppState.isRunning) {
            val context = this
            val intent = Intent(context, MetronomeService::class.java).apply {
                // Отправляем текущие параметры.
                putExtra("bpm", AppState.currentBpm)
                putExtra("rhythm", AppState.selectedRhythm)
                putExtra("soundRes", AppSoundHolder.selectedSoundRes)
                // Команда: Проверить все изменения (ритм и звук могли измениться на других экранах).
                putExtra("ACTION", "UPDATE_ALL")
            }
            // Используем startService, чтобы передать новые настройки уже работающему сервису.
            context.startService(intent)
        }
    }

    override fun onStop() {
        super.onStop()
    }
}

// ------------------------------------------------------------------------------------
// --- ГЛОБАЛЬНОЕ СОСТОЯНИЕ И UI КОМПОНЕНТЫ ---
// ------------------------------------------------------------------------------------

/**
 * AppState - Синглтон для хранения глобальных настроек метронома.
 */
object AppState {
    var currentBpm: Int = 120 // Текущий темп, по умолчанию 120.
    var selectedSound: String = "Колокольчик" // Имя звука для отображения в UI.
    var selectedRhythm: String = "4/4" // Выбранный ритм.

    // Реактивное состояние: изменение isRunning автоматически перерисовывает UI (например, кнопку).
    var isRunning by mutableStateOf(false)
    // Флаг для восстановления состояния при возврате из фона.
    var wasRunning by mutableStateOf(false)
}

/**
 * AppSoundHolder - Синглтон для хранения ID ресурса звука.
 * Этот Int ID используется MetronomeService для загрузки SoundPool.
 */
object AppSoundHolder {
    var selectedSoundRes: Int = R.raw.bell // Фактический ID ресурса (например, R.raw.bell).
}

/**
 * ColumnAndUI - Основной компонент для построения макета главного экрана.
 */
@Composable
fun ColumnAndUI(
    bpm: Float,
    isRunning: Boolean,
    onStartStop: () -> Unit, // Лямбда для Start/Stop
    onBpmChange: (Float) -> Unit // Лямбда для изменения BPM
) {
    // Box позволяет наложить нижнюю панель поверх основного контента.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Делаем основную колонку прокручиваемой на случай маленьких экранов.
                .verticalScroll(rememberScrollState())
                // Отступ снизу, чтобы контент не перекрывался навигационной панелью.
                .padding(bottom = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar("Метроном")
            Spacer(modifier = Modifier.height(28.dp))

            // Круг BPM (визуальный индикатор темпа)
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape) // Обрезка в форме круга
                    .background(
                        // Градиентная заливка для красивого вида.
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF7A84FF), Color(0xFF4D5AFF))
                        )
                    )
                    .shadow(10.dp, CircleShape), // Тень для объема
                contentAlignment = Alignment.Center
            ) {
                // Отображение BPM, округленное до целого числа.
                Text(
                    text = "${bpm.toInt()}",
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Компонент для кнопок управления
            MetronomeControls(
                isRunning = isRunning,
                onMinusClick = {
                    // Уменьшение BPM, с ограничением снизу до 40.
                    val newBpm = (bpm - 1).coerceAtLeast(40f)
                    onBpmChange(newBpm)
                },
                onStartClick = onStartStop, // Передаем колбэк запуска/остановки
                onPlusClick = {
                    // Увеличение BPM, с ограничением сверху до 240.
                    val newBpm = (bpm + 1).coerceAtMost(240f)
                    onBpmChange(newBpm)
                }
            )

            // Слайдер для настройки BPM.
            Slider(
                value = bpm,
                // Вызывается при движении ползунка, передает новое значение BPM.
                onValueChange = { onBpmChange(it.coerceIn(40f, 240f)) },
                valueRange = 40f..240f, // Диапазон значений BPM.
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF635AFF),
                    activeTrackColor = Color(0xFF635AFF),
                    inactiveTrackColor = Color(0xFFB5B3FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            )

            // Информационные карточки, отображающие текущий выбор
            InfoCard(text = "Звук — ${AppState.selectedSound}")
            InfoCard(text = "Ритм — ${AppState.selectedRhythm}")
            Spacer(modifier = Modifier.height(70.dp))
        }

        // Размещаем нижнюю панель навигации внизу Box.
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomNavBarMain()
        }
    }
}

/**
 * MetronomeControls - Компонент для кнопок управления темпом (+, -, Start/Stop).
 */
@Composable
fun MetronomeControls(
    isRunning: Boolean,
    onMinusClick: () -> Unit,
    onStartClick: () -> Unit,
    onPlusClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center, // Кнопки центрированы.
        verticalAlignment = Alignment.CenterVertically
    ) {
        val buttonBrush = Brush.linearGradient(listOf(Color(0xFF7A84FF), Color(0xFF635AFF)))
        CircleButton("-", buttonBrush, onMinusClick)
        Spacer(modifier = Modifier.width(24.dp))
        // Текст кнопки меняется в зависимости от состояния (▶ Пуск или ■ Стоп).
        CircleButton(if (isRunning) "■" else "▶", buttonBrush, onStartClick)
        Spacer(modifier = Modifier.width(24.dp))
        CircleButton("+", buttonBrush, onPlusClick)
    }
}

/**
 * CircleButton - Переиспользуемый компонент для круглой кнопки с градиентом.
 */
@Composable
fun CircleButton(text: String, brush: Brush, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape) // Обрезка в форме круга
            .background(brush)
            .shadow(8.dp, CircleShape) // Добавление тени
            .clickable { onClick() }, // Обработка клика
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * InfoCard - Компонент для отображения дополнительной информации (Звук, Ритм).
 */
@Composable
fun InfoCard(text: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color(0xFFEAE9FF), RoundedCornerShape(16.dp)) // Светло-голубой фон со скруглением
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4D47FF))
    }
}

/**
 * BottomNavBarMain - Компонент для нижней панели навигации.
 */
@Composable
fun BottomNavBarMain() {
    val context = LocalContext.current
    val activeColor = Color(0xFF635AFF)
    val inactiveColor = Color(0xFFB5B3FF)

    Box(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2FF)).padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, // Равное расстояние между элементами
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "Метроном" активна (true) и не выполняет переход.
            BottomNavTextButton("Метроном", true, { }, activeColor, inactiveColor)
            // Кнопка "Ритм" неактивна, запускает Rhythm::class.
            BottomNavTextButton("Ритм", false, { context.startActivity(Intent(context, Rhythm::class.java)) }, activeColor, inactiveColor)
            // Кнопка "Звуки" неактивна, запускает Sound::class.
            BottomNavTextButton("Звуки", false, { context.startActivity(Intent(context, Sound::class.java)) }, activeColor, inactiveColor)
            // Кнопка "Помощь" неактивна, запускает Help::class.
            BottomNavTextButton("Помощь", false, { context.startActivity(Intent(context, Help::class.java)) }, activeColor, inactiveColor)
        }
    }
}

/**
 * BottomNavTextButton - Переиспользуемый компонент для элемента навигационной панели.
 */
@Composable
fun BottomNavTextButton(
    label: String,
    isActive: Boolean, // Флаг активности
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            // Фон: если активна, то полупрозрачный акцентный цвет, иначе прозрачный.
            .background(if (isActive) activeColor.copy(0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            // Цвет текста зависит от активности.
            color = if (isActive) activeColor else inactiveColor,
            // Жирность шрифта зависит от активности.
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * TopBar - Верхняя панель заголовка.
 */
@Composable
fun TopBar(title: String) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF635AFF))
            // Добавляет отступ для системного статус-бара, чтобы UI не залезал под него.
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}