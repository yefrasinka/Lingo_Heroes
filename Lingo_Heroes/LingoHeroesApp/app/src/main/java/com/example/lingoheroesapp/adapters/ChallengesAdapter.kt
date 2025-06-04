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
        Log.d("ChallengesAdapter", "Aktualizacja listy wyzwań, liczba elementów: ${newChallenges.size}")
        for (challenge in newChallenges) {
            Log.d("ChallengesAdapter", "Status wyzwania ${challenge.id} (${challenge.title}): ukończone=${challenge.isCompleted}, nagroda odebrana=${challenge.isRewardClaimed}")
        }
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
                holder.timeRemainingText.text = "NAGRODA ODEBRANA ✅"
                holder.timeRemainingText.setTextColor(Color.GREEN)
                holder.timeRemainingText.textSize = 14f
                holder.itemView.isClickable = false
                holder.itemView.setBackgroundResource(android.R.color.transparent)
                
                // Upewnijmy się, że nie mamy przypisanego clickListenera
                holder.itemView.setOnClickListener(null)
                
                Log.d("ChallengesAdapter", "Wyzwanie ${challenge.id} (${challenge.title}) - NAGRODA ODEBRANA")
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
                    Log.d("ChallengesAdapter", "Kliknięto na wyzwanie ${challenge.id} (${challenge.title}) aby odebrać nagrodę")
                    // Wywołujemy bezpośrednio naszą metodę, pomijając listener
                    awardChallengeReward(challenge)
                }
                
                Log.d("ChallengesAdapter", "Wyzwanie ${challenge.id} (${challenge.title}) - DO ODEBRANIA")
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
            
            // Upewnijmy się, że nie mamy przypisanego clickListenera
            holder.itemView.setOnClickListener(null)
            
            Log.d("ChallengesAdapter", "Wyzwanie ${challenge.id} (${challenge.title}) - W TRAKCIE")
        } else {
            // Wyzwanie wygasło
            holder.itemView.alpha = 0.5f
            holder.timeRemainingText.text = "Wygasło"
            holder.timeRemainingText.setTextColor(Color.RED)
            holder.timeRemainingText.textSize = 14f
            holder.timeRemainingText.isAllCaps = false
            holder.itemView.isClickable = false
            holder.itemView.setBackgroundResource(android.R.color.transparent)
            
            // Upewnijmy się, że nie mamy przypisanego clickListenera
            holder.itemView.setOnClickListener(null)
            
            Log.d("ChallengesAdapter", "Wyzwanie ${challenge.id} (${challenge.title}) - WYGASŁO")
        }
    }

    override fun getItemCount() = challenges.size
    
    private fun awardChallengeReward(challenge: Challenge) {
        try {
            Log.d("ChallengesAdapter", "=== FUNKCJA awardChallengeReward WYWOŁANA ===")
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e("ChallengesAdapter", "Brak zalogowanego użytkownika")
                return
            }
            
            val userId = currentUser.uid
            Log.d("ChallengesAdapter", "User ID: $userId")
            
            val userRef = database.getReference("users").child(userId)
            val challengeRef = userRef.child("challenges").child(challenge.id)
            
            // Zapamiętaj kontekst aktualnego adaptera
            val adapterContext = challengeHolders.firstOrNull()?.itemView?.context
            
            // Dodajemy logi
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
            
            // Najprostsze rozwiązanie - bezpośrednio aktualizujemy wartości w bazie danych
            userRef.child("coins").get().addOnSuccessListener { coinsSnapshot ->
                val currentCoins = coinsSnapshot.getValue(Int::class.java) ?: 0
                Log.d("ChallengesAdapter", "Aktualna liczba monet użytkownika: $currentCoins")
                
                // Dodaj monety do konta użytkownika
                val newCoins = currentCoins + challenge.reward.coins
                Log.d("ChallengesAdapter", "Nowa liczba monet: $newCoins")
                
                userRef.child("coins").setValue(newCoins)
                    .addOnSuccessListener {
                        Log.d("ChallengesAdapter", "Monety dodane pomyślnie")
                        
                        // Oznacz wyzwanie jako odebrane
                        challengeRef.child("isRewardClaimed").setValue(true)
                        challengeRef.child("rewardClaimed").setValue(true)
                            .addOnSuccessListener {
                                Log.d("ChallengesAdapter", "Status nagrody zaktualizowany pomyślnie")
                                
                                // Wyświetl powiadomienie
                                adapterContext?.let {
                                    Toast.makeText(
                                        it,
                                        "Otrzymałeś ${challenge.reward.coins} monet za ukończenie wyzwania: ${challenge.title}!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                
                                // Aktualizuj lokalny model
                                val updatedChallenge = challenge.copy(isRewardClaimed = true)
                                val updatedChallenges = challenges.toMutableList()
                                val index = updatedChallenges.indexOfFirst { it.id == challenge.id }
                                if (index != -1) {
                                    updatedChallenges[index] = updatedChallenge
                                    updateChallenges(updatedChallenges)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChallengesAdapter", "Błąd przy aktualizacji statusu nagrody: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChallengesAdapter", "Błąd przy dodawaniu monet: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                Log.e("ChallengesAdapter", "Błąd przy pobieraniu liczby monet: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("ChallengesAdapter", "Wyjątek podczas przyznawania nagrody: ${e.message}")
            e.printStackTrace()
        }
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