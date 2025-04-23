package com.example.lingoheroesapp

import android.app.Application
import android.util.Log
import com.example.lingoheroesapp.utils.SuperPowerSynchronizer
import com.google.firebase.FirebaseApp

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
    }
} 