package com.example.lingoheroesapp.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.User
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RankingActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rankingAdapter: RankingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        initializeFirebase()
        setupUI()
        loadRankingData()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
    }

    private fun setupUI() {
        // Ustawienie przycisku powrotu
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Inicjalizacja RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rankingRecyclerView)
        rankingAdapter = RankingAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RankingActivity)
            adapter = rankingAdapter
        }
    }

    private fun loadRankingData() {
        // Najpierw pobierz dane aktualnego użytkownika
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            database.child("users").child(currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            updateUserRankCard(user)
                            loadTop100Users()
                        } else {
                            showError("Nie można załadować danych użytkownika")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Błąd podczas ładowania danych: ${error.message}")
                    }
                })
        } else {
            showError("Użytkownik nie jest zalogowany")
            finish()
        }
    }

    private fun updateUserRankCard(user: User) {
        try {
            findViewById<TextView>(R.id.userNameText).text = user.username
            findViewById<TextView>(R.id.userXpText).text = "${user.xp} XP"
        } catch (e: Exception) {
            showError("Błąd podczas aktualizacji karty użytkownika")
        }
    }

    private fun loadTop100Users() {
        database.child("users")
            .orderByChild("xp")
            .limitToLast(100)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val users = mutableListOf<User>()
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                users.add(user)
                            }
                        }
                        
                        if (users.isEmpty()) {
                            showError("Brak danych w rankingu")
                            return
                        }

                        // Sortuj użytkowników malejąco według XP
                        users.sortByDescending { it.xp }
                        
                        // Znajdź pozycję aktualnego użytkownika
                        val currentUserRank = users.indexOfFirst { it.uid == auth.currentUser?.uid } + 1
                        findViewById<TextView>(R.id.userRankText).text = 
                            if (currentUserRank > 0) "#$currentUserRank" else "Poza rankingiem"
                        
                        // Aktualizuj listę
                        rankingAdapter.submitList(users)
                    } catch (e: Exception) {
                        showError("Błąd podczas przetwarzania danych rankingu")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Błąd podczas ładowania rankingu: ${error.message}")
                }
            })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

class RankingAdapter : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {
    private var users: List<User> = emptyList()

    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user, position + 1)
    }

    override fun getItemCount() = users.size

    class RankingViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val rankNumberText: TextView = itemView.findViewById(R.id.rankNumberText)
        private val userAvatar: ShapeableImageView = itemView.findViewById(R.id.userAvatar)
        private val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        private val xpText: TextView = itemView.findViewById(R.id.xpText)

        fun bind(user: User, position: Int) {
            rankNumberText.text = "#$position"
            usernameText.text = user.username
            xpText.text = "${user.xp} XP"
        }
    }
} 