package com.example.lingoheroesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserSearchAdapter(
    private var users: List<User> = emptyList(),
    private val onAddFriend: (String) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val friendsMap = mutableMapOf<String, Boolean>()
    private val pendingRequestsMap = mutableMapOf<String, Boolean>()
    private val database = FirebaseDatabase.getInstance().reference

    init {
        loadCurrentUserFriends()
        loadPendingRequests()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.searchUsername)
        val level: TextView = itemView.findViewById(R.id.searchLevel)
        val addButton: Button = itemView.findViewById(R.id.addFriendButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.username.text = user.username
        holder.level.text = "Poziom: ${user.level}"

        // Sprawdź stan przycisku na podstawie relacji z użytkownikiem
        when {
            // Jeśli jest już znajomym
            friendsMap.containsKey(user.uid) && friendsMap[user.uid] == true -> {
                holder.addButton.text = "Już znajomy"
                holder.addButton.isEnabled = false
                holder.addButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    holder.addButton.context.resources.getColor(R.color.gray_dark)
                )
            }
            // Jeśli zaproszenie zostało wysłane ale nie zaakceptowane
            pendingRequestsMap.containsKey(user.uid) && pendingRequestsMap[user.uid] == true -> {
                holder.addButton.text = "Zaproszenie wysłane"
                holder.addButton.isEnabled = false
                holder.addButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    holder.addButton.context.resources.getColor(R.color.orange_500)
                )
            }
            // W przeciwnym razie pozwól dodać znajomego
            else -> {
                holder.addButton.text = "Dodaj"
                holder.addButton.isEnabled = true
                holder.addButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    holder.addButton.context.resources.getColor(R.color.green_500)
                )
                holder.addButton.setOnClickListener {
                    onAddFriend(user.uid)
                    // Zaktualizuj lokalny stan na "zaproszenie wysłane"
                    pendingRequestsMap[user.uid] = true
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    private fun loadCurrentUserFriends() {
        if (currentUserId.isNotEmpty()) {
            database.child("users").child(currentUserId).child("friends")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        friendsMap.clear()
                        for (friendSnapshot in snapshot.children) {
                            val friendId = friendSnapshot.key ?: continue
                            val isAccepted = friendSnapshot.getValue(Boolean::class.java) ?: false
                            friendsMap[friendId] = isAccepted
                        }
                        // Aktualizacja UI
                        notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Obsługa błędu
                    }
                })
        }
    }
    
    private fun loadPendingRequests() {
        if (currentUserId.isNotEmpty()) {
            database.child("friendRequests")
                .orderByChild("senderId")
                .equalTo(currentUserId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        pendingRequestsMap.clear()
                        for (requestSnapshot in snapshot.children) {
                            val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
                            val receiverId = requestSnapshot.child("receiverId").getValue(String::class.java) ?: continue
                            
                            // Tylko zaproszenia oczekujące
                            if (status == "pending") {
                                pendingRequestsMap[receiverId] = true
                            }
                        }
                        // Aktualizacja UI
                        notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Obsługa błędu
                    }
                })
        }
    }
} 