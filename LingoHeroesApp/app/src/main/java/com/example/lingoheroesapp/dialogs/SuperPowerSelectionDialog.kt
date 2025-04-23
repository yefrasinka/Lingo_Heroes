package com.example.lingoheroesapp.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.SuperPowerAdapter
import com.example.lingoheroesapp.models.SuperPower
import com.example.lingoheroesapp.models.SuperPowerDifficulty
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log
import kotlin.random.Random

class SuperPowerSelectionDialog(
    context: Context,
    private val correctAnswers: Int,
    private val onSuperPowerSelected: (SuperPower) -> Unit
) : Dialog(context) {

    private val superPowerAdapter: SuperPowerAdapter
    private val availableSuperPowers = mutableListOf<SuperPower>()
    private val random = Random(System.currentTimeMillis())

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_superpower_selection)
        setCancelable(true)

        // Find views
        val recyclerView = findViewById<RecyclerView>(R.id.superPowerRecyclerView)
        val closeButton = findViewById<Button>(R.id.closeButton)
        val infoText = findViewById<TextView>(R.id.superPowerDialogInfo)

        // Update info text based on correct answers
        infoText.text = when {
            correctAnswers >= 9 -> "Masz ${correctAnswers} poprawnych odpowiedzi! Wybierz jedną z potężnych supermocy:"
            correctAnswers >= 6 -> "Masz ${correctAnswers} poprawnych odpowiedzi! Wybierz jedną ze średnich supermocy:"
            else -> "Masz ${correctAnswers} poprawnych odpowiedzi! Wybierz jedną z dostępnych supermocy:"
        }

        // Set up recycler view
        superPowerAdapter = SuperPowerAdapter(emptyList()) { selectedSuperPower ->
            // Sprawdź czy supermoc jest odpowiednia dla liczby poprawnych odpowiedzi
            val canUseSuperPower = when (selectedSuperPower.difficulty) {
                SuperPowerDifficulty.HARD -> correctAnswers >= 9
                SuperPowerDifficulty.MEDIUM -> correctAnswers >= 6
                SuperPowerDifficulty.EASY -> true
            }
            
            if (canUseSuperPower) {
                onSuperPowerSelected(selectedSuperPower)
                dismiss()
            } else {
                // Pokaż komunikat o niespełnieniu wymagań (opcjonalnie)
                val requiredAnswers = when (selectedSuperPower.difficulty) {
                    SuperPowerDifficulty.HARD -> 9
                    SuperPowerDifficulty.MEDIUM -> 6
                    SuperPowerDifficulty.EASY -> 3
                }
                
                val infoView = findViewById<TextView>(R.id.superPowerDialogInfo)
                infoView.text = "Potrzebujesz $requiredAnswers poprawnych odpowiedzi, aby użyć tej supermocy!"
                infoView.setTextColor(context.resources.getColor(R.color.incorrect_red, context.theme))
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = superPowerAdapter
        }

        // Set close button listener
        closeButton.setOnClickListener {
            dismiss()
        }

        // Load superpowers from Firebase or fallback to local predefined ones
        loadSuperPowers()
    }

    private fun loadSuperPowers() {
        val database = FirebaseDatabase.getInstance()
        val superPowersRef = database.reference.child("superpowers")

        superPowersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    // Load from Firebase
                    try {
                        // Pobieramy wszystkie supermoce i grupujemy je według trudności
                        val allPowers = snapshot.children.mapNotNull { it.getValue(SuperPower::class.java) }
                        
                        // Filtrujemy supermoce według trudności
                        val easyPowers = allPowers.filter { it.difficulty == SuperPowerDifficulty.EASY }
                        val mediumPowers = allPowers.filter { it.difficulty == SuperPowerDifficulty.MEDIUM }
                        val hardPowers = allPowers.filter { it.difficulty == SuperPowerDifficulty.HARD }
                        
                        // Wybieramy supermoce dla każdego poziomu trudności
                        val selectedEasy = easyPowers.shuffled().take(if (easyPowers.size > 2) 2 else easyPowers.size)
                        val selectedMedium = if (correctAnswers >= 6) {
                            mediumPowers.shuffled().take(if (mediumPowers.size > 2) 2 else mediumPowers.size)
                        } else emptyList()
                        val selectedHard = if (correctAnswers >= 9) {
                            hardPowers.shuffled().take(if (hardPowers.size > 2) 2 else hardPowers.size)
                        } else emptyList()
                        
                        // Łączymy wybrane supermoce
                        val selectedPowers = selectedEasy + selectedMedium + selectedHard
                        
                        if (selectedPowers.isNotEmpty()) {
                            availableSuperPowers.clear()
                            availableSuperPowers.addAll(selectedPowers)
                            superPowerAdapter.updateSuperPowers(availableSuperPowers)
                            
                            // Log dla debugowania
                            Log.d("SuperPowerDialog", "Załadowano ${selectedPowers.size} supermocy: " +
                                   "${selectedEasy.size} łatwych, ${selectedMedium.size} średnich, ${selectedHard.size} trudnych")
                        } else {
                            loadPredefinedSuperPowers()
                        }
                    } catch (e: Exception) {
                        Log.e("SuperPowerDialog", "Błąd podczas ładowania supermocy z Firebase: ${e.message}")
                        loadPredefinedSuperPowers()
                    }
                } else {
                    loadPredefinedSuperPowers()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SuperPowerDialog", "Anulowano ładowanie supermocy: ${error.message}")
                loadPredefinedSuperPowers()
            }
        })
    }

    private fun loadPredefinedSuperPowers() {
        // Pobierz wszystkie dostępne supermoce i pokaż odpowiednie według liczby poprawnych odpowiedzi
        val easyPowers = SuperPower.getEasySuperPowers().shuffled().take(2)
        val mediumPowers = if (correctAnswers >= 6) SuperPower.getMediumSuperPowers().shuffled().take(2) else emptyList()
        val hardPowers = if (correctAnswers >= 9) SuperPower.getHardSuperPowers().shuffled().take(2) else emptyList()
        
        // Połącz wszystkie wybrane supermoce
        val selectedPowers = easyPowers + mediumPowers + hardPowers
        
        availableSuperPowers.clear()
        availableSuperPowers.addAll(selectedPowers)
        superPowerAdapter.updateSuperPowers(availableSuperPowers)
        
        // Log dla debugowania
        Log.d("SuperPowerDialog", "Załadowano ${selectedPowers.size} predefiniowanych supermocy")
    }
} 