package com.example.lingoheroesapp.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Achievement
import com.example.lingoheroesapp.models.AchievementType

class AchievementsAdapter : RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {
    private var achievements: List<Achievement> = listOf()

    fun updateAchievements(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.achievement_item, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        holder.bind(achievement)
    }

    override fun getItemCount(): Int = achievements.size

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.achievementIcon)
        private val title: TextView = itemView.findViewById(R.id.achievementTitle)
        private val description: TextView = itemView.findViewById(R.id.achievementDescription)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.achievementProgress)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)
        private val completionStatus: ImageView = itemView.findViewById(R.id.completionStatus)

        fun bind(achievement: Achievement) {
            title.text = achievement.title
            description.text = achievement.description
            
            // Set progress
            progressBar.max = achievement.requiredValue
            val limitedProgress = minOf(achievement.progress, achievement.requiredValue)
            progressBar.progress = limitedProgress
            progressText.text = "${limitedProgress}/${achievement.requiredValue}"

            // Show completion status if achieved
            completionStatus.visibility = if (achievement.isUnlocked) View.VISIBLE else View.GONE

            // Set colors based on unlock status
            val color = if (achievement.isUnlocked) {
                itemView.context.getColor(R.color.purple_500)
            } else {
                itemView.context.getColor(R.color.color_gray)
            }

            // Apply colors
            title.setTextColor(color)
            icon.setColorFilter(color)
            progressBar.progressTintList = ColorStateList.valueOf(color)

            // Set icon based on achievement type
            val iconResource = when (achievement.type) {
                AchievementType.STREAK_DAYS -> R.drawable.ic_streak
                AchievementType.PERFECT_SCORES -> R.drawable.ic_perfect_score
                AchievementType.TASKS_COMPLETED -> R.drawable.ic_tasks
                AchievementType.XP -> R.drawable.ic_trophy
                AchievementType.LEVEL -> R.drawable.ic_trophy
            }
            icon.setImageResource(iconResource)
        }
    }
} 