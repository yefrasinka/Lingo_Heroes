package com.example.lingoheroesapp.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

object NotificationManager {
    
    fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Tutaj możesz dodać kod do wyświetlenia dialogu z prośbą o uprawnienia
            }
        }
    }

    fun subscribeToTopics() {
        // Subskrybuj się na różne tematy powiadomień
        FirebaseMessaging.getInstance().subscribeToTopic("challenges")
        FirebaseMessaging.getInstance().subscribeToTopic("events")
        FirebaseMessaging.getInstance().subscribeToTopic("rewards")
    }

    fun getNotificationToken(onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                onTokenReceived(token)
            }
        }
    }
} 