package com.example.lingoheroesapp.utils

import android.util.Log
import com.example.lingoheroesapp.models.Challenge
import com.example.lingoheroesapp.models.ChallengeType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

/**
 * Centralny menadżer wyzwań odpowiedzialny za aktualizację postępu i zarządzanie wyzwaniami
 */
object ChallengeManager {
    private const val TAG = "ChallengeManager"
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Aktualizuje postęp określonego typu wyzwania dla bieżącego użytkownika
     * @param challengeType Typ wyzwania (np. DAILY, WEEKLY)
     * @param progressType Typ postępu (np. "xp", "tasks", "perfectScores", "streakDays")
     * @param incrementValue Wartość o jaką zwiększamy postęp
     */
    fun updateProgress(challengeType: String, progressType: String, incrementValue: Int = 1) {
        val userId = auth.currentUser?.uid ?: return
        
        // Pobierz wyzwania użytkownika
        database.child("users").child(userId).child("challenges")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Jeśli nie ma wyzwań, inicjalizujemy domyślne
                    if (!snapshot.exists()) {
                        createDefaultChallengesForUser(userId)
                        return
                    }
                    
                    // Zaktualizuj każde wyzwanie, które pasuje do typu
                    for (challengeSnapshot in snapshot.children) {
                        val challenge = challengeSnapshot.getValue(Challenge::class.java) ?: continue
                        
                        // Sprawdź czy wyzwanie powinno być zaktualizowane
                        if (shouldUpdateChallenge(challenge, challengeType, progressType)) {
                            val newProgress = minOf(challenge.currentProgress + incrementValue, challenge.requiredValue)
                            
                            // Aktualizuj postęp w bazie danych
                            val updates = hashMapOf<String, Any>(
                                "currentProgress" to newProgress,
                                "lastUpdateTime" to System.currentTimeMillis()
                            )
                            
                            // Oznaczamy wyzwanie jako ukończone, jeśli osiągnięto wymagany postęp
                            if (newProgress >= challenge.requiredValue) {
                                updates["isCompleted"] = true
                                
                                // Używamy również alternatywnych nazw pól dla Firebase
                                updates["completed"] = true
                            }
                            
                            challengeSnapshot.ref.updateChildren(updates)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Zaktualizowano postęp wyzwania ${challenge.id}: $newProgress/${challenge.requiredValue}")
                                    
                                    // Jeśli wyzwanie zostało ukończone, synchronizuj odpowiednie osiągnięcia
                                    if (newProgress >= challenge.requiredValue) {
                                        syncRelatedAchievements(userId, challenge)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Błąd podczas aktualizacji wyzwania ${challenge.id}: ${e.message}")
                                }
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd podczas pobierania wyzwań: ${error.message}")
                }
            })
    }
    
    /**
     * Synchronizuje osiągnięcia powiązane z ukończonym wyzwaniem
     */
    private fun syncRelatedAchievements(userId: String, challenge: Challenge) {
        // Importowanie klasy AchievementManager w razie potrzeby
        val achievementManager = com.example.lingoheroesapp.utils.AchievementManager
        
        // Określ typ osiągnięcia na podstawie typu wyzwania i jego opisu
        when {
            // Dla wyzwania "Perfekcyjny tydzień"
            challenge.id == "weekly_perfect" && challenge.isCompleted -> {
                achievementManager.updateAchievements(userId, 
                    com.example.lingoheroesapp.models.AchievementType.PERFECT_SCORES, 
                    10) // Synchronizuj z osiągnięciem perfekt
            }
            
            // Dla wyzwania "Tygodniowa seria"
            challenge.id == "weekly_streak" && challenge.isCompleted -> {
                // Aktualizuj osiągnięcie streak_7
                achievementManager.updateAchievements(userId, 
                    com.example.lingoheroesapp.models.AchievementType.STREAK_DAYS, 
                    7) // Synchronizuj z osiągnięciem 7-dniowej serii
                
                // Dodatkowo sprawdź, czy użytkownik ma już dłuższą serię
                database.child("users").child(userId).child("streakDays")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentStreak = snapshot.getValue(Int::class.java) ?: 0
                            // Aktualizuj serię tylko jeśli jest mniejsza niż 7
                            if (currentStreak < 7) {
                                database.child("users").child(userId).child("streakDays")
                                    .setValue(7)
                            }
                        }
                        
                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Błąd podczas aktualizacji serii: ${error.message}")
                        }
                    })
            }
        }
    }
    
    private fun shouldUpdateChallenge(challenge: Challenge, challengeType: String, progressType: String): Boolean {
        // Sprawdź czy wyzwanie nie wygasło i nie jest jeszcze ukończone
        if (challenge.expiresAt < System.currentTimeMillis() || challenge.isCompleted) {
            return false
        }
        
        // Najpierw sprawdź typ wyzwania (Daily/Weekly)
        val matchesType = when (challengeType) {
            "daily" -> challenge.type == ChallengeType.DAILY
            "weekly" -> challenge.type == ChallengeType.WEEKLY
            "all" -> true
            else -> false
        }
        
        if (!matchesType) return false
        
        // Następnie sprawdź czy wyzwanie dotyczy danego typu postępu
        return when (progressType) {
            "xp" -> challenge.description.contains("XP", ignoreCase = true)
            "tasks" -> challenge.description.contains("zadań", ignoreCase = true) || 
                       challenge.description.contains("ukończ", ignoreCase = true)
            "perfectScores" -> challenge.description.contains("perfekcyjn", ignoreCase = true)
            "streakDays" -> challenge.description.contains("seri", ignoreCase = true) ||
                            challenge.description.contains("dni", ignoreCase = true)
            else -> false
        }
    }
    
    /**
     * Aktualizuje pojedyncze wyzwanie w bazie danych
     */
    fun updateChallengeProgress(challengeRef: DatabaseReference, newProgress: Int, requiredValue: Int) {
        val updates = HashMap<String, Any>()
        updates["currentProgress"] = newProgress
        
        // Jeśli osiągnięto cel, oznaczamy wyzwanie jako ukończone
        if (newProgress >= requiredValue) {
            updates["isCompleted"] = true
            // Używamy również alternatywnej nazwy pola
            updates["completed"] = true
        }
        
        challengeRef.updateChildren(updates).addOnSuccessListener {
            Log.d(TAG, "Zaktualizowano postęp wyzwania: $newProgress/$requiredValue")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Błąd aktualizacji wyzwania: ${e.message}")
        }
    }

    /**
     * Sprawdza i resetuje wygasłe wyzwania
     */
    fun checkAndResetExpiredChallenges() {
        val userId = auth.currentUser?.uid ?: return
        val currentTime = System.currentTimeMillis()
        
        database.child("users").child(userId).child("challenges")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // Brak wyzwań, inicjalizujemy domyślne
                        createDefaultChallengesForUser(userId)
                        return
                    }
                    
                    val challenges = mutableListOf<Challenge>()
                    snapshot.children.forEach { 
                        val challenge = it.getValue(Challenge::class.java)
                        challenge?.let { challenges.add(it) }
                    }
                    
                    // Przetwarzanie wyzwań dziennych i tygodniowych osobno
                    val dailyChallenges = challenges.filter { it.type == ChallengeType.DAILY }
                    val weeklyChallenges = challenges.filter { it.type == ChallengeType.WEEKLY }
                    
                    // Sprawdzenie wyzwań dziennych
                    val anyDailyExpired = dailyChallenges.any { it.expiresAt < currentTime }
                    if (anyDailyExpired) {
                        // Resetowanie wszystkich wyzwań dziennych
                        resetDailyChallenges(userId)
                    }
                    
                    // Sprawdzenie wyzwań tygodniowych
                    val anyWeeklyExpired = weeklyChallenges.any { it.expiresAt < currentTime }
                    if (anyWeeklyExpired) {
                        // Resetowanie wszystkich wyzwań tygodniowych
                        resetWeeklyChallenges(userId)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd podczas sprawdzania wygasłych wyzwań: ${error.message}")
                }
            })
    }
    
    /**
     * Resetuje wszystkie wyzwania dzienne dla użytkownika
     */
    private fun resetDailyChallenges(userId: String) {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        
        // Ustawienie końca obecnego dnia
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.timeInMillis
        
        // Resetowanie wyzwań dziennych
        database.child("users").child(userId).child("challenges")
            .orderByChild("type")
            .equalTo(ChallengeType.DAILY.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { challengeSnapshot ->
                        val challenge = challengeSnapshot.getValue(Challenge::class.java) ?: return@forEach
                        
                        val updatedChallenge = Challenge(
                            id = challenge.id,
                            title = challenge.title,
                            description = challenge.description,
                            type = ChallengeType.DAILY,
                            requiredValue = challenge.requiredValue,
                            currentProgress = 0,
                            reward = challenge.reward,
                            isCompleted = false,
                            isRewardClaimed = false,
                            expiresAt = endOfDay,
                            lastUpdateTime = currentTime
                        )
                        
                        challengeSnapshot.ref.setValue(updatedChallenge)
                            .addOnSuccessListener {
                                Log.d(TAG, "Zresetowano wyzwanie dzienne: ${challenge.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Błąd podczas resetowania wyzwania: ${e.message}")
                            }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd podczas resetowania wyzwań dziennych: ${error.message}")
                }
            })
    }
    
    /**
     * Resetuje wszystkie wyzwania tygodniowe dla użytkownika
     */
    private fun resetWeeklyChallenges(userId: String) {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        
        // Ustawienie końca tygodnia (niedziela, 23:59:59)
        calendar.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.WEEK_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfWeek = calendar.timeInMillis
        
        // Resetowanie wyzwań tygodniowych
        database.child("users").child(userId).child("challenges")
            .orderByChild("type")
            .equalTo(ChallengeType.WEEKLY.toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { challengeSnapshot ->
                        val challenge = challengeSnapshot.getValue(Challenge::class.java) ?: return@forEach
                        
                        val updatedChallenge = Challenge(
                            id = challenge.id,
                            title = challenge.title,
                            description = challenge.description,
                            type = ChallengeType.WEEKLY,
                            requiredValue = challenge.requiredValue,
                            currentProgress = 0,
                            reward = challenge.reward,
                            isCompleted = false,
                            isRewardClaimed = false,
                            expiresAt = endOfWeek,
                            lastUpdateTime = currentTime
                        )
                        
                        challengeSnapshot.ref.setValue(updatedChallenge)
                            .addOnSuccessListener {
                                Log.d(TAG, "Zresetowano wyzwanie tygodniowe: ${challenge.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Błąd podczas resetowania wyzwania: ${e.message}")
                            }
                    }
                    
                    // Po resecie wyzwań tygodniowych, powiadom użytkownika
                    Log.d(TAG, "Wszystkie wyzwania tygodniowe zostały zresetowane")
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd podczas resetowania wyzwań tygodniowych: ${error.message}")
                }
            })
    }
    
    /**
     * Przyznaje nagrodę za ukończone wyzwanie
     */
    fun awardChallengeReward(challenge: Challenge) {
        val userId = auth.currentUser?.uid ?: return
        try {
            Log.d(TAG, "=== PRZYZNAWANIE NAGRODY ZA WYZWANIE ===")
            Log.d(TAG, "Wyzwanie: ${challenge.id}, ${challenge.title}")
            Log.d(TAG, "Status wyzwania - ukończone: ${challenge.isCompleted}, nagroda odebrana: ${challenge.isRewardClaimed}")
            
            if (!challenge.isCompleted) {
                Log.d(TAG, "Wyzwanie nie jest ukończone, nie przyznajemy nagrody")
                return
            }
            
            if (challenge.isRewardClaimed) {
                Log.d(TAG, "Nagroda już została odebrana")
                return
            }
            
            val userRef = database.child("users").child(userId)
            val challengeRef = userRef.child("challenges").child(challenge.id)
            
            // Pobierz aktualną liczbę monet użytkownika
            userRef.child("coins").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(coinsSnapshot: DataSnapshot) {
                    val currentCoins = coinsSnapshot.getValue(Int::class.java) ?: 0
                    Log.d(TAG, "Aktualna liczba monet: $currentCoins")
                    
                    // Oblicz nową liczbę monet
                    val newCoins = currentCoins + challenge.reward.coins
                    Log.d(TAG, "Nowa liczba monet: $newCoins")
                    
                    // Zaktualizuj monety i oznacz nagrodę jako odebraną
                    val updates = HashMap<String, Any>()
                    updates["coins"] = newCoins
                    
                    userRef.updateChildren(updates)
                        .addOnSuccessListener {
                            // Oznacz nagrodę jako odebraną
                            val challengeUpdates = HashMap<String, Any>()
                            challengeUpdates["isRewardClaimed"] = true
                            challengeUpdates["rewardClaimed"] = true // Alternatywna nazwa pola
                            
                            challengeRef.updateChildren(challengeUpdates)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Pomyślnie przyznano nagrodę i oznaczono jako odebraną")
                                    
                                    // Po odebraniu nagrody z wyzwania, sprawdź osiągnięcia związane z wyzwaniami
                                    if (challenge.type == ChallengeType.WEEKLY) {
                                        com.example.lingoheroesapp.utils.AchievementManager
                                            .syncAchievements(userId)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Błąd podczas oznaczania nagrody jako odebranej: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Błąd podczas aktualizacji monet: ${e.message}")
                        }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd podczas pobierania liczby monet: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek podczas przyznawania nagrody: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Resetuje wyzwanie, tworząc nowe na jego podstawie
     */
    private fun resetChallenge(challenge: Challenge): Challenge {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        
        val newExpiresAt = when (challenge.type) {
            ChallengeType.DAILY -> {
                calendar.apply {
                    // Ustawienie końca następnego dnia
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
            ChallengeType.WEEKLY -> {
                calendar.apply {
                    // Znajdź następną niedzielę (koniec tygodnia)
                    add(Calendar.WEEK_OF_YEAR, 1)
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
        }
        
        return Challenge(
            id = challenge.id,
            title = challenge.title,
            description = challenge.description,
            type = challenge.type,
            requiredValue = challenge.requiredValue,
            currentProgress = 0,
            reward = challenge.reward,
            isCompleted = false,
            isRewardClaimed = false,
            expiresAt = newExpiresAt,
            lastUpdateTime = currentTime
        )
    }
    
    /**
     * Tworzy domyślne wyzwania dla użytkownika
     */
    fun createDefaultChallengesForUser(userId: String) {
        val userRef = database.child("users").child(userId)
        val currentTime = System.currentTimeMillis()
        
        val calendar = Calendar.getInstance()
        
        // Ustaw koniec dnia (23:59:59)
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.timeInMillis
        
        // Znajdź koniec bieżącego tygodnia (niedziela, 23:59:59)
        calendar.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfWeek = calendar.timeInMillis
        
        val defaultChallenges = listOf(
            Challenge(
                id = "daily_xp",
                title = "Dzienny zdobywca XP",
                description = "Zdobądź 100 XP w ciągu dnia",
                type = ChallengeType.DAILY,
                requiredValue = 100,
                currentProgress = 0,
                reward = com.example.lingoheroesapp.models.Reward(coins = 150),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfDay,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "daily_tasks",
                title = "Codzienna praktyka",
                description = "Ukończ 5 zadań",
                type = ChallengeType.DAILY,
                requiredValue = 5,
                currentProgress = 0,
                reward = com.example.lingoheroesapp.models.Reward(coins = 100),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfDay,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "weekly_perfect",
                title = "Perfekcyjny tydzień",
                description = "Zdobądź 10 perfekcyjnych wyników w tym tygodniu",
                type = ChallengeType.WEEKLY,
                requiredValue = 10,
                currentProgress = 0,
                reward = com.example.lingoheroesapp.models.Reward(coins = 500),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfWeek,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "weekly_streak",
                title = "Tygodniowa seria",
                description = "Utrzymaj 7-dniową serię nauki",
                type = ChallengeType.WEEKLY,
                requiredValue = 7,
                currentProgress = 0,
                reward = com.example.lingoheroesapp.models.Reward(coins = 400),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfWeek,
                lastUpdateTime = currentTime
            )
        )
        
        // Zapisz każde wyzwanie
        defaultChallenges.forEach { challenge ->
            userRef.child("challenges").child(challenge.id).setValue(challenge)
                .addOnSuccessListener {
                    Log.d(TAG, "Utworzono domyślne wyzwanie: ${challenge.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Błąd podczas tworzenia wyzwania ${challenge.id}: ${e.message}")
                }
        }
    }
} 