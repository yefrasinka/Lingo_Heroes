package com.example.lingoheroesapp.models

data class Subtopic(
    val id: String = "",
    val topicId: String = "",
    val title: String = "",
    val description: String = "",
    val totalTasks: Int = 0,           // Łączna liczba zadań w podtemacie
    val completedTasks: Int = 0        // Liczba ukończonych zadań
) {
    // Obliczanie procentowego postępu w podtemacie
    fun getProgressPercentage(): Int {
        return if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks * 100).toInt()
        } else 0
    }
}
