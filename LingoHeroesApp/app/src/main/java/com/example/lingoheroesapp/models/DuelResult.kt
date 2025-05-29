package com.example.lingoheroesapp.models

data class DuelResult(
    val userId: String = "",
    val isVictory: Boolean = false,
    val experienceGained: Int = 0,
    val coinsGained: Int = 0,
    val questionsAnswered: Int = 0,
    val correctAnswers: Int = 0,
    val totalDamageDealt: Int = 0,
    val totalDamageTaken: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("")
} 