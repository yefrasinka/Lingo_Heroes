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
    val imageResId: Int = 0,  // Pozostawione dla wstecznej kompatybilności
    val imageUrl: String = "", // URL do obrazka przeciwnika
    val attack: Int = 10,
    val defense: Int = 5,
    val description: String = "",
    val stageId: Int = 1,
    val hp: Int = 100,
    val baseAttack: Int = 10,
    val weaknesses: List<ElementType> = listOf(),  // Słabości przeciwnika
    val resistances: List<ElementType> = listOf()  // Odporności przeciwnika
) {
    // Konstruktor bezargumentowy wymagany przez Firebase
    constructor() : this(
        id = "",
        name = "",
        element = ElementType.FIRE,
        imageResId = 0,
        imageUrl = "",
        attack = 10,
        defense = 5,
        description = "",
        stageId = 1,
        hp = 100,
        baseAttack = 10,
        weaknesses = listOf(),
        resistances = listOf()
    )
    
    // Metoda do sprawdzania skuteczności ataku w zależności od elementu
    fun getElementEffectiveness(attackerElement: ElementType): Double {
        return when {
            weaknesses.contains(attackerElement) -> 1.5  // Super efektywny
            resistances.contains(attackerElement) -> 0.5 // Mało efektywny
            else -> 1.0  // Normalny
        }
    }
} 