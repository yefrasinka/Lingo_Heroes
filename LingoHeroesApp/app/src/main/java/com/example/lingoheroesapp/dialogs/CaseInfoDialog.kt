package com.example.lingoheroesapp.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.CaseItemAdapter
import com.example.lingoheroesapp.models.Case
import com.example.lingoheroesapp.models.CaseRarity
import com.google.android.material.button.MaterialButton
import android.widget.ImageView
import android.widget.TextView

/**
 * Dialog wyświetlający informacje o skrzynce
 */
class CaseInfoDialog(
    context: Context,
    private val case: Case,
    private val onBuyClicked: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_case_info)
        
        // Ustawienie przezroczystego tła i pełnego wymiary
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Inicjalizacja widoków
        val titleTextView = findViewById<TextView>(R.id.dialogTitleTextView)
        val descriptionTextView = findViewById<TextView>(R.id.dialogDescriptionTextView)
        val caseImageView = findViewById<ImageView>(R.id.dialogCaseImageView)
        val itemsRecyclerView = findViewById<RecyclerView>(R.id.dialogItemsRecyclerView)
        val buyButton = findViewById<MaterialButton>(R.id.buyNowButton)
        
        // Ustawienie danych skrzynki
        titleTextView.text = case.name
        descriptionTextView.text = case.description
        
        // Wczytanie obrazka skrzynki
        val imageRes = when (case.rarity) {
            CaseRarity.ELITE -> R.drawable.elite_chest
            CaseRarity.PREMIUM -> R.drawable.golden_chest
            CaseRarity.STANDARD -> R.drawable.wood_chest
        }

        caseImageView.setImageResource(imageRes)
        
        // Ustawienie listy przedmiotów
        val itemsAdapter = CaseItemAdapter(case.items)
        itemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = itemsAdapter
        }
        
        // Ustawienie tekstu przycisku zakupu z ceną
        buyButton.text = "Kup teraz za ${case.price} monet"
        
        // Obsługa kliknięcia przycisku zakupu
        buyButton.setOnClickListener {
            dismiss()
            onBuyClicked()
        }
    }
} 