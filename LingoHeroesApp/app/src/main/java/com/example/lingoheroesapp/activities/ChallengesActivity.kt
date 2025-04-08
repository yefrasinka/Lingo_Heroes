package com.example.lingoheroesapp.activities

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.ChallengesAdapter
import com.example.lingoheroesapp.models.Challenge
import com.example.lingoheroesapp.models.ChallengeType
import com.example.lingoheroesapp.utils.ChallengeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChallengesActivity : AppCompatActivity() {
    private lateinit var challengesAdapter: ChallengesAdapter
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenges)

        setupViews()
        
        val filterType = intent.getStringExtra("filter")?.let { 
            try {
                ChallengeType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        
        // Sprawdź czy są jakieś zaległe nagrody do odebrania
        ChallengeManager.checkAndResetExpiredChallenges()
        
        // Ładuj wyzwania
        loadChallenges(filterType)
    }

    private fun setupViews() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        challengesAdapter = ChallengesAdapter()
        challengesAdapter.setChallengeClickListener(object : ChallengesAdapter.ChallengeClickListener {
            override fun onClaimRewardClicked(challenge: Challenge) {
                // Adapter będzie obsługiwał odbiór nagrody samodzielnie
            }
        })
        
        findViewById<RecyclerView>(R.id.challengesRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@ChallengesActivity)
            adapter = challengesAdapter
        }
    }

    private fun loadChallenges(filterType: ChallengeType? = null) {
        val currentUser = auth.currentUser ?: return
        val userRef = database.getReference("users")
            .child(currentUser.uid)
            .child("challenges")

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    // Inicjalizuj domyślne wyzwania, jeśli nie istnieją
                    ChallengeManager.createDefaultChallengesForUser(currentUser.uid)
                    return
                }
                
                val challenges = mutableListOf<Challenge>()
                
                snapshot.children.forEach { challengeSnapshot ->
                    val challenge = challengeSnapshot.getValue(Challenge::class.java)
                    challenge?.let {
                        if (filterType == null || it.type == filterType) {
                            challenges.add(it)
                        }
                    }
                }
                
                // Sortuj wyzwania: najpierw typy (daily przed weekly), potem nieukończone przed ukończonymi
                val sortedChallenges = challenges.sortedWith(
                    compareBy(
                        { it.type.ordinal },                  // Sortuj po typie (DAILY = 0, WEEKLY = 1)
                        { it.isCompleted },                  // Nieukończone najpierw
                        { it.isRewardClaimed },             // Nieodebrane nagrody najpierw
                        { it.requiredValue - it.currentProgress } // Najbliższe ukończenia najpierw
                    )
                )
                
                challengesAdapter.updateChallenges(sortedChallenges)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChallengesActivity, 
                    "Błąd podczas ładowania wyzwań: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
} 