package com.example.lingoheroesapp.models

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Klasa reprezentująca przeciwnika w pojedynku
 */
@IgnoreExtraProperties
data class DuelBattleEnemy(
    val id: String = "",
    val name: String = "",
    val element: ElementType = ElementType.FIRE,
    val imageResId: Int = 0,
    val attack: Int = 10,
    val defense: Int = 5,
    val description: String = "",
    val stageId: Int = 1,
    val hp: Int = 100,
    val baseAttack: Int = 10
) {
    // Konstruktor bezargumentowy wymagany przez Firebase
    constructor() : this(
        id = "",
        name = "",
        element = ElementType.FIRE,
        imageResId = 0,
        attack = 10,
        defense = 5,
        description = "",
        stageId = 1,
        hp = 100,
        baseAttack = 10
    )
} 