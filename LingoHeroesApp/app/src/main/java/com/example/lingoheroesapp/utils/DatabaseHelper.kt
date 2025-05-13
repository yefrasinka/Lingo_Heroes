package com.example.lingoheroesapp.utils

import android.util.Log
import com.example.lingoheroesapp.models.Achievement
import com.example.lingoheroesapp.models.AchievementType
import com.example.lingoheroesapp.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.*

/**
 * Klasa pomocnicza do bezpiecznej obsługi operacji bazodanowych
 */
object DatabaseHelper {
    private const val TAG = "DatabaseHelper"

    /**
     * Bezpiecznie pobiera dane użytkownika z bazy danych
     */
    fun safelyGetUser(userRef: DatabaseReference, callback: (User?) -> Unit) {
        userRef.get().addOnSuccessListener { snapshot ->
            try {
                if (snapshot.exists()) {
                    try {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            user.uid = snapshot.key ?: ""
                            callback(user)
                            return@addOnSuccessListener
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Błąd deserializacji obiektu User, próbuję alternatywny sposób: ${e.message}")
                    }
                    
                    // Jeśli standardowa deserializacja nie zadziałała, próbujemy ręcznie zbudować obiekt User
                    try {
                        // Pobierz podstawowe dane użytkownika
                        val uid = snapshot.key ?: ""
                        val username = snapshot.child("username").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val level = safelyGetIntValue(snapshot.child("level"), 1)
                        val xp = safelyGetIntValue(snapshot.child("xp"), 0)
                        val coins = safelyGetIntValue(snapshot.child("coins"), 0)
                        val streakDays = safelyGetIntValue(snapshot.child("streakDays"), 0)
                        val perfectScores = safelyGetIntValue(snapshot.child("perfectScores"), 0)
                        val tasksCompleted = safelyGetIntValue(snapshot.child("tasksCompleted"), 0)
                        val challengesCompleted = safelyGetIntValue(snapshot.child("challengesCompleted"), 0)
                        val duelsCompleted = safelyGetIntValue(snapshot.child("duelsCompleted"), 0)
                        val bossesDefeated = safelyGetIntValue(snapshot.child("bossesDefeated"), 0)
                        
                        // Stwórz użytkownika z pobranymi danymi
                        val minimalUser = User(
                            uid = uid,
                            username = username,
                            email = email,
                            level = level,
                            xp = xp,
                            coins = coins,
                            streakDays = streakDays,
                            perfectScores = perfectScores,
                            tasksCompleted = tasksCompleted,
                            challengesCompleted = challengesCompleted,
                            duelsCompleted = duelsCompleted,
                            bossesDefeated = bossesDefeated
                        )
                        
                        callback(minimalUser)
                    } catch (e: Exception) {
                        Log.e(TAG, "Nie udało się odtworzyć danych użytkownika: ${e.message}")
                        callback(null)
                    }
                } else {
                    Log.e(TAG, "Nie znaleziono danych użytkownika")
                    callback(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd podczas pobierania danych użytkownika: ${e.message}")
                callback(null)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Błąd Firebase podczas pobierania danych użytkownika: ${e.message}")
            callback(null)
        }
    }

    /**
     * Bezpiecznie wyszukuje użytkowników według wzorca nazwy użytkownika
     */
    fun safelySearchUsers(
        databaseRef: DatabaseReference,
        searchPattern: String,
        currentUserId: String,
        callback: (List<User>) -> Unit
    ) {
        if (searchPattern.length < 3) {
            callback(emptyList())
            return
        }

        databaseRef.child("users")
            .orderByChild("username")
            .startAt(searchPattern)
            .endAt(searchPattern + "\uf8ff")
            .limitToFirst(20)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = mutableListOf<User>()
                    
                    for (userSnapshot in snapshot.children) {
                        try {
                            if (userSnapshot.key == currentUserId) continue
                            
                            // Pobierz podstawowe dane zamiast całego obiektu User
                            val username = userSnapshot.child("username").getValue(String::class.java) ?: continue
                            val level = safelyGetIntValue(userSnapshot.child("level"), 1)
                            val uid = userSnapshot.key ?: continue
                            
                            // Stwórz uproszczony obiekt User
                            val user = User(
                                uid = uid,
                                username = username,
                                level = level
                            )
                            
                            users.add(user)
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd deserializacji użytkownika: ${e.message}")
                            continue
                        }
                    }
                    
                    callback(users)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd wyszukiwania: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    /**
     * Bezpiecznie sprawdza status znajomego
     */
    fun safelyCheckFriendStatus(userRef: DatabaseReference, friendId: String, callback: (Boolean) -> Unit) {
        userRef.child("friends").child(friendId)
            .get().addOnSuccessListener { snapshot ->
                try {
                    val isFriend = if (snapshot.exists()) {
                        // Próbuje pobrać wartość jako Boolean
                        try {
                            snapshot.getValue(Boolean::class.java) ?: false
                        } catch (e: Exception) {
                            // Jeśli nie jest to Boolean, traktujemy sam klucz jako wskaźnik przyjaźni
                            Log.w(TAG, "Wartość przyjaciela nie jest typu Boolean: ${e.message}")
                            true
                        }
                    } else {
                        false
                    }
                    callback(isFriend)
                } catch (e: Exception) {
                    Log.e(TAG, "Błąd podczas sprawdzania statusu przyjaciela: ${e.message}")
                    callback(false)
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Błąd Firebase podczas sprawdzania statusu przyjaciela: ${e.message}")
                callback(false)
            }
    }

    /**
     * Bezpiecznie pobiera osiągnięcia użytkownika
     */
    fun safelyGetUserAchievements(
        databaseRef: DatabaseReference,
        userId: String,
        callback: (List<Achievement>) -> Unit
    ) {
        Log.d(TAG, "Pobieranie osiągnięć dla użytkownika: $userId")
        
        // Najpierw próbujemy pobrać osiągnięcia z kolekcji 'achievements'
        databaseRef.child("achievements")
            .orderByChild("userId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val achievements = mutableListOf<Achievement>()
                    
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        Log.d(TAG, "Brak osiągnięć w kolekcji achievements, próbuję pobrać z profilu użytkownika")
                        // Jeśli nie ma osiągnięć w kolekcji 'achievements', sprawdź w profilu użytkownika
                        tryGetAchievementsFromUserProfile(databaseRef, userId, callback)
                        return
                    }
                    
                    Log.d(TAG, "Znaleziono ${snapshot.childrenCount} osiągnięć w kolekcji achievements")
                    
                    for (achievementSnapshot in snapshot.children) {
                        try {
                            val achievement = achievementSnapshot.getValue(Achievement::class.java)
                            if (achievement != null) {
                                achievement.id = achievementSnapshot.key ?: ""
                                achievements.add(achievement)
                                Log.d(TAG, "Pomyślnie dodano osiągnięcie: ${achievement.title}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd deserializacji osiągnięcia: ${e.message}")
                            // Rekonstruuj podstawowe dane osiągnięcia
                            try {
                                val id = achievementSnapshot.key ?: ""
                                val title = achievementSnapshot.child("title").getValue(String::class.java) ?: "Nieznane osiągnięcie"
                                val description = achievementSnapshot.child("description").getValue(String::class.java) ?: "Brak opisu"
                                val isUnlocked = achievementSnapshot.child("isUnlocked").getValue(Boolean::class.java) ?: false
                                val typeStr = achievementSnapshot.child("type").getValue(String::class.java) ?: "XP"
                                
                                // Pobierz wartości progress i requiredValue z bazy danych
                                val progress = safelyGetIntValue(achievementSnapshot.child("progress"), 1)
                                val requiredValue = safelyGetIntValue(achievementSnapshot.child("requiredValue"), 100)
                                
                                // Konwertuj typy danych z różnych formatów
                                val achievementType = try {
                                    AchievementType.valueOf(typeStr)
                                } catch (e: Exception) {
                                    AchievementType.XP
                                }
                                
                                val achievementObj = Achievement(
                                    id = id,
                                    title = title,
                                    description = description,
                                    isUnlocked = isUnlocked,
                                    type = achievementType,
                                    progress = progress,
                                    requiredValue = requiredValue,
                                    userId = userId
                                )
                                
                                achievements.add(achievementObj)
                                Log.d(TAG, "Ręcznie odtworzono osiągnięcie: $title")
                            } catch (e2: Exception) {
                                Log.e(TAG, "Nie udało się odtworzyć danych osiągnięcia: ${e2.message}")
                                // Pomijamy to osiągnięcie
                                continue
                            }
                        }
                    }
                    
                    // Sortowanie osiągnięć według typu
                    achievements.sortBy { it.type.ordinal }
                    Log.d(TAG, "Zwracam ${achievements.size} osiągnięć dla użytkownika $userId")
                    callback(achievements)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd pobierania osiągnięć z kolekcji achievements: ${error.message}")
                    // Spróbuj pobrać z profilu użytkownika jako fallback
                    tryGetAchievementsFromUserProfile(databaseRef, userId, callback)
                }
            })
    }
    
    /**
     * Próbuje pobrać osiągnięcia z profilu użytkownika (jako fallback)
     */
    private fun tryGetAchievementsFromUserProfile(
        databaseRef: DatabaseReference,
        userId: String,
        callback: (List<Achievement>) -> Unit
    ) {
        databaseRef.child("users").child(userId).child("achievements")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val achievements = mutableListOf<Achievement>()
                    
                    if (!snapshot.exists()) {
                        Log.d(TAG, "Brak osiągnięć również w profilu użytkownika $userId")
                        callback(emptyList())
                        return
                    }
                    
                    try {
                        // Sprawdź, czy mamy do czynienia z listą czy mapą
                        val value = snapshot.getValue()
                        when (value) {
                            is ArrayList<*> -> {
                                Log.d(TAG, "Znaleziono listę osiągnięć zamiast mapy")
                                // Generujemy domyślne osiągnięcia, ponieważ mamy tylko listę identyfikatorów
                                generateDefaultAchievements(userId, callback)
                                return
                            }
                            is Map<*, *> -> {
                                Log.d(TAG, "Znaleziono mapę osiągnięć w profilu użytkownika")
                                for (achievementSnapshot in snapshot.children) {
                                    try {
                                        val key = achievementSnapshot.key ?: continue
                                        val title = achievementSnapshot.child("title").getValue(String::class.java) ?: 
                                            key.replace("_", " ").capitalize(Locale.getDefault())
                                        val description = achievementSnapshot.child("description").getValue(String::class.java) ?: 
                                            "Osiągnięcie $title"
                                        val isUnlocked = achievementSnapshot.child("isUnlocked").getValue(Boolean::class.java) ?: true
                                        
                                        // Pobierz wartości progress i requiredValue z bazy danych
                                        val progress = safelyGetIntValue(achievementSnapshot.child("progress"), 1)
                                        val requiredValue = safelyGetIntValue(achievementSnapshot.child("requiredValue"), 100)
                                        
                                        // Określenie typu na podstawie klucza
                                        val type = when {
                                            key.contains("xp", true) -> AchievementType.XP
                                            key.contains("level", true) -> AchievementType.LEVEL
                                            key.contains("task", true) -> AchievementType.TASKS_COMPLETED
                                            key.contains("streak", true) -> AchievementType.STREAK_DAYS
                                            key.contains("perfect", true) -> AchievementType.PERFECT_SCORES
                                            key.contains("duel", true) -> AchievementType.DUELS_COMPLETED
                                            key.contains("boss", true) -> AchievementType.BOSS_DEFEATED
                                            else -> AchievementType.XP
                                        }
                                        
                                        val achievement = Achievement(
                                            id = key,
                                            title = title,
                                            description = description,
                                            isUnlocked = isUnlocked,
                                            type = type,
                                            progress = progress,
                                            requiredValue = requiredValue,
                                            userId = userId
                                        )
                                        
                                        achievements.add(achievement)
                                        Log.d(TAG, "Dodano osiągnięcie z profilu: $title")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Błąd przetwarzania osiągnięcia z profilu: ${e.message}")
                                        continue
                                    }
                                }
                            }
                            else -> {
                                Log.w(TAG, "Nieznany format osiągnięć w profilu użytkownika")
                                generateDefaultAchievements(userId, callback)
                                return
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Błąd podczas przetwarzania osiągnięć z profilu: ${e.message}")
                        generateDefaultAchievements(userId, callback)
                        return
                    }
                    
                    // Sortowanie osiągnięć według typu
                    achievements.sortBy { it.type.ordinal }
                    Log.d(TAG, "Zwracam ${achievements.size} osiągnięć z profilu użytkownika $userId")
                    callback(achievements)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Błąd pobierania osiągnięć z profilu użytkownika: ${error.message}")
                    generateDefaultAchievements(userId, callback)
                }
            })
    }
    
    /**
     * Generuje domyślną listę osiągnięć gdy nie można pobrać rzeczywistych osiągnięć
     */
    private fun generateDefaultAchievements(userId: String, callback: (List<Achievement>) -> Unit) {
        val defaultAchievements = mutableListOf<Achievement>()
        
        // Dodaj osiągnięcia XP
        defaultAchievements.add(
            Achievement(
                id = "xp_1000",
                title = "Początkujący",
                description = "Zdobądź 1000 punktów doświadczenia",
                type = AchievementType.XP,
                progress = 0,
                requiredValue = 1000,
                isUnlocked = false,
                userId = userId
            )
        )
        
        defaultAchievements.add(
            Achievement(
                id = "xp_5000",
                title = "Zaawansowany",
                description = "Zdobądź 5000 punktów doświadczenia",
                type = AchievementType.XP,
                progress = 0,
                requiredValue = 5000,
                isUnlocked = false,
                userId = userId
            )
        )
        
        // Dodaj osiągnięcia poziomu
        defaultAchievements.add(
            Achievement(
                id = "level_5",
                title = "Uczeń",
                description = "Osiągnij poziom 5",
                type = AchievementType.LEVEL,
                progress = 0,
                requiredValue = 5,
                isUnlocked = false,
                userId = userId
            )
        )
        
        defaultAchievements.add(
            Achievement(
                id = "level_10",
                title = "Student",
                description = "Osiągnij poziom 10",
                type = AchievementType.LEVEL,
                progress = 0,
                requiredValue = 10,
                isUnlocked = false,
                userId = userId
            )
        )
        
        // Dodaj osiągnięcia serii
        defaultAchievements.add(
            Achievement(
                id = "streak_7",
                title = "Tygodniowa seria",
                description = "Ucz się przez 7 dni z rzędu",
                type = AchievementType.STREAK_DAYS,
                progress = 0,
                requiredValue = 7,
                isUnlocked = false,
                userId = userId
            )
        )
        
        defaultAchievements.add(
            Achievement(
                id = "streak_30",
                title = "Miesięczna seria",
                description = "Ucz się przez 30 dni z rzędu",
                type = AchievementType.STREAK_DAYS,
                progress = 0,
                requiredValue = 30,
                isUnlocked = false,
                userId = userId
            )
        )
        
        // Dodaj osiągnięcia zadań
        defaultAchievements.add(
            Achievement(
                id = "tasks_50",
                title = "Pracowity",
                description = "Ukończ 50 zadań",
                type = AchievementType.TASKS_COMPLETED,
                progress = 0,
                requiredValue = 50,
                isUnlocked = false,
                userId = userId
            )
        )
        
        // Dodaj osiągnięcia pojedynków
        defaultAchievements.add(
            Achievement(
                id = "duels_10",
                title = "Początkujący duelist",
                description = "Ukończ 10 pojedynków",
                type = AchievementType.DUELS_COMPLETED,
                progress = 0,
                requiredValue = 10,
                isUnlocked = false,
                userId = userId
            )
        )
        
        Log.d(TAG, "Wygenerowano ${defaultAchievements.size} domyślnych osiągnięć dla użytkownika $userId")
        callback(defaultAchievements)
    }
    
    /**
     * Bezpiecznie pobiera wartość Int z DataSnapshot
     */
    private fun safelyGetIntValue(snapshot: DataSnapshot, defaultValue: Int): Int {
        if (!snapshot.exists()) return defaultValue
        
        return try {
            val value = snapshot.getValue()
            when (value) {
                is Int -> value
                is Long -> value.toInt()
                is Double -> value.toInt()
                is String -> value.toIntOrNull() ?: defaultValue
                else -> defaultValue
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd konwersji wartości na Int: ${e.message}")
            defaultValue
        }
    }
}