package com.example.lingoheroesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Challenge
import com.example.lingoheroesapp.models.ChallengeType
import java.util.concurrent.TimeUnit

class ChallengesAdapter : RecyclerView.Adapter<ChallengesAdapter.ChallengeViewHolder>() {
    private var challenges: List<Challenge> = emptyList()

    fun updateChallenges(newChallenges: List<Challenge>) {
        challenges = newChallenges
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.challenge_item, parent, false)
        return ChallengeViewHolder(view)
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
        
        // Ustawienie pozostałego czasu
        val timeRemaining = challenge.expiresAt - System.currentTimeMillis()
        if (timeRemaining > 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60
            holder.timeRemainingText.text = "Pozostało: ${hours}h ${minutes}m"
        } else {
            holder.timeRemainingText.text = "Wygasło"
        }
    }

    override fun getItemCount() = challenges.size

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