package com.example.lingoheroesapp.models

import kotlin.random.Random

class AIPlayer(private val difficulty: String = "MEDIUM") {
    
    private val correctAnswerChance = when (difficulty) {
        "EASY" -> 0.5
        "MEDIUM" -> 0.75
        "HARD" -> 0.9
        else -> 0.75
    }
    
    private val minAnswerTime = 1000L // 1 sekunda
    private val maxAnswerTime = when (difficulty) {
        "EASY" -> 4000L
        "MEDIUM" -> 3000L
        "HARD" -> 2000L
        else -> 3000L
    }
    
    fun willAnswerCorrectly(): Boolean {
        return Random.nextDouble() < correctAnswerChance
    }
    
    fun getAnswerDelay(): Long {
        return Random.nextLong(minAnswerTime, maxAnswerTime)
    }
    
    fun calculateDamage(answerTimeMs: Long): Int {
        val baseDamage = when (difficulty) {
            "EASY" -> 15
            "MEDIUM" -> 20
            "HARD" -> 25
            else -> 20
        }
        
        // Bonus za szybkość odpowiedzi
        val timeBonus = ((maxAnswerTime - answerTimeMs).toFloat() / maxAnswerTime * 10).toInt()
        return baseDamage + timeBonus
    }
} 