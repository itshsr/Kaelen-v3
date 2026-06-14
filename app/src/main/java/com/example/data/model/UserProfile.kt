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
    val briefingEnabled: Boolean = true,
    val customGeminiApiKey: String = "",
    val birthDate: String = "1995-11-20",
    val birthTime: String = "08:15",
    val birthPlace: String = "New Delhi, India",
    val dailyTarotCard: String = "",
    val dailyTarotDate: String = "",
    val lastBriefingDate: String = "",
    val selectedTheme: String = "INFERNO",
    val monthlyGoal: Double = 50000.0,
    val focusStreak: Int = 0,
    val habitsJson: String = "",
    val ebooksJson: String = "",
    val preferredFocusMinutes: Int = 25,
    val preferredBreakMinutes: Int = 5
)
