package com.example.lingoheroesapp.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.TestActivity
import com.example.lingoheroesapp.models.Progress
import com.example.lingoheroesapp.models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainMenuActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI elements
    private lateinit var usernameTextView: TextView
    private lateinit var levelTextView: TextView
    private lateinit var xpTextView: TextView
    private lateinit var coinsTextView: TextView
    private lateinit var topicProgressText: TextView
    private lateinit var subtopicProgressBar: ProgressBar // ProgressBar dla podtematu
    private lateinit var taskProgressBar: ProgressBar // ProgressBar dla zadania
    private lateinit var subtopicProgressBars: List<ProgressBar>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize UI elements
        usernameTextView = findViewById(R.id.usernameText)
        xpTextView = findViewById(R.id.experienceText)
        coinsTextView = findViewById(R.id.currencyText)
        topicProgressText = findViewById(R.id.topicProgressText)
        subtopicProgressBar = findViewById(R.id.subtopicProgressBar)

        // Bottom navigation setup
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

        // Button for starting the test
        val testButton = findViewById<Button>(R.id.testButton)
        val avatarButton = findViewById<ImageView>(R.id.avatarImage)

        testButton.setOnClickListener {
            val intent = Intent(this, TestActivity::class.java)
            startActivity(intent)
        }

        avatarButton.setOnClickListener {
            // Navigate to account screen
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
        }

        // Check if the user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d("MainMenuActivity", "User is logged in: ${currentUser.uid}")
            fetchUserData(currentUser.uid)
            fetchUserProgress(currentUser.uid)
        } else {
            Log.d("MainMenuActivity", "User not logged in")
            // Handle case where user is not logged in (redirect to login screen)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close this activity
        }
    }

    private fun fetchUserData(uid: String) {
        val userRef = database.child("users").child(uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        usernameTextView.text = it.username
                        xpTextView.text = it.xp.toString()
                        coinsTextView.text = it.coins.toString()
                        Log.d("MainMenuActivity", "User data fetched successfully")
                    }
                } else {
                    usernameTextView.text = "User not found"
                    Log.e("MainMenuActivity", "User not found in database")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainMenuActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainMenuActivity", "Error fetching user data: ${error.message}")
            }
        })
    }

    private fun fetchUserProgress(uid: String) {
        val progressRef = database
            .child("progress")
            .orderByChild("uid")
            .equalTo(uid)

        progressRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val progressList = mutableListOf<Progress>()
                snapshot.children.forEach { data ->
                    val progress = data.getValue(Progress::class.java)
                    if (progress != null) {
                        progressList.add(progress)
                    }
                }
                updateProgressUI(progressList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainMenuActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainMenuActivity", "Error fetching user progress: ${error.message}")
            }
        })
    }

    private fun updateProgressUI(progressList: List<Progress>) {
        val topicProgress = calculateTopicProgress(progressList)
        val subtopicProgresses = calculateSubtopicProgress(progressList)

        // Update progress for subtopics
        subtopicProgresses.forEachIndexed { index, progress ->
            updateSubtopicProgressBar(subtopicProgressBars[index], progress)
        }

        // Update main topic progress based on subtopics
        val allSubtopicsComplete = subtopicProgresses.all { it == 100 }
        updateTopicProgressColor(allSubtopicsComplete)
    }

    private fun calculateTopicProgress(progressList: List<Progress>): Int {
        // Check if all subtopics are completed (100% progress)
        val allSubtopicsComplete = progressList.all { it.score == 100 }
        return if (allSubtopicsComplete) 100 else 0
    }

    private fun calculateSubtopicProgress(progressList: List<Progress>): List<Int> {
        // Calculate the progress for each subtopic
        return progressList.map { it.score }
    }

    private fun updateSubtopicProgressBar(progressBar: ProgressBar, progress: Int) {
        progressBar.progress = progress

        // Change color based on progress
        when {
            progress < 30 -> progressBar.progressTintList = ColorStateList.valueOf(Color.RED)
            progress in 30..60 -> progressBar.progressTintList = ColorStateList.valueOf(Color.YELLOW)
            else -> progressBar.progressTintList = ColorStateList.valueOf(Color.GREEN)
        }
    }

    fun updateTopicProgressColor(isCompleted: Boolean) {
        if (isCompleted) {
            topicProgressText.setTextColor(ContextCompat.getColor(this, R.color.color_green))
        } else {
            topicProgressText.setTextColor(ContextCompat.getColor(this, R.color.color_gray))
        }
    }
}
