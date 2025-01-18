package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account)

        val changeUsernameButton = findViewById<Button>(R.id.changeUsernameButton)
        val changeEmailButton = findViewById<Button>(R.id.changeEmailButton)
        val changePasswordButton = findViewById<Button>(R.id.changePasswordButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val closeButton = findViewById<ImageView>(R.id.closeButton) // Кнопка-крестик

        changeUsernameButton.setOnClickListener {
            // Логика смены имени пользователя
        }

        changeEmailButton.setOnClickListener {
            // Логика смены email
        }

        changePasswordButton.setOnClickListener {
            // Логика смены пароля
        }

        logoutButton.setOnClickListener {
            // Логика выхода из аккаунта
        }

        closeButton.setOnClickListener {
            // Переход на главный экран
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish() // Закрытие текущего экрана
        }
    }
}
