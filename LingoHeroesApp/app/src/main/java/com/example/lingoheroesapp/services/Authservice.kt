package com.example.lingoheroesapp.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.lingoheroesapp.models.User
object AuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    fun registerUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val newUser = User(userId, email)
                    database.child("users").child(userId).setValue(newUser)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { exception ->
                            onFailure(exception.message ?: "Błąd zapisu użytkownika")
                        }
                } else {
                    task.exception?.let { exception ->
                        onFailure(exception.message ?: "Błąd rejestracji")
                    }
                }
            }
    }

    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    task.exception?.let { exception ->
                        onFailure(exception.message ?: "Błąd logowania")
                    }
                }
            }
    }

}
