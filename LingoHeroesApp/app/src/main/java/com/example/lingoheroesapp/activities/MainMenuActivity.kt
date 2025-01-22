package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Subtopic
import com.example.lingoheroesapp.models.Topic
import com.example.lingoheroesapp.models.TopicProgress
import com.example.lingoheroesapp.models.SubtopicProgress
import com.example.lingoheroesapp.models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainMenuActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var usernameTextView: TextView
    private lateinit var levelTextView: TextView
    private lateinit var xpTextView: TextView
    private lateinit var coinsTextView: TextView
    private lateinit var topicsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebase()
        initializeUI()
        setupBottomNavigation()
        checkUserAndLoadData()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }

    private fun initializeUI() {
        usernameTextView = findViewById(R.id.usernameText)
        levelTextView = findViewById(R.id.levelText)
        xpTextView = findViewById(R.id.experienceText)
        coinsTextView = findViewById(R.id.currencyText)
        topicsContainer = findViewById(R.id.topicsContainer)


            val avatarImage = findViewById<ImageView>(R.id.avatarImage)
            avatarImage.setOnClickListener {
                startActivity(Intent(this, AccountActivity::class.java))
            }
    }


    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_learning

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_learning -> true
                R.id.nav_minigames -> {
                    startActivity(Intent(this, MinigamesActivity::class.java))
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
                else -> false
            }
        }
    }

    private fun checkUserAndLoadData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserData(currentUser.uid)
            loadTopicsForUser(currentUser.uid)
        } else {
            navigateToLogin()
        }
    }

    private fun loadUserData(userId: String) {
        database.child("users").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let { updateUserUI(it) }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to load user data: ${error.message}")
                }
            })
    }

    private fun updateUserUI(user: User) {
        usernameTextView.text = user.username
        levelTextView.text = "Level ${user.level}"
        xpTextView.text = "${user.xp} XP"
        coinsTextView.text = "${user.coins} coins"
    }

    private fun loadTopicsForUser(userId: String) {
        database.child("topics").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val topics = mutableListOf<Topic>()
                snapshot.children.forEach { topicSnapshot ->
                    val topic = topicSnapshot.getValue(Topic::class.java)
                    topic?.let { topics.add(it) }
                }
                loadProgressForTopics(topics, userId)
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Failed to load topics: ${error.message}")
            }
        })
    }

    private fun loadProgressForTopics(topics: List<Topic>, userId: String) {
        database.child("users").child(userId).child("topicsProgress")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val progressMap = mutableMapOf<String, TopicProgress>()

                    snapshot.children.forEach { topicSnapshot ->
                        val key = topicSnapshot.key
                        val topicData = topicSnapshot.getValue(TopicProgress::class.java)

                        if (key != null && topicData != null) {
                            progressMap[key] = topicData
                        } else {
                            Log.e("Firebase", "Invalid topic progress format for $key")
                        }
                    }

                    // Wyświetlanie tematów z postępem
                    displayTopicsWithProgress(topics, progressMap)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to load progress: ${error.message}")
                }
            })
    }

    private fun displayTopicsWithProgress(topics: List<Topic>, progress: Map<String, TopicProgress>) {
        topicsContainer.removeAllViews()
        
        // Pobieramy aktualny poziom użytkownika
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("users").child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userLevel = snapshot.child("level").getValue(Long::class.java)?.toInt() ?: 1
                        
                        // Sortujemy tematy według poziomu
                        val sortedTopics = topics.sortedBy { it.level }
                        
                        sortedTopics.forEach { topic ->
                            val topicProgress = progress[topic.id]
                            val topicView = createTopicView(topic, topicProgress, userLevel)
                            topicsContainer.addView(topicView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Błąd podczas ładowania poziomu użytkownika")
                    }
                })
        }
    }

    private fun createTopicView(topic: Topic, progress: TopicProgress?, userLevel: Int): android.view.View {
        val view = LayoutInflater.from(this).inflate(R.layout.topic_item, null, false)
        val topicTitle = view.findViewById<TextView>(R.id.topicProgressText)
        val subtopicsContainer = view.findViewById<LinearLayout>(R.id.subtopicsContainer)

        // Sprawdzamy, czy temat jest dostępny dla aktualnego poziomu użytkownika
        val isTopicAvailable = topic.level <= userLevel
        
        // Obliczamy postęp tematu
        val completedSubtopics = progress?.completedSubtopics ?: 0
        val totalSubtopics = topic.subtopics.size
        val progressPercentage = if (totalSubtopics > 0) {
            (completedSubtopics * 100) / totalSubtopics
        } else 0

        // Ustawiamy tekst z postępem i poziomem
        val levelText = when(topic.level) {
            1 -> "A1"
            2 -> "A2"
            3 -> "B1"
            4 -> "B2"
            else -> "A1"
        }
        
        if (isTopicAvailable) {
            topicTitle.text = "${topic.title} ($completedSubtopics/$totalSubtopics) - $levelText"
        } else {
            topicTitle.text = "\uD83D\uDD12 ${topic.title} - $levelText" // 🔒 Emoji kłódki
        }

        // Ustawiamy kolor w zależności od postępu i dostępności
        val backgroundColor = when {
            !isTopicAvailable -> ContextCompat.getColor(this, R.color.color_locked) // Dodaj nowy kolor
            progressPercentage == 100 -> ContextCompat.getColor(this, R.color.color_green)
            progressPercentage > 0 -> ContextCompat.getColor(this, R.color.color_yellow)
            else -> ContextCompat.getColor(this, R.color.color_gray)
        }
        topicTitle.setBackgroundColor(backgroundColor)

        // Wyświetlamy podtematy tylko jeśli temat jest dostępny
        subtopicsContainer.removeAllViews()
        if (isTopicAvailable) {
            topic.subtopics.forEach { subtopic ->
                val subtopicView = createSubtopicView(
                    subtopic = subtopic,
                    topic = topic,
                    topicId = topic.id,
                    progress = progress?.subtopics?.get(subtopic.id)
                )
                subtopicsContainer.addView(subtopicView)
            }
        } else {
            // Dodajemy informację o wymaganym poziomie
            val lockInfoView = TextView(this).apply {
                text = "Ten temat będzie dostępny na poziomie $levelText"
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setPadding(16, 8, 16, 8)
            }
            subtopicsContainer.addView(lockInfoView)
        }

        return view
    }

    private fun createSubtopicView(
        subtopic: Subtopic, 
        topic: Topic, 
        topicId: String,
        progress: SubtopicProgress?
    ): android.view.View {
        val view = LayoutInflater.from(this).inflate(R.layout.subtopic_item, null, false)
        val subtopicButton = view.findViewById<Button>(R.id.subtopicButton)
        val subtopicProgressBar = view.findViewById<ProgressBar>(R.id.subtopicProgressBar)

        // Pobieramy postęp podtematu
        val completedTasks = progress?.completedTasks ?: 0
        val totalTasks = subtopic.totalTasks

        // Ustawiamy tekst z postępem
        subtopicButton.text = "${subtopic.title} ($completedTasks/$totalTasks)"

        // Ustawiamy progress bar
        val progressPercentage = if (totalTasks > 0) {
            (completedTasks * 100) / totalTasks
        } else 0
        subtopicProgressBar.progress = progressPercentage

        // Ustawiamy kolor w zależności od postępu
        val backgroundColor = when {
            progressPercentage == 100 -> ContextCompat.getColor(this, R.color.color_green)
            progressPercentage > 0 -> ContextCompat.getColor(this, R.color.color_yellow)
            else -> ContextCompat.getColor(this, R.color.color_gray)
        }
        subtopicButton.setBackgroundColor(backgroundColor)

        subtopicButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                if (completedTasks >= totalTasks) {
                    Toast.makeText(this, "Ten test został już ukończony!", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, TaskDisplayActivity::class.java)
                    intent.putExtra("TOPIC_ID", topicId)
                    intent.putExtra("SUBTOPIC_ID", subtopic.id)
                    startActivity(intent)
                }
            }
        }

        return view
    }

    // Dodajemy pomocniczą funkcję do aktualizacji XP i monet
    private fun updateUserRewards(userRef: DatabaseReference, xpReward: Int, coinsReward: Int) {
        userRef.get().addOnSuccessListener { snapshot ->
            val currentXp = snapshot.child("xp").getValue(Long::class.java)?.toInt() ?: 0
            val currentCoins = snapshot.child("coins").getValue(Long::class.java)?.toInt() ?: 0
            
            val updates = hashMapOf<String, Any>(
                "xp" to (currentXp + xpReward),
                "coins" to (currentCoins + coinsReward)
            )
            
            userRef.updateChildren(updates)
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e("MainMenuActivity", message)
    }
}
