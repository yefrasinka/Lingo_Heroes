package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.activities.LanguageLevelActivity
import com.example.lingoheroesapp.activities.LoginActivity
import com.example.lingoheroesapp.activities.RegisterActivity
import com.example.lingoheroesapp.activities.MainMenuActivity
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Если пользователь не залогинен, переходим на экран логина
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Если пользователь авторизован, переходим в MainMenuActivity
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }
}