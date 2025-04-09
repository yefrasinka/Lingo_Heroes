package com.example.lingoheroesapp.models

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Klasa reprezentująca postać gracza w pojedynku
 */
@IgnoreExtraProperties
data class DuelBattleCharacter(
    val id: String = "",
    val name: String = "",
    val element: ElementType = ElementType.FIRE,
    val imageResId: Int = 0,
    val baseAttack: Int = 12,
    val baseDefense: Int = 8,
    val specialAbilityName: String = "",
    val specialAbilityDescription: String = "",
    val specialAbilityCooldown: Int = 3,
    val hp: Int = 100,
    val defense: Int = 0
) {
    // Konstruktor bezargumentowy wymagany przez Firebase
    constructor() : this(
        id = "",
        name = "",
        element = ElementType.FIRE,
        imageResId = 0,
        baseAttack = 12,
        baseDefense = 8,
        specialAbilityName = "",
        specialAbilityDescription = "",
        specialAbilityCooldown = 3,
        hp = 100,
        defense = 0
    )
} 