package com.example.lingoheroesapp.models

/**
 * Model reprezentujący przedmiot, który można wylosować ze skrzynki
 */
data class CaseItem(
    val id: String = "",                    // Unikalny identyfikator przedmiotu
    val name: String = "",                  // Nazwa przedmiotu
    val description: String = "",           // Opis przedmiotu
    val imageUrl: String = "",              // URL obrazka przedmiotu
    val type: CaseItemType = CaseItemType.COIN, // Typ przedmiotu
    val rarity: ItemRarity = ItemRarity.COMMON, // Rzadkość przedmiotu
    val value: Int = 0,                    // Wartość przedmiotu (monety, obrażenia, obrona itp.)
    val dropChance: Double = 0.0,          // Szansa wylosowania w procentach (0-100)
    val armorTier: String = ""             // Poziom zbroi (BRONZE, SILVER, GOLD) - tylko dla ARMOR_TIER
)

/**
 * Enum definiujący typ przedmiotu ze skrzynki
 */
enum class CaseItemType {
    COIN,        // Monety
    ARMOR,       // Zbroja z bezpośrednim bonusem
    WEAPON,      // Broń
    SPECIAL,     // Specjalny przedmiot
    ARMOR_TIER   // Zbroja określonego poziomu (brązowa, srebrna, złota)
}

/**
 * Enum definiujący rzadkość przedmiotu ze skrzynki
 */
enum class ItemRarity {
    COMMON,     // Pospolity
    UNCOMMON,   // Niepospolity
    RARE,       // Rzadki
    EPIC,       // Epicki
    LEGENDARY   // Legendarny
} 