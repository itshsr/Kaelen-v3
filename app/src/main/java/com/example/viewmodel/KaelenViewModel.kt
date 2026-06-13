package com.example.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    CHAT, BUDGET, TASKS, PROGRESS, NOTES, PROFILE
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

class KaelenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KaelenRepository
    init {
        val database = KaelenDatabase.getDatabase(application)
        repository = KaelenRepository(database.appDao)
        initializeProfileIfNeeded()
    }

    // UI Navigation State
    private val _currentTab = MutableStateFlow(AppTab.CHAT)
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

    // Budget Configuration
    private val _monthlyGoal = MutableStateFlow(50000.0) // default 50K
    val monthlyGoal: StateFlow<Double> = _monthlyGoal.asStateFlow()

    fun updateMonthlyGoal(goal: Double) {
        _monthlyGoal.value = goal
    }

    // Confirmation Screen State
    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    fun requestAction(action: PendingAction) {
        _pendingAction.value = action
    }

    fun dismissAction() {
        _pendingAction.value = null
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
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
                    }
                    is PendingAction.AddTask -> {
                        repository.insertTask(Task(title = action.title, note = action.note))
                    }
                    is PendingAction.DeleteTask -> {
                        repository.deleteTask(action.task)
                    }
                    is PendingAction.ToggleTaskComplete -> {
                        val updated = action.task.copy(
                            isCompleted = action.completed,
                            completedDate = if (action.completed) System.currentTimeMillis() else null
                        )
                        repository.insertTask(updated)
                    }
                    is PendingAction.AddProject -> {
                        repository.insertProject(Project(name = action.name, status = action.status, note = action.note))
                    }
                    is PendingAction.UpdateProjectStatus -> {
                        val updated = action.project.copy(status = action.newStatus)
                        repository.insertProject(updated)
                    }
                    is PendingAction.DeleteProject -> {
                        repository.deleteProject(action.project)
                    }
                    is PendingAction.AddNote -> {
                        repository.insertNote(Note(title = action.title, content = action.content))
                    }
                    is PendingAction.DeleteNote -> {
                        repository.deleteNote(action.note)
                    }
                    is PendingAction.UpdateProfile -> {
                        repository.insertUserProfile(action.profile)
                        val context = getApplication<android.app.Application>().applicationContext
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
                        when (action.type.lowercase().trim()) {
                            "note" -> repository.insertNote(Note(title = action.title, content = action.body))
                            "task" -> repository.insertTask(Task(title = action.title, note = action.body))
                            "project" -> repository.insertProject(Project(name = action.title, status = action.extra ?: "In Progress", note = action.body))
                        }
                        // Remove suggestion after confirm
                        _voiceSuggestions.update { list ->
                            list.filterNot { it.title == action.title && it.body == action.body }
                        }
                    }
                    is PendingAction.ClearChat -> {
                        repository.clearChatHistory()
                    }
                }
            } catch (e: Exception) {
                Log.e("KaelenViewModel", "Error in pending action execution: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _pendingAction.value = null
                }
            }
        }
    }

    // Chat Screen State
    private val _activeChatMode = MutableStateFlow("Jarvis") // Jarvis, Advise, Critique, Counsel
    val activeChatMode: StateFlow<String> = _activeChatMode.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText: StateFlow<String> = _chatInputText.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    fun updateChatInput(text: String) {
        _chatInputText.value = text
    }

    fun selectChatMode(mode: String) {
        _activeChatMode.value = mode
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
                val userMsg = ChatMessage(text = messageText, sender = "user", mode = _activeChatMode.value)
                repository.insertChatMessage(userMsg)

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
                    "Advise" -> "STRATEGIC & ADVOCATING mode. Deliver sharp, structured, business-minded recommendations. Focus on structural solutions, long-term options, and key opportunities. Keep it organized using lists and clear steps."
                    "Critique" -> "BRUTAL & DIRECT critique mode. Be extremely direct and clear. Challenge assumptions, find logical flaws in Harmeet's spending or planning, and state facts of issues with dry wit. Avoid sugarcoating."
                    "Counsel" -> "WARM COUNSEL mode. Explore the issue from multiple human, psychological, and emotional angles. Be warm, supportive, asking reflective questions to let Harmeet discover his best path."
                    else -> "DEFAULT JAVIS PERSONALITY. Address Harmeet respectfully block-to-block, use time of day markers naturally, deliver clever wit occasionally, and always wrap structural intelligence into any conversation. Never present problems without immediately stating actionable solutions."
                }

                val systemPrompt = """
                    You are KAELEN, a premium AI companion designed with a warm, intelligent, Jarvis-like personality. 
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
                // Add the current query
                listPartContent.add(Content(parts = listOf(Part(text = "user: $messageText"))))

                // Build request
                val request = GenerateContentRequest(
                    contents = listPartContent,
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                    tools = listOf(mapOf("googleSearch" to emptyMap())),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                // 5. Call API
                val apiKey = BuildConfig.GEMINI_API_KEY
                var replyText = ""
                
                if (apiKey.trim().isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    replyText = "Harmeet, I am currently disconnected from my neural core. Please securely register your Google Gemini API key in the Secrets Panel in AI Studio, and I will be fully online instantly."
                } else {
                    try {
                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                            ?: "Harmeet, my core processing returned an empty transmission. Please query me again."
                    } catch (apiError: Exception) {
                        Log.e("KaelenViewModel", "Gemini API Connection failed", apiError)
                        // Try fallback without Search Grounding if API key quota/type restricts it
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

                // 6. Save KAELEN's reply to DB
                repository.insertChatMessage(
                    ChatMessage(text = replyText, sender = "kaelen", mode = _activeChatMode.value)
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
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.trim().isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Fallback structured simulation if offline
                simulateVoiceCategorization(textToAnalyze)
                return@launch
            }

            val schemaPrompt = """
                You are a smart organizational parsing assistant.
                You are given a text conversation transcript captured from a user's speech.
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
                
                // Parse JSON
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
                // Fallback simulation
                simulateVoiceCategorization(textToAnalyze)
            }
        }
    }

    private fun simulateVoiceCategorization(rawText: String) {
        viewModelScope.launch(Dispatchers.Default) {
            // Generate standard smart simulation based on input keywords
            val lower = rawText.lowercase()
            val suggestions = mutableListOf<VoiceSuggestion>()

            if (lower.contains("project") || lower.contains("launch") || lower.contains("build")) {
                suggestions.add(VoiceSuggestion("Project", "Quantum Launch Core", "Manage core launch timelines highlighted in discussions.", "In Progress"))
            }
            if (lower.contains("task") || lower.contains("todo") || lower.contains("call") || lower.contains("buy") || lower.contains("check")) {
                suggestions.add(VoiceSuggestion("Task", "Check QA Bug Backlog", "Review the checklist matching current logs.", null))
            }
            // Always have at least one note suggestion
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

    // Initializer to ensure profile is not empty
    private fun initializeProfileIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getUserProfileOneOff()
            if (existing == null) {
                val defaultProfile = UserProfile()
                repository.insertUserProfile(defaultProfile)
                val context = getApplication<android.app.Application>().applicationContext
                com.example.scheduler.BriefingScheduler.scheduleDailyBriefing(
                    context,
                    defaultProfile.briefingHour,
                    defaultProfile.briefingMinute
                )
            } else if (existing.briefingEnabled) {
                val context = getApplication<android.app.Application>().applicationContext
                com.example.scheduler.BriefingScheduler.scheduleDailyBriefing(
                    context,
                    existing.briefingHour,
                    existing.briefingMinute
                )
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

    // Trigger instant Mock Morning Briefing
    fun triggerMorningBriefing() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val text = "$greeting, Harmeet. I have prepared your morning intelligence briefing. Check your active projects and task directories to plan your strategy."
        sendLocalNotification("Morning Briefing", text)
    }
}
