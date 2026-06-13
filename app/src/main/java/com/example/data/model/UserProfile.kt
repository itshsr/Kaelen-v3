package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Harmeet",
    val role: String = "Lead Developer",
    val city: String = "Delhi",
    val currentProjects: String = "Quantum AI, Kaelen Core",
    val preferences: String = "Witty, structured guidance, deeply analytical. Prefers high contrast visual layouts.",
    val briefingHour: Int = 8,
    val briefingMinute: Int = 30,
    val briefingEnabled: Boolean = true
)
