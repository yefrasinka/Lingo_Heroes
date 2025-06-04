package com.example.lingoheroesapp.models

data class Topic(
    var id: String = "",
    val title: String = "",
    val level: Int = 1,
    val subtopics: List<Subtopic> = listOf(),
    val progressPercentage: Int = 0
) {
    // Dodajemy metodę do debugowania
    override fun toString(): String {
        return "Topic(id='$id', title='$title', level=$level, subtopicsCount=${subtopics.size})"
    }
}