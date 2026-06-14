package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "database_logs")
data class DatabaseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,          // "INSERT", "UPDATE", "DELETE", "SYSTEM"
    val tableName: String,       // "tasks", "expenses", "notes", "projects", "chat_messages", "user_profile"
    val description: String,     // Human-readable description
    val timestamp: Long = System.currentTimeMillis()
)
