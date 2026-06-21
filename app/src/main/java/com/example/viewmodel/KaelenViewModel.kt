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
