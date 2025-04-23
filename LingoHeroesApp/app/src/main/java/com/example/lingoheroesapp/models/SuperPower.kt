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
        // SŁABE SUPERMOCY (łatwe zadania)
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
        
        // ŚREDNIE SUPERMOCY (średnie zadania)
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
        
        val ELEMENTAL_SHIELD = SuperPower(
            id = "elemental_shield",
            name = "Tarcza żywiołów",
            description = "Zmniejsza otrzymywane obrażenia o 25% przez 3 tury.",
            iconUrl = "elemental_shield_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 3,
            cooldown = 4,
            effectType = "damage_reduction",
            effectValue = 25.0,
            isPercentage = true
        )
        
        val MANA_GATHERING = SuperPower(
            id = "mana_gathering",
            name = "Zbieranie many",
            description = "Następny atak zadaje 150% obrażeń.",
            iconUrl = "mana_gathering_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 1,
            cooldown = 4,
            effectType = "damage_boost",
            effectValue = 150.0,
            isPercentage = true
        )
        
        val DOOM_SEAL = SuperPower(
            id = "doom_seal",
            name = "Pieczęć zagłady",
            description = "Jeśli potwór ma mniej niż 30% HP, zadaje dodatkowe 25% obrażeń.",
            iconUrl = "doom_seal_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 1,
            cooldown = 4,
            effectType = "execute",
            effectValue = 25.0,
            isPercentage = true
        )
        
        val SUPER_ATTACK = SuperPower(
            id = "super_attack",
            name = "Superatak",
            description = "Szansa na efekt supermocy laski zwiększona o 30%.",
            iconUrl = "super_attack_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 1,
            cooldown = 4,
            effectType = "wand_effect_boost",
            effectValue = 30.0,
            isPercentage = true
        )
        
        val ELEMENTAL = SuperPower(
            id = "elemental",
            name = "Żywiołak",
            description = "Dodaje losową kulę (ognistą/lodową/błyskawiczną) z 10% szansą na superatak.",
            iconUrl = "elemental_icon",
            difficulty = SuperPowerDifficulty.MEDIUM,
            duration = 1,
            cooldown = 4,
            effectType = "random_element",
            effectValue = 10.0,
            isPercentage = true
        )
        
        // MOCNE SUPERMOCY (trudne zadania)
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
        
        val ARMAGEDDON = SuperPower(
            id = "armageddon",
            name = "Armagedon",
            description = "Zadaje 300% obrażeń, jeśli potwór ma mniej niż 25% HP.",
            iconUrl = "armageddon_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 1,
            cooldown = 5,
            effectType = "execute",
            effectValue = 300.0,
            isPercentage = true
        )
        
        val POWER_REFLECTION = SuperPower(
            id = "power_reflection",
            name = "Odbicie mocy",
            description = "Całkowicie odbija następny atak potwora, zadając mu tyle samo obrażeń.",
            iconUrl = "power_reflection_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 1,
            cooldown = 5,
            effectType = "reflect",
            effectValue = 100.0,
            isPercentage = true
        )
        
        val ICE_STORM = SuperPower(
            id = "ice_storm",
            name = "Lodowa burza",
            description = "Zamraża potwora na 2 tury, niezależnie od rodzaju laski.",
            iconUrl = "ice_storm_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 2,
            cooldown = 5,
            effectType = "freeze",
            effectValue = 100.0,
            isPercentage = true
        )
        
        val THUNDER = SuperPower(
            id = "thunder",
            name = "Grom z nieba",
            description = "Paraliżuje potwora i zadaje dodatkowo 30% obrażeń.",
            iconUrl = "thunder_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 1,
            cooldown = 5,
            effectType = "stun_damage",
            effectValue = 30.0,
            isPercentage = true
        )
        
        val MAGIC_FIELD = SuperPower(
            id = "magic_field",
            name = "Pole magiczne",
            description = "Przez 3 tury wszystkie otrzymywane obrażenia są zmniejszone o 50%.",
            iconUrl = "magic_field_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 3,
            cooldown = 5,
            effectType = "damage_reduction",
            effectValue = 50.0,
            isPercentage = true
        )
        
        val CHAOS_TIME = SuperPower(
            id = "chaos_time",
            name = "Czas chaosu",
            description = "Przez 3 tury bohater atakuje dwukrotnie.",
            iconUrl = "chaos_time_icon",
            difficulty = SuperPowerDifficulty.HARD,
            duration = 3,
            cooldown = 5,
            effectType = "double_attack",
            effectValue = 100.0,
            isPercentage = true
        )
        
        // Pobierz wszystkie łatwe supermoce
        fun getEasySuperPowers(): List<SuperPower> {
            return listOf(DODGE, SPARK, SMALL_HEAL, ENERGY_SHIELD, MINI_CRIT, DISTRACTION, RESISTANCE)
        }
        
        // Pobierz wszystkie średnie supermoce
        fun getMediumSuperPowers(): List<SuperPower> {
            return listOf(
                DOUBLE_STRIKE, VAMPIRISM, CRITICAL_HIT, ELEMENTAL_SHIELD, 
                MANA_GATHERING, DOOM_SEAL, SUPER_ATTACK, ELEMENTAL
            )
        }
        
        // Pobierz wszystkie trudne supermoce
        fun getHardSuperPowers(): List<SuperPower> {
            return listOf(
                FIRE_STORM, FURY_PARALYSIS, ARMAGEDDON, POWER_REFLECTION, 
                ICE_STORM, THUNDER, MAGIC_FIELD, CHAOS_TIME
            )
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
        
        // Pobierz wszystkie supermoce
        fun getAllSuperPowers(): List<SuperPower> {
            return getEasySuperPowers() + getMediumSuperPowers() + getHardSuperPowers()
        }
    }
} 