package com.example.lingoheroesapp.models

data class Topic(
    val id: String = "",
    val title: String = "",
    val level: Int = 0,
    val totalSubtopic: Int = 0,
    val subtopics: List<Subtopic> = listOf()
)