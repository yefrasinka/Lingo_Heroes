package com.example.lingoheroesapp.models

import android.graphics.Color

enum class ElementType {
    FIRE, ICE, LIGHTNING;
    
    /**
     * Zwraca efektywność tego typu żywiołu przeciwko innemu typowi
     * 1.5f - super efektywny (silny przeciwko)
     * 1.0f - neutralny
     * 0.5f - nieefektywny (słaby przeciwko)
     */
    fun getEffectiveness(against: ElementType): Float {
        return when (this) {
            FIRE -> when (against) {
                ICE -> 1.5f
                LIGHTNING -> 0.5f
                else -> 1.0f
            }
            ICE -> when (against) {
                LIGHTNING -> 1.5f
                FIRE -> 0.5f
                else -> 1.0f
            }
            LIGHTNING -> when (against) {
                FIRE -> 1.5f
                ICE -> 0.5f
                else -> 1.0f
            }
        }
    }
    
    /**
     * Konwertuje string do typu elementu
     */
    companion object {
        fun fromString(value: String): ElementType {
            return when (value.uppercase()) {
                "FIRE" -> FIRE
                "ICE" -> ICE
                "LIGHTNING" -> LIGHTNING
                else -> FIRE // Domyślnie ogień, jeśli nie znaleziono dopasowania
            }
        }
    }
    
    /**
     * Zwraca kod koloru dla elementu
     */
    fun getColorCode(): Int {
        return when (this) {
            FIRE -> Color.parseColor("#F44336") // Czerwony
            ICE -> Color.parseColor("#2196F3") // Niebieski
            LIGHTNING -> Color.parseColor("#FFC107") // Żółty
        }
    }
    
    /**
     * Zwraca nazwę elementu przyjazną dla użytkownika
     */
    fun getDisplayName(): String {
        return when (this) {
            FIRE -> "Ogień"
            ICE -> "Lód"
            LIGHTNING -> "Błyskawica"
        }
    }
} 