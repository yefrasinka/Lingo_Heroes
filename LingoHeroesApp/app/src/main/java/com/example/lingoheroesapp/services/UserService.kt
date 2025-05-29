package com.example.lingoheroesapp.services

import android.util.Log
import com.example.lingoheroesapp.models.Achievement
import com.example.lingoheroesapp.models.AchievementType
import com.example.lingoheroesapp.models.User
import com.example.lingoheroesapp.utils.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

object UserService {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private const val TAG = "UserService"
    
    /**
     * Pobiera dane użytkownika na podstawie ID
     */
    fun getUserById(userId: String, callback: (User?, String?) -> Unit) {
        DatabaseHelper.safelyGetUser(database.child("users").child(userId)) { user ->
            if (user != null) {
                callback(user, null)
            } else {
                callback(null, "Nie udało się pobrać danych użytkownika")
            }
        }
    }
    
    /**
     * Pobiera osiągnięcia użytkownika
     */
    fun getUserAchievements(userId: String, callback: (List<Achievement>) -> Unit) {
        // Użyj bezpiecznej metody z DatabaseHelper
        DatabaseHelper.safelyGetUserAchievements(database, userId, callback)
    }
} 