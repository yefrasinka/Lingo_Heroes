package com.example.lingoheroesapp.models

data class Subtopic(
    val id: String = "",  // Default values to ensure deserialization works correctly
    val title: String = "",
    val description: String = "",
    val topicId: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val progressPercentage: Int = 0
)