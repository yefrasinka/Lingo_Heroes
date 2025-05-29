package com.example.lingoheroesapp.utils

import android.util.Log
import com.example.lingoheroesapp.models.SuperPower
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Klasa pomocnicza do synchronizacji supermocy z bazą Firebase
 */
class SuperPowerSynchronizer {
    private val database = FirebaseDatabase.getInstance()
    private val superPowersRef = database.reference.child("superpowers")
    
    /**
     * Synchronizuje wszystkie zdefiniowane supermoce z bazą Firebase.
     * Jeśli dana supermoc nie istnieje w bazie, dodaje ją.
     */
    fun synchronizeSuperPowers() {
        // Pobierz wszystkie predefiniowane supermoce
        val allSuperPowers = SuperPower.getAllSuperPowers()
        
        // Sprawdź, które supermoce już istnieją w bazie
        superPowersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingPowerIds = mutableSetOf<String>()
                
                // Zbierz ID istniejących supermocy
                for (superPowerSnapshot in snapshot.children) {
                    val id = superPowerSnapshot.child("id").getValue(String::class.java)
                    if (id != null) {
                        existingPowerIds.add(id)
                    }
                }
                
                // Dodaj brakujące supermoce
                for (superPower in allSuperPowers) {
                    if (!existingPowerIds.contains(superPower.id)) {
                        superPowersRef.child(superPower.id).setValue(superPower)
                            .addOnSuccessListener {
                                Log.d("SuperPowerSynchronizer", "Dodano supermoc: ${superPower.name}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("SuperPowerSynchronizer", "Błąd podczas dodawania supermocy: ${e.message}")
                            }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("SuperPowerSynchronizer", "Błąd synchronizacji supermocy: ${error.message}")
            }
        })
    }
    
    /**
     * Aktualizuje wszystkie supermoce w bazie Firebase
     */
    fun updateAllSuperPowers() {
        val allSuperPowers = SuperPower.getAllSuperPowers()
        
        for (superPower in allSuperPowers) {
            superPowersRef.child(superPower.id).setValue(superPower)
                .addOnSuccessListener {
                    Log.d("SuperPowerSynchronizer", "Zaktualizowano supermoc: ${superPower.name}")
                }
                .addOnFailureListener { e ->
                    Log.e("SuperPowerSynchronizer", "Błąd podczas aktualizacji supermocy: ${e.message}")
                }
        }
    }
    
    companion object {
        // Singleton instance
        private var instance: SuperPowerSynchronizer? = null
        
        fun getInstance(): SuperPowerSynchronizer {
            if (instance == null) {
                instance = SuperPowerSynchronizer()
            }
            return instance!!
        }
    }
} 