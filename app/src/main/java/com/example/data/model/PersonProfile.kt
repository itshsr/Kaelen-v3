package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people_profiles")
data class PersonProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val relationship: String,
    val dateOfBirth: String = "", // YYYY-MM-DD
    val birthTime: String = "", // HH:MM
    val birthPlace: String = "",
    val photoEmoji: String = "👤",
    val city: String = "",
    val notes: String = "",
    val encryptedPin: String = "", // Non-empty means locked
    val isPinLocked: Boolean = false,
    val tarotCard: String = "",
    val tarotReading: String = "",
    val dailyHoroscope: String = "",
    val kundliAscendant: String = "Saggitarius",
    val kundliRasi: String = "Leo",
    val kundliNakshatra: String = "Purva Phalguni"
)
