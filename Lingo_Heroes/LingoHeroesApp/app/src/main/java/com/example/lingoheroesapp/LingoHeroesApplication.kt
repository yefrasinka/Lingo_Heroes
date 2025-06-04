package com.example.lingoheroesapp

import android.app.Application
import android.util.Log
import com.example.lingoheroesapp.utils.SuperPowerSynchronizer
import com.google.firebase.FirebaseApp
import com.example.lingoheroesapp.utils.NotificationScheduler
import com.example.lingoheroesapp.utils.AutomatedNotificationManager

class LingoHeroesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Synchronize superpowers with Firebase
        try {
            SuperPowerSynchronizer.getInstance().synchronizeSuperPowers()
            Log.d("LingoHeroesApplication", "Superpowers synchronized successfully")
        } catch (e: Exception) {
            Log.e("LingoHeroesApplication", "Error synchronizing superpowers: ${e.message}", e)
        }
        
        // Inicjalizacja menedżera automatycznych powiadomień
        try {
            AutomatedNotificationManager.init(this)
            Log.d("LingoHeroesApplication", "AutomatedNotificationManager initialized successfully")
        } catch (e: Exception) {
            Log.e("LingoHeroesApplication", "Error initializing AutomatedNotificationManager: ${e.message}", e)
        }
        
        // Schedule notifications
        try {
            NotificationScheduler.scheduleNotifications(this)
            Log.d("LingoHeroesApplication", "Notifications scheduled successfully")
        } catch (e: Exception) {
            Log.e("LingoHeroesApplication", "Error scheduling notifications: ${e.message}", e)
        }
    }
} 