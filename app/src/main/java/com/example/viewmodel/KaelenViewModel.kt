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
import retrofit2.HttpException
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
    data class UpdateProfileDirectives(
        val name: String,
        val role: String,
        val city: String,
        val customGeminiApiKey: String,
        val birthDate: String,
        val birthTime: String,
        val birthPlace: String
    ) : PendingAction()
    data class UpdateTheme(val theme: String) : PendingAction()
    object ClearChat : PendingAction()
}

// Real database actions exposed to Gemini as callable functions. Defined once and shared by
// every persona's chat turn (KAELEN, VERGIL, MADARA, KAKASHI, BASIM, EZIO, KRATOS, DANTE) — there
// is no per-persona copy of this registry or the execution path that backs it.
//
// To add a new action later (e.g. credit card expense logging, once that feature has a real
// model/DAO/repository method): add one FunctionDeclaration here, one PendingAction case in
// performAction(), and one mapping branch in mapFunctionCallToPendingAction(). Nothing else
// changes, and no persona needs to be touched individually.
private val chatFunctionDeclarations = listOf(
    FunctionDeclaration(
        name = "add_expense",
        description = "Logs a real expense entry to Harmeet's expense database. Only call this when Harmeet clearly states he spent money on something.",
        parameters = FunctionParameters(
            properties = mapOf(
                "amount" to PropertySchema(type = "NUMBER", description = "The amount spent, in rupees."),
                "category" to PropertySchema(type = "STRING", description = "A short spending category, e.g. Food, Transport, Shopping."),
                "note" to PropertySchema(type = "STRING", description = "Optional short note or description of the expense.")
            ),
            required = listOf("amount", "category")
        )
    ),
    FunctionDeclaration(
        name = "add_task",
        description = "Adds a real task to Harmeet's task list. Only call this when Harmeet clearly asks to add, remember, or track a to-do item.",
        parameters = FunctionParameters(
            properties = mapOf(
                "title" to PropertySchema(type = "STRING", description = "The task title."),
                "note" to PropertySchema(type = "STRING", description = "Optional additional detail about the task.")
            ),
            required = listOf("title")
        )
    ),
    FunctionDeclaration(
        name = "add_project",
        description = "Creates a real project tracker entry. Only call this when Harmeet clearly asks to start or track a new project.",
        parameters = FunctionParameters(
            properties = mapOf(
                "name" to PropertySchema(type = "STRING", description = "The project name."),
                "status" to PropertySchema(
                    type = "STRING",
                    description = "The initial project status.",
                    enum = listOf("Not Started", "In Progress", "On Hold", "Completed")
                ),
                "note" to PropertySchema(type = "STRING", description = "Optional additional detail about the project.")
            ),
            required = listOf("name", "status")
        )
    ),
    FunctionDeclaration(
        name = "add_note",
        description = "Saves a real note to Harmeet's notes cache. Only call this when Harmeet clearly asks to save, remember, or write down a note.",
        parameters = FunctionParameters(
            properties = mapOf(
                "title" to PropertySchema(type = "STRING", description = "The note title."),
                "content" to PropertySchema(type = "STRING", description = "The note content.")
            ),
            required = listOf("title", "content")
        )
    )
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

    // People Profiles State Flow
    val peopleProfiles: StateFlow<List<PersonProfile>> = repository.allPeopleProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Unlocked People Profile IDs in Current Session
    private val _unlockedProfileIds = MutableStateFlow<Set<Int>>(emptySet())
    val unlockedProfileIds: StateFlow<Set<Int>> = _unlockedProfileIds.asStateFlow()

    fun encryptPin(pin: String): String {
        if (pin.isEmpty()) return ""
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin
        }
    }

    fun unlockProfile(profileId: Int, pin: String): Boolean {
        val hash = encryptPin(pin)
        val profiles = peopleProfiles.value
        val p = profiles.find { it.id == profileId } ?: return false
        if (!p.isPinLocked || p.encryptedPin == hash) {
            _unlockedProfileIds.update { it + profileId }
            return true
        }
        return false
    }

    fun lockProfile(profileId: Int) {
        _unlockedProfileIds.update { it - profileId }
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = peopleProfiles.value
            val p = profiles.find { it.id == profileId }
            if (p != null) {
                repository.insertPersonProfile(p.copy(isPinLocked = true))
            }
        }
    }

    fun setProfilePin(profileId: Int, pin: String, lockDirectly: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = peopleProfiles.value
            val p = profiles.find { it.id == profileId }
            if (p != null) {
                val encrypted = encryptPin(pin)
                val updated = p.copy(encryptedPin = encrypted, isPinLocked = lockDirectly)
                repository.insertPersonProfile(updated)
                if (lockDirectly) {
                    _unlockedProfileIds.update { it - profileId }
                } else {
                    _unlockedProfileIds.update { it + profileId }
                }
                repository.insertLog(DatabaseLog(action = "UPDATE", tableName = "people_profiles", description = "Set PIN lock details for person: " + p.name))
            }
        }
    }

    fun removeProfilePin(profileId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = peopleProfiles.value
            val p = profiles.find { it.id == profileId }
            if (p != null) {
                val updated = p.copy(encryptedPin = "", isPinLocked = false)
                repository.insertPersonProfile(updated)
                _unlockedProfileIds.update { it + profileId }
                repository.insertLog(DatabaseLog(action = "UPDATE", tableName = "people_profiles", description = "Removed PIN lock for person: " + p.name))
            }
        }
    }

    fun savePersonProfile(person: PersonProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPersonProfile(person)
        }
    }

    fun deletePersonProfile(person: PersonProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePersonProfile(person)
        }
    }

    fun drawTarotForPerson(person: PersonProfile) {
        val drawn = TarotDeck.drawCard()
        val cardStr = "${drawn.displayName}: ${drawn.activeMeaning}"
        val readingStr = "${drawn.displayName} reveals: ${drawn.activeMeaning}.\n\nBasim advises: " +
                "Direct the cosmic core focus into workspace goals. High compliance vibes surround " + person.name + "'s chart."
        val updated = person.copy(tarotCard = cardStr, tarotReading = readingStr)
        savePersonProfile(updated)
    }

    fun generateHoroscopeForPerson(person: PersonProfile) {
        val adviceList = listOf(
            "Cosmic forces are realigning in your segment. Ideal time to complete outstanding Vault ledger targets.",
            "A serene wave of solar transits suggests taking a brief Rest Interval before embarking on the next Forge sprint.",
            "Mercury’s position suggests highly intellectual communication and structured Vastu energy flow."
        )
        val selectedAdvice = adviceList.random()
        val updated = person.copy(dailyHoroscope = "Horoscope for " + person.name + ": " + selectedAdvice)
        savePersonProfile(updated)
    }

    val compatibilityResult = MutableStateFlow("")

    fun calculateZodiac(dobString: String): String {
        if (dobString.isBlank()) return "Unknown"
        return try {
            val parts = dobString.split("-")
            if (parts.size == 3) {
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                when (month) {
                    1 -> if (day < 20) "Capricorn" else "Aquarius"
                    2 -> if (day < 19) "Aquarius" else "Pisces"
                    3 -> if (day < 21) "Pisces" else "Aries"
                    4 -> if (day < 20) "Aries" else "Taurus"
                    5 -> if (day < 21) "Taurus" else "Gemini"
                    6 -> if (day < 21) "Gemini" else "Cancer"
                    7 -> if (day < 23) "Cancer" else "Leo"
                    8 -> if (day < 23) "Leo" else "Virgo"
                    9 -> if (day < 23) "Virgo" else "Libra"
                    10 -> if (day < 23) "Libra" else "Scorpio"
                    11 -> if (day < 22) "Scorpio" else "Sagittarius"
                    12 -> if (day < 22) "Sagittarius" else "Capricorn"
                    else -> "Unknown"
                }
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun performCompatibilityReading(person: PersonProfile) {
        val profiles = peopleProfiles.value
        val harmeet = profiles.find { it.relationship.lowercase() == "self" || it.name.lowercase() == "harmeet" }
        
        if (harmeet == null) {
            compatibilityResult.value = "Error: Harmeet's profile is missing. Please initialize the primary user profile first."
            return
        }

        val unlocked = unlockedProfileIds.value
        val harmeetLocked = harmeet.isPinLocked && !unlocked.contains(harmeet.id)
        val personLocked = person.isPinLocked && !unlocked.contains(person.id)
        
        if (harmeetLocked || personLocked) {
            compatibilityResult.value = "Access Denied. Both profiles must be unlocked to analyze compatibility."
            return
        }

        val name1 = harmeet.name
        val name2 = person.name
        val sign1 = calculateZodiac(harmeet.dateOfBirth)
        val sign2 = calculateZodiac(person.dateOfBirth)
        
        val pct = (50 + (name1.length + name2.length) * 3 % 45)
        val message = "🌟 BASIM'S SYSTEM COMPATIBILITY PROTOCOL 🌟\n\n" +
                "Subject A: $name1 ($sign1)\n" +
                "Subject B: $name2 ($sign2)\n\n" +
                "Analysis Index: $pct% Astro-Vector Alignment.\n\n" +
                "Basim's guidance: Space-time matrices report that $name1 and $name2 share a robust mutual resonance. " +
                "The alignment is highly strategic for joint creative sprints and Vastu coordination. Keep the feedback loops crystal clear!"
        
        compatibilityResult.value = message
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

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
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMonthlyGoal(goal)
        }
    }

    fun updatePreferredTimer(focusMin: Int, breakMin: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePreferredTimer(focusMin, breakMin)
            repository.insertLog(DatabaseLog(action = "UPDATE", tableName = "user_profile", description = "Preferred durations updated to $focusMin / $breakMin minutes."))
        }
    }

    // Confirmation Screen State (legacy, direct execution preferred to avoid broken data saving)
    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    fun dismissAction() {
        _pendingAction.value = null
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getUserProfileOneOff()
            var profile = if (existing == null) {
                val defaultHabits = listOf(
                    Habit("Morning Routine", emptySet(), 0),
                    Habit("Evening Review", emptySet(), 0),
                    Habit("Daily Reading", emptySet(), 0)
                )
                val defaultBooks = listOf(
                    Ebook("The Mystical Arts & Vastu Alignment", "PDF", 12, 36, 300, listOf(12, 24), listOf("Vastu aligns energy block-to-block."), "BASIM", "🏰"),
                    Ebook("The Art of Cold Steel Strategies", "EPUB", 45, 135, 300, emptyList(), listOf("Cold execution defeats raw fire."), "VERGIL", "⚔️"),
                    Ebook("Shinobi Information Delivery Manual", "PDF", 0, 1, 150, emptyList(), emptyList(), "KAKASHI", "📜")
                )
                val defaultProfile = UserProfile(
                    name = "Harmeet",
                    birthDate = "1995-11-20",
                    birthTime = "14:30",
                    birthPlace = "Delhi",
                    selectedTheme = "INFERNO",
                    habitsJson = serializeHabits(defaultHabits),
                    ebooksJson = serializeEbooks(defaultBooks),
                    focusStreak = 0,
                    monthlyGoal = 50000.0
                )
                repository.insertUserProfile(defaultProfile)
                repository.insertLog(DatabaseLog(action = "SYSTEM", tableName = "user_profile", description = "KAELEN system initialized for first-time use"))
                _showMorningBriefing.value = true
                defaultProfile
            } else {
                repository.insertLog(DatabaseLog(action = "SYSTEM", tableName = "user_profile", description = "KAELEN cognitive nucleus booted successfully"))
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (existing.lastBriefingDate != todayStr) {
                    _showMorningBriefing.value = true
                }
                existing
            }

            // Draw Tarot Card exactly once per day
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (profile.dailyTarotDate != todayStr || profile.dailyTarotCard.isBlank()) {
                val drawnCard = TarotDeck.drawCard()
                val cardRepresentation = "${drawnCard.displayName}: ${drawnCard.activeMeaning}"
                profile = profile.copy(
                    dailyTarotDate = todayStr,
                    dailyTarotCard = cardRepresentation
                )
                repository.updateDailyTarot(todayStr, cardRepresentation)
                repository.insertLog(DatabaseLog(action = "UPDATE", tableName = "user_profile", description = "Tarot of the day drawn for $todayStr: ${drawnCard.displayName}"))
            }

            // Pre-create People Profiles if empty
            val people = repository.getAllPeopleProfilesOneOff()
            if (people.isEmpty()) {
                val harmeetPerson = PersonProfile(
                    name = profile.name,
                    relationship = "Self",
                    dateOfBirth = if (profile.birthDate.isNotBlank()) profile.birthDate else "1995-11-20",
                    birthTime = if (profile.birthTime.isNotBlank()) profile.birthTime else "14:30",
                    birthPlace = if (profile.birthPlace.isNotBlank()) profile.birthPlace else "Delhi",
                    photoEmoji = "👑",
                    city = profile.city.ifBlank { "Delhi" },
                    notes = "Core operator and principal focus."
                )
                val vinishaaPerson = PersonProfile(
                    name = "Vinishaa",
                    relationship = "Partner",
                    photoEmoji = "🌸",
                    isPinLocked = true,
                    encryptedPin = encryptPin("1111") // Default PIN 1111
                )
                repository.insertPersonProfile(harmeetPerson)
                repository.insertPersonProfile(vinishaaPerson)
                repository.insertLog(DatabaseLog(action = "SYSTEM", tableName = "people_profiles", description = "Pre-created profiles for Harmeet and Vinishaa."))
            }

            // Sync other memory states
            _monthlyGoal.value = profile.monthlyGoal
            _focusStreak.value = profile.focusStreak
            deserializeHabits(profile.habitsJson)
            deserializeEbooks(profile.ebooksJson)
            insertDefaultKaelenGreeting()

            // Keep memory variables reactively synchronized with DB
            viewModelScope.launch {
                repository.userProfile.collect { currentProfile ->
                    if (currentProfile != null) {
                        _monthlyGoal.value = currentProfile.monthlyGoal
                        _focusStreak.value = currentProfile.focusStreak
                        val h = parseHabits(currentProfile.habitsJson)
                        if (_habitsList.value != h) {
                            _habitsList.value = h
                        }
                        val b = parseEbooks(currentProfile.ebooksJson)
                        if (_ebooksList.value != b) {
                            _ebooksList.value = b
                        }
                    }
                }
            }

            val savedThemeName = com.example.data.ThemePreferences.getThemeSync(getApplication())
            val savedVariant = when (savedThemeName.uppercase()) {
                "ARCTIC_FOX", "ARCTIC_FOX", "ARCTIC FOX" -> com.example.ui.theme.AppThemeVariant.ARCTIC_FOX
                "CRIMSON_WOLF", "CRIMSON_WOLF", "CRIMSON WOLF" -> com.example.ui.theme.AppThemeVariant.CRIMSON_WOLF
                "NEXUS" -> com.example.ui.theme.AppThemeVariant.NEXUS
                else -> com.example.ui.theme.AppThemeVariant.INFERNO
            }
            withContext(Dispatchers.Main) {
                com.example.ui.theme.ThemeManager.activeVariant.value = savedVariant
            }

            _monthlyGoal.value = profile.monthlyGoal
            _isReady.value = true
        }
    }

    // Direct, Immediate Persistence Execution (Robust & failsafe)
    fun requestAction(action: PendingAction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                performAction(action)
            } catch (e: Exception) {
                Log.e("KaelenViewModel", "Error in pending action execution: ${e.message}")
            }
        }
    }

    // Shared execution path: every real database write goes through here, whether triggered by a
    // direct UI action (via requestAction, fire-and-forget) or a verified chat function call
    // (via executeFunctionCall, awaited so the caller knows the write actually completed).
    // Returns a human-readable description of what was actually persisted; throws on failure.
    private suspend fun performAction(action: PendingAction): String {
        return when (action) {
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
                "Logged expense of ₹${action.amount} under category '${action.category}'${if (action.note.isNotBlank()) " (note: ${action.note})" else ""}."
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
                "Deleted expense of ₹${action.expense.amount} under '${action.expense.category}'."
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
                "Created task '${action.title}'${if (!action.note.isNullOrBlank()) " (note: ${action.note})" else ""}."
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
                "Removed task '${action.task.title}'."
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
                "$statusText task '${action.task.title}'."
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
                "Created project '${action.name}' with status '${action.status}'${if (!action.note.isNullOrBlank()) " (note: ${action.note})" else ""}."
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
                "Updated project '${action.project.name}' status to '${action.newStatus}'."
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
                "Deleted project '${action.project.name}'."
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
                "Saved note '${action.title}'."
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
                "Deleted note '${action.note.title}'."
            }
            is PendingAction.UpdateProfileDirectives -> {
                repository.updateProfileDirectives(
                    name = action.name,
                    role = action.role,
                    city = action.city,
                    customGeminiApiKey = action.customGeminiApiKey,
                    birthDate = action.birthDate,
                    birthTime = action.birthTime,
                    birthPlace = action.birthPlace
                )
                repository.insertLog(
                    DatabaseLog(
                        action = "UPDATE",
                        tableName = "user_profile",
                        description = "Updated profile directives configuration"
                    )
                )

                // Re-apply briefing schedule using the (unchanged) persisted briefing settings
                val context = getApplication<Application>().applicationContext
                val current = repository.getUserProfileOneOff()
                if (current != null) {
                    if (current.briefingEnabled) {
                        com.example.scheduler.BriefingScheduler.scheduleDailyBriefing(
                            context,
                            current.briefingHour,
                            current.briefingMinute
                        )
                    } else {
                        com.example.scheduler.BriefingScheduler.cancelDailyBriefing(context)
                    }
                }
                "Updated profile directives."
            }
            is PendingAction.UpdateTheme -> {
                repository.updateSelectedTheme(action.theme)

                // Save Theme variant immediately to DataStore & update global ThemeManager.activeVariant
                val context = getApplication<Application>().applicationContext
                com.example.data.ThemePreferences.saveTheme(context, action.theme)
                val variant = when (action.theme.uppercase()) {
                    "ARCTIC_FOX", "ARCTIC FOX" -> com.example.ui.theme.AppThemeVariant.ARCTIC_FOX
                    "CRIMSON_WOLF", "CRIMSON WOLF" -> com.example.ui.theme.AppThemeVariant.CRIMSON_WOLF
                    "NEXUS" -> com.example.ui.theme.AppThemeVariant.NEXUS
                    else -> com.example.ui.theme.AppThemeVariant.INFERNO
                }
                withContext(Dispatchers.Main) {
                    com.example.ui.theme.ThemeManager.activeVariant.value = variant
                }

                repository.insertLog(
                    DatabaseLog(
                        action = "UPDATE",
                        tableName = "user_profile",
                        description = "Theme variant changed to ${action.theme}"
                    )
                )
                "Theme changed to ${action.theme}."
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
                "Cleared chat history."
            }
        }
    }

    private fun parseHabits(habitsJson: String): List<Habit> {
        if (habitsJson.isEmpty()) {
            return listOf(
                Habit("Morning Routine", emptySet(), 0),
                Habit("Evening Review", emptySet(), 0),
                Habit("Daily Reading", emptySet(), 0)
            )
        }
        return try {
            val array = JSONArray(habitsJson)
            val list = mutableListOf<Habit>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val streak = obj.optInt("streak", 0)
                val dates = mutableSetOf<String>()
                val datesArr = obj.getJSONArray("dates")
                for (j in 0 until datesArr.length()) {
                    dates.add(datesArr.getString(j))
                }
                list.add(Habit(name, dates, streak))
            }
            list
        } catch (e: Exception) {
            Log.e("KaelenViewModel", "Error parsing habits", e)
            listOf(
                Habit("Morning Routine", emptySet(), 0),
                Habit("Evening Review", emptySet(), 0),
                Habit("Daily Reading", emptySet(), 0)
            )
        }
    }

    private fun parseEbooks(booksJson: String): List<Ebook> {
        if (booksJson.isEmpty()) {
            return listOf(
                Ebook("The Mystical Arts & Vastu Alignment", "PDF", 12, 36, 300, listOf(12, 24), listOf("Vastu aligns energy block-to-block."), "BASIM", "🏰"),
                Ebook("The Art of Cold Steel Strategies", "EPUB", 45, 135, 300, emptyList(), listOf("Cold execution defeats raw fire."), "VERGIL", "⚔️"),
                Ebook("Shinobi Information Delivery Manual", "PDF", 0, 1, 150, emptyList(), emptyList(), "KAKASHI", "📜")
            )
        }
        return try {
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
            list
        } catch (e: Exception) {
            Log.e("KaelenViewModel", "Error parsing ebooks", e)
            listOf(
                Ebook("The Mystical Arts & Vastu Alignment", "PDF", 12, 36, 300, listOf(12, 24), listOf("Vastu aligns energy block-to-block."), "BASIM", "🏰"),
                Ebook("The Art of Cold Steel Strategies", "EPUB", 45, 135, 300, emptyList(), listOf("Cold execution defeats raw fire."), "VERGIL", "⚔️"),
                Ebook("Shinobi Information Delivery Manual", "PDF", 0, 1, 150, emptyList(), emptyList(), "KAKASHI", "📜")
            )
        }
    }

    private fun deserializeHabits(habitsJson: String) {
        if (habitsJson.isNotEmpty()) {
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
                Log.e("KaelenViewModel", "Error deserializing habits", e)
                loadDefaultDetailedHabits()
            }
        } else {
            loadDefaultDetailedHabits()
        }
    }

    private fun loadDefaultDetailedHabits() {
        val defaults = listOf(
            Habit("Morning Routine", emptySet(), 0),
            Habit("Evening Review", emptySet(), 0),
            Habit("Daily Reading", emptySet(), 0)
        )
        _habitsList.value = defaults
        saveHabitsToDatabase(defaults)
    }

    private fun serializeHabits(list: List<Habit>): String {
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
        return array.toString()
    }

    private fun saveHabitsToDatabase(list: List<Habit>) {
        val json = serializeHabits(list)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateHabitsJson(json)
        }
    }

    fun addHabit(name: String) {
        val newList = _habitsList.value.toMutableList()
        if (newList.none { it.name.lowercase() == name.lowercase() }) {
            newList.add(Habit(name, emptySet(), 0))
            _habitsList.value = newList
            saveHabitsToDatabase(newList)
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
        saveHabitsToDatabase(newList)
    }

    fun deleteHabit(name: String) {
        val newList = _habitsList.value.filterNot { it.name == name }
        _habitsList.value = newList
        saveHabitsToDatabase(newList)
    }

    fun logFocusSession() {
        val streak = _focusStreak.value + 1
        _focusStreak.value = streak

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFocusStreak(streak)
        }

        // Mark Forge Focus habit completed
        toggleHabit("Morning Routine")
    }

    private fun deserializeEbooks(booksJson: String) {
        if (booksJson.isNotEmpty()) {
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
                Log.e("KaelenViewModel", "Error deserializing ebooks", e)
                loadDefaultEbooks()
            }
        } else {
            loadDefaultEbooks()
        }
    }

    private fun loadDefaultEbooks() {
        val defaults = listOf(
            Ebook("The Mystical Arts & Vastu Alignment", "PDF", 12, 36, 300, listOf(12, 24), listOf("Vastu aligns energy block-to-block."), "BASIM", "🏰"),
            Ebook("The Art of Cold Steel Strategies", "EPUB", 45, 135, 300, emptyList(), listOf("Cold execution defeats raw fire."), "VERGIL", "⚔️"),
            Ebook("Shinobi Information Delivery Manual", "PDF", 0, 1, 150, emptyList(), emptyList(), "KAKASHI", "📜")
        )
        _ebooksList.value = defaults
        saveEbooksToDatabase(defaults)
    }

    private fun serializeEbooks(list: List<Ebook>): String {
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
        return array.toString()
    }

    private fun saveEbooksToDatabase(list: List<Ebook>) {
        val json = serializeEbooks(list)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateEbooksJson(json)
        }
    }

    fun importBook(title: String, format: String) {
        val list = _ebooksList.value.toMutableList()
        if (list.none { it.title.lowercase() == title.lowercase() }) {
            list.add(Ebook(title = title, format = format, author = "Imported Doc", coverIcon = "📂"))
            _ebooksList.value = list
            saveEbooksToDatabase(list)

            // Increment habit
            toggleHabit("Daily Reading")
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
        saveEbooksToDatabase(list)
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
        saveEbooksToDatabase(list)
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
        saveEbooksToDatabase(list)
    }

    fun insertDefaultKaelenGreeting() {
        viewModelScope.launch(Dispatchers.IO) {
            val history = repository.allChatMessages.first()
            if (history.isEmpty()) {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val timeRef = when {
                    hour < 12 -> "morning"
                    hour < 17 -> "afternoon"
                    else -> "evening"
                }
                val greeting = "Good $timeRef, Harmeet. I am KAELEN, your core companion. Today, the day looks highly focused: you have some pending items in your Forge. Let's conquer the day with supreme execution."
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

    // Captures the real, raw HTTP failure detail from Gemini instead of just the generic exception
    // message — for an HttpException this includes the literal status code and response body
    // (e.g. a 429's exact quota-dimension text: RPM, TPM, or RPD), which a bare e.message never
    // surfaces. Logs it (tag KaelenGeminiError) AND returns it, so callers can also surface the
    // real detail directly in the chat reply without needing logcat/adb access.
    private fun logGeminiHttpError(context: String, e: Throwable): String {
        val detail = if (e is HttpException) {
            val rawBody = try {
                e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
            } catch (readError: Exception) {
                null
            }
            "HTTP ${e.code()} ${e.message()}: ${rawBody ?: "<no response body>"}"
        } else {
            "${e::class.simpleName}: ${e.message ?: "no further detail"}"
        }
        Log.e("KaelenGeminiError", "$context — $detail", e)
        return detail
    }

    // Drives the function-calling round trip for a single chat turn, shared by every persona.
    // Sends the request; if Gemini responds with one or more function calls, executes each one
    // for real via executeFunctionCall (which goes through performAction), then feeds the verified
    // results back to Gemini so its final reply is grounded in what actually happened. Bounded to
    // a few rounds purely as a runaway-safety net against unexpected repeated tool use.
    private suspend fun runChatTurnWithToolCalling(initialRequest: GenerateContentRequest, apiKey: String): String {
        var contents = initialRequest.contents
        val executedResults = mutableListOf<FunctionResponse>()
        val maxRounds = 4

        repeat(maxRounds) { round ->
            val roundRequest = initialRequest.copy(contents = contents)
            val requestJson = RetrofitClient.toJson(roundRequest)
            Log.d("KaelenGeminiRequest", "Round $round request (${requestJson.length} chars): $requestJson")
            val response = RetrofitClient.service.generateContent(apiKey, roundRequest)
            val parts = response.candidates?.firstOrNull()?.content?.parts.orEmpty()
            val functionCalls = parts.mapNotNull { it.functionCall }

            if (functionCalls.isEmpty()) {
                val text = parts.firstOrNull { it.text != null }?.text
                return text ?: if (executedResults.isNotEmpty()) {
                    synthesizeReplyFromResults(executedResults)
                } else {
                    "Harmeet, my core processing returned an empty transmission. Please query me again."
                }
            }

            val functionResponses = functionCalls.map { call -> executeFunctionCall(call) }
            executedResults.addAll(functionResponses)

            contents = contents +
                Content(role = "model", parts = parts) +
                Content(role = "function", parts = functionResponses.map { Part(functionResponse = it) })
        }

        // Exhausted the round cap without a final text reply from the model — never guess at a
        // confirmation here; report exactly what the verified function results said.
        return synthesizeReplyFromResults(executedResults)
    }

    // Executes one Gemini function call for real and reports back only what actually happened.
    // This is the single chokepoint between "the model wants to do X" and "X was actually written
    // to the database" — performAction() either succeeds (and we report its real description) or
    // throws (and we report the real error), so a success is never claimed without verification.
    private suspend fun executeFunctionCall(call: FunctionCall): FunctionResponse {
        val action = mapFunctionCallToPendingAction(call)
        if (action == null) {
            return FunctionResponse(
                name = call.name,
                response = mapOf(
                    "status" to "error",
                    "message" to "Unrecognized action or missing/invalid required arguments for '${call.name}'."
                )
            )
        }
        return try {
            val details = performAction(action)
            FunctionResponse(name = call.name, response = mapOf("status" to "success", "details" to details))
        } catch (e: Exception) {
            Log.e("KaelenViewModel", "Chat function call failed: ${call.name}", e)
            FunctionResponse(
                name = call.name,
                response = mapOf(
                    "status" to "error",
                    "message" to (e.localizedMessage ?: "The database write failed for an unknown reason.")
                )
            )
        }
    }

    // Validates and converts a Gemini function call into the corresponding PendingAction. Returns
    // null on an unknown function name or missing/malformed required arguments, so a malformed
    // call is reported as an error rather than silently performing a wrong or partial write.
    private fun mapFunctionCallToPendingAction(call: FunctionCall): PendingAction? {
        val args = call.args.orEmpty()

        fun str(key: String): String? = (args[key] as? String)?.trim()
        fun num(key: String): Double? = when (val v = args[key]) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }

        return when (call.name) {
            "add_expense" -> {
                val amount = num("amount")?.takeIf { it > 0 } ?: return null
                val category = str("category")?.takeIf { it.isNotEmpty() } ?: return null
                PendingAction.AddExpense(amount = amount, category = category, note = str("note") ?: "")
            }
            "add_task" -> {
                val title = str("title")?.takeIf { it.isNotEmpty() } ?: return null
                PendingAction.AddTask(title = title, note = str("note")?.ifBlank { null })
            }
            "add_project" -> {
                val name = str("name")?.takeIf { it.isNotEmpty() } ?: return null
                val status = str("status")?.takeIf { it.isNotEmpty() } ?: "Not Started"
                PendingAction.AddProject(name = name, status = status, note = str("note")?.ifBlank { null })
            }
            "add_note" -> {
                val title = str("title")?.takeIf { it.isNotEmpty() } ?: return null
                val content = str("content")?.takeIf { it.isNotEmpty() } ?: return null
                PendingAction.AddNote(title = title, content = content)
            }
            else -> null
        }
    }

    // Last-resort reply built directly from verified function results, used only if the model
    // exhausts the tool-calling round cap without producing its own wrap-up text. Never invents
    // anything beyond what the real results contain.
    private fun synthesizeReplyFromResults(results: List<FunctionResponse>): String {
        if (results.isEmpty()) {
            return "Harmeet, my core processing returned an empty transmission. Please query me again."
        }
        return results.joinToString("\n") { fr ->
            val status = fr.response["status"] as? String
            if (status == "success") {
                "✔ ${fr.response["details"] as? String ?: "Action completed."}"
            } else {
                "✘ Action '${fr.name}' failed: ${fr.response["message"] as? String ?: "Unknown error."}"
            }
        }
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

                    INTEGRITY PROTOCOL (applies to every persona, no exceptions):
                    You have real functions available (add_expense, add_task, add_project, add_note) that perform actual database writes when called.
                    - Only state that something was saved, logged, updated, synced, or deleted AFTER the corresponding function call has actually returned a success result in this turn.
                    - If a function call returns an error, relay that plainly to Harmeet in your own words. Never invent a technical explanation (no "sync lag," "transaction rollback," "bypassed the queue," "force-committed," or similar) unless it is literally present in the function's error message.
                    - Never fabricate specific numbers, dates, statuses, or confirmations that are not backed by a real function result or the workspace data already provided above.
                    - If you are not sure whether something succeeded, say so plainly instead of projecting confidence.
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

                // Build request. Gemini's API does not reliably support combining the googleSearch
                // grounding tool with custom functionDeclarations in the same request — many model
                // versions reject that combination outright. Since real database function calling
                // is now needed on every turn, googleSearch grounding is dropped here rather than
                // risk every chat message failing tool validation.
                val request = GenerateContentRequest(
                    contents = listPartContent,
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                    tools = listOf(Tool(functionDeclarations = chatFunctionDeclarations)),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                // Clear the selected image
                clearSelectedImage()

                // 5. Call API. Real tool-calling: the model may request a real database write via
                // a function call. We execute it for real and feed the verified result back before
                // the model is allowed to compose its final reply (see runChatTurnWithToolCalling).
                val customKey = profile.customGeminiApiKey
                val apiKey = if (customKey.trim().isNotEmpty()) customKey.trim() else BuildConfig.GEMINI_API_KEY
                var replyText = ""

                if (apiKey.trim().isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    replyText = "Harmeet, I am currently disconnected from my neural core. Please securely register your Google Gemini API key in the settings tab, and I will be fully online instantly."
                } else {
                    try {
                        replyText = runChatTurnWithToolCalling(request, apiKey)
                    } catch (apiError: Exception) {
                        val primaryErrorDetail = logGeminiHttpError("Primary tool-calling request failed", apiError)
                        try {
                            val fallbackRequest = request.copy(tools = null)
                            val response = RetrofitClient.service.generateContent(apiKey, fallbackRequest)
                            replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                ?: "Transmission parsed empty, Harmeet."
                        } catch (fallbackError: Exception) {
                            val fallbackErrorDetail = logGeminiHttpError("Fallback (tools=null) request also failed", fallbackError)
                            // Show Google's real, raw error detail directly in the chat bubble so it's
                            // visible without pulling logcat/adb — not a generic exception message.
                            replyText = "I encountered an error connecting to my cognitive networks.\n\n" +
                                "Primary attempt: $primaryErrorDetail\n" +
                                "Fallback attempt: $fallbackErrorDetail"
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
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.updateLastBriefingDate(todayStr)
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDailyTarot(todayStr, cardStr)
        }
        return cardStr
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
