package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.services.AuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TestLanguageActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var answersRadioGroup: RadioGroup
    private lateinit var nextButton: Button
    private var currentQuestionIndex = 0
    private var correctAnswersCount = 0
    private val questions = listOf(
        Question(
            "Wybierz poprawne tłumaczenie:\n\"Chodzę do szkoły codziennie.\"",
            listOf(
                "I am going to school every day.",
                "I go to school every day.",
                "I went to school every day.",
                "I was going to school every day."
            ),
            "I go to school every day."
        ),
        Question(
            "Wstaw czasownik w poprawnej formie:\nShe ___ (to work) in the garden now.",
            listOf("works", "worked", "is working", "has worked"),
            "is working"
        ),
        Question(
            "Wybierz poprawną odpowiedź:\nHow often do you watch TV?",
            listOf("On the weekend.", "Every day.", "At 5 o'clock.", "In the evening."),
            "Every day."
        ),
        Question(
            "Dokończ zdanie:\nIf I were you, I ___.",
            listOf("will go there.", "would go there.", "went there.", "go there."),
            "would go there."
        ),
        Question(
            "Wskaż poprawne tłumaczenie zdania:\n\"Ona nigdy się nie spóźnia na lekcje.\"",
            listOf(
                "She is never late to lessons.",
                "She never late to lessons.",
                "She is not late never to lessons.",
                "She was never late to lessons."
            ),
            "She is never late to lessons."
        ),
        Question(
            "Wybierz odpowiedni czasownik modalny:\nYou ___ wear a helmet while riding a bike.",
            listOf("can", "must", "should", "might"),
            "must"
        ),
        Question(
            "Ułóż słowa w poprawnej kolejności:\nbought / a / she / yesterday / dress / nice.",
            listOf(
                "She bought yesterday a dress nice.",
                "She nice dress bought yesterday.",
                "She bought a nice dress yesterday.",
                "Yesterday nice dress she bought."
            ),
            "She bought a nice dress yesterday."
        ),
        Question(
            "Rozpoznaj czas w zdaniu:\nThey have been living here for five years.",
            listOf("Present Perfect", "Present Perfect Continuous", "Past Simple", "Present Continuous"),
            "Present Perfect Continuous"
        ),
        Question(
            "Wstaw brakujące słowo:\nI am looking forward ___ seeing you.",
            listOf("to", "for", "at", "on"),
            "to"
        ),
        Question(
            "Wybierz poprawną odpowiedź:\nWhat is the capital of the United Kingdom?",
            listOf("Paris", "London", "Dublin", "Edinburgh"),
            "London"
        ),
        Question(
            "Wskaż poprawną odpowiedź:\nShe ___ her homework before dinner yesterday.",
            listOf("finished", "has finished", "finishes", "is finishing"),
            "finished"
        ),
        Question(
            "Przetłumacz zdanie:\n\"Ta książka jest ciekawsza niż tamta.\"",
            listOf(
                "This book is the most interesting than that one.",
                "This book is interesting than that one.",
                "This book is more interesting than that one.",
                "This book is interesting as that one."
            ),
            "This book is more interesting than that one."
        ),
        Question(
            "Połącz zdania:\nHe is very tired. He worked all day.",
            listOf(
                "He is very tired because he has worked all day.",
                "He is very tired because he is working all day.",
                "He is very tired because he worked all day.",
                "He is very tired because he works all day."
            ),
            "He is very tired because he worked all day."
        ),
        Question(
            "Popraw błąd w zdaniu:\nShe don't like coffee.",
            listOf(
                "She doesn't like coffee.",
                "She didn't like coffee.",
                "She won't like coffee.",
                "She isn't like coffee."
            ),
            "She doesn't like coffee."
        ),
        Question(
            "Przetłumacz zdanie:\n\"Potrzebuję więcej czasu, żeby pomyśleć.\"",
            listOf(
                "I need more time to think.",
                "I am needing more time to think.",
                "I have needed more time to think.",
                "I need time more to think."
            ),
            "I need more time to think."
        ),
        Question(
            "Wybierz poprawny wariant:\nIf it rains tomorrow, we ___.",
            listOf(
                "will stay at home.",
                "stay at home.",
                "would stay at home.",
                "have stayed at home."
            ),
            "will stay at home."
        ),
        Question(
            "Utwórz zdanie z \"used to\":",
            listOf(
                "I used to play football when I was a child.",
                "I am used to play football when I was a child.",
                "I was used to play football when I was a child.",
                "I use to play football when I was a child."
            ),
            "I used to play football when I was a child."
        ),
        Question(
            "Wybierz poprawną odpowiedź:\nWhat time does the train leave?",
            listOf(
                "The train is leaving at 10:00.",
                "The train leaves at 10:00.",
                "The train has left at 10:00.",
                "The train left at 10:00."
            ),
            "The train leaves at 10:00."
        ),
        Question(
            "Który z wariantów jest poprawnym tłumaczeniem zdania:\n\"Nigdy nie widziałem tak pięknych kwiatów.\"",
            listOf(
                "I never see such beautiful flowers.",
                "I never saw such beautiful flowers.",
                "I have never seen such beautiful flowers.",
                "I had never seen such beautiful flowers."
            ),
            "I have never seen such beautiful flowers."
        ),
        Question(
            "Dokończ zdanie:\nBy the time we arrived, they ___ dinner.",
            listOf("have finished", "finished", "had finished", "are finishing"),
            "had finished"
        )
    )

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_level_test)

        questionTextView = findViewById(R.id.questionTextView)
        answersRadioGroup = findViewById(R.id.answersRadioGroup)
        nextButton = findViewById(R.id.nextButton)

        auth = FirebaseAuth.getInstance()

        loadQuestion()
        setupNextButton()
    }

    private fun loadQuestion() {
        val currentQuestion = questions[currentQuestionIndex]
        questionTextView.text = currentQuestion.question
        answersRadioGroup.removeAllViews()
        currentQuestion.answers.forEach { answer ->
            val radioButton = RadioButton(this)
            radioButton.text = answer
            answersRadioGroup.addView(radioButton)
        }
    }

    private fun setupNextButton() {
        nextButton.setOnClickListener {
            val selectedAnswer = getSelectedAnswer()
            if (isCorrectAnswer(selectedAnswer)) {
                correctAnswersCount++
            }
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                loadQuestion()
            } else {
                showTestResult()
            }
        }
    }

    private fun getSelectedAnswer(): String {
        val selectedId = answersRadioGroup.checkedRadioButtonId
        if (selectedId != -1) {
            val selectedRadioButton = findViewById<RadioButton>(selectedId)
            return selectedRadioButton.text.toString()
        }
        return ""
    }

    private fun isCorrectAnswer(answer: String): Boolean {
        val currentQuestion = questions[currentQuestionIndex]
        return answer == currentQuestion.correctAnswer
    }

    private fun showTestResult() {
        val totalQuestions = questions.size
        val percentage = (correctAnswersCount.toDouble() / totalQuestions) * 100
        val level = when {
            percentage >= 80 -> 4 // B2
            percentage >= 60 -> 3 // B1
            percentage >= 40 -> 2 // A2
            else -> 1 // A1
        }

        val levelName = when (level) {
            1 -> "A1"
            2 -> "A2"
            3 -> "B1"
            4 -> "B2"
            else -> "A1"
        }

        AlertDialog.Builder(this)
            .setTitle("Wynik testu")
            .setMessage("Twój poziom to: $levelName\nPoprawne odpowiedzi: $correctAnswersCount/$totalQuestions")
            .setPositiveButton("OK") { _, _ ->
                updateUserLevel(level, levelName)
            }
            .setCancelable(false)
            .show()
    }

    private fun updateUserLevel(level: Int, levelName: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("level").setValue(level)
                .addOnSuccessListener {
                    Toast.makeText(this, "Twój poziom został ustawiony na $levelName", Toast.LENGTH_SHORT).show()
                    // Przekierowanie do MainMenuActivity
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Błąd podczas zapisywania poziomu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

data class Question(
    val question: String,
    val answers: List<String>,
    val correctAnswer: String
)