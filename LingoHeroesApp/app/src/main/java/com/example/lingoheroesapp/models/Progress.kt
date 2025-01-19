package com.example.lingoheroesapp.models

data class Progress(
    val uid: String = "",              // UID użytkownika
    val taskId: String = "",           // ID zadania
    val status: Status = Status.STARTED, // Status zadania (started, completed)
    val score: Int = 0,                // Wynik zadania
    val completedAt: Long? = null,     // Data ukończenia (timestamp)
    val startedAt: Long? = null        // Data rozpoczęcia (timestamp)
) {
    // Status zadania
    enum class Status {
        STARTED, COMPLETED, IN_PROGRESS
    }

    // Obliczanie postępu jako procent
    fun getProgressPercentage(totalScore: Int): Int {
        return if (totalScore > 0) {
            (score.toFloat() / totalScore * 100).toInt()
        } else 0
    }

    // Sprawdzenie, czy zadanie jest ukończone
    fun isCompleted(): Boolean {
        return status == Status.COMPLETED
    }
}
