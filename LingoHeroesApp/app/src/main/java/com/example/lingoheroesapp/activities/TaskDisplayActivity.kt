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
import androidx.activity.OnBackPressedCallback
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
import com.example.lingoheroesapp.models.Reward
import com.google.firebase.database.Transaction
import com.google.firebase.database.MutableData
import java.util.*
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog


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

        // Rejestrujemy callback dla przycisku wstecz
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })

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

        if (subtopicId.isNotEmpty() && topicId.isNotEmpty()) {
            loadTasksForSubtopic(topicId, subtopicId)
        } else {
            Toast.makeText(this, "Brak wymaganych danych", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTasksForSubtopic(topicId: String, subtopicId: String) {
        Log.d("TaskDebug", "Loading tasks for Topic ID: $topicId, Subtopic ID: $subtopicId")
        
        // Najpierw pobieramy zapisany postęp
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("TaskDebug", "Current user ID: $userId")
        
        database.child("users").child(userId)
            .child("topicsProgress").child(topicId)
            .child("subtopics").child(subtopicId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(progressSnapshot: DataSnapshot) {
                    Log.d("TaskDebug", "Progress snapshot exists: ${progressSnapshot.exists()}")
                    Log.d("TaskDebug", "Progress snapshot value: ${progressSnapshot.value}")
                    
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
        Log.d("TaskDebug", "Loading tasks for Topic ID: $topicId, Subtopic ID: $subtopicId")
        
        database.child("topics").child(topicId).child("subtopics")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val loadedTasks = mutableListOf<Task>()
                    
                    Log.d("TaskDebug", "Raw data from Firebase: ${snapshot.value}")
                    
                    try {
                        // Znajdź odpowiedni subtopic
                        val subtopicSnapshot = snapshot.children.find { 
                            it.child("id").getValue(String::class.java) == subtopicId 
                        }
                        
                        if (subtopicSnapshot != null) {
                            Log.d("TaskDebug", "Found subtopic: ${subtopicSnapshot.key}")
                            
                            // Próbujemy najpierw znaleźć zadania w strukturze "task"
                            val taskSnapshot = subtopicSnapshot.child("task")
                            if (taskSnapshot.exists()) {
                                Log.d("TaskDebug", "Found tasks in 'task' structure")
                                for (task in taskSnapshot.children) {
                                    try {
                                        val taskObj = Task(
                                            taskId = task.child("taskId").getValue(String::class.java) ?: "",
                                            type = task.child("type").getValue(String::class.java) ?: "",
                                            question = task.child("question").getValue(String::class.java) ?: "",
                                            options = task.child("options").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: listOf(),
                                            correctAnswer = task.child("correctAnswer").getValue(String::class.java) ?: "",
                                            description = task.child("description").getValue(String::class.java) ?: "",
                                            rewardXp = task.child("rewardXp").getValue(Int::class.java) ?: 0,
                                            rewardCoins = task.child("rewardCoins").getValue(Int::class.java) ?: 0,
                                            isCompleted = task.child("isCompleted").getValue(Boolean::class.java) ?: false,
                                            mediaUrl = task.child("mediaUrl").getValue(String::class.java) ?: ""
                                        )
                                        loadedTasks.add(taskObj)
                                        Log.d("TaskDebug", "Added task: ${taskObj.question}")
                                    } catch (e: Exception) {
                                        Log.e("TaskDebug", "Error parsing task: ${e.message}")
                                    }
                                }
                            } else {
                                // Jeśli nie ma struktury "task", próbujemy znaleźć zadania w strukturze "tasks"
                                val tasksSnapshot = subtopicSnapshot.child("tasks")
                                if (tasksSnapshot.exists()) {
                                    Log.d("TaskDebug", "Found tasks in 'tasks' structure")
                                    for (task in tasksSnapshot.children) {
                                        try {
                                            val taskObj = Task(
                                                taskId = task.child("taskId").getValue(String::class.java) ?: "",
                                                type = task.child("type").getValue(String::class.java) ?: "",
                                                question = task.child("question").getValue(String::class.java) ?: "",
                                                options = task.child("options").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: listOf(),
                                                correctAnswer = task.child("correctAnswer").getValue(String::class.java) ?: "",
                                                description = task.child("description").getValue(String::class.java) ?: "",
                                                rewardXp = task.child("rewardXp").getValue(Int::class.java) ?: 0,
                                                rewardCoins = task.child("rewardCoins").getValue(Int::class.java) ?: 0,
                                                isCompleted = task.child("isCompleted").getValue(Boolean::class.java) ?: false,
                                                mediaUrl = task.child("mediaUrl").getValue(String::class.java) ?: ""
                                            )
                                            loadedTasks.add(taskObj)
                                            Log.d("TaskDebug", "Added task: ${taskObj.question}")
                                        } catch (e: Exception) {
                                            Log.e("TaskDebug", "Error parsing task: ${e.message}")
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.e("TaskDebug", "Subtopic not found: $subtopicId")
                        }
                    } catch (e: Exception) {
                        Log.e("TaskDebug", "Error processing tasks: ${e.message}")
                    }
                    
                    if (loadedTasks.isEmpty()) {
                        Log.e("TaskDebug", "No tasks found for Topic ID: $topicId, Subtopic ID: $subtopicId")
                        runOnUiThread {
                            Toast.makeText(this@TaskDisplayActivity, "Brak zadań dla tego podtematu", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return
                    }

                    tasks = loadedTasks
                    Log.d("TaskDebug", "Loaded ${tasks.size} tasks, starting from task $currentTaskIndex")
                    runOnUiThread {
                        displayCurrentTask()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TaskDebug", "Error loading tasks: ${error.message}")
                    runOnUiThread {
                        Toast.makeText(this@TaskDisplayActivity, "Błąd podczas ładowania zadań: ${error.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            })
    }

    private fun displayCurrentTask() {
        try {
            Log.d("TaskDebug", "Displaying task at index: $currentTaskIndex")
            
            if (currentTaskIndex >= tasks.size) {
                Log.d("TaskDebug", "All tasks completed, finishing activity")
                runOnUiThread {
                    Toast.makeText(this, "Gratulacje! Ukończyłeś wszystkie zadania!", Toast.LENGTH_LONG).show()
                }
                finish()
                return
            }

            if (tasks.isEmpty()) {
                Log.e("TaskDebug", "Tasks list is empty")
                runOnUiThread {
                    Toast.makeText(this, "Błąd: Lista zadań jest pusta", Toast.LENGTH_SHORT).show()
                }
                finish()
                return
            }

            val currentTask = tasks[currentTaskIndex]
            Log.d("TaskDebug", "Current task: $currentTask")
            
            runOnUiThread {
                try {
                    // Aktualizuj tekst postępu
                    progressTextView.text = "Zadanie ${currentTaskIndex + 1}/${tasks.size}"

                    // Ustaw pytanie
                    questionTextView.text = currentTask.question

                    // Wyczyść poprzednie opcje
                    optionsContainer.removeAllViews()

                    // Sprawdź czy mamy opcje odpowiedzi
                    if (currentTask.options.isEmpty()) {
                        Log.e("TaskDebug", "No options available for task: ${currentTask.taskId}")
                        Toast.makeText(this, "Błąd: Brak opcji odpowiedzi", Toast.LENGTH_SHORT).show()
                        finish()
                        return@runOnUiThread
                    }

                    // Dodaj nowe przyciski opcji
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
                                Log.d("TaskDebug", "Selected answer: $option")
                                handleAnswer(option, currentTask.correctAnswer)
                            }
                            // Ustawiamy styl i wygląd przycisku
                            setTextColor(ContextCompat.getColor(context, android.R.color.white))
                            setBackgroundResource(R.drawable.answer_button_background)
                            textSize = 16f
                            setPadding(48, 32, 48, 32)
                            elevation = 4f
                            isEnabled = true // Upewniamy się, że przycisk jest aktywny
                        }
                        optionsContainer.addView(button)
                    }

                    // Ukryj feedback
                    feedbackTextView.visibility = View.INVISIBLE

                    // Sprawdź czy zadanie ma URL do audio
                    if (currentTask.mediaUrl.isNotEmpty()) {
                        playAudioButton.visibility = View.VISIBLE
                        playAudioButton.setOnClickListener {
                            playAudio(currentTask.mediaUrl)
                        }
                    } else {
                        playAudioButton.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e("TaskDebug", "Error updating UI: ${e.message}", e)
                    Toast.makeText(this, "Błąd podczas aktualizacji interfejsu: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e("TaskDebug", "Error in displayCurrentTask: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Błąd podczas wyświetlania zadania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun handleAnswer(selectedAnswer: String, correctAnswer: String) {
        try {
            // Sprawdzamy czy wszystkie wymagane dane są dostępne
            if (!::subtopicId.isInitialized || !::topicId.isInitialized) {
                Log.e("TaskDisplayActivity", "subtopicId lub topicId nie zostały zainicjalizowane")
                Toast.makeText(this, "Błąd: Brak wymaganych danych", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (tasks.isEmpty()) {
                Log.e("TaskDisplayActivity", "Lista zadań jest pusta")
                Toast.makeText(this, "Błąd: Brak zadań", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (currentTaskIndex >= tasks.size) {
                Log.e("TaskDisplayActivity", "Invalid task index: $currentTaskIndex, tasks size: ${tasks.size}")
                Toast.makeText(this, "Błąd: Nieprawidłowy indeks zadania", Toast.LENGTH_SHORT).show()
                return
            }

            // Wyłączamy przyciski na czas przetwarzania odpowiedzi
            runOnUiThread {
                for (i in 0 until optionsContainer.childCount) {
                    optionsContainer.getChildAt(i).isEnabled = false
                }
            }
            
            lastSelectedAnswer = selectedAnswer
            val isCorrect = selectedAnswer == correctAnswer

            // Aktualizujemy postęp niezależnie od poprawności odpowiedzi
            updateProgress(subtopicId, topicId, tasks[currentTaskIndex])

            // Pokaż feedback i przejdź do następnego zadania
            runOnUiThread {
                showFeedbackAndContinue(isCorrect, correctAnswer)
            }
        } catch (e: Exception) {
            Log.e("TaskDisplayActivity", "Error handling answer: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Wystąpił błąd podczas przetwarzania odpowiedzi: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Włączamy z powrotem przyciski w przypadku błędu
                for (i in 0 until optionsContainer.childCount) {
                    val button = optionsContainer.getChildAt(i)
                    button.isEnabled = true
                }
            }
        }
    }

    private fun showFeedbackAndContinue(isCorrect: Boolean, correctAnswer: String) {
        try {
            // Wyłącz przyciski
            for (i in 0 until optionsContainer.childCount) {
                optionsContainer.getChildAt(i).isEnabled = false
            }

            // Tworzenie i konfiguracja okna dialogowego
            val bottomSheetDialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.feedback_bottom_sheet, null)
            bottomSheetDialog.setContentView(view)

            // Konfiguracja tekstu feedbacku
            val feedbackMessageText = view.findViewById<TextView>(R.id.feedbackMessageText)
            if (feedbackMessageText != null) {
                if (isCorrect) {
                    feedbackMessageText.text = "Poprawna odpowiedź!"
                    feedbackMessageText.setTextColor(Color.GREEN)
                } else {
                    feedbackMessageText.text = "Niepoprawna odpowiedź.\nPoprawna odpowiedź to: $correctAnswer"
                    feedbackMessageText.setTextColor(Color.RED)
                }
            }

            // Konfiguracja przycisku "następne"
            val nextButton = view.findViewById<Button>(R.id.nextButton)
            if (nextButton != null) {
                nextButton.setOnClickListener {
                    try {
                        bottomSheetDialog.dismiss()
                        currentTaskIndex++
                        saveCurrentProgress {
                            runOnUiThread {
                                try {
                                    displayCurrentTask()
                                } catch (e: Exception) {
                                    Log.e("TaskDisplayActivity", "Error displaying next task: ${e.message}", e)
                                    Toast.makeText(this, "Błąd podczas wyświetlania następnego zadania: ${e.message}", Toast.LENGTH_SHORT).show()
                                    finish() // Wracamy do poprzedniego ekranu w przypadku błędu
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TaskDisplayActivity", "Error in next button click handler: ${e.message}", e)
                        Toast.makeText(this, "Błąd podczas przechodzenia do następnego zadania: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish() // Wracamy do poprzedniego ekranu w przypadku błędu
                    }
                }
            }

            // Pokazanie okna dialogowego
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)
            bottomSheetDialog.show()
        } catch (e: Exception) {
            Log.e("TaskDisplayActivity", "Error showing feedback: ${e.message}", e)
            Toast.makeText(this, "Błąd podczas wyświetlania informacji zwrotnej: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // W przypadku błędu, próbujemy przejść do następnego zadania
            currentTaskIndex++
            try {
                displayCurrentTask()
            } catch (e2: Exception) {
                Log.e("TaskDisplayActivity", "Error displaying next task after feedback error: ${e2.message}", e2)
                Toast.makeText(this, "Błąd podczas wyświetlania następnego zadania: ${e2.message}", Toast.LENGTH_SHORT).show()
                finish() // Wracamy do poprzedniego ekranu w przypadku błędu
            }
        }
    }

    private fun updateProgress(subtopicId: String, topicId: String, currentTask: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("TaskDebug", "Updating progress for user: $userId, topic: $topicId, subtopic: $subtopicId")
        
        val userRef = database.child("users").child(userId)
        val userProgressRef = userRef.child("topicsProgress").child(topicId)

        database.child("topics").child(topicId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(topicSnapshot: DataSnapshot) {
                Log.d("TaskDebug", "Topic snapshot exists: ${topicSnapshot.exists()}")
                
                val topic = topicSnapshot.getValue(Topic::class.java)
                if (topic == null) {
                    Log.e("TaskDebug", "Failed to parse topic data")
                    return
                }
                
                val subtopic = topic.subtopics.find { it.id == subtopicId }
                if (subtopic == null) {
                    Log.e("TaskDebug", "Subtopic not found in topic")
                    return
                }
                
                Log.d("TaskDebug", "Found subtopic: ${subtopic.title}")
                
                userProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(progressSnapshot: DataSnapshot) {
                        try {
                            Log.d("TaskDebug", "Progress snapshot exists: ${progressSnapshot.exists()}")
                            
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

                            Log.d("TaskDebug", "Current completed tasks: ${currentSubtopicProgress.completedTasks}")
                            Log.d("TaskDebug", "New completed tasks: $newCompletedTasks")

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

                            Log.d("TaskDebug", "Updating topic progress: $updatedTopicProgress")

                            userProgressRef.setValue(updatedTopicProgress)
                                .addOnSuccessListener {
                                    Log.d("TaskDebug", "Successfully updated topic progress")
                                    // Aktualizujemy statystyki użytkownika
                                    updateUserStats(userRef, currentTask)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("TaskDebug", "Failed to update progress", e)
                                    runOnUiThread {
                                        Toast.makeText(this@TaskDisplayActivity, 
                                            "Błąd podczas aktualizacji postępu", 
                                            Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } catch (e: Exception) {
                            Log.e("TaskDebug", "Error updating progress", e)
                            runOnUiThread {
                                Toast.makeText(this@TaskDisplayActivity, 
                                    "Błąd podczas aktualizacji postępu: ${e.message}", 
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TaskDebug", "Failed to update progress", error.toException())
                        runOnUiThread {
                            Toast.makeText(this@TaskDisplayActivity, 
                                "Błąd podczas aktualizacji postępu: ${error.message}", 
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskDebug", "Failed to load topic data", error.toException())
                runOnUiThread {
                    Toast.makeText(this@TaskDisplayActivity, 
                        "Błąd podczas ładowania danych: ${error.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateUserStats(userRef: DatabaseReference, currentTask: Task) {
        userRef.get().addOnSuccessListener { userSnapshot ->
            try {
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
                
                Log.d("TaskDebug", "Updating user stats: $updates")
                
                userRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("TaskDebug", "Successfully updated user stats")
                        // Aktualizacja wyzwań
                        updateChallenges(userRef.key ?: return@addOnSuccessListener)
                    }
                    .addOnFailureListener { e ->
                        Log.e("TaskDebug", "Failed to update user stats", e)
                        runOnUiThread {
                            Toast.makeText(this@TaskDisplayActivity, 
                                "Błąd podczas aktualizacji statystyk", 
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: Exception) {
                Log.e("TaskDebug", "Error updating user stats", e)
                runOnUiThread {
                    Toast.makeText(this@TaskDisplayActivity, 
                        "Błąd podczas aktualizacji statystyk: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("TaskDebug", "Failed to get user data", e)
            runOnUiThread {
                Toast.makeText(this@TaskDisplayActivity, 
                    "Błąd podczas pobierania danych użytkownika", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateChallenges(userId: String) {
        val challengesRef = database.child("users").child(userId).child("challenges")
        val userRef = database.child("users").child(userId)

        userRef.get().addOnSuccessListener { userSnapshot ->
            val user = userSnapshot.getValue(User::class.java) ?: return@addOnSuccessListener
            val currentDate = System.currentTimeMillis()
            
            // Sprawdzamy czy mamy poprawną odpowiedź na aktualne zadanie
            val isCorrectAnswer = lastSelectedAnswer == tasks[currentTaskIndex].correctAnswer
            val currentTask = tasks[currentTaskIndex]
            
            // Pobieramy aktualną wartość XP użytkownika bezpośrednio z Firebase
            val currentXp = userSnapshot.child("xp").getValue(Int::class.java) ?: 0
            
            challengesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Jeśli nie ma wyzwań, tworzymy domyślne (to również zapewnia, że wszystkie wymagane wyzwania istnieją)
                    if (!snapshot.exists() || !snapshot.hasChildren()) {
                        // Tworzymy domyślne wyzwania używając tych samych wartości co w ChallengesActivity
                        val defaultChallenges = createDefaultChallenges()
                        defaultChallenges.forEach { challenge ->
                            challengesRef.child(challenge.id).setValue(challenge)
                        }
                    }
                    
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
                                                if (isCorrectAnswer) {
                                                    val newProgress = it.currentProgress + 1
                                                    updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                }
                                            }
                                            "Dzienny zdobywca XP" -> {
                                                if (isCorrectAnswer) {
                                                    // Sprawdzamy bezpośrednio aktualny postęp wyzwania z Firebase
                                                    val currentProgress = challengeSnapshot.child("currentProgress").getValue(Int::class.java) ?: 0
                                                    val xpToAdd = currentTask.rewardXp
                                                    
                                                    // Upewniamy się, że postęp nie przekroczy wymaganej wartości
                                                    val newProgress = Math.min(currentProgress + xpToAdd, it.requiredValue)
                                                    
                                                    Log.d("ChallengeProgress", "Updating XP challenge: current XP in DB: $currentXp, " +
                                                            "current progress: $currentProgress, adding: $xpToAdd, new progress: $newProgress")
                                                    
                                                    // Aktualizujemy postęp tylko jeśli się zmienił
                                                    if (newProgress > currentProgress) {
                                                        updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                    }
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
                                    }
                                    
                                    // Aktualizujemy postęp tylko jeśli wyzwanie nie jest jeszcze ukończone
                                    if (!it.isCompleted) {
                                        when (it.title) {
                                            "Tygodniowa seria" -> {
                                                val lastDayTimestamp = userSnapshot.child("lastDayTimestamp").getValue(Long::class.java) ?: 0
                                                val isDayChange = (currentDate - lastDayTimestamp) > 24 * 60 * 60 * 1000
                                                
                                                if (isDayChange) {
                                                    // Sprawdzamy czy użytkownik wykonał minimalną liczbę zadań danego dnia
                                                    val tasksToday = user.tasksCompleted - (userSnapshot.child("lastDayTasksCount").getValue(Int::class.java) ?: 0)
                                                    if (tasksToday >= 5) { // Zmniejszona wartość wymagana do 5 zadań dziennie
                                                        val newProgress = it.currentProgress + 1
                                                        updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                    }
                                                    
                                                    // Aktualizujemy liczniki niezależnie od liczby wykonanych zadań
                                                    userRef.updateChildren(mapOf(
                                                        "lastDayTasksCount" to user.tasksCompleted,
                                                        "lastDayTimestamp" to currentDate
                                                    ))
                                                }
                                            }
                                            "Perfekcyjny tydzień" -> {
                                                // Aktualizujemy liczniki dzisiejszych zadań
                                                val todaysPerfectTasks = userSnapshot.child("todaysPerfectTasks").getValue(Int::class.java) ?: 0
                                                val todaysTotalTasks = userSnapshot.child("todaysTotalTasks").getValue(Int::class.java) ?: 0
                                                
                                                val updates = hashMapOf<String, Any>(
                                                    "todaysTotalTasks" to (todaysTotalTasks + 1)
                                                )
                                                
                                                // Jeśli odpowiedź jest poprawna, zwiększamy licznik perfekcyjnych zadań
                                                if (isCorrectAnswer) {
                                                    updates["todaysPerfectTasks"] = todaysPerfectTasks + 1
                                                    
                                                    // Sprawdzamy czy mamy wszystkie zadania poprawne i czy osiągnęliśmy minimalną liczbę (5)
                                                    if (todaysPerfectTasks + 1 >= 5 && todaysPerfectTasks + 1 == todaysTotalTasks + 1) {
                                                        // Dodajemy +1 do postępu wyzwania
                                                        val newProgress = it.currentProgress + 1
                                                        updateChallengeProgress(challengeSnapshot.ref, newProgress, it.requiredValue)
                                                        
                                                        // Resetujemy liczniki na nowy dzień
                                                        updates["lastPerfectDay"] = currentDate
                                                        updates["todaysPerfectTasks"] = 0
                                                        updates["todaysTotalTasks"] = 0
                                                    }
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

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TaskDisplayActivity", "Failed to update challenges", error.toException())
                }
            })
        }
    }

    private fun updateChallengeProgress(challengeRef: DatabaseReference, newProgress: Int, requiredValue: Int) {
        // Sprawdzamy, czy już ukończyliśmy wyzwanie
        challengeRef.get().addOnSuccessListener { snapshot ->
            // Sprawdzamy oba możliwe nazwy pola
            val isCompletedField1 = snapshot.child("isCompleted").getValue(Boolean::class.java)
            val isCompletedField2 = snapshot.child("completed").getValue(Boolean::class.java)
            val isAlreadyCompleted = isCompletedField1 ?: isCompletedField2 ?: false
            
            // Jeśli wyzwanie jest już ukończone, nie aktualizujemy go ponownie
            if (isAlreadyCompleted) {
                Log.d("ChallengeProgress", "Challenge already completed, skipping update")
                return@addOnSuccessListener
            }
            
            if (newProgress >= requiredValue) {
                val userRef = challengeRef.parent?.parent // Przejście do referencji użytkownika
                userRef?.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val challengeKey = challengeRef.key
                        val challengeData = currentData.child("challenges").child(challengeKey!!)
                        
                        // Sprawdzamy ponownie, czy wyzwanie nie zostało już ukończone (sprawdzając oba możliwe pola)
                        val isCompletedField1 = challengeData.child("isCompleted").getValue(Boolean::class.java)
                        val isCompletedField2 = challengeData.child("completed").getValue(Boolean::class.java)
                        val isCompleted = isCompletedField1 ?: isCompletedField2 ?: false
                        
                        if (isCompleted) {
                            return Transaction.success(currentData)
                        }
                        
                        val currentCompletedChallenges = currentData.child("completedChallenges").getValue(Int::class.java) ?: 0
                        
                        challengeData.child("currentProgress").value = requiredValue
                        // Aktualizujemy oba możliwe pola
                        challengeData.child("isCompleted").value = true
                        challengeData.child("completed").value = true
                        currentData.child("completedChallenges").value = currentCompletedChallenges + 1
                        
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error != null) {
                            Log.e("TaskDisplayActivity", "Failed to update challenge progress: ${error.message}")
                        } else if (committed) {
                            // Pobierz informacje o ukończonym wyzwaniu, aby pokazać powiadomienie
                            challengeRef.get().addOnSuccessListener { challengeSnapshot ->
                                val challenge = challengeSnapshot.getValue(Challenge::class.java)
                                challenge?.let {
                                    // Powiadomienie dla użytkownika o ukończeniu wyzwania
                                    Toast.makeText(
                                        this@TaskDisplayActivity,
                                        "Wyzwanie ukończone: ${it.title}! Odbierz nagrodę w sekcji wyzwań.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                })
            } else {
                // Aktualizujemy postęp tylko jeśli jest większy niż obecny
                val currentProgress = snapshot.child("currentProgress").getValue(Int::class.java) ?: 0
                if (newProgress > currentProgress) {
                    challengeRef.child("currentProgress").setValue(newProgress)
                    Log.d("ChallengeProgress", "Updated challenge progress from $currentProgress to $newProgress")
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        handler.removeCallbacksAndMessages(null)
    }

    // Pomocnicza metoda do tworzenia domyślnych wyzwań
    private fun createDefaultChallenges(): List<Challenge> {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val endOfWeek = Calendar.getInstance().apply {
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return listOf(
            Challenge(
                id = "daily_xp",
                title = "Dzienny zdobywca XP",
                description = "Zdobądź 100 XP w ciągu dnia",
                type = ChallengeType.DAILY,
                requiredValue = 100,
                reward = Reward(coins = 150),
                expiresAt = endOfDay,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "daily_tasks",
                title = "Codzienna praktyka",
                description = "Ukończ 5 zadań",
                type = ChallengeType.DAILY,
                requiredValue = 5,
                reward = Reward(coins = 100),
                expiresAt = endOfDay,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "weekly_perfect",
                title = "Perfekcyjny tydzień",
                description = "Zdobądź 10 perfekcyjnych wyników w tym tygodniu",
                type = ChallengeType.WEEKLY,
                requiredValue = 10,
                reward = Reward(coins = 500),
                expiresAt = endOfWeek,
                lastUpdateTime = currentTime
            ),
            Challenge(
                id = "weekly_streak",
                title = "Tygodniowa seria",
                description = "Utrzymaj 7-dniową serię nauki",
                type = ChallengeType.WEEKLY,
                requiredValue = 7,
                reward = Reward(coins = 400),
                expiresAt = endOfWeek,
                lastUpdateTime = currentTime
            )
        )
    }
}