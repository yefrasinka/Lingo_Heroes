package com.example.lingoheroesapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.TestActivity
import com.example.lingoheroesapp.models.Progress
import com.example.lingoheroesapp.models.Subtopic
import com.example.lingoheroesapp.models.Topic
import com.example.lingoheroesapp.models.TopicProgress
import com.example.lingoheroesapp.models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.core.view.View

class MainMenuActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI elements
    private lateinit var usernameTextView: TextView
    private lateinit var levelTextView: TextView
    private lateinit var xpTextView: TextView
    private lateinit var coinsTextView: TextView
    private lateinit var subtopicsContainer: LinearLayout
    private lateinit var topicsContainer: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebase()
        initializeUI()
        setupBottomNavigation()
        setupButtons()
        checkUserAndLoadData()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }

    private fun initializeUI() {
        usernameTextView = findViewById(R.id.usernameText)


        //level dla poziomi jezyka
        levelTextView = findViewById(R.id.levelText)

        xpTextView = findViewById(R.id.experienceText)
        coinsTextView = findViewById(R.id.currencyText)
        subtopicsContainer = findViewById(R.id.subtopicsContainer)
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

    private fun setupButtons() {
        findViewById<Button>(R.id.testButton).setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
        }

        findViewById<ImageView>(R.id.avatarImage).setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
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
        levelTextView.text = "Level ${user.level}"  // Setting the level text dynamically
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
                loadSubtopicsForTopics(topics, userId)
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Failed to load topics: ${error.message}")
            }
        })
    }

    private fun loadSubtopicsForTopics(topics: List<Topic>, userId: String) {
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
        topicsContainer.removeAllViews() // Clear previous views

        topics.forEach { topic ->
            val topicProgress = progress[topic.id]
            val topicView = updateTopicView(topic, topicProgress)
            topicsContainer.addView(topicView)
        }
    }

    private fun updateTopicView(topic: Topic, progress: TopicProgress?): android.view.View {
        // Inflate the layout for the topic
        val view = LayoutInflater.from(this).inflate(R.layout.activity_main, null, false)

        val topicProgressText = view.findViewById<TextView>(R.id.topicProgressText)
        val subtopicsContainer = view.findViewById<LinearLayout>(R.id.subtopicsContainer)

        // Set the topic description
        topicProgressText.text = topic.description  // Display the topic description

        // Set background color based on progress
        val progressPercentage = progress?.progressPercentage ?: 0
        val backgroundColor = if (progressPercentage == 100) {
            ContextCompat.getColor(this, R.color.color_green)
        } else {
            ContextCompat.getColor(this, R.color.color_gray)
        }
        topicProgressText.setBackgroundColor(backgroundColor)

        // Clear any existing subtopics
        subtopicsContainer.removeAllViews()

        // Add subtopics dynamically
        topic.subtopics.forEach { subtopic ->
            val subtopicView = createSubtopicView(subtopic)  // Create subtopic views
            subtopicsContainer.addView(subtopicView)
        }

        return view  // Return the fully populated view for the topic
    }

    private fun createSubtopicView(subtopic: Subtopic): android.view.View {
        // Inflate the layout for the subtopic
        val view = LayoutInflater.from(this).inflate(R.layout.activity_main, null, false)

        val subtopicButton = view.findViewById<Button>(R.id.subtopicButton)
        val subtopicProgressBar = view.findViewById<ProgressBar>(R.id.subtopicProgressBar)

        // Set button text and progress bar based on data
        subtopicButton.text = "${subtopic.title} (${subtopic.completedTasks}/${subtopic.totalTasks})"
        subtopicProgressBar.progress = subtopic.progressPercentage

        // Set background color based on progress
        val progress = subtopic.progressPercentage
        val backgroundColor = if (progress == 100) {
            ContextCompat.getColor(this, R.color.color_green)
        } else {
            ContextCompat.getColor(this, R.color.color_gray)
        }
        subtopicButton.setBackgroundColor(backgroundColor)

        return view  // Return the fully populated view for the subtopic
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


