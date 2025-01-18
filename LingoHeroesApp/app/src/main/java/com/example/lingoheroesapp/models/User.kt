package com.example.lingoheroesapp.models

data class User(
    val uid: String = "",             // UID z Firebase Authentication
    val username: String = "",        // Nazwa użytkownika
    val email: String = "",           // E-mail
    val level: Int = 1,               // Poziom użytkownika
    val xp: Int = 0,                  // Punkty doświadczenia
    val coins: Int = 0,               // Waluta w grze
    val completedTasks: List<String> = emptyList(),  // Lista wykonanych zadań
    val purchasedItems: List<String> = emptyList()   // Lista kupionych przedmiotów
)