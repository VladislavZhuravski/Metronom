package com.example.metronom

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Импорт для clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.metronom.ui.theme.MetronomTheme

class Help : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MetronomTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 70.dp) // место под нижнюю панель
                    ) {

                        TopBar("Помощь") // Используем компонент TopBar, который был в MainActivity

                        HelpContent()
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
                        BottomNavBarHelp()
                    }
                }
            }
        }
    }
}

// 🔥 Вспомогательный компонент TopBar, как в MainActivity

@Composable
fun HelpContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📘 Добро пожаловать в приложение «Метроном»!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF635AFF)
        )

        HelpCard(
            title = "🎵 Назначение приложения",
            description = "Приложение помогает держать точный темп при занятиях музыкой, тренировках или других задачах, где важен стабильный ритм. Благодаря **Foreground Service**, метроном продолжает работать даже при выключенном экране или при сворачивании приложения."
        )

        HelpCard(
            title = "⚙️ Главный экран",
            description = "На **Главном экране** вы можете:\n" +
                    "1. **Запустить/Остановить** метроном.\n" +
                    "2. **Настроить BPM** (темп) с помощью слайдера или кнопок +/-.\n" +
                    "3. Метроном **динамически** меняет темп, ритм и звук на лету."
        )

        HelpCard(
            title = "🧩 Раздел «Ритм»",
            description = "Здесь вы выбираете **музыкальный размер** (например, 4/4, 3/4 и т.д.). Первый удар в такте всегда звучит **громче** (акцент), что помогает ориентироваться в музыкальной структуре. Изменение ритма применяется мгновенно при возврате на Главный экран."
        )

        HelpCard(
            title = "🔔 Раздел «Звуки»",
            description = "Позволяет выбрать **тембр** метронома (например, классический щелчок, колокольчик, барабан). Выбранный звук будет использоваться в рабочем цикле метронома. Изменение звука применяется мгновенно при возврате на Главный экран."
        )

        HelpCard(
            title = "📱 Навигация и Фон",
            description = "Внизу экрана расположена панель навигации (Метроном, Ритм, Звуки, Помощь). Приложение спроектировано так, что **метроном продолжает работать**, когда вы переходите между этими разделами или сворачиваете приложение."
        )

        HelpCard(
            title = "🛑 Остановка метронома",
            description = "Метроном полностью останавливается только при нажатии кнопки **«Стоп» (квадрат)** на Главном экране."
        )

        Text(
            text = "Спасибо, что используете наш метроном! 🥁",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF635AFF),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun HelpCard(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2FF))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF635AFF)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 15.sp,
            color = Color.DarkGray,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun BottomNavBarHelp() {
    val context = LocalContext.current
    val activeColor = Color(0xFF635AFF)
    val inactiveColor = Color(0xFFB5B3FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2FF))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTextButton(
                label = "Метроном",
                isActive = false,
                onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            BottomNavTextButton(
                label = "Ритм",
                isActive = false,
                onClick = {
                    context.startActivity(Intent(context, Rhythm::class.java))
                },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            BottomNavTextButton(
                label = "Звуки",
                isActive = false,
                onClick = {
                    context.startActivity(Intent(context, Sound::class.java))
                },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            BottomNavTextButton(
                label = "Помощь",
                isActive = true,
                onClick = { /* Уже здесь */ },
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
        }
    }
}

