package com.example.lingoheroesapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerLink = findViewById<TextView>(R.id.registerLink)

        loginButton.setOnClickListener {

            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            when {
                email == "yefrasinnia@gmail.com" && password == "12345678" -> {
                    val intent = Intent(this, MainMenuActivity::class.java)
                    startActivity(intent)
                }
                email == "yefrasinnia@gmail.com" -> {
                    Toast.makeText(this, "Niepoprawne hasło.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    AlertDialog.Builder(this)
                        .setTitle("Nie znaleziono konta")
                        .setMessage("Czy chcesz utworzyć konto?")
                        .setPositiveButton("Tak") { _, _ ->
                            val intent = Intent(this, RegisterActivity::class.java)
                            startActivity(intent)
                        }
                        .setNegativeButton("Nie", null)
                        .show()
                }
            }
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
