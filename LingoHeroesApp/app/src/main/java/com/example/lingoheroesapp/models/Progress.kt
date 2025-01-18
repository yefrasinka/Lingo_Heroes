package com.example.lingoheroesapp.models

data class Progress(
    val uid: String = "",              // UID użytkownika
    val taskId: String = "",           // ID zadania
    val status: String = "",           // Status zadania (started, completed)
    val score: Int = 0,                // Wynik zadania
    val completedAt: Long = 0L         // Data ukończenia (timestamp)
)