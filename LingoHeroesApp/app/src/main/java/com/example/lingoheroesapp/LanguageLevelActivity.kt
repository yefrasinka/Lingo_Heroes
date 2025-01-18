package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class LanguageLevelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_level)

        // Кнопка для прохождения теста
        val testButton = findViewById<Button>(R.id.testButton)
        testButton.setOnClickListener {
            // Переход на экран с тестом
            val intent = Intent(this, TestActivity::class.java)
            startActivity(intent)
        }

        // RadioGroup для выбора уровня
        val levelSelectionLayout = findViewById<RadioGroup>(R.id.levelSelectionLayout)

        // Кнопка "Kontynuuj"
        val continueButton = findViewById<Button>(R.id.continueButton)

        // Изначально выключаем кнопку
        continueButton.isEnabled = false

        // Добавляем слушатель изменений в RadioGroup
        levelSelectionLayout.setOnCheckedChangeListener { group, checkedId ->
            // Включаем кнопку, если выбран уровень
            continueButton.isEnabled = checkedId != -1
        }

        // Обработчик клика по кнопке "Kontynuuj"
        continueButton.setOnClickListener {
            val selectedLevel = getSelectedLevel(levelSelectionLayout)
            // Здесь можно сохранить уровень или передать на другой экран
            val intent = Intent(this, MainMenuActivity::class.java)
            intent.putExtra("userLevel", selectedLevel)
            startActivity(intent)
        }
    }

    // Метод для получения выбранного уровня
    private fun getSelectedLevel(radioGroup: RadioGroup): String {
        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId != -1) {
            val selectedRadioButton = findViewById<RadioButton>(selectedId)
            return selectedRadioButton.text.toString()
        }
        return "Wybierz swój poziom języka angielskiego lub zbadaj testem!"
    }
}
