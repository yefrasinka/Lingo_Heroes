package com.example.lingoheroesapp.utils

import android.util.Log
import com.example.lingoheroesapp.models.Achievement
import com.example.lingoheroesapp.models.AchievementType
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
            id = "level_5",
            title = "Ambitny uczeń",
            description = "Osiągnij poziom 5",
            type = AchievementType.LEVEL,
            requiredValue = 5
        ),
        Achievement(
            id = "level_10",
            title = "Ekspert językowy",
            description = "Osiągnij poziom 10",
            type = AchievementType.LEVEL,
            requiredValue = 10
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
    fun updateAchievements(userId: String, statType: AchievementType, newValue: Int) {
        val userRef = database.child("users").child(userId)
        val achievementsRef = userRef.child("achievements")
        
        // Pobierz aktualne osiągnięcia użytkownika
        achievementsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Jeśli nie ma osiągnięć, zainicjalizuj je
                    initializeAchievementsForUser(userId)
                    return
                }
                
                // Filtruj osiągnięcia odpowiadające danemu typowi statystyki
                val relevantAchievements = allAchievements.filter { it.type == statType }
                val updates = HashMap<String, Any>()
                
                relevantAchievements.forEach { achievement ->
                    val currentSnapshot = snapshot.child(achievement.id)
                    
                    // Pobierz istniejące dane lub użyj domyślnych
                    val currentProgress = currentSnapshot.child("progress").getValue(Int::class.java) ?: 0
                    val isUnlocked = currentSnapshot.child("isUnlocked").getValue(Boolean::class.java) ?: false
                    
                    // Zaktualizuj postęp tylko jeśli osiągnięcie nie zostało jeszcze odblokowane
                    if (!isUnlocked) {
                        // Sprawdź czy nowa wartość oznacza ukończenie osiągnięcia
                        val shouldUnlock = newValue >= achievement.requiredValue
                        
                        // Zaktualizuj osiągnięcie
                        updates["${achievement.id}/progress"] = newValue
                        
                        if (shouldUnlock && !isUnlocked) {
                            updates["${achievement.id}/isUnlocked"] = true
                            
                            // Wyślij powiadomienie o odblokowaniu osiągnięcia (opcjonalnie)
                            Log.d(TAG, "Osiągnięcie ${achievement.title} zostało odblokowane dla użytkownika $userId")
                            
                            // Tutaj można dodać system nagród za osiągnięcia
                            rewardAchievement(userRef, achievement)
                        }
                    }
                }
                
                // Jeśli są jakieś aktualizacje, wyślij je do Firebase
                if (updates.isNotEmpty()) {
                    achievementsRef.updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "Pomyślnie zaktualizowano osiągnięcia dla użytkownika $userId")
                        }
                        .addOnFailureListener { error ->
                            Log.e(TAG, "Błąd podczas aktualizacji osiągnięć: ${error.message}")
                        }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas pobierania osiągnięć: ${error.message}")
            }
        })
    }
    
    /**
     * Przyznaje nagrodę za odblokowanie osiągnięcia
     */
    private fun rewardAchievement(userRef: DatabaseReference, achievement: Achievement) {
        // Przykładowe nagrody za osiągnięcia
        val coinReward = when (achievement.type) {
            AchievementType.XP -> 50 * (achievement.requiredValue / 1000)
            AchievementType.LEVEL -> 100 * achievement.requiredValue
            AchievementType.TASKS_COMPLETED -> 30 * (achievement.requiredValue / 50)
            AchievementType.STREAK_DAYS -> 50 * (achievement.requiredValue / 7)
            AchievementType.PERFECT_SCORES -> 40 * (achievement.requiredValue / 10)
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
                    val achievement = achievementSnapshot.getValue(Achievement::class.java)
                    achievement?.let { achievements.add(it) }
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
                // Pobierz statystyki użytkownika
                val xp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                val level = snapshot.child("level").getValue(Int::class.java) ?: 1
                val tasksCompleted = snapshot.child("tasksCompleted").getValue(Int::class.java) ?: 0
                val streakDays = snapshot.child("streakDays").getValue(Int::class.java) ?: 0
                val perfectScores = snapshot.child("perfectScores").getValue(Int::class.java) ?: 0
                
                // Zaktualizuj osiągnięcia dla każdego typu
                updateAchievements(userId, AchievementType.XP, xp)
                updateAchievements(userId, AchievementType.LEVEL, level)
                updateAchievements(userId, AchievementType.TASKS_COMPLETED, tasksCompleted)
                updateAchievements(userId, AchievementType.STREAK_DAYS, streakDays)
                updateAchievements(userId, AchievementType.PERFECT_SCORES, perfectScores)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas synchronizacji osiągnięć: ${error.message}")
            }
        })
    }
} 