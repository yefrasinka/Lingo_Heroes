package com.example.lingoheroesapp.utils

import android.util.Log
import com.example.lingoheroesapp.models.Achievement
import com.example.lingoheroesapp.models.AchievementType
import com.example.lingoheroesapp.models.LanguageLevel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/**
 * Centralny menadżer osiągnięć odpowiedzialny za aktualizację i zarządzanie osiągnięciami
 */
object AchievementManager {
    private const val TAG = "AchievementManager"
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Standardowa lista wszystkich osiągnięć w aplikacji
     */
    private val allAchievements = listOf(
        Achievement(
            id = "xp_1000",
            title = "Początkujący lingwista",
            description = "Zdobądź 1000 punktów doświadczenia",
            type = AchievementType.XP,
            requiredValue = 1000
        ),
        Achievement(
            id = "xp_5000",
            title = "Zaawansowany lingwista",
            description = "Zdobądź 5000 punktów doświadczenia",
            type = AchievementType.XP,
            requiredValue = 5000
        ),
        Achievement(
            id = "level_A2",
            title = "Poziom podstawowy",
            description = "Osiągnij poziom językowy A2",
            type = AchievementType.LEVEL,
            requiredValue = LanguageLevel.A2.value
        ),
        Achievement(
            id = "level_B1",
            title = "Poziom średniozaawansowany",
            description = "Osiągnij poziom językowy B1",
            type = AchievementType.LEVEL,
            requiredValue = LanguageLevel.B1.value
        ),
        Achievement(
            id = "level_B2",
            title = "Ekspert językowy",
            description = "Osiągnij najwyższy poziom językowy B2",
            type = AchievementType.LEVEL,
            requiredValue = LanguageLevel.B2.value
        ),
        Achievement(
            id = "tasks_50",
            title = "Pracowity student",
            description = "Ukończ 50 zadań",
            type = AchievementType.TASKS_COMPLETED,
            requiredValue = 50
        ),
        Achievement(
            id = "tasks_200",
            title = "Mistrz zadań",
            description = "Ukończ 200 zadań",
            type = AchievementType.TASKS_COMPLETED,
            requiredValue = 200
        ),
        Achievement(
            id = "streak_7",
            title = "Tygodniowa seria",
            description = "Utrzymaj serię nauki przez 7 dni",
            type = AchievementType.STREAK_DAYS,
            requiredValue = 7
        ),
        Achievement(
            id = "streak_30",
            title = "Miesięczna seria",
            description = "Utrzymaj serię nauki przez 30 dni",
            type = AchievementType.STREAK_DAYS,
            requiredValue = 30
        ),
        Achievement(
            id = "perfect_10",
            title = "Perfekcjonista",
            description = "Zdobądź 10 perfekcyjnych wyników",
            type = AchievementType.PERFECT_SCORES,
            requiredValue = 10
        ),
        Achievement(
            id = "perfect_50",
            title = "Bezbłędny mistrz",
            description = "Zdobądź 50 perfekcyjnych wyników",
            type = AchievementType.PERFECT_SCORES,
            requiredValue = 50
        ),
        // Nowe osiągnięcia związane z pojedynkami
        Achievement(
            id = "duels_5",
            title = "Początkujący wojownik",
            description = "Ukończ 5 pojedynków",
            type = AchievementType.DUELS_COMPLETED,
            requiredValue = 5
        ),
        Achievement(
            id = "duels_20",
            title = "Doświadczony wojownik",
            description = "Ukończ 20 pojedynków",
            type = AchievementType.DUELS_COMPLETED,
            requiredValue = 20
        ),
        Achievement(
            id = "duels_50",
            title = "Mistrz pojedynków",
            description = "Ukończ 50 pojedynków",
            type = AchievementType.DUELS_COMPLETED,
            requiredValue = 50
        ),
        Achievement(
            id = "boss_1",
            title = "Pogromca wyzwań",
            description = "Pokonaj pierwszego bossa",
            type = AchievementType.BOSS_DEFEATED,
            requiredValue = 1
        ),
        Achievement(
            id = "boss_3",
            title = "Łowca bossów",
            description = "Pokonaj trzech bossów",
            type = AchievementType.BOSS_DEFEATED,
            requiredValue = 3
        ),
        Achievement(
            id = "boss_all",
            title = "Legenda pojedynków",
            description = "Pokonaj wszystkich bossów",
            type = AchievementType.BOSS_DEFEATED,
            requiredValue = 4 // Zakładamy 4 bossów (etapy 3, 6, 9, 10)
        )
    )
    
    /**
     * Inicjalizuje osiągnięcia dla nowego użytkownika
     */
    fun initializeAchievementsForUser(userId: String) {
        val userRef = database.child("users").child(userId)
        val achievementsRef = userRef.child("achievements")
        
        // Sprawdź czy użytkownik ma już zapisane osiągnięcia
        achievementsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.childrenCount < allAchievements.size) {
                    // Utwórz początkowe osiągnięcia dla użytkownika
                    allAchievements.forEach { achievement ->
                        achievementsRef.child(achievement.id).setValue(achievement)
                    }
                    Log.d(TAG, "Zainicjalizowano osiągnięcia dla użytkownika $userId")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas inicjalizacji osiągnięć: ${error.message}")
            }
        })
    }
    
    /**
     * Aktualizuje osiągnięcia użytkownika na podstawie zmiany określonej statystyki
     */
    fun updateAchievements(userId: String, type: AchievementType, newValue: Int) {
        // Get user achievements
        database.child("users").child(userId).child("achievements")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userAchievements = mutableListOf<Achievement>()
                    
                    // Convert snapshot to Achievement objects
                    for (achievementSnapshot in snapshot.children) {
                        try {
                            val achievement = achievementSnapshot.getValue(Achievement::class.java)
                            achievement?.let { userAchievements.add(it) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd deserializacji osiągnięcia: ${e.message}")
                            // Skip this achievement
                            continue
                        }
                    }
                    
                    // Filter achievements by type
                    val typeAchievements = userAchievements.filter { it.type == type }
                    
                    // Update each achievement of the specified type
                    for (achievement in typeAchievements) {
                        val progress = when (type) {
                            AchievementType.XP -> newValue
                            AchievementType.LEVEL -> newValue
                            AchievementType.TASKS_COMPLETED -> newValue
                            AchievementType.STREAK_DAYS -> newValue
                            AchievementType.PERFECT_SCORES -> newValue
                            AchievementType.DUELS_COMPLETED -> newValue // Nowy typ osiągnięcia
                            AchievementType.BOSS_DEFEATED -> newValue   // Nowy typ osiągnięcia
                        }
                        
                        // Update the achievement progress
                        achievement.progress = progress
                        
                        // Check if achievement should be unlocked
                        if (progress >= achievement.requiredValue && !achievement.isUnlocked) {
                            achievement.isUnlocked = true
                            
                            // Reward the user for unlocking this achievement
                            rewardAchievement(userId, achievement)
                            
                            // Notify with toast if app is in foreground
                            // (this would need AppContext implementation in a real app)
                            // For simplicity, we'll log it instead
                            Log.d("AchievementManager", "Achievement unlocked: ${achievement.title}")
                        }
                        
                        // Update the achievement in Firebase
                        try {
                            database.child("users").child(userId).child("achievements")
                                .child(achievement.id.toString()).setValue(achievement)
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd aktualizacji osiągnięcia: ${e.message}")
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("AchievementManager", "Error updating achievements: ${error.message}")
                }
            })
    }
    
    /**
     * Przyznaje nagrodę za odblokowanie osiągnięcia
     */
    private fun rewardAchievement(userId: String, achievement: Achievement) {
        val userRef = database.child("users").child(userId)
        
        // Przykładowe nagrody za osiągnięcia
        val coinReward = when (achievement.type) {
            AchievementType.XP -> 50 * (achievement.requiredValue / 1000)
            AchievementType.LEVEL -> 100 * achievement.requiredValue
            AchievementType.TASKS_COMPLETED -> 30 * (achievement.requiredValue / 50)
            AchievementType.STREAK_DAYS -> 50 * (achievement.requiredValue / 7)
            AchievementType.PERFECT_SCORES -> 40 * (achievement.requiredValue / 10)
            AchievementType.DUELS_COMPLETED -> 60 * (achievement.requiredValue / 5) // Większa nagroda za pojedynki
            AchievementType.BOSS_DEFEATED -> 100 * achievement.requiredValue // Duża nagroda za pokonanie bossów
        }
        
        // Minimum 50 monet
        val finalReward = maxOf(coinReward, 50)
        
        // Zaktualizuj monety użytkownika
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val coins = currentData.child("coins").getValue(Int::class.java) ?: 0
                currentData.child("coins").value = coins + finalReward
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Błąd podczas przyznawania nagrody za osiągnięcie: ${error.message}")
                } else if (committed) {
                    Log.d(TAG, "Przyznano $finalReward monet za osiągnięcie ${achievement.title}")
                }
            }
        })
    }
    
    /**
     * Pobiera wszystkie osiągnięcia użytkownika wraz z ich statusem
     */
    fun getUserAchievements(userId: String, callback: (List<Achievement>) -> Unit) {
        val achievementsRef = database.child("users").child(userId).child("achievements")
        
        achievementsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Jeśli nie ma osiągnięć, zainicjalizuj je i zwróć domyślną listę
                    initializeAchievementsForUser(userId)
                    callback(allAchievements)
                    return
                }
                
                // Mapuj dane z Firebase do obiektów Achievement
                val achievements = mutableListOf<Achievement>()
                
                for (achievementSnapshot in snapshot.children) {
                    try {
                        val achievement = achievementSnapshot.getValue(Achievement::class.java)
                        achievement?.let { achievements.add(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Błąd deserializacji osiągnięcia w getUserAchievements: ${e.message}")
                        continue
                    }
                }
                
                // Sortuj według typu i postępu
                val sortedAchievements = achievements.sortedWith(
                    compareBy({ it.type.ordinal }, { it.requiredValue })
                )
                
                callback(sortedAchievements)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas pobierania osiągnięć: ${error.message}")
                callback(emptyList())
            }
        })
    }
    
    /**
     * Aktualizuje wszystkie osiągnięcia na podstawie aktualnych danych użytkownika
     */
    fun syncAchievements(userId: String) {
        // Pobierz dane użytkownika
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Pobierz statystyki użytkownika
                    val xp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                    val level = snapshot.child("level").getValue(Int::class.java) ?: 1
                    val tasksCompleted = snapshot.child("tasksCompleted").getValue(Int::class.java) ?: 0
                    val streakDays = snapshot.child("streakDays").getValue(Int::class.java) ?: 0
                    val perfectScores = snapshot.child("perfectScores").getValue(Int::class.java) ?: 0
                    
                    // Pobierz statystyki pojedynków
                    val duelsCompleted = snapshot.child("duelsCompleted").getValue(Int::class.java) ?: 0
                    val bossesDefeated = snapshot.child("bossesDefeated").getValue(Int::class.java) ?: 0
                    
                    // Zaktualizuj osiągnięcia dla każdego typu
                    updateAchievements(userId, AchievementType.XP, xp)
                    updateAchievements(userId, AchievementType.LEVEL, level)
                    updateAchievements(userId, AchievementType.TASKS_COMPLETED, tasksCompleted)
                    updateAchievements(userId, AchievementType.STREAK_DAYS, streakDays)
                    updateAchievements(userId, AchievementType.PERFECT_SCORES, perfectScores)
                    updateAchievements(userId, AchievementType.DUELS_COMPLETED, duelsCompleted)
                    updateAchievements(userId, AchievementType.BOSS_DEFEATED, bossesDefeated)
                } catch (e: Exception) {
                    Log.e(TAG, "Błąd podczas przetwarzania danych użytkownika: ${e.message}")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas synchronizacji osiągnięć: ${error.message}")
            }
        })
    }
} 