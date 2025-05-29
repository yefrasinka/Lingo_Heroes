package com.example.lingoheroesapp.models

data class Enemy(
    var id: String = "",
    var name: String = "",
    var element: ElementType = ElementType.FIRE,
    var imageUrl: String = "",     // URL do obrazka przeciwnika (zamiast imageResId)
    var hp: Int = 100,
    var attack: Int = 10,
    var defense: Int = 5,
    var description: String = "",
    var stageId: Int = 1,
    var weaknesses: List<ElementType> = listOf(),  // Słabości przeciwnika (rodzaje elementów)
    var resistances: List<ElementType> = listOf(), // Odporności przeciwnika
    var associatedQuestionIds: List<String> = listOf() // Pytania przypisane do tego przeciwnika
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
        
    // Metoda do sprawdzania skuteczności ataku w zależności od elementu
    fun getElementEffectiveness(attackerElement: ElementType): Double {
        return when {
            weaknesses.contains(attackerElement) -> 1.5  // Super efektywny
            resistances.contains(attackerElement) -> 0.5 // Mało efektywny
            else -> 1.0  // Normalny
        }
    }
    
    // Konwersja do DuelBattleEnemy
    fun toDuelBattleEnemy(): DuelBattleEnemy {
        return DuelBattleEnemy(
            id = this.id,
            name = this.name,
            element = this.element,
            imageResId = 0, // Nie używamy już imageResId
            attack = this.attack,
            defense = this.defense,
            description = this.description,
            stageId = this.stageId,
            hp = this.hp,
            baseAttack = this.attack,
            imageUrl = this.imageUrl
        )
    }
} 