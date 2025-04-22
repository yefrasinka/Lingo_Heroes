package com.example.lingoheroesapp.models

data class DuelQuestion(
    val id: String = "",
    val question: String = "",
    val answers: List<String> = listOf(),
    val correctAnswerIndex: Int = 0,
    val level: Int = 1,
    val category: String = "",
    val imageUrl: String = "",       // URL do obrazka związanego z pytaniem (jeśli istnieje)
    val enemyId: String = "",        // ID przeciwnika związanego z pytaniem
    val difficulty: Int = 1,         // Poziom trudności pytania (1-3)
    val usedInDuelIds: List<String> = listOf() // Lista ID pojedynków, w których to pytanie zostało użyte
) {
    constructor() : this("", "", listOf(), 0, 1, "")
    
    // Konwersja z modelu Question do DuelQuestion
    companion object {
        fun fromQuestion(question: Question): DuelQuestion {
            return DuelQuestion(
                id = question.id,
                question = question.question,
                answers = question.answers,
                correctAnswerIndex = question.correctAnswerIndex,
                level = question.difficulty,
                category = question.category,
                imageUrl = question.imageUrl,
                enemyId = question.enemyId,
                difficulty = question.difficulty
            )
        }
    }
} 