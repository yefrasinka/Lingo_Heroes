package com.example.lingoheroesapp.adapters

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Challenge
import com.example.lingoheroesapp.models.ChallengeType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.concurrent.TimeUnit

class ChallengesAdapter : RecyclerView.Adapter<ChallengesAdapter.ChallengeViewHolder>() {
    private var challenges: List<Challenge> = emptyList()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val challengeHolders = mutableListOf<ChallengeViewHolder>()
    private var challengeClickListener: ChallengeClickListener? = null
    
    interface ChallengeClickListener {
        fun onClaimRewardClicked(challenge: Challenge)
    }
    
    fun setChallengeClickListener(listener: ChallengeClickListener) {
        this.challengeClickListener = listener
    }

    fun updateChallenges(newChallenges: List<Challenge>) {
        challenges = newChallenges
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.challenge_item, parent, false)
        val holder = ChallengeViewHolder(view)
        challengeHolders.add(holder)
        return holder
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        
        holder.titleText.text = challenge.title
        holder.descriptionText.text = challenge.description
        
        // Ustawienie postępu
        holder.progressBar.max = challenge.requiredValue
        holder.progressBar.progress = challenge.currentProgress
        holder.progressText.text = "${challenge.currentProgress}/${challenge.requiredValue}"
        
        // Ustawienie nagrody w monetach
        holder.coinsRewardText.text = "${challenge.reward.coins} monet"
        
        // Ustawienie typu wyzwania
        holder.typeText.text = when (challenge.type) {
            ChallengeType.DAILY -> "Dzienne"
            ChallengeType.WEEKLY -> "Tygodniowe"
        }
        
        // Ustawienie stylu całego elementu
        if (challenge.isCompleted) {
            if (challenge.isRewardClaimed) {
                // Wyzwanie ukończone i nagroda odebrana
                holder.itemView.alpha = 0.7f
                holder.timeRemainingText.text = "Ukończone"
                holder.timeRemainingText.setTextColor(Color.GREEN)
                holder.itemView.isClickable = false
                holder.itemView.setBackgroundResource(android.R.color.transparent)
            } else {
                // Wyzwanie ukończone, ale nagroda nie została jeszcze odebrana
                holder.itemView.alpha = 1.0f
                holder.timeRemainingText.text = "ODBIERZ NAGRODĘ! ⭐"
                holder.timeRemainingText.setTextColor(Color.BLUE)
                holder.timeRemainingText.textSize = 16f
                holder.timeRemainingText.isAllCaps = true
                holder.itemView.isClickable = true
                
                // Dodajmy podświetlenie dla elementu - granicę wokół elementu
                holder.itemView.setBackgroundResource(R.drawable.reward_ready_background)
                
                // Dodanie obsługi kliknięcia, aby odebrać nagrodę
                holder.itemView.setOnClickListener {
                    challengeClickListener?.onClaimRewardClicked(challenge) ?: awardChallengeReward(challenge)
                }
            }
        } else if (challenge.expiresAt - System.currentTimeMillis() > 0) {
            // Wyzwanie w trakcie
            holder.itemView.alpha = 1.0f
            val timeRemaining = challenge.expiresAt - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(timeRemaining)
            val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60
            
            if (days > 0) {
                holder.timeRemainingText.text = "Pozostało: ${days}d ${hours}h"
            } else {
                holder.timeRemainingText.text = "Pozostało: ${hours}h ${minutes}m"
            }
            holder.timeRemainingText.setTextColor(Color.BLACK)
            holder.timeRemainingText.textSize = 14f
            holder.timeRemainingText.isAllCaps = false
            holder.itemView.isClickable = false
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        } else {
            // Wyzwanie wygasło
            holder.itemView.alpha = 0.5f
            holder.timeRemainingText.text = "Wygasło"
            holder.timeRemainingText.setTextColor(Color.RED)
            holder.timeRemainingText.textSize = 14f
            holder.timeRemainingText.isAllCaps = false
            holder.itemView.isClickable = false
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun getItemCount() = challenges.size
    
    private fun awardChallengeReward(challenge: Challenge) {
        val currentUser = auth.currentUser ?: return
        val userRef = database.getReference("users").child(currentUser.uid)
        val challengeRef = userRef.child("challenges").child(challenge.id)
        
        // Zapamiętaj kontekst aktualnego adaptera
        val adapterContext = challengeHolders.firstOrNull()?.itemView?.context
        
        // Dodajemy logi
        Log.d("ChallengesAdapter", "Próba przyznania nagrody za wyzwanie: ${challenge.id}, ${challenge.title}")
        Log.d("ChallengesAdapter", "Status wyzwania - ukończone: ${challenge.isCompleted}, nagroda odebrana: ${challenge.isRewardClaimed}")
        
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val challengeData = currentData.child("challenges").child(challenge.id)
                
                // Sprawdzamy status
                val isCompleted = challengeData.child("isCompleted").getValue(Boolean::class.java) ?: false
                val isRewardClaimed = challengeData.child("isRewardClaimed").getValue(Boolean::class.java) ?: false
                
                Log.d("ChallengesAdapter", "W transakcji - status wyzwania - ukończone: $isCompleted, nagroda odebrana: $isRewardClaimed")
                
                // Sprawdź, czy wyzwanie jest ukończone i nagroda nie została jeszcze odebrana
                if (isRewardClaimed || !isCompleted) {
                    Log.d("ChallengesAdapter", "Nie przyznajemy nagrody - warunki nie spełnione")
                    return Transaction.success(currentData)
                }
                
                // Pobierz aktualną liczbę monet
                val coins = currentData.child("coins").getValue(Int::class.java) ?: 0
                val rewardCoins = challenge.reward.coins
                
                // Dodaj monety do konta użytkownika
                currentData.child("coins").value = coins + rewardCoins
                
                // Oznacz wyzwanie jako odebrane
                challengeData.child("isRewardClaimed").value = true
                
                Log.d("ChallengesAdapter", "Nagroda przyznana! +$rewardCoins monet")
                
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    // Obsługa błędu
                    Log.e("ChallengesAdapter", "Błąd podczas przyznawania nagrody: ${error.message}")
                    adapterContext?.let {
                        Toast.makeText(
                            it,
                            "Błąd podczas odbierania nagrody: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (committed) {
                    Log.d("ChallengesAdapter", "Transakcja wykonana pomyślnie")
                    
                    // Sprawdź aktualny stan konta
                    userRef.child("coins").get().addOnSuccessListener { snapshot ->
                        val currentCoins = snapshot.getValue(Int::class.java) ?: 0
                        Log.d("ChallengesAdapter", "Aktualna liczba monet: $currentCoins")
                    }
                    
                    // Aktualizuj adapter, aby pokazać, że nagroda została odebrana
                    val updatedChallenges = challenges.toMutableList()
                    val index = updatedChallenges.indexOfFirst { it.id == challenge.id }
                    if (index != -1) {
                        // Tworzymy nowy obiekt z zaktualizowaną flagą
                        val updatedChallenge = challenge.copy(isRewardClaimed = true)
                        updatedChallenges[index] = updatedChallenge
                        
                        // Wyświetl powiadomienie o przyznanej nagrodzie
                        adapterContext?.let {
                            Toast.makeText(
                                it,
                                "Otrzymałeś ${challenge.reward.coins} monet za ukończenie wyzwania: ${challenge.title}!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        
                        // Aktualizuj listę
                        updateChallenges(updatedChallenges)
                    }
                }
            }
        })
    }

    class ChallengeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.challengeTitleText)
        val descriptionText: TextView = view.findViewById(R.id.challengeDescriptionText)
        val progressBar: ProgressBar = view.findViewById(R.id.challengeProgressBar)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val coinsRewardText: TextView = view.findViewById(R.id.coinsRewardText)
        val typeText: TextView = view.findViewById(R.id.challengeTypeText)
        val timeRemainingText: TextView = view.findViewById(R.id.timeRemainingText)
    }
} 