package com.example.lingoheroesapp.models

data class Subtopic(
    val id: String = "",
    val title: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val tasks: List<Task> = listOf()
)
