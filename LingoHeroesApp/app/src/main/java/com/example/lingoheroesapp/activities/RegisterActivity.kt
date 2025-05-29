package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.services.AuthService
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val usernameEditText = findViewById<EditText>(R.id.nameEditText) // Pole na nazwę użytkownika
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()
            val username = usernameEditText.text.toString().trim() // Pobieramy nazwę użytkownika

            // Sprawdzenie, czy wszystkie pola są wypełnione
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Wszystkie pola muszą być wypełnione.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sprawdzenie, czy hasła się zgadzają
            if (password != confirmPassword) {
                Toast.makeText(this, "Hasła się nie zgadzają.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sprawdzenie długości hasła
            if (password.length < 6) {
                Toast.makeText(this, "Hasło musi mieć co najmniej 6 znaków.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Wywołanie metody rejestracji
            AuthService.registerUser(email, password, username, {
                Toast.makeText(this, "Rejestracja zakończona sukcesem!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }, { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            })
        }

        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
