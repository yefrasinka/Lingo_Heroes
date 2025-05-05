package com.example.lingoheroesapp.utils

import android.util.Log
import com.example.lingoheroesapp.models.Case
import com.example.lingoheroesapp.models.CaseItem
import com.example.lingoheroesapp.models.CaseItemType
import com.example.lingoheroesapp.models.CaseRarity
import com.example.lingoheroesapp.models.ItemRarity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID
import java.util.concurrent.CountDownLatch

/**
 * Klasa pomocnicza do inicjalizacji danych skrzynek w Firebase
 */
class FirebaseInitializer {
    private val TAG = "FirebaseInitializer"

    /**
     * Inicjalizuje przykładowe skrzynki w Firebase
     */
    fun initializeCases() {
        try {
            val database = FirebaseDatabase.getInstance()
            val casesRef = database.getReference("cases")
            
            // Bezpośrednio tworzymy i zapisujemy skrzynki bez sprawdzania
            Log.d(TAG, "Rozpoczynam bezpośrednie tworzenie skrzynek")
            
            // Najpierw usuwamy wszystkie istniejące skrzynki, a potem dodajemy nowe
            casesRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Pomyślnie usunięto poprzednie skrzynki")
                    // Tworzymy nowe skrzynki z opóźnieniem
                    saveStandardCase(casesRef)
                } else {
                    Log.e(TAG, "Błąd podczas usuwania poprzednich skrzynek", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas inicjalizacji skrzynek", e)
        }
    }
    
    /**
     * Zapisuje standardową skrzynkę, a po jej zapisaniu zapisuje premium
     */
    private fun saveStandardCase(casesRef: DatabaseReference) {
        val standardCase = createStandardCase()
        
        casesRef.child("standard_case").setValue(standardCase)
            .addOnSuccessListener { 
                Log.d(TAG, "SUKCES: Standard case zapisany") 
                // Po zapisaniu standardowej skrzynki zapisujemy premium
                savePremiumCase(casesRef)
            }
            .addOnFailureListener { 
                Log.e(TAG, "BŁĄD podczas zapisywania standardowej skrzynki", it) 
                // Mimo błędu próbujemy zapisać pozostałe skrzynki
                savePremiumCase(casesRef)
            }
    }
    
    /**
     * Zapisuje premium skrzynkę, a po jej zapisaniu zapisuje elite
     */
    private fun savePremiumCase(casesRef: DatabaseReference) {
        val premiumCase = createPremiumCase()
        
        casesRef.child("premium_case").setValue(premiumCase)
            .addOnSuccessListener { 
                Log.d(TAG, "SUKCES: Premium case zapisany") 
                // Po zapisaniu premium skrzynki zapisujemy elite
                saveEliteCase(casesRef)
            }
            .addOnFailureListener { 
                Log.e(TAG, "BŁĄD podczas zapisywania premium skrzynki", it) 
                // Mimo błędu próbujemy zapisać elitarną skrzynkę
                saveEliteCase(casesRef)
            }
    }
    
    /**
     * Zapisuje elitarną skrzynkę
     */
    private fun saveEliteCase(casesRef: DatabaseReference) {
        val eliteCase = createEliteCase()
        
        casesRef.child("elite_case").setValue(eliteCase)
            .addOnSuccessListener { 
                Log.d(TAG, "SUKCES: Elite case zapisany") 
                // Wszystkie skrzynki zostały zapisane
                Log.d(TAG, "Zakończono zapisywanie wszystkich skrzynek")
            }
            .addOnFailureListener { 
                Log.e(TAG, "BŁĄD podczas zapisywania elitarnej skrzynki", it) 
            }
    }
    
    /**
     * Tworzy standardową skrzynkę
     */
    private fun createStandardCase(): Case {
        val id = "standard_case"
        
        // Standardowe przedmioty
        val items = listOf(
            CaseItem(
                id = "coins_50",
                name = "50 Monet",
                description = "Niewielka ilość monet do wydania w sklepie",
                imageUrl = "",
                type = CaseItemType.COIN,
                rarity = ItemRarity.COMMON,
                value = 50,
                dropChance = 35.0
            ),
            CaseItem(
                id = "coins_100",
                name = "100 Monet",
                description = "Średnia ilość monet do wydania w sklepie",
                imageUrl = "",
                type = CaseItemType.COIN,
                rarity = ItemRarity.UNCOMMON,
                value = 100,
                dropChance = 30.0
            ),
            CaseItem(
                id = "bronze_armor_piece",
                name = "Brązowa zbroja",
                description = "Fragment brązowej zbroi. Zbierz 10 sztuk, aby awansować na srebrny poziom.",
                imageUrl = "",
                type = CaseItemType.ARMOR_TIER,
                rarity = ItemRarity.UNCOMMON,
                value = 1,
                dropChance = 20.0,
                armorTier = "BRONZE"
            ),
            CaseItem(
                id = "simple_armor",
                name = "Prosta zbroja",
                description = "Fragment brązowej zbroi. Dodaje +1 do licznika brązowych zbroi.",
                imageUrl = "",
                type = CaseItemType.ARMOR,
                rarity = ItemRarity.UNCOMMON,
                value = 1,
                dropChance = 15.0
            )
        )
        
        return Case(
            id = id,
            name = "Podstawowa skrzynia",
            description = "Zawiera monety i elementy zbroi. Idealna dla początkujących graczy.",
            imageUrl = "",
            price = 200,
            items = items,
            rarity = CaseRarity.STANDARD
        )
    }
    
    /**
     * Tworzy skrzynkę premium
     */
    private fun createPremiumCase(): Case {
        val id = "premium_case"
        
        // Przedmioty premium
        val items = listOf(
            CaseItem(
                id = "coins_100",
                name = "100 Monet",
                description = "Średnia ilość monet do wydania w sklepie",
                imageUrl = "",
                type = CaseItemType.COIN,
                rarity = ItemRarity.COMMON,
                value = 100,
                dropChance = 30.0
            ),
            CaseItem(
                id = "coins_250",
                name = "250 Monet",
                description = "Duża ilość monet do wydania w sklepie",
                imageUrl = "",
                type = CaseItemType.COIN,
                rarity = ItemRarity.UNCOMMON,
                value = 250,
                dropChance = 25.0
            ),
            CaseItem(
                id = "bronze_armor_piece",
                name = "Brązowa zbroja",
                description = "Fragment brązowej zbroi. Zbierz 10 sztuk, aby awansować na srebrny poziom.",
                imageUrl = "",
                type = CaseItemType.ARMOR_TIER,
                rarity = ItemRarity.UNCOMMON,
                value = 1,
                dropChance = 20.0,
                armorTier = "BRONZE"
            ),
            CaseItem(
                id = "silver_armor_piece",
                name = "Srebrna zbroja",
                description = "Fragment srebrnej zbroi. Zbierz 10 sztuk, aby awansować na złoty poziom.",
                imageUrl = "",
                type = CaseItemType.ARMOR_TIER,
                rarity = ItemRarity.RARE,
                value = 1,
                dropChance = 20.0,
                armorTier = "SILVER"
            ),
            CaseItem(
                id = "epic_armor",
                name = "Epicka zbroja",
                description = "Dodaje +1 do licznika srebrnych zbroi. Pomaga w awansie na złoty poziom.",
                imageUrl = "",
                type = CaseItemType.ARMOR,
                rarity = ItemRarity.EPIC,
                value = 1,
                dropChance = 5.0
            )
        )
        
        return Case(
            id = id,
            name = "Skrzynia premium",
            description = "Zawiera lepsze przedmioty i większe ilości monet. Szansa na srebrne elementy zbroi.",
            imageUrl = "",
            price = 500,
            items = items,
            rarity = CaseRarity.PREMIUM
        )
    }
    
    /**
     * Tworzy elitarną skrzynkę
     */
    private fun createEliteCase(): Case {
        val id = "elite_case"
        
        // Elitarne przedmioty
        val items = listOf(
            CaseItem(
                id = "coins_500",
                name = "500 Monet",
                description = "Ogromna ilość monet do wydania w sklepie",
                imageUrl = "",
                type = CaseItemType.COIN,
                rarity = ItemRarity.RARE,
                value = 500,
                dropChance = 15.0
            ),
            CaseItem(
                id = "silver_armor_piece_x2",
                name = "2x Srebrna zbroja",
                description = "Dwa fragmenty srebrnej zbroi. Zbliża cię do złotego poziomu.",
                imageUrl = "",
                type = CaseItemType.ARMOR_TIER,
                rarity = ItemRarity.RARE,
                value = 2,
                dropChance = 30.0,
                armorTier = "SILVER"
            ),
            CaseItem(
                id = "gold_armor_piece",
                name = "Złota zbroja",
                description = "Fragment złotej zbroi. Dodaje +1 do licznika złotych zbroi.",
                imageUrl = "",
                type = CaseItemType.ARMOR_TIER,
                rarity = ItemRarity.EPIC,
                value = 1,
                dropChance = 25.0,
                armorTier = "GOLD"
            ),
            CaseItem(
                id = "epic_armor",
                name = "Epicka zbroja",
                description = "Dodaje +1 do licznika srebrnych zbroi. Pomaga w awansie na złoty poziom.",
                imageUrl = "",
                type = CaseItemType.ARMOR,
                rarity = ItemRarity.EPIC,
                value = 1,
                dropChance = 15.0
            ),
            CaseItem(
                id = "legendary_armor",
                name = "Legendarna zbroja",
                description = "Dodaje +1 do licznika złotych zbroi. Bardzo rzadka nagroda!",
                imageUrl = "",
                type = CaseItemType.ARMOR,
                rarity = ItemRarity.LEGENDARY,
                value = 1,
                dropChance = 15.0
            )
        )
        
        return Case(
            id = id,
            name = "Elitarna skrzynia",
            description = "Zawiera epickie i legendarne przedmioty. Najlepsza szansa na złote elementy zbroi!",
            imageUrl = "",
            price = 1000,
            items = items,
            rarity = CaseRarity.ELITE
        )
    }
} 