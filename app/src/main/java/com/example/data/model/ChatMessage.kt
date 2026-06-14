package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val sender: String, // "user", "kaelen"
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "Jarvis", // "Advise", "Critique", "Counsel" or "Jarvis"
    val imageUri: String? = null
)
