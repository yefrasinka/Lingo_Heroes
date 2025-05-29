package com.example.lingoheroesapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lingoheroesapp.MainActivity
import com.example.lingoheroesapp.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*
import kotlin.random.Random

object AutomatedNotificationManager {
    private val database = FirebaseDatabase.getInstance()
    private val notificationsRef = database.getReference("notifications")
    private val usersRef = database.getReference("users")
    private const val CHANNEL_ID = "lingo_heroes_channel"
    private var notificationId = 1
    private const val TAG = "AutoNotificationMgr"
    
    private var applicationContext: Context? = null
    
    // Inicjalizacja menedżera powiadomień
    fun init(context: Context) {
        applicationContext = context.applicationContext
        createNotificationChannel(context)
        Log.d(TAG, "AutomatedNotificationManager zainicjalizowany")
    }

    // Inicjalizacja kanału powiadomień (wymagane na Android 8.0+)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lingo Heroes"
            val descriptionText = "Powiadomienia z aplikacji Lingo Heroes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Rejestracja kanału w systemie
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Utworzono kanał powiadomień: $CHANNEL_ID")
        }
    }

    // Funkcja do wysyłania powiadomienia
    private fun sendNotification(title: String, message: String, type: String, userId: String? = null) {
        Log.d(TAG, "Wysyłanie powiadomienia: $title, $message, typ: $type")
        
        // Zapisz w Firebase
        val notification = mapOf(
            "title" to title,
            "message" to message,
            "type" to type,
            "timestamp" to System.currentTimeMillis(),
            "userId" to userId
        )
        
        notificationsRef.push().setValue(notification)
        
        // Próba wysłania lokalnego powiadomienia
        applicationContext?.let { context ->
            showLocalNotification(context, title, message)
        } ?: Log.e(TAG, "Nie można wysłać lokalnego powiadomienia - applicationContext jest null")
    }
    
    // Funkcja do wyświetlania lokalnego powiadomienia
    private fun showLocalNotification(context: Context, title: String, message: String) {
        Log.d(TAG, "Wyświetlanie lokalnego powiadomienia: $title, $message")
        
        try {
            // Stwórz intent do otwarcia aplikacji po kliknięciu w powiadomienie
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Stwórz powiadomienie
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            
            // Wyświetl powiadomienie
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId++, builder.build())
            
            Log.d(TAG, "Pomyślnie wyświetlono powiadomienie")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas wyświetlania powiadomienia: ${e.message}", e)
        }
    }

    // Funkcja do wysyłania losowego powiadomienia z danej kategorii
    private fun sendRandomNotification(category: List<String>, type: String, userId: String? = null) {
        val randomMessage = category[Random.nextInt(category.size)]
        sendNotification("Lingo Heroes", randomMessage, type, userId)
    }

    // Funkcja do sprawdzania postępów użytkownika i wysyłania odpowiednich powiadomień
    fun checkAndSendProgressNotifications(userId: String) {
        usersRef.child(userId).get().addOnSuccessListener { snapshot ->
            val userData = snapshot.value as? Map<*, *>
            
            userData?.let {
                // Sprawdź poziom języka
                val languageLevel = it["languageLevel"] as? Long ?: 0
                if (languageLevel > 0) {
                    sendNotification(
                        "Lingo Heroes",
                        "Gratulacje! Osiągnąłeś poziom $languageLevel w nauce języka!",
                        "progress",
                        userId
                    )
                }

                // Sprawdź liczbę ukończonych lekcji
                val completedLessons = it["completedLessons"] as? Long ?: 0
                if (completedLessons % 10 == 0L && completedLessons > 0) {
                    sendNotification(
                        "Lingo Heroes",
                        "Świetna robota! Ukończyłeś już $completedLessons lekcji!",
                        "progress",
                        userId
                    )
                }

                // Sprawdź liczbę wygranych pojedynków
                val wonDuels = it["wonDuels"] as? Long ?: 0
                if (wonDuels % 5 == 0L && wonDuels > 0) {
                    sendNotification(
                        "Lingo Heroes",
                        "Gratulacje! Wygrałeś już $wonDuels pojedynków PvE!",
                        "progress",
                        userId
                    )
                }

                // Sprawdź pozycję w rankingu
                val rankingPosition = it["rankingPosition"] as? Long ?: 0
                if (rankingPosition > 0 && rankingPosition <= 10) {
                    sendNotification(
                        "Lingo Heroes",
                        "Świetna robota! Jesteś w top $rankingPosition w rankingu!",
                        "progress",
                        userId
                    )
                }
            }
        }
    }

    // Funkcja do wysyłania powiadomień o wyzwaniach
    fun sendChallengeNotification(userId: String? = null) {
        sendRandomNotification(NotificationTemplates.challengeNotifications, "challenge", userId)
    }

    // Funkcja do wysyłania powiadomień o wydarzeniach
    fun sendEventNotification(userId: String? = null) {
        sendRandomNotification(NotificationTemplates.eventNotifications, "event", userId)
    }

    // Funkcja do wysyłania powiadomień o nagrodach
    fun sendRewardNotification(userId: String? = null) {
        sendRandomNotification(NotificationTemplates.rewardNotifications, "reward", userId)
    }

    // Funkcja do wysyłania powiadomień motywacyjnych
    fun sendMotivationalNotification(userId: String? = null) {
        sendRandomNotification(NotificationTemplates.motivationalNotifications, "motivational", userId)
    }

    // Funkcja do wysyłania powiadomień o postępach
    fun sendProgressNotification(userId: String? = null) {
        sendRandomNotification(NotificationTemplates.progressNotifications, "progress", userId)
    }

    // Funkcja do wysyłania powiadomień o nowościach
    fun sendNewContentNotification(userId: String? = null) {
        sendRandomNotification(NotificationTemplates.newContentNotifications, "new_content", userId)
    }

    // Funkcja do subskrybowania użytkownika na powiadomienia
    fun subscribeUserToNotifications(userId: String) {
        // Subskrybuj użytkownika na wszystkie typy powiadomień
        FirebaseMessaging.getInstance().subscribeToTopic("user_$userId")
        FirebaseMessaging.getInstance().subscribeToTopic("challenges")
        FirebaseMessaging.getInstance().subscribeToTopic("events")
        FirebaseMessaging.getInstance().subscribeToTopic("rewards")
        FirebaseMessaging.getInstance().subscribeToTopic("motivational")
        FirebaseMessaging.getInstance().subscribeToTopic("progress")
        FirebaseMessaging.getInstance().subscribeToTopic("new_content")
    }

    // Funkcja do anulowania subskrypcji użytkownika
    fun unsubscribeUserFromNotifications(userId: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("user_$userId")
        FirebaseMessaging.getInstance().unsubscribeFromTopic("challenges")
        FirebaseMessaging.getInstance().unsubscribeFromTopic("events")
        FirebaseMessaging.getInstance().unsubscribeFromTopic("rewards")
        FirebaseMessaging.getInstance().unsubscribeFromTopic("motivational")
        FirebaseMessaging.getInstance().unsubscribeFromTopic("progress")
        FirebaseMessaging.getInstance().unsubscribeFromTopic("new_content")
    }

    // Funkcja do bezpośredniego testowania powiadomień
    fun sendTestNotification(context: Context, message: String = "To jest testowe powiadomienie") {
        Log.d(TAG, "Wysyłanie testowego powiadomienia")
        applicationContext = context.applicationContext
        showLocalNotification(context, "Lingo Heroes Test", message)
    }
} 