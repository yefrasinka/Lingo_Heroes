package com.example.lingoheroesapp.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.AchievementsAdapter
import com.example.lingoheroesapp.services.UserService
import com.google.android.material.snackbar.Snackbar

class UserProfileActivity : AppCompatActivity() {
    private lateinit var usernameText: TextView
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView
    private lateinit var streakText: TextView
    private lateinit var duelsText: TextView
    private lateinit var bossesText: TextView
    private lateinit var backButton: ImageView
    private lateinit var achievementsRecyclerView: RecyclerView
    private lateinit var emptyAchievementsView: LinearLayout
    private lateinit var loadingIndicator: ProgressBar
    
    private var userId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        
        // Pobierz ID użytkownika z intentu
        userId = intent.getStringExtra("USER_ID")
        
        if (userId == null) {
            Snackbar.make(findViewById(android.R.id.content), "Błąd: Nie znaleziono ID użytkownika", Snackbar.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initViews()
        setupListeners()
        loadUserData()
    }
    
    private fun initViews() {
        usernameText = findViewById(R.id.usernameText)
        levelText = findViewById(R.id.levelText)
        xpText = findViewById(R.id.xpText)
        streakText = findViewById(R.id.streakText)
        duelsText = findViewById(R.id.duelsText)
        bossesText = findViewById(R.id.bossesText)
        backButton = findViewById(R.id.backButton)
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView)
        emptyAchievementsView = findViewById(R.id.emptyAchievementsView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        
        // Skonfiguruj RecyclerView
        achievementsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadUserData() {
        showLoading(true)
        
        userId?.let { id ->
            UserService.getUserById(id) { user, message ->
                runOnUiThread {
                    if (user != null) {
                        try {
                            // Uzupełnij dane użytkownika
                            usernameText.text = user.username
                            levelText.text = user.level.toString()
                            xpText.text = try { user.xp.toString() } catch (e: Exception) { "0" }
                            streakText.text = try { user.streakDays.toString() } catch (e: Exception) { "0" }
                            duelsText.text = try { user.tasksCompleted.toString() } catch (e: Exception) { "0" }
                            bossesText.text = try { user.challengesCompleted.toString() } catch (e: Exception) { "0" }
                            
                            // Załaduj osiągnięcia
                            loadAchievements(id)
                        } catch (e: Exception) {
                            Log.e("UserProfileActivity", "Błąd podczas wyświetlania danych użytkownika: ${e.message}")
                            showLoading(false)
                            
                            // Próbujemy wyświetlić przynajmniej podstawowe dane
                            try {
                                usernameText.text = user.username
                                levelText.text = user.level.toString() 
                                
                                // Wyświetlamy domyślne wartości dla pozostałych pól
                                xpText.text = "0"
                                streakText.text = "0"
                                duelsText.text = "0"
                                bossesText.text = "0"
                                
                                // Załaduj osiągnięcia
                                loadAchievements(id)
                            } catch (e2: Exception) {
                                Log.e("UserProfileActivity", "Krytyczny błąd podczas wyświetlania danych: ${e2.message}")
                                Snackbar.make(findViewById(android.R.id.content), "Nie można wyświetlić profilu użytkownika", Snackbar.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } else {
                        showLoading(false)
                        Snackbar.make(findViewById(android.R.id.content), message ?: "Błąd podczas ładowania danych użytkownika", Snackbar.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
    
    private fun loadAchievements(userId: String) {
        UserService.getUserAchievements(userId) { achievements ->
            runOnUiThread {
                showLoading(false)
                
                try {
                    if (achievements.isEmpty()) {
                        emptyAchievementsView.visibility = View.VISIBLE
                        achievementsRecyclerView.visibility = View.GONE
                    } else {
                        emptyAchievementsView.visibility = View.GONE
                        achievementsRecyclerView.visibility = View.VISIBLE
                        
                        val adapter = AchievementsAdapter(achievements)
                        achievementsRecyclerView.adapter = adapter
                    }
                } catch (e: Exception) {
                    Log.e("UserProfileActivity", "Błąd podczas wyświetlania osiągnięć: ${e.message}")
                    emptyAchievementsView.visibility = View.VISIBLE
                    achievementsRecyclerView.visibility = View.GONE
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            achievementsRecyclerView.visibility = View.GONE
            emptyAchievementsView.visibility = View.GONE
        }
    }
} 