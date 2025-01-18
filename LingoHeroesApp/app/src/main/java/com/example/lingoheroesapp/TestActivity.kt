package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class TestActivity : AppCompatActivity() {

    // Вопросы с вариантами ответов
    private val questions = listOf(
        // Грамматическое задание
        Question(
            task = "Choose the correct form:",
            word = "I usually ___ breakfast at 8 AM.",
            imageRes = R.drawable.ic_breakfast,
            options = listOf("ate", "eats", "eating", "eat"),
            correctAnswerIndex = 3
        ),
        // Задание на подбор фразы
        Question(
            task = "How do you say it in English?",
            word = "Śniadanie",
            imageRes = R.drawable.ic_breakfast1,
            options = listOf("Lunch", "Dinner", "Breakfast", "Snack"),
            correctAnswerIndex = 2
        ),
        // Перевод
        Question(
            task = "Translate the sentence:",
            word = "Jem owsiankę na śniadanie.",
            imageRes = R.drawable.ic_oatmeal,
            options = listOf("I drink tea for breakfast.", "I eat oatmeal for breakfast.", "I eat pancakes for breakfast.", "I make coffee in the morning."),
            correctAnswerIndex = 1
        ),
        // Задание на выбор
        Question(
            task = "Choose the correct form of the verb:",
            word = "She ___ eggs for breakfast.",
            imageRes = R.drawable.ic_egg,
            options = listOf("cook", "cooked", "cooks", "cooking"),
            correctAnswerIndex = 2
        ),
        // Понимание предложения
        Question(
            task = "Answer the question.",
            word = "What do you have for breakfast?",
            imageRes = R.drawable.ic_toast,
            options = listOf("I have lunch.", "I have toast and coffee.", "I go to work.", "I sleep."),
            correctAnswerIndex = 1
        ),
        // Заполнение пропуска
        Question(
            task = "Fill in the blank: ",
            word = "I ____ bread and butter in the morning.",
            imageRes = R.drawable.ic_bread,
            options = listOf("eating", "eats", "ate", "eat"),
            correctAnswerIndex = 3
        ),
        // Вопрос с выбором правильного варианта
        Question(
            task = "Answer the question.",
            word = "Which of these is usually eaten for breakfast?",
            imageRes = R.drawable.ic_cereal,
            options = listOf("Cereal", "Pizza", "Spaghetti", "Burger"),
            correctAnswerIndex = 0
        ),
        // Перевод предложения
        Question(
            task = "Translate the sentence:",
            word = "Lubię jeść jajka na miękko.",
            imageRes = R.drawable.ic_egg1,
            options = listOf("I like to eat fried eggs.", "I eat scrambled eggs.", "I like to eat soft-boiled eggs.", "I like to eat bread."),
            correctAnswerIndex = 2
        ),
        // Задание на согласование времен
        Question(
            task = "Choose the correct option:",
            word = "They ___ coffee every morning.",
            imageRes = R.drawable.ic_coffee,
            options = listOf("drinks", "drink", "drank", "drinking"),
            correctAnswerIndex = 1
        ),
        // Задание на понимание контекста
        Question(
            task = "Answer the question.",
            word = "What would you most likely say after finishing breakfast?",
            imageRes = R.drawable.ic_breakfast2,
            options = listOf("I'm full, thank you.", "I'm hungry.", "It's time for lunch.", "I'm going to bed."),
            correctAnswerIndex = 0
        ),
    )

    private var currentQuestionIndex = 0
    private var correctAnswers = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        showQuestion()

        findViewById<Button>(R.id.option1Button).setOnClickListener { checkAnswer(0) }
        findViewById<Button>(R.id.option2Button).setOnClickListener { checkAnswer(1) }
        findViewById<Button>(R.id.option3Button).setOnClickListener { checkAnswer(2) }
        findViewById<Button>(R.id.option4Button).setOnClickListener { checkAnswer(3) }

        // Обработчик кнопки закрытия теста
        findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            showExitConfirmationDialog()
        }
    }

    private fun showQuestion() {
        val question = questions[currentQuestionIndex]

        findViewById<TextView>(R.id.taskTextView).text = question.task
        findViewById<TextView>(R.id.wordTextView).text = question.word
        findViewById<ImageView>(R.id.imageView).setImageResource(question.imageRes)

        findViewById<Button>(R.id.option1Button).apply {
            text = question.options[0]
            contentDescription = "Opcja 1: ${question.options[0]}"
        }

        findViewById<Button>(R.id.option2Button).apply {
            text = question.options[1]
            contentDescription = "Opcja 2: ${question.options[1]}"
        }

        findViewById<Button>(R.id.option3Button).apply {
            text = question.options[2]
            contentDescription = "Opcja 3: ${question.options[2]}"
        }

        findViewById<Button>(R.id.option4Button).apply {
            text = question.options[3]
            contentDescription = "Opcja 4: ${question.options[3]}"
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val question = questions[currentQuestionIndex]
        val isCorrect = selectedIndex == question.correctAnswerIndex

        if (isCorrect) {
            correctAnswers++
        } else {
            Toast.makeText(
                this,
                "Źle! Prawidłowa odpowiedź: ${question.options[question.correctAnswerIndex]}",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (currentQuestionIndex < questions.size - 1) {
            currentQuestionIndex++
            showQuestion()
        } else {
            // Показать результат
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("correctAnswers", correctAnswers)
            intent.putExtra("totalQuestions", questions.size)
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
    }
}

data class Question(
    val task: String,
    val word: String,
    val imageRes: Int,
    val options: List<String>,
    val correctAnswerIndex: Int
)