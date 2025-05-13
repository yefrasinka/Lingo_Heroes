package com.example.lingoheroesapp.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.adapters.UserSearchAdapter
import com.example.lingoheroesapp.services.FriendService
import com.google.android.material.snackbar.Snackbar

class UserSearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptySearchView: LinearLayout
    private lateinit var noResultsView: LinearLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var adapter: UserSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_search)

        initializeViews()
        setupRecyclerView()
        setupListeners()
    }

    private fun initializeViews() {
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchButton = findViewById(R.id.clearSearchButton)
        recyclerView = findViewById(R.id.searchResultsRecyclerView)
        emptySearchView = findViewById(R.id.emptySearchView)
        noResultsView = findViewById(R.id.noResultsView)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = UserSearchAdapter { userId ->
            sendFriendRequest(userId)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearSearchButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                if (s?.length ?: 0 >= 3) {
                    searchUsers(s.toString())
                } else {
                    showEmptyState()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString()
                if (query.length >= 3) {
                    searchUsers(query)
                }
                return@setOnEditorActionListener true
            }
            false
        }

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            showEmptyState()
        }
    }

    private fun searchUsers(query: String) {
        showLoading(true)

        FriendService.searchUsers(query) { users ->
            runOnUiThread {
                showLoading(false)
                if (users.isEmpty()) {
                    showNoResults()
                } else {
                    showResults(users)
                }
            }
        }
    }

    private fun sendFriendRequest(userId: String) {
        showLoading(true)

        FriendService.sendFriendRequest(userId) { success, message ->
            runOnUiThread {
                showLoading(false)
                Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
                
                // Odśwież wyniki wyszukiwania
                val query = searchEditText.text.toString()
                if (query.length >= 3) {
                    searchUsers(query)
                }
            }
        }
    }

    private fun showResults(users: List<com.example.lingoheroesapp.models.User>) {
        emptySearchView.visibility = View.GONE
        noResultsView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        adapter.updateUsers(users)
    }

    private fun showNoResults() {
        emptySearchView.visibility = View.GONE
        noResultsView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showEmptyState() {
        emptySearchView.visibility = View.VISIBLE
        noResultsView.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            emptySearchView.visibility = View.GONE
            noResultsView.visibility = View.GONE
        }
    }
} 