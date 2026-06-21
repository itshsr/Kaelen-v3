package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOneOff(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // Targeted column updates - avoid full-row REPLACE so concurrent profile-field
    // writes (theme, API key, habits, focus streak, etc.) can't clobber each other.
    @Query("UPDATE user_profile SET selectedTheme = :theme WHERE id = 1")
    suspend fun updateSelectedTheme(theme: String)

    @Query(
        """
        UPDATE user_profile SET
            name = :name,
            role = :role,
            city = :city,
            customGeminiApiKey = :customGeminiApiKey,
            birthDate = :birthDate,
            birthTime = :birthTime,
            birthPlace = :birthPlace
        WHERE id = 1
        """
    )
    suspend fun updateProfileDirectives(
        name: String,
        role: String,
        city: String,
        customGeminiApiKey: String,
        birthDate: String,
        birthTime: String,
        birthPlace: String
    )

    @Query("UPDATE user_profile SET monthlyGoal = :goal WHERE id = 1")
    suspend fun updateMonthlyGoal(goal: Double)

    @Query("UPDATE user_profile SET preferredFocusMinutes = :focusMin, preferredBreakMinutes = :breakMin WHERE id = 1")
    suspend fun updatePreferredTimer(focusMin: Int, breakMin: Int)

    @Query("UPDATE user_profile SET habitsJson = :json WHERE id = 1")
    suspend fun updateHabitsJson(json: String)

    @Query("UPDATE user_profile SET ebooksJson = :json WHERE id = 1")
    suspend fun updateEbooksJson(json: String)

    @Query("UPDATE user_profile SET focusStreak = :streak WHERE id = 1")
    suspend fun updateFocusStreak(streak: Int)

    @Query("UPDATE user_profile SET lastBriefingDate = :date WHERE id = 1")
    suspend fun updateLastBriefingDate(date: String)

    @Query("UPDATE user_profile SET dailyTarotDate = :date, dailyTarotCard = :card WHERE id = 1")
    suspend fun updateDailyTarot(date: String, card: String)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesOneOff(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY addedDate DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY addedDate DESC")
    suspend fun getAllTasksOneOff(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Projects
    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    suspend fun getAllProjectsOneOff(): List<Project>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    // Notes
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    suspend fun getAllNotesOneOff(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    // Database Logs
    @Query("SELECT * FROM database_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DatabaseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DatabaseLog)

    @Query("DELETE FROM database_logs")
    suspend fun clearAllLogs()

    // People Profiles
    @Query("SELECT * FROM people_profiles ORDER BY name ASC")
    fun getAllPeopleProfiles(): Flow<List<PersonProfile>>

    @Query("SELECT * FROM people_profiles ORDER BY name ASC")
    suspend fun getAllPeopleProfilesOneOff(): List<PersonProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonProfile(person: PersonProfile)

    @Delete
    suspend fun deletePersonProfile(person: PersonProfile)
}
