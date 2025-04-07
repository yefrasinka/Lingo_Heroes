package com.example.lingoheroesapp.models

data class Enemy(
    var id: String = "",
    var name: String = "",
    var element: ElementType = ElementType.FIRE,
    var imageResId: Int = 0,
    var hp: Int = 100,
    var attack: Int = 10,
    var defense: Int = 5,
    var description: String = "",
    var stageId: Int = 1
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