package com.c0dex.passmanager

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.c0dex.passmanager.data.PasswordItem

class PasswordListAdapter(
    private val onItemClick: (PasswordItem) -> Unit
) : RecyclerView.Adapter<PasswordListAdapter.ViewHolder>() {

    // Оригинальный список (все пароли)
    private var originalList: List<PasswordItem> = emptyList()
    // Отфильтрованный список
    private var filteredList: List<PasswordItem> = emptyList()
    // 👇 ДОБАВЛЕНО: храним текущий поисковый запрос для подсветки
    private var currentSearchQuery: String = ""

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
        currentSearchQuery = "" // 👈 сброс запроса при полной перезагрузке
        notifyDataSetChanged()
    }

    /**
     * Фильтрует список по поисковому запросу
     */
    fun filter(query: String) {
        currentSearchQuery = query.trim() // 👈 сохраняем запрос для подсветки
        val normalizedQuery = currentSearchQuery.lowercase()
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

        // 👇 ИСПРАВЛЕНО: добавлен параметр searchQuery для подсветки
        fun bind(item: PasswordItem, searchQuery: String) {
            if (searchQuery.isEmpty()) {
                descriptionText.text = item.description
            } else {
                // 👇 ДОБАВЛЕНО: подсветка точного совпадения подстроки
                descriptionText.text = highlightText(item.description, searchQuery)
            }
            itemView.setOnClickListener { onItemClick(item) }
        }

        // 👇 ДОБАВЛЕНО: функция подсветки точного совпадения (регистронезависимо)
        private fun highlightText(text: String, query: String): SpannableString {
            val spannable = SpannableString(text)
            if (query.isEmpty()) return spannable

            val queryLower = query.lowercase()
            val textLower = text.lowercase()
            // 👇 ИСПОЛЬЗУЕМ ЦВЕТ ИЗ РЕСУРСОВ (создай res/values/colors.xml)
            val highlightColor = ContextCompat.getColor(itemView.context, R.color.search_highlight)

            var startIndex = textLower.indexOf(queryLower)
            while (startIndex >= 0) {
                val endIndex = startIndex + query.length
                spannable.setSpan(
                    ForegroundColorSpan(highlightColor),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 👇 ИЩЕМ СЛЕДУЮЩЕЕ ВХОЖДЕНИЕ (важно: +1, а не +query.length)
                startIndex = textLower.indexOf(queryLower, startIndex + 1)
            }
            return spannable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password_simple, parent, false)
        return ViewHolder(view)
    }

    // 👇 ИСПРАВЛЕНО: передаём currentSearchQuery в bind()
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredList[position], currentSearchQuery)
    }

    override fun getItemCount(): Int = filteredList.size
}