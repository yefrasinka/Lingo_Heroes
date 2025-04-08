package com.example.lingoheroesapp.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.lingoheroesapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DuelsActivity : AppCompatActivity() {

    private lateinit var loadingIndicator: ProgressBar
    private lateinit var mapContainer: ConstraintLayout
    private lateinit var playerLevelText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userId: String? = null
    
    // User game data
    private var userLevel: Int = 1
    private var userXp: Int = 0
    private var userCoins: Int = 0
    private var unlockedStages: MutableSet<Int> = mutableSetOf(1) // Stage 1 is always unlocked
    private var completedStages: MutableSet<Int> = mutableSetOf()
    private var stageStars: MutableMap<Int, Int> = mutableMapOf() // Przechowuje ilość gwiazdek dla każdego etapu
    
    // Stage buttons and indicators
    private val stageButtons = mutableListOf<ImageButton>()
    private val stageLocks = mutableListOf<ImageView>()
    private val stageCheckmarks = mutableListOf<ImageView>()
    private val stageTexts = mutableListOf<TextView>()
    private val stageStarImages = mutableMapOf<Int, List<ImageView>>() // Przechowuje ImageView dla gwiazdek każdego etapu
    
    // Total number of stages
    private val TOTAL_STAGES = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duels)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid
        
        // Initialize views
        initializeViews()
        
        // Ograniczenie przewijania do końca mapy
        limitMapScrolling()
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Check if user is logged in
        if (userId == null) {
            // Handle not logged in state - redirect to login 
            Toast.makeText(this, "Nie jesteś zalogowany", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Load user data
        loadUserData()
        
        // Set up stage click listeners
        setupStageListeners()
    }
    
    private fun initializeViews() {
        loadingIndicator = findViewById(R.id.loadingIndicator)
        mapContainer = findViewById(R.id.mapContainer)
        playerLevelText = findViewById(R.id.playerLevelText)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        
        // Initialize stage buttons
        for (i in 1..TOTAL_STAGES) {
            val buttonId = resources.getIdentifier("btnStage$i", "id", packageName)
            val lockId = resources.getIdentifier("lockStage$i", "id", packageName)
            val checkId = resources.getIdentifier("checkStage$i", "id", packageName)
            val textId = resources.getIdentifier("txtStage$i", "id", packageName)
            
            stageButtons.add(findViewById(buttonId))
            stageLocks.add(findViewById(lockId))
            stageCheckmarks.add(findViewById(checkId))
            stageTexts.add(findViewById(textId))
            
            // Initialize star images for each stage
            val starsList = mutableListOf<ImageView>()
            for (j in 1..3) {
                val starId = resources.getIdentifier("star${j}Stage$i", "id", packageName)
                starsList.add(findViewById(starId))
            }
            stageStarImages[i] = starsList
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.nav_duels
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_learning -> {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    true
                }
                R.id.nav_minigames -> {
                    startActivity(Intent(this, HeroActivity::class.java))
                    true
                }
                R.id.nav_duels -> true
                R.id.nav_store -> {
                    startActivity(Intent(this, StoreActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadUserData() {
        // Pokazujemy wskaźnik ładowania
        loadingIndicator.visibility = View.VISIBLE
        mapContainer.visibility = View.INVISIBLE
        
        val userRef = database.reference.child("users").child(userId!!)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Load existing user data
                    userLevel = snapshot.child("level").getValue(Int::class.java) ?: 1
                    userXp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                    userCoins = snapshot.child("coins").getValue(Int::class.java) ?: 0
                    
                    // Load unlocked stages
                    unlockedStages.clear()
                    unlockedStages.add(1) // Stage 1 zawsze odblokowany
                    val unlockedStagesSnapshot = snapshot.child("unlockedStages")
                    if (unlockedStagesSnapshot.exists()) {
                        for (stageSnapshot in unlockedStagesSnapshot.children) {
                            val stageNum = stageSnapshot.getValue(Int::class.java)
                            if (stageNum != null) {
                                unlockedStages.add(stageNum)
                            }
                        }
                    }
                    
                    // Load completed stages
                    completedStages.clear()
                    val completedStagesSnapshot = snapshot.child("completedStages")
                    if (completedStagesSnapshot.exists()) {
                        for (stageSnapshot in completedStagesSnapshot.children) {
                            val stageNum = stageSnapshot.getValue(Int::class.java)
                            if (stageNum != null) {
                                completedStages.add(stageNum)
                            }
                        }
                    }
                    
                    // Load stage stars
                    stageStars.clear()
                    val stageStarsSnapshot = snapshot.child("stageStars")
                    if (stageStarsSnapshot.exists()) {
                        for (stageSnapshot in stageStarsSnapshot.children) {
                            val stageNum = stageSnapshot.key?.toIntOrNull()
                            val starCount = stageSnapshot.getValue(Int::class.java) ?: 0
                            if (stageNum != null) {
                                stageStars[stageNum] = starCount
                            }
                        }
                    }
                } else {
                    // First time user, initialize data
                    initializeNewUserData()
                }
                
                // Update UI
                updateUI()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DuelsActivity, 
                    "Błąd ładowania danych: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
                
                // Show empty state or retry option
                loadingIndicator.visibility = View.GONE
                
                // Pokaż przycisk ponowienia próby
                showRetryButton()
            }
        })
    }
    
    private fun showRetryButton() {
        // Ta funkcja mogłaby tworzyć przycisk "Spróbuj ponownie" na środku ekranu
        Toast.makeText(this, "Nie można załadować danych. Spróbuj ponownie później.", Toast.LENGTH_LONG).show()
    }
    
    private fun initializeNewUserData() {
        // Default level is 1
        userLevel = 1
        userXp = 0
        userCoins = 100 // Dajemy trochę początkowych monet
        
        // Default unlocked is only stage 1
        unlockedStages = mutableSetOf(1)
        
        // No completed stages
        completedStages = mutableSetOf()
        
        // No stars for stages
        stageStars = mutableMapOf()
        
        // Save initial data to Firebase
        saveUserData()
    }
    
    private fun saveUserData() {
        val userRef = database.reference.child("users").child(userId!!)
        
        // Create a map of the user data
        val userData = mapOf(
            "level" to userLevel,
            "xp" to userXp,
            "coins" to userCoins,
            "unlockedStages" to unlockedStages.toList(),
            "completedStages" to completedStages.toList(),
            "stageStars" to stageStars
        )
        
        // Update the data
        userRef.updateChildren(userData)
            .addOnSuccessListener {
                // Dane zostały zapisane pomyślnie
                Log.d("DuelsActivity", "Dane użytkownika zapisane pomyślnie")
            }
            .addOnFailureListener { e ->
                // Wystąpił błąd podczas zapisywania danych
                Log.e("DuelsActivity", "Błąd podczas zapisywania danych użytkownika", e)
                Toast.makeText(this, "Nie udało się zapisać postępu. Sprawdź połączenie z internetem.", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun updateUI() {
        // Update level text
        playerLevelText.text = "Level $userLevel"
        
        // Update stage visuals
        for (i in 0 until TOTAL_STAGES) {
            val stageNum = i + 1
            
            // Check if stage is unlocked
            if (stageNum in unlockedStages) {
                stageLocks[i].visibility = View.INVISIBLE
                stageButtons[i].isEnabled = true
                
                // Powiększamy i pogrubiamy tekst numeru etapu
                stageTexts[i].textSize = 22f
                stageTexts[i].setTypeface(null, Typeface.BOLD)
                
                // Check if completed
                if (stageNum in completedStages) {
                    stageCheckmarks[i].visibility = View.VISIBLE
                    
                    // Show stars for the stage
                    val starCount = stageStars[stageNum] ?: 0
                    stageStarImages[stageNum]?.forEachIndexed { index, imageView ->
                        imageView.visibility = if (index < starCount) View.VISIBLE else View.GONE
                    }
                } else {
                    stageCheckmarks[i].visibility = View.INVISIBLE
                    // Hide all stars for incomplete stage
                    stageStarImages[stageNum]?.forEach { it.visibility = View.GONE }
                }
            } else {
                // Stage is locked
                stageLocks[i].visibility = View.VISIBLE
                stageCheckmarks[i].visibility = View.INVISIBLE
                stageButtons[i].isEnabled = false
                // Hide all stars for locked stage
                stageStarImages[stageNum]?.forEach { it.visibility = View.GONE }
            }
        }
        
        // Hide loading indicator and show map
        loadingIndicator.visibility = View.GONE
        mapContainer.visibility = View.VISIBLE
    }
    
    private fun setupStageListeners() {
        for (i in 0 until TOTAL_STAGES) {
            val stageNum = i + 1
            val button = stageButtons[i]
            
            button.setOnClickListener {
                // Animation effect on click
                val bounceAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                button.startAnimation(bounceAnimation)
                
                if (stageNum in unlockedStages) {
                    // Start DuelBattleActivity with stage number
                    val intent = Intent(this, DuelBattleActivity::class.java)
                    intent.putExtra("STAGE_NUMBER", stageNum)
                    startActivityForResult(intent, REQUEST_DUEL_BATTLE)
                }
            }
            
            // Add long click for locked stage shake animation
            button.setOnLongClickListener {
                if (stageNum !in unlockedStages) {
                    // Play shake animation on the lock
                    val shakeAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                    stageLocks[i].startAnimation(shakeAnimation)
                    
                    // Show message about locked stage
                    Toast.makeText(this, 
                        "Etap $stageNum jest zablokowany. Ukończ poprzednie etapy.", 
                        Toast.LENGTH_SHORT).show()
                    return@setOnLongClickListener true
                }
                false
            }
        }
    }
    
    // Called when we get result from DuelBattleActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_DUEL_BATTLE && resultCode == RESULT_OK && data != null) {
            val completedStage = data.getIntExtra("COMPLETED_STAGE", -1)
            val stars = data.getIntExtra("STAGE_STARS", 0)
            val xpGained = data.getIntExtra("XP_GAINED", 0)
            val coinsGained = data.getIntExtra("COINS_GAINED", 0)
            
            if (completedStage > 0) {
                // Jeśli etap został ukończony
                completeStage(completedStage, stars)
                
                // Dodajemy zdobyte XP i monety
                updateRewards(xpGained, coinsGained)
                
                // Wyświetlamy Toast z informacją o zdobytych nagrodach
                Toast.makeText(this, 
                    "Zdobyto: $xpGained XP, $coinsGained monet, $stars ⭐", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateRewards(xpGained: Int, coinsGained: Int) {
        // Dodaj zdobyte XP i monety
        userXp += xpGained
        userCoins += coinsGained
        
        // Sprawdź czy użytkownik awansował na wyższy poziom
        checkLevelUp()
        
        // Zapisz dane do Firebase
        saveUserData()
    }
    
    private fun checkLevelUp() {
        // Prosta formuła wymaganego XP do następnego poziomu: level * 100
        val requiredXpForNextLevel = userLevel * 100
        
        if (userXp >= requiredXpForNextLevel) {
            // Awans na następny poziom
            userLevel++
            userXp -= requiredXpForNextLevel
            
            // Dodatkowa nagroda za awans
            userCoins += 50
            
            // Pokaż informację o awansie
            Toast.makeText(this, 
                "Gratulacje! Awansowałeś na poziom $userLevel! +50 monet", 
                Toast.LENGTH_LONG).show()
            
            // Aktualizuj UI
            playerLevelText.text = "Level $userLevel"
        }
    }
    
    // Called from DuelBattleActivity when a stage is completed
    fun completeStage(stageNumber: Int, stars: Int) {
        // Add to completed stages
        completedStages.add(stageNumber)
        
        // Save star count (only update if better than previous)
        val currentStars = stageStars[stageNumber] ?: 0
        if (stars > currentStars) {
            stageStars[stageNumber] = stars
        }
        
        // Unlock next stage if not the last one
        if (stageNumber < TOTAL_STAGES) {
            unlockedStages.add(stageNumber + 1)
        }
        
        // Save to Firebase
        saveUserData()
        
        // Update UI
        updateUI()
    }
    
    private fun limitMapScrolling() {
        // Znajdź ScrollView i boss stage
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
        val bossStage = findViewById<ImageButton>(R.id.btnStage10)
        
        // Dodaj listener na zmianę układu, aby ograniczyć przewijanie
        scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            // Uzyskaj wysokość ScrollView i bossa
            val scrollViewHeight = scrollView.height
            val bossBottomPosition = bossStage.bottom + 150 // 150dp dodatkowego marginesu
            
            // Ogranicz maksymalną wysokość kontenera mapy
            val mapContainer = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.mapContainer)
            val layoutParams = mapContainer.layoutParams
            layoutParams.height = bossBottomPosition
            mapContainer.layoutParams = layoutParams
        }
    }
    
    companion object {
        private const val REQUEST_DUEL_BATTLE = 100
    }
} 
