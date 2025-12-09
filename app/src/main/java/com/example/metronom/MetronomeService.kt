package com.example.metronom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import kotlinx.coroutines.* // Для асинхронной работы метронома (цикл ударов)
import com.example.metronom.R // Ресурсы приложения (звуки)

/**
 * MetronomeService - это Android-сервис, который отвечает за выполнение
 * ритмических ударов метронома в фоновом режиме, даже когда приложение свернуто.
 * Он запускается как Foreground Service (сервис переднего плана), чтобы система
 * не завершила его из-за нехватки ресурсов.
 */
class MetronomeService : Service() {

    // --- Переменные состояния метронома ---
    private var soundPool: SoundPool? = null // Объект для быстрого воспроизведения звука
    @Volatile private var soundId: Int = 0 // ID загруженного звука в SoundPool (не ресурс ID!)
    @Volatile private var isSoundLoaded = false // Флаг, указывающий, загружен ли звук
    @Volatile private var currentBpm: Int = 120 // Текущий темп (ударов в минуту)
    @Volatile private var currentSoundResId: Int = R.raw.bell // ID ресурса звука (например, bell.wav)
    @Volatile private var beatsPerBar: Int = 4 // Количество ударов в такте (числитель ритма, например, 4 в 4/4)

    private val serviceScope = GlobalScope // Область видимости для корутин сервиса
    private var job: Job? = null // Корутина, выполняющая главный цикл ударов
    @Volatile private var beatIndex = 0 // Счетчик ударов в такте (0 — это акцентированный удар)

    private var wakeLock: PowerManager.WakeLock? = null // Замок, предотвращающий засыпание процессора

    // Сервис не связан с Activity, поэтому onBind всегда возвращает null.
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Инициализация WakeLock
        // Создаем PARTIAL_WAKE_LOCK. Он позволяет процессору работать,
        // даже если экран выключен, что критически важно для точного отсчета времени.
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MetronomeService::WakeLockTag" // Уникальный тег для отслеживания
        )

        // 2. Инициализация SoundPool
        // SoundPool идеально подходит для коротких, низколатентных звуков (как удары метронома).
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA) // Тип использования: медиа
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) // Тип контента
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4) // Максимум 4 одновременно играющих звука
            .setAudioAttributes(attrs)
            .build()

        // Устанавливаем слушатель, который срабатывает, когда звук полностью загружен.
        soundPool?.setOnLoadCompleteListener { _, loadedId, status ->
            if (status == 0 && loadedId == soundId) {
                isSoundLoaded = true // Звук готов
                startLoop() // Запускаем цикл метронома, если он еще не запущен
            } else if (status != 0) {
                isSoundLoaded = false // Ошибка загрузки
            }
        }

        // Запускаем сервис в режиме переднего плана, показывая уведомление.
        startForegroundNotification()
    }

    /**
     * Вызывается, когда Activity отправляет команду сервису через startService().
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Извлекаем действие и новые параметры.
        val action = intent?.getStringExtra("ACTION") ?: "UPDATE_ALL"
        val newBpm = intent?.getIntExtra("bpm", 120) ?: 120
        val newRhythm = intent?.getStringExtra("rhythm") ?: "4/4"
        val newSoundResId = intent?.getIntExtra("soundRes", R.raw.bell) ?: R.raw.bell

        // 1. Обновляем BPM и парсим ритм.
        currentBpm = newBpm
        val newBeatsPerBar = parseBeats(newRhythm) // Получаем числитель ритма (например, 4 из 4/4)

        // --- Обработка действий ---
        if (action == "START") {
            // 🚀 НОВЫЙ ЗАПУСК
            beatIndex = 0 // Сбрасываем счетчик ударов, чтобы следующий удар был акцентированным.
            beatsPerBar = newBeatsPerBar
            stopLoop() // Останавливаем старый цикл, если он был активен.

            // Активируем WakeLock, чтобы цикл не прерывался.
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
            }

            // Проверяем, нужно ли загружать новый звук (если ID изменился или SoundPool пуст).
            if (newSoundResId != currentSoundResId || soundId == 0) {
                if (soundId != 0) soundPool?.unload(soundId) // Выгружаем старый звук
                currentSoundResId = newSoundResId
                isSoundLoaded = false
                // Запускаем асинхронную загрузку нового звука.
                soundId = soundPool?.load(this, currentSoundResId, 1) ?: 0
                // Цикл запустится в onLoadCompleteListener после успешной загрузки.
            } else {
                // Если звук не менялся и уже загружен, запускаем цикл сразу.
                if (isSoundLoaded) {
                    startLoop()
                }
            }

        } else if (action == "UPDATE_BPM") {
            // 🔄 Обновление только BPM
            beatsPerBar = newBeatsPerBar // Обновляем ритм на случай, если он поменялся вместе с BPM
            startLoop() // Убеждаемся, что цикл запущен (он автоматически подхватит новый currentBpm).

        } else if (action == "UPDATE_ALL") {
            // 🔄 Обновление Ритма/Звука (при возвращении из других Activity)

            val rhythmChanged = newBeatsPerBar != beatsPerBar
            beatsPerBar = newBeatsPerBar

            // Если ритм изменился (например, с 4/4 на 3/4), сбрасываем счетчик,
            // чтобы первый удар нового ритма был акцентированным.
            if (rhythmChanged) {
                beatIndex = 0
            }

            // Проверка смены звука
            if (newSoundResId != currentSoundResId) {
                if (soundId != 0) soundPool?.unload(soundId) // Выгружаем старый
                currentSoundResId = newSoundResId
                isSoundLoaded = false
                // Загружаем новый, цикл запустится после загрузки.
                soundId = soundPool?.load(this, currentSoundResId, 1) ?: 0
            } else {
                // Если звук не менялся, просто убеждаемся, что цикл активен.
                startLoop()
            }
        }

        // Обновляем текст уведомления с текущим BPM.
        updateForegroundNotification()
        // START_STICKY: если сервис будет убит системой, он будет перезапущен с null Intent.
        return START_STICKY
    }

    // --- Вспомогательные методы ---

    /**
     * Создает канал уведомлений и запускает сервис в режиме Foreground.
     * Это обязательно для долгой фоновой работы.
     */
    private fun startForegroundNotification() {
        val channelId = "metronome_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Создание канала для Android 8.0 и выше.
            val channel = NotificationChannel(channelId, "Metronome", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        // Создание самого уведомления
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }

        val notification = notificationBuilder
            .setContentTitle("Метроном работает")
            .setContentText("Метроном в фоне (${currentBpm} BPM)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        // Запуск Foreground-режима. 1 - уникальный ID уведомления.
        startForeground(1, notification)
    }

    /**
     * Обновляет существующее уведомление с актуальным BPM.
     */
    private fun updateForegroundNotification() {
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "metronome_channel")
        } else {
            Notification.Builder(this)
        }

        val notification = notificationBuilder
            .setContentTitle("Метроном работает")
            .setContentText("Метроном в фоне (${currentBpm} BPM)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        // Обновление уведомления по его ID.
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, notification)
    }

    /**
     * Парсит числитель из строки ритма (например, "4/4" -> 4).
     */
    private fun parseBeats(rhythm: String): Int {
        return try {
            rhythm.split("/")[0].toInt()
        } catch (_: Exception) {
            4 // По умолчанию 4 удара в такте
        }
    }

    /**
     * Останавливает цикл метронома и освобождает WakeLock.
     */
    private fun stopLoop() {
        job?.cancel() // Отменяем корутину (цикл)
        job = null

        // Освобождаем WakeLock, чтобы процессор мог заснуть, если экран выключен.
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    /**
     * Запускает главный асинхронный цикл ударов метронома.
     */
    private fun startLoop() {
        // Если цикл уже запущен и активен, ничего не делаем.
        if (job != null && job?.isActive == true) return

        // Запускаем новую корутину. Dispatchers.Default подходит для CPU-интенсивной работы.
        job = serviceScope.launch(Dispatchers.Default) {

            // 🔥 Повышение приоритета потока
            // Установка приоритета AUDIO помогает снизить задержку и повысить точность ударов.
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            delay(1) // Небольшая задержка для старта

            // Цикл будет работать, пока корутина активна (isActive).
            while (isActive) {

                // Вычисляем базовый интервал между ударами в миллисекундах (60000 мс в минуте).
                val baseIntervalMs = 60000.0 / currentBpm
                val startTime = System.currentTimeMillis()

                // Воспроизведение звука
                if (isSoundLoaded && soundId != 0) {
                    // Громкость: 1.0f для акцентированного удара (beatIndex == 0), 0.5f для остальных.
                    val volume = if (beatIndex == 0) 1.0f else 0.5f
                    soundPool?.play(soundId, volume, volume, 1, 0, 1f)
                }

                // Увеличиваем счетчик и берем остаток от деления на beatsPerBar,
                // чтобы счетчик циклически переходил от 0 до beatsPerBar - 1.
                beatIndex = (beatIndex + 1) % beatsPerBar

                // Компенсация джиттера (jitter compensation)
                // Измеряем время, которое фактически заняло выполнение кода выше (воспроизведение).
                val elapsed = System.currentTimeMillis() - startTime
                // Вычисляем, сколько еще нужно ждать, чтобы получить точный интервал.
                val nextDelay = (baseIntervalMs - elapsed).toLong()

                // Задержка до следующего удара. Убеждаемся, что задержка не меньше 1 мс.
                delay(nextDelay.coerceAtLeast(1L))
            }
        }
    }

    /**
     * Вызывается, когда сервис останавливается через stopService().
     */
    override fun onDestroy() {
        stopLoop() // Останавливаем цикл и освобождаем WakeLock.
        soundPool?.release() // Освобождаем ресурсы SoundPool.
        soundPool = null
        super.onDestroy()
    }
}