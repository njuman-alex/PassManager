package com.c0dex.passmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.c0dex.passmanager.data.PasswordItem

class PasswordListAdapter(
    private val onItemClick: (PasswordItem) -> Unit
) : RecyclerView.Adapter<PasswordListAdapter.ViewHolder>() {

    // Оригинальный список (все пароли)
    private var originalList: List<PasswordItem> = emptyList()
    // Отфильтрованный список
    private var filteredList: List<PasswordItem> = emptyList()

    // 👇 ДОБАВЛЕНО: безопасные геттеры (каноничный способ в Kotlin)
    val originalCount: Int
        get() = originalList.size

    val filteredCount: Int
        get() = filteredList.size

    /**
     * Обновляет полный список паролей (вызывается из Activity)
     */
    fun updatePasswords(newPasswords: List<PasswordItem>) {
        originalList = newPasswords
        filteredList = newPasswords
        notifyDataSetChanged()
    }

    /**
     * Фильтрует список по поисковому запросу
     */
    fun filter(query: String) {
        val normalizedQuery = query.trim().lowercase()
        filteredList = if (normalizedQuery.isEmpty()) {
            originalList
        } else {
            originalList.filter { item ->
                item.description.lowercase().contains(normalizedQuery)
            }
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)

        fun bind(item: PasswordItem) {
            descriptionText.text = item.description
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size
}