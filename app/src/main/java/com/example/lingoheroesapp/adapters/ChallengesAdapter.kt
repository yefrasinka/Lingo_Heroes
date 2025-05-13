/**
 * Przyznaje nagrodę za ukończone wyzwanie
 */
private fun awardChallengeReward(challenge: Challenge) {
    val userId = auth.currentUser?.uid ?: return
    try {
        Log.d("ChallengesAdapter", "=== KLIKNIĘTO PRZYCISK ODBIERZ NAGRODĘ ===")
        Log.d("ChallengesAdapter", "Próba przyznania nagrody za wyzwanie: ${challenge.id}, ${challenge.title}")
        Log.d("ChallengesAdapter", "Status wyzwania - ukończone: ${challenge.isCompleted}, nagroda odebrana: ${challenge.isRewardClaimed}")
        Log.d("ChallengesAdapter", "Nagroda w monetach: ${challenge.reward.coins}")
        
        // Najpierw sprawdźmy, czy wyzwanie jest już ukończone w naszym lokalnym modelu
        if (!challenge.isCompleted) {
            Log.d("ChallengesAdapter", "Wyzwanie nie jest ukończone lokalnie, nie przyznajemy nagrody")
            return
        }
        
        if (challenge.isRewardClaimed) {
            Log.d("ChallengesAdapter", "Nagroda już została odebrana lokalnie, nie przyznajemy jej ponownie")
            return
        }
        
        // Przekazujemy zadanie do ChallengeManager, który zajmie się przyznaniem nagrody
        // i aktualizacją stanu wyzwania w bazie danych
        com.example.lingoheroesapp.utils.ChallengeManager.awardChallengeReward(challenge)
        
        // Aktualizujemy licznik ukończonych wyzwań w osiągnięciach
        val achievementManager = com.example.lingoheroesapp.utils.AchievementManager
        achievementManager.updateChallengeCompletion(userId, challenge.type == ChallengeType.DAILY)
        
        // Pokazujemy komunikat o odebranej nagrodzie
        val adapterContext = challengeHolders.firstOrNull()?.itemView?.context
        adapterContext?.let {
            Toast.makeText(
                it, 
                "Odebrano nagrodę: ${challenge.reward.coins} monet!",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        // Aktualizujemy lokalny model wyzwania, by natychmiast odzwierciedlić zmianę w UI
        challenge.isRewardClaimed = true
        notifyDataSetChanged()
        
    } catch (e: Exception) {
        Log.e("ChallengesAdapter", "Wyjątek podczas przyznawania nagrody: ${e.message}")
        e.printStackTrace()
    }
} 