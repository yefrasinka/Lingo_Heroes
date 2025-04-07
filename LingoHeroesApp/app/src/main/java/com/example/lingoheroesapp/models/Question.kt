package com.example.lingoheroesapp.models

data class Question(
    val text: String = "",
    val correctAnswer: String = "",
    val incorrectAnswers: List<String> = listOf()
) {
    constructor() : this("", "", listOf())
} 