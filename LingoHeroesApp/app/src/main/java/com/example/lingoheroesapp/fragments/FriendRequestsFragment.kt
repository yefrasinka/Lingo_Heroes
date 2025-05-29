package com.example.lingoheroesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.FriendRequestsAdapter
import com.example.lingoheroesapp.models.FriendRequest
import com.example.lingoheroesapp.services.FriendService
import com.google.android.material.snackbar.Snackbar

class FriendRequestsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_requests, container, false)

        recyclerView = view.findViewById(R.id.requestsRecyclerView)
        emptyView = view.findViewById(R.id.emptyRequestsView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        setupRecyclerView()
        loadRequests()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
    }

    private fun setupRecyclerView() {
        adapter = FriendRequestsAdapter(
            onAccept = { requestId, request ->
                acceptRequest(requestId, request)
            },
            onReject = { requestId ->
                rejectRequest(requestId)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadRequests() {
        showLoading(true)

        FriendService.getPendingFriendRequests { requests ->
            activity?.runOnUiThread {
                showLoading(false)
                updateUI(requests)
            }
        }
    }

    private fun updateUI(requests: List<Pair<String, FriendRequest>>) {
        if (requests.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.updateRequests(requests)
        }
    }

    private fun acceptRequest(requestId: String, request: FriendRequest) {
        showLoading(true)

        FriendService.acceptFriendRequest(requestId, request) { success, message ->
            activity?.runOnUiThread {
                showLoading(false)
                if (success) {
                    Snackbar.make(recyclerView, "Zaproszenie zaakceptowane", Snackbar.LENGTH_SHORT).show()
                    loadRequests()
                } else {
                    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rejectRequest(requestId: String) {
        showLoading(true)

        FriendService.declineFriendRequest(requestId) { success, message ->
            activity?.runOnUiThread {
                showLoading(false)
                if (success) {
                    Snackbar.make(recyclerView, "Zaproszenie odrzucone", Snackbar.LENGTH_SHORT).show()
                    loadRequests()
                } else {
                    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }
} 