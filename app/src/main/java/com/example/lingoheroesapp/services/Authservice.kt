// Tworzymy obiekt User z domyślnymi wartościami
val userData = User(
    uid = user?.uid ?: "",
    username = username,
    email = email,
    level = 1,
    xp = 0,
    coins = 100, // Dajemy startową ilość monet
    purchasedItems = emptyList(),
    topicsProgress = mapOf(),
    streakDays = 0,
    perfectScores = 0,
    tasksCompleted = 0,
    // Inicjalizacja pól związanych z wyzwaniami
    challengesCompleted = 0,
    dailyChallengesCompleted = 0,
    weeklyChallengesCompleted = 0,
    lastActiveDay = System.currentTimeMillis(),
    todaysPerfectTasks = 0,
    todaysTotalTasks = 0,
    equipment = Equipment()
)

// Zapisujemy dane użytkownika w Realtime Database
database.reference.child("users").child(user?.uid ?: "").setValue(userData)
    .addOnSuccessListener {
        // Inicjalizacja domyślnych wyzwań i osiągnięć dla nowego użytkownika
        user?.uid?.let { userId ->
            com.example.lingoheroesapp.utils.ChallengeManager.createDefaultChallengesForUser(userId)
            com.example.lingoheroesapp.utils.AchievementManager.initializeAchievementsForUser(userId)
        }
        onSuccess()
    }
    .addOnFailureListener { exception ->
        Log.e("AuthService", "Error storing user data: ${exception.message}")
        onFailure("Error storing user data: ${exception.message}")
    } 