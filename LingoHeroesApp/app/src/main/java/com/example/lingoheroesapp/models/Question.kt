package com.example.lingoheroesapp.models

data class Question(
    val question: String = "",
    val correctAnswer: String = "",
    val incorrectAnswers: List<String> = listOf(),
    val difficulty: Int = 1,
    val category: String = "general"
) {
    val answers: List<String>
        get() = listOf(correctAnswer) + incorrectAnswers

    val correctAnswerIndex: Int
        get() = 0

    constructor() : this("", "", listOf())
} 