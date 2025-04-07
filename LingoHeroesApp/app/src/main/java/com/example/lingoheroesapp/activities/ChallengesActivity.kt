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
        
        // Sprawdź czy są jakieś zaległe nagrody do odebrania
        checkForUnclaimedRewards()
        
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
                if (challenge.isCompleted && !challenge.isRewardClaimed) {
                    awardChallengeReward(auth.currentUser?.uid ?: return, challenge)
                }
            }
        })
        
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
                    
                    if (!snapshot.exists() || !snapshot.hasChildren()) {
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
        val currentTime = System.currentTimeMillis()
        
        val newExpiresAt = when (challenge.type) {
            ChallengeType.DAILY -> {
                calendar.apply {
                    // Ustawienie końca następnego dnia
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
            ChallengeType.WEEKLY -> {
                calendar.apply {
                    // Znajdź następną niedzielę (koniec tygodnia)
                    add(Calendar.DAY_OF_YEAR, 1) // Przesunięcie do przodu o przynajmniej jeden dzień
                    while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
        }
        
        // Tworzymy nowy obiekt Challenge z zerowymi wartościami postępu
        val newChallenge = Challenge(
            id = challenge.id,
            title = challenge.title,
            description = challenge.description,
            type = challenge.type,
            requiredValue = challenge.requiredValue,
            currentProgress = 0,
            reward = challenge.reward,
            isCompleted = false,
            isRewardClaimed = false,
            expiresAt = newExpiresAt,
            lastUpdateTime = currentTime
        )
        
        return newChallenge
    }

    private fun awardChallengeReward(userId: String, challenge: Challenge) {
        val userRef = database.getReference("users").child(userId)
        val challengeRef = userRef.child("challenges").child(challenge.id)
        
        // Wypisujemy do logów, że próbujemy przyznać nagrodę
        Log.d("ChallengesActivity", "Próba przyznania nagrody za wyzwanie: ${challenge.id}, ${challenge.title}")
        Log.d("ChallengesActivity", "Status wyzwania - ukończone: ${challenge.isCompleted}, nagroda odebrana: ${challenge.isRewardClaimed}")

        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val challengeData = currentData.child("challenges").child(challenge.id)
                
                // Sprawdzamy status
                val isCompleted = challengeData.child("isCompleted").getValue(Boolean::class.java) ?: false
                val isRewardClaimed = challengeData.child("isRewardClaimed").getValue(Boolean::class.java) ?: false
                
                // Zapisujemy logi
                Log.d("ChallengesActivity", "W transakcji - status wyzwania - ukończone: $isCompleted, nagroda odebrana: $isRewardClaimed")
                
                // Sprawdź, czy wyzwanie jest ukończone i nagroda nie została jeszcze odebrana
                if (isRewardClaimed || !isCompleted) {
                    Log.d("ChallengesActivity", "Nie przyznajemy nagrody - warunki nie spełnione")
                    return Transaction.success(currentData)
                }
                
                // Pobierz aktualną liczbę monet
                val coins = currentData.child("coins").getValue(Int::class.java) ?: 0
                val rewardCoins = challenge.reward.coins
                
                // Dodaj monety do konta użytkownika
                currentData.child("coins").value = coins + rewardCoins
                
                // Oznacz wyzwanie jako odebrane
                challengeData.child("isRewardClaimed").value = true
                
                Log.d("ChallengesActivity", "Nagroda przyznana! +$rewardCoins monet")
                
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("ChallengesActivity", "Błąd podczas przyznawania nagrody: ${error.message}")
                    Toast.makeText(
                        this@ChallengesActivity, 
                        "Błąd podczas odbierania nagrody: ${error.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                } else if (committed) {
                    Log.d("ChallengesActivity", "Transakcja wykonana pomyślnie")
                    // Pokaż powiadomienie o przyznaniu nagrody
                    Toast.makeText(
                        this@ChallengesActivity, 
                        "Otrzymałeś ${challenge.reward.coins} monet za ukończenie wyzwania: ${challenge.title}!", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Sprawdź aktualny stan konta
                    userRef.child("coins").get().addOnSuccessListener { snapshot ->
                        val currentCoins = snapshot.getValue(Int::class.java) ?: 0
                        Log.d("ChallengesActivity", "Aktualna liczba monet: $currentCoins")
                    }
                    
                    // Odśwież listę wyzwań
                    loadChallenges()
                }
            }
        })
    }

    private fun createDefaultChallenges(): List<Challenge> {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val endOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.WEEK_OF_YEAR, 1)
        }.timeInMillis

        return listOf(
            Challenge(
                id = "daily_xp",
                title = "Dzienny zdobywca XP",
                description = "Zdobądź 100 XP w ciągu dnia",
                type = ChallengeType.DAILY,
                requiredValue = 100,
                currentProgress = 0,
                reward = Reward(coins = 150),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfDay,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "daily_tasks",
                title = "Codzienna praktyka",
                description = "Ukończ 5 zadań",
                type = ChallengeType.DAILY,
                requiredValue = 5,
                currentProgress = 0,
                reward = Reward(coins = 100),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfDay,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "weekly_perfect",
                title = "Perfekcyjny tydzień",
                description = "Zdobądź 10 perfekcyjnych wyników w tym tygodniu",
                type = ChallengeType.WEEKLY,
                requiredValue = 10,
                currentProgress = 0,
                reward = Reward(coins = 500),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfWeek,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "weekly_streak",
                title = "Tygodniowa seria",
                description = "Utrzymaj 7-dniową serię nauki",
                type = ChallengeType.WEEKLY,
                requiredValue = 7,
                currentProgress = 0,
                reward = Reward(coins = 400),
                isCompleted = false,
                isRewardClaimed = false,
                expiresAt = endOfWeek,
                lastUpdateTime = currentTime
            )
        )
    }

    // Nowa metoda do sprawdzania i automatycznego przyznawania zaległych nagród
    private fun checkForUnclaimedRewards() {
        val currentUser = auth.currentUser ?: return
        val userRef = database.getReference("users").child(currentUser.uid)
        
        Log.d("ChallengesActivity", "Sprawdzanie zaległych nagród...")
        
        userRef.child("challenges").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasUnclaimedRewards = false
                var totalCoins = 0
                
                Log.d("ChallengesActivity", "Znaleziono ${snapshot.childrenCount} wyzwań")
                
                snapshot.children.forEach { challengeSnapshot ->
                    val challenge = challengeSnapshot.getValue(Challenge::class.java)
                    if (challenge != null) {
                        // Sprawdzamy status
                        val isCompleted = challengeSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false
                        val isRewardClaimed = challengeSnapshot.child("isRewardClaimed").getValue(Boolean::class.java) ?: false
                        
                        Log.d("ChallengesActivity", "Wyzwanie ${challenge.id} - ukończone: $isCompleted, nagroda odebrana: $isRewardClaimed")
                        
                        if (isCompleted && !isRewardClaimed) {
                            hasUnclaimedRewards = true
                            totalCoins += challenge.reward.coins
                            
                            // Oznacz nagrodę jako odebraną
                            challengeSnapshot.ref.child("isRewardClaimed").setValue(true)
                        }
                    }
                }
                
                // Jeśli są nieodebrane nagrody, przyznaj je
                if (hasUnclaimedRewards) {
                    Log.d("ChallengesActivity", "Znaleziono zaległe nagrody: $totalCoins monet")
                    
                    userRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val currentCoins = currentData.child("coins").getValue(Int::class.java) ?: 0
                            val newCoins = currentCoins + totalCoins
                            currentData.child("coins").value = newCoins
                            
                            Log.d("ChallengesActivity", "Przyznano zaległe nagrody: $totalCoins monet (stare saldo: $currentCoins, nowe saldo: $newCoins)")
                            return Transaction.success(currentData)
                        }
                        
                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                            if (error != null) {
                                Log.e("ChallengesActivity", "Błąd podczas przyznawania zaległych nagród: ${error.message}")
                            } else if (committed) {
                                Log.d("ChallengesActivity", "Pomyślnie przyznano zaległe nagrody")
                                Toast.makeText(
                                    this@ChallengesActivity,
                                    "Przyznano zaległe nagrody: $totalCoins monet!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    })
                } else {
                    Log.d("ChallengesActivity", "Nie znaleziono zaległych nagród")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChallengesActivity", "Błąd podczas sprawdzania zaległych nagród: ${error.message}")
            }
        })
    }
} 