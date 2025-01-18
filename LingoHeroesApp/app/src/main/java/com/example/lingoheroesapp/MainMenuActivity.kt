package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_learning

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_learning -> true
                R.id.nav_minigames -> {
                    startActivity(Intent(this, MinigamesActivity::class.java))
                    true
                }
                R.id.nav_duels -> {
                    startActivity(Intent(this, DuelsActivity::class.java))
                    true
                }
                R.id.nav_store -> {
                    startActivity(Intent(this, StoreActivity::class.java))
                    true
                }
                else -> false
            }
        }

        val testButton = findViewById<Button>(R.id.testButton)
        val avatarButton = findViewById<ImageView>(R.id.avatarImage) // Аватарка как кнопка

        testButton.setOnClickListener {
            val intent = Intent(this, TestActivity::class.java)
            startActivity(intent)
        }

        avatarButton.setOnClickListener {
            // Переход на экран аккаунта
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
        }
    }
}