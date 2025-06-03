package com.example.lingoheroesapp.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.ArmorTier
import com.example.lingoheroesapp.models.Equipment
import com.example.lingoheroesapp.models.User
import com.example.lingoheroesapp.models.WandType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.lingoheroesapp.utils.FirebaseDebugHelper

class HeroActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var usernameTextView: TextView
    private lateinit var xpTextView: TextView
    private lateinit var coinsTextView: TextView
    private lateinit var hpTextView: TextView
    private lateinit var atkTextView: TextView
    private lateinit var armorButton: ImageButton
    private lateinit var staffButton: ImageButton
    private lateinit var heroImage: ImageView
    private lateinit var armorCountText: TextView
    private lateinit var armorProgressIndicator: ProgressBar

    private var currentUser: User? = null
    private var userCoins: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hero)

        initializeFirebase()
        initializeUI()
        setupBottomNavigation()
        loadUserData()
        setupEquipmentListener()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }

    private fun initializeUI() {
        usernameTextView = findViewById(R.id.usernameText)
        xpTextView = findViewById(R.id.experienceText)
        coinsTextView = findViewById(R.id.currencyText)
        hpTextView = findViewById(R.id.hpText)
        atkTextView = findViewById(R.id.atkText)
        armorButton = findViewById(R.id.armorButton)
        staffButton = findViewById(R.id.staffButton)
        heroImage = findViewById(R.id.heroImage)
        armorCountText = findViewById(R.id.armorCountText)
        armorProgressIndicator = findViewById(R.id.armorProgressIndicator)

        val avatarImage = findViewById<ImageView>(R.id.avatarImage)
        avatarImage.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        // Set up equipment upgrade click listeners
        armorButton.setOnClickListener {
            currentUser?.let { user ->
                showArmorUpgradeDialog(user)
            }
        }

        staffButton.setOnClickListener {
            currentUser?.let { user ->
                showWandUpgradeDialog(user)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_minigames

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_learning -> {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    true
                }
                R.id.nav_minigames -> true
                R.id.nav_duels -> {
                    startActivity(Intent(this, DuelsActivity::class.java))
                    true
                }
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
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Pokaż ProgressBar lub inny wskaźnik ładowania
            val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
            loadingIndicator?.visibility = View.VISIBLE
            
            // Użyj addListenerForSingleValueEvent zamiast addValueEventListener aby ograniczyć odczyty
            val userRef = database.child("users").child(currentUser.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Zastosuj tę operację w tle
                        Thread {
                            try {
                                // Podstawowe dane użytkownika
                                val uid = snapshot.child("uid").getValue(String::class.java) ?: currentUser.uid
                                val username = snapshot.child("username").getValue(String::class.java) ?: "Użytkownik"
                                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                                val level = snapshot.child("level").getValue(Int::class.java) ?: 1
                                val xp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                                val coins = snapshot.child("coins").getValue(Int::class.java) ?: 0
                                
                                // Ekwipunek
                                val equipmentSnapshot = snapshot.child("equipment")
                                val equipment = if (equipmentSnapshot.exists()) {
                                    try {
                                        equipmentSnapshot.getValue(Equipment::class.java)
                                    } catch (e: Exception) {
                                        Log.e("HeroActivity", "Error parsing equipment: ${e.message}")
                                        Equipment() // Domyślny ekwipunek w przypadku błędu
                                    }
                                } else {
                                    Equipment() // Domyślny ekwipunek jeśli nie ma w bazie
                                } ?: Equipment()
                                
                                // Sprawdź czy jest oczekujące ulepszenie
                                if (equipment.pendingArmorUpgrade) {
                                    // Pokaż dialog z potwierdzeniem ulepszenia
                                    runOnUiThread {
                                        showArmorUpgradeConfirmation(equipment)
                                    }
                                }
                                
                                // Tworzymy obiekt User z odczytanych danych
                                val user = User(
                                    uid = uid,
                                    username = username,
                                    email = email,
                                    level = level,
                                    xp = xp,
                                    coins = coins,
                                    equipment = equipment
                                )
                                
                                // Zaktualizuj UI na głównym wątku
                                runOnUiThread {
                                    // Ukryj ProgressBar
                                    loadingIndicator?.visibility = View.GONE
                                    
                                    // Aktualizujemy UI
                                    this@HeroActivity.currentUser = user
                                    updateUserUI(user)
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    loadingIndicator?.visibility = View.GONE
                                    Log.e("HeroActivity", "Error parsing user data: ${e.message}")
                                    showError("Błąd podczas ładowania danych użytkownika: ${e.message}")
                                }
                            }
                        }.start()
                        
                    } catch (e: Exception) {
                        loadingIndicator?.visibility = View.GONE
                        Log.e("HeroActivity", "Error in loadUserData: ${e.message}")
                        showError("Błąd podczas ładowania danych użytkownika: ${e.message}")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    loadingIndicator?.visibility = View.GONE
                    showError("Błąd podczas ładowania danych użytkownika: ${error.message}")
                }
            })
        }
    }

    private fun updateUserUI(user: User) {
        // Uruchom aktualizację UI na wątku roboczym
        Thread {
            // Przygotuj dane
            val usernameTxt = user.username
            val xpTxt = "${user.xp} XP"
            val coinsTxt = "${user.coins} coins"
            val hpTxt = user.equipment.getCurrentHp().toString()
            val atkTxt = user.equipment.getCurrentDamage().toString()
            
            // Zaktualizuj UI na głównym wątku
            runOnUiThread {
                usernameTextView.text = usernameTxt
                xpTextView.text = xpTxt
                coinsTextView.text = coinsTxt
                userCoins = user.coins
                
                // Update hero stats based on equipment
                hpTextView.text = hpTxt
                atkTextView.text = atkTxt
                
                // Update armor UI
                updateArmorUI(user.equipment)
            }
        }.start()
    }

    private fun updateArmorUI(equipment: Equipment, animate: Boolean = false) {
        // Przygotuj dane - ten kod może być wykonany poza głównym wątkiem
        val armorImageResource = equipment.armorTier.getImageResourceId()
        val staffImageResource = equipment.wandType.getWandImageResourceId()
        val characterImageResource = equipment.getCharacterImageByElement()
        
        val armorCount = equipment.getCurrentTierArmorCount()
        val maxArmorCount = 10
        val armorCountText = "$armorCount/$maxArmorCount"
        
        val progress = (armorCount.toFloat() / maxArmorCount * 100).toInt()
        
        val colorResId = when (equipment.armorTier) {
            ArmorTier.BRONZE -> R.color.progress_bronze
            ArmorTier.SILVER -> R.color.progress_silver
            ArmorTier.GOLD -> R.color.progress_gold
        }
        
        // Zaktualizuj UI na głównym wątku
        runOnUiThread {
            if (animate) {
                // Animacja zmiany zbroi
                val armorAnimation = AnimationUtils.loadAnimation(this, R.anim.armor_upgrade_animation)
                armorAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        // Rozpoczęcie animacji
                    }
                    
                    override fun onAnimationEnd(animation: Animation?) {
                        // Po zakończeniu animacji
                        armorButton.setImageResource(armorImageResource)
                        heroImage.setImageResource(characterImageResource)
                    }
                    
                    override fun onAnimationRepeat(animation: Animation?) {
                        // Powtórzenie animacji
                    }
                })
                
                // Rozpocznij animację
                armorButton.startAnimation(armorAnimation)
                heroImage.startAnimation(armorAnimation)
            } else {
                // Aktualizacja bez animacji
                armorButton.setImageResource(armorImageResource)
                heroImage.setImageResource(characterImageResource)
            }
            
            // Aktualizacja ikony różdżki
            staffButton.setImageResource(staffImageResource)
            
            // Aktualizacja licznika zbroi
            this.armorCountText.text = armorCountText
            
            // Aktualizacja progressbara
            armorProgressIndicator.progress = progress
            
            // Ustawienie koloru progressbara
            ContextCompat.getColorStateList(this, colorResId)?.let { colorStateList ->
                armorProgressIndicator.progressTintList = colorStateList
            }
            
            // Aktualizacja statystyk bohatera na podstawie ekwipunku
            hpTextView.text = equipment.getCurrentHp().toString()
            atkTextView.text = equipment.getCurrentDamage().toString()
            
            // Aktualizacja maksymalnego poziomu
            val maxLevel = equipment.getMaxLevelForCurrentTier()
            val currentLevel = equipment.armorLevel
            val levelText = "Poziom: $currentLevel/$maxLevel"
            findViewById<TextView>(R.id.armorLevelText).text = levelText
        }
    }

    private fun showArmorUpgradeDialog(user: User) {
        // Przygotuj dane w tle
        Thread {
            val equipment = user.equipment
            val currentLevel = equipment.armorLevel
            val maxLevel = equipment.getMaxLevelForCurrentTier()
            val currentStat = equipment.getCurrentHp()
            val upgradedStat = equipment.getUpgradedHp()
            val statDifference = upgradedStat - currentStat
            val upgradeCost = equipment.getArmorUpgradeCost()
            val armorImageResource = equipment.armorTier.getImageResourceId()
            
            val armorCount = equipment.getCurrentTierArmorCount()
            val progress = (currentLevel.toFloat() / maxLevel * 100).toInt()
            
            // Utwórz i pokaż dialog na głównym wątku
            runOnUiThread {
                val dialog = Dialog(this)
                dialog.setContentView(R.layout.dialog_equipment_upgrade)
                
                // Set dialog content
                dialog.findViewById<TextView>(R.id.dialogTitle).text = "Ulepszenie Zbroi"
                dialog.findViewById<ImageView>(R.id.equipmentImage).setImageResource(armorImageResource)
                dialog.findViewById<TextView>(R.id.currentLevelText).text = "Aktualny poziom: $currentLevel/$maxLevel (${equipment.armorTier.name})"
                dialog.findViewById<TextView>(R.id.currentStatText).text = currentStat.toString()
                dialog.findViewById<TextView>(R.id.upgradeStatText).text = upgradedStat.toString()
                dialog.findViewById<TextView>(R.id.statDifferenceText).text = " (+$statDifference)"
                dialog.findViewById<TextView>(R.id.upgradeCostText).text = upgradeCost.toString()
                
                // Kontener dla sekcji informacji o zebranych zbrojach
                val armorInfoLayout = dialog.findViewById<LinearLayout>(R.id.extraInfoLayout)
                armorInfoLayout.removeAllViews()
                
                // Dodaj nagłówek
                val headerText = TextView(this)
                headerText.text = "Zebrane zbroje:"
                headerText.textSize = 16f
                headerText.setTypeface(null, Typeface.BOLD)
                headerText.setPadding(0, 16, 0, 16)
                armorInfoLayout.addView(headerText)
                
                // Dodaj aktualny postęp
                val currentProgressLayout = LinearLayout(this)
                currentProgressLayout.orientation = LinearLayout.HORIZONTAL
                currentProgressLayout.gravity = Gravity.CENTER_VERTICAL
                
                val currentArmorText = TextView(this)
                currentArmorText.text = "${equipment.getCurrentTierArmorCount()}/10 (${equipment.armorTier.name})"
                currentArmorText.setPadding(0, 8, 0, 8)
                
                val currentTierImage = ImageView(this)
                currentTierImage.setImageResource(equipment.armorTier.getImageResourceId())
                currentTierImage.layoutParams = LinearLayout.LayoutParams(48, 48)
                
                currentProgressLayout.addView(currentTierImage)
                currentProgressLayout.addView(currentArmorText)
                armorInfoLayout.addView(currentProgressLayout)
                
                // Dodaj informacje o brązowych zbrojach
                val bronzeLayout = LinearLayout(this)
                bronzeLayout.orientation = LinearLayout.HORIZONTAL
                bronzeLayout.gravity = Gravity.CENTER_VERTICAL
                
                val bronzeImage = ImageView(this)
                bronzeImage.setImageResource(R.drawable.ic_armor_bronze)
                bronzeImage.layoutParams = LinearLayout.LayoutParams(48, 48)
                
                val bronzeText = TextView(this)
                bronzeText.text = "Brązowe: ${equipment.getArmorCount(ArmorTier.BRONZE)}"
                bronzeText.setPadding(8, 8, 0, 8)
                
                bronzeLayout.addView(bronzeImage)
                bronzeLayout.addView(bronzeText)
                armorInfoLayout.addView(bronzeLayout)
                
                // Dodaj informacje o srebrnych zbrojach
                val silverLayout = LinearLayout(this)
                silverLayout.orientation = LinearLayout.HORIZONTAL
                silverLayout.gravity = Gravity.CENTER_VERTICAL
                
                val silverImage = ImageView(this)
                silverImage.setImageResource(R.drawable.ic_armor_silver)
                silverImage.layoutParams = LinearLayout.LayoutParams(48, 48)
                
                val silverText = TextView(this)
                silverText.text = "Srebrne: ${equipment.getArmorCount(ArmorTier.SILVER)}"
                silverText.setPadding(8, 8, 0, 8)
                
                silverLayout.addView(silverImage)
                silverLayout.addView(silverText)
                armorInfoLayout.addView(silverLayout)
                
                // Dodaj informacje o złotych zbrojach
                val goldLayout = LinearLayout(this)
                goldLayout.orientation = LinearLayout.HORIZONTAL
                goldLayout.gravity = Gravity.CENTER_VERTICAL
                
                val goldImage = ImageView(this)
                goldImage.setImageResource(R.drawable.ic_armor_silver) // Tymczasowo używamy srebrnej zbroi dla złotego poziomu
                goldImage.layoutParams = LinearLayout.LayoutParams(48, 48)
                
                val goldText = TextView(this)
                goldText.text = "Złote: ${equipment.getArmorCount(ArmorTier.GOLD)}"
                goldText.setPadding(8, 8, 0, 8)
                
                goldLayout.addView(goldImage)
                goldLayout.addView(goldText)
                armorInfoLayout.addView(goldLayout)
                
                // Dodaj informację o zdobywaniu
                val infoText = TextView(this)
                infoText.text = "\nUzbrojenie zdobywasz za ukończenie pojedynków na 3 gwiazdki."
                infoText.setPadding(0, 16, 0, 0)
                armorInfoLayout.addView(infoText)
                
                // Set progress bar based on current level (max 20 levels)
                val progressBar = dialog.findViewById<ProgressBar>(R.id.levelProgressBar)
                progressBar.progress = progress
                
                val upgradeButton = dialog.findViewById<Button>(R.id.upgradeButton)
                
                // Disable upgrade button if max level or not enough coins
                if (!equipment.canUpgradeArmorLevel()) {
                    upgradeButton.isEnabled = false
                    upgradeButton.text = "Maksymalny poziom"
                    upgradeButton.alpha = 0.5f
                } else if (user.coins.toLong() < upgradeCost.toLong()) {
                    upgradeButton.isEnabled = false
                    upgradeButton.text = "Za mało monet"
                    upgradeButton.alpha = 0.5f
                }
                
                dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                    dialog.dismiss()
                }
                
                upgradeButton.setOnClickListener {
                    if (user.coins.toLong() >= upgradeCost.toLong()) {
                        dialog.dismiss() // Zamknij dialog przed wykonaniem operacji
                        // Wykonaj ulepszenie w osobnym wątku
                        Thread {
                            upgradeArmor(user, equipment, upgradeCost)
                        }.start()
                    } else {
                        showError("Nie masz wystarczającej liczby monet!")
                    }
                }
                
                // Make dialog fill width with slight margins
                dialog.window?.setLayout(
                    (resources.displayMetrics.widthPixels * 0.9).toInt(),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                
                dialog.show()
            }
        }.start()
    }
    
    private fun showWandUpgradeDialog(user: User) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_equipment_upgrade)
        
        val equipment = user.equipment
        val currentLevel = equipment.wandLevel
        val currentStat = equipment.getCurrentDamage()
        val upgradedStat = equipment.getUpgradedDamage()
        val statDifference = upgradedStat - currentStat
        val upgradeCost = equipment.getWandUpgradeCost()
        
        // Set dialog content
        dialog.findViewById<TextView>(R.id.dialogTitle).text = "Ulepszenie Różdżki"
        dialog.findViewById<ImageView>(R.id.equipmentImage).setImageResource(equipment.wandType.getWandImageResourceId())
        dialog.findViewById<TextView>(R.id.currentLevelText).text = "Aktualny poziom: $currentLevel (${equipment.wandType.displayName})"
        dialog.findViewById<TextView>(R.id.currentStatText).text = currentStat.toString()
        dialog.findViewById<TextView>(R.id.upgradeStatText).text = upgradedStat.toString()
        dialog.findViewById<TextView>(R.id.statDifferenceText).text = " (+$statDifference)"
        dialog.findViewById<TextView>(R.id.upgradeCostText).text = upgradeCost.toString()
        
        // Dodaj informację o efektach różdżki
        val wandEffectInfo = dialog.findViewById<TextView>(R.id.extraInfoText)
        wandEffectInfo.visibility = View.VISIBLE
        wandEffectInfo.text = "Efekt różdżki: ${equipment.wandType.getEffectDescription()}"
        
        // Add buttons for changing wand type
        val wandTypeButtonsContainer = LinearLayout(this)
        wandTypeButtonsContainer.orientation = LinearLayout.HORIZONTAL
        wandTypeButtonsContainer.gravity = Gravity.CENTER
        
        // Create buttons for each wand type
        WandType.values().forEach { wandType ->
            val button = Button(this)
            button.text = wandType.displayName
            button.setOnClickListener {
                if (equipment.wandType != wandType) {
                    changeWandType(user, equipment, wandType)
                    dialog.dismiss()
                }
            }
            
            // Highlight the current wand type button
            if (equipment.wandType == wandType) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.light_gray))
                button.setTextColor(Color.BLACK)
            }
            
            // Set layout parameters
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            params.setMargins(8, 8, 8, 8)
            button.layoutParams = params
            
            wandTypeButtonsContainer.addView(button)
        }
        
        // Add the wand type buttons container to dialog
        val extraInfoContainer = dialog.findViewById<LinearLayout>(R.id.extraInfoContainer)
        extraInfoContainer.addView(wandTypeButtonsContainer)
        
        // Set progress bar based on current level (max 20 levels)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.levelProgressBar)
        val maxLevel = 20
        val progress = (currentLevel.toFloat() / maxLevel * 100).toInt()
        progressBar.progress = progress
        
        val upgradeButton = dialog.findViewById<Button>(R.id.upgradeButton)
        
        // Disable upgrade button if not enough coins
        if (user.coins.toLong() < upgradeCost.toLong()) {
            upgradeButton.isEnabled = false
            upgradeButton.text = "Za mało monet"
            upgradeButton.alpha = 0.5f
        }
        
        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
        
        upgradeButton.setOnClickListener {
            if (user.coins.toLong() >= upgradeCost.toLong()) {
                upgradeWand(user, equipment, upgradeCost)
                dialog.dismiss()
            } else {
                showError("Nie masz wystarczającej liczby monet!")
            }
        }
        
        // Make dialog fill width with slight margins
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        dialog.show()
    }
    
    private fun changeWandType(user: User, equipment: Equipment, newWandType: WandType) {
        val currentUserId = auth.currentUser?.uid ?: return
        val newEquipment = equipment.changeWandType(newWandType)
        
        // Pokaż wskaźnik postępu
        runOnUiThread {
            val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
            loadingIndicator?.visibility = View.VISIBLE
        }
        
        // Update equipment in Firebase
        val updates = HashMap<String, Any>()
        updates["users/$currentUserId/equipment"] = newEquipment
        
        database.updateChildren(updates)
            .addOnSuccessListener {
                runOnUiThread {
                    // Ukryj wskaźnik ładowania
                    val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
                    loadingIndicator?.visibility = View.GONE
                    
                    // Aktualizacja UI po zmianie różdżki
                    updateArmorUI(newEquipment)
                    showSuccess("Typ różdżki zmieniony na ${newWandType.displayName}!")
                    
                    // Aktualizuj lokalny obiekt użytkownika
                    this@HeroActivity.currentUser = this@HeroActivity.currentUser?.copy(
                        equipment = newEquipment
                    )
                }
            }
            .addOnFailureListener { exception ->
                runOnUiThread {
                    // Ukryj wskaźnik ładowania
                    val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
                    loadingIndicator?.visibility = View.GONE
                    
                    showError("Błąd podczas zmiany typu różdżki: ${exception.message}")
                }
            }
    }
    
    private fun upgradeArmor(user: User, equipment: Equipment, cost: Int) {
        val currentUserId = auth.currentUser?.uid ?: return
        val newEquipment = equipment.upgradeArmor()
        val newCoins = user.coins - cost
        
        // Pokaż wskaźnik postępu
        runOnUiThread {
            val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
            loadingIndicator?.visibility = View.VISIBLE
        }
        
        // Update both equipment and coins in Firebase
        val updates = HashMap<String, Any>()
        updates["users/$currentUserId/equipment"] = newEquipment
        updates["users/$currentUserId/coins"] = newCoins
        
        database.updateChildren(updates)
            .addOnSuccessListener {
                runOnUiThread {
                    // Ukryj wskaźnik ładowania
                    val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
                    loadingIndicator?.visibility = View.GONE
                    
                    // Aktualizuj widok UI po ulepszeniu zbroi
                    updateArmorUI(newEquipment)
                    coinsTextView.text = "$newCoins coins"
                    showSuccess("Zbroja ulepszona pomyślnie!")
                    
                    // Aktualizuj lokalny obiekt użytkownika
                    this@HeroActivity.currentUser = this@HeroActivity.currentUser?.copy(
                        equipment = newEquipment,
                        coins = newCoins
                    )
                }
            }
            .addOnFailureListener { exception ->
                runOnUiThread {
                    // Ukryj wskaźnik ładowania
                    val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
                    loadingIndicator?.visibility = View.GONE
                    
                    showError("Błąd podczas ulepszania: ${exception.message}")
                }
            }
    }
    
    private fun upgradeWand(user: User, equipment: Equipment, cost: Int) {
        val currentUserId = auth.currentUser?.uid ?: return
        val newEquipment = equipment.upgradeWand()
        val newCoins = user.coins - cost
        
        // Pokaż wskaźnik postępu
        runOnUiThread {
            val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
            loadingIndicator?.visibility = View.VISIBLE
        }
        
        // Update both equipment and coins in Firebase
        val updates = HashMap<String, Any>()
        updates["users/$currentUserId/equipment"] = newEquipment
        updates["users/$currentUserId/coins"] = newCoins
        
        database.updateChildren(updates)
            .addOnSuccessListener {
                runOnUiThread {
                    // Ukryj wskaźnik ładowania
                    val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
                    loadingIndicator?.visibility = View.GONE
                    
                    // Aktualizuj widok UI po ulepszeniu różdżki
                    updateArmorUI(newEquipment)
                    coinsTextView.text = "$newCoins coins"
                    showSuccess("Różdżka ulepszona pomyślnie!")
                    
                    // Aktualizuj lokalny obiekt użytkownika
                    this@HeroActivity.currentUser = this@HeroActivity.currentUser?.copy(
                        equipment = newEquipment,
                        coins = newCoins
                    )
                }
            }
            .addOnFailureListener { exception ->
                runOnUiThread {
                    // Ukryj wskaźnik ładowania
                    val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
                    loadingIndicator?.visibility = View.GONE
                    
                    showError("Błąd podczas ulepszania: ${exception.message}")
                }
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showCustomToast(message: String, isUpgrade: Boolean = false) {
        // Przygotuj layout dla toasta
        val layout = layoutInflater.inflate(R.layout.custom_toast_layout, null)
        
        // Znajdź widoki w layoucie
        val messageText = layout.findViewById<TextView>(R.id.toastMessage)
        val iconView = layout.findViewById<ImageView>(R.id.toastIcon)
        
        // Ustaw tekst i ikonę
        messageText.text = message
        if (isUpgrade) {
            iconView.setImageResource(R.drawable.ic_level_up)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
        
        // Stwórz i pokaż Toast
        val toast = Toast(this)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    private fun setupEquipmentListener() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val equipmentRef = database.child("users").child(currentUser.uid).child("equipment")
            equipmentRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Przetwarzaj dane w oddzielnym wątku
                        Thread {
                            try {
                                val equipment = snapshot.getValue(Equipment::class.java)
                                if (equipment != null) {
                                    // Aktualizuj UI na głównym wątku
                                    runOnUiThread {
                                        updateArmorUI(equipment)
                                        Log.d("HeroActivity", "Ekwipunek zaktualizowany: Brązowe zbroje: ${equipment.bronzeArmorCount}")
                                        
                                        // Aktualizuj lokalny obiekt użytkownika
                                        this@HeroActivity.currentUser = this@HeroActivity.currentUser?.copy(
                                            equipment = equipment
                                        )
                                    }
                                } else {
                                    // Alternatywny sposób odczytania danych, jeśli deserializacja bezpośrednia nie zadziała
                                    try {
                                        // Odczytaj pola ręcznie
                                        val armorLevel = snapshot.child("armorLevel").getValue(Long::class.java)?.toInt() ?: 1
                                        val wandLevel = snapshot.child("wandLevel").getValue(Long::class.java)?.toInt() ?: 1
                                        val baseHp = snapshot.child("baseHp").getValue(Long::class.java)?.toInt() ?: 100
                                        val baseDamage = snapshot.child("baseDamage").getValue(Long::class.java)?.toInt() ?: 10
                                        
                                        // Użyj nowej metody do konwersji armorTier
                                        val armorTierValue = snapshot.child("armorTier").getValue()
                                        val armorTier = ArmorTier.fromAny(armorTierValue)
                                        
                                        val bronzeArmorCount = snapshot.child("bronzeArmorCount").getValue(Long::class.java)?.toInt() ?: 0
                                        val silverArmorCount = snapshot.child("silverArmorCount").getValue(Long::class.java)?.toInt() ?: 0
                                        val goldArmorCount = snapshot.child("goldArmorCount").getValue(Long::class.java)?.toInt() ?: 0
                                        
                                        val wandTypeStr = snapshot.child("wandType").getValue(String::class.java) ?: "FIRE"
                                        val wandType = try {
                                            WandType.valueOf(wandTypeStr)
                                        } catch (e: Exception) {
                                            WandType.FIRE
                                        }
                                        
                                        val manualEquipment = Equipment(
                                            armorLevel = armorLevel,
                                            wandLevel = wandLevel,
                                            baseHp = baseHp,
                                            baseDamage = baseDamage,
                                            armorTier = armorTier,
                                            bronzeArmorCount = bronzeArmorCount,
                                            silverArmorCount = silverArmorCount,
                                            goldArmorCount = goldArmorCount,
                                            wandType = wandType
                                        )
                                        
                                        runOnUiThread {
                                            updateArmorUI(manualEquipment)
                                            Log.d("HeroActivity", "Ekwipunek zaktualizowany (ręcznie): Brązowe zbroje: ${manualEquipment.bronzeArmorCount}")
                                            
                                            // Aktualizuj lokalny obiekt użytkownika
                                            this@HeroActivity.currentUser = this@HeroActivity.currentUser?.copy(
                                                equipment = manualEquipment
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e("HeroActivity", "Błąd podczas ręcznej aktualizacji ekwipunku: ${e.message}")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("HeroActivity", "Błąd podczas aktualizacji ekwipunku: ${e.message}")
                            }
                        }.start()
                    } catch (e: Exception) {
                        Log.e("HeroActivity", "Błąd podczas przetwarzania danych ekwipunku: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HeroActivity", "Anulowano nasłuchiwanie ekwipunku: ${error.message}")
                }
            })
        }
    }

    private fun showArmorUpgradeConfirmation(equipment: Equipment) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_armor_upgrade_confirmation)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Ustaw obrazki zbroi
        val currentArmorImage = dialog.findViewById<ImageView>(R.id.currentArmorImage)
        val nextArmorImage = dialog.findViewById<ImageView>(R.id.nextArmorImage)
        
        val nextTier = when (equipment.armorTier) {
            ArmorTier.BRONZE -> {
                currentArmorImage.setImageResource(R.drawable.ic_armor_bronze)
                nextArmorImage.setImageResource(R.drawable.ic_armor_silver)
                "srebrnej"
            }
            ArmorTier.SILVER -> {
                currentArmorImage.setImageResource(R.drawable.ic_armor_silver)
                nextArmorImage.setImageResource(R.drawable.ic_armor_gold)
                "złotej"
            }
            ArmorTier.GOLD -> return // Nie powinno się zdarzyć
        }
        
        dialog.findViewById<TextView>(R.id.dialogMessage).text = 
            "Zebrałeś wystarczającą ilość elementów zbroi, aby ulepszyć ją do $nextTier!\n\n" +
            "Twoja postać otrzyma nowy wygląd i zwiększone statystyki!"
        
        // Dodaj animację dla obrazków zbroi
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.armor_upgrade_animation)
        nextArmorImage.startAnimation(scaleAnimation)
        
        dialog.findViewById<Button>(R.id.yesButton).setOnClickListener {
            dialog.dismiss()
            performArmorUpgrade(equipment)
        }
        
        dialog.findViewById<Button>(R.id.noButton).setOnClickListener {
            dialog.dismiss()
        }
        
        // Ustaw szerokość dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        dialog.show()
    }
    
    private fun performArmorUpgrade(equipment: Equipment) {
        val currentUser = auth.currentUser ?: return
        val updatedEquipment = equipment.performPendingUpgrade()
        
        // Pokaż wskaźnik ładowania
        val loadingIndicator = findViewById<ProgressBar>(R.id.loadingIndicator)
        loadingIndicator?.visibility = View.VISIBLE
        
        // Aktualizuj w bazie danych
        database.child("users").child(currentUser.uid).child("equipment")
            .setValue(updatedEquipment)
            .addOnSuccessListener {
                loadingIndicator?.visibility = View.GONE
                
                // Pokaż animację i komunikat o ulepszeniu
                updateArmorUI(updatedEquipment, true)
                
                val message = when (updatedEquipment.armorTier) {
                    ArmorTier.SILVER -> "Gratulacje! Twoja zbroja została ulepszona do srebrnej!\nOdblokowano nowy wygląd postaci z lepszą ochroną!"
                    ArmorTier.GOLD -> "Gratulacje! Twoja zbroja została ulepszona do złotej!\nOdblokowano nowy wygląd postaci z najlepszą ochroną!"
                    else -> "Zbroja została ulepszona!"
                }
                showCustomToast(message, true)
                
                // Aktualizuj lokalny obiekt użytkownika
                this@HeroActivity.currentUser = this@HeroActivity.currentUser?.copy(
                    equipment = updatedEquipment
                )
            }
            .addOnFailureListener { e ->
                loadingIndicator?.visibility = View.GONE
                showError("Błąd podczas ulepszania zbroi: ${e.message}")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Usuń nasłuchiwanie zmian w ekwipunku przy zniszczeniu aktywności
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val equipmentRef = database.child("users").child(currentUser.uid).child("equipment")
            equipmentRef.removeEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}
