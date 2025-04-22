package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StoreActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var usernameTextView: TextView
    private lateinit var xpTextView: TextView
    private lateinit var coinsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        initializeFirebase()
        initializeUI()
        setupBottomNavigation()
        loadUserData()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }

    private fun initializeUI() {
        usernameTextView = findViewById(R.id.usernameText)
        xpTextView = findViewById(R.id.experienceText)
        coinsTextView = findViewById(R.id.currencyText)

        val avatarImage = findViewById<ImageView>(R.id.avatarImage)
        avatarImage.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_store

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_learning -> {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    true
                }
                R.id.nav_minigames -> {
                    startActivity(Intent(this, HeroActivity::class.java))
                    true
                }
                R.id.nav_duels -> {
                    startActivity(Intent(this, DuelsActivity::class.java))
                    true
                }
                R.id.nav_store -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("users").child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            // Ręczne mapowanie danych zamiast automatycznej deserializacji
                            val uid = snapshot.child("uid").getValue(String::class.java) ?: currentUser.uid
                            val username = snapshot.child("username").getValue(String::class.java) ?: "Użytkownik"
                            val email = snapshot.child("email").getValue(String::class.java) ?: ""
                            val level = snapshot.child("level").getValue(Long::class.java)?.toInt() ?: 1
                            val xp = snapshot.child("xp").getValue(Long::class.java)?.toInt() ?: 0
                            val coins = snapshot.child("coins").getValue(Long::class.java)?.toInt() ?: 0
                            
                            // Utwórz obiekt User tylko z potrzebnymi polami
                            val user = User(
                                uid = uid,
                                username = username,
                                email = email,
                                level = level,
                                xp = xp,
                                coins = coins
                            )
                            
                            // Zaktualizuj UI
                            updateUserUI(user)
                        } catch (e: Exception) {
                            showError("Błąd podczas przetwarzania danych: ${e.message}")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Błąd podczas ładowania danych: ${error.message}")
                    }
                })
        }
    }

    private fun updateUserUI(user: User) {
        usernameTextView.text = user.username
        xpTextView.text = "${user.xp} XP"
        coinsTextView.text = "${user.coins} coins"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
