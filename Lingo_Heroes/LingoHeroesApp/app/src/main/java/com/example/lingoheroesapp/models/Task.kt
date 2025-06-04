package com.example.lingoheroesapp.models

data class Task(
    val taskId: String = "",
    val type: String = "",
    val question: String = "",
    val options: List<String> = listOf(),
    val correctAnswer: String = "",
    val description: String = "",
    val rewardXp: Int = 0,
    val rewardCoins: Int = 0,
    val isCompleted: Boolean = false,
    val mediaUrl: String = ""
)
