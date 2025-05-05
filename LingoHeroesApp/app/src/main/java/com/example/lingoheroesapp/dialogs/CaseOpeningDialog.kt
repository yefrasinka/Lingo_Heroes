package com.example.lingoheroesapp.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Case
import com.example.lingoheroesapp.models.CaseItem
import com.example.lingoheroesapp.models.CaseItemType
import com.example.lingoheroesapp.models.ItemRarity

/**
 * Dialog animacji otwierania skrzynki i wyświetlania nagrody
 */
class CaseOpeningDialog(
    context: Context,
    private val case: Case,
    private val rewardItem: CaseItem,
    private val onDismissed: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_case_opening)
        setCancelable(false)
        
        // Ustaw szerokość okna dialogowego
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Inicjalizacja widoków
        val caseImage = findViewById<ImageView>(R.id.caseImage)
        val caseTitle = findViewById<TextView>(R.id.caseTitle)
        val itemImage = findViewById<ImageView>(R.id.itemImage)
        val rarityIndicator = findViewById<View>(R.id.rarityIndicator)
        val itemNameText = findViewById<TextView>(R.id.itemNameText)
        val itemDescriptionText = findViewById<TextView>(R.id.itemDescriptionText)
        val closeButton = findViewById<Button>(R.id.closeButton)
        
        // Ustaw tytuł skrzynki
        caseTitle.text = case.name
        
        // Ustaw ikony skrzynki i przedmiotu
        caseImage.setImageResource(getCaseImageResource())
        itemImage.setImageResource(getItemImageResource())
        
        // Ustaw kolor wskaźnika rzadkości
        rarityIndicator.setBackgroundColor(getRarityColor())
        
        // Ustaw nazwę i opis przedmiotu
        itemNameText.text = getDisplayNameForItem()
        itemDescriptionText.text = rewardItem.description
        
        // Animacja otwierania skrzynki
        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        val slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        
        // Najpierw pokaż skrzynkę
        caseImage.visibility = View.VISIBLE
        itemImage.visibility = View.INVISIBLE
        rarityIndicator.visibility = View.INVISIBLE
        itemNameText.visibility = View.INVISIBLE
        itemDescriptionText.visibility = View.INVISIBLE
        
        // Po 1.5 sekundy rozpocznij animację otwierania
        Handler(Looper.getMainLooper()).postDelayed({
            caseImage.startAnimation(fadeOut)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                
                override fun onAnimationEnd(animation: Animation?) {
                    caseImage.visibility = View.INVISIBLE
                    
                    // Pokaż przedmiot z animacją
                    itemImage.visibility = View.VISIBLE
                    rarityIndicator.visibility = View.VISIBLE
                    itemNameText.visibility = View.VISIBLE
                    itemDescriptionText.visibility = View.VISIBLE
                    
                    itemImage.startAnimation(slideUp)
                    rarityIndicator.startAnimation(fadeIn)
                    itemNameText.startAnimation(fadeIn)
                    itemDescriptionText.startAnimation(fadeIn)
                }
                
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }, 1500)
        
        // Przycisk zamknięcia
        closeButton.setOnClickListener {
            dismiss()
            onDismissed()
        }
    }
    
    private fun getCaseImageResource(): Int {
        return when (case.rarity) {
            com.example.lingoheroesapp.models.CaseRarity.STANDARD -> R.drawable.ic_bohater
            com.example.lingoheroesapp.models.CaseRarity.PREMIUM -> R.drawable.ic_trophy
            com.example.lingoheroesapp.models.CaseRarity.ELITE -> R.drawable.stage_icon
        }
    }
    
    private fun getItemImageResource(): Int {
        // Wybierz odpowiednią ikonę w zależności od typu przedmiotu
        return when (rewardItem.type) {
            CaseItemType.COIN -> R.drawable.ic_heart // Używamy tymczasowo ikony serca zamiast monet
            CaseItemType.ARMOR -> R.drawable.ic_armor_silver
            CaseItemType.ARMOR_TIER -> {
                // Wybierz ikonę w zależności od poziomu zbroi
                when (rewardItem.armorTier) {
                    "BRONZE" -> R.drawable.ic_armor_bronze
                    "SILVER" -> R.drawable.ic_armor_silver
                    "GOLD" -> R.drawable.ic_armor_silver // Użyj srebrnej ikony tymczasowo dla złota
                    else -> R.drawable.ic_armor_bronze
                }
            }
            CaseItemType.WEAPON -> R.drawable.ic_staff_fire
            CaseItemType.SPECIAL -> R.drawable.ic_sword // Używamy ikony miecza dla przedmiotów specjalnych
        }
    }
    
    private fun getRarityColor(): Int {
        // Zwróć kolor w zależności od rzadkości przedmiotu
        return when (rewardItem.rarity) {
            ItemRarity.COMMON -> Color.parseColor("#7E7E7E") // szary
            ItemRarity.UNCOMMON -> Color.parseColor("#2E7D32") // zielony
            ItemRarity.RARE -> Color.parseColor("#1565C0") // niebieski
            ItemRarity.EPIC -> Color.parseColor("#6A1B9A") // fioletowy
            ItemRarity.LEGENDARY -> Color.parseColor("#FF6F00") // pomarańczowy
        }
    }
    
    /**
     * Generuje nazwę wyświetlaną dla przedmiotu, dodając wartość dla monet
     */
    private fun getDisplayNameForItem(): String {
        return when (rewardItem.type) {
            CaseItemType.COIN -> "${rewardItem.name} (${rewardItem.value})"
            CaseItemType.ARMOR_TIER -> {
                val tierName = when (rewardItem.armorTier) {
                    "BRONZE" -> "Brązowa"
                    "SILVER" -> "Srebrna" 
                    "GOLD" -> "Złota"
                    else -> ""
                }
                
                // Pokaż liczbę sztuk zbroi, jeśli jest większa niż 1
                val countPrefix = if (rewardItem.value > 1) "${rewardItem.value}x " else ""
                
                // Określ, ile brakuje do awansu
                val maxPerTier = 10
                val progressText = if (rewardItem.armorTier == "GOLD") {
                    "∞" // Złoty poziom nie ma limitu
                } else {
                    "$maxPerTier"
                }
                
                "$countPrefix$tierName zbroja (+${rewardItem.value}/$progressText)"
            }
            else -> rewardItem.name
        }
    }
} 