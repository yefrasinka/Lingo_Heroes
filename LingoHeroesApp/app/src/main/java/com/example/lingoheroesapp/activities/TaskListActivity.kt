package com.example.lingoheroesapp.activities

import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Task
import com.google.firebase.database.*

class TaskListActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var tasksListView: ListView
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        tasksListView = findViewById(R.id.tasksListView)
        taskAdapter = TaskAdapter(this, mutableListOf())
        tasksListView.adapter = taskAdapter

        val subtopicId = intent.getStringExtra("SUBTOPIC_ID")
        if (subtopicId != null) {
            loadTasksForSubtopic(subtopicId)
        } else {
            Toast.makeText(this, "No Subtopic ID found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTasksForSubtopic(subtopicId: String) {
        // Zmieniamy ścieżkę, aby odpowiadała strukturze w Firebase
        database.child("topics").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()

                // Przeszukujemy wszystkie topic'i
                for (topicSnapshot in snapshot.children) {
                    // Szukamy w subtopics
                    val subtopicsSnapshot = topicSnapshot.child("subtopics")
                    for (subtopicSnapshot in subtopicsSnapshot.children) {
                        // Sprawdzamy, czy to jest szukany subtopic
                        val currentSubtopicId = subtopicSnapshot.child("id").getValue(String::class.java)
                        if (currentSubtopicId == subtopicId) {
                            // Pobieramy tasks
                            val tasksSnapshot = subtopicSnapshot.child("task")
                            tasksSnapshot.children.forEach { taskSnapshot ->
                                try {
                                    val task = Task(
                                        taskId = taskSnapshot.child("taskId").getValue(String::class.java) ?: "",
                                        description = taskSnapshot.child("description").getValue(String::class.java) ?: "",
                                        type = taskSnapshot.child("type").getValue(String::class.java) ?: "",
                                        question = taskSnapshot.child("question").getValue(String::class.java) ?: "",
                                        options = taskSnapshot.child("options").getValue<List<String>>() ?: emptyList(),
                                        correctAnswer = taskSnapshot.child("correctAnswer").getValue(String::class.java) ?: "",
                                        rewardXp = taskSnapshot.child("rewardXp").getValue(Int::class.java) ?: 0,
                                        rewardCoins = taskSnapshot.child("rewardCoins").getValue(Int::class.java) ?: 0,
                                        isCompleted = taskSnapshot.child("iscompleted").getValue(Boolean::class.java) ?: false
                                    )
                                    tasks.add(task)
                                } catch (e: Exception) {
                                    println("Error parsing task: ${e.message}")
                                }
                            }
                            break
                        }
                    }
                }

                println("Znalezione zadania: ${tasks.size}")
                if (tasks.isEmpty()) {
                    Toast.makeText(this@TaskListActivity, "No tasks found", Toast.LENGTH_SHORT).show()
                }
                taskAdapter.updateTasks(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TaskListActivity, "Failed to load tasks: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
