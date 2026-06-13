package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String? = null,
    val isCompleted: Boolean = false,
    val addedDate: Long = System.currentTimeMillis(),
    val completedDate: Long? = null
)
