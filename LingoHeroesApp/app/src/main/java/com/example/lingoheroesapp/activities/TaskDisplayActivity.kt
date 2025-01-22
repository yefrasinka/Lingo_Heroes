package com.example.lingoheroesapp.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.lingoheroesapp.models.TopicProgress
import com.example.lingoheroesapp.models.Topic
import com.example.lingoheroesapp.models.SubtopicProgress


///

//odpowiada za taski         (TaskListActivity  chyba nie wykorzystuje sie w kodzie)

///



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
    private lateinit var subtopicId: String
    private lateinit var topicId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_display)

        // Inicjalizacja widoków
        questionTextView = findViewById(R.id.questionTextView)
        optionsContainer = findViewById(R.id.optionsContainer)
        feedbackTextView = findViewById(R.id.feedbackTextView)
        progressTextView = findViewById(R.id.progressTextView)

        // Inicjalizacja przycisku powrotu
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            showExitConfirmationDialog()
        }

        database = FirebaseDatabase.getInstance().reference
        subtopicId = intent.getStringExtra("SUBTOPIC_ID") ?: ""
        topicId = intent.getStringExtra("TOPIC_ID") ?: ""
        if (subtopicId != null) {
            loadTasksForSubtopic(subtopicId)
        } else {
            Toast.makeText(this, "No Subtopic ID found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTasksForSubtopic(subtopicId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Najpierw sprawdzamy postęp użytkownika
        database.child("users").child(userId).child("topicsProgress").child(topicId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(progressSnapshot: DataSnapshot) {
                    val topicProgress = progressSnapshot.getValue(TopicProgress::class.java)
                    val subtopicProgress = topicProgress?.subtopics?.get(subtopicId)

                    if (subtopicProgress != null && subtopicProgress.completedTasks >= subtopicProgress.totalTasks) {
                        // Test już ukończony
                        Toast.makeText(this@TaskDisplayActivity, "Ten test został już ukończony!", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    // Ustawiamy currentTaskIndex na podstawie zapisanego postępu
                    currentTaskIndex = subtopicProgress?.completedTasks ?: 0
                    
                    // Jeśli test nie jest ukończony, ładujemy zadania
                    loadTasks()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TaskDisplayActivity", "Error checking progress", error.toException())
                    finish()
                }
            })
    }

    private fun loadTasks() {
        database.child("topics").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tasks.clear()
                var foundSubtopic = false

                for (topicSnapshot in snapshot.children) {
                    val subtopicsSnapshot = topicSnapshot.child("subtopics")
                    for (subtopicSnapshot in subtopicsSnapshot.children) {
                        val currentSubtopicId = subtopicSnapshot.child("id").getValue(String::class.java)
                        if (currentSubtopicId == subtopicId) {
                            foundSubtopic = true
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
                                        isCompleted = taskSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false
                                    )
                                    tasks.add(task)
                                } catch (e: Exception) {
                                    Log.e("TaskDisplayActivity", "Error parsing task: ${e.message}")
                                }
                            }
                            break
                        }
                    }
                    if (foundSubtopic) break
                }

                if (tasks.isNotEmpty()) {
                    displayCurrentTask()
                } else {
                    Toast.makeText(this@TaskDisplayActivity, "Nie znaleziono zadań", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TaskDisplayActivity, "Błąd podczas ładowania zadań", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun displayCurrentTask() {
        if (currentTaskIndex >= tasks.size) {
            // Wszystkie zadania zostały wykonane
            Toast.makeText(this, "Gratulacje! Ukończyłeś wszystkie zadania!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val currentTask = tasks[currentTaskIndex]

        // Aktualizacja progress
        progressTextView.text = "Zadanie ${currentTaskIndex + 1}/${tasks.size}"

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
        if (!::subtopicId.isInitialized || !::topicId.isInitialized) {
            Log.e("TaskDisplayActivity", "subtopicId lub topicId nie zostały zainicjalizowane")
            return
        }
        val isCorrect = selectedAnswer == correctAnswer

        // Aktualizujemy postęp tylko jeśli odpowiedź jest poprawna
        if (isCorrect) {
            updateProgress(subtopicId, topicId, tasks[currentTaskIndex])
        }

        // Pokaż feedback i przejdź do następnego zadania po 5 sekundach
        showFeedbackAndContinue(isCorrect, correctAnswer)
    }

    private fun showFeedbackAndContinue(isCorrect: Boolean, correctAnswer: String) {
        // Wyłącz przyciski
        for (i in 0 until optionsContainer.childCount) {
            optionsContainer.getChildAt(i).isEnabled = false
        }

        // Pokaż feedback
        feedbackTextView.apply {
            visibility = View.VISIBLE
            if (isCorrect) {
                text = "Poprawna odpowiedź!"
                setTextColor(Color.GREEN)
            } else {
                text = "Niepoprawna odpowiedź. Prawidłowa odpowiedź to: $correctAnswer"
                setTextColor(Color.RED)
            }
        }

        // Czekaj 5 sekund i przejdź do następnego zadania
        handler.postDelayed({
            currentTaskIndex++ // Zawsze przechodzimy do następnego zadania
            displayCurrentTask()
        }, 5000)
    }

    private fun updateProgress(subtopicId: String, topicId: String, currentTask: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userProgressRef = database.child("users").child(userId).child("topicsProgress").child(topicId)

        // Najpierw pobieramy aktualny stan tematu i podtematu
        database.child("topics").child(topicId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(topicSnapshot: DataSnapshot) {
                val topic = topicSnapshot.getValue(Topic::class.java) ?: return
                
                // Znajdujemy odpowiedni podtemat
                val subtopic = topic.subtopics.find { it.id == subtopicId } ?: return
                
                // Aktualizujemy postęp w Firebase
                userProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(progressSnapshot: DataSnapshot) {
                        // Pobieramy lub tworzymy nowy postęp tematu
                        val topicProgress = progressSnapshot.getValue(TopicProgress::class.java) 
                            ?: TopicProgress()
                        
                        // Pobieramy aktualny postęp podtematu
                        val currentSubtopicProgress = topicProgress.subtopics[subtopicId] 
                            ?: SubtopicProgress(title = subtopic.title)
                        
                        // Sprawdzamy, czy nie przekroczyliśmy maksymalnej liczby zadań
                        val newCompletedTasks = if (currentSubtopicProgress.completedTasks < subtopic.totalTasks) {
                            currentSubtopicProgress.completedTasks + 1
                        } else {
                            subtopic.totalTasks
                        }

                        // Aktualizujemy postęp podtematu
                        val updatedSubtopicProgress = currentSubtopicProgress.copy(
                            completedTasks = newCompletedTasks,
                            totalTasks = subtopic.totalTasks
                        )

                        // Tworzymy nową mapę podtematów z zaktualizowanym postępem
                        val updatedSubtopics = topicProgress.subtopics.toMutableMap()
                        updatedSubtopics[subtopicId] = updatedSubtopicProgress

                        // Obliczamy całkowity postęp tematu
                        val totalCompletedTasks = updatedSubtopics.values.sumOf { 
                            minOf(it.completedTasks, it.totalTasks) // Upewniamy się, że nie przekroczymy maksimum
                        }
                        val totalTasks = topic.subtopics.sumOf { it.totalTasks }
                        val completedSubtopics = updatedSubtopics.count { (_, progress) -> 
                            progress.completedTasks >= progress.totalTasks 
                        }

                        // Tworzymy zaktualizowany postęp tematu
                        val updatedTopicProgress = topicProgress.copy(
                            completedTasks = totalCompletedTasks,
                            totalTasks = totalTasks,
                            completedSubtopics = completedSubtopics,
                            totalSubtopics = topic.subtopics.size,
                            subtopics = updatedSubtopics
                        )

                        // Aktualizujemy wszystko w Firebase
                        userProgressRef.setValue(updatedTopicProgress)
                            .addOnSuccessListener {
                                // Aktualizujemy XP i monety użytkownika tylko jeśli faktycznie ukończyliśmy nowe zadanie
                                if (newCompletedTasks > currentSubtopicProgress.completedTasks) {
                                    val userRef = database.child("users").child(userId)
                                    updateUserRewards(userRef, currentTask.rewardXp, currentTask.rewardCoins)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("TaskDisplayActivity", "Failed to update progress", e)
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TaskDisplayActivity", "Failed to update progress", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskDisplayActivity", "Failed to load topic data", error.toException())
            }
        })
    }

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

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Powrót do menu")
            .setMessage("Czy na pewno chcesz wrócić do menu głównego? Twój postęp zostanie zachowany.")
            .setPositiveButton("Tak") { _, _ ->
                // Zapisujemy aktualny postęp przed wyjściem
                saveCurrentProgress {
                    // Po zapisaniu postępu wracamy do menu
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    finish()
                }
            }
            .setNegativeButton("Nie", null)
            .show()
    }

    private fun saveCurrentProgress(onComplete: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userProgressRef = database.child("users").child(userId).child("topicsProgress").child(topicId)

        // Pobieramy aktualny postęp
        userProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val topicProgress = snapshot.getValue(TopicProgress::class.java) ?: TopicProgress()
                
                // Pobieramy lub tworzymy postęp podtematu
                val currentSubtopicProgress = topicProgress.subtopics[subtopicId] 
                    ?: SubtopicProgress(title = "")

                // Aktualizujemy postęp podtematu
                val updatedSubtopicProgress = currentSubtopicProgress.copy(
                    completedTasks = currentTaskIndex,
                    totalTasks = tasks.size
                )

                // Tworzymy nową mapę podtematów
                val updatedSubtopics = topicProgress.subtopics.toMutableMap()
                updatedSubtopics[subtopicId] = updatedSubtopicProgress

                // Aktualizujemy postęp tematu
                val updatedTopicProgress = topicProgress.copy(
                    completedTasks = updatedSubtopics.values.sumOf { it.completedTasks },
                    totalTasks = updatedSubtopics.values.sumOf { it.totalTasks },
                    completedSubtopics = updatedSubtopics.count { (_, progress) -> 
                        progress.completedTasks >= progress.totalTasks 
                    },
                    subtopics = updatedSubtopics
                )

                // Zapisujemy zaktualizowany postęp
                userProgressRef.setValue(updatedTopicProgress)
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { e ->
                        Log.e("TaskDisplayActivity", "Failed to save progress", e)
                        onComplete()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskDisplayActivity", "Failed to load progress for saving", error.toException())
                onComplete()
            }
        })
    }

    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}