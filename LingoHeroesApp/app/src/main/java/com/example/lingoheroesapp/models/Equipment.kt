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
            GOLD -> com.example.lingoheroesapp.R.drawable.ic_armor_gold
        }
    }
    
    fun getCharacterImageResourceId(): Int {
        return when (this) {
            BRONZE -> com.example.lingoheroesapp.R.drawable.ic_warrior_fire
            SILVER -> com.example.lingoheroesapp.R.drawable.ic_silver_fire
            GOLD -> com.example.lingoheroesapp.R.drawable.ic_silver_fire  // Tymczasowo używamy srebrnej wersji
        }
    }
    
    companion object {
        fun fromInt(value: Int): ArmorTier {
            return values().getOrElse(value) { BRONZE }
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
    val baseHp: Int = 100,
    val baseDamage: Int = 10,
    val armorTier: ArmorTier = ArmorTier.BRONZE,
    val bronzeArmorCount: Int = 0,
    val silverArmorCount: Int = 0,
    val goldArmorCount: Int = 0,
    val characterElement: String = "fire",
    val wandType: WandType = WandType.FIRE,
    val pendingArmorUpgrade: Boolean = false
) {
    companion object {
        const val MAX_BRONZE_LEVEL = 10
        const val MAX_SILVER_LEVEL = 15
        const val MAX_GOLD_LEVEL = 20
        const val MAX_ARMOR_COUNT = 10

        // Add a method to create Equipment from a DataSnapshot
        fun fromMap(map: Map<String, Any?>): Equipment {
            return Equipment(
                armorLevel = (map["armorLevel"] as? Long)?.toInt() ?: 1,
                wandLevel = (map["wandLevel"] as? Long)?.toInt() ?: 1,
                baseHp = (map["baseHp"] as? Long)?.toInt() ?: 100,
                baseDamage = (map["baseDamage"] as? Long)?.toInt() ?: 10,
                armorTier = ArmorTier.fromAny(map["armorTier"]),
                bronzeArmorCount = (map["bronzeArmorCount"] as? Long)?.toInt() ?: 0,
                silverArmorCount = (map["silverArmorCount"] as? Long)?.toInt() ?: 0,
                goldArmorCount = (map["goldArmorCount"] as? Long)?.toInt() ?: 0,
                characterElement = map["characterElement"] as? String ?: "fire",
                wandType = WandType.fromString(map["wandType"] as? String),
                pendingArmorUpgrade = map["pendingArmorUpgrade"] as? Boolean ?: false
            )
        }
    }

    // Constructor needed for Firebase
    constructor() : this(1, 1, 100, 10, ArmorTier.BRONZE, 0, 0, 0, "fire", WandType.FIRE)
    
    // Get max level for current tier
    @Exclude
    fun getMaxLevelForCurrentTier(): Int {
        return when (armorTier) {
            ArmorTier.BRONZE -> MAX_BRONZE_LEVEL
            ArmorTier.SILVER -> MAX_SILVER_LEVEL
            ArmorTier.GOLD -> MAX_GOLD_LEVEL
        }
    }
    
    // Calculate current HP based on armor level and tier
    @Exclude
    fun getCurrentHp(): Int {
        val baseIncrease = (baseHp * (1 + (armorLevel - 1) * 0.1))
        val tierMultiplier = when (armorTier) {
            ArmorTier.BRONZE -> 1.0
            ArmorTier.SILVER -> 1.2
            ArmorTier.GOLD -> 1.5
        }
        return (baseIncrease * tierMultiplier).toInt()
    }
    
    // Check if can upgrade armor level
    @Exclude
    fun canUpgradeArmorLevel(): Boolean {
        return armorLevel < getMaxLevelForCurrentTier()
    }
    
    // Calculate HP after a potential upgrade
    @Exclude
    fun getUpgradedHp(): Int {
        if (!canUpgradeArmorLevel()) return getCurrentHp()
        
        val baseIncrease = (baseHp * (1 + armorLevel * 0.1))
        val tierMultiplier = when (armorTier) {
            ArmorTier.BRONZE -> 1.0
            ArmorTier.SILVER -> 1.2
            ArmorTier.GOLD -> 1.5
        }
        return (baseIncrease * tierMultiplier).toInt()
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
    fun upgradeArmor(): Equipment {
        return if (canUpgradeArmorLevel()) {
            this.copy(armorLevel = armorLevel + 1)
        } else {
            this
        }
    }
    
    // Create a new Equipment object with upgraded wand
    fun upgradeWand(): Equipment {
        return this.copy(wandLevel = wandLevel + 1)
    }
    
    // Zmień typ różdżki
    fun changeWandType(newType: WandType): Equipment {
        return this.copy(wandType = newType)
    }
    
    // Get armor count for current tier
    fun getCurrentTierArmorCount(): Int {
        return when (armorTier) {
            ArmorTier.BRONZE -> bronzeArmorCount
            ArmorTier.SILVER -> silverArmorCount
            ArmorTier.GOLD -> MAX_ARMOR_COUNT // Zawsze pokazuj MAX dla złotego poziomu
        }
    }
    
    // Get armor count of specified tier
    fun getArmorCount(tier: ArmorTier): Int {
        // Jeśli mamy złoty poziom, wszystkie liczniki pokazują MAX
        if (armorTier == ArmorTier.GOLD) {
            return MAX_ARMOR_COUNT
        }
        
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
                "fire" -> com.example.lingoheroesapp.R.drawable.ic_silver_fire // Tymczasowo używamy srebrnej wersji
                "ice" -> com.example.lingoheroesapp.R.drawable.ic_silver_ice // Tymczasowo używamy srebrnej wersji
                "lightning" -> com.example.lingoheroesapp.R.drawable.ic_silver_lightning // Tymczasowo używamy srebrnej wersji
                else -> com.example.lingoheroesapp.R.drawable.ic_silver_fire
            }
        }
    }
    
    // Add one bronze armor
    fun addBronzeArmor(): Equipment {
        // Jeśli mamy już złotą zbroję, nie zbieramy więcej
        if (armorTier == ArmorTier.GOLD) {
            return this
        }

        val newBronzeCount = bronzeArmorCount + 1
        
        return if (newBronzeCount >= MAX_ARMOR_COUNT && armorTier == ArmorTier.BRONZE) {
            // Oznacz, że jest dostępny upgrade, ale nie wykonuj go automatycznie
            this.copy(
                bronzeArmorCount = newBronzeCount,
                pendingArmorUpgrade = true
            )
        } else if (newBronzeCount >= MAX_ARMOR_COUNT) {
            // Jeśli zebraliśmy 10 brązowych, dodaj 1 srebrną i wyzeruj licznik brązowych
            this.copy(
                bronzeArmorCount = 0,
                silverArmorCount = silverArmorCount + 1
            )
        } else {
            this.copy(bronzeArmorCount = newBronzeCount)
        }
    }
    
    // Add one silver armor
    fun addSilverArmor(): Equipment {
        // Jeśli mamy już złotą zbroję, nie zbieramy więcej
        if (armorTier == ArmorTier.GOLD) {
            return this
        }

        val newSilverCount = silverArmorCount + 1
        
        return if (newSilverCount >= MAX_ARMOR_COUNT && armorTier == ArmorTier.SILVER) {
            // Oznacz, że jest dostępny upgrade, ale nie wykonuj go automatycznie
            this.copy(
                silverArmorCount = newSilverCount,
                pendingArmorUpgrade = true
            )
        } else if (newSilverCount >= MAX_ARMOR_COUNT) {
            // Jeśli zebraliśmy 10 srebrnych, dodaj 1 złotą i wyzeruj licznik srebrnych
            this.copy(
                silverArmorCount = 0,
                goldArmorCount = goldArmorCount + 1
            )
        } else {
            this.copy(silverArmorCount = newSilverCount)
        }
    }

    // Nowa metoda do wykonania oczekującego ulepszenia
    fun performPendingUpgrade(): Equipment {
        if (!pendingArmorUpgrade) return this
        
        return when (armorTier) {
            ArmorTier.BRONZE -> this.copy(
                bronzeArmorCount = 0, // Zerujemy licznik po ulepszeniu
                armorTier = ArmorTier.SILVER,
                armorLevel = 1,
                pendingArmorUpgrade = false
            )
            ArmorTier.SILVER -> this.copy(
                silverArmorCount = 0, // Zerujemy licznik po ulepszeniu
                armorTier = ArmorTier.GOLD,
                armorLevel = 1,
                pendingArmorUpgrade = false
            )
            ArmorTier.GOLD -> this // Już na najwyższym poziomie
        }
    }

    // Add a method to convert Equipment to a Map for Firebase
    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "armorLevel" to armorLevel,
            "wandLevel" to wandLevel,
            "baseHp" to baseHp,
            "baseDamage" to baseDamage,
            "armorTier" to armorTier.name, // Store as String instead of ordinal
            "bronzeArmorCount" to bronzeArmorCount,
            "silverArmorCount" to silverArmorCount,
            "goldArmorCount" to goldArmorCount,
            "characterElement" to characterElement,
            "wandType" to wandType.name,
            "pendingArmorUpgrade" to pendingArmorUpgrade
        )
    }
} 