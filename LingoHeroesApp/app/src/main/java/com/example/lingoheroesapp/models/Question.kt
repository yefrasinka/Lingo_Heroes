package com.example.lingoheroesapp.models

data class Question(
    var id: String = "",
    val question: String = "",
    val correctAnswer: String = "",
    val incorrectAnswers: List<String> = listOf(),
    val difficulty: Int = 1,
    val category: String = "general",
    val imageUrl: String = "",  // URL do obrazka związanego z pytaniem (jeśli istnieje)
    val stageId: Int = 0,       // ID etapu, do którego przypisane jest pytanie
    val enemyId: String = ""    // ID przeciwnika powiązanego z pytaniem
) {
    val answers: List<String>
        get() = listOf(correctAnswer) + incorrectAnswers

    val correctAnswerIndex: Int
        get() = 0

    constructor() : this("", "", "", listOf())
} 