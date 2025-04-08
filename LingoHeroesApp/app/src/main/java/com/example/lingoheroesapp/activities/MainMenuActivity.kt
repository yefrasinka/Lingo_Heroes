package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import android.content.res.ColorStateList

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

        // Dodanie obsługi przycisku rankingu
        val rankingButton = findViewById<ImageButton>(R.id.rankingButton)
        rankingButton.setOnClickListener {
            startActivity(Intent(this, RankingActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_learning

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_learning -> true
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
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
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
        xpTextView.text = "${user.xp}"
        coinsTextView.text = "${user.coins}"
    }

    private fun loadTopicsForUser(userId: String) {
        topicsContainer.removeAllViews()
        
        database.child("topics").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Logujemy całą strukturę danych
                Log.d("TopicDebug", "Raw topics data from Firebase: ${snapshot.value}")
                
                val topics = mutableListOf<Topic>()
                
                for (topicSnapshot in snapshot.children) {
                    val topic = topicSnapshot.getValue(Topic::class.java)
                    if (topic != null) {
                        topic.id = topicSnapshot.key ?: continue
                        topics.add(topic)
                        
                        // Logujemy szczegóły każdego tematu
                        Log.d("TopicDebug", """
                            Topic details:
                            ID: ${topic.id}
                            Title: ${topic.title}
                            Level: ${topic.level}
                            Subtopics count: ${topic.subtopics.size}
                            Subtopics: ${topic.subtopics.map { "${it.id}: ${it.title}" }}
                        """.trimIndent())
                    }
                }
                
                val sortedTopics = topics.sortedWith(compareBy({ it.level }, { it.id }))
                loadProgressForTopics(sortedTopics, userId)
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
                    
                    for (topicSnapshot in snapshot.children) {
                        val topicId = topicSnapshot.key
                        val progress = topicSnapshot.getValue(TopicProgress::class.java)
                        if (topicId != null && progress != null) {
                            progressMap[topicId] = progress
                            Log.d("TopicDebug", "Loaded progress for topic: $topicId")
                        }
                    }

                    database.child("users").child(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val userLevel = userSnapshot.child("level").getValue(Long::class.java)?.toInt() ?: 1
                                
                                topicsContainer.removeAllViews()
                                
                                topics.forEach { topic ->
                                    val topicView = createTopicView(topic, progressMap[topic.id], topics, progressMap)
                                    topicsContainer.addView(topicView)
                                    Log.d("TopicDebug", "Added view for topic: ${topic.id} - ${topic.title}")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                showError("Błąd podczas ładowania poziomu użytkownika")
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to load progress: ${error.message}")
                }
            })
    }

    private fun isLevelCompleted(topics: List<Topic>, progressMap: Map<String, TopicProgress>, level: Int): Boolean {
        val levelTopics = topics.filter { it.level == level }
        if (levelTopics.isEmpty()) return true // jeśli nie ma tematów na danym poziomie, uznajemy go za ukończony
        
        return levelTopics.all { topic ->
            val progress = progressMap[topic.id]
            if (progress == null) return@all false
            
            // Sprawdź, czy wszystkie podtematy zostały ukończone
            topic.subtopics.all { subtopic ->
                val subtopicProgress = progress.subtopics[subtopic.id]
                subtopicProgress?.completedTasks == subtopic.tasks.size
            }
        }
    }

    private fun createTopicView(
        topic: Topic, 
        progress: TopicProgress?, 
        allTopics: List<Topic>,
        progressMap: Map<String, TopicProgress>
    ): android.view.View {
        val view = LayoutInflater.from(this).inflate(R.layout.topic_item, null, false)
        val topicTitle = view.findViewById<TextView>(R.id.topicProgressText)
        val topicProgressBar = view.findViewById<ProgressBar>(R.id.topicProgressBar)
        val subtopicsContainer = view.findViewById<LinearLayout>(R.id.subtopicsContainer)

        // Sprawdzamy warunki odblokowania poziomu
        val previousLevelCompleted = if (topic.level == 1) {
            true // Poziom 1 jest zawsze dostępny
        } else {
            isLevelCompleted(allTopics, progressMap, topic.level - 1)
        }

        // Pobieramy aktualny poziom użytkownika i aktualizujemy UI
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("users").child(currentUser.uid)
                .child("level")
                .get()
                .addOnSuccessListener { snapshot ->
                    val userLevel = snapshot.getValue(Long::class.java)?.toInt() ?: 1
                    
                    // Temat jest dostępny jeśli spełniony jest którykolwiek z warunków:
                    // 1. Poprzedni poziom został ukończony
                    // 2. Poziom użytkownika jest wystarczający
                    val isTopicAvailable = previousLevelCompleted || topic.level <= userLevel
                    
                    // Obliczamy postęp tematu
                    val completedSubtopics = progress?.completedSubtopics ?: 0
                    val totalSubtopics = topic.subtopics.size
                    val progressPercentage = if (totalSubtopics > 0) {
                        (completedSubtopics * 100) / totalSubtopics
                    } else 0

                    // Ustawiamy progress bar
                    topicProgressBar.progress = progressPercentage

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
                        topicTitle.setTextColor(ContextCompat.getColor(this, R.color.purple_700))
                        topicProgressBar.visibility = View.VISIBLE
                        
                        // Wyświetlamy podtematy
                        subtopicsContainer.removeAllViews()
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
                        topicTitle.text = "\uD83D\uDD12 ${topic.title} - $levelText" // 🔒 Emoji kłódki
                        topicTitle.setTextColor(ContextCompat.getColor(this, R.color.color_locked))
                        topicProgressBar.visibility = View.GONE
                        subtopicsContainer.removeAllViews()
                    }
                }
                .addOnFailureListener {
                    // W przypadku błędu, pokazujemy temat jako zablokowany
                    topicTitle.text = "\uD83D\uDD12 ${topic.title}"
                    topicTitle.setTextColor(ContextCompat.getColor(this, R.color.color_locked))
                    topicProgressBar.visibility = View.GONE
                    subtopicsContainer.removeAllViews()
                }
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

        Log.d("SubtopicDebug", "Creating view for subtopic: ${subtopic.id} in topic: $topicId")

        // Ustawiamy początkowy tekst na przycisku
        subtopicButton.text = subtopic.title
        subtopicButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        // Pobieramy aktualny progress z bazy danych
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.child("users").child(currentUser.uid)
                .child("topicsProgress").child(topicId)
                .child("subtopics").child(subtopic.id)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentProgress = snapshot.getValue(SubtopicProgress::class.java)
                        val completedTasks = currentProgress?.completedTasks ?: 0
                        val totalTasks = subtopic.totalTasks

                        Log.d("SubtopicDebug", "Progress for subtopic ${subtopic.id}: $completedTasks/$totalTasks")

                        // Aktualizujemy UI z aktualnym progressem
                        subtopicButton.text = "${subtopic.title} ($completedTasks/$totalTasks)"
                        val progressPercentage = if (totalTasks > 0) {
                            (completedTasks * 100) / totalTasks
                        } else 0
                        subtopicProgressBar.progress = progressPercentage
                        
                        // Ustawiamy kolor tła przycisku w zależności od postępu
                        when {
                            progressPercentage == 100 -> {
                                subtopicButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainMenuActivity, R.color.color_green))
                            }
                            progressPercentage > 0 -> {
                                subtopicButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainMenuActivity, R.color.teal_700))
                            }
                            else -> {
                                subtopicButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainMenuActivity, R.color.purple_500))
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainMenuActivity", "Error loading subtopic progress: ${error.message}")
                    }
                })
        }

        subtopicButton.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Dodajemy więcej logów do debugowania
                Log.d("SubtopicDebug", """
                    Opening task:
                    Topic ID: $topicId
                    Topic Title: ${topic.title}
                    Topic Level: ${topic.level}
                    Subtopic ID: ${subtopic.id}
                    Subtopic Title: ${subtopic.title}
                """.trimIndent())

                // Sprawdzamy, czy ID są poprawne przed przejściem do TaskDisplayActivity
                if (topicId.isNotEmpty() && subtopic.id.isNotEmpty()) {
                    val intent = Intent(this, TaskDisplayActivity::class.java).apply {
                        putExtra("TOPIC_ID", topicId)
                        putExtra("SUBTOPIC_ID", subtopic.id)
                        // Dodajemy więcej informacji do intentu
                        putExtra("TOPIC_LEVEL", topic.level)
                        putExtra("TOPIC_TITLE", topic.title)
                        putExtra("SUBTOPIC_TITLE", subtopic.title)
                    }
                    startActivity(intent)
                } else {
                    Log.e("SubtopicDebug", "Invalid IDs - Topic ID: $topicId, Subtopic ID: ${subtopic.id}")
                    showError("Błąd: Nieprawidłowe ID tematu lub podtematu")
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

    override fun onResume() {
        super.onResume()
        // Odświeżamy dane po powrocie do aktywności
        checkUserAndLoadData()
    }
}
