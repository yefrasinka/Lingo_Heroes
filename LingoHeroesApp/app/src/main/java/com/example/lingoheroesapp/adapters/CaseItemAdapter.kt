package com.example.lingoheroesapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.CaseItem
import com.example.lingoheroesapp.models.CaseItemType
import com.example.lingoheroesapp.models.ItemRarity

class CaseItemAdapter(
    private val items: List<CaseItem>
) : RecyclerView.Adapter<CaseItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.itemNameTextView)
        val rarityTextView: TextView = view.findViewById(R.id.itemRarityTextView)
        val chanceTextView: TextView = view.findViewById(R.id.itemChanceTextView)
        val itemImageView: ImageView = view.findViewById(R.id.itemImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.case_content_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        
        // Ustawienie nazwy i szansy
        holder.nameTextView.text = item.name
        holder.rarityTextView.text = item.rarity.name
        holder.chanceTextView.text = "${item.dropChance}%"
        
        // Ustawienie koloru tekstu rzadkości
        val rarityColor = when (item.rarity) {
            ItemRarity.COMMON -> "#757575"        // Szary
            ItemRarity.UNCOMMON -> "#4CAF50"      // Zielony
            ItemRarity.RARE -> "#2196F3"          // Niebieski
            ItemRarity.EPIC -> "#9C27B0"          // Fioletowy
            ItemRarity.LEGENDARY -> "#FF9800"     // Pomarańczowy
        }
        holder.rarityTextView.setTextColor(Color.parseColor(rarityColor))
        
        // Załadowanie obrazka przedmiotu
        if (item.imageUrl.isNotEmpty()) {
            try {
                Glide.with(holder.itemImageView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.itemImageView)
            } catch (e: Exception) {
                // Fallback w przypadku błędu z Glide
                holder.itemImageView.setImageResource(getDefaultImageResource(item))
            }
        } else {
            // Domyślny obrazek w zależności od typu
            holder.itemImageView.setImageResource(getDefaultImageResource(item))
        }
    }
    
    private fun getDefaultImageResource(item: CaseItem): Int {
        return when (item.type) {
            CaseItemType.COIN -> R.drawable.ic_coin // Używamy ikony serca zamiast monety
            CaseItemType.ARMOR -> R.drawable.ic_armor_silver
            CaseItemType.WEAPON -> R.drawable.ic_staff_fire
            CaseItemType.SPECIAL -> R.drawable.ic_trophy
            CaseItemType.ARMOR_TIER -> {
                // Wybieramy odpowiednią ikonę w zależności od poziomu zbroi
                when (item.armorTier) {
                    "BRONZE" -> R.drawable.ic_armor_bronze
                    "SILVER" -> R.drawable.ic_armor_silver
                    "GOLD" -> R.drawable.ic_armor_silver // Tymczasowo używamy srebrnej ikony dla złota
                    else -> R.drawable.ic_armor_bronze
                }
            }
        }
    }

    override fun getItemCount() = items.size
} 