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
import com.example.lingoheroesapp.models.Challenge
import com.example.lingoheroesapp.models.User
import com.example.lingoheroesapp.models.ChallengeType
import com.google.firebase.database.Transaction
import com.google.firebase.database.MutableData


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
        val userRef = database.child("users").child(userId)
        val userProgressRef = userRef.child("topicsProgress").child(topicId)

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
                                // Aktualizujemy statystyki użytkownika
                                userRef.get().addOnSuccessListener { userSnapshot ->
                                    val currentXp = userSnapshot.child("xp").getValue(Long::class.java)?.toInt() ?: 0
                                    val currentCoins = userSnapshot.child("coins").getValue(Long::class.java)?.toInt() ?: 0
                                    val tasksCompleted = userSnapshot.child("tasksCompleted").getValue(Long::class.java)?.toInt() ?: 0
                                    val perfectScores = userSnapshot.child("perfectScores").getValue(Long::class.java)?.toInt() ?: 0
                                    
                                    val updates = hashMapOf<String, Any>(
                                        // Zawsze zwiększamy licznik ukończonych zadań
                                        "tasksCompleted" to (tasksCompleted + 1)
                                    )

                                    // Dodajemy XP i monety tylko za poprawną odpowiedź
                                    if (lastSelectedAnswer == currentTask.correctAnswer) {
                                        updates["xp"] = currentXp + currentTask.rewardXp
                                        updates["coins"] = currentCoins + currentTask.rewardCoins
                                        
                                        // Sprawdzamy czy to idealny wynik (wszystkie odpowiedzi poprawne w temacie)
                                        if (currentTaskIndex == tasks.size - 1 && !tasks.any { it.correctAnswer != lastSelectedAnswer }) {
                                            updates["perfectScores"] = perfectScores + 1
                                        }
                                    }
                                    
                                    userRef.updateChildren(updates).addOnSuccessListener {
                                        // Aktualizacja wyzwań
                                        updateChallenges(userId)
                                    }
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

    private fun updateChallenges(userId: String) {
        val challengesRef = database.child("users").child(userId).child("challenges")
        val userRef = database.child("users").child(userId)

        userRef.get().addOnSuccessListener { userSnapshot ->
            val user = userSnapshot.getValue(User::class.java) ?: return@addOnSuccessListener
            val currentDate = System.currentTimeMillis()
            
            challengesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { challengeSnapshot ->
                        val challenge = challengeSnapshot.getValue(Challenge::class.java)
                        challenge?.let {
                            when (it.type) {
                                ChallengeType.DAILY -> {
                                    // Sprawdzamy czy to nowy dzień
                                    val lastUpdateTime = it.lastUpdateTime ?: 0
                                    val isNewDay = (currentDate - lastUpdateTime) > 24 * 60 * 60 * 1000 // 24 godziny
                                    
                                    if (isNewDay) {
                                        // Resetujemy postęp na początku nowego dnia
                                        challengeSnapshot.ref.updateChildren(mapOf(
                                            "currentProgress" to 0,
                                            "isCompleted" to false,
                                            "lastUpdateTime" to currentDate
                                        ))
                                    }
                                    
                                    // Aktualizujemy postęp tylko jeśli wyzwanie nie jest jeszcze ukończone
                                    if (!it.isCompleted) {
                                        when (it.title) {
                                            "Codzienna praktyka" -> {
                                                if (lastSelectedAnswer == tasks[currentTaskIndex].correctAnswer) {
                                                    val newProgress = it.currentProgress + 1
                                                    updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                }
                                            }
                                            "Dzienny zdobywca XP" -> {
                                                if (lastSelectedAnswer == tasks[currentTaskIndex].correctAnswer) {
                                                    val newProgress = it.currentProgress + tasks[currentTaskIndex].rewardXp
                                                    updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                }
                                            }
                                        }
                                    }
                                }
                                ChallengeType.WEEKLY -> {
                                    // Sprawdzamy czy to nowy tydzień
                                    val lastUpdateTime = it.lastUpdateTime ?: 0
                                    val isNewWeek = (currentDate - lastUpdateTime) > 7 * 24 * 60 * 60 * 1000 // 7 dni
                                    
                                    if (isNewWeek) {
                                        // Resetujemy postęp na początku nowego tygodnia
                                        challengeSnapshot.ref.updateChildren(mapOf(
                                            "currentProgress" to 0,
                                            "isCompleted" to false,
                                            "lastUpdateTime" to currentDate
                                        ))
                                        
                                        // Resetujemy także liczniki dla wyzwań tygodniowych
                                        userRef.updateChildren(mapOf(
                                            "lastDayTasksCount" to 0,
                                            "lastDayTimestamp" to 0,
                                            "todaysPerfectTasks" to 0,
                                            "todaysTotalTasks" to 0,
                                            "lastPerfectDay" to 0
                                        ))
                                    }
                                    
                                    // Aktualizujemy postęp tylko jeśli wyzwanie nie jest jeszcze ukończone
                                    if (!it.isCompleted) {
                                        when (it.title) {
                                            "Tygodniowa seria" -> {
                                                val lastDayTimestamp = userSnapshot.child("lastDayTimestamp").getValue(Long::class.java) ?: 0
                                                val isDayChange = (currentDate - lastDayTimestamp) > 24 * 60 * 60 * 1000
                                                
                                                if (isDayChange) {
                                                    val tasksToday = user.tasksCompleted - (userSnapshot.child("lastDayTasksCount").getValue(Int::class.java) ?: 0)
                                                    if (tasksToday >= 10) {
                                                        val newProgress = it.currentProgress + 1
                                                        updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                        
                                                        userRef.updateChildren(mapOf(
                                                            "lastDayTasksCount" to user.tasksCompleted,
                                                            "lastDayTimestamp" to currentDate
                                                        ))
                                                    }
                                                }
                                            }
                                            "Perfekcyjny tydzień" -> {
                                                val lastPerfectDay = userSnapshot.child("lastPerfectDay").getValue(Long::class.java) ?: 0
                                                val isDayChange = (currentDate - lastPerfectDay) > 24 * 60 * 60 * 1000
                                                
                                                if (isDayChange) {
                                                    val todaysPerfectTasks = userSnapshot.child("todaysPerfectTasks").getValue(Int::class.java) ?: 0
                                                    val todaysTotalTasks = userSnapshot.child("todaysTotalTasks").getValue(Int::class.java) ?: 0
                                                    
                                                    val updates = hashMapOf<String, Any>(
                                                        "todaysPerfectTasks" to (if (lastSelectedAnswer == tasks[currentTaskIndex].correctAnswer) todaysPerfectTasks + 1 else todaysPerfectTasks),
                                                        "todaysTotalTasks" to (todaysTotalTasks + 1)
                                                    )
                                                    
                                                    if (todaysPerfectTasks + 1 == todaysTotalTasks + 1 && lastSelectedAnswer == tasks[currentTaskIndex].correctAnswer) {
                                                        val newProgress = it.currentProgress + 1
                                                        updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                        
                                                        updates["lastPerfectDay"] = currentDate
                                                        updates["todaysPerfectTasks"] = 0
                                                        updates["todaysTotalTasks"] = 0
                                                    }
                                                    
                                                    userRef.updateChildren(updates)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TaskDisplayActivity", "Failed to update challenges", error.toException())
                }
            })
        }
    }

    private fun updateChallengeProgress(challengeRef: DatabaseReference, newProgress: Int, requiredValue: Int) {
        if (newProgress >= requiredValue) {
            val userRef = challengeRef.parent?.parent // Przejście do referencji użytkownika
            userRef?.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentCompletedChallenges = currentData.child("completedChallenges").getValue(Int::class.java) ?: 0
                    val challengeKey = challengeRef.key
                    
                    currentData.child("challenges").child(challengeKey!!).child("currentProgress").value = requiredValue
                    currentData.child("challenges").child(challengeKey).child("isCompleted").value = true
                    currentData.child("completedChallenges").value = currentCompletedChallenges + 1
                    
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        Log.e("TaskDisplayActivity", "Failed to update challenge progress: ${error.message}")
                    }
                }
            })
        } else {
            challengeRef.child("currentProgress").setValue(newProgress)
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