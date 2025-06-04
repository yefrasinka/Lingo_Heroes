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
import androidx.appcompat.app.AlertDialog

class MainMenuActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var usernameTextView: TextView
    private lateinit var levelTextView: TextView
    private lateinit var xpTextView: TextView
    private lateinit var coinsTextView: TextView
    private lateinit var topicsContainer: LinearLayout
    
    // Zmienne do przechowywania referencji nasłuchiwania
    private var userValueEventListener: ValueEventListener? = null
    private var topicsProgressValueEventListener: ValueEventListener? = null
    private var userRef: DatabaseReference? = null
    private var topicsProgressRef: DatabaseReference? = null

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
            // Użytkownik jest zalogowany, możemy bezpiecznie załadować dane
            loadUserData(currentUser.uid)
            loadTopicsForUser(currentUser.uid)
        } else {
            // Użytkownik nie jest zalogowany, przekieruj do logowania bez prób pobierania danych
            navigateToLogin()
        }
    }

    private fun loadUserData(userId: String) {
        // Usuwamy poprzednie nasłuchiwanie, jeśli istnieje
        if (userValueEventListener != null && userRef != null) {
            userRef?.removeEventListener(userValueEventListener!!)
            userValueEventListener = null
        }
        
        // Tworzymy nową referencję
        userRef = database.child("users").child(userId)
        
        // Tworzymy i dodajemy nowe nasłuchiwanie
        userValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Używamy ręcznej konwersji zamiast automatycznego mapowania
                    val uid = snapshot.child("uid").getValue(String::class.java) ?: userId
                    val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val level = snapshot.child("level").getValue(Int::class.java) ?: 1
                    val xp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                    val coins = snapshot.child("coins").getValue(Int::class.java) ?: 0
                    
                    // Tworzymy obiekt User z podstawowych danych
                    val user = User(
                        uid = uid,
                        username = username,
                        email = email,
                        level = level,
                        xp = xp,
                        coins = coins
                    )
                    
                    updateUserUI(user)
                } catch (e: Exception) {
                    Log.e("MainMenuActivity", "Error deserializing user data", e)
                    // Tylko wyświetl błąd w logach, nie pokazuj toast użytkownikowi
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainMenuActivity", "Failed to load user data: ${error.message}")
                // Nie pokazuj błędów podczas ładowania jeśli użytkownik zmienił ekran
                if (!isFinishing && !isDestroyed) {
                    showError("Failed to load user data: ${error.message}")
                }
            }
        }
        
        // Dodajemy nasłuchiwanie
        userRef?.addValueEventListener(userValueEventListener!!)
    }

    private fun updateUserUI(user: User) {
        // Sprawdź, czy aktywność wciąż działa
        if (isFinishing || isDestroyed) {
            return
        }
        
        usernameTextView.text = user.username
        levelTextView.text = "Level ${user.level}"
        xpTextView.text = "${user.xp}"
        coinsTextView.text = "${user.coins}"
    }

    private fun loadTopicsForUser(userId: String) {
        // Sprawdź, czy aktywność wciąż działa
        if (isFinishing || isDestroyed) {
            return
        }
        
        topicsContainer.removeAllViews()
        
        database.child("topics").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Sprawdź, czy aktywność wciąż działa
                if (isFinishing || isDestroyed) {
                    return
                }
                
                // Logujemy całą strukturę danych
                Log.d("TopicDebug", "Raw topics data from Firebase: ${snapshot.value}")
                
                var topics = mutableListOf<Topic>()
                
                for (topicSnapshot in snapshot.children) {
                    try {
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
                    } catch (e: Exception) {
                        Log.e("TopicDebug", "Error parsing topic: ${e.message}")
                        // Kontynuuj z kolejnymi tematami
                        continue
                    }
                }
                
                if (topics.isEmpty()) {
                    Log.w("TopicDebug", "No topics found or all topics failed to parse")
                    return
                }
                
                val sortedTopics = topics.sortedWith(compareBy({ it.level }, { it.id }))
                loadProgressForTopics(sortedTopics, userId)
            }

            override fun onCancelled(error: DatabaseError) {
                // Loguj błąd, ale nie pokazuj go użytkownikowi
                Log.e("TopicDebug", "Failed to load topics: ${error.message}")
                
                // Pokaż błąd tylko jeśli aktywność wciąż działa
                if (!isFinishing && !isDestroyed) {
                    showError("Failed to load topics: ${error.message}")
                }
            }
        })
    }

    private fun loadProgressForTopics(topics: List<Topic>, userId: String) {
        // Sprawdź, czy aktywność wciąż działa
        if (isFinishing || isDestroyed) {
            return
        }
        
        // Usuwamy poprzednie nasłuchiwanie, jeśli istnieje
        if (topicsProgressValueEventListener != null && topicsProgressRef != null) {
            topicsProgressRef?.removeEventListener(topicsProgressValueEventListener!!)
            topicsProgressValueEventListener = null
        }
        
        // Tworzymy nową referencję
        topicsProgressRef = database.child("users").child(userId).child("topicsProgress")
        
        // Tworzymy i dodajemy nowe nasłuchiwanie
        topicsProgressValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Sprawdź, czy aktywność wciąż działa
                if (isFinishing || isDestroyed) {
                    return
                }
                
                val progressMap = mutableMapOf<String, TopicProgress>()
                
                try {
                    for (topicSnapshot in snapshot.children) {
                        val topicId = topicSnapshot.key ?: continue
                        
                        // Ręczne mapowanie danych TopicProgress
                        val completedSubtopics = topicSnapshot.child("completedSubtopics").getValue(Int::class.java) ?: 0
                        val totalSubtopics = topicSnapshot.child("totalSubtopics").getValue(Int::class.java) ?: 0
                        val completedTasks = topicSnapshot.child("completedTasks").getValue(Int::class.java) ?: 0
                        val totalTasks = topicSnapshot.child("totalTasks").getValue(Int::class.java) ?: 0
                        
                        // Mapa subtopics
                        val subtopicsMap = mutableMapOf<String, SubtopicProgress>()
                        val subtopicsSnapshot = topicSnapshot.child("subtopics")
                        if (subtopicsSnapshot.exists()) {
                            for (subtopicSnapshot in subtopicsSnapshot.children) {
                                try {
                                    val subtopicId = subtopicSnapshot.key ?: continue
                                    
                                    val subCompletedTasks = subtopicSnapshot.child("completedTasks").getValue(Int::class.java) ?: 0
                                    val subTotalTasks = subtopicSnapshot.child("totalTasks").getValue(Int::class.java) ?: 0
                                    val subTitle = subtopicSnapshot.child("title").getValue(String::class.java) ?: ""
                                    
                                    val subtopicProgress = SubtopicProgress(
                                        completedTasks = subCompletedTasks,
                                        totalTasks = subTotalTasks,
                                        title = subTitle
                                    )
                                    
                                    subtopicsMap[subtopicId] = subtopicProgress
                                } catch (e: Exception) {
                                    Log.e("TopicDebug", "Error parsing subtopic progress: ${e.message}")
                                    continue
                                }
                            }
                        }
                        
                        // Tworzymy obiekt postępu
                        val progress = TopicProgress(
                            completedSubtopics = completedSubtopics,
                            totalSubtopics = totalSubtopics,
                            completedTasks = completedTasks,
                            totalTasks = totalTasks,
                            subtopics = subtopicsMap
                        )
                        
                        progressMap[topicId] = progress
                        Log.d("TopicDebug", "Loaded progress for topic: $topicId")
                    }

                    // Sprawdź, czy aktywność wciąż działa
                    if (isFinishing || isDestroyed) {
                        return
                    }

                    database.child("users").child(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                // Sprawdź, czy aktywność wciąż działa
                                if (isFinishing || isDestroyed) {
                                    return
                                }
                                
                                val userLevel = userSnapshot.child("level").getValue(Long::class.java)?.toInt() ?: 1
                                
                                topicsContainer.removeAllViews()
                                
                                topics.forEach { topic ->
                                    val topicView = createTopicView(topic, progressMap[topic.id], topics, progressMap)
                                    topicsContainer.addView(topicView)
                                    Log.d("TopicDebug", "Added view for topic: ${topic.id} - ${topic.title}")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Loguj błąd, ale nie pokazuj go użytkownikowi
                                Log.e("TopicDebug", "Failed to load user level: ${error.message}")
                            }
                        })
                } catch (e: Exception) {
                    Log.e("TopicDebug", "Error deserializing topics progress", e)
                    // Nie pokazuj błędu na UI, tylko zaloguj
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Loguj błąd, ale nie pokazuj go użytkownikowi
                Log.e("TopicDebug", "Failed to load progress: ${error.message}")
            }
        }
        
        // Dodajemy nasłuchiwanie
        topicsProgressRef?.addValueEventListener(topicsProgressValueEventListener!!)
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
                    // Sprawdź, czy aktywność wciąż działa
                    if (isFinishing || isDestroyed) {
                        return@addOnSuccessListener
                    }
                    
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
                    // Sprawdź, czy aktywność wciąż działa
                    if (isFinishing || isDestroyed) {
                        return@addOnFailureListener
                    }
                    
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

        // Ustawiamy początkowy progress z dostarczonego parametru progress
        val initialCompletedTasks = progress?.completedTasks ?: 0
        val initialTotalTasks = subtopic.totalTasks
        
        subtopicButton.text = "${subtopic.title} ($initialCompletedTasks/$initialTotalTasks)"
        val initialProgressPercentage = if (initialTotalTasks > 0) {
            (initialCompletedTasks * 100) / initialTotalTasks
        } else 0
        subtopicProgressBar.progress = initialProgressPercentage
        
        // Ustawiamy kolor tła przycisku w zależności od początkowego postępu
        when {
            initialProgressPercentage == 100 -> {
                subtopicButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_green))
            }
            initialProgressPercentage > 0 -> {
                subtopicButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal_700))
            }
            else -> {
                subtopicButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500))
            }
        }

        // Pobieramy aktualny progress z bazy danych tylko jeśli użytkownik jest zalogowany
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Używamy addListenerForSingleValueEvent zamiast addValueEventListener, aby
            // uniknąć wielu aktualizacji i potencjalnych błędów po wylogowaniu
            database.child("users").child(currentUser.uid)
                .child("topicsProgress").child(topicId)
                .child("subtopics").child(subtopic.id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Sprawdź, czy aktywność wciąż działa
                        if (isFinishing || isDestroyed) {
                            return
                        }
                        
                        try {
                            val currentProgress = snapshot.getValue(SubtopicProgress::class.java)
                            val completedTasks = currentProgress?.completedTasks ?: initialCompletedTasks
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
                        } catch (e: Exception) {
                            Log.e("SubtopicDebug", "Error processing subtopic progress: ${e.message}")
                            // W przypadku błędu nie aktualizujemy UI, zachowujemy początkowe wartości
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("SubtopicDebug", "Error loading subtopic progress: ${error.message}")
                        // W przypadku błędu nie aktualizujemy UI, zachowujemy początkowe wartości
                    }
                })
        }

        subtopicButton.setOnClickListener {
            val userId = auth.currentUser?.uid ?: run {
                showError("Najpierw się zaloguj")
                return@setOnClickListener
            }

            val topicProgressRef = database.child("users").child(userId).child("topicsProgress").child(topicId)
            topicProgressRef.child("subtopics").child(subtopic.id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val completed = snapshot.child("completedTasks").getValue(Int::class.java) ?: 0
                        val total = subtopic.totalTasks

                        if (completed >= total) {
                            // Повністю завершено — питаємо
                            AlertDialog.Builder(this@MainMenuActivity)
                                .setTitle("Powtórne przejście")
                                .setMessage("Ten test został już ukończony. Zacząć od początku?")
                                .setPositiveButton("Tak") { _, _ ->
                                    resetSubtopicProgress(userId, topicId, subtopic.id, total)
                                    openTaskActivity(topicId, subtopic, topic)
                                }
                                .setNegativeButton("Nie", null)
                                .show()
                        } else {
                            // Не завершено — продовжуємо без обнулення
                            openTaskActivity(topicId, subtopic, topic)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Error loading subtopic progress")
                    }
                })
        }



        return view
    }
    private fun openTaskActivity(topicId: String, subtopic: Subtopic, topic: Topic) {
        val intent = Intent(this, TaskDisplayActivity::class.java).apply {
            putExtra("TOPIC_ID", topicId)
            putExtra("SUBTOPIC_ID", subtopic.id)
            putExtra("TOPIC_LEVEL", topic.level)
            putExtra("TOPIC_TITLE", topic.title)
            putExtra("SUBTOPIC_TITLE", subtopic.title)
        }
        startActivity(intent)
    }

    private fun resetSubtopicProgress(userId: String, topicId: String, subtopicId: String, taskCount: Int) {
        val topicProgressRef = database.child("users").child(userId).child("topicsProgress").child(topicId)

        topicProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val completedSubtopics = snapshot.child("completedSubtopics").getValue(Int::class.java) ?: 0
                val completedTasks = snapshot.child("completedTasks").getValue(Int::class.java) ?: 0
                val subtopicProgress = snapshot.child("subtopics").child(subtopicId)
                val wasCompleted = subtopicProgress.child("completedTasks").getValue(Int::class.java) == taskCount

                val updates = mutableMapOf<String, Any>(
                    "subtopics/$subtopicId/completedTasks" to 0
                )
                if (wasCompleted) {
                    updates["completedSubtopics"] = (completedSubtopics - 1).coerceAtLeast(0)
                }
                updates["completedTasks"] = (completedTasks - (subtopicProgress.child("completedTasks").getValue(Int::class.java) ?: 0)).coerceAtLeast(0)

                topicProgressRef.updateChildren(updates)
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Error: ${error.message}")
            }
        })
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
        // Wyświetl błąd tylko jeśli aktywność wciąż działa
        if (!isFinishing && !isDestroyed) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        Log.e("MainMenuActivity", message)
    }

    // Usuwanie nasłuchiwania przy zamykaniu aktywności
    private fun removeAllListeners() {
        try {
            // Usuwamy nasłuchiwanie użytkownika
            if (userValueEventListener != null && userRef != null) {
                userRef?.removeEventListener(userValueEventListener!!)
                userValueEventListener = null
            }
            
            // Usuwamy nasłuchiwanie postępów tematów
            if (topicsProgressValueEventListener != null && topicsProgressRef != null) {
                topicsProgressRef?.removeEventListener(topicsProgressValueEventListener!!)
                topicsProgressValueEventListener = null
            }
            
            Log.d("MainMenuActivity", "All Firebase listeners removed successfully")
        } catch (e: Exception) {
            Log.e("MainMenuActivity", "Error removing Firebase listeners", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Odświeżamy dane po powrocie do aktywności
        checkUserAndLoadData()
    }
    
    override fun onPause() {
        super.onPause()
        // Usuwamy nasłuchiwania przy wyjściu z aktywności
        removeAllListeners()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Dodatkowe zabezpieczenie - usuwamy nasłuchiwania przy zniszczeniu aktywności
        removeAllListeners()
    }
}
