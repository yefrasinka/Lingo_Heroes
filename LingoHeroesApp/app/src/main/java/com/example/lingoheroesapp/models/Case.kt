package com.example.lingoheroesapp.models

/**
 * Model reprezentujący skrzynkę, którą można kupić w sklepie
 */
data class Case(
    val id: String = "",                  // Unikalny identyfikator skrzynki
    val name: String = "",                // Nazwa skrzynki
    val description: String = "",         // Opis skrzynki
    val imageUrl: String = "",            // URL obrazka skrzynki
    val price: Int = 0,                   // Cena skrzynki w monetach
    val items: List<CaseItem> = emptyList(), // Lista możliwych przedmiotów
    val rarity: CaseRarity = CaseRarity.STANDARD // Rzadkość skrzynki
)

/**
 * Enum definiujący rzadkość skrzynek
 */
enum class CaseRarity {
    STANDARD,    // Standardowa
    PREMIUM,     // Premium
    ELITE        // Elitarna
} 