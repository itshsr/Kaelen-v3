package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String, // Food, Transport, Shopping, Bills, Health, Other
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
