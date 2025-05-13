package com.example.lingoheroesapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase

class NotificationService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID = "lingo_heroes_channel"
        private const val CHANNEL_NAME = "Lingo Heroes Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for challenges, events and rewards"
        private const val NOTIFICATION_ID = 1
    }

    private val database = FirebaseDatabase.getInstance()
    private val notificationsRef = database.getReference("notifications")

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Zapisz token w bazie danych dla aktualnie zalogowanego użytkownika
        val userId = getCurrentUserId()
        userId?.let {
            database.getReference("users")
                .child(it)
                .child("fcmToken")
                .setValue(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Obsługa powiadomień z Firebase Cloud Messaging
        message.notification?.let { notification ->
            val title = notification.title ?: "Lingo Heroes"
            val body = notification.body ?: ""
            val type = message.data["type"] ?: "general"
            
            // Zapisz powiadomienie w bazie danych
            saveNotificationToDatabase(title, body, type)
            
            // Wyświetl powiadomienie
            showNotification(title, body, type)
        }
    }

    private fun saveNotificationToDatabase(title: String, message: String, type: String) {
        val notification = mapOf(
            "title" to title,
            "message" to message,
            "type" to type,
            "timestamp" to System.currentTimeMillis(),
            "userId" to getCurrentUserId()
        )
        
        notificationsRef.push().setValue(notification)
    }

    private fun showNotification(title: String, message: String, type: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Utwórz kanał powiadomień dla Android 8.0 i nowszych
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        // Przygotuj intent do otwarcia aplikacji po kliknięciu w powiadomienie
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Utwórz powiadomienie
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#FF5722")) // Kolor akcentu aplikacji
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(type.hashCode(), notification)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                
                // Ustaw dźwięk powiadomienia
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getCurrentUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
} 