package com.example.lingoheroesapp.models

data class DuelReport(
    val stageNumber: Int,
    val isCompleted: Boolean,
    val correctAnswers: Int,
    val totalAnswers: Int,
    val timeSpent: Long,
    val xpGained: Int,
    val coinsGained: Int,
    val stars: Int,
    val mistakes: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) 