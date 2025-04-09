package com.example.lingoheroesapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lingoheroesapp.R

class MistakesAdapter(private val mistakes: List<String>) : 
    RecyclerView.Adapter<MistakesAdapter.MistakeViewHolder>() {

    class MistakeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mistakeText: TextView = view.findViewById(R.id.mistakeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MistakeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mistake, parent, false)
        return MistakeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MistakeViewHolder, position: Int) {
        holder.mistakeText.text = mistakes[position]
    }

    override fun getItemCount() = mistakes.size
} 