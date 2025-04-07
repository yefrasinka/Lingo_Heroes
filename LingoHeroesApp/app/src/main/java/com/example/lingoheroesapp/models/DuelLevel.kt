package com.example.lingoheroesapp.models

data class DuelLevel(
    val id: String = "",
    val name: String = "",
    val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val requiredLevel: Int = 1,
    val background: String = "", // URL do tła poziomu
    val rewards: DuelRewards = DuelRewards(),
    val isLocked: Boolean = true,
    val enemies: List<DuelEnemy> = listOf(),
    val position: Position = Position()
) {
    constructor() : this("")
}

data class DuelRewards(
    val experience: Int = 0,
    val coins: Int = 0,
    val items: List<String> = listOf() // ID przedmiotów do odblokowania
) {
    constructor() : this(0)
}

data class DuelEnemy(
    val id: String = "",
    val name: String = "",
    val avatar: String = "", // URL do awatara przeciwnika
    val difficulty: String = "MEDIUM",
    val level: Int = 1,
    val questionCategories: List<String> = listOf() // Kategorie pytań dla tego przeciwnika
) {
    constructor() : this("")
}

data class Position(
    val x: Int = 0,
    val y: Int = 0,
    val isCheckpoint: Boolean = false
) {
    constructor() : this(0)
} 