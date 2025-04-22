package com.example.lingoheroesapp.models

import com.google.firebase.database.IgnoreExtraProperties

enum class SuperPowerDifficulty {
    EASY,      // Łatwe (słabe supermocy)
    MEDIUM,    // Średnie supermocy
    HARD       // Trudne (mocne supermocy)
}

@IgnoreExtraProperties
data class SuperPower(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",                           // URL do ikony supermocy
    val difficulty: SuperPowerDifficulty = SuperPowerDifficulty.EASY,  // Poziom trudności
    val duration: Int = 1,                              // Liczba tur działania (1 = jednorazowy efekt)
    val cooldown: Int = 3,                              // Liczba tur oczekiwania przed ponownym użyciem
    
    // Parametry efektu
    val effectType: String = "",                        // Typ efektu (damage, healing, defense, etc.)
    val effectValue: Double = 0.0,                      // Wartość efektu (procent lub stała wartość)
    val isPercentage: Boolean = true                    // Czy wartość efektu jest procentowa
) {
    // Konstruktor bezargumentowy wymagany przez Firebase
    constructor() : this(
        id = "",
        name = "",
        description = "",
        iconUrl = "",
        difficulty = SuperPowerDifficulty.EASY,
        duration = 1,
        cooldown = 3,
        effectType = "",
        effectValue = 0.0,
        isPercentage = true
    )
    
    companion object {
        // Predefiniowane supermoce - słabe (łatwe)
        val DODGE = SuperPower(
            id = "dodge",
            name = "Unik",
            description = "Następny atak potwora na 100% nie trafi.",
            iconUrl = "dodge_icon",
            difficulty = SuperPowerDifficulty.EASY,
            duration = 1,
            cooldown = 3,
            effectType = "dodge",
            effectValue = 100.0,
            isPercentage = true
        )
        
        val SPARK = SuperPower(
            id = "spark",
            name = "Iskra",
            description = "Natychmiast zadaje dodatkowe 10% podstawowych obrażeń.",
            iconUrl = "spark_icon",
            difficulty = SuperPowerDifficulty.EASY,
            duration = 1,
            cooldown = 3,
            effectType = "damage",
            effectValue = 10.0,
            isPercentage = true
        )
        
        val SMALL_HEAL = SuperPower(
            id = "small_heal",
            name = "Małe leczenie",
            description = "Przywraca 15% maksymalnego HP.",
            iconUrl = "small_heal_icon",
            difficulty = SuperPowerDifficulty.EASY,
            duration = 1,
            cooldown = 3,
            effectType = "healing",
            effectValue = 15.0,
            isPercentage = true
        )
        
        val ENERGY_SHIELD = SuperPower(
            id = "energy_shield",
            name = "Tarcza energetyczna",
            description = "Następne otrzymane obrażenia są zmniejszone o 50%.",
            iconUrl = "energy_shield_icon",
            difficulty = SuperPowerDifficulty.EASY,
            duration = 1,
            cooldown = 3,
            effectType = "damage_reduction",
            effectValue = 50.0,
            isPercentage = true
        )
        
        val MINI_CRIT = SuperPower(
            id = "mini_crit",
            name = "Mini-kryt",
            description = "Następny atak zadaje +50% obrażeń.",
            iconUrl = "mini_crit_icon",
            difficulty = SuperPowerDifficulty.EASY,
            duration = 1,
            cooldown = 3,
            effectType = "damage_boost",
            effectValue = 50.0,
            isPercentage = true
        )
        
        val DISTRACTION = SuperPower(
            id = "distraction",
            name = "Rozproszenie",
            description = "Siła następnego ataku potwora zmniejszona o 20%.",
            iconUrl = "distraction_icon",
            difficulty = SuperPowerDifficulty.EASY,
            duration = 1,
            cooldown = 3,
            effectType = "enemy_attack_reduction",
            effectValue = 20.0,
            isPercentage = true
        )
        
        val RESISTANCE = SuperPower(
            id = "resistance",
            name = "Odporność",
            description = "Wszystkie otrzymywane obrażenia są zmniejszone o 25% przez 2 tury.",
            iconUrl = "resistance_icon",
            difficulty = SuperPowerDifficulty.EASY,
            duration = 2,
            cooldown = 3,
            effectType = "damage_reduction",
            effectValue = 25.0,
            isPercentage = true
        )
        
        // Predefiniowane supermoce - średnie
        val DOUBLE_STRIKE = SuperPower(
            id = "double_strike",
            name = "Podwójne uderzenie",
            description = "Bohater od razu wykonuje podwójny atak.",
            iconUrl = "double_strike_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 1,
            cooldown = 4,
            effectType = "double_attack",
            effectValue = 100.0,
            isPercentage = true
        )
        
        val VAMPIRISM = SuperPower(
            id = "vampirism",
            name = "Wampiryzm",
            description = "Odzyskuje 50% zadanych obrażeń jako HP.",
            iconUrl = "vampirism_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 1,
            cooldown = 4,
            effectType = "life_steal",
            effectValue = 50.0,
            isPercentage = true
        )
        
        val CRITICAL_HIT = SuperPower(
            id = "critical_hit",
            name = "Cios krytyczny",
            description = "Następny atak zadaje +100% obrażeń.",
            iconUrl = "critical_hit_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 1,
            cooldown = 4,
            effectType = "damage_boost",
            effectValue = 100.0,
            isPercentage = true
        )
        
        // Predefiniowane supermoce - mocne
        val FIRE_STORM = SuperPower(
            id = "fire_storm",
            name = "Ognista burza",
            description = "Podpala potwora na 3 tury, co turę zadając 20% obrażeń.",
            iconUrl = "fire_storm_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 3,
            cooldown = 5,
            effectType = "damage_over_time",
            effectValue = 20.0,
            isPercentage = true
        )
        
        val FURY_PARALYSIS = SuperPower(
            id = "fury_paralysis",
            name = "Paraliż furii",
            description = "Potwór nie może atakować przez 2 pełne tury.",
            iconUrl = "fury_paralysis_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 2,
            cooldown = 5,
            effectType = "stun",
            effectValue = 100.0,
            isPercentage = true
        )
        
        // Pobierz wszystkie łatwe supermoce
        fun getEasySuperPowers(): List<SuperPower> {
            return listOf(DODGE, SPARK, SMALL_HEAL, ENERGY_SHIELD, MINI_CRIT, DISTRACTION, RESISTANCE)
        }
        
        // Pobierz wszystkie średnie supermoce
        fun getMediumSuperPowers(): List<SuperPower> {
            return listOf(DOUBLE_STRIKE, VAMPIRISM, CRITICAL_HIT)
        }
        
        // Pobierz wszystkie trudne supermoce
        fun getHardSuperPowers(): List<SuperPower> {
            return listOf(FIRE_STORM, FURY_PARALYSIS)
        }
        
        // Pobierz losową supermoc o określonej trudności
        fun getRandomSuperPower(difficulty: SuperPowerDifficulty): SuperPower {
            val powers = when(difficulty) {
                SuperPowerDifficulty.EASY -> getEasySuperPowers()
                SuperPowerDifficulty.MEDIUM -> getMediumSuperPowers()
                SuperPowerDifficulty.HARD -> getHardSuperPowers()
            }
            return powers.random()
        }
    }
} 