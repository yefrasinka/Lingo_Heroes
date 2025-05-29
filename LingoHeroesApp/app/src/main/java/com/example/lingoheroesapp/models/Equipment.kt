package com.example.lingoheroesapp.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

enum class ArmorTier {
    BRONZE, 
    SILVER, 
    GOLD;
    
    // Dodaj pole dla wartości ordinal, które będzie serializowane
    val ordinalValue: Int
        get() = ordinal
    
    fun getNextTier(): ArmorTier {
        return when (this) {
            BRONZE -> SILVER
            SILVER -> GOLD
            GOLD -> GOLD // już najwyższy poziom
        }
    }
    
    fun getImageResourceId(): Int {
        return when (this) {
            BRONZE -> com.example.lingoheroesapp.R.drawable.ic_armor_bronze
            SILVER -> com.example.lingoheroesapp.R.drawable.ic_armor_silver
            GOLD -> com.example.lingoheroesapp.R.drawable.ic_armor_gold2 // Tymczasowo używamy srebrnej zbroi dla złotego poziomu
        }
    }
    
    fun getCharacterImageResourceId(): Int {
        return when (this) {
            BRONZE -> com.example.lingoheroesapp.R.drawable.ic_warrior_fire
            SILVER -> com.example.lingoheroesapp.R.drawable.ic_silver_fire
            GOLD -> com.example.lingoheroesapp.R.drawable.ic_silver_fire  // Tymczasowo używamy srebrnej zbroi dla złotego poziomu
        }
    }
    
    companion object {
        fun fromInt(value: Int): ArmorTier {
            return when (value) {
                1 -> BRONZE
                2 -> SILVER
                3 -> GOLD
                else -> BRONZE
            }
        }
        
        // Dodaj metodę do konwersji z różnych typów danych
        fun fromAny(value: Any?): ArmorTier {
            return when (value) {
                is Int -> values().getOrElse(value) { BRONZE }
                is Long -> values().getOrElse(value.toInt()) { BRONZE }
                is String -> try {
                    valueOf(value)
                } catch (e: Exception) {
                    try {
                        values().getOrElse(value.toIntOrNull() ?: 0) { BRONZE }
                    } catch (e: Exception) {
                        BRONZE
                    }
                }
                else -> BRONZE
            }
        }
    }
}

enum class WandType(val displayName: String) {
    FIRE("Ognista Różdżka"),
    ICE("Lodowa Różdżka"),
    LIGHTNING("Różdżka Błyskawic");
    
    // Pobierz obrazek różdżki na podstawie typu
    fun getWandImageResourceId(): Int {
        return when (this) {
            FIRE -> com.example.lingoheroesapp.R.drawable.ic_staff_fire
            ICE -> com.example.lingoheroesapp.R.drawable.ic_staff_ice
            LIGHTNING -> com.example.lingoheroesapp.R.drawable.ic_staff_lightning
        }
    }
    
    // Pobierz obrazek efektu różdżki
    fun getWandEffectResourceId(): Int {
        return when (this) {
            FIRE -> com.example.lingoheroesapp.R.drawable.effect_fire
            ICE -> com.example.lingoheroesapp.R.drawable.effect_ice
            LIGHTNING -> com.example.lingoheroesapp.R.drawable.effect_lightning
        }
    }
    
    // Opis efektu różdżki
    fun getEffectDescription(): String {
        return when (this) {
            FIRE -> "Zadaje dodatkowe obrażenia (25% szans)"
            ICE -> "Zamraża przeciwnika, blokując jego atak (20% szans)"
            LIGHTNING -> "Paraliżuje przeciwnika, obniżając jego obronę (15% szans)"
        }
    }
    
    // Szansa na aktywację efektu specjalnego
    fun getEffectChance(): Float {
        return when (this) {
            FIRE -> 0.25f      // 25% szans na dodatkowe obrażenia
            ICE -> 0.20f       // 20% szans na zamrożenie
            LIGHTNING -> 0.15f // 15% szans na paraliż
        }
    }
    
    // Współczynnik efektu (np. mnożnik obrażeń dla ognia)
    fun getEffectMultiplier(): Float {
        return when (this) {
            FIRE -> 1.5f       // +50% obrażeń
            ICE -> 1.0f        // blokuje atak przeciwnika
            LIGHTNING -> 0.7f  // zmniejsza obronę przeciwnika o 30%
        }
    }
    
    companion object {
        fun fromString(value: String?): WandType {
            return when (value?.lowercase()) {
                "fire" -> FIRE
                "ice" -> ICE
                "lightning" -> LIGHTNING
                else -> FIRE // domyślnie ogień
            }
        }
    }
}

@IgnoreExtraProperties
data class Equipment(
    val armorLevel: Int = 1,
    val wandLevel: Int = 1,
    val baseHp: Int = 100, // Base HP value at level 1
    val baseDamage: Int = 10, // Base damage value at level 1
    val armorTier: ArmorTier = ArmorTier.BRONZE,
    val bronzeArmorCount: Int = 0,
    val silverArmorCount: Int = 0,
    val goldArmorCount: Int = 0,
    val characterElement: String = "fire", // Domyślny element postaci
    val wandType: WandType = WandType.FIRE // Domyślny typ różdżki
) {
    // Constructor needed for Firebase
    constructor() : this(1, 1, 100, 10, ArmorTier.BRONZE, 0, 0, 0, "fire", WandType.FIRE)
    
    // Calculate current HP based on armor level and tier (increases by 10% per level and additional bonuses for higher tiers)
    @Exclude
    fun getCurrentHp(): Int {
        val tierMultiplier = when (armorTier) {
            ArmorTier.BRONZE -> 1.0
            ArmorTier.SILVER -> 1.2
            ArmorTier.GOLD -> 1.5
        }
        return (baseHp * (1 + (armorLevel - 1) * 0.1) * tierMultiplier).toInt()
    }
    
    // Calculate current damage based on wand level (increases by 10% per level)
    @Exclude
    fun getCurrentDamage(): Int {
        return (baseDamage * (1 + (wandLevel - 1) * 0.1)).toInt()
    }
    
    // Oblicz całkowite obrażenia z uwzględnieniem efektu różdżki
    @Exclude
    fun calculateTotalDamage(isEffectTriggered: Boolean = false): Int {
        val baseDmg = getCurrentDamage()
        
        // Jeśli efekt został aktywowany i jest to różdżka ognia, zwiększ obrażenia
        return if (isEffectTriggered && wandType == WandType.FIRE) {
            (baseDmg * wandType.getEffectMultiplier()).toInt()
        } else {
            baseDmg
        }
    }
    
    // Sprawdź, czy efekt różdżki zostanie aktywowany
    @Exclude
    fun isWandEffectTriggered(): Boolean {
        return Math.random() < wandType.getEffectChance()
    }
    
    // Calculate HP after a potential upgrade
    @Exclude
    fun getUpgradedHp(): Int {
        return (baseHp * (1 + armorLevel * 0.1) * when (armorTier) {
            ArmorTier.BRONZE -> 1.0
            ArmorTier.SILVER -> 1.2
            ArmorTier.GOLD -> 1.5
        }).toInt()
    }
    
    // Calculate damage after a potential upgrade
    @Exclude
    fun getUpgradedDamage(): Int {
        return (baseDamage * (1 + wandLevel * 0.1)).toInt()
    }
    
    // Calculate upgrade cost for armor based on current level
    // Formula: 100 * level^1.5
    @Exclude
    fun getArmorUpgradeCost(): Int {
        return (100 * Math.pow(armorLevel.toDouble(), 1.5)).toInt()
    }
    
    // Calculate upgrade cost for wand based on current level
    // Formula: 120 * level^1.5
    @Exclude
    fun getWandUpgradeCost(): Int {
        return (120 * Math.pow(wandLevel.toDouble(), 1.5)).toInt()
    }
    
    // Create a new Equipment object with upgraded armor
    fun upgradeArmor(tier: ArmorTier): Equipment {
        val updated = when (tier) {
            ArmorTier.BRONZE -> {
                require(bronzeArmorCount >= 10) { "Malo zbroje!" }
                this.copy(
                    armorLevel = armorLevel + 5,
                    bronzeArmorCount = bronzeArmorCount - 10,
                    silverArmorCount = silverArmorCount + 1
                )
            }
            ArmorTier.SILVER -> {
                require(silverArmorCount >= 10) { "Malo zbroje!" }
                this.copy(
                    armorLevel = armorLevel + 15,
                    silverArmorCount = silverArmorCount - 10,
                    goldArmorCount = goldArmorCount + 1
                )
            }
            ArmorTier.GOLD -> {
                require(goldArmorCount >= 10) { "Malo zbroje!" }
                this.copy(
                    armorLevel = armorLevel + 20,
                    goldArmorCount = goldArmorCount - 10
                    // GOLD — максимальний рівень, не додаємо вище
                )
            }
        }

        // Логіка зміни armorTier на найвищу доступну
        val newTier = when {
            updated.goldArmorCount > 0 -> ArmorTier.GOLD
            updated.silverArmorCount > 0 -> ArmorTier.SILVER
            else -> ArmorTier.BRONZE
        }

        return updated.copy(armorTier = newTier)
    }


    // Create a new Equipment object with upgraded wand
    fun upgradeWand(): Equipment {
        return this.copy(wandLevel = wandLevel + 1)
    }
    
    // Zmień typ różdżki
    fun changeWandType(newType: WandType): Equipment {
        return this.copy(wandType = newType)
    }
    
    // Add one bronze armor
    fun addBronzeArmor(): Equipment {
        val newBronzeCount = bronzeArmorCount + 1
        
        // Sprawdź, czy należy awansować na srebrny poziom
        return if (newBronzeCount >= 10 && armorTier == ArmorTier.BRONZE) {
            // Awansuj na srebrny poziom i zresetuj licznik brązowych zbroi na 0
            this.copy(
                bronzeArmorCount = 0,
                silverArmorCount = silverArmorCount + 1,
                armorTier = ArmorTier.SILVER
            )
        } else if (armorTier != ArmorTier.BRONZE) {
            // Jeśli już jesteśmy na wyższym poziomie
            
            // Sprawdź, czy zebraliśmy 10 brązowych zbroi na wyższym poziomie
            if (newBronzeCount >= 10) {
                // Resetujemy licznik brązowych zbroi i zwiększamy licznik srebrnych
                this.copy(
                    bronzeArmorCount = 0,
                    silverArmorCount = silverArmorCount + 1
                )
            } else {
                // Tylko zwiększamy licznik brązowych zbroi
                this.copy(bronzeArmorCount = newBronzeCount)
            }
        } else {
            // Standardowo zwiększamy licznik brązowych zbroi
            this.copy(bronzeArmorCount = newBronzeCount)
        }
    }
    
    // Add one silver armor
    fun addSilverArmor(): Equipment {
        val newSilverCount = silverArmorCount + 1
        
        // Sprawdź, czy należy awansować na złoty poziom
        return if (newSilverCount >= 10 && armorTier == ArmorTier.SILVER) {
            // Awansuj na złoty poziom i zresetuj licznik srebrnych zbroi na 0
            this.copy(
                silverArmorCount = 0,
                goldArmorCount = goldArmorCount + 1,
                armorTier = ArmorTier.GOLD
            )
        } else if (armorTier == ArmorTier.GOLD) {
            // Jeśli już jesteśmy na złotym poziomie, po prostu zwiększamy licznik srebrnych zbroi
            
            // Sprawdź, czy zebraliśmy 10 srebrnych zbroi na złotym poziomie
            if (newSilverCount >= 10) {
                // Resetujemy licznik srebrnych zbroi i zwiększamy licznik złotych
                this.copy(
                    silverArmorCount = 0,
                    goldArmorCount = goldArmorCount + 1
                )
            } else {
                // Tylko zwiększamy licznik srebrnych zbroi
                this.copy(silverArmorCount = newSilverCount)
            }
        } else {
            // Standardowo zwiększamy licznik srebrnych zbroi
            this.copy(silverArmorCount = newSilverCount)
        }
    }
    
    // Get armor count for current tier
    fun getCurrentTierArmorCount(): Int {
        return when (armorTier) {
            ArmorTier.BRONZE -> bronzeArmorCount
            ArmorTier.SILVER -> silverArmorCount
            ArmorTier.GOLD -> goldArmorCount
        }
    }
    
    // Get armor count of specified tier
    fun getArmorCount(tier: ArmorTier): Int {
        return when (tier) {
            ArmorTier.BRONZE -> bronzeArmorCount
            ArmorTier.SILVER -> silverArmorCount
            ArmorTier.GOLD -> goldArmorCount
        }
    }
    
    // Pobierz obrazek postaci na podstawie poziomu zbroi i typu elementu (różdżki)
    fun getCharacterImageByElement(): Int {
        // Użyj typu różdżki jako elementu postaci
        val element = wandType.name.lowercase()
        
        return when (armorTier) {
            ArmorTier.BRONZE -> when (element) {
                "fire" -> com.example.lingoheroesapp.R.drawable.ic_warrior_fire
                "ice" -> com.example.lingoheroesapp.R.drawable.ic_warrior_ice
                "lightning" -> com.example.lingoheroesapp.R.drawable.ic_warrior_lightning
                else -> com.example.lingoheroesapp.R.drawable.ic_warrior_fire
            }
            ArmorTier.SILVER -> when (element) {
                "fire" -> com.example.lingoheroesapp.R.drawable.ic_silver_fire
                "ice" -> com.example.lingoheroesapp.R.drawable.ic_silver_ice
                "lightning" -> com.example.lingoheroesapp.R.drawable.ic_silver_lightning
                else -> com.example.lingoheroesapp.R.drawable.ic_silver_fire
            }
            ArmorTier.GOLD -> when (element) {
                "fire" -> com.example.lingoheroesapp.R.drawable.ic_gold_fire // Tymczasowo używamy srebrnej wersji
                "ice" -> com.example.lingoheroesapp.R.drawable.ic_gold_ice // Tymczasowo używamy srebrnej wersji
                "lightning" -> com.example.lingoheroesapp.R.drawable.ic_gold_lightning // Tymczasowo używamy srebrnej wersji
                else -> com.example.lingoheroesapp.R.drawable.ic_gold_fire
            }
        }
    }
} 