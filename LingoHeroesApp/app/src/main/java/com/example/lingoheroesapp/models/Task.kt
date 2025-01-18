package com.example.lingoheroesapp.models

data class Task(
    val taskId: String = "",                // ID zadania
    val title: String = "",                 // Tytuł zadania
    val description: String = "",           // Opis zadania
    val difficulty: String = "",            // Poziom trudności (easy, medium, hard)
    val type: String = "",                  // Typ zadania (vocabulary, grammar, reading, listening)
    val question: String = "",              // Pytanie
    val options: List<String> = emptyList(),// Opcje odpowiedzi
    val correctAnswer: String = "",         // Poprawna odpowiedź
    val rewardXp: Int = 0,                  // Nagroda XP
    val rewardCoins: Int = 0,              // Nagroda monety
    val topicId: String = "",              // ID powiązanego tematu
    val subtopicId: String = "",           // ID powiązanego podtematu
    val mediaUrl: String? = null           // Link do multimediów (audio, video) jeśli dotyczy
)