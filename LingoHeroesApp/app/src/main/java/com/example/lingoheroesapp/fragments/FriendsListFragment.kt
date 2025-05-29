package com.example.lingoheroesapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.activities.UserSearchActivity
import com.example.lingoheroesapp.activities.UserProfileActivity
import com.example.lingoheroesapp.adapters.FriendsAdapter
import com.example.lingoheroesapp.models.User
import com.example.lingoheroesapp.services.FriendService
import com.google.android.material.snackbar.Snackbar

class FriendsListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var findFriendsButton: Button
    private lateinit var adapter: FriendsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friends_list, container, false)

        recyclerView = view.findViewById(R.id.friendsRecyclerView)
        emptyView = view.findViewById(R.id.emptyFriendsView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        findFriendsButton = view.findViewById(R.id.findFriendsButton)

        setupRecyclerView()
        setupButtons()
        loadFriends()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadFriends()
    }

    private fun setupRecyclerView() {
        adapter = FriendsAdapter(
            onRemoveFriend = { friendId, friendName ->
                showRemoveConfirmationDialog(friendId, friendName)
            },
            onViewFriendInfo = { friend ->
                showFriendInfo(friend)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        findFriendsButton.setOnClickListener {
            startActivity(Intent(requireContext(), UserSearchActivity::class.java))
        }
    }

    private fun loadFriends() {
        showLoading(true)

        FriendService.getFriends { friends ->
            activity?.runOnUiThread {
                showLoading(false)
                updateUI(friends)
            }
        }
    }

    private fun updateUI(friends: List<User>) {
        if (friends.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.updateFriends(friends)
        }
    }
    
    private fun showRemoveConfirmationDialog(friendId: String, friendName: String) {
        context?.let {
            AlertDialog.Builder(it)
                .setTitle("Usuwanie znajomego")
                .setMessage("Czy na pewno chcesz usunąć $friendName z listy znajomych?")
                .setPositiveButton("Usuń") { _, _ ->
                    removeFriend(friendId)
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }

    private fun removeFriend(friendId: String) {
        showLoading(true)

        FriendService.removeFriend(friendId) { success, message ->
            activity?.runOnUiThread {
                showLoading(false)
                if (success) {
                    Snackbar.make(recyclerView, "Usunięto z listy znajomych", Snackbar.LENGTH_SHORT).show()
                    loadFriends()
                } else {
                    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showFriendInfo(friend: User) {
        val intent = Intent(requireContext(), UserProfileActivity::class.java)
        intent.putExtra("USER_ID", friend.uid)
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }
} 