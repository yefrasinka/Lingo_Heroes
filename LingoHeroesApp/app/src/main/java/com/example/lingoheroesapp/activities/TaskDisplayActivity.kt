package com.example.lingoheroesapp.activities

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
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
    private var lastSelectedAnswer: String = ""
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var questionTextView: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var feedbackTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var playAudioButton: ImageButton
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
        playAudioButton = findViewById(R.id.playAudioButton)

        // Inicjalizacja przycisku powrotu
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            releaseMediaPlayer()
            showExitConfirmationDialog()
        }

        database = FirebaseDatabase.getInstance().reference
        subtopicId = intent.getStringExtra("SUBTOPIC_ID") ?: ""
        topicId = intent.getStringExtra("TOPIC_ID") ?: ""
        if (subtopicId != null) {
            loadTasksForSubtopic(topicId, subtopicId)
        } else {
            Toast.makeText(this, "No Subtopic ID found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTasksForSubtopic(topicId: String, subtopicId: String) {
        Log.d("TaskDebug", "Loading tasks for Topic ID: $topicId, Subtopic ID: $subtopicId")
        
        // Najpierw pobieramy zapisany postęp
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.child("users").child(userId)
            .child("topicsProgress").child(topicId)
            .child("subtopics").child(subtopicId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(progressSnapshot: DataSnapshot) {
                    val progress = progressSnapshot.getValue(SubtopicProgress::class.java)
                    currentTaskIndex = progress?.completedTasks ?: 0
                    Log.d("TaskDebug", "Loaded saved progress, starting from task: $currentTaskIndex")
                    
                    // Teraz ładujemy zadania
                    loadTasks()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TaskDebug", "Error loading progress: ${error.message}")
                    loadTasks() // Jeśli nie udało się wczytać postępu, i tak ładujemy zadania
                }
            })
    }

    private fun loadTasks() {
        database.child("topics").child(topicId).child("subtopics")
            .child("0").child("task")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val loadedTasks = mutableListOf<Task>()
                    
                    Log.d("TaskDebug", "Raw data from Firebase: ${snapshot.value}")
                    
                    if (snapshot.hasChild("0")) {
                        for (taskSnapshot in snapshot.children) {
                            try {
                                Log.d("TaskDebug", "Processing task: ${taskSnapshot.value}")
                                val task = Task(
                                    taskId = taskSnapshot.child("taskId").getValue(String::class.java) ?: "",
                                    type = taskSnapshot.child("type").getValue(String::class.java) ?: "",
                                    question = taskSnapshot.child("question").getValue(String::class.java) ?: "",
                                    options = taskSnapshot.child("options").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: listOf(),
                                    correctAnswer = taskSnapshot.child("correctAnswer").getValue(String::class.java) ?: "",
                                    description = taskSnapshot.child("description").getValue(String::class.java) ?: "",
                                    rewardXp = taskSnapshot.child("rewardXp").getValue(Int::class.java) ?: 0,
                                    rewardCoins = taskSnapshot.child("rewardCoins").getValue(Int::class.java) ?: 0,
                                    isCompleted = taskSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false,
                                    mediaUrl = taskSnapshot.child("mediaUrl").getValue(String::class.java) ?: ""
                                )
                                loadedTasks.add(task)
                                Log.d("TaskDebug", "Added task with mediaUrl: ${task.mediaUrl}")
                            } catch (e: Exception) {
                                Log.e("TaskDebug", "Error parsing task: ${e.message}")
                            }
                        }
                    } else {
                        try {
                            val task = Task(
                                taskId = snapshot.child("taskId").getValue(String::class.java) ?: "",
                                type = snapshot.child("type").getValue(String::class.java) ?: "",
                                question = snapshot.child("question").getValue(String::class.java) ?: "",
                                options = snapshot.child("options").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: listOf(),
                                correctAnswer = snapshot.child("correctAnswer").getValue(String::class.java) ?: "",
                                description = snapshot.child("description").getValue(String::class.java) ?: "",
                                rewardXp = snapshot.child("rewardXp").getValue(Int::class.java) ?: 0,
                                rewardCoins = snapshot.child("rewardCoins").getValue(Int::class.java) ?: 0,
                                isCompleted = snapshot.child("isCompleted").getValue(Boolean::class.java) ?: false,
                                mediaUrl = snapshot.child("mediaUrl").getValue(String::class.java) ?: ""
                            )
                            loadedTasks.add(task)
                            Log.d("TaskDebug", "Added single task with mediaUrl: ${task.mediaUrl}")
                        } catch (e: Exception) {
                            Log.e("TaskDebug", "Error parsing single task: ${e.message}")
                        }
                    }
                    
                    if (loadedTasks.isEmpty()) {
                        Log.e("TaskDebug", "No tasks found for Topic ID: $topicId, Subtopic ID: $subtopicId")
                        Toast.makeText(this@TaskDisplayActivity, "Brak zadań dla tego podtematu", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    tasks = loadedTasks
                    Log.d("TaskDebug", "Starting from task $currentTaskIndex of ${tasks.size}")
                    displayCurrentTask()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TaskDebug", "Error loading tasks: ${error.message}")
                    Toast.makeText(this@TaskDisplayActivity, "Błąd podczas ładowania zadań: ${error.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun displayCurrentTask() {
        if (currentTaskIndex >= tasks.size) {
            releaseMediaPlayer()
            Toast.makeText(this, "Gratulacje! Ukończyłeś wszystkie zadania!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val currentTask = tasks[currentTaskIndex]
        Log.d("TaskDebug", "Displaying task with mediaUrl: ${currentTask.mediaUrl}")

        // Aktualizacja progress
        progressTextView.text = "Zadanie ${currentTaskIndex + 1}/${tasks.size}"

        // Wyświetl pytanie
        questionTextView.text = currentTask.question

        // Obsługa przycisku audio dla zadań typu Listening
        if (currentTask.mediaUrl.isNotEmpty()) {
            Log.d("TaskDebug", "Task has audio URL: ${currentTask.mediaUrl}")
            playAudioButton.visibility = View.VISIBLE
            playAudioButton.setOnClickListener {
                playAudio(currentTask.mediaUrl)
            }
            // Automatyczne odtwarzanie audio przy wyświetleniu zadania
            playAudio(currentTask.mediaUrl)
        } else {
            Log.d("TaskDebug", "Task has no audio URL")
            playAudioButton.visibility = View.GONE
        }

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
        lastSelectedAnswer = selectedAnswer
        val isCorrect = selectedAnswer == correctAnswer

        // Aktualizujemy postęp niezależnie od poprawności odpowiedzi
        updateProgress(subtopicId, topicId, tasks[currentTaskIndex])

        // Pokaż feedback i przejdź do następnego zadania
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

        // Czekaj 3 sekundy i przejdź do następnego zadania
        handler.postDelayed({
            currentTaskIndex++
            saveCurrentProgress {
                displayCurrentTask()
            }
        }, 3000) // Zmniejszamy czas oczekiwania do 3 sekund
    }

    private fun updateProgress(subtopicId: String, topicId: String, currentTask: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userProgressRef = database.child("users").child(userId).child("topicsProgress").child(topicId)

        database.child("topics").child(topicId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(topicSnapshot: DataSnapshot) {
                val topic = topicSnapshot.getValue(Topic::class.java) ?: return
                val subtopic = topic.subtopics.find { it.id == subtopicId } ?: return
                
                userProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(progressSnapshot: DataSnapshot) {
                        val topicProgress = progressSnapshot.getValue(TopicProgress::class.java) 
                            ?: TopicProgress()
                        
                        val currentSubtopicProgress = topicProgress.subtopics[subtopicId] 
                            ?: SubtopicProgress(title = subtopic.title)
                        
                        // Zawsze zwiększamy licznik ukończonych zadań
                        val newCompletedTasks = if (currentSubtopicProgress.completedTasks < subtopic.totalTasks) {
                            currentSubtopicProgress.completedTasks + 1
                        } else {
                            subtopic.totalTasks
                        }

                        val updatedSubtopicProgress = currentSubtopicProgress.copy(
                            completedTasks = newCompletedTasks,
                            totalTasks = subtopic.totalTasks
                        )

                        val updatedSubtopics = topicProgress.subtopics.toMutableMap()
                        updatedSubtopics[subtopicId] = updatedSubtopicProgress

                        val totalCompletedTasks = updatedSubtopics.values.sumOf { 
                            minOf(it.completedTasks, it.totalTasks)
                        }
                        val totalTasks = topic.subtopics.sumOf { it.totalTasks }
                        val completedSubtopics = updatedSubtopics.count { (_, progress) -> 
                            progress.completedTasks >= progress.totalTasks 
                        }

                        val updatedTopicProgress = topicProgress.copy(
                            completedTasks = totalCompletedTasks,
                            totalTasks = totalTasks,
                            completedSubtopics = completedSubtopics,
                            totalSubtopics = topic.subtopics.size,
                            subtopics = updatedSubtopics
                        )

                        userProgressRef.setValue(updatedTopicProgress)
                            .addOnSuccessListener {
                                // Aktualizujemy XP i monety tylko przy poprawnej odpowiedzi
                                if (lastSelectedAnswer == currentTask.correctAnswer) {
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

        userProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val topicProgress = snapshot.getValue(TopicProgress::class.java) ?: TopicProgress()
                
                val currentSubtopicProgress = topicProgress.subtopics[subtopicId] 
                    ?: SubtopicProgress(title = "")

                // Zapisujemy aktualny postęp
                val updatedSubtopicProgress = currentSubtopicProgress.copy(
                    completedTasks = currentTaskIndex,
                    totalTasks = tasks.size
                )

                Log.d("TaskDebug", "Saving progress: Task $currentTaskIndex of ${tasks.size}")

                val updatedSubtopics = topicProgress.subtopics.toMutableMap()
                updatedSubtopics[subtopicId] = updatedSubtopicProgress

                val updatedTopicProgress = topicProgress.copy(
                    completedTasks = updatedSubtopics.values.sumOf { it.completedTasks },
                    totalTasks = updatedSubtopics.values.sumOf { it.totalTasks },
                    completedSubtopics = updatedSubtopics.count { (_, progress) -> 
                        progress.completedTasks >= progress.totalTasks 
                    },
                    subtopics = updatedSubtopics
                )

                userProgressRef.setValue(updatedTopicProgress)
                    .addOnSuccessListener { 
                        Log.d("TaskDebug", "Progress saved successfully")
                        onComplete() 
                    }
                    .addOnFailureListener { e ->
                        Log.e("TaskDebug", "Failed to save progress", e)
                        onComplete()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskDebug", "Failed to load progress for saving", error.toException())
                onComplete()
            }
        })
    }

    private fun playAudio(url: String) {
        Log.d("TaskDebug", "Attempting to play audio from URL: $url")
        releaseMediaPlayer()
        
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    Log.d("TaskDebug", "MediaPlayer prepared successfully")
                    mp.start()
                    playAudioButton.isEnabled = false
                    playAudioButton.alpha = 0.5f
                }
                setOnCompletionListener { mp ->
                    Log.d("TaskDebug", "Audio playback completed")
                    playAudioButton.isEnabled = true
                    playAudioButton.alpha = 1.0f
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("TaskDebug", "MediaPlayer error: what=$what, extra=$extra")
                    Toast.makeText(this@TaskDisplayActivity, "Błąd odtwarzania audio", Toast.LENGTH_SHORT).show()
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e("TaskDebug", "Error setting up MediaPlayer: ${e.message}")
                Toast.makeText(this@TaskDisplayActivity, "Błąd odtwarzania audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        playAudioButton.isEnabled = true
        playAudioButton.alpha = 1.0f
    }

    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        handler.removeCallbacksAndMessages(null)
    }
}