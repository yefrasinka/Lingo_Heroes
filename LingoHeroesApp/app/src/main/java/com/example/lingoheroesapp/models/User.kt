package com.example.lingoheroesapp.models

import com.google.firebase.database.PropertyName

data class User(
    val uid: String = "",                 // UID z Firebase Authentication
    val username: String = "",            // Nazwa użytkownika
    val email: String="",
    val level: Int = 1,                   // Poziom użytkownika
    val xp: Int = 0,                      // Punkty doświadczenia
    val coins: Int = 0,                   // Waluta w grze
    val purchasedItems: List<String> = emptyList(),
    val topicsProgress: Map<String, TopicProgress> = mapOf(),  // Postęp w tematach
    val achievements: Map<String, Achievement> = mapOf(),      // Osiągnięcia użytkownika
    val streakDays: Int = 0,              // Seria dni nauki
    val perfectScores: Int = 0,           // Liczba idealnych wyników
    val tasksCompleted: Int = 0,          // Liczba ukończonych zadań
    val completedChallenges: Int = 0,     // Liczba ukończonych wyzwań
    val equipment: Equipment = Equipment(), // Ekwipunek postaci gracza
    
    // Pola dla systemu wyzwań
    val challenges: Map<String, Challenge> = mapOf(),  // Wyzwania użytkownika
    val lastDayTasksCount: Int = 0,       // Liczba zadań z ostatniego dnia
    val lastDayTimestamp: Long = 0,       // Timestamp ostatniego dnia
    val todaysPerfectTasks: Int = 0,      // Liczba perfekcyjnych zadań dzisiaj
    val todaysTotalTasks: Int = 0,        // Całkowita liczba zadań dzisiaj
    val lastPerfectDay: Long = 0          // Timestamp ostatniego perfekcyjnego dnia
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
        equipment = Equipment(),
        challenges = mapOf(),
        lastDayTasksCount = 0,
        lastDayTimestamp = 0,
        todaysPerfectTasks = 0,
        todaysTotalTasks = 0,
        lastPerfectDay = 0
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
