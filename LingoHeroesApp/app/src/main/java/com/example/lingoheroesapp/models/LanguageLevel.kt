package com.example.lingoheroesapp.models

/**
 * Poziomy zaawansowania językowego wg skali CEFR (Common European Framework of Reference for Languages)
 */
enum class LanguageLevel(val code: String, val value: Int, val description: String) {
    A1("A1", 1, "Początkujący"),
    A2("A2", 2, "Podstawowy"),
    B1("B1", 3, "Średniozaawansowany"),
    B2("B2", 4, "Wyższy średniozaawansowany");
    
    companion object {
        /**
         * Konwertuje wartość Int na odpowiedni poziom języka
         */
        fun fromValue(value: Int): LanguageLevel {
            return values().find { it.value == value } ?: A1
        }
        
        /**
         * Konwertuje kod poziomu na odpowiedni poziom języka
         */
        fun fromCode(code: String): LanguageLevel {
            return values().find { it.code == code } ?: A1
        }
        
        /**
         * Zwraca maksymalny dostępny poziom
         */
        fun getMaxLevel(): Int {
            return values().maxOf { it.value }
        }
    }
} 