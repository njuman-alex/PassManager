package com.c0dex.passmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.c0dex.passmanager.data.PasswordItem

class PasswordListAdapter(
    private var passwords: List<PasswordItem> = emptyList(),
    private val onItemClick: (PasswordItem) -> Unit
) : RecyclerView.Adapter<PasswordListAdapter.ViewHolder>() {

    fun updatePasswords(newPasswords: List<PasswordItem>) {
        passwords = newPasswords
        notifyDataSetChanged()
    }



    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)

        fun bind(item: PasswordItem) {
            descriptionText.text = item.description

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(passwords[position])
    }

    override fun getItemCount() = passwords.size
}