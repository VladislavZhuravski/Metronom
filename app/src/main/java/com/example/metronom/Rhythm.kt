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

/**
 * Rhythm — это активити (экран) для выбора тактового размера (ритма) метронома.
 * Она позволяет пользователю выбрать один из стандартных ритмов и сохраняет выбор в AppState.
 */
class Rhythm : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Включает отрисовку от края до края (для лучшего использования пространства экрана).
        enableEdgeToEdge()

        setContent {
            // Локальное реактивное состояние для выбранного ритма.
            // Инициализируется из глобального состояния AppState.
            var selectedRhythm by remember { mutableStateOf(AppState.selectedRhythm) }

            MetronomTheme {
                // Используем Box для наложения нижней навигационной панели поверх основного контента.
                Box(modifier = Modifier.fillMaxSize()) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            // Отступ снизу, чтобы контент не перекрывался BottomNavBar.
                            .padding(bottom = 70.dp)
                    ) {

                        // Компонент верхнего заголовка с названием экрана.
                        TopBar("Ритм")

                        Spacer(modifier = Modifier.height(24.dp))

                        // Основной компонент с кнопками выбора ритма.
                        RhythmContent(
                            selectedRhythm = selectedRhythm,
                            // Лямбда-функция, которая вызывается при нажатии на кнопку ритма.
                            onRhythmSelected = { rhythm ->
                                selectedRhythm = rhythm // Обновляем локальное UI-состояние.
                                AppState.selectedRhythm = rhythm // Обновляем глобальное состояние.
                            }
                        )
                    }

                    // Контейнер для навигационной панели, выравнивание по нижнему центру.
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        BottomNavBarRhythm()
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
 * RhythmContent — Компонент, отображающий список кнопок для выбора тактового размера.
 */
@Composable
fun RhythmContent(
    selectedRhythm: String, // Текущий выбранный ритм для подсветки
    onRhythmSelected: (String) -> Unit // Колбэк при выборе нового ритма
) {
    // Список доступных ритмов.
    val rhythms = listOf("3/4", "4/4", "5/4", "6/8", "7/8", "9/8")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок секции.
        Text(
            text = "Выберите ритм",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4D47FF),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Разделение списка ритмов на строки по 2 элемента в каждой.
        rhythms.chunked(2).forEach { rowRhythms ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                // Расстояние между кнопками в ряду.
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Итерация по двум ритмам в текущей строке.
                rowRhythms.forEach { rhythm ->
                    val isSelected = rhythm == selectedRhythm // Проверяем, активна ли кнопка.
                    Button(
                        onClick = { onRhythmSelected(rhythm) }, // Обработка нажатия.
                        modifier = Modifier
                            .weight(1f) // Занимает половину доступной ширины.
                            .height(70.dp),
                        shape = RoundedCornerShape(16.dp), // Скругленные углы.
                        colors = ButtonDefaults.buttonColors(
                            // Цвет фона зависит от того, выбран ли ритм.
                            containerColor = if (isSelected) Color(0xFF635AFF) else Color(0xFFE0E0FF),
                            // Цвет текста зависит от того, выбран ли ритм.
                            contentColor = if (isSelected) Color.White else Color(0xFF635AFF)
                        )
                    ) {
                        Text(
                            text = rhythm,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Если в последней строке только одна кнопка, добавляем пустой Spacer,
                // чтобы выровнять кнопку по левому краю и занять оставшееся место.
                if (rowRhythms.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(14.dp)) // Расстояние между строками кнопок.
        }
    }
}

/**
 * BottomNavBarRhythm — Компонент нижней навигационной панели для экрана "Ритм".
 */
@Composable
fun BottomNavBarRhythm() {
    val context = LocalContext.current
    val activeColor = Color(0xFF635AFF)
    val inactiveColor = Color(0xFFB5B3FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2FF)) // Светлый фон навигационной панели.
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, // Равное распределение места.
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "Метроном" (переход на главный экран).
            BottomNavTextButton("Метроном", false, {
                context.startActivity(Intent(context, MainActivity::class.java))
            }, activeColor, inactiveColor)

            // Кнопка "Ритм" (текущий экран), помечена как активная (true) и без действия.
            BottomNavTextButton("Ритм", true, { }, activeColor, inactiveColor)

            // Кнопка "Звуки" (переход на экран Sound).
            BottomNavTextButton("Звуки", false, {
                context.startActivity(Intent(context, Sound::class.java))
            }, activeColor, inactiveColor)

            // Кнопка "Помощь" (переход на экран Help).
            BottomNavTextButton("Помощь", false, {
                context.startActivity(Intent(context, Help::class.java))
            }, activeColor, inactiveColor)
        }
    }
}

