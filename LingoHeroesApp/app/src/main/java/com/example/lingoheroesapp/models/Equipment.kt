package com.example.lingoheroesapp.models

data class Equipment(
    val armorLevel: Int = 1,
    val wandLevel: Int = 1,
    val baseHp: Int = 100, // Base HP value at level 1
    val baseDamage: Int = 10 // Base damage value at level 1
) {
    // Constructor needed for Firebase
    constructor() : this(1, 1, 100, 10)
    
    // Calculate current HP based on armor level (increases by 10% per level)
    fun getCurrentHp(): Int {
        return (baseHp * (1 + (armorLevel - 1) * 0.1)).toInt()
    }
    
    // Calculate current damage based on wand level (increases by 10% per level)
    fun getCurrentDamage(): Int {
        return (baseDamage * (1 + (wandLevel - 1) * 0.1)).toInt()
    }
    
    // Calculate HP after a potential upgrade
    fun getUpgradedHp(): Int {
        return (baseHp * (1 + armorLevel * 0.1)).toInt()
    }
    
    // Calculate damage after a potential upgrade
    fun getUpgradedDamage(): Int {
        return (baseDamage * (1 + wandLevel * 0.1)).toInt()
    }
    
    // Calculate upgrade cost for armor based on current level
    // Formula: 100 * level^1.5
    fun getArmorUpgradeCost(): Int {
        return (100 * Math.pow(armorLevel.toDouble(), 1.5)).toInt()
    }
    
    // Calculate upgrade cost for wand based on current level
    // Formula: 120 * level^1.5
    fun getWandUpgradeCost(): Int {
        return (120 * Math.pow(wandLevel.toDouble(), 1.5)).toInt()
    }
    
    // Create a new Equipment object with upgraded armor
    fun upgradeArmor(): Equipment {
        return this.copy(armorLevel = armorLevel + 1)
    }
    
    // Create a new Equipment object with upgraded wand
    fun upgradeWand(): Equipment {
        return this.copy(wandLevel = wandLevel + 1)
    }
} 