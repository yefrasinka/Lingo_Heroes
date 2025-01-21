package com.example.lingoheroesapp.activities

import android.content.Intent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.lingoheroesapp.R
import com.example.lingoheroesapp.models.Task

class TaskAdapter(
    private val context: Context,
    private var tasks: MutableList<Task>
) : BaseAdapter() {

    override fun getCount(): Int = tasks.size

    override fun getItem(position: Int): Any = tasks[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.task_item, parent, false)
        val task = tasks[position]

        val taskTitle = view.findViewById<TextView>(R.id.taskTitle)
        val taskDescription = view.findViewById<TextView>(R.id.taskDescription)

        // Wyświetlamy informacje o zadaniu
        taskTitle.text = buildTaskTitle(task)
        taskDescription.text = buildTaskDescription(task)

        // Dodajemy obsługę kliknięcia na element listy
        view.setOnClickListener {
            startTaskDisplayActivity(task)
        }

        return view
    }

    private fun buildTaskTitle(task: Task): String {
        return "Task ${task.taskId}: ${task.type.capitalize()}"
    }

    private fun buildTaskDescription(task: Task): String {
        return buildString {
            append(task.question)
            if (task.isCompleted) {
                append("\n✓ Completed")
            }
            // Możesz dodać więcej informacji o zadaniu, np.:
            append("\nReward: ${task.rewardXp}XP, ${task.rewardCoins} coins")
        }
    }

    private fun startTaskDisplayActivity(task: Task) {
        val intent = Intent(context, TaskDisplayActivity::class.java).apply {
            putExtra("CURRENT_TASK_INDEX", tasks.indexOf(task))
            putExtra("TASKS_COUNT", tasks.size)
        }
        context.startActivity(intent)
    }

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}