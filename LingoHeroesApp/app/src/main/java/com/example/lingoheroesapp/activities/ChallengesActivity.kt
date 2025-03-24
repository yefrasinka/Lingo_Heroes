package com.example.lingoheroesapp.activities

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.ChallengesAdapter
import com.example.lingoheroesapp.models.Challenge
import com.example.lingoheroesapp.models.ChallengeType
import com.example.lingoheroesapp.models.Reward
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
        
        loadChallenges(filterType)
    }

    private fun setupViews() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        challengesAdapter = ChallengesAdapter()
        findViewById<RecyclerView>(R.id.challengesRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@ChallengesActivity)
            adapter = challengesAdapter
        }
    }

    private fun loadChallenges(filterType: ChallengeType? = null) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userChallengesRef = database.getReference("users")
                .child(currentUser.uid)
                .child("challenges")

            userChallengesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val challenges = mutableListOf<Challenge>()
                    val currentTime = System.currentTimeMillis()
                    
                    if (!snapshot.exists() || snapshot.children.count() == 0) {
                        // Jeśli nie ma wyzwań, tworzymy domyślne
                        val defaultChallenges = createDefaultChallenges()
                        defaultChallenges.forEach { challenge ->
                            userChallengesRef.child(challenge.id).setValue(challenge)
                        }
                        challenges.addAll(defaultChallenges)
                    } else {
                        // Jeśli są wyzwania, sprawdzamy każde z nich
                        snapshot.children.forEach { challengeSnapshot ->
                            val challenge = challengeSnapshot.getValue(Challenge::class.java)
                            challenge?.let {
                                if (it.expiresAt < currentTime) {
                                    // Jeśli wyzwanie wygasło
                                    if (it.isCompleted && !it.isRewardClaimed) {
                                        awardChallengeReward(currentUser.uid, it)
                                    }
                                    // Resetuj wyzwanie
                                    val newChallenge = resetChallenge(it)
                                    userChallengesRef.child(it.id).setValue(newChallenge)
                                    if (filterType == null || newChallenge.type == filterType) {
                                        challenges.add(newChallenge)
                                    }
                                } else {
                                    // Jeśli wyzwanie jest nadal aktywne
                                    if (filterType == null || it.type == filterType) {
                                        challenges.add(it)
                                    }
                                }
                            }
                        }
                    }
                    
                    challengesAdapter.updateChallenges(challenges)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChallengesActivity, 
                        "Błąd podczas ładowania wyzwań: ${error.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun resetChallenge(challenge: Challenge): Challenge {
        val calendar = Calendar.getInstance()
        
        val newExpiresAt = when (challenge.type) {
            ChallengeType.DAILY -> {
                calendar.apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
            }
            ChallengeType.WEEKLY -> {
                calendar.apply {
                    add(Calendar.WEEK_OF_YEAR, 1)
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
            }
        }
        
        return challenge.copy(
            currentProgress = 0,
            isCompleted = false,
            isRewardClaimed = false,
            expiresAt = newExpiresAt
        )
    }

    private fun awardChallengeReward(userId: String, challenge: Challenge) {
        val userRef = database.getReference("users").child(userId)
        
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val coins = currentData.child("coins").getValue(Int::class.java) ?: 0
                currentData.child("coins").value = coins + challenge.reward.coins
                currentData.child("challenges").child(challenge.id).child("isRewardClaimed").value = true
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Toast.makeText(this@ChallengesActivity,
                        "Błąd podczas przyznawania nagrody: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun createDefaultChallenges(): List<Challenge> {
        val calendar = Calendar.getInstance()
        
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val endOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            add(Calendar.WEEK_OF_YEAR, 1)
        }.timeInMillis

        return listOf(
            Challenge(
                id = "daily_xp",
                title = "Dzienny zdobywca XP",
                description = "Zdobądź 100 XP w ciągu dnia",
                type = ChallengeType.DAILY,
                requiredValue = 100,
                reward = Reward(coins = 150),
                expiresAt = endOfDay
            ),
            Challenge(
                id = "daily_tasks",
                title = "Codzienna praktyka",
                description = "Ukończ 5 zadań",
                type = ChallengeType.DAILY,
                requiredValue = 5,
                reward = Reward(coins = 100),
                expiresAt = endOfDay
            ),
            Challenge(
                id = "weekly_perfect",
                title = "Perfekcyjny tydzień",
                description = "Zdobądź 10 perfekcyjnych wyników w tym tygodniu",
                type = ChallengeType.WEEKLY,
                requiredValue = 10,
                reward = Reward(coins = 500),
                expiresAt = endOfWeek
            ),
            Challenge(
                id = "weekly_streak",
                title = "Tygodniowa seria",
                description = "Utrzymaj 7-dniową serię nauki",
                type = ChallengeType.WEEKLY,
                requiredValue = 7,
                reward = Reward(coins = 400),
                expiresAt = endOfWeek
            )
        )
    }
} 