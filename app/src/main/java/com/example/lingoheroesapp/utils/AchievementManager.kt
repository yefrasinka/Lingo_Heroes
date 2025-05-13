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
        ),
        // Nowe osiągnięcia związane z wyzwaniami
        Achievement(
            id = "weekly_challenges_5",
            title = "Pogromca wyzwań tygodniowych",
            description = "Ukończ 5 wyzwań tygodniowych",
            type = AchievementType.CHALLENGES_COMPLETED,
            requiredValue = 5
        ),
        Achievement(
            id = "daily_challenges_20",
            title = "Codzienny wytrwały",
            description = "Ukończ 20 wyzwań dziennych",
            type = AchievementType.CHALLENGES_COMPLETED,
            requiredValue = 20
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
                    
                    // Inicjalizacja liczników dla osiągnięć związanych z wyzwaniami
                    userRef.child("challengesCompleted").setValue(0)
                    userRef.child("dailyChallengesCompleted").setValue(0)
                    userRef.child("weeklyChallengesCompleted").setValue(0)
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
        Log.d(TAG, "Aktualizacja osiągnięć typu $type z wartością $newValue")
        
        // Get user achievements
        database.child("users").child(userId).child("achievements")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userAchievements = mutableListOf<Achievement>()
                    
                    // Convert snapshot to Achievement objects
                    for (achievementSnapshot in snapshot.children) {
                        val achievement = achievementSnapshot.getValue(Achievement::class.java)
                        achievement?.let { userAchievements.add(it) }
                    }
                    
                    // Filter achievements by type
                    val typeAchievements = userAchievements.filter { it.type == type }
                    
                    // Update each achievement of the specified type
                    for (achievement in typeAchievements) {
                        // Aktualizuj tylko jeśli nowa wartość jest większa od aktualnej
                        if (newValue > achievement.progress) {
                            achievement.progress = newValue
                            
                            // Check if achievement should be unlocked
                            if (newValue >= achievement.requiredValue && !achievement.isUnlocked) {
                                achievement.isUnlocked = true
                                
                                // Reward the user for unlocking this achievement
                                rewardAchievement(userId, achievement)
                                
                                // Log the unlocked achievement
                                Log.d(TAG, "Odblokowano osiągnięcie: ${achievement.title}")
                            }
                            
                            // Update the achievement in Firebase
                            database.child("users").child(userId).child("achievements")
                                .child(achievement.id.toString()).setValue(achievement)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Zaktualizowano osiągnięcie ${achievement.id}: postęp=${achievement.progress}, odblokowane=${achievement.isUnlocked}")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Błąd podczas aktualizacji osiągnięcia: ${e.message}")
                                }
                        }
                    }
                    
                    // Zaktualizuj statystyki użytkownika związane z osiągnięciami
                    updateUserStatistics(userId, type, newValue)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd podczas aktualizacji osiągnięć: ${error.message}")
                }
            })
    }
    
    /**
     * Aktualizuje statystyki użytkownika związane z osiągnięciami
     */
    private fun updateUserStatistics(userId: String, type: AchievementType, newValue: Int) {
        val userRef = database.child("users").child(userId)
        
        // Aktualizuj odpowiednie statystyki na podstawie typu osiągnięcia
        when (type) {
            AchievementType.STREAK_DAYS -> {
                // Aktualizuj statystykę dni serii tylko jeśli nowa wartość jest większa
                userRef.child("streakDays").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentStreak = snapshot.getValue(Int::class.java) ?: 0
                        if (newValue > currentStreak) {
                            userRef.child("streakDays").setValue(newValue)
                            Log.d(TAG, "Zaktualizowano serię dni: $newValue")
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Błąd podczas aktualizacji serii dni: ${error.message}")
                    }
                })
            }
            
            AchievementType.CHALLENGES_COMPLETED -> {
                // Zaktualizuj liczniki ukończonych wyzwań
                userRef.child("challengesCompleted").setValue(newValue)
                Log.d(TAG, "Zaktualizowano liczbę ukończonych wyzwań: $newValue")
            }
            
            else -> {
                // Inne typy osiągnięć są aktualizowane w innych miejscach kodu
            }
        }
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
            AchievementType.CHALLENGES_COMPLETED -> 75 * (achievement.requiredValue / 5) // Nagroda za ukończone wyzwania
        }
        
        // Minimum 50 monet
        val finalReward = maxOf(coinReward, 50)
        
        // Zaktualizuj monety użytkownika
        userRef.child("coins").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentCoins = snapshot.getValue(Int::class.java) ?: 0
                userRef.child("coins").setValue(currentCoins + finalReward)
                    .addOnSuccessListener {
                        Log.d(TAG, "Przyznano nagrodę za osiągnięcie ${achievement.title}: $finalReward monet")
                    }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas przyznawania nagrody za osiągnięcie: ${error.message}")
            }
        })
    }
    
    /**
     * Aktualizuje osiągnięcie związane z ukończeniem wyzwania
     */
    fun updateChallengeCompletion(userId: String, isDaily: Boolean) {
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Odczytaj aktualną liczbę ukończonych wyzwań
                val totalChallenges = snapshot.child("challengesCompleted").getValue(Int::class.java) ?: 0
                val dailyChallenges = snapshot.child("dailyChallengesCompleted").getValue(Int::class.java) ?: 0
                val weeklyChallenges = snapshot.child("weeklyChallengesCompleted").getValue(Int::class.java) ?: 0
                
                // Zaktualizuj odpowiednie liczniki
                val updates = HashMap<String, Any>()
                updates["challengesCompleted"] = totalChallenges + 1
                
                if (isDaily) {
                    updates["dailyChallengesCompleted"] = dailyChallenges + 1
                } else {
                    updates["weeklyChallengesCompleted"] = weeklyChallenges + 1
                }
                
                // Zapisz zmiany
                database.child("users").child(userId).updateChildren(updates)
                    .addOnSuccessListener {
                        // Aktualizuj osiągnięcia związane z wyzwaniami
                        updateAchievements(userId, AchievementType.CHALLENGES_COMPLETED, totalChallenges + 1)
                        
                        Log.d(TAG, "Zaktualizowano liczniki ukończonych wyzwań")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Błąd podczas aktualizacji liczników wyzwań: ${e.message}")
                    }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas odczytu liczników wyzwań: ${error.message}")
            }
        })
    }
    
    /**
     * Synchronizuje wszystkie osiągnięcia na podstawie aktualnych statystyk użytkownika
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
                
                // Pobierz statystyki pojedynków
                val duelsCompleted = snapshot.child("duelsCompleted").getValue(Int::class.java) ?: 0
                val bossesDefeated = snapshot.child("bossesDefeated").getValue(Int::class.java) ?: 0
                
                // Pobierz statystyki wyzwań
                val challengesCompleted = snapshot.child("challengesCompleted").getValue(Int::class.java) ?: 0
                
                // Sprawdź również wyzwania użytkownika
                checkChallengesForAchievements(userId)
                
                // Zaktualizuj osiągnięcia dla każdego typu
                updateAchievements(userId, AchievementType.XP, xp)
                updateAchievements(userId, AchievementType.LEVEL, level)
                updateAchievements(userId, AchievementType.TASKS_COMPLETED, tasksCompleted)
                updateAchievements(userId, AchievementType.STREAK_DAYS, streakDays)
                updateAchievements(userId, AchievementType.PERFECT_SCORES, perfectScores)
                updateAchievements(userId, AchievementType.DUELS_COMPLETED, duelsCompleted)
                updateAchievements(userId, AchievementType.BOSS_DEFEATED, bossesDefeated)
                updateAchievements(userId, AchievementType.CHALLENGES_COMPLETED, challengesCompleted)
                
                Log.d(TAG, "Zsynchronizowano wszystkie osiągnięcia użytkownika")
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas synchronizacji osiągnięć: ${error.message}")
            }
        })
    }
    
    /**
     * Sprawdza wyzwania użytkownika pod kątem osiągnięć
     */
    private fun checkChallengesForAchievements(userId: String) {
        database.child("users").child(userId).child("challenges")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    
                    var weeklyStreakCompleted = false
                    var perfectWeekCompleted = false
                    
                    // Sprawdź wyzwania tygodniowe
                    for (challengeSnapshot in snapshot.children) {
                        val challenge = challengeSnapshot.getValue(com.example.lingoheroesapp.models.Challenge::class.java) ?: continue
                        
                        // Sprawdź czy wyzwanie tygodniowe "streak" jest ukończone
                        if (challenge.id == "weekly_streak" && challenge.isCompleted) {
                            weeklyStreakCompleted = true
                            
                            // Aktualizuj osiągnięcie związane z serią dni
                            updateAchievements(userId, AchievementType.STREAK_DAYS, 7)
                        }
                        
                        // Sprawdź czy wyzwanie tygodniowe "perfect" jest ukończone
                        if (challenge.id == "weekly_perfect" && challenge.isCompleted) {
                            perfectWeekCompleted = true
                            
                            // Aktualizuj osiągnięcie związane z perfekcyjnymi wynikami
                            updateAchievements(userId, AchievementType.PERFECT_SCORES, 10)
                        }
                    }
                    
                    // Zaktualizuj statystyki użytkownika, jeśli znaleziono ukończone wyzwania
                    if (weeklyStreakCompleted || perfectWeekCompleted) {
                        val updates = HashMap<String, Any>()
                        
                        if (weeklyStreakCompleted) {
                            // Upewnij się, że użytkownik ma co najmniej 7 dni serii
                            database.child("users").child(userId).child("streakDays")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(streakSnapshot: DataSnapshot) {
                                        val currentStreak = streakSnapshot.getValue(Int::class.java) ?: 0
                                        if (currentStreak < 7) {
                                            database.child("users").child(userId).child("streakDays").setValue(7)
                                        }
                                    }
                                    
                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(TAG, "Błąd podczas aktualizacji serii dni: ${error.message}")
                                    }
                                })
                        }
                        
                        if (perfectWeekCompleted) {
                            // Upewnij się, że użytkownik ma co najmniej 10 perfekcyjnych wyników
                            database.child("users").child(userId).child("perfectScores")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(perfectSnapshot: DataSnapshot) {
                                        val currentPerfect = perfectSnapshot.getValue(Int::class.java) ?: 0
                                        if (currentPerfect < 10) {
                                            database.child("users").child(userId).child("perfectScores").setValue(10)
                                        }
                                    }
                                    
                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(TAG, "Błąd podczas aktualizacji perfekcyjnych wyników: ${error.message}")
                                    }
                                })
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd podczas sprawdzania wyzwań: ${error.message}")
                }
            })
    }
} 