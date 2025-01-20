package com.example.lingoheroesapp.models

data class Topic(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val subtopics: List<Subtopic> = listOf()// Lista podtematów
) {
    // Obliczanie ogólnego postępu w temacie na podstawie postępu w subtematach
    fun getProgressPercentage(): Int {
        val completedSubtopics = subtopics.count { it.completedTasks == it.totalTasks }
        return if (subtopics.isNotEmpty()) {
            (completedSubtopics.toFloat() / subtopics.size * 100).toInt()
        } else 0
    }
}
