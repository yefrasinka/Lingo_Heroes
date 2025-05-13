package com.example.lingoheroesapp.models

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Achievement(
    var id: String = "",                  // Identyfikator osiągnięcia
    val title: String = "",               // Nazwa osiągnięcia
    val description: String = "",         // Opis osiągnięcia
    val iconResId: Int = 0,              // ID zasobu ikony
    val requiredValue: Int = 0,          // Wymagana wartość do zdobycia
    val type: AchievementType = AchievementType.XP,  // Typ osiągnięcia
    var progress: Int = 0,               // Aktualny postęp
    @get:PropertyName("isUnlocked")
    @set:PropertyName("isUnlocked")
    var isUnlocked: Boolean = false,      // Czy osiągnięcie zostało odblokowane
    val userId: String = ""               // ID użytkownika, który zdobył osiągnięcie
)

enum class AchievementType {
    XP,                 // Osiągnięcia związane z punktami doświadczenia
    LEVEL,             // Osiągnięcia związane z poziomem
    TASKS_COMPLETED,   // Osiągnięcia związane z ukończonymi zadaniami
    STREAK_DAYS,       // Osiągnięcia związane z serią dni nauki
    PERFECT_SCORES,    // Osiągnięcia związane z idealnymi wynikami
    DUELS_COMPLETED,   // Osiągnięcia związane z ukończonymi pojedynkami
    BOSS_DEFEATED      // Osiągnięcia związane z pokonaniem bossów
} 