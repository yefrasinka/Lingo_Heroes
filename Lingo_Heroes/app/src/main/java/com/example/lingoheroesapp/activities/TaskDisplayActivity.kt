private fun updateChallenges(userId: String) {
    val challengesRef = database.child("users").child(userId).child("challenges")
    val userRef = database.child("users").child(userId)

    userRef.get().addOnSuccessListener { userSnapshot ->
        val user = userSnapshot.getValue(User::class.java) ?: return@addOnSuccessListener
        val currentDate = System.currentTimeMillis()
        
        // Sprawdzamy czy mamy poprawną odpowiedź na aktualne zadanie
        val isCorrectAnswer = lastSelectedAnswer == tasks[currentTaskIndex].correctAnswer
        val currentTask = tasks[currentTaskIndex]
        
        // Pobieramy aktualną wartość XP użytkownika bezpośrednio z Firebase
        val currentXp = userSnapshot.child("xp").getValue(Int::class.java) ?: 0
        
        challengesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { challengeSnapshot ->
                    val challenge = challengeSnapshot.getValue(Challenge::class.java)
                    
                    challenge?.let {
                        when (it.type) {
                            ChallengeType.DAILY -> {
                                // Sprawdzamy wyzwania dzienne
                                if (it.title.contains("XP", ignoreCase = true)) {
                                    // Aktualizacja postępu dla wyzwania związanego z XP
                                    if (isCorrectAnswer) {
                                        val newProgress = it.currentProgress + currentTask.rewardXp
                                        updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                    }
                                } else if (it.title.contains("praktyka", ignoreCase = true)) {
                                    // Aktualizacja postępu dla wyzwania związanego z zadaniami
                                    val newProgress = it.currentProgress + 1
                                    updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                }
                            }
                            ChallengeType.WEEKLY -> {
                                // Sprawdzamy wyzwania tygodniowe
                                if (it.title.contains("Perfekcyjny tydzień")) {
                                    // Aktualizujemy liczniki dzisiejszych zadań
                                    val todaysPerfectTasks = userSnapshot.child("todaysPerfectTasks").getValue(Int::class.java) ?: 0
                                    val todaysTotalTasks = userSnapshot.child("todaysTotalTasks").getValue(Int::class.java) ?: 0
                                    
                                    val updates = hashMapOf<String, Any>(
                                        "todaysTotalTasks" to (todaysTotalTasks + 1)
                                    )
                                    
                                    // Jeśli odpowiedź jest poprawna, zwiększamy licznik perfekcyjnych zadań
                                    if (isCorrectAnswer) {
                                        updates["todaysPerfectTasks"] = todaysPerfectTasks + 1
                                        
                                        // Sprawdzamy czy mamy wszystkie zadania poprawne i czy osiągnęliśmy minimalną liczbę (5)
                                        if (todaysPerfectTasks + 1 >= 5 && todaysPerfectTasks + 1 == todaysTotalTasks + 1) {
                                            // Dodajemy +1 do postępu wyzwania
                                            val newProgress = it.currentProgress + 1
                                            updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                            
                                            // Resetujemy liczniki na nowy dzień
                                            updates["lastPerfectDay"] = currentDate
                                            updates["todaysPerfectTasks"] = 0
                                            updates["todaysTotalTasks"] = 0
                                        }
                                    }
                                    
                                    userRef.updateChildren(updates)
                                } else if (it.title.contains("Tygodniowa seria")) {
                                    // Aktualizacja wyzwania związanego z serią nauki
                                    
                                    // Pobierz aktualną serię dni nauki użytkownika
                                    val streakDays = userSnapshot.child("streakDays").getValue(Int::class.java) ?: 0
                                    val lastActiveDay = userSnapshot.child("lastActiveDay").getValue(Long::class.java) ?: 0
                                    
                                    // Utwórz kalendarz dla obecnego dnia i ostatniego aktywnego dnia
                                    val today = Calendar.getInstance()
                                    val lastActive = Calendar.getInstance()
                                    lastActive.timeInMillis = lastActiveDay
                                    
                                    // Sprawdź czy to nowy dzień
                                    val isNewDay = today.get(Calendar.DAY_OF_YEAR) != lastActive.get(Calendar.DAY_OF_YEAR) ||
                                            today.get(Calendar.YEAR) != lastActive.get(Calendar.YEAR)
                                    
                                    if (isNewDay) {
                                        // Jeśli to nowy dzień, zaktualizuj ostatni aktywny dzień
                                        userRef.child("lastActiveDay").setValue(currentDate)
                                        
                                        // Sprawdź czy seria jest kontynuowana czy przerwana
                                        val isDayAfter = today.get(Calendar.DAY_OF_YEAR) == lastActive.get(Calendar.DAY_OF_YEAR) + 1 ||
                                                (today.get(Calendar.DAY_OF_YEAR) == 1 && lastActive.get(Calendar.DAY_OF_MONTH) == 31)
                                        
                                        if (isDayAfter) {
                                            // Seria kontynuowana - zwiększ licznik serii
                                            val newStreakDays = streakDays + 1
                                            userRef.child("streakDays").setValue(newStreakDays)
                                            
                                            // Aktualizuj wyzwanie tygodniowe
                                            if (newStreakDays <= it.requiredValue) {
                                                updateChallengeProgress(challengeSnapshot.ref, newStreakDays, it.requiredValue)
                                            }
                                            
                                            // Informuj użytkownika o nowym dniu serii
                                            Toast.makeText(
                                                this@TaskDisplayActivity,
                                                "Gratulacje! Twoja seria nauki: $newStreakDays dni!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskDisplayActivity", "Failed to update challenges", error.toException())
            }
        })
    }
} 