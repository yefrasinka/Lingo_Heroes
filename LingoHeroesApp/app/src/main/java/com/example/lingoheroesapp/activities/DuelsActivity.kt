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
                            // Próba konwersji String -> Int
                            val stageNum = try {
                                stageSnapshot.getValue(String::class.java)?.toInt() 
                                    ?: stageSnapshot.getValue(Int::class.java)
                            } catch (e: NumberFormatException) {
                                null
                            }
                            
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
                            // Próba konwersji String -> Int
                            val stageNum = try {
                                stageSnapshot.getValue(String::class.java)?.toInt() 
                                    ?: stageSnapshot.getValue(Int::class.java)
                            } catch (e: NumberFormatException) {
                                null
                            }
                            
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
                            // Klucz może być teraz stringiem, więc konwertujemy go na Int
                            val stageNum = try {
                                stageSnapshot.key?.toIntOrNull()
                            } catch (e: NumberFormatException) {
                                null
                            }
                            
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
                // Ukryj wskaźnik ładowania i pokaż komunikat o błędzie
                loadingIndicator.visibility = View.GONE
                Toast.makeText(this@DuelsActivity, "Błąd ładowania danych: ${error.message}", Toast.LENGTH_LONG).show()
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
        
        // Konwertujemy listy i mapy, aby miały stringi jako klucze
        val stringUnlockedStages = unlockedStages.map { it.toString() }
        val stringCompletedStages = completedStages.map { it.toString() }
        
        // Konwertujemy mapę stageStars, aby używała stringów jako kluczy
        val stringStageStars = mutableMapOf<String, Int>()
        stageStars.forEach { (stage, stars) ->
            stringStageStars[stage.toString()] = stars
        }
        
        // Create a map of the user data
        val userData = mapOf(
            "level" to userLevel,
            "xp" to userXp,
            "coins" to userCoins,
            "unlockedStages" to stringUnlockedStages,
            "completedStages" to stringCompletedStages,
            "stageStars" to stringStageStars
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
            // Odczytaj informacje o ukończonym etapie
            val completedStage = data.getIntExtra("COMPLETED_STAGE", -1)
            val stars = data.getIntExtra("STAGE_STARS", 0)
            var xpGained = 0 // Zawsze 0, nie przyznajemy XP za pojedynki
            var coinsGained = data.getIntExtra("COINS_GAINED", 0)
            
            // Sprawdź czy etap został ukończony
            if (completedStage > 0) {
                // Przyznaj nagrody oraz odblokuj nowy etap
                var shouldAddBronzeArmor = stars == 3 // Dodaj brązową zbroję tylko dla 3 gwiazdek
                
                // Sprawdź poprzedni najlepszy wynik dla tego etapu
                val previousStars = stageStars[completedStage] ?: 0
                
                // Zmodyfikuj nagrody na podstawie poprzedniego wyniku
                when {
                    // Jeśli poprzednio było 3 gwiazdki, nie dodawaj zbroi
                    previousStars == 3 -> {
                        // Brak nagród za ponowne ukończenie etapu na 3 gwiazdki
                        xpGained = 0
                        coinsGained = 0
                        shouldAddBronzeArmor = false
                        
                        Toast.makeText(this, 
                            "Etap $completedStage już został ukończony na 3 gwiazdki! Brak dodatkowych nagród.", 
                            Toast.LENGTH_LONG).show()
                        
                        // Nie aktualizuj liczby gwiazdek, tylko wyjdź
                        return
                    }
                    
                    // Jeśli gracz poprawił swój wynik (więcej gwiazdek)
                    stars > previousStars -> {
                        // Zachowaj tylko nagrody monetowe
                        // Zachowaj nagrodę za zbroję, jeśli osiągnięto 3 gwiazdki
                        shouldAddBronzeArmor = stars == 3
                        
                        Toast.makeText(this, 
                            "Poprawiłeś swój wynik z $previousStars na $stars gwiazdki! Zdobyto: $coinsGained monet" + 
                                (if (shouldAddBronzeArmor) ", +brązowa zbroja" else ""), 
                            Toast.LENGTH_LONG).show()
                    }
                    
                    // Jeśli liczba gwiazdek jest taka sama lub niższa
                    else -> {
                        // Minimalna nagroda za ponowne ukończenie
                        coinsGained = (coinsGained * 0.25).toInt()
                        shouldAddBronzeArmor = false
                        
                        Toast.makeText(this, 
                            "Etap $completedStage już został ukończony z lepszym wynikiem. Zdobyto: $coinsGained monet.", 
                            Toast.LENGTH_LONG).show()
                    }
                }
                
                // Aktualizuj etap w danych gracza
                completeStage(completedStage, stars, shouldAddBronzeArmor)
                
                // Dodajemy zdobyte monety (bez XP)
                if (coinsGained > 0) {
                    updateRewards(xpGained, coinsGained)
                }
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
        // Nie zwiększamy poziomu na podstawie XP - tylko po pokonaniu bossa
        // Zostawiamy kod obsługi XP dla innych celów, ale bez zmiany poziomu
        
        // Prosta formuła wymaganego XP do następnego poziomu: level * 100 - używamy do innych celów
        val requiredXpForNextLevel = userLevel * 100
        
        if (userXp >= requiredXpForNextLevel) {
            // Resetujemy XP powyżej progu, ale nie zwiększamy poziomu automatycznie
            userXp -= requiredXpForNextLevel
            
            // Dodatkowa nagroda za przekroczenie progu XP
            userCoins += 25
            
            // Pokaż informację o nagrodzie
            Toast.makeText(this, 
                "Zdobyłeś dodatkowe monety za osiągnięcie progu XP! +25 monet", 
                Toast.LENGTH_LONG).show()
        }
    }
    
    // Called from DuelBattleActivity when a stage is completed
    fun completeStage(stageNumber: Int, stars: Int, shouldAddBronzeArmor: Boolean = true) {
        // Add to completed stages
        completedStages.add(stageNumber)
        
        // Save star count (only update if better than previous)
        val currentStars = stageStars[stageNumber] ?: 0
        if (stars > currentStars) {
            stageStars[stageNumber] = stars
        }
        
        // Określ, który etap jest bossem na tym poziomie (ostatni etap na tym poziomie)
        val bossStageLevels = listOf(3, 6, 9, 10) // Przykładowo: etapy 3, 6, 9, 10 są bossami
        
        // Sprawdź czy ukończony etap jest bossem
        val isBossStage = bossStageLevels.contains(stageNumber)
        
        // Odblokuj następny etap, jeśli nie ostatni
        if (stageNumber < TOTAL_STAGES) {
            unlockedStages.add(stageNumber + 1)
        }
        
        // Jeśli pokonano bossa, zwiększ poziom gracza
        if (isBossStage) {
            userLevel++
            
            // Dodatkowa nagroda za pokonanie bossa
            userCoins += 50
            
            // Pokaż informację o awansie
            Toast.makeText(this, 
                "Gratulacje! Pokonałeś bossa i awansowałeś na poziom $userLevel! +50 monet", 
                Toast.LENGTH_LONG).show()
            
            // Aktualizuj UI
            playerLevelText.text = "Level $userLevel"
            
            // Aktualizuj statystykę bossesDefeated
            updateBossesDefeatedStat()
        }
        
        // Dodaj brązową zbroję, jeśli uzyskano 3 gwiazdki i jeśli jest to wymagane
        if (stars == 3 && shouldAddBronzeArmor) {
            addBronzeArmorToUserEquipment()
        }
        
        // Aktualizuj licznik ukończonych pojedynków
        updateDuelsCompletedStat()
        
        // Save to Firebase
        saveUserData()
        
        // Update UI
        updateUI()
    }
    
    /**
     * Aktualizuje statystykę ukończonych pojedynków
     */
    private fun updateDuelsCompletedStat() {
        if (userId == null) return
        
        val userRef = database.reference.child("users").child(userId!!)
        
        userRef.child("duelsCompleted").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentDuelsCompleted = snapshot.getValue(Int::class.java) ?: 0
                val newDuelsCompleted = currentDuelsCompleted + 1
                
                // Aktualizuj liczbę ukończonych pojedynków
                userRef.child("duelsCompleted").setValue(newDuelsCompleted)
                    .addOnSuccessListener {
                        // Powiadom AchievementManager o aktualizacji
                        com.example.lingoheroesapp.utils.AchievementManager.updateAchievements(
                            userId!!, 
                            com.example.lingoheroesapp.models.AchievementType.DUELS_COMPLETED, 
                            newDuelsCompleted
                        )
                        
                        Log.d("DuelsActivity", "Zaktualizowano liczbę ukończonych pojedynków: $newDuelsCompleted")
                    }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelsActivity", "Błąd podczas aktualizacji statystyki pojedynków: ${error.message}")
            }
        })
    }
    
    /**
     * Aktualizuje statystykę pokonanych bossów
     */
    private fun updateBossesDefeatedStat() {
        if (userId == null) return
        
        val userRef = database.reference.child("users").child(userId!!)
        
        userRef.child("bossesDefeated").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentBossesDefeated = snapshot.getValue(Int::class.java) ?: 0
                val newBossesDefeated = currentBossesDefeated + 1
                
                // Aktualizuj liczbę pokonanych bossów
                userRef.child("bossesDefeated").setValue(newBossesDefeated)
                    .addOnSuccessListener {
                        // Powiadom AchievementManager o aktualizacji
                        com.example.lingoheroesapp.utils.AchievementManager.updateAchievements(
                            userId!!, 
                            com.example.lingoheroesapp.models.AchievementType.BOSS_DEFEATED, 
                            newBossesDefeated
                        )
                        
                        Log.d("DuelsActivity", "Zaktualizowano liczbę pokonanych bossów: $newBossesDefeated")
                    }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelsActivity", "Błąd podczas aktualizacji statystyki bossów: ${error.message}")
            }
        })
    }
    
    // Metoda do dodawania brązowej zbroi do ekwipunku gracza
    private fun addBronzeArmorToUserEquipment() {
        // Najpierw sprawdź, czy użytkownik jest zalogowany
        if (userId == null) return
        
        val userRef = database.reference.child("users").child(userId!!)
        
        // Pobierz aktualny ekwipunek użytkownika
        userRef.child("equipment").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Odczytaj dane ekwipunku
                    val armorTierRaw = snapshot.child("armorTier").getValue()
                    val armorTier = when {
                        armorTierRaw is Long -> armorTierRaw.toInt()
                        armorTierRaw is Int -> armorTierRaw
                        armorTierRaw is String -> try { armorTierRaw.toInt() } catch (e: Exception) { 0 }
                        else -> 0
                    }
                    
                    val bronzeArmorCount = when {
                        snapshot.child("bronzeArmorCount").getValue(Long::class.java) != null ->
                            snapshot.child("bronzeArmorCount").getValue(Long::class.java)!!.toInt()
                        snapshot.child("bronzeArmorCount").getValue(Int::class.java) != null ->
                            snapshot.child("bronzeArmorCount").getValue(Int::class.java)!!
                        snapshot.child("bronzeArmorCount").getValue(String::class.java) != null ->
                            snapshot.child("bronzeArmorCount").getValue(String::class.java)!!.toInt()
                        else -> 0
                    }
                    
                    // Zwiększ liczbę brązowych zbroi o 1
                    val newBronzeArmorCount = bronzeArmorCount + 1
                    
                    // Sprawdź, czy nastąpił awans zbroi (co 10 sztuk)
                    val newArmorTier = if (newBronzeArmorCount >= 10) {
                        // Resetuj licznik i zwiększ poziom zbroi
                        userRef.child("equipment").child("bronzeArmorCount").setValue(0)
                        armorTier + 1 // Awans do następnego poziomu
                    } else {
                        // Aktualizuj tylko liczbę brązowych zbroi
                        userRef.child("equipment").child("bronzeArmorCount").setValue(newBronzeArmorCount)
                        armorTier // Zachowaj ten sam poziom
                    }
                    
                    // Jeśli nastąpił awans, zaktualizuj poziom zbroi
                    if (newArmorTier > armorTier) {
                        userRef.child("equipment").child("armorTier").setValue(newArmorTier)
                        
                        // Pokaż informację o awansie zbroi
                        Toast.makeText(this@DuelsActivity, 
                            "Gratulacje! Twoja zbroja została ulepszona do poziomu $newArmorTier!\n" +
                            "Odblokowano nowy wygląd postaci z lepszą ochroną!", 
                            Toast.LENGTH_LONG).show()
                    } else {
                        // Pokaż informację o zbieraniu zbroi
                        Toast.makeText(this@DuelsActivity, 
                            "Zdobyłeś brązową zbroję! ($newBronzeArmorCount/10)\n" +
                            "Zbierz jeszcze ${10 - newBronzeArmorCount} sztuk, aby awansować na wyższy poziom.", 
                            Toast.LENGTH_LONG).show()
                    }
                    
                } catch (e: Exception) {
                    Log.e("DuelsActivity", "Błąd podczas dodawania brązowej zbroi: ${e.message}")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DuelsActivity", "Błąd podczas pobierania ekwipunku: ${error.message}")
            }
        })
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
