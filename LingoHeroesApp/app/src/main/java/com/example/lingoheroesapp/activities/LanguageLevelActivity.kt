package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.TestActivity
import com.example.lingoheroesapp.services.AuthService
import com.google.firebase.auth.FirebaseAuth

class LanguageLevelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_level)

        // Кнопка для прохождения теста
        val testButton = findViewById<Button>(R.id.testButton)
        testButton.setOnClickListener {
            // Переход на экран с тестом
            val intent = Intent(this, TestLanguageActivity::class.java)
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
            val numericLevel = convertLevelToNumeric(selectedLevel)
            saveUserLevel(numericLevel)
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
    private fun convertLevelToNumeric(level: String): Int {
        return when (level) {
            "A1" -> 1
            "A2" -> 2
            "B1" -> 3
            "B2" -> 4
            else -> 0 // Або інше значення за замовчуванням
        }
    }
    private fun saveUserLevel(level: Int) {
        // Отримуємо uid користувача
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Зберігаємо рівень в базі даних
        AuthService.updateUserField(uid, "level", level, {
            // Успішно збережено
            // Можна додати Toast або інше повідомлення
        }, { errorMessage ->
            // Помилка збереження
            // Можна додати Toast або інше повідомлення
        })
    }
}
