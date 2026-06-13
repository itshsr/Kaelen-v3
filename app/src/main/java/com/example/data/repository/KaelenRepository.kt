package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class KaelenRepository(private val appDao: AppDao) {
    // Profile
    val userProfile: Flow<UserProfile?> = appDao.getUserProfile()
    suspend fun getUserProfileOneOff(): UserProfile? = appDao.getUserProfileOneOff()
    suspend fun insertUserProfile(profile: UserProfile) = appDao.insertUserProfile(profile)

    // Expenses
    val allExpenses: Flow<List<Expense>> = appDao.getAllExpenses()
    suspend fun getAllExpensesOneOff(): List<Expense> = appDao.getAllExpensesOneOff()
    suspend fun insertExpense(expense: Expense) = appDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = appDao.deleteExpense(expense)

    // Tasks
    val allTasks: Flow<List<Task>> = appDao.getAllTasks()
    suspend fun getAllTasksOneOff(): List<Task> = appDao.getAllTasksOneOff()
    suspend fun insertTask(task: Task) = appDao.insertTask(task)
    suspend fun deleteTask(task: Task) = appDao.deleteTask(task)

    // Projects
    val allProjects: Flow<List<Project>> = appDao.getAllProjects()
    suspend fun getAllProjectsOneOff(): List<Project> = appDao.getAllProjectsOneOff()
    suspend fun insertProject(project: Project) = appDao.insertProject(project)
    suspend fun deleteProject(project: Project) = appDao.deleteProject(project)

    // Notes
    val allNotes: Flow<List<Note>> = appDao.getAllNotes()
    suspend fun getAllNotesOneOff(): List<Note> = appDao.getAllNotesOneOff()
    suspend fun insertNote(note: Note) = appDao.insertNote(note)
    suspend fun deleteNote(note: Note) = appDao.deleteNote(note)

    // Chat
    val allChatMessages: Flow<List<ChatMessage>> = appDao.getAllChatMessages()
    suspend fun insertChatMessage(message: ChatMessage) = appDao.insertChatMessage(message)
    suspend fun clearChatHistory() = appDao.clearChatHistory()
}
