package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.CaseAdapter
import com.example.lingoheroesapp.dialogs.CaseInfoDialog
import com.example.lingoheroesapp.dialogs.CaseOpeningDialog
import com.example.lingoheroesapp.models.Case
import com.example.lingoheroesapp.models.CaseItem
import com.example.lingoheroesapp.models.User
import com.example.lingoheroesapp.services.CaseService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StoreActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var caseService: CaseService

    private lateinit var usernameTextView: TextView
    private lateinit var xpTextView: TextView
    private lateinit var coinsTextView: TextView
    private lateinit var casesRecyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    
    private lateinit var caseAdapter: CaseAdapter
    private var userCoins: Int = 0
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        initializeFirebase()
        initializeUI()
        setupBottomNavigation()
        loadUserData()
        loadCases()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        caseService = CaseService()
        
        // Upewnij się, że użytkownik jest zalogowany
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
        } else {
            // Przekieruj do ekranu logowania
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun initializeUI() {
        usernameTextView = findViewById(R.id.usernameText)
        xpTextView = findViewById(R.id.experienceText)
        coinsTextView = findViewById(R.id.currencyText)
        casesRecyclerView = findViewById(R.id.casesRecyclerView)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        val avatarImage = findViewById<ImageView>(R.id.avatarImage)
        avatarImage.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
        
        // Inicjalizacja adaptera dla skrzynek
        caseAdapter = CaseAdapter(onCaseClicked = { case ->
            showCaseInfo(case)
        })
        
        casesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@StoreActivity)
            adapter = caseAdapter
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_store
        
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_learning -> {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_minigames -> {
                    startActivity(Intent(this, HeroActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_duels -> {
                    startActivity(Intent(this, DuelsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_store -> {
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserData() {
        showLoading(true)
        
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
                            
                            // Zapisz monety użytkownika
                            userCoins = coins
                            
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
                            showLoading(false)
                        } catch (e: Exception) {
                            showError("Błąd podczas przetwarzania danych: ${e.message}")
                            showLoading(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Błąd podczas ładowania danych: ${error.message}")
                        showLoading(false)
                    }
                })
        } else {
            showLoading(false)
        }
    }

    private fun loadCases() {
        showLoading(true)
        
        caseService.getCases { cases ->
            if (cases.isNotEmpty()) {
                caseAdapter.updateCases(cases)
            } else {
                showError("Nie udało się załadować skrzynek")
            }
            showLoading(false)
        }
    }

    private fun updateUserUI(user: User) {
        usernameTextView.text = user.username
        xpTextView.text = "${user.xp} XP"
        coinsTextView.text = "${user.coins} coins"
    }

    private fun showCaseInfo(case: Case) {
        val dialog = CaseInfoDialog(this, case) {
            if (userCoins >= case.price) {
                purchaseCase(case)
            } else {
                showError("Nie masz wystarczającej liczby monet!")
            }
        }
        dialog.show()
    }
    
    private fun purchaseCase(case: Case) {
        showLoading(true)
        
        // Obciążenie konta użytkownika
        caseService.chargeUserForCase(userId, case.price) { success ->
            if (success) {
                // Losowanie przedmiotu ze skrzynki
                val rewardItem = caseService.openCase(case)
                
                // Zapisanie przedmiotu w ekwipunku użytkownika
                caseService.saveItemToUserInventory(userId, rewardItem) { saved ->
                    if (saved) {
                        // Pokaż animację otwierania skrzyni
                        showCaseOpeningAnimation(case, rewardItem)
                        
                        // Odśwież dane użytkownika
                        loadUserData()
                    } else {
                        showError("Nie udało się zapisać nagrody")
                    }
                    showLoading(false)
                }
            } else {
                showError("Nie udało się dokonać zakupu")
                showLoading(false)
            }
        }
    }
    
    private fun showCaseOpeningAnimation(case: Case, rewardItem: CaseItem) {
        val dialog = CaseOpeningDialog(this, case, rewardItem) {
            // Po zamknięciu dialogu można wykonać dodatkowe akcje
        }
        dialog.show()
    }
    
    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
