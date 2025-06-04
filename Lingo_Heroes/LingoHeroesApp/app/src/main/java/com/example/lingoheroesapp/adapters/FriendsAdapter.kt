package com.example.lingoheroesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.User

class FriendsAdapter(
    private var friends: List<User> = emptyList(),
    private val onRemoveFriend: (String, String) -> Unit,
    private val onViewFriendInfo: (User) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.friendUsername)
        val level: TextView = itemView.findViewById(R.id.friendLevel)
        val removeButton: Button = itemView.findViewById(R.id.removeFriendButton)
        val infoButton: Button = itemView.findViewById(R.id.infoFriendButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.username.text = friend.username
        holder.level.text = "Poziom: ${friend.level}"

        holder.removeButton.setOnClickListener {
            onRemoveFriend(friend.uid, friend.username)
        }
        
        holder.infoButton.setOnClickListener {
            onViewFriendInfo(friend)
        }
    }

    override fun getItemCount() = friends.size

    fun updateFriends(newFriends: List<User>) {
        friends = newFriends
        notifyDataSetChanged()
    }
} 