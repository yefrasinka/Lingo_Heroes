package com.example.lingoheroesapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = Firebase.database.reference
    private var currentUsername: String = ""
    private var currentEmail: String = ""
    private lateinit var usernameTextView: TextView // Оголошуємо змінну для usernameTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        auth = Firebase.auth
        val userId = auth.currentUser?.uid
        usernameTextView = findViewById(R.id.usernameTextView) // Прив'язуємо до елементу в activity

        // Загружаем текущие данные пользователя
        if (userId != null) {
            database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUsername = snapshot.child("username").getValue(String::class.java) ?: ""
                    currentEmail = snapshot.child("email").getValue(String::class.java) ?: auth.currentUser?.email ?: ""

                    // Устанавливаем текст в usernameTextView
                    usernameTextView.text = currentUsername
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AccountActivity, "Не удалось загрузить данные пользователя.", Toast.LENGTH_SHORT).show()
                }
            })
        }

        val changeUsernameButton = findViewById<Button>(R.id.changeUsernameButton)
        val changeEmailButton = findViewById<Button>(R.id.changeEmailButton)
        val changePasswordButton = findViewById<Button>(R.id.changePasswordButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val closeButton = findViewById<ImageView>(R.id.closeButton)

        changeUsernameButton.setOnClickListener { showChangeUsernameDialog() }
        changeEmailButton.setOnClickListener { showChangeEmailDialog() }
        changePasswordButton.setOnClickListener { showChangePasswordDialog() }
        logoutButton.setOnClickListener { logoutUser() }
        closeButton.setOnClickListener {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showChangeUsernameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_username, null)
        val editText = dialogView.findViewById<EditText>(R.id.usernameEditText)

        // Устанавливаем текущее имя пользователя
        editText.setText(currentUsername)

        AlertDialog.Builder(this)
            .setTitle("Zmiana nazwy użytkownika")
            .setView(dialogView)
            .setPositiveButton("Zmień") { _, _ ->
                val newUsername = editText.text.toString().trim()
                if (newUsername.isNotEmpty()) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        database.child("users").child(userId).child("username").setValue(newUsername)
                            .addOnSuccessListener {
                                currentUsername = newUsername
                                usernameTextView.text = newUsername // Оновлюємо текст в TextView
                                Toast.makeText(this, "Nazwa użytkownika została zmieniona!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Błąd zmiany nazwy użytkownika.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Pole nie może być puste.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()
            .show()
    }

    private fun showChangeEmailDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_email, null)
        val editText = dialogView.findViewById<EditText>(R.id.emailEditText)

        // Устанавливаем текущий email
        editText.setText(currentEmail)

        AlertDialog.Builder(this)
            .setTitle("Zmiana emaila")
            .setView(dialogView)
            .setPositiveButton("Zmień") { _, _ ->
                val newEmail = editText.text.toString().trim()
                if (newEmail.isNotEmpty()) {
                    auth.currentUser?.updateEmail(newEmail)
                        ?.addOnSuccessListener {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                database.child("users").child(userId).child("email").setValue(newEmail)
                            }
                            currentEmail = newEmail
                            Toast.makeText(this, "Email został zmieniony!", Toast.LENGTH_SHORT).show()
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(this, "Błąd zmiany emaila: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Pole nie może być puste.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val editText = dialogView.findViewById<EditText>(R.id.passwordEditText)

        AlertDialog.Builder(this)
            .setTitle("Zmiana hasła")
            .setView(dialogView)
            .setPositiveButton("Zmień") { _, _ ->
                val newPassword = editText.text.toString()
                if (newPassword.length >= 6) {
                    auth.currentUser?.updatePassword(newPassword)
                        ?.addOnSuccessListener {
                            Toast.makeText(this, "Hasło zostało zmienione!", Toast.LENGTH_SHORT).show()
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(this, "Błąd zmiany hasła: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Hasło musi mieć co najmniej 6 znaków.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()
            .show()
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Wylogowano pomyślnie.", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
