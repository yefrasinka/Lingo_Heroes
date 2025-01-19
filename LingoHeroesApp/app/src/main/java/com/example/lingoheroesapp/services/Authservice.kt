package com.example.lingoheroesapp.services

import android.util.Log
import com.example.lingoheroesapp.models.TopicProgress
import com.example.lingoheroesapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object AuthService {

    // Metoda rejestracji użytkownika
    fun registerUser(email: String, password: String, username: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        // Sprawdzanie poprawności danych wejściowych
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            onFailure("Proszę wypełnić wszystkie pola.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onFailure("Podany adres e-mail jest nieprawidłowy.")
            return
        }

        // Rejestracja użytkownika w Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val database = FirebaseDatabase.getInstance()

                    // Tworzymy obiekt User z domyślnymi wartościami
                    val userData = User(
                        uid = user?.uid ?: "",
                        username = username,
                        email = email,
                        level = 1,
                        xp = 0,
                        coins = 0,
                        completedTasks = emptyList(), // Pusta lista wykonanych zadań
                        purchasedItems = emptyList(), // Pusta lista zakupionych przedmiotów
                        topicsProgress = mapOf( // Pusta mapa postępów w tematach

                            "topicId_1" to TopicProgress(
                                topicId = "topicId_1",
                                completedSubtopics = 0,
                                totalSubtopics = 5,
                                progressPercentage = 0,
                                completedTasks = emptyList(),
                                totalTasks = 10
                            ),
                            "topicId_2" to TopicProgress(
                                topicId = "topicId_2",
                                completedSubtopics = 0,
                                totalSubtopics = 3,
                                progressPercentage = 0,
                                completedTasks = emptyList(),
                                totalTasks = 7
                            )
                        )
                    )

                    user?.uid?.let { uid ->
                        // Zapisujemy dane użytkownika w ścieżce /users/{uid}
                        database.reference.child("users").child(uid).setValue(userData)
                            .addOnSuccessListener {
                                Log.d("AuthService", "Dane użytkownika zapisane pomyślnie: $userData")
                                onSuccess()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("AuthService", "Błąd zapisu danych użytkownika: ${exception.message}")
                                onFailure("Błąd zapisu danych użytkownika: ${exception.message}")
                            }
                    }
                } else {
                    // Obsługa błędu rejestracji
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        onFailure("Użytkownik z tym adresem e-mail już istnieje.")
                    } else if (task.exception is FirebaseAuthWeakPasswordException) {
                        onFailure("Hasło jest za słabe. Wymagane jest co najmniej 6 znaków.")
                    } else if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        onFailure("Nieprawidłowy format adresu e-mail.")
                    } else {
                        onFailure("Błąd rejestracji: ${task.exception?.message}")
                    }
                }
            }
    }




    // Metoda logowania użytkownika
    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        // Sprawdzanie poprawności danych wejściowych
        if (email.isEmpty() || password.isEmpty()) {
            onFailure("Proszę wypełnić wszystkie pola.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onFailure("Podany adres e-mail jest nieprawidłowy.")
            return
        }

        // Próba logowania użytkownika w Firebase Authentication
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val database = FirebaseDatabase.getInstance()

                    user?.uid?.let { uid ->
                        // Sprawdzamy dane użytkownika w Realtime Database
                        database.reference.child("users").child(uid).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    // Mapowanie danych użytkownika na obiekt User
                                    val userData = snapshot.getValue(User::class.java)

                                    // Sprawdzenie, czy dane użytkownika zostały poprawnie pobrane
                                    if (userData != null) {
                                        // Zalogowano użytkownika pomyślnie
                                        // Zaktualizowanie użytkownika lokalnie
                                        // Możesz teraz przekazać dane użytkownika do dalszego przetwarzania
                                        onSuccess()
                                    } else {
                                        onFailure("Błąd pobierania danych użytkownika.")
                                    }
                                } else {
                                    onFailure("Użytkownik nie istnieje w bazie danych.")
                                }
                            }
                            .addOnFailureListener { exception ->
                                onFailure("Błąd przy weryfikacji użytkownika: ${exception.message}")
                            }
                    }
                } else {
                    // Obsługa błędów przy logowaniu
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        onFailure("Nieprawidłowy adres e-mail lub hasło.")
                    } else if (task.exception is FirebaseAuthUserCollisionException) {
                        onFailure("Użytkownik z tym adresem e-mail już istnieje.")
                    } else {
                        onFailure("Błąd logowania: ${task.exception?.message}")
                    }
                }
            }
    }

}
