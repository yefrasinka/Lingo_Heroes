package com.example.lingoheroesapp.models

import com.google.firebase.database.IgnoreExtraProperties
import java.util.UUID

@IgnoreExtraProperties
data class Duel(
    val duelId: String = "",               // ID pojedynku
    val playerOneId: String = "",          // UID gracza 1
    val playerTwoId: String = "",          // UID gracza 2 (może być puste dla pojedynków z przeciwnikiem AI)
    val status: String = "waiting",        // Status (waiting, ongoing, finished)
    val winnerId: String? = null,          // UID zwycięzcy
    val questions: List<DuelQuestion> = emptyList(), // Pytania w pojedynku
    val enemyId: String = "",              // ID przeciwnika
    val enemyData: Enemy? = null,          // Dane przeciwnika
    val createdAt: Long = System.currentTimeMillis(),  // Data rozpoczęcia
    val lastUpdatedAt: Long = System.currentTimeMillis(), // Data ostatniej aktualizacji
    val difficulty: Int = 1,               // Poziom trudności pojedynku (1-3)
    val maxQuestions: Int = 10,            // Maksymalna liczba pytań w pojedynku
    val currentQuestionIndex: Int = 0,     // Indeks aktualnego pytania
    val playerHealth: Int = 100,           // Aktualne zdrowie gracza
    val enemyHealth: Int = 100,            // Aktualne zdrowie przeciwnika
    val correctAnswers: Int = 0,           // Liczba poprawnych odpowiedzi
    val incorrectAnswers: Int = 0,         // Liczba niepoprawnych odpowiedzi
    val totalDamageDealt: Int = 0,         // Całkowite obrażenia zadane
    val totalDamageTaken: Int = 0,         // Całkowite obrażenia otrzymane
    val playerEquipmentId: String = "",    // ID ekwipunku gracza
    val activeSuperPowers: List<ActiveSuperPower> = emptyList() // Aktywne supermoce
) {
    // Konstruktor bez argumentów wymagany przez Firebase
    constructor() : this(
        duelId = UUID.randomUUID().toString(),
        playerOneId = "",
        status = "waiting"
    )
    
    // Sprawdź, czy pojedynek się zakończył
    fun isFinished(): Boolean {
        return status == "finished" || currentQuestionIndex >= maxQuestions || 
            playerHealth <= 0 || enemyHealth <= 0
    }
    
    // Sprawdź, czy gracz wygrał
    fun isPlayerWinner(): Boolean {
        return isFinished() && (enemyHealth <= 0 || playerHealth > 0 && currentQuestionIndex >= maxQuestions)
    }
    
    // Sprawdź, czy gracz przegrał
    fun isPlayerLost(): Boolean {
        return isFinished() && playerHealth <= 0
    }
    
    // Sprawdź, czy pojedynek jest w trakcie
    fun isOngoing(): Boolean {
        return status == "ongoing"
    }
    
    // Sprawdź, czy pojedynek jest w oczekiwaniu
    fun isWaiting(): Boolean {
        return status == "waiting"
    }
    
    // Oblicz wynik końcowy gracza
    fun calculatePlayerScore(): Int {
        return correctAnswers * 100 - incorrectAnswers * 50 + totalDamageDealt - totalDamageTaken
    }
    
    // Pobierz aktualnie dostępne supermoce
    fun getAvailableSuperPowers(): List<SuperPower> {
        // Pobierz supermoce w zależności od liczby poprawnych odpowiedzi
        return when {
            correctAnswers >= 10 -> SuperPower.getHardSuperPowers() // Trudne supermoce po 10 poprawnych odpowiedziach
            correctAnswers >= 5 -> SuperPower.getMediumSuperPowers() // Średnie supermoce po 5 poprawnych odpowiedziach
            correctAnswers >= 3 -> SuperPower.getEasySuperPowers() // Łatwe supermoce po 3 poprawnych odpowiedziach
            else -> emptyList() // Brak dostępnych supermocy
        }
    }
    
    // Sprawdź, czy gracz może użyć supermocy
    fun canUseSuperPower(): Boolean {
        return correctAnswers >= 3 // Wymaga minimum 3 poprawnych odpowiedzi
    }
    
    // Aktualnie zadawane pytanie
    val currentQuestion: DuelQuestion?
        get() = if (currentQuestionIndex < questions.size) questions[currentQuestionIndex] else null
    
    companion object {
        // Utwórz nowy pojedynek
        fun createNewDuel(
            playerId: String,
            enemyId: String,
            enemyData: Enemy,
            questions: List<DuelQuestion>,
            difficulty: Int = 1,
            maxQuestions: Int = 10
        ): Duel {
            return Duel(
                duelId = UUID.randomUUID().toString(),
                playerOneId = playerId,
                status = "waiting",
                questions = questions,
                enemyId = enemyId,
                enemyData = enemyData,
                createdAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis(),
                difficulty = difficulty,
                maxQuestions = maxQuestions,
                enemyHealth = enemyData.hp
            )
        }
    }
}

// Klasa do przechowywania informacji o aktywnych supermocach
@IgnoreExtraProperties
data class ActiveSuperPower(
    val superPowerId: String = "",          // ID supermocy
    var remainingDuration: Int = 0,         // Pozostała liczba tur działania
    val appliedAt: Long = System.currentTimeMillis(), // Czas zastosowania
    val superPowerData: SuperPower? = null  // Dane supermocy
) {
    // Konstruktor bezargumentowy wymagany przez Firebase
    constructor() : this(
        superPowerId = "",
        remainingDuration = 0,
        appliedAt = System.currentTimeMillis()
    )
}