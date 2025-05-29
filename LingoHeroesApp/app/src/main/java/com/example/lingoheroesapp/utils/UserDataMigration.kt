package com.example.lingoheroesapp.utils

import android.util.Log
import com.example.lingoheroesapp.models.Equipment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Utility class for handling user data migrations when new fields are added
 */
object UserDataMigration {
    private const val TAG = "UserDataMigration"
    
    /**
     * Checks if the current user has equipment data and adds it if missing
     * @param userId Firebase UID of the current user
     */
    fun migrateEquipmentData(userId: String) {
        val database = FirebaseDatabase.getInstance().reference
        val userRef = database.child("users").child(userId)
        
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if the equipment field exists
                if (!snapshot.hasChild("equipment")) {
                    // Add default equipment data
                    val defaultEquipment = Equipment()
                    userRef.child("equipment").setValue(defaultEquipment)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully added equipment data for user $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to add equipment data: ${e.message}")
                        }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error during equipment migration: ${error.message}")
            }
        })
    }
} 