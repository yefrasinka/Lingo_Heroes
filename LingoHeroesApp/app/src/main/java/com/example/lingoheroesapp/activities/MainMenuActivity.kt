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
                    val userProgress = snapshot.getValue<Map<String, TopicProgress>>()
                    displayTopicsWithProgress(topics, userProgress ?: emptyMap())
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to load progress: ${error.message}")
                }
            })
    }

    private fun displayTopicsWithProgress(topics: List<Topic>, progress: Map<String, TopicProgress>) {
        topicsContainer.removeAllViews()
        topics.forEach { topic ->
            val topicProgress = progress[topic.id]
            val topicView = createTopicView(topic, topicProgress)
            topicsContainer.addView(topicView)
        }
    }

    private fun createTopicView(topic: Topic, progress: TopicProgress?): android.view.View {
        val view = LayoutInflater.from(this).inflate(R.layout.topic_item, null, false)
        val topicTitle = view.findViewById<TextView>(R.id.topicProgressText)
        val subtopicsContainer = view.findViewById<LinearLayout>(R.id.subtopicsContainer)

        topicTitle.text = topic.title
        val completedSubtopics = topic.subtopics.count { it.completedTasks == it.totalTasks }
        val backgroundColor = if (completedSubtopics == topic.subtopics.size) {
            ContextCompat.getColor(this, R.color.color_green)
        } else {
            ContextCompat.getColor(this, R.color.color_gray)
        }
        topicTitle.setBackgroundColor(backgroundColor)

        subtopicsContainer.removeAllViews()
        topic.subtopics.forEach { subtopic ->
            val subtopicView = createSubtopicView(subtopic)
            subtopicsContainer.addView(subtopicView)
        }

        return view
    }

    private fun createSubtopicView(subtopic: Subtopic): android.view.View {
        val view = LayoutInflater.from(this).inflate(R.layout.subtopic_item, null, false)
        val subtopicButton = view.findViewById<Button>(R.id.subtopicButton)
        val subtopicProgressBar = view.findViewById<ProgressBar>(R.id.subtopicProgressBar)

        subtopicButton.text = "${subtopic.title} (${subtopic.completedTasks}/${subtopic.totalTasks})"
        subtopicProgressBar.progress = if (subtopic.totalTasks > 0) {
            (subtopic.completedTasks * 100) / subtopic.totalTasks
        } else 0

        val backgroundColor = if (subtopic.completedTasks == subtopic.totalTasks) {
            ContextCompat.getColor(this, R.color.color_green)
        } else {
            ContextCompat.getColor(this, R.color.color_gray)
        }
        subtopicButton.setBackgroundColor(backgroundColor)


        subtopicButton.setOnClickListener {
            val intent = Intent(this, TaskDisplayActivity::class.java)
            intent.putExtra("SUBTOPIC_ID", subtopic.id) // Pass the subtopic ID
            startActivity(intent)
        }

        return view
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
