package com.example.lingoheroesapp.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LanguageLevelActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_level)

        auth = FirebaseAuth.getInstance()

        // Inicjalizacja przycisków poziomów
        findViewById<Button>(R.id.levelA1Button).setOnClickListener { updateLevel(1, "A1") }
        findViewById<Button>(R.id.levelA2Button).setOnClickListener { updateLevel(2, "A2") }
        findViewById<Button>(R.id.levelB1Button).setOnClickListener { updateLevel(3, "B1") }
        findViewById<Button>(R.id.levelB2Button).setOnClickListener { updateLevel(4, "B2") }

        // Przycisk powrotu
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun updateLevel(level: Int, levelName: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("level").setValue(level)
                .addOnSuccessListener {
                    Toast.makeText(this, "Poziom zmieniony na $levelName", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Błąd podczas zmiany poziomu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
