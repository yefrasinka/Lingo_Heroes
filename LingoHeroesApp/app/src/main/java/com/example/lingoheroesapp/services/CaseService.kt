package com.example.lingoheroesapp.services

import android.util.Log
import com.example.lingoheroesapp.models.ArmorTier
import com.example.lingoheroesapp.models.Case
import com.example.lingoheroesapp.models.CaseItem
import com.example.lingoheroesapp.models.CaseItemType
import com.example.lingoheroesapp.models.Equipment
import com.example.lingoheroesapp.models.ItemRarity
import com.example.lingoheroesapp.utils.FirebaseInitializer
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

/**
 * Serwis do obsługi skrzynek w sklepie
 */
class CaseService {
    private val TAG = "CaseService"
    private val database = FirebaseDatabase.getInstance().reference

    /**
     * Pobiera listę dostępnych skrzynek z Firebase
     */
    fun getCases(callback: (List<Case>) -> Unit) {
        Log.d(TAG, "Rozpoczynam pobieranie skrzynek z Firebase")
        database.child("cases").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val casesList = mutableListOf<Case>()
                
                Log.d(TAG, "Otrzymano dane z Firebase, liczba skrzynek: ${snapshot.childrenCount}")
                
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    Log.e(TAG, "Brak skrzynek w bazie danych! Tworzę nowe skrzynki...")
                    
                    // Jeśli w bazie nie ma skrzynek, tworzymy je na miejscu
                    createCasesAndRetry(callback)
                    return
                }
                
                for (caseSnapshot in snapshot.children) {
                    try {
                        Log.d(TAG, "Próbuję sparsować skrzynkę: ${caseSnapshot.key}")
                        val case = caseSnapshot.getValue(Case::class.java)
                        if (case != null) {
                            casesList.add(case)
                            Log.d(TAG, "Dodano skrzynkę: ${case.name}")
                        } else {
                            Log.e(TAG, "Nie udało się odczytać skrzynki: ${caseSnapshot.key}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Błąd podczas parsowania skrzynki: ${caseSnapshot.key}", e)
                    }
                }
                
                Log.d(TAG, "Zakończono pobieranie skrzynek, znaleziono: ${casesList.size}")
                
                if (casesList.isEmpty()) {
                    // Jeśli wciąż mamy pustą listę, spróbujmy stworzyć skrzynki i załadować je ponownie
                    createCasesAndRetry(callback)
                } else {
                    callback(casesList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas pobierania skrzynek: ${error.message}", error.toException())
                callback(emptyList())
            }
        })
    }
    
    /**
     * Tworzy skrzynki i próbuje załadować je ponownie
     */
    private fun createCasesAndRetry(callback: (List<Case>) -> Unit) {
        Log.d(TAG, "Tworzę nowe skrzynki na miejscu...")
        
        try {
            // Tworzymy nowe skrzynki
            val firebaseInitializer = FirebaseInitializer()
            firebaseInitializer.initializeCases()
            
            // Czekamy chwilę i próbujemy ponownie
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Ponowna próba pobrania skrzynek po utworzeniu...")
                
                // Drugi stopień zabezpieczenia - jeśli po inicjalizacji wciąż nie ma skrzynek, zwracamy listę hardkodowanych skrzynek
                database.child("cases").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val casesList = mutableListOf<Case>()
                        
                        Log.d(TAG, "Druga próba - liczba skrzynek: ${snapshot.childrenCount}")
                        
                        for (caseSnapshot in snapshot.children) {
                            try {
                                val case = caseSnapshot.getValue(Case::class.java)
                                if (case != null) {
                                    casesList.add(case)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Błąd w drugiej próbie: ${e.message}", e)
                            }
                        }
                        
                        if (casesList.isEmpty()) {
                            // Jeśli nadal nie ma skrzynek, zwracamy hardkodowane skrzynki
                            Log.d(TAG, "Druga próba nieudana, zwracam hardkodowane skrzynki")
                            callback(createHardcodedCases())
                        } else {
                            Log.d(TAG, "Druga próba udana, znaleziono: ${casesList.size} skrzynek")
                            callback(casesList)
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Błąd podczas drugiej próby: ${error.message}")
                        callback(createHardcodedCases())
                    }
                })
            }, 2000) // 2 sekundy opóźnienia
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas tworzenia skrzynek: ${e.message}", e)
            callback(createHardcodedCases())
        }
    }
    
    /**
     * Tworzy zestaw hardkodowanych skrzynek na wszelki wypadek
     */
    private fun createHardcodedCases(): List<Case> {
        Log.d(TAG, "Tworzę hardkodowane skrzynki jako ostateczność")
        
        // Skrzynka standardowa
        val standardItems = listOf(
            CaseItem(
                id = "coins_50",
                name = "50 Monet",
                description = "Niewielka ilość monet do wydania w sklepie",
                type = CaseItemType.COIN,
                rarity = ItemRarity.COMMON,
                value = 50,
                dropChance = 40.0
            ),
            CaseItem(
                id = "simple_armor",
                name = "Prosta zbroja",
                description = "Podstawowa zbroja zapewniająca +5 do obrony",
                type = CaseItemType.ARMOR,
                rarity = ItemRarity.UNCOMMON,
                value = 5,
                dropChance = 30.0
            )
        )
        
        val standardCase = Case(
            id = "standard_case",
            name = "Podstawowa skrzynia",
            description = "Zawiera monety i podstawowy ekwipunek.",
            price = 200,
            items = standardItems
        )
        
        // Skrzynka premium
        val premiumItems = listOf(
            CaseItem(
                id = "coins_250",
                name = "250 Monet",
                description = "Duża ilość monet do wydania w sklepie",
                type = CaseItemType.COIN,
                rarity = ItemRarity.UNCOMMON,
                value = 250,
                dropChance = 50.0
            ),
            CaseItem(
                id = "steel_sword",
                name = "Stalowy miecz",
                description = "Solidna broń zapewniająca +15 do ataku",
                type = CaseItemType.WEAPON,
                rarity = ItemRarity.RARE,
                value = 15,
                dropChance = 50.0
            )
        )
        
        val premiumCase = Case(
            id = "premium_case",
            name = "Skrzynia premium",
            description = "Zawiera lepsze przedmioty i więcej monet.",
            price = 500,
            items = premiumItems
        )
        
        return listOf(standardCase, premiumCase)
    }

    /**
     * Pobiera szczegóły skrzynki z listy przedmiotów
     */
    fun getCaseDetails(caseId: String, callback: (Case?) -> Unit) {
        database.child("cases").child(caseId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val case = snapshot.getValue(Case::class.java)
                        callback(case)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    /**
     * Losuje przedmiot ze skrzynki na podstawie szans wypadnięcia
     */
    fun openCase(case: Case): CaseItem {
        val random = Random.nextDouble(0.0, 100.0)
        var cumulativeProbability = 0.0
        
        // Sortujemy przedmioty według rzadkości (od najrzadszych do najpospolitszych)
        val sortedItems = case.items.sortedByDescending { it.rarity.ordinal }
        
        for (item in sortedItems) {
            cumulativeProbability += item.dropChance
            if (random <= cumulativeProbability) {
                return item
            }
        }
        
        // Jeśli z jakiegoś powodu nie wylosowano przedmiotu, zwróć pierwszy z listy
        return sortedItems.firstOrNull() ?: createDefaultItem()
    }
    
    /**
     * Zapisuje wylosowany przedmiot do ekwipunku użytkownika
     */
    fun saveItemToUserInventory(userId: String, item: CaseItem, callback: (Boolean) -> Unit) {
        when (item.type) {
            CaseItemType.COIN -> {
                // Dodaj monety do konta użytkownika
                database.child("users").child(userId).child("coins")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentCoins = snapshot.getValue(Int::class.java) ?: 0
                            val newCoins = currentCoins + item.value
                            
                            database.child("users").child(userId).child("coins")
                                .setValue(newCoins)
                                .addOnCompleteListener { task ->
                                    callback(task.isSuccessful)
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            callback(false)
                        }
                    })
            }
            
            CaseItemType.ARMOR_TIER -> {
                // Dodaj odpowiedni typ zbroi do ekwipunku użytkownika
                database.child("users").child(userId).child("equipment")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            try {
                                // Pobierz aktualny ekwipunek użytkownika
                                val equipment = snapshot.getValue(Equipment::class.java) ?: Equipment()
                                
                                // Liczba elementów zbroi do dodania
                                val armorCount = item.value
                                
                                // Aktualizuj ekwipunek w zależności od typu zbroi i liczby elementów
                                var updatedEquipment = equipment
                                
                                // Dodaj odpowiednią liczbę elementów zbroi, po jednym, aby zachować logikę promocji
                                for (i in 1..armorCount) {
                                    updatedEquipment = when (item.armorTier) {
                                        "BRONZE" -> updatedEquipment.addBronzeArmor()
                                        "SILVER" -> updatedEquipment.addSilverArmor()
                                        "GOLD" -> updatedEquipment.copy(goldArmorCount = updatedEquipment.goldArmorCount + 1)
                                        else -> updatedEquipment
                                    }
                                }
                                
                                // Zapisz zaktualizowany ekwipunek
                                val equipmentMap = updatedEquipment.toMap()
                                database.child("users").child(userId).child("equipment")
                                    .updateChildren(equipmentMap)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d(TAG, "Dodano ${armorCount} sztuk zbroi poziomu ${item.armorTier} do ekwipunku użytkownika")
                                            callback(true)
                                        } else {
                                            Log.e(TAG, "Błąd podczas zapisywania ekwipunku: ${task.exception?.message}")
                                            callback(false)
                                        }
                                    }
                            } catch (e: Exception) {
                                Log.e(TAG, "Błąd podczas aktualizacji ekwipunku: ${e.message}")
                                callback(false)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Anulowano pobieranie ekwipunku: ${error.message}")
                            callback(false)
                        }
                    })
            }
            
            CaseItemType.ARMOR -> {
                // Dodaj zbroję do ekwipunku I zaktualizuj liczniki w zależności od rzadkości
                database.child("users").child(userId).child("equipment")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            try {
                                // Pobierz aktualny ekwipunek użytkownika
                                val equipment = snapshot.getValue(Equipment::class.java) ?: Equipment()
                                
                                // Określ liczbę elementów zbroi do dodania w zależności od rzadkości
                                var updatedEquipment = equipment
                                
                                // Dodaj zbroje do odpowiednich liczników w zależności od rzadkości
                                updatedEquipment = when (item.rarity) {
                                    ItemRarity.COMMON, ItemRarity.UNCOMMON -> {
                                        // Prosta zbroja - dodaj brązową
                                        updatedEquipment.addBronzeArmor()
                                    }
                                    ItemRarity.RARE, ItemRarity.EPIC -> {
                                        // Epicka zbroja - dodaj srebrną
                                        updatedEquipment.addSilverArmor()
                                    }
                                    ItemRarity.LEGENDARY -> {
                                        // Legendarna zbroja - dodaj złotą
                                        updatedEquipment.copy(goldArmorCount = updatedEquipment.goldArmorCount + 1)
                                    }
                                }
                                
                                // Również dodaj przedmiot do inwentarza
                                database.child("users").child(userId).child("inventory").child(item.id)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(inventorySnapshot: DataSnapshot) {
                                            val currentCount = inventorySnapshot.getValue(Int::class.java) ?: 0
                                            val updates = HashMap<String, Any>()
                                            
                                            // Aktualizuj zarówno ekwipunek jak i inwentarz
                                            updates["users/$userId/equipment"] = updatedEquipment
                                            updates["users/$userId/inventory/${item.id}"] = currentCount + 1
                                            
                                            database.updateChildren(updates)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val tierName = when (item.rarity) {
                                                            ItemRarity.COMMON, ItemRarity.UNCOMMON -> "brązowa"
                                                            ItemRarity.RARE, ItemRarity.EPIC -> "srebrna"
                                                            ItemRarity.LEGENDARY -> "złota"
                                                        }
                                                        Log.d(TAG, "Dodano zbroję typu ${item.rarity} i zaktualizowano licznik ($tierName)")
                                                        callback(true)
                                                    } else {
                                                        Log.e(TAG, "Błąd podczas zapisywania: ${task.exception?.message}")
                                                        callback(false)
                                                    }
                                                }
                                        }
                                        
                                        override fun onCancelled(error: DatabaseError) {
                                            callback(false)
                                        }
                                    })
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Błąd podczas aktualizacji ekwipunku: ${e.message}")
                                callback(false)
                            }
                        }
                        
                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Anulowano pobieranie ekwipunku: ${error.message}")
                            callback(false)
                        }
                    })
            }
            
            CaseItemType.WEAPON, CaseItemType.SPECIAL -> {
                // Dodaj przedmiot do inwentarza użytkownika
                database.child("users").child(userId).child("inventory").child(item.id)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentCount = snapshot.getValue(Int::class.java) ?: 0
                            
                            database.child("users").child(userId).child("inventory").child(item.id)
                                .setValue(currentCount + 1)
                                .addOnCompleteListener { task ->
                                    callback(task.isSuccessful)
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            callback(false)
                        }
                    })
            }
        }
    }
    
    /**
     * Odejmuje monety użytkownika po zakupie skrzynki
     */
    fun chargeUserForCase(userId: String, casePrice: Int, callback: (Boolean) -> Unit) {
        database.child("users").child(userId).child("coins")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentCoins = snapshot.getValue(Int::class.java) ?: 0
                    
                    if (currentCoins < casePrice) {
                        // Użytkownik nie ma wystarczającej liczby monet
                        callback(false)
                        return
                    }
                    
                    val newCoins = currentCoins - casePrice
                    database.child("users").child(userId).child("coins")
                        .setValue(newCoins)
                        .addOnCompleteListener { task ->
                            callback(task.isSuccessful)
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }
    
    /**
     * Tworzy domyślny przedmiot w przypadku błędu
     */
    private fun createDefaultItem(): CaseItem {
        return CaseItem(
            id = "default_coin",
            name = "Monety",
            description = "Niewielka ilość monet",
            type = CaseItemType.COIN,
            rarity = ItemRarity.COMMON,
            value = 50,
            dropChance = 100.0
        )
    }
} 