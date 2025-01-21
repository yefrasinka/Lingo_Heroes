package com.example.lingoheroesapp.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ServerValue
import com.example.lingoheroesapp.models.Progress
// TaskDisplayActivity.kt
class TaskDisplayActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var tasks = mutableListOf<Task>()
    private var currentTaskIndex = 0
    private var handler = Handler(Looper.getMainLooper())

    private lateinit var questionTextView: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var feedbackTextView: TextView
    private lateinit var progressTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_display)

        // Inicjalizacja widoków
        questionTextView = findViewById(R.id.questionTextView)
        optionsContainer = findViewById(R.id.optionsContainer)
        feedbackTextView = findViewById(R.id.feedbackTextView)
        progressTextView = findViewById(R.id.progressTextView)

        database = FirebaseDatabase.getInstance().reference
        val subtopicId = intent.getStringExtra("SUBTOPIC_ID")

        if (subtopicId != null) {
            loadTasksForSubtopic(subtopicId)
        } else {
            Toast.makeText(this, "No Subtopic ID found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTasksForSubtopic(subtopicId: String) {
        database.child("topics").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tasks.clear()

                for (topicSnapshot in snapshot.children) {
                    val subtopicsSnapshot = topicSnapshot.child("subtopics")
                    for (subtopicSnapshot in subtopicsSnapshot.children) {
                        val currentSubtopicId = subtopicSnapshot.child("id").getValue(String::class.java)
                        if (currentSubtopicId == subtopicId) {
                            val tasksSnapshot = subtopicSnapshot.child("task")
                            tasksSnapshot.children.forEach { taskSnapshot ->
                                try {
                                    val task = Task(
                                        taskId = taskSnapshot.child("taskId").getValue(String::class.java) ?: "",
                                        description = taskSnapshot.child("description").getValue(String::class.java) ?: "",
                                        type = taskSnapshot.child("type").getValue(String::class.java) ?: "",
                                        question = taskSnapshot.child("question").getValue(String::class.java) ?: "",
                                        options = taskSnapshot.child("options").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList(),
                                        correctAnswer = taskSnapshot.child("correctAnswer").getValue(String::class.java) ?: "",
                                        rewardXp = taskSnapshot.child("rewardXp").getValue(Int::class.java) ?: 0,
                                        rewardCoins = taskSnapshot.child("rewardCoins").getValue(Int::class.java) ?: 0,
                                        isCompleted = taskSnapshot.child("iscompleted").getValue(Boolean::class.java) ?: false
                                    )
                                    tasks.add(task)
                                } catch (e: Exception) {
                                    println("Error parsing task: ${e.message}")
                                }
                            }
                            break
                        }
                    }
                }

                if (tasks.isNotEmpty()) {
                    displayCurrentTask()
                } else {
                    Toast.makeText(this@TaskDisplayActivity, "No tasks found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TaskDisplayActivity, "Failed to load tasks", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun displayCurrentTask() {
        if (currentTaskIndex >= tasks.size) {
            // Wszystkie zadania zostały wykonane
            Toast.makeText(this, "All tasks completed!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val currentTask = tasks[currentTaskIndex]

        // Aktualizacja progress
        progressTextView.text = "Question ${currentTaskIndex + 1}/${tasks.size}"

        // Wyświetl pytanie
        questionTextView.text = currentTask.question

        // Wyczyść poprzednie opcje
        optionsContainer.removeAllViews()

        // Dodaj opcje odpowiedzi
        currentTask.options.forEach { option ->
            val button = Button(this).apply {
                text = option
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setOnClickListener {
                    handleAnswer(option, currentTask.correctAnswer)
                }
            }
            optionsContainer.addView(button)
        }

        // Ukryj feedback
        feedbackTextView.visibility = View.INVISIBLE
    }

    private fun handleAnswer(selectedAnswer: String, correctAnswer: String) {


        // Wyłącz przyciski
        for (i in 0 until optionsContainer.childCount) {
            optionsContainer.getChildAt(i).isEnabled = false
        }

        // Pokaż feedback
        feedbackTextView.apply {
            visibility = View.VISIBLE
            if (selectedAnswer == correctAnswer) {
                text = "Correct! The answer is: $correctAnswer"
                setTextColor(Color.GREEN)
            } else {
                text = "Incorrect. The correct answer is: $correctAnswer"
                setTextColor(Color.RED)
            }
        }

        // Czekaj 5 sekund i przejdź do następnego zadania
        handler.postDelayed({
            currentTaskIndex++
            displayCurrentTask()
        }, 5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}