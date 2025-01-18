package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val correctAnswers = intent.getIntExtra("correctAnswers", 0)
        val totalQuestions = intent.getIntExtra("totalQuestions", 0)
        val percentage = (correctAnswers * 100) / totalQuestions

        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val myImage = findViewById<ImageView>(R.id.myImage)
        val backToMenuButton = findViewById<Button>(R.id.backToMenuButton)

        // Устанавливаем текст результата
        resultTextView.text = "Gratulacje! Zdobyłeś $percentage% poprawnych odpowiedzi."

        // Устанавливаем изображение в зависимости от процента
        when (percentage) {
            in 0..30 -> myImage.setImageResource(R.drawable.image_low)
            in 30..70 -> myImage.setImageResource(R.drawable.image_neutral)
            in 70..100 -> myImage.setImageResource(R.drawable.image_high)
        }

        // Обработчик кнопки
        backToMenuButton.setOnClickListener {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
        }
    }
}

