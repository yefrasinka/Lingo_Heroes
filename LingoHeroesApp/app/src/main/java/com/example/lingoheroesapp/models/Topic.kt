package com.example.lingoheroesapp.models

data class Topic(
    var id: String = "",
    val title: String = "",
    val level: Int = 0,
    val totalSubtopic: Int = 0,
    val subtopics: List<Subtopic> = listOf()
) {
    // Dodajemy metodę do debugowania
    override fun toString(): String {
        return "Topic(id='$id', title='$title', level=$level, subtopicsCount=${subtopics.size})"
    }
}