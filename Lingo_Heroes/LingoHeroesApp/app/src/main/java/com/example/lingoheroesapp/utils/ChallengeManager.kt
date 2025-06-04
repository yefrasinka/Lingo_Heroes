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
    fun updateChallengeProgress(challengeType: String, progressType: String, incrementValue: Int = 1) {
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)
        
        // Pobierz wszystkie wyzwania użytkownika
        userRef.child("challenges").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Jeśli nie ma wyzwań, nie robimy nic
                if (!snapshot.exists()) return
                
                // Sprawdź każde wyzwanie
                snapshot.children.forEach { challengeSnapshot ->
                    val challenge = challengeSnapshot.getValue(Challenge::class.java) ?: return@forEach
                    
                    // Sprawdź czy wyzwanie spełnia kryteria aktualizacji
                    if (shouldUpdateChallenge(challenge, challengeType, progressType)) {
                        // Aktualizuj postęp wyzwania
                        updateChallenge(userRef, challenge, incrementValue)
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas aktualizacji postępu wyzwań: ${error.message}")
            }
        })
    }
    
    /**
     * Sprawdza czy wyzwanie powinno być zaktualizowane na podstawie typu wyzwania i postępu
     */
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
    private fun updateChallenge(userRef: DatabaseReference, challenge: Challenge, incrementValue: Int) {
        val challengeRef = userRef.child("challenges").child(challenge.id)
        
        // Pobierz aktualne dane wyzwania, aby mieć pewność, że pracujemy na najnowszej wersji
        challengeRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentProgress = currentData.child("currentProgress").getValue(Int::class.java) ?: 0
                val requiredValue = currentData.child("requiredValue").getValue(Int::class.java) ?: 0
                
                // Zwiększ postęp
                val newProgress = currentProgress + incrementValue
                currentData.child("currentProgress").value = newProgress
                currentData.child("lastUpdateTime").value = System.currentTimeMillis()
                
                // Jeśli osiągnięto wymagany postęp, oznacz wyzwanie jako ukończone
                if (newProgress >= requiredValue) {
                    currentData.child("isCompleted").value = true
                }
                
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Błąd transakcji podczas aktualizacji wyzwania ${challenge.id}: ${error.message}")
                } else if (committed) {
                    val newProgress = currentData?.child("currentProgress")?.getValue(Int::class.java) ?: 0
                    val requiredValue = currentData?.child("requiredValue")?.getValue(Int::class.java) ?: 0
                    val isCompleted = newProgress >= requiredValue
                    
                    Log.d(TAG, "Zaktualizowano postęp wyzwania ${challenge.id}: $newProgress/$requiredValue, ukończone: $isCompleted")
                }
            }
        })
    }
    
    /**
     * Tworzy i inicjalizuje domyślne wyzwania dla nowego użytkownika
     */
    fun createDefaultChallengesForUser(userId: String) {
        val userRef = database.child("users").child(userId)
        val challengesRef = userRef.child("challenges")
        
        // Pobierz aktualne wyzwania, aby sprawdzić czy już istnieją
        challengesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // Użytkownik nie ma jeszcze wyzwań, utwórz domyślne
                    val defaultChallenges = createDefaultChallenges()
                    defaultChallenges.forEach { challenge ->
                        challengesRef.child(challenge.id).setValue(challenge)
                    }
                    Log.d(TAG, "Utworzono domyślne wyzwania dla użytkownika $userId")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas tworzenia domyślnych wyzwań: ${error.message}")
            }
        })
    }
    
    /**
     * Sprawdza i resetuje wygasłe wyzwania
     */
    fun checkAndResetExpiredChallenges() {
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)
        val challengesRef = userRef.child("challenges")
        
        // Pobierz wszystkie wyzwania użytkownika
        challengesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                val currentTime = System.currentTimeMillis()
                val updates = HashMap<String, Any>()
                
                // Sprawdź każde wyzwanie
                snapshot.children.forEach { challengeSnapshot ->
                    val challenge = challengeSnapshot.getValue(Challenge::class.java) ?: return@forEach
                    
                    // Jeśli wyzwanie wygasło
                    if (challenge.expiresAt < currentTime) {
                        // Jeśli ukończone ale nagroda nie odebrana, dodaj monety do konta użytkownika
                        if (challenge.isCompleted && !challenge.isRewardClaimed) {
                            // Przyznaj nagrodę automatycznie
                            awardChallengeReward(userRef, challenge)
                        }
                        
                        // Resetuj wyzwanie
                        val newChallenge = resetChallenge(challenge)
                        updates["challenges/${challenge.id}"] = newChallenge
                    }
                }
                
                // Jeśli są jakieś aktualizacje, zastosuj je
                if (updates.isNotEmpty()) {
                    userRef.updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "Zresetowano wygasłe wyzwania dla użytkownika ${currentUser.uid}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Błąd podczas resetowania wyzwań: ${e.message}")
                        }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas sprawdzania wygasłych wyzwań: ${error.message}")
            }
        })
    }
    
    /**
     * Przyznaje nagrodę za wyzwanie
     */
    private fun awardChallengeReward(userRef: DatabaseReference, challenge: Challenge) {
        // Aktualizuj liczbę monet użytkownika i oznacz nagrodę jako odebraną
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val challengeData = currentData.child("challenges").child(challenge.id)
                
                // Sprawdź czy nagroda nie została już odebrana
                val isRewardClaimed = challengeData.child("isRewardClaimed").getValue(Boolean::class.java) ?: false
                if (isRewardClaimed) {
                    return Transaction.success(currentData)
                }
                
                // Pobierz aktualną liczbę monet
                val coins = currentData.child("coins").getValue(Int::class.java) ?: 0
                val rewardCoins = challenge.reward.coins
                
                // Dodaj monety i oznacz nagrodę jako odebraną
                currentData.child("coins").value = coins + rewardCoins
                challengeData.child("isRewardClaimed").value = true
                
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Błąd podczas przyznawania nagrody: ${error.message}")
                } else if (committed) {
                    Log.d(TAG, "Automatycznie przyznano nagrodę za wyzwanie ${challenge.id}: ${challenge.reward.coins} monet")
                }
            }
        })
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
                    add(Calendar.DAY_OF_YEAR, 1) // Przesunięcie do przodu o przynajmniej jeden dzień
                    while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
        }
        
        // Tworzymy nowy obiekt Challenge z zerowymi wartościami postępu
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
     * Tworzy domyślne wyzwania
     */
    private fun createDefaultChallenges(): List<Challenge> {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val endOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }.timeInMillis

        return listOf(
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
    }
} 