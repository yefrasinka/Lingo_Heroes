package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.lingoheroesapp.activities.MainMenuActivity
import com.example.lingoheroesapp.activities.ResultActivity
import com.example.lingoheroesapp.models.Task
import com.google.firebase.database.FirebaseDatabase

class TestActivity : AppCompatActivity() {

    // Pytania z opcjami odpowiedzi
    private val questions = listOf(
        // Gramatyczne zadanie
        Task(
            taskId = "1",
            title = "Choose the correct form:",
            description = "I usually ___ breakfast at 8 AM.",
            difficulty = "medium",
            type = "grammar",
            question = "I usually ___ breakfast at 8 AM.",
            options = listOf("ate", "eats", "eating", "eat"),
            correctAnswer = "eat",
            rewardXp = 10,
            rewardCoins = 5,
            topicId = "breakfast",
            subtopicId = "verb_forms",
            mediaUrl = null
        ),
        // Zadanie na dobór frazy
        Task(
            taskId = "2",
            title = "How do you say it in English?",
            description = "Śniadanie",
            difficulty = "easy",
            type = "vocabulary",
            question = "Śniadanie",
            options = listOf("Lunch", "Dinner", "Breakfast", "Snack"),
            correctAnswer = "Breakfast",
            rewardXp = 5,
            rewardCoins = 3,
            topicId = "meals",
            subtopicId = "vocabulary",
            mediaUrl = null
        ),
        // Tłumaczenie
        Task(
            taskId = "3",
            title = "Translate the sentence:",
            description = "Jem owsiankę na śniadanie.",
            difficulty = "medium",
            type = "grammar",
            question = "Jem owsiankę na śniadanie.",
            options = listOf("I drink tea for breakfast.", "I eat oatmeal for breakfast.", "I eat pancakes for breakfast.", "I make coffee in the morning."),
            correctAnswer = "I eat oatmeal for breakfast.",
            rewardXp = 15,
            rewardCoins = 8,
            topicId = "breakfast",
            subtopicId = "translations",
            mediaUrl = null
        ),
        // Wybór odpowiedniej formy czasownika
        Task(
            taskId = "4",
            title = "Choose the correct form of the verb:",
            description = "She ___ eggs for breakfast.",
            difficulty = "medium",
            type = "grammar",
            question = "She ___ eggs for breakfast.",
            options = listOf("cook", "cooked", "cooks", "cooking"),
            correctAnswer = "cooks",
            rewardXp = 12,
            rewardCoins = 6,
            topicId = "breakfast",
            subtopicId = "verb_forms",
            mediaUrl = null
        ),
        // Rozumienie pytania
        Task(
            taskId = "5",
            title = "Answer the Task.",
            description = "What do you have for breakfast?",
            difficulty = "easy",
            type = "reading",
            question = "What do you have for breakfast?",
            options = listOf("I have lunch.", "I have toast and coffee.", "I go to work.", "I sleep."),
            correctAnswer = "I have toast and coffee.",
            rewardXp = 8,
            rewardCoins = 4,
            topicId = "breakfast",
            subtopicId = "comprehension",
            mediaUrl = null
        ),
        // Wypełnianie luk
        Task(
            taskId = "6",
            title = "Fill in the blank:",
            description = "I ____ bread and butter in the morning.",
            difficulty = "medium",
            type = "grammar",
            question = "I ____ bread and butter in the morning.",
            options = listOf("eating", "eats", "ate", "eat"),
            correctAnswer = "eat",
            rewardXp = 10,
            rewardCoins = 5,
            topicId = "breakfast",
            subtopicId = "verb_forms",
            mediaUrl = null
        ),
        // Wybór poprawnego wariantu
        Task(
            taskId = "7",
            title = "Answer the Task.",
            description = "Which of these is usually eaten for breakfast?",
            difficulty = "easy",
            type = "vocabulary",
            question = "Which of these is usually eaten for breakfast?",
            options = listOf("Cereal", "Pizza", "Spaghetti", "Burger"),
            correctAnswer = "Cereal",
            rewardXp = 5,
            rewardCoins = 3,
            topicId = "meals",
            subtopicId = "vocabulary",
            mediaUrl = null
        ),
        // Tłumaczenie zdania
        Task(
            taskId = "8",
            title = "Translate the sentence:",
            description = "Lubię jeść jajka na miękko.",
            difficulty = "medium",
            type = "grammar",
            question = "Lubię jeść jajka na miękko.",
            options = listOf("I like to eat fried eggs.", "I eat scrambled eggs.", "I like to eat soft-boiled eggs.", "I like to eat bread."),
            correctAnswer = "I like to eat soft-boiled eggs.",
            rewardXp = 15,
            rewardCoins = 7,
            topicId = "breakfast",
            subtopicId = "translations",
            mediaUrl = null
        ),
        // Zadanie na zgodność czasów
        Task(
            taskId = "9",
            title = "Choose the correct option:",
            description = "They ___ coffee every morning.",
            difficulty = "medium",
            type = "grammar",
            question = "They ___ coffee every morning.",
            options = listOf("drinks", "drink", "drank", "drinking"),
            correctAnswer = "drink",
            rewardXp = 12,
            rewardCoins = 6,
            topicId = "breakfast",
            subtopicId = "verb_forms",
            mediaUrl = null
        ),
        // Rozumienie kontekstu
        Task(
            taskId = "10",
            title = "Answer the Task.",
            description = "What would you most likely say after finishing breakfast?",
            difficulty = "easy",
            type = "reading",
            question = "What would you most likely say after finishing breakfast?",
            options = listOf("I'm full, thank you.", "I'm hungry.", "It's time for lunch.", "I'm going to bed."),
            correctAnswer = "I'm full, thank you.",
            rewardXp = 10,
            rewardCoins = 5,
            topicId = "breakfast",
            subtopicId = "comprehension",
            mediaUrl = null
        )
    )


    //add new task
    fun loadQuestionsToFirebase() {
        val database = FirebaseDatabase.getInstance()
        val tasksRef = database.reference.child("tasks")


        questions.forEach { question ->
            //sprawdzamy, czy pytanie już istnieje
            tasksRef.child(question.taskId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    //jesli pytanie istnieje, zaktualizuj je
                    tasksRef.child(question.taskId).setValue(question)
                        .addOnSuccessListener {
                            println("Pytanie zostało zaktualizowane!")
                        }
                        .addOnFailureListener { e ->
                            println("Błąd przy aktualizowaniu pytania: ${e.message}")
                        }
                } else {
                    //jesli pytanie nie istnieje, dodaj je jako nowe
                    val taskId = tasksRef.push().key ?: return@addOnSuccessListener
                    tasksRef.child(taskId).setValue(question)
                        .addOnSuccessListener {
                            println("Pytanie zostało zapisane!")
                        }
                        .addOnFailureListener { e ->
                            println("Błąd przy zapisywaniu pytania: ${e.message}")
                        }
                }
            }.addOnFailureListener { e ->
                println("Błąd przy pobieraniu pytania: ${e.message}")
            }
        }
    }

   /* private var currentTaskIndex = 0
    private var correctAnswers = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        showTask()

        findViewById<Button>(R.id.option1Button).setOnClickListener { checkAnswer(0) }
        findViewById<Button>(R.id.option2Button).setOnClickListener { checkAnswer(1) }
        findViewById<Button>(R.id.option3Button).setOnClickListener { checkAnswer(2) }
        findViewById<Button>(R.id.option4Button).setOnClickListener { checkAnswer(3) }

        // Обработчик кнопки закрытия теста
        findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            showExitConfirmationDialog()
        }
    }

  private fun showTask() {
        val Task = Tasks[currentTaskIndex]

        findViewById<TextView>(R.id.taskTextView).text = Task.task
        findViewById<TextView>(R.id.wordTextView).text = Task.word
        findViewById<ImageView>(R.id.imageView).setImageResource(Task.imageRes)

        findViewById<Button>(R.id.option1Button).apply {
            text = Task.options[0]
            contentDescription = "Opcja 1: ${Task.options[0]}"
        }

        findViewById<Button>(R.id.option2Button).apply {
            text = Task.options[1]
            contentDescription = "Opcja 2: ${Task.options[1]}"
        }

        findViewById<Button>(R.id.option3Button).apply {
            text = Task.options[2]
            contentDescription = "Opcja 3: ${Task.options[2]}"
        }

        findViewById<Button>(R.id.option4Button).apply {
            text = Task.options[3]
            contentDescription = "Opcja 4: ${Task.options[3]}"
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val Task = Tasks[currentTaskIndex]
        val isCorrect = selectedIndex == Task.correctAnswerIndex

        if (isCorrect) {
            correctAnswers++
        } else {
            Toast.makeText(
                this,
                "Źle! Prawidłowa odpowiedź: ${Task.options[Task.correctAnswerIndex]}",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (currentTaskIndex < Tasks.size - 1) {
            currentTaskIndex++
            showTask()
        } else {
            // Показать результат
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("correctAnswers", correctAnswers)
            intent.putExtra("totalTasks", Tasks.size)
            startActivity(intent)
            finish()
        }
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Czy na pewno chcesz zakończyć test?")
            .setCancelable(false)
            .setPositiveButton("Tak") { _, _ ->
                // Переход в главное меню
                val intent = Intent(this, MainMenuActivity::class.java)
                startActivity(intent)
                finish() // Закрыть текущий экран (тест)
            }
            .setNegativeButton("Nie") { dialog, _ ->
                dialog.dismiss() // Закрыть диалог без выхода
            }

        val alert = builder.create()
        alert.show()
    }*/
}

