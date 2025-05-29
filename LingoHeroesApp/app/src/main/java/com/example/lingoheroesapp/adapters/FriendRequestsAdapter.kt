package com.example.lingoheroesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.FriendRequest

class FriendRequestsAdapter(
    private var requests: List<Pair<String, FriendRequest>> = emptyList(),
    private val onAccept: (String, FriendRequest) -> Unit,
    private val onReject: (String) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.requestUsername)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val rejectButton: Button = itemView.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val (requestId, request) = requests[position]
        holder.username.text = request.senderName

        holder.acceptButton.setOnClickListener {
            onAccept(requestId, request)
        }

        holder.rejectButton.setOnClickListener {
            onReject(requestId)
        }
    }

    override fun getItemCount() = requests.size

    fun updateRequests(newRequests: List<Pair<String, FriendRequest>>) {
        requests = newRequests
        notifyDataSetChanged()
    }
} 