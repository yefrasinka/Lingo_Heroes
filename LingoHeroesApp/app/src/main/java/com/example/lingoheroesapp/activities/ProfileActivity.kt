package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.AchievementsAdapter
import com.example.lingoheroesapp.models.Achievement
import com.example.lingoheroesapp.models.AchievementType
import com.example.lingoheroesapp.models.Challenge
import com.example.lingoheroesapp.models.ChallengeType
import com.example.lingoheroesapp.models.User
import com.example.lingoheroesapp.utils.AchievementManager
import com.example.lingoheroesapp.utils.ChallengeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {
    private lateinit var usernameText: TextView
    private lateinit var levelText: TextView
    private lateinit var xpText: TextView
    private lateinit var streakText: TextView
    private lateinit var achievementsRecyclerView: RecyclerView
    private lateinit var achievementsAdapter: AchievementsAdapter
    private lateinit var dailyChallengesText: TextView
    private lateinit var weeklyChallengesText: TextView
    private lateinit var dailyChallengesLayout: LinearLayout
    private lateinit var weeklyChallengesLayout: LinearLayout
    private val databaseRef = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupRecyclerView()
        loadUserData()
        setupBottomNavigation()
        setupViews()
        
        // Sprawdź i resetuj wygasłe wyzwania
        ChallengeManager.checkAndResetExpiredChallenges()
    }

    private fun initializeViews() {
        usernameText = findViewById(R.id.usernameText)
        levelText = findViewById(R.id.levelText)
        xpText = findViewById(R.id.xpText)
        streakText = findViewById(R.id.streakText)
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView)
        dailyChallengesText = findViewById(R.id.dailyChallengesText)
        weeklyChallengesText = findViewById(R.id.weeklyChallengesText)
        dailyChallengesLayout = findViewById(R.id.dailyChallengesLayout)
        weeklyChallengesLayout = findViewById(R.id.weeklyChallengesLayout)
    }

    private fun setupRecyclerView() {
        achievementsAdapter = AchievementsAdapter()
        achievementsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = achievementsAdapter
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            databaseRef.child("users").child(currentUser.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let { 
                            updateUserUI(it)
                            
                            // Synchronizuj osiągnięcia z aktualnymi danymi użytkownika
                            AchievementManager.syncAchievements(currentUser.uid)
                        }
                        loadChallengesData(currentUser.uid)
                        loadAchievements(currentUser.uid)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Błąd podczas ładowania danych użytkownika")
                    }
                })
        }
    }

    private fun updateUserUI(user: User) {
        usernameText.text = user.username
        levelText.text = user.level.toString()
        xpText.text = "${user.xp} XP"
        streakText.text = "${user.streakDays} dni"

        val settingsImage = findViewById<ImageView>(R.id.settingsImage)
        settingsImage.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
    }
    
    private fun loadAchievements(userId: String) {
        // Użyj nowego menedżera do pobrania osiągnięć
        AchievementManager.getUserAchievements(userId) { achievements ->
            achievementsAdapter.updateAchievements(achievements)
        }
    }

    private fun loadChallengesData(userId: String) {
        databaseRef.child("users").child(userId).child("challenges")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var completedDaily = 0
                    var totalDaily = 0
                    var completedWeekly = 0
                    var totalWeekly = 0

                    snapshot.children.forEach { challengeSnapshot ->
                        val challenge = challengeSnapshot.getValue(Challenge::class.java)
                        challenge?.let {
                            when (it.type) {
                                ChallengeType.DAILY -> {
                                    totalDaily++
                                    if (it.isCompleted) completedDaily++
                                }
                                ChallengeType.WEEKLY -> {
                                    totalWeekly++
                                    if (it.isCompleted) completedWeekly++
                                }
                            }
                        }
                    }

                    dailyChallengesText.text = "$completedDaily/$totalDaily"
                    weeklyChallengesText.text = "$completedWeekly/$totalWeekly"
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Błąd podczas ładowania wyzwań")
                }
            })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
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
                R.id.nav_store -> {
                    startActivity(Intent(this, StoreActivity::class.java))
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun setupViews() {
        findViewById<Button>(R.id.challengesButton).setOnClickListener {
            startActivity(Intent(this, ChallengesActivity::class.java))
        }

        // Dodanie obsługi kliknięć na sekcje wyzwań
        dailyChallengesLayout.setOnClickListener {
            val intent = Intent(this, ChallengesActivity::class.java)
            intent.putExtra("filter", ChallengeType.DAILY.name)
            startActivity(intent)
        }

        weeklyChallengesLayout.setOnClickListener {
            val intent = Intent(this, ChallengesActivity::class.java)
            intent.putExtra("filter", ChallengeType.WEEKLY.name)
            startActivity(intent)
        }
    }
} 