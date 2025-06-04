package com.example.lingoheroesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.SuperPower
import com.example.lingoheroesapp.models.SuperPowerDifficulty

class SuperPowerAdapter(
    private var superPowers: List<SuperPower>,
    private val onSuperPowerSelected: (SuperPower) -> Unit
) : RecyclerView.Adapter<SuperPowerAdapter.SuperPowerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperPowerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_superpower, parent, false)
        return SuperPowerViewHolder(view)
    }

    override fun getItemCount(): Int = superPowers.size

    override fun onBindViewHolder(holder: SuperPowerViewHolder, position: Int) {
        val superPower = superPowers[position]
        holder.bind(superPower)
    }

    fun updateSuperPowers(newSuperPowers: List<SuperPower>) {
        superPowers = newSuperPowers
        notifyDataSetChanged()
    }

    inner class SuperPowerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val superPowerIcon: ImageView = itemView.findViewById(R.id.superPowerIcon)
        private val superPowerName: TextView = itemView.findViewById(R.id.superPowerName)
        private val superPowerDescription: TextView = itemView.findViewById(R.id.superPowerDescription)
        private val selectButton: Button = itemView.findViewById(R.id.selectButton)

        fun bind(superPower: SuperPower) {
            superPowerName.text = superPower.name
            superPowerDescription.text = superPower.description

            // Set icon based on superpower difficulty
            val iconResource = when (superPower.difficulty) {
                SuperPowerDifficulty.EASY -> R.drawable.ic_easy_power // Replace with actual icon resources
                SuperPowerDifficulty.MEDIUM -> R.drawable.ic_medium_power
                SuperPowerDifficulty.HARD -> R.drawable.ic_hard_power
            }
            
            // Fallback to a default icon if the resource doesn't exist
            try {
                superPowerIcon.setImageResource(iconResource)
            } catch (e: Exception) {
                superPowerIcon.setImageResource(R.drawable.ic_player_avatar)
            }

            // Set background color based on superpower difficulty
            val cardBackground = when (superPower.difficulty) {
                SuperPowerDifficulty.EASY -> R.color.easy_power_bg
                SuperPowerDifficulty.MEDIUM -> R.color.medium_power_bg
                SuperPowerDifficulty.HARD -> R.color.hard_power_bg
            }
            
            // Apply background tint if resource exists
            try {
                itemView.setBackgroundResource(cardBackground)
            } catch (e: Exception) {
                // Default background
            }

            selectButton.setOnClickListener {
                onSuperPowerSelected(superPower)
            }
        }
    }
} 