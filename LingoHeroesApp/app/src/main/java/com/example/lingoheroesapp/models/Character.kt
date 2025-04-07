package com.example.lingoheroesapp.models

data class Character(
    var id: String = "",
    var name: String = "",
    var element: ElementType = ElementType.FIRE,
    var imageResId: Int = 0,
    var baseAttack: Int = 10,
    var baseDefense: Int = 5,
    var specialAbilityName: String = "",
    var specialAbilityDescription: String = "",
    var specialAbilityCooldown: Int = 3
) {
    // Konstruktor bezargumentowy potrzebny dla Firebase
    constructor() : this(
        id = "",
        name = "",
        element = ElementType.FIRE
    )
    
    // Metoda konwertująca element z/do Firebase (jako string)
    @get:JvmName("getElementValue")
    var elementValue: String
        get() = element.name
        set(value) {
            element = ElementType.fromString(value)
        }
} 