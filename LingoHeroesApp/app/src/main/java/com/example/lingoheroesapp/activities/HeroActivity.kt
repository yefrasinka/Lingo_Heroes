package com.example.lingoheroesapp.activities

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Equipment
import com.example.lingoheroesapp.models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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

    private var currentUser: User? = null
    private var userCoins: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hero)

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
        hpTextView = findViewById(R.id.hpText)
        atkTextView = findViewById(R.id.atkText)
        armorButton = findViewById(R.id.armorButton)
        staffButton = findViewById(R.id.staffButton)

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
            database.child("users").child(currentUser.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let { 
                            this@HeroActivity.currentUser = it
                            updateUserUI(it) 
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Failed to load user data: ${error.message}")
                    }
                })
        }
    }

    private fun updateUserUI(user: User) {
        usernameTextView.text = user.username
        xpTextView.text = "${user.xp} XP"
        coinsTextView.text = "${user.coins} coins"
        userCoins = user.coins
        
        // Update hero stats based on equipment
        hpTextView.text = user.equipment.getCurrentHp().toString()
        atkTextView.text = user.equipment.getCurrentDamage().toString()
    }

    private fun showArmorUpgradeDialog(user: User) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_equipment_upgrade)
        
        val equipment = user.equipment
        val currentLevel = equipment.armorLevel
        val currentStat = equipment.getCurrentHp()
        val upgradedStat = equipment.getUpgradedHp()
        val statDifference = upgradedStat - currentStat
        val upgradeCost = equipment.getArmorUpgradeCost()
        
        // Set dialog content
        dialog.findViewById<TextView>(R.id.dialogTitle).text = "Ulepszenie Zbroi"
        dialog.findViewById<ImageView>(R.id.equipmentImage).setImageResource(R.drawable.ic_armor_bronze)
        dialog.findViewById<TextView>(R.id.currentLevelText).text = "Aktualny poziom: $currentLevel"
        dialog.findViewById<TextView>(R.id.currentStatText).text = currentStat.toString()
        dialog.findViewById<TextView>(R.id.upgradeStatText).text = upgradedStat.toString()
        dialog.findViewById<TextView>(R.id.statDifferenceText).text = " (+$statDifference)"
        dialog.findViewById<TextView>(R.id.upgradeCostText).text = upgradeCost.toString()
        
        // Set progress bar based on current level (max 20 levels)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.levelProgressBar)
        val maxLevel = 20
        val progress = (currentLevel.toFloat() / maxLevel * 100).toInt()
        progressBar.progress = progress
        
        val upgradeButton = dialog.findViewById<Button>(R.id.upgradeButton)
        
        // Disable upgrade button if not enough coins
        if (user.coins < upgradeCost) {
            upgradeButton.isEnabled = false
            upgradeButton.text = "Za mało monet"
            upgradeButton.alpha = 0.5f
        }
        
        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
        
        upgradeButton.setOnClickListener {
            if (user.coins >= upgradeCost) {
                upgradeArmor(user, equipment, upgradeCost)
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
        dialog.findViewById<ImageView>(R.id.equipmentImage).setImageResource(R.drawable.ic_staff_lightning)
        dialog.findViewById<TextView>(R.id.currentLevelText).text = "Aktualny poziom: $currentLevel"
        dialog.findViewById<TextView>(R.id.currentStatText).text = currentStat.toString()
        dialog.findViewById<TextView>(R.id.upgradeStatText).text = upgradedStat.toString()
        dialog.findViewById<TextView>(R.id.statDifferenceText).text = " (+$statDifference)"
        dialog.findViewById<TextView>(R.id.upgradeCostText).text = upgradeCost.toString()
        
        // Set progress bar based on current level (max 20 levels)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.levelProgressBar)
        val maxLevel = 20
        val progress = (currentLevel.toFloat() / maxLevel * 100).toInt()
        progressBar.progress = progress
        
        val upgradeButton = dialog.findViewById<Button>(R.id.upgradeButton)
        
        // Disable upgrade button if not enough coins
        if (user.coins < upgradeCost) {
            upgradeButton.isEnabled = false
            upgradeButton.text = "Za mało monet"
            upgradeButton.alpha = 0.5f
        }
        
        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
        
        upgradeButton.setOnClickListener {
            if (user.coins >= upgradeCost) {
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
    
    private fun upgradeArmor(user: User, equipment: Equipment, cost: Int) {
        val currentUserId = auth.currentUser?.uid ?: return
        val newEquipment = equipment.upgradeArmor()
        val newCoins = user.coins - cost
        
        // Update both equipment and coins in Firebase
        val updates = HashMap<String, Any>()
        updates["users/$currentUserId/equipment"] = newEquipment
        updates["users/$currentUserId/coins"] = newCoins
        
        database.updateChildren(updates)
            .addOnSuccessListener {
                showSuccess("Zbroja ulepszona pomyślnie!")
            }
            .addOnFailureListener { exception ->
                showError("Błąd podczas ulepszania: ${exception.message}")
            }
    }
    
    private fun upgradeWand(user: User, equipment: Equipment, cost: Int) {
        val currentUserId = auth.currentUser?.uid ?: return
        val newEquipment = equipment.upgradeWand()
        val newCoins = user.coins - cost
        
        // Update both equipment and coins in Firebase
        val updates = HashMap<String, Any>()
        updates["users/$currentUserId/equipment"] = newEquipment
        updates["users/$currentUserId/coins"] = newCoins
        
        database.updateChildren(updates)
            .addOnSuccessListener {
                showSuccess("Różdżka ulepszona pomyślnie!")
            }
            .addOnFailureListener { exception ->
                showError("Błąd podczas ulepszania: ${exception.message}")
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
