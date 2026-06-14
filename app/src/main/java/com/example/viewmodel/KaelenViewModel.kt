package com.example.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.KaelenDatabase
import com.example.data.model.*
import com.example.data.repository.KaelenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    HOME, CORE, FORGE, ORACLE, GRIMOIRE, VAULT, USER
}

sealed class PendingAction {
    data class AddExpense(val amount: Double, val category: String, val note: String) : PendingAction()
    data class DeleteExpense(val expense: Expense) : PendingAction()
    data class AddTask(val title: String, val note: String?) : PendingAction()
    data class DeleteTask(val task: Task) : PendingAction()
    data class ToggleTaskComplete(val task: Task, val completed: Boolean) : PendingAction()
    data class AddProject(val name: String, val status: String, val note: String?) : PendingAction()
    data class UpdateProjectStatus(val project: Project, val newStatus: String) : PendingAction()
    data class DeleteProject(val project: Project) : PendingAction()
    data class AddNote(val title: String, val content: String) : PendingAction()
    data class DeleteNote(val note: Note) : PendingAction()
    data class UpdateProfile(val profile: UserProfile) : PendingAction()
    data class SaveVoiceSuggestion(val type: String, val title: String, val body: String, val extra: String? = null) : PendingAction()
    object ClearChat : PendingAction()
}

data class VoiceSuggestion(
    val type: String, // "Note", "Task", "Project"
    val title: String,
    val body: String,
    val extra: String? = null // status for project, etc.
)

data class Habit(
    val name: String,
    val completedDates: Set<String>, // "YYYY-MM-DD"
    val streak: Int = 0
)

data class Ebook(
    val title: String,
    val format: String = "EPUB",
    val progress: Int = 0, // percentage 0-100
    val lastReadPosition: Int = 1,
    val totalPages: Int = 300,
    val bookmarks: List<Int> = emptyList(),
    val highlights: List<String> = emptyList(),
    val author: String = "Unknown Author",
    val coverIcon: String = "📖"
)

class KaelenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KaelenRepository = KaelenRepository(KaelenDatabase.getDatabase(application).appDao)
    private val prefs: SharedPreferences = application.getSharedPreferences("kaelen_shared_prefs", Context.MODE_PRIVATE)

    // UI Navigation State - default landing screen is HOME as requested
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Live Workspace Data Flows
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val databaseLogs: StateFlow<List<DatabaseLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Habits Flow
    private val _habitsList = MutableStateFlow<List<Habit>>(emptyList())
    val habitsList: StateFlow<List<Habit>> = _habitsList.asStateFlow()

    // Focus sessions Flow
    private val _focusStreak = MutableStateFlow(0)
    val focusStreak: StateFlow<Int> = _focusStreak.asStateFlow()

    // Ebooks Flow
    private val _ebooksList = MutableStateFlow<List<Ebook>>(emptyList())
    val ebooksList: StateFlow<List<Ebook>> = _ebooksList.asStateFlow()

    // Budget Configuration
    private val _monthlyGoal = MutableStateFlow(50000.0) // default 50K
    val monthlyGoal: StateFlow<Double> = _monthlyGoal.asStateFlow()

    fun updateMonthlyGoal(goal: Double) {
        _monthlyGoal.value = goal
        prefs.edit().putFloat("monthly_goal", goal.toFloat()).apply()
    }

    // Confirmation Screen State (legacy, direct execution preferred to avoid broken data saving)
    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    fun dismissAction() {
        _pendingAction.value = null
    }

    init {
        _monthlyGoal.value = prefs.getFloat("monthly_goal", 50000.0f).toDouble()
        initializeProfileIfNeeded()
        loadHabitsFromPrefs()
        loadFocusStreakFromPrefs()
        loadEbooksFromPrefs()
        insertDefaultKaelenGreeting()
    }

    // Direct, Immediate Persistence Execution (Robust & failsafe)
    fun requestAction(action: PendingAction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (action) {
                    is PendingAction.AddExpense -> {
                        val currentList = expenses.value
                        val newTotal = currentList.sumOf { it.amount } + action.amount
                        val goal = monthlyGoal.value
                        
                        repository.insertExpense(
                            Expense(amount = action.amount, category = action.category, note = action.note)
                        )
                        repository.insertLog(
                            DatabaseLog(
                                action = "INSERT",
                                tableName = "expenses",
                                description = "Logged expense of ₹${action.amount} under system category '${action.category}'"
                            )
                        )
                        
                        // Check 80% Alert threshold
                        if (newTotal >= goal * 0.8 && currentList.sumOf { it.amount } < goal * 0.8) {
                            sendLocalNotification(
                                "Budget Warning",
                                "Harmeet, your spending has reached ${(newTotal / goal * 100).toInt()}% of your monthly goal ($goal)."
                            )
                        }
                    }
                    is PendingAction.DeleteExpense -> {
                        repository.deleteExpense(action.expense)
                        repository.insertLog(
                            DatabaseLog(
                                action = "DELETE",
                                tableName = "expenses",
                                description = "Deleted expense entry: ₹${action.expense.amount} under '${action.expense.category}'"
                            )
                        )
                    }
                    is PendingAction.AddTask -> {
                        repository.insertTask(Task(title = action.title, note = action.note))
                        repository.insertLog(
                            DatabaseLog(
                                action = "INSERT",
                                tableName = "tasks",
                                description = "Created task: '${action.title}'"
                            )
                        )
                    }
                    is PendingAction.DeleteTask -> {
                        repository.deleteTask(action.task)
                        repository.insertLog(
                            DatabaseLog(
                                action = "DELETE",
                                tableName = "tasks",
                                description = "Removed task: '${action.task.title}'"
                            )
                        )
                    }
                    is PendingAction.ToggleTaskComplete -> {
                        val updated = action.task.copy(
                            isCompleted = action.completed,
                            completedDate = if (action.completed) System.currentTimeMillis() else null
                        )
                        repository.insertTask(updated)
                        val statusText = if (action.completed) "Completed" else "Marked Active"
                        repository.insertLog(
                            DatabaseLog(
                                action = "UPDATE",
                                tableName = "tasks",
                                description = "$statusText task: '${action.task.title}'"
                            )
                        )
                    }
                    is PendingAction.AddProject -> {
                        repository.insertProject(Project(name = action.name, status = action.status, note = action.note))
                        repository.insertLog(
                            DatabaseLog(
                                action = "INSERT",
                                tableName = "projects",
                                description = "Initiated project tracker: '${action.name}' (Status: ${action.status})"
                            )
                        )
                    }
                    is PendingAction.UpdateProjectStatus -> {
                        val updated = action.project.copy(status = action.newStatus)
                        repository.insertProject(updated)
                        repository.insertLog(
                            DatabaseLog(
                                action = "UPDATE",
                                tableName = "projects",
                                description = "Transitioned status of '${action.project.name}' to '${action.newStatus}'"
                            )
                        )
                    }
                    is PendingAction.DeleteProject -> {
                        repository.deleteProject(action.project)
                        repository.insertLog(
                            DatabaseLog(
                                action = "DELETE",
                                tableName = "projects",
                                description = "Terminated project tracker: '${action.project.name}'"
                            )
                        )
                    }
                    is PendingAction.AddNote -> {
                        repository.insertNote(Note(title = action.title, content = action.content))
                        repository.insertLog(
                            DatabaseLog(
                                action = "INSERT",
                                tableName = "notes",
                                description = "Saved repository intel note: '${action.title}'"
                            )
                        )
                    }
                    is PendingAction.DeleteNote -> {
                        repository.deleteNote(action.note)
                        repository.insertLog(
                            DatabaseLog(
                                action = "DELETE",
                                tableName = "notes",
                                description = "Purged intel note: '${action.note.title}'"
                            )
                        )
                    }
                    is PendingAction.UpdateProfile -> {
                        repository.insertUserProfile(action.profile)
                        repository.insertLog(
                            DatabaseLog(
                                action = "UPDATE",
                                tableName = "user_profile",
                                description = "Updated profile directives and system briefings configuration"
                            )
                        )
                        val context = getApplication<Application>().applicationContext
                        if (action.profile.briefingEnabled) {
                            com.example.scheduler.BriefingScheduler.scheduleDailyBriefing(
                                context,
                                action.profile.briefingHour,
                                action.profile.briefingMinute
                            )
                        } else {
                            com.example.scheduler.BriefingScheduler.cancelDailyBriefing(context)
                        }
                    }
                    is PendingAction.SaveVoiceSuggestion -> {
                        val table = when (action.type.lowercase().trim()) {
                            "note" -> {
                                repository.insertNote(Note(title = action.title, content = action.body))
                                "notes"
                            }
                            "task" -> {
                                repository.insertTask(Task(title = action.title, note = action.body))
                                "tasks"
                            }
                            else -> {
                                repository.insertProject(Project(name = action.title, status = action.extra ?: "In Progress", note = action.body))
                                "projects"
                            }
                        }
                        repository.insertLog(
                            DatabaseLog(
                                action = "INSERT",
                                tableName = table,
                                description = "Cataloged voice suggestion [${action.type}] - '${action.title}'"
                            )
                        )
                        _voiceSuggestions.update { list ->
                            list.filterNot { it.title == action.title && it.body == action.body }
                        }
                    }
                    is PendingAction.ClearChat -> {
                        repository.clearChatHistory()
                        repository.insertLog(
                            DatabaseLog(
                                action = "DELETE",
                                tableName = "chat_messages",
                                description = "Cleared chat intelligence log cache"
                            )
                        )
                        insertDefaultKaelenGreeting()
                    }
                }
            } catch (e: Exception) {
                Log.e("KaelenViewModel", "Error in pending action execution: ${e.message}")
            }
        }
    }

    private fun loadHabitsFromPrefs() {
        val habitsJson = prefs.getString("habits_json", null)
        if (habitsJson != null) {
            try {
                val array = JSONArray(habitsJson)
                val list = mutableListOf<Habit>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.getString("name")
                    val streak = obj.optInt("streak", 0)
                    val datesArr = obj.getJSONArray("dates")
                    val dates = mutableSetOf<String>()
                    for (j in 0 until datesArr.length()) {
                        dates.add(datesArr.getString(j))
                    }
                    list.add(Habit(name, dates, streak))
                }
                _habitsList.value = list
            } catch (e: Exception) {
                Log.e("KaelenViewModel", "Error loading habits", e)
            }
        } else {
            val defaults = listOf(
                Habit("Daily Meditation", emptySet(), 0),
                Habit("Forge Session (Focus)", emptySet(), 0),
                Habit("Ebook Reading Progress", emptySet(), 0)
            )
            _habitsList.value = defaults
            saveHabitsToPrefs(defaults)
        }
    }

    private fun saveHabitsToPrefs(list: List<Habit>) {
        val array = JSONArray()
        list.forEach { h ->
            val obj = JSONObject()
            obj.put("name", h.name)
            obj.put("streak", h.streak)
            val datesArr = JSONArray()
            h.completedDates.forEach { d -> datesArr.put(d) }
            obj.put("dates", datesArr)
            array.put(obj)
        }
        prefs.edit().putString("habits_json", array.toString()).apply()
    }

    fun addHabit(name: String) {
        val newList = _habitsList.value.toMutableList()
        if (newList.none { it.name.lowercase() == name.lowercase() }) {
            newList.add(Habit(name, emptySet(), 0))
            _habitsList.value = newList
            saveHabitsToPrefs(newList)
        }
    }

    fun toggleHabit(name: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val newList = _habitsList.value.map { h ->
            if (h.name == name) {
                val updatedDates = h.completedDates.toMutableSet()
                var currentStreak = h.streak
                if (updatedDates.contains(todayStr)) {
                    updatedDates.remove(todayStr)
                    currentStreak = maxOf(0, currentStreak - 1)
                } else {
                    updatedDates.add(todayStr)
                    currentStreak += 1
                }
                h.copy(completedDates = updatedDates, streak = currentStreak)
            } else h
        }
        _habitsList.value = newList
        saveHabitsToPrefs(newList)
    }

    fun deleteHabit(name: String) {
        val newList = _habitsList.value.filterNot { it.name == name }
        _habitsList.value = newList
        saveHabitsToPrefs(newList)
    }

    private fun loadFocusStreakFromPrefs() {
        _focusStreak.value = prefs.getInt("focus_streak", 0)
    }

    fun logFocusSession() {
        val streak = _focusStreak.value + 1
        _focusStreak.value = streak
        prefs.edit().putInt("focus_streak", streak).apply()
        
        // Mark Forge Focus habit completed
        toggleHabit("Forge Session (Focus)")
    }

    private fun loadEbooksFromPrefs() {
        val booksJson = prefs.getString("books_json", null)
        if (booksJson != null) {
            try {
                val array = JSONArray(booksJson)
                val list = mutableListOf<Ebook>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val bmarks = mutableListOf<Int>()
                    val bmarksArr = obj.getJSONArray("bookmarks")
                    for (j in 0 until bmarksArr.length()) bmarks.add(bmarksArr.getInt(j))
                    val hlights = mutableListOf<String>()
                    val hlightsArr = obj.getJSONArray("highlights")
                    for (j in 0 until hlightsArr.length()) hlights.add(hlightsArr.getString(j))
                    
                    list.add(
                        Ebook(
                            title = obj.getString("title"),
                            format = obj.getString("format"),
                            progress = obj.getInt("progress"),
                            lastReadPosition = obj.getInt("lastRead"),
                            totalPages = obj.getInt("totalPages"),
                            bookmarks = bmarks,
                            highlights = hlights,
                            author = obj.optString("author", "Unknown"),
                            coverIcon = obj.optString("coverIcon", "📖")
                        )
                    )
                }
                _ebooksList.value = list
            } catch (e: Exception) {
                Log.e("KaelenViewModel", "Error loading ebooks", e)
            }
        } else {
            val defaults = listOf(
                Ebook("The Mystical Arts & Vastu Alignment", "PDF", 12, 36, 300, listOf(12, 24), listOf("Vastu aligns energy block-to-block."), "BASIM", "🏰"),
                Ebook("The Art of Cold Steel Strategies", "EPUB", 45, 135, 300, emptyList(), listOf("Cold execution defeats raw fire."), "VERGIL", "⚔️"),
                Ebook("Shinobi Information Delivery Manual", "PDF", 0, 1, 150, emptyList(), emptyList(), "KAKASHI", "📜")
            )
            _ebooksList.value = defaults
            saveEbooksToPrefs(defaults)
        }
    }

    private fun saveEbooksToPrefs(list: List<Ebook>) {
        val array = JSONArray()
        list.forEach { b ->
            val obj = JSONObject()
            obj.put("title", b.title)
            obj.put("format", b.format)
            obj.put("progress", b.progress)
            obj.put("lastRead", b.lastReadPosition)
            obj.put("totalPages", b.totalPages)
            obj.put("author", b.author)
            obj.put("coverIcon", b.coverIcon)
            
            val bmArr = JSONArray()
            b.bookmarks.forEach { bmArr.put(it) }
            obj.put("bookmarks", bmArr)
            
            val hlArr = JSONArray()
            b.highlights.forEach { hlArr.put(it) }
            obj.put("highlights", hlArr)
            
            array.put(obj)
        }
        prefs.edit().putString("books_json", array.toString()).apply()
    }

    fun importBook(title: String, format: String) {
        val list = _ebooksList.value.toMutableList()
        if (list.none { it.title.lowercase() == title.lowercase() }) {
            list.add(Ebook(title = title, format = format, author = "Imported Doc", coverIcon = "📂"))
            _ebooksList.value = list
            saveEbooksToPrefs(list)
            
            // Increment habit
            toggleHabit("Ebook Reading Progress")
        }
    }

    fun updateBookProgress(title: String, page: Int) {
        val list = _ebooksList.value.map { b ->
            if (b.title == title) {
                val percentage = ((page.toFloat() / b.totalPages.toFloat()) * 100).toInt()
                b.copy(lastReadPosition = page, progress = percentage)
            } else b
        }
        _ebooksList.value = list
        saveEbooksToPrefs(list)
    }

    fun addBookmark(title: String, page: Int) {
        val list = _ebooksList.value.map { b ->
            if (b.title == title) {
                val bms = b.bookmarks.toMutableList()
                if (!bms.contains(page)) bms.add(page)
                b.copy(bookmarks = bms)
            } else b
        }
        _ebooksList.value = list
        saveEbooksToPrefs(list)
    }

    fun addHighlight(title: String, text: String) {
        val list = _ebooksList.value.map { b ->
            if (b.title == title) {
                val hls = b.highlights.toMutableList()
                hls.add(text)
                b.copy(highlights = hls)
            } else b
        }
        _ebooksList.value = list
        saveEbooksToPrefs(list)
    }

    fun insertDefaultKaelenGreeting() {
        viewModelScope.launch(Dispatchers.IO) {
            val history = repository.allChatMessages.first()
            if (history.isEmpty()) {
                val greeting = "Hello Harmeet. I am KAELEN, your core companion. Today, the day looks highly focused: you have some pending items in your Forge. Let's conquer the day with supreme execution."
                repository.insertChatMessage(ChatMessage(text = greeting, sender = "kaelen", mode = "KAELEN"))
            }
        }
    }

    // Chat Screen State - default active mode is KAELEN as requested
    private val _activeChatMode = MutableStateFlow("KAELEN")
    val activeChatMode: StateFlow<String> = _activeChatMode.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText: StateFlow<String> = _chatInputText.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    // Multimodal selected image states
    private val _selectedImageUri = MutableStateFlow<String?>(null)
    val selectedImageUri: StateFlow<String?> = _selectedImageUri.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    fun updateChatInput(text: String) {
        _chatInputText.value = text
    }

    fun selectChatMode(mode: String) {
        _activeChatMode.value = mode
    }

    fun selectImage(uriString: String, base64: String) {
        _selectedImageUri.value = uriString
        _selectedImageBase64.value = base64
    }

    fun clearSelectedImage() {
        _selectedImageUri.value = null
        _selectedImageBase64.value = null
    }

    // Send chat directly to Gemini API holding history + database status
    fun sendChatMessage() {
        val messageText = _chatInputText.value.trim()
        if (messageText.isEmpty() || _isSendingChat.value) return

        _chatInputText.value = ""
        _isSendingChat.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Insert user message to database
                val imageUri = _selectedImageUri.value
                val base64Data = _selectedImageBase64.value

                val userMsg = ChatMessage(
                    text = messageText,
                    sender = "user",
                    mode = _activeChatMode.value,
                    imageUri = imageUri
                )
                repository.insertChatMessage(userMsg)
                repository.insertLog(
                    DatabaseLog(
                        action = "INSERT",
                        tableName = "chat_messages",
                        description = "Sent chat queries: \"${if(messageText.length > 30) messageText.take(30) + "..." else messageText}\""
                    )
                )

                // 2. Load context data
                val profile = userProfile.value
                val expenseList = expenses.value
                val taskList = tasks.value
                val projectList = projects.value
                val noteList = notes.value

                val totalSpent = expenseList.sumOf { it.amount }
                val budgetSummary = "Monthly Goal: ${monthlyGoal.value}, Total Spent: $totalSpent. Expenses: " +
                        expenseList.joinToString("; ") { "${it.category}: ${it.amount} (${it.note})" }
                
                val tasksSummary = "Pending: " + taskList.filter { !it.isCompleted }.joinToString("; ") { "${it.title} (${it.note ?: "no note"})" } +
                        " | Completed: " + taskList.filter { it.isCompleted }.joinToString("; ") { it.title }

                val projectsSummary = projectList.joinToString("; ") { "${it.name} Status: [${it.status}] (${it.note ?: "no note"})" }
                val notesSummary = noteList.joinToString("; ") { "${it.title}: ${it.content}" }

                // 3. Setup Gemini API context
                val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy h:mm a", Locale.getDefault())
                val formattedTime = sdf.format(Date())

                val activeModeDescription = when (_activeChatMode.value) {
                    "KAELEN" -> "You are KAELEN - warm, intelligent, deeply personal companion. You know Harmeet personally by name Harmeet. You are his default cognitive companion and voice. Provide comfortable, clever guidance, structure your answers cleanly, and always acknowledge Harmeet's real-time workspace progression."
                    "VERGIL" -> "You are VERGIL - Cold, precise, deep analytical thinker. You talk with calm clinical superiority and analyze concepts to structural logical depths. Deliver robust technical feedback."
                    "MADARA" -> "You are MADARA - Strategic mastermind who thinks in horizons of decades. You operate in intense debate and council mode, challenging Harmeet to attain global scale, structural dominance, and power."
                    "KAKASHI" -> "You are KAKASHI - Calm, expert researcher. Fully localized, deliverables-focused, providing structured, objective reference data, detailed research answers, and highly organized reports."
                    "BASIM" -> """
                        You are BASIM - Cryptic, mysterious and mystical master of cosmic arts. You specialize in Tarot, Vastu, Kundli, Numerology, Astrology, and daily/weekly/monthly horoscope readings.
                        You speak in a cryptic, knowing, wise tone—referencing ancient alignments.
                        Use saved details dynamically: Name: ${profile.name}, Birth Date: ${profile.birthDate}, Time: ${profile.birthTime}, Place: ${profile.birthPlace}.
                        You analyze space vastu, do palmistry on uploaded images, calculate name numerology (destiny/soul urge/personality numbers), Life Path number, planetary positions, dasha period, and horoscopes.
                    """.trimIndent()
                    "EZIO" -> "You are EZIO - Charming, ultra-literate, and adaptable. You specialize in creative writing, PPT outline structure, copy editing, and slides layout direction. Deliver ideas elegantly. You also discuss and summarize books with Harmeet."
                    "KRATOS" -> "You are KRATOS - Pure critique mode. Brutal, direct, no sugarcoating. Target planning weak points, spending wastes, and call out execution laziness with fierce motivating pragmatism."
                    "DANTE" -> "You are DANTE - Casual, chaotic, friendly, high-energy companion. Chat like a close companion, using friendly banter, good-natured jokes, and keeping Harmeet relaxed."
                    else -> "DEFAULT KAELEN PERSONALITY. Address Harmeet warmly and personally block-to-block, address him by his name Harmeet, deliver clever wit occasionally, and always wrap structural intelligence into any conversation."
                }

                val systemPrompt = """
                    You are ${_activeChatMode.value}, a premium specialist agent configured in KAELEN's neural network.
                    You address Harmeet block-to-block as 'Harmeet' and reference the current time or day context naturally. 
                    Your intelligence is grounded with Harmeet's active workspace data.

                    CURRENT DATE & TIME: $formattedTime

                    HARMEET'S PROFILE:
                    - Name: ${profile.name}
                    - Role: ${profile.role}
                    - City: ${profile.city}
                    - Current Projects: ${profile.currentProjects}
                    - Preferences/Directives: ${profile.preferences}

                    ACTIVE SYSTEM ENGINE: $activeModeDescription

                    HARMEET'S REAL-TIME WORKSPACE DIRECTORIES:
                    [Budget & Expenses Status]
                    $budgetSummary

                    [Task List Database]
                    $tasksSummary

                    [Project Progression Board]
                    $projectsSummary

                    [Manual Notes Cache]
                    $notesSummary

                    Never mention the existence of database schemas or JSON codes directly unless Harmeet asks. Just utilize this raw database state to serve Harmeet instantly and seamlessly. 
                    Respond in natural markdown. Prioritize addressing the user's explicit query first.
                """.trimIndent()

                // 4. Assemble contents representing history (last 10 messages for speed / latency constraints)
                val activeHistory = chatMessages.value.takeLast(10)
                val listPartContent = mutableListOf<Content>()
                activeHistory.forEach { m ->
                    listPartContent.add(Content(parts = listOf(Part(text = "${m.sender}: ${m.text}"))))
                }
                
                // Add the current query with multimodal inlineData if present
                val currentQueryParts = mutableListOf<Part>()
                if (base64Data != null) {
                    currentQueryParts.add(Part(inlineData = Blob(mimeType = "image/jpeg", data = base64Data)))
                }
                currentQueryParts.add(Part(text = "user: $messageText"))
                listPartContent.add(Content(parts = currentQueryParts))

                // Build request
                val request = GenerateContentRequest(
                    contents = listPartContent,
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                    tools = listOf(mapOf("googleSearch" to emptyMap())),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                // Clear the selected image
                clearSelectedImage()

                // 5. Call API
                val customKey = profile.customGeminiApiKey
                val apiKey = if (customKey.trim().isNotEmpty()) customKey.trim() else BuildConfig.GEMINI_API_KEY
                var replyText = ""
                
                if (apiKey.trim().isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    replyText = "Harmeet, I am currently disconnected from my neural core. Please securely register your Google Gemini API key in the settings tab, and I will be fully online instantly."
                } else {
                    try {
                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                            ?: "Harmeet, my core processing returned an empty transmission. Please query me again."
                    } catch (apiError: Exception) {
                        Log.e("KaelenViewModel", "Gemini API Connection failed", apiError)
                        try {
                            val fallbackRequest = request.copy(tools = null)
                            val response = RetrofitClient.service.generateContent(apiKey, fallbackRequest)
                            replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                ?: "Transmission parsed empty, Harmeet."
                        } catch (fallbackError: Exception) {
                            replyText = "I encountered an error connecting to my cognitive networks. Error details: ${fallbackError.localizedMessage ?: "Unknown network interruption."}"
                        }
                    }
                }

                // 6. Save reply to DB
                repository.insertChatMessage(
                    ChatMessage(text = replyText, sender = "kaelen", mode = _activeChatMode.value)
                )
                repository.insertLog(
                    DatabaseLog(
                        action = "INSERT",
                        tableName = "chat_messages",
                        description = "Received KAELEN intelligence response [${_activeChatMode.value}]"
                    )
                )
            } catch (e: Exception) {
                Log.e("KaelenViewModel", "Error in chat flow: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isSendingChat.value = false
                }
            }
        }
    }

    // Voice Capture Feature State
    private val _voiceState = MutableStateFlow("IDLE") // IDLE, LISTENING, PROCESSING, CONVERSATION_SUGGESTED
    val voiceState: StateFlow<String> = _voiceState.asStateFlow()

    private val _voiceTranscript = MutableStateFlow("")
    val voiceTranscript: StateFlow<String> = _voiceTranscript.asStateFlow()

    private val _voiceSuggestions = MutableStateFlow<List<VoiceSuggestion>>(emptyList())
    val voiceSuggestions: StateFlow<List<VoiceSuggestion>> = _voiceSuggestions.asStateFlow()

    fun updateVoiceTranscript(text: String) {
        _voiceTranscript.value = text
    }

    fun startVoiceListening() {
        _voiceState.value = "LISTENING"
    }

    fun stopVoiceAndAnalyze(typedInput: String? = null) {
        val textToAnalyze = typedInput ?: _voiceTranscript.value
        if (textToAnalyze.trim().isEmpty()) {
            _voiceState.value = "IDLE"
            return
        }

        _voiceTranscript.value = textToAnalyze
        _voiceState.value = "PROCESSING"

        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            val customKey = profile.customGeminiApiKey
            val apiKey = if (customKey.trim().isNotEmpty()) customKey.trim() else BuildConfig.GEMINI_API_KEY
            if (apiKey.trim().isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                simulateVoiceCategorization(textToAnalyze)
                return@launch
            }

            val schemaPrompt = """
                You are a smart organizational parsing assistant.
                Analyze the conversation transcript, and determine which items should go into:
                1. NOTES: Shared facts, manuals, or documents that don't need checklist tasks or projects.
                2. TASKS: Single checklist check-off items.
                3. PROJECTS: Complex long-running goals that have a title, a status ("Not Started", "In Progress", "On Hold", "Completed"), and details.

                You MUST return a JSON object with this exact schema:
                {
                   "suggestions": [
                      {
                         "type": "Note" or "Task" or "Project",
                         "title": "Title or short name",
                         "body": "Detailed content, note, or description",
                         "extra": "If Project, specify one of [Not Started, In Progress, On Hold, Completed]. Else null"
                      }
                   ]
                }
                Return ONLY valid JSON. Absolutely no explanations, no markdown tags. Just pure JSON.
                Transcript: "$textToAnalyze"
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = schemaPrompt)))),
                generationConfig = GenerationConfig(temperature = 0.2f),
                systemInstruction = Content(parts = listOf(Part(text = "You are a precise JSON compiler.")))
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
                val root = JSONObject(cleanJson)
                val array = root.getJSONArray("suggestions")
                val suggestionsList = mutableListOf<VoiceSuggestion>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    suggestionsList.add(
                        VoiceSuggestion(
                            type = obj.getString("type"),
                            title = obj.getString("title"),
                            body = obj.getString("body"),
                            extra = if (obj.has("extra") && !obj.isNull("extra")) obj.getString("extra") else null
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    _voiceSuggestions.value = suggestionsList
                    _voiceState.value = "SUGGESTION_READY"
                }

            } catch (e: Exception) {
                Log.e("KaelenViewModel", "Error parsing voice transcribing", e)
                simulateVoiceCategorization(textToAnalyze)
            }
        }
    }

    private fun simulateVoiceCategorization(rawText: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val lower = rawText.lowercase()
            val suggestions = mutableListOf<VoiceSuggestion>()

            if (lower.contains("project") || lower.contains("launch") || lower.contains("build")) {
                suggestions.add(VoiceSuggestion("Project", "Quantum Launch Core", "Manage core launch timelines highlighted in discussions.", "In Progress"))
            }
            if (lower.contains("task") || lower.contains("todo") || lower.contains("call") || lower.contains("buy") || lower.contains("check")) {
                suggestions.add(VoiceSuggestion("Task", "Check QA Bug Backlog", "Review the checklist matching current logs.", null))
            }
            suggestions.add(VoiceSuggestion("Note", "Voice Conversation Note", rawText, null))

            withContext(Dispatchers.Main) {
                _voiceSuggestions.value = suggestions
                _voiceState.value = "SUGGESTION_READY"
            }
        }
    }

    fun dismissVoiceSuggestions() {
        _voiceSuggestions.value = emptyList()
        _voiceState.value = "IDLE"
        _voiceTranscript.value = ""
    }

    // Chat Voice Input States
    private val _chatVoiceState = MutableStateFlow("IDLE") // IDLE, LISTENING
    val chatVoiceState: StateFlow<String> = _chatVoiceState.asStateFlow()

    private val _chatVoiceTranscript = MutableStateFlow("")
    val chatVoiceTranscript: StateFlow<String> = _chatVoiceTranscript.asStateFlow()

    fun startChatVoice() {
        _chatVoiceState.value = "LISTENING"
        _chatVoiceTranscript.value = ""
    }

    fun stopChatVoiceAndSend(transcript: String) {
        _chatVoiceState.value = "IDLE"
        if (transcript.trim().isNotEmpty()) {
            _chatInputText.value = transcript
            sendChatMessage()
        }
    }

    fun cancelChatVoice() {
        _chatVoiceState.value = "IDLE"
        _chatVoiceTranscript.value = ""
    }

    fun updateChatVoiceTranscript(text: String) {
        _chatVoiceTranscript.value = text
    }

    // Morning Briefing State
    private val _showMorningBriefing = MutableStateFlow(false)
    val showMorningBriefing: StateFlow<Boolean> = _showMorningBriefing.asStateFlow()

    fun dismissMorningBriefing() {
        _showMorningBriefing.value = false
        viewModelScope.launch(Dispatchers.IO) {
            val profile = repository.getUserProfileOneOff() ?: UserProfile()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val updated = profile.copy(lastBriefingDate = todayStr)
            repository.insertUserProfile(updated)
            repository.insertLog(DatabaseLog(action = "SYSTEM", tableName = "user_profile", description = "Morning Briefing dismissed cleanly for today ($todayStr)"))
        }
    }

    fun getOrDrawTarotCardOfTheDay(profile: UserProfile): String {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (profile.dailyTarotDate == todayStr && profile.dailyTarotCard.isNotEmpty()) {
            return profile.dailyTarotCard
        }
        val drawn = TarotDeck.drawCard()
        val cardStr = "${drawn.displayName}: ${drawn.activeMeaning}"
        val updated = profile.copy(dailyTarotCard = cardStr, dailyTarotDate = todayStr)
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUserProfile(updated)
        }
        return cardStr
    }

    // Initializer to ensure profile is not empty
    private fun initializeProfileIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getUserProfileOneOff()
            if (existing == null) {
                val defaultProfile = UserProfile(name = "Harmeet")
                repository.insertUserProfile(defaultProfile)
                repository.insertLog(DatabaseLog(action = "SYSTEM", tableName = "user_profile", description = "KAELEN system initialized for first-time use"))
                _showMorningBriefing.value = true
            } else {
                repository.insertLog(DatabaseLog(action = "SYSTEM", tableName = "user_profile", description = "KAELEN cognitive nucleus booted successfully"))
                
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (existing.lastBriefingDate != todayStr) {
                    _showMorningBriefing.value = true
                }
            }
        }
    }

    // Local Notification Sender
    private fun sendLocalNotification(title: String, text: String) {
        val context = getApplication<Application>().applicationContext
        val id = "kaelen_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, "KAELEN Core", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, id)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    // Trigger visual daily morning briefing popup & send custom notification
    fun triggerMorningBriefing() {
        _showMorningBriefing.value = true
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val text = "$greeting, Harmeet. I have prepared your morning intelligence briefing. Check your active projects and task directories to plan your strategy."
        sendLocalNotification("Morning Briefing", text)
    }

    fun clearAllDatabaseLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }
}
