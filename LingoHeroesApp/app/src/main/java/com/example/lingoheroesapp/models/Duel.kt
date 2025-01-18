package com.example.lingoheroesapp.models

data class Duel(
    val duelId: String = "",           // ID pojedynku
    val playerOneId: String = "",      // UID gracza 1
    val playerTwoId: String = "",      // UID gracza 2
    val status: String = "",           // Status (waiting, ongoing, finished)
    val winnerId: String? = null,      // UID zwycięzcy
    val tasks: List<String> = emptyList(), // Zadania w pojedynku
    val createdAt: Long = 0L           // Data rozpoczęcia
)