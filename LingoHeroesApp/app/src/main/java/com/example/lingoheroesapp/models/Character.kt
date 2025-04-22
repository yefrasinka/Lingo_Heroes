package com.example.lingoheroesapp.models

import com.google.firebase.database.PropertyName

data class Character(
    val id: String = "",
    val name: String = "",
    val element: ElementType = ElementType.FIRE,
    val imageResId: Int = 0,
    val baseAttack: Int = 10,
    val baseDefense: Int = 5,
    val specialAbilityName: String = "",
    val specialAbilityDescription: String = "",
    val specialAbilityCooldown: Int = 3,
    @get:PropertyName("hp")
    @set:PropertyName("hp")
    var hp: Int = 100,
    @get:PropertyName("defense")
    @set:PropertyName("defense")
    var defense: Int = 5
) {
    // Konstruktor bezargumentowy potrzebny dla Firebase
    constructor() : this(
        id = "",
        name = "",
        element = ElementType.FIRE,
        imageResId = 0,
        baseAttack = 10,
        baseDefense = 5,
        specialAbilityName = "",
        specialAbilityDescription = "",
        specialAbilityCooldown = 3,
        hp = 100,
        defense = 5
    )
    
    // Metoda konwertująca element do string dla Firebase
    fun getElementValue(): String {
        return element.name
    }
    
    // Metoda konwertująca string do typu elementu
    fun setElementValue(value: String) {
        // Nie możemy przypisać, bo element jest val, ale zostawiamy tę metodę
        // dla Firebase, żeby nie wyświetlał ostrzeżeń
    }
    
    companion object {
        // Metoda pomocnicza do tworzenia Character z wartością elementValue
        fun withElementValue(
            id: String = "",
            name: String = "",
            elementValue: String = "FIRE",
            imageResId: Int = 0,
            baseAttack: Int = 10,
            baseDefense: Int = 5,
            specialAbilityName: String = "",
            specialAbilityDescription: String = "",
            specialAbilityCooldown: Int = 3,
            hp: Int = 100,
            defense: Int = 5
        ): Character {
            return Character(
                id, name, ElementType.fromString(elementValue), imageResId,
                baseAttack, baseDefense, specialAbilityName, specialAbilityDescription,
                specialAbilityCooldown, hp, defense
            )
        }
    }
} 