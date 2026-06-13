package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: String, // Not Started, In Progress, On Hold, Completed
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
