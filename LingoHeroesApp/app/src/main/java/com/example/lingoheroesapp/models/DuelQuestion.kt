package com.example.lingoheroesapp.models

data class DuelQuestion(
    val id: String = "",
    val question: String = "",
    val answers: List<String> = listOf(),
    val correctAnswerIndex: Int = 0,
    val level: Int = 1,
    val category: String = ""
) {
    constructor() : this("", "", listOf(), 0, 1, "")
} 