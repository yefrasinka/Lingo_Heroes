package com.example.lingoheroesapp.services

import com.example.lingoheroesapp.models.FriendRequest
import com.example.lingoheroesapp.models.User
import com.example.lingoheroesapp.utils.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import android.util.Log

object FriendService {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Wysyłanie zaproszenia do znajomych
    fun sendFriendRequest(receiverId: String, receiverCallback: (Boolean, String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("FriendService", "Próba wysłania zaproszenia bez zalogowanego użytkownika")
            receiverCallback(false, "Nie jesteś zalogowany")
            return
        }
        
        Log.d("FriendService", "Wysyłanie zaproszenia od ${currentUser.uid} do $receiverId")
        
        // Najpierw sprawdź, czy użytkownik nie jest już znajomym
        isFriend(receiverId) { isFriend ->
            if (isFriend) {
                Log.d("FriendService", "Użytkownik $receiverId jest już znajomym")
                receiverCallback(false, "Ten użytkownik jest już Twoim znajomym")
                return@isFriend
            }
            
            // Sprawdź, czy zaproszenie nie zostało już wysłane
            checkExistingRequest(currentUser.uid, receiverId) { requestExists ->
                if (requestExists) {
                    Log.d("FriendService", "Zaproszenie do $receiverId zostało już wysłane")
                    receiverCallback(false, "Zaproszenie zostało już wysłane")
                    return@checkExistingRequest
                }
                
                // Pobierz dane użytkownika wysyłającego zaproszenie używając DatabaseHelper
                Log.d("FriendService", "Pobieranie danych użytkownika wysyłającego zaproszenie: ${currentUser.uid}")
                DatabaseHelper.safelyGetUser(database.child("users").child(currentUser.uid)) { sender ->
                    if (sender != null) {
                        // Utwórz zaproszenie
                        val requestId = database.child("friendRequests").push().key ?: return@safelyGetUser
                        Log.d("FriendService", "Utworzono ID zaproszenia: $requestId")
                        
                        val friendRequest = FriendRequest(
                            senderId = currentUser.uid,
                            receiverId = receiverId,
                            senderName = sender.username,
                            timestamp = Date().time
                        )

                        // Zapisz zaproszenie w bazie danych
                        Log.d("FriendService", "Zapisywanie zaproszenia od ${sender.username} do $receiverId")
                        database.child("friendRequests").child(requestId).setValue(friendRequest)
                            .addOnSuccessListener {
                                Log.d("FriendService", "Zaproszenie zapisane pomyślnie")
                                receiverCallback(true, "Zaproszenie wysłane pomyślnie")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FriendService", "Błąd podczas zapisywania zaproszenia: ${e.message}")
                                receiverCallback(false, "Błąd podczas wysyłania zaproszenia: ${e.message}")
                            }
                    } else {
                        Log.e("FriendService", "Nie można znaleźć danych użytkownika ${currentUser.uid}")
                        receiverCallback(false, "Nie można znaleźć danych użytkownika")
                    }
                }
            }
        }
    }
    
    // Sprawdza, czy istnieje już zaproszenie między tymi użytkownikami
    private fun checkExistingRequest(senderId: String, receiverId: String, callback: (Boolean) -> Unit) {
        database.child("friendRequests")
            .orderByChild("senderId")
            .equalTo(senderId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var exists = false
                    for (requestSnapshot in snapshot.children) {
                        try {
                            val request = requestSnapshot.getValue(FriendRequest::class.java)
                            if (request != null && request.receiverId == receiverId && request.status == "pending") {
                                exists = true
                                break
                            }
                        } catch (e: Exception) {
                            Log.e("FriendService", "Błąd deserializacji zaproszenia: ${e.message}")
                            // Pomijamy to zaproszenie w przypadku błędu
                            continue
                        }
                    }
                    callback(exists)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendService", "Błąd sprawdzania zaproszeń: ${error.message}")
                    callback(false)
                }
            })
    }
    
    // Sprawdza, czy użytkownik jest już znajomym
    fun isFriend(userId: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser?.uid
        if (currentUser == null) {
            callback(false)
            return
        }
        
        // Użyj bezpiecznej metody z DatabaseHelper
        DatabaseHelper.safelyCheckFriendStatus(database.child("users").child(currentUser), userId, callback)
    }

    // Akceptacja zaproszenia do znajomych
    fun acceptFriendRequest(requestId: String, request: FriendRequest, callback: (Boolean, String) -> Unit) {
        val currentUser = auth.currentUser?.uid ?: return callback(false, "Nie jesteś zalogowany")

        // Najpierw sprawdź, czy nie są już znajomymi
        isFriend(request.senderId) { alreadyFriend ->
            if (alreadyFriend) {
                // Jeśli już są znajomymi, zaktualizuj tylko status zaproszenia
                database.child("friendRequests").child(requestId).child("status").setValue("accepted")
                    .addOnSuccessListener {
                        callback(true, "Jesteście już znajomymi")
                    }
                    .addOnFailureListener { e ->
                        callback(false, "Błąd podczas aktualizacji zaproszenia: ${e.message}")
                    }
                return@isFriend
            }

            // Aktualizuj status zaproszenia
            database.child("friendRequests").child(requestId).child("status").setValue("accepted")
                .addOnSuccessListener {
                    // Tworzymy licznik operacji, które muszą zostać zakończone
                    var operationsCompleted = 0
                    var operationsFailed = false
                    var errorMessage = ""

                    // Funkcja pomocnicza do sprawdzenia, czy wszystkie operacje zostały zakończone
                    fun checkOperationsComplete() {
                        if (operationsCompleted == 2) {
                            if (operationsFailed) {
                                callback(false, errorMessage)
                            } else {
                                callback(true, "Zaproszenie zaakceptowane")
                            }
                        }
                    }

                    // Dodaj nadawcę do listy znajomych odbiorcy (bieżący użytkownik)
                    database.child("users").child(currentUser).child("friends").child(request.senderId).setValue(true)
                        .addOnSuccessListener {
                            Log.d("FriendService", "Dodano nadawcę do listy znajomych odbiorcy")
                            operationsCompleted++
                            checkOperationsComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FriendService", "Błąd dodawania nadawcy do listy znajomych: ${e.message}")
                            operationsFailed = true
                            errorMessage = "Błąd podczas dodawania znajomego: ${e.message}"
                            operationsCompleted++
                            checkOperationsComplete()
                        }

                    // Dodaj odbiorcę do listy znajomych nadawcy
                    database.child("users").child(request.senderId).child("friends").child(currentUser).setValue(true)
                        .addOnSuccessListener {
                            Log.d("FriendService", "Dodano odbiorcę do listy znajomych nadawcy")
                            operationsCompleted++
                            checkOperationsComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FriendService", "Błąd dodawania odbiorcy do listy znajomych: ${e.message}")
                            operationsFailed = true
                            errorMessage = "Błąd podczas dodawania znajomego: ${e.message}"
                            operationsCompleted++
                            checkOperationsComplete()
                        }
                }
                .addOnFailureListener { e ->
                    callback(false, "Błąd podczas akceptowania zaproszenia: ${e.message}")
                }
        }
    }

    // Odrzucenie zaproszenia do znajomych
    fun declineFriendRequest(requestId: String, callback: (Boolean, String) -> Unit) {
        database.child("friendRequests").child(requestId).child("status").setValue("declined")
            .addOnSuccessListener {
                callback(true, "Zaproszenie odrzucone")
            }
            .addOnFailureListener { e ->
                callback(false, "Błąd podczas odrzucania zaproszenia: ${e.message}")
            }
    }

    // Usuwanie użytkownika z listy znajomych
    fun removeFriend(friendId: String, callback: (Boolean, String) -> Unit) {
        val currentUser = auth.currentUser?.uid ?: return callback(false, "Nie jesteś zalogowany")

        // Tworzymy licznik operacji, które muszą zostać zakończone
        var operationsCompleted = 0
        var operationsFailed = false
        var errorMessage = ""

        // Funkcja pomocnicza do sprawdzenia, czy wszystkie operacje zostały zakończone
        fun checkOperationsComplete() {
            if (operationsCompleted == 2) {
                if (operationsFailed) {
                    callback(false, errorMessage)
                } else {
                    callback(true, "Usunięto z listy znajomych")
                }
            }
        }

        // Usuń znajomego z listy bieżącego użytkownika
        database.child("users").child(currentUser).child("friends").child(friendId).removeValue()
            .addOnSuccessListener {
                Log.d("FriendService", "Usunięto znajomego z listy bieżącego użytkownika")
                operationsCompleted++
                checkOperationsComplete()
            }
            .addOnFailureListener { e ->
                Log.e("FriendService", "Błąd usuwania znajomego: ${e.message}")
                operationsFailed = true
                errorMessage = "Błąd podczas usuwania z listy znajomych: ${e.message}"
                operationsCompleted++
                checkOperationsComplete()
            }

        // Usuń bieżącego użytkownika z listy znajomych drugiego użytkownika
        database.child("users").child(friendId).child("friends").child(currentUser).removeValue()
            .addOnSuccessListener {
                Log.d("FriendService", "Usunięto bieżącego użytkownika z listy znajomych drugiego użytkownika")
                operationsCompleted++
                checkOperationsComplete()
            }
            .addOnFailureListener { e ->
                Log.e("FriendService", "Błąd usuwania bieżącego użytkownika: ${e.message}")
                operationsFailed = true
                errorMessage = "Błąd podczas usuwania z listy znajomych drugiego użytkownika: ${e.message}"
                operationsCompleted++
                checkOperationsComplete()
            }
    }

    // Pobieranie listy znajomych
    fun getFriends(callback: (List<User>) -> Unit) {
        val currentUser = auth.currentUser?.uid ?: return callback(emptyList())

        database.child("users").child(currentUser).child("friends")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Sprawdź, czy dane istnieją i czy nie są puste
                    if (!snapshot.exists()) {
                        Log.d("FriendService", "Brak danych o znajomych dla użytkownika $currentUser")
                        callback(emptyList())
                        return
                    }

                    // Lista, która będzie przechowywać ID znajomych
                    val friendIds = mutableListOf<String>()

                    try {
                        // Sprawdź, czy mamy do czynienia z listą czy mapą
                        val value = snapshot.getValue()
                        when (value) {
                            // Jeśli jest to lista
                            is ArrayList<*> -> {
                                Log.d("FriendService", "Znaleziono listę znajomych zamiast mapy")
                                for (item in value) {
                                    if (item is String) {
                                        friendIds.add(item)
                                    }
                                }
                            }
                            // Jeśli jest to mapa (oczekiwany przypadek)
                            is Map<*, *> -> {
                                for (friendSnapshot in snapshot.children) {
                                    val key = friendSnapshot.key
                                    if (key != null) {
                                        try {
                                            val isAccepted = friendSnapshot.getValue(Boolean::class.java) ?: false
                                            if (isAccepted) {
                                                friendIds.add(key)
                                            }
                                        } catch (e: Exception) {
                                            // W przypadku błędu konwersji, traktujemy sam klucz jako przyjaciela
                                            Log.w("FriendService", "Błąd podczas pobierania statusu znajomego: ${e.message}")
                                            friendIds.add(key)
                                        }
                                    }
                                }
                            }
                            // Nieoczekiwany typ
                            else -> {
                                Log.w("FriendService", "Nieoczekiwany typ danych znajomych: ${value?.javaClass?.name}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FriendService", "Błąd podczas przetwarzania danych znajomych: ${e.message}")
                        // Próbujemy przetworzyć bezpośrednio, zbierając klucze
                        for (friendSnapshot in snapshot.children) {
                            val key = friendSnapshot.key
                            if (key != null) {
                                friendIds.add(key)
                            }
                        }
                    }

                    if (friendIds.isEmpty()) {
                        Log.d("FriendService", "Lista znajomych jest pusta")
                        callback(emptyList())
                        return
                    }

                    val friends = mutableListOf<User>()
                    var count = 0

                    for (friendId in friendIds) {
                        // Używamy DatabaseHelper do pobierania danych użytkownika
                        DatabaseHelper.safelyGetUser(database.child("users").child(friendId)) { user ->
                            if (user != null) {
                                friends.add(user)
                            } else {
                                Log.w("FriendService", "Nie można pobrać danych użytkownika o ID: $friendId")
                            }
                            
                            count++
                            if (count == friendIds.size) {
                                callback(friends)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendService", "Błąd pobierania przyjaciół: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    // Pobieranie oczekujących zaproszeń do znajomych
    fun getPendingFriendRequests(callback: (List<Pair<String, FriendRequest>>) -> Unit) {
        val currentUser = auth.currentUser?.uid ?: return callback(emptyList())

        Log.d("FriendService", "Pobieranie zaproszeń dla użytkownika: $currentUser")
        database.child("friendRequests")
            .orderByChild("receiverId")
            .equalTo(currentUser)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = mutableListOf<Pair<String, FriendRequest>>()
                    
                    if (!snapshot.exists()) {
                        Log.d("FriendService", "Brak zaproszeń do znajomych dla użytkownika $currentUser")
                        callback(emptyList())
                        return
                    }
                    
                    Log.d("FriendService", "Znaleziono ${snapshot.childrenCount} potencjalnych zaproszeń")
                    
                    for (requestSnapshot in snapshot.children) {
                        try {
                            val request = requestSnapshot.getValue(FriendRequest::class.java)
                            if (request != null && request.status == "pending") {
                                Log.d("FriendService", "Znaleziono oczekujące zaproszenie od: ${request.senderName}")
                                requests.add(Pair(requestSnapshot.key ?: "", request))
                            }
                        } catch (e: Exception) {
                            Log.e("FriendService", "Błąd deserializacji zaproszenia: ${e.message}")
                            
                            // Próbujemy ręcznie utworzyć obiekt FriendRequest z dostępnych danych
                            try {
                                val key = requestSnapshot.key ?: ""
                                val senderId = requestSnapshot.child("senderId").getValue(String::class.java) ?: ""
                                val senderName = requestSnapshot.child("senderName").getValue(String::class.java) ?: "Nieznany użytkownik"
                                val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
                                
                                if (senderId.isNotEmpty() && status == "pending") {
                                    val manualRequest = FriendRequest(
                                        senderId = senderId,
                                        receiverId = currentUser,
                                        senderName = senderName,
                                        status = status,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    
                                    Log.d("FriendService", "Ręcznie odtworzono zaproszenie od: $senderName")
                                    requests.add(Pair(key, manualRequest))
                                }
                            } catch (e2: Exception) {
                                Log.e("FriendService", "Nie udało się ręcznie odtworzyć zaproszenia: ${e2.message}")
                                continue
                            }
                        }
                    }
                    
                    Log.d("FriendService", "Znaleziono ${requests.size} oczekujących zaproszeń")
                    callback(requests)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FriendService", "Błąd pobierania zaproszeń: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    // Wyszukiwanie użytkowników po nazwie użytkownika
    fun searchUsers(username: String, callback: (List<User>) -> Unit) {
        val currentUser = auth.currentUser?.uid ?: return callback(emptyList())
        
        // Użyj pomocy bezpiecznego wyszukiwania z DatabaseHelper
        DatabaseHelper.safelySearchUsers(database, username, currentUser, callback)
    }
} 