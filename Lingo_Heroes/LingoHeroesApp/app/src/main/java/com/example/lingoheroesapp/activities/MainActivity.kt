package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.utils.FirebaseInitializer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Inicjalizacja przykładowych skrzynek w Firebase (tylko do testów)
        initializeTestData()
        
        // Dodajemy opóźnienie przed przejściem do kolejnego ekranu,
        // aby mieć pewność, że dane zostaną zapisane w Firebase
        Handler(Looper.getMainLooper()).postDelayed({
            // Sprawdzamy czy skrzynki zostały pomyślnie zapisane
            checkCasesAndNavigate(currentUser)
        }, 5000) // 5 sekund opóźnienia - dużo dłuższy czas na zapis
    }
    
    /**
     * Sprawdza czy skrzynki zostały zapisane w bazie i przechodzi do odpowiedniego ekranu
     */
    private fun checkCasesAndNavigate(currentUser: com.google.firebase.auth.FirebaseUser?) {
        val database = FirebaseDatabase.getInstance()
        val casesRef = database.getReference("cases")
        
        casesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val casesCount = snapshot.childrenCount
                Log.d(TAG, "Liczba skrzynek w bazie: $casesCount")
                
                if (casesCount < 3) {
                    // Jeśli nadal nie ma wystarczającej liczby skrzynek, spróbujmy jeszcze raz
                    Toast.makeText(this@MainActivity, "Inicjalizacja danych, proszę czekać...", Toast.LENGTH_SHORT).show()
                    
                    // Ponowna inicjalizacja
                    initializeTestData()
                    
                    // Dajemy jeszcze chwilę i przechodzimy do następnego ekranu
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToNextScreen(currentUser)
                    }, 3000)
                } else {
                    // Skrzynki zostały zapisane, przechodzimy do następnego ekranu
                    navigateToNextScreen(currentUser)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Błąd podczas sprawdzania skrzynek: ${error.message}")
                // W przypadku błędu i tak przechodzimy dalej
                navigateToNextScreen(currentUser)
            }
        })
    }
    
    /**
     * Przechodzi do odpowiedniego ekranu na podstawie stanu zalogowania
     */
    private fun navigateToNextScreen(currentUser: com.google.firebase.auth.FirebaseUser?) {
        if (currentUser == null) {
            // Jeśli użytkownik nie jest zalogowany, przechodzimy na ekran logowania
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Jeśli użytkownik jest zalogowany, przechodzimy do głównego menu
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }

    private fun initializeTestData() {
        try {
            // Inicjalizacja skrzynek (tylko dla celów testowych - w produkcji można usunąć)
            val firebaseInitializer = FirebaseInitializer()
            firebaseInitializer.initializeCases()
            Log.d(TAG, "Inicjalizacja skrzynek została uruchomiona")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas inicjalizacji danych testowych: ${e.message}", e)
        }
    }
} 