package com.example.lingoheroesapp.models

data class LeaderboardEntry(
    val uid: String = "",              // UID użytkownika
    val username: String = "",         // Nazwa użytkownika
    val xp: Int = 0,                   // Punkty doświadczenia
    val rank: Int = 0                  // Miejsce w rankingu
)