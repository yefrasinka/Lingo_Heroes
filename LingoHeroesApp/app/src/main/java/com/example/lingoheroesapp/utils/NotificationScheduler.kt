package com.example.lingoheroesapp.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.lingoheroesapp.receivers.NotificationReceiver
import kotlin.random.Random

object NotificationScheduler {
    private const val CHALLENGE_NOTIFICATION_INTERVAL = 24 * 60 * 60 * 1000L // 24 godziny
    private const val EVENT_NOTIFICATION_INTERVAL = 3 * 24 * 60 * 60 * 1000L // 3 dni
    private const val REWARD_NOTIFICATION_INTERVAL = 60 * 1000L // 1 minuta
    private const val MOTIVATIONAL_NOTIFICATION_INTERVAL = 8 * 60 * 60 * 1000L // 8 godzin
    private const val PROGRESS_NOTIFICATION_INTERVAL = 48 * 60 * 60 * 1000L // 48 godzin
    private const val NEW_CONTENT_NOTIFICATION_INTERVAL = 7 * 24 * 60 * 60 * 1000L // 7 dni

    fun scheduleNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Harmonogram powiadomień o wyzwaniach
        scheduleNotification(
            context,
            alarmManager,
            "challenge",
            CHALLENGE_NOTIFICATION_INTERVAL
        )

        // Harmonogram powiadomień o wydarzeniach
        scheduleNotification(
            context,
            alarmManager,
            "event",
            EVENT_NOTIFICATION_INTERVAL
        )

        // Harmonogram powiadomień o nagrodach
        scheduleNotification(
            context,
            alarmManager,
            "reward",
            REWARD_NOTIFICATION_INTERVAL
        )

        // Harmonogram powiadomień motywacyjnych
        scheduleNotification(
            context,
            alarmManager,
            "motivational",
            MOTIVATIONAL_NOTIFICATION_INTERVAL
        )

        // Harmonogram powiadomień o postępach
        scheduleNotification(
            context,
            alarmManager,
            "progress",
            PROGRESS_NOTIFICATION_INTERVAL
        )

        // Harmonogram powiadomień o nowościach
        scheduleNotification(
            context,
            alarmManager,
            "new_content",
            NEW_CONTENT_NOTIFICATION_INTERVAL
        )
    }

    private fun scheduleNotification(
        context: Context,
        alarmManager: AlarmManager,
        type: String,
        interval: Long
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.example.lingoheroesapp.NOTIFICATION"
            putExtra("type", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ustaw pierwsze powiadomienie na losowy czas (szybciej dla "reward")
        val firstTriggerTime = if (type == "reward") {
            System.currentTimeMillis() + 10000 // 10 sekund
        } else {
            System.currentTimeMillis() + Random.nextLong(0, 24 * 60 * 60 * 1000)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            firstTriggerTime,
            interval,
            pendingIntent
        )
    }

    fun cancelAllNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val types = listOf("challenge", "event", "reward", "motivational", "progress", "new_content")

        types.forEach { type ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "com.example.lingoheroesapp.NOTIFICATION"
                putExtra("type", type)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                type.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
        }
    }
} 