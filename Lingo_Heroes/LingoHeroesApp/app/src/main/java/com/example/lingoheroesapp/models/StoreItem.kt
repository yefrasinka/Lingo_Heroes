package com.example.lingoheroesapp.models

data class StoreItem(
    val itemId: String = "",           // ID przedmiotu
    val name: String = "",             // Nazwa przedmiotu
    val description: String = "",      // Opis
    val price: Int = 0,                // Cena w monetach
    val type: String = "",             // Typ (np. skin, upgrade)
    val imageUrl: String = ""          // Link do obrazka
)