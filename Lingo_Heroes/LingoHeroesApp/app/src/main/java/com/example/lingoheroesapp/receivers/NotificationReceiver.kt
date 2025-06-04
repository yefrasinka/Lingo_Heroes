package com.example.lingoheroesapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lingoheroesapp.utils.AutomatedNotificationManager
import com.google.firebase.auth.FirebaseAuth

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Otrzymano intent: ${intent.action}")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(TAG, "Aktualny userId: $userId")
        
        val notificationType = intent.getStringExtra("type")
        Log.d(TAG, "Typ powiadomienia: $notificationType")

        when (notificationType) {
            "challenge" -> {
                Log.d(TAG, "Wysyłanie powiadomienia o wyzwaniu")
                AutomatedNotificationManager.sendChallengeNotification(userId)
            }
            "event" -> {
                Log.d(TAG, "Wysyłanie powiadomienia o wydarzeniu")
                AutomatedNotificationManager.sendEventNotification(userId)
            }
            "reward" -> {
                Log.d(TAG, "Wysyłanie powiadomienia o nagrodzie")
                AutomatedNotificationManager.sendRewardNotification(userId)
            }
            "motivational" -> {
                Log.d(TAG, "Wysyłanie powiadomienia motywacyjnego")
                AutomatedNotificationManager.sendMotivationalNotification(userId)
            }
            "progress" -> {
                Log.d(TAG, "Sprawdzanie i wysyłanie powiadomienia o postępie")
                userId?.let {
                    AutomatedNotificationManager.checkAndSendProgressNotifications(it)
                }
            }
            "new_content" -> {
                Log.d(TAG, "Wysyłanie powiadomienia o nowej zawartości")
                AutomatedNotificationManager.sendNewContentNotification(userId)
            }
        }
    }
} 