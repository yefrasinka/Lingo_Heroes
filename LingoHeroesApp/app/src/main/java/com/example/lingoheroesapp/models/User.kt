package com.example.lingoheroesapp.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class User(
    var uid: String = "",                 // UID z Firebase Authentication
    val username: String = "",            // Nazwa użytkownika
    val email: String="",
    val level: Int = 1,                   // Poziom użytkownika
    val xp: Int = 0,                      // Punkty doświadczenia
    val coins: Int = 0,                   // Waluta w grze
    val purchasedItems: List<String> = emptyList(),
    @PropertyName("topicsProgress") val topicsProgress: Map<String, TopicProgress> = mapOf(),  // Postęp w tematach
    @PropertyName("achievements") val achievements: Map<String, Achievement> = mapOf(),      // Osiągnięcia użytkownika
    val streakDays: Int = 0,              // Seria dni nauki
    val perfectScores: Int = 0,           // Liczba idealnych wyników
    val tasksCompleted: Int = 0,          // Liczba ukończonych zadań
    
    // Liczniki związane z wyzwaniami
    val completedChallenges: Int = 0,     // Łączna liczba ukończonych wyzwań (przestarzałe)
    val challengesCompleted: Int = 0,     // Nowe pole - łączna liczba ukończonych wyzwań
    val dailyChallengesCompleted: Int = 0, // Liczba ukończonych wyzwań dziennych
    val weeklyChallengesCompleted: Int = 0, // Liczba ukończonych wyzwań tygodniowych
    
    val equipment: Equipment = Equipment(), // Ekwipunek postaci gracza
    
    // Pola dla systemu wyzwań
    @PropertyName("challenges") val challenges: Map<String, Challenge> = mapOf(),  // Wyzwania użytkownika
    val lastDayTasksCount: Int = 0,       // Liczba zadań z ostatniego dnia
    val lastDayTimestamp: Long = 0,       // Timestamp ostatniego dnia
    val lastActiveDay: Long = 0,          // Timestamp ostatniego dnia aktywności (dla serii)
    val todaysPerfectTasks: Int = 0,      // Liczba perfekcyjnych zadań dzisiaj
    val todaysTotalTasks: Int = 0,        // Całkowita liczba zadań dzisiaj
    val lastPerfectDay: Long = 0,         // Timestamp ostatniego perfekcyjnego dnia
    val character: Character? = null,
    @PropertyName("stagesCompleted") val stagesCompleted: Map<String, Boolean> = mapOf(),
    @PropertyName("stageStars") val stageStars: Map<String, Int> = mapOf(),
    @PropertyName("inventory") val inventory: Map<String, Int> = mapOf(),
    
    // Pola dla systemu pojedynków
    val duelsCompleted: Int = 0,          // Liczba ukończonych pojedynków
    val bossesDefeated: Int = 0,          // Liczba pokonanych bossów
    
    // Pole dla znajomych
    @PropertyName("friends") val friends: Map<String, Boolean> = mapOf() // ID znajomych (wartość true oznacza zaakceptowane zaproszenie)
) {
    // Konstruktor bezargumentowy wymagany przez Firebase
    constructor() : this(
        uid = "",
        username = "",
        email = "",
        level = 1,
        xp = 0,
        coins = 0,
        purchasedItems = emptyList(),
        topicsProgress = mapOf(),
        achievements = mapOf(),
        streakDays = 0,
        perfectScores = 0,
        tasksCompleted = 0,
        completedChallenges = 0,
        challengesCompleted = 0,
        dailyChallengesCompleted = 0,
        weeklyChallengesCompleted = 0,
        equipment = Equipment(),
        challenges = mapOf(),
        lastDayTasksCount = 0,
        lastDayTimestamp = 0,
        lastActiveDay = 0,
        todaysPerfectTasks = 0,
        todaysTotalTasks = 0,
        lastPerfectDay = 0,
        character = null,
        stagesCompleted = mapOf(),
        stageStars = mapOf(),
        inventory = mapOf(),
        duelsCompleted = 0,
        bossesDefeated = 0,
        friends = mapOf()
    )
}

data class TopicProgress(
    val completedSubtopics: Int = 0,      // Liczba ukończonych podtematów
    val totalSubtopics: Int = 0,          // Łączna liczba podtematów
    val completedTasks: Int = 0,          // Liczba ukończonych zadań
    val totalTasks: Int = 0,              // Łączna liczba zadań
    val subtopics: Map<String, SubtopicProgress> = mapOf()  // Postęp w podtematach
) {
    constructor() : this(0, 0, 0, 0, mapOf())
}

data class SubtopicProgress(
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val title: String = ""
) {
    constructor() : this(0, 0, "")
}
