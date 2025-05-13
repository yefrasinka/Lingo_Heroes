package com.example.lingoheroesapp.models

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.util.Date

@IgnoreExtraProperties
data class FriendRequest(
    val senderId: String = "",      // ID użytkownika wysyłającego zaproszenie
    val receiverId: String = "",    // ID użytkownika odbierającego zaproszenie
    val senderName: String = "",    // Nazwa użytkownika wysyłającego
    val status: String = "pending", // Status zaproszenia: "pending", "accepted", "declined"
    val timestamp: Long = Date().time  // Czas utworzenia zaproszenia
) {
    // Konstruktor bezargumentowy wymagany przez Firebase
    constructor() : this("", "", "", "pending", Date().time)
} 