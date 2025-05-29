package com.example.lingoheroesapp.models


data class Progress(
    val userId: String = "",
    val topicId: String = "",
    val subtopicId: String = "",
    val lastCompletedTaskIndex: Int = -1,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0
)