package com.example.lingoheroesapp.models

data class Task(
    val taskId: String = "",
    val description: String = "",
    val type: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val rewardXp: Int = 0,
    val rewardCoins: Int = 0,
    val mediaUrl: String? = "",
    val isCompleted: Boolean = false
)
