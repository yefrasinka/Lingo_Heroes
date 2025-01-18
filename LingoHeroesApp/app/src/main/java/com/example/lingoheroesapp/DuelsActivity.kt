package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity

class DuelsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duels)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_duels

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_learning -> {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    true
                }
                R.id.nav_minigames -> {
                    startActivity(Intent(this, MinigamesActivity::class.java))
                    true
                }
                R.id.nav_duels -> true
                R.id.nav_store -> {
                    startActivity(Intent(this, StoreActivity::class.java))
                    true
                }
                else -> false
            }
        }

        val avatarImage = findViewById<ImageView>(R.id.avatarImage)
        avatarImage.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
    }
}
