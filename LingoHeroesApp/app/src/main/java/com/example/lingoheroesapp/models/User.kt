package com.example.lingoheroesapp.models

data class User(
    val uid: String = "",                 // UID z Firebase Authentication
    val username: String = "",            // Nazwa użytkownika
    val email: String="",
    val level: Int = 1,                   // Poziom użytkownika
    val xp: Int = 0,                      // Punkty doświadczenia
    val coins: Int = 0,                   // Waluta w grze
    val purchasedItems: List<String> = emptyList(),
    val topicsProgress: Map<String, TopicProgress> = mapOf()  // Postęp w tematach
)

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
