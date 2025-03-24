package com.example.lingoheroesapp.models

data class Achievement(
    val id: String = "",                  // Identyfikator osiągnięcia
    val title: String = "",               // Nazwa osiągnięcia
    val description: String = "",         // Opis osiągnięcia
    val iconResId: Int = 0,              // ID zasobu ikony
    val requiredValue: Int = 0,          // Wymagana wartość do zdobycia
    val type: AchievementType = AchievementType.XP,  // Typ osiągnięcia
    var progress: Int = 0,               // Aktualny postęp
    var isUnlocked: Boolean = false      // Czy osiągnięcie zostało odblokowane
)

enum class AchievementType {
    XP,                 // Osiągnięcia związane z punktami doświadczenia
    LEVEL,             // Osiągnięcia związane z poziomem
    TASKS_COMPLETED,   // Osiągnięcia związane z ukończonymi zadaniami
    STREAK_DAYS,       // Osiągnięcia związane z serią dni nauki
    PERFECT_SCORES     // Osiągnięcia związane z idealnymi wynikami
} 