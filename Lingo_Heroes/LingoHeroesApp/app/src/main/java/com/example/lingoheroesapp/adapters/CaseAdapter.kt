package com.example.lingoheroesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Case
import com.example.lingoheroesapp.models.CaseRarity

class CaseAdapter(
    private val cases: MutableList<Case> = mutableListOf(),
    private val onCaseClicked: (Case) -> Unit
) : RecyclerView.Adapter<CaseAdapter.CaseViewHolder>() {

    class CaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.caseImageView)
        val nameTextView: TextView = view.findViewById(R.id.caseNameTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.caseDescriptionTextView)
        val priceTextView: TextView = view.findViewById(R.id.casePriceTextView)
        val rarityTag: TextView = view.findViewById(R.id.caseRarityTag)
        val buyButton: Button = view.findViewById(R.id.buyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.case_item, parent, false)
        return CaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CaseViewHolder, position: Int) {
        val caseItem = cases[position]
        
        // Ustawienie nazwy, opisu i ceny
        holder.nameTextView.text = caseItem.name
        holder.descriptionTextView.text = caseItem.description
        holder.priceTextView.text = caseItem.price.toString()
        
        // Ustawienie tagu rzadkości
        holder.rarityTag.text = caseItem.rarity.name
        
        // Ustawienie koloru tła dla tagu rzadkości
        val rarityColor = when (caseItem.rarity) {
            CaseRarity.STANDARD -> "#4CAF50" // Zielony
            CaseRarity.PREMIUM -> "#2196F3"  // Niebieski
            CaseRarity.ELITE -> "#E91E63"    // Różowy
        }
        holder.rarityTag.setBackgroundColor(android.graphics.Color.parseColor(rarityColor))

        val caseImage = when (caseItem.rarity) {
            CaseRarity.STANDARD -> (R.drawable.wood_chest)
            CaseRarity.PREMIUM -> (R.drawable.golden_chest)
            CaseRarity.ELITE -> (R.drawable.elite_chest)
        }
        holder.imageView.setImageResource(caseImage)
        
        // Obsługa kliknięcia przycisku zakupu
        holder.buyButton.setOnClickListener {
            onCaseClicked(caseItem)
        }
        
        // Obsługa kliknięcia całego widoku
        holder.itemView.setOnClickListener {
            onCaseClicked(caseItem)
        }
    }

    override fun getItemCount() = cases.size

    fun updateCases(newCases: List<Case>) {
        cases.clear()
        cases.addAll(newCases)
        notifyDataSetChanged()
    }
} 