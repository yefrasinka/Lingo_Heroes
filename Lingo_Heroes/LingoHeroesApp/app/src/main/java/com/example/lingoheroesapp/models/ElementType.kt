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
    fun getEffectiveness(target: ElementType): Float {
        return when (this) {
            FIRE -> when (target) {
                ICE -> 1.5f      // Ogień jest efektywny przeciwko lodowi
                LIGHTNING -> 0.8f // Ogień jest słaby przeciwko błyskawicom
                FIRE -> 1.0f     // Neutralne przeciwko temu samemu typowi
            }
            ICE -> when (target) {
                LIGHTNING -> 1.5f // Lód jest efektywny przeciwko błyskawicom
                FIRE -> 0.8f     // Lód jest słaby przeciwko ogniowi
                ICE -> 1.0f      // Neutralne przeciwko temu samemu typowi
            }
            LIGHTNING -> when (target) {
                FIRE -> 1.5f     // Błyskawica jest efektywna przeciwko ogniowi
                ICE -> 0.8f      // Błyskawica jest słaba przeciwko lodowi
                LIGHTNING -> 1.0f // Neutralne przeciwko temu samemu typowi
            }
        }
    }
    
    /**
     * Konwertuje string do typu elementu
     */
    companion object {
        fun fromString(value: String): ElementType {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                FIRE // Domyślny element, jeśli string jest nieprawidłowy
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