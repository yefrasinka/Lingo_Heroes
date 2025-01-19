package com.example.lingoheroesapp.models

data class User(
    val uid: String = "",                // UID z Firebase Authentication
    val username: String = "",           // Nazwa użytkownika
    val email: String = "",              // E-mail
    val level: Int = 1,                  // Poziom użytkownika
    val xp: Int = 0,                     // Punkty doświadczenia
    val coins: Int = 0,                  // Waluta w grze
    val completedTasks: List<String> = emptyList(),  // Lista wykonanych zadań
    val purchasedItems: List<String> = emptyList(),  // Lista kupionych przedmiotów
    val topicsProgress: Map<String, TopicProgress> = emptyMap()  // Postęp w tematach
)

// Postęp w temacie
data class TopicProgress(
    val topicId: String = "",            // ID tematu
    val completedSubtopics: Int = 0,     // Liczba ukończonych podtematów
    val totalSubtopics: Int = 0,         // Łączna liczba podtematów
    val progressPercentage: Int = 0,     // Procentowy postęp w temacie
    val completedTasks: List<String> = emptyList(),  // Lista ukończonych zadań
    val totalTasks: Int = 0             // Łączna liczba zadań w temacie
) {
    // Obliczanie procentowego postępu na podstawie ukończonych zadań
    fun getTasksProgressPercentage(): Int {
        return if (totalTasks > 0) {
            (completedTasks.size.toFloat() / totalTasks * 100).toInt()
        } else 0
    }
}
