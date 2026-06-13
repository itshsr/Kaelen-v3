package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.KaelenViewModel
import com.example.viewmodel.PendingAction
import com.example.viewmodel.VoiceSuggestion
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorHeader() {
    var activeVariant by remember { ThemeManager.activeVariant }
    var isSideBySide by remember { ThemeManager.isSideBySide }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(24.dp)),
        color = CosmicCard,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val modes = listOf("ARCTIC WOLF", "SIDE-BY-SIDE", "SOLAR WOLF")
            modes.forEach { mode ->
                val isSelected = when (mode) {
                    "ARCTIC WOLF" -> activeVariant == AppThemeVariant.ARCTIC_WOLF && !isSideBySide
                    "SOLAR WOLF" -> activeVariant == AppThemeVariant.SOLAR_WOLF && !isSideBySide
                    else -> isSideBySide
                }
                
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) ElectricCyan else Color.Transparent)
                        .clickable {
                            when (mode) {
                                "ARCTIC WOLF" -> {
                                    activeVariant = AppThemeVariant.ARCTIC_WOLF
                                    isSideBySide = false
                                }
                                "SOLAR WOLF" -> {
                                    activeVariant = AppThemeVariant.SOLAR_WOLF
                                    isSideBySide = false
                                }
                                "SIDE-BY-SIDE" -> {
                                    isSideBySide = true
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        color = if (isSelected) OnPrimaryColor else CoolWhite,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun RenderActiveTab(currentTab: AppTab, viewModel: KaelenViewModel) {
    when (currentTab) {
        AppTab.CHAT -> ChatScreen(viewModel)
        AppTab.BUDGET -> BudgetScreen(viewModel)
        AppTab.TASKS -> TasksScreen(viewModel)
        AppTab.PROGRESS -> ProgressScreen(viewModel)
        AppTab.NOTES -> NotesScreen(viewModel)
        AppTab.PROFILE -> ProfileScreen(viewModel)
    }
}

@Composable
fun MainScreen(viewModel: KaelenViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        bottomBar = {
            KaelenBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = CosmicNavy
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ThemeSelectorHeader()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (ThemeManager.isSideBySide.value) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(BorderStroke(1.dp, Color(0xFFD1D5DB)))
                        ) {
                            MyApplicationTheme(themeVariant = AppThemeVariant.ARCTIC_WOLF) {
                                Surface(color = CosmicNavy, modifier = Modifier.fillMaxSize()) {
                                    RenderActiveTab(currentTab, viewModel)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(BorderStroke(1.dp, Color(0xFFFED7AA)))
                        ) {
                            MyApplicationTheme(themeVariant = AppThemeVariant.SOLAR_WOLF) {
                                Surface(color = CosmicNavy, modifier = Modifier.fillMaxSize()) {
                                    RenderActiveTab(currentTab, viewModel)
                                }
                            }
                        }
                    }
                } else {
                    RenderActiveTab(currentTab, viewModel)
                }

                // Universal Confirmation Dialog Rule
                pendingAction?.let { action ->
                    ConfirmationDialog(
                        action = action,
                        onConfirm = { viewModel.confirmPendingAction() },
                        onDismiss = { viewModel.dismissAction() }
                    )
                }
            }
        }
    }
}

@Composable
fun KaelenBottomNavigation(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = CosmicCard,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTabItem(
                tab = AppTab.CHAT,
                icon = Icons.Default.ChatBubble,
                label = "CORE",
                isSelected = currentTab == AppTab.CHAT,
                onClick = { onTabSelected(AppTab.CHAT) }
            )
            NavigationTabItem(
                tab = AppTab.BUDGET,
                icon = Icons.Default.AccountBalanceWallet,
                label = "VAULT",
                isSelected = currentTab == AppTab.BUDGET,
                onClick = { onTabSelected(AppTab.BUDGET) }
            )
            NavigationTabItem(
                tab = AppTab.TASKS,
                icon = Icons.Default.Check,
                label = "TASKS",
                isSelected = currentTab == AppTab.TASKS,
                onClick = { onTabSelected(AppTab.TASKS) }
            )
            NavigationTabItem(
                tab = AppTab.PROGRESS,
                icon = Icons.Default.Timeline,
                label = "PROJECTS",
                isSelected = currentTab == AppTab.PROGRESS,
                onClick = { onTabSelected(AppTab.PROGRESS) }
            )
            NavigationTabItem(
                tab = AppTab.NOTES,
                icon = Icons.Default.EditNote,
                label = "INTEL",
                isSelected = currentTab == AppTab.NOTES,
                onClick = { onTabSelected(AppTab.NOTES) }
            )
            NavigationTabItem(
                tab = AppTab.PROFILE,
                icon = Icons.Default.Person,
                label = "USER",
                isSelected = currentTab == AppTab.PROFILE,
                onClick = { onTabSelected(AppTab.PROFILE) }
            )
        }
    }
}

@Composable
fun RowScope.NavigationTabItem(
    tab: AppTab,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = ElectricCyan
    val inactiveColor = TextMuted
    val glowColor = if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent

    Column(
        modifier = Modifier
            .height(56.dp)
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(glowColor)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else inactiveColor,
            fontSize = 9.sp
        )
    }
}

// ==================== CHAT SCREEN ====================
@Composable
fun ChatScreen(viewModel: KaelenViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val activeMode by viewModel.activeChatMode.collectAsState()
    val inputVal by viewModel.chatInputText.collectAsState()
    val isSending by viewModel.isSendingChat.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val chatVoiceState by viewModel.chatVoiceState.collectAsState()
    val chatVoiceTranscript by viewModel.chatVoiceTranscript.collectAsState()

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startListening() {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                viewModel.updateChatVoiceTranscript("Tuning neural receiver... Speak now.")
            }

            override fun onBeginningOfSpeech() {
                viewModel.updateChatVoiceTranscript("Listening to wave transmission...")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                viewModel.updateChatVoiceTranscript("Analyzing spectrum...")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                    else -> "Speech system error: $error"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                viewModel.cancelChatVoice()
                recognizer.destroy()
                speechRecognizer = null
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                viewModel.stopChatVoiceAndSend(text)
                recognizer.destroy()
                speechRecognizer = null
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    viewModel.updateChatVoiceTranscript(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        viewModel.startChatVoice()
        try {
            recognizer.startListening(intent)
            speechRecognizer = recognizer
        } catch (e: Exception) {
            Toast.makeText(context, "Could not initialize speech recognizer: ${e.message}", Toast.LENGTH_SHORT).show()
            viewModel.cancelChatVoice()
            recognizer.destroy()
            speechRecognizer = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input.", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // TOP MODE PILLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf("Advise", "Critique", "Counsel")
                modes.forEach { mode ->
                    val isSelected = activeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                BorderStroke(1.dp, if (isSelected) ElectricCyan else BorderColor),
                                RoundedCornerShape(20.dp)
                            )
                            .background(if (isSelected) ElectricCyan else Color.Transparent)
                            .clickable {
                                if (isSelected) viewModel.selectChatMode("Jarvis") else viewModel.selectChatMode(mode)
                            }
                            .padding(vertical = 8.dp)
                            .testTag("mode_pill_$mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.uppercase(),
                            color = if (isSelected) OnPrimaryColor else ElectricCyan,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CHAT WINDOW HISTORIC MESSAGES
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Security Core",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "KAELEN INTELLIGENCE PORTAL",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "System initialized. Talk to me block-to-block, Harmeet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(messages) { message ->
                        ChatMessageBubble(message = message, onLongClick = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "Transmission copied to log clipboard", Toast.LENGTH_SHORT).show()
                        })
                    }
                }

                if (isSending) {
                    item {
                        Text(
                            "KAELEN IS FORMULATING REASONING SYSTEMS...",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
                        )
                    }
                }
            }

            // INPUT FIELD SYSTEM BUTTONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputVal,
                    onValueChange = { viewModel.updateChatInput(it) },
                    placeholder = { Text("Query intelligence core...", color = TextMuted) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("chat_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = ElectricCyan
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CosmicCard,
                        unfocusedContainerColor = CosmicCard,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                        .testTag("chat_input_text")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.sendChatMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = OnPrimaryColor
                    )
                }
            }
        }

        // Gorgeous STT Voice Wavelength Active Overlay
        if (chatVoiceState == "LISTENING") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(BorderStroke(1.dp, ElectricCyan), RoundedCornerShape(16.dp))
                        .testTag("voice_listening_card"),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Pulsing voice interception animation
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Text(
                            "NEURAL VOICE WAVELENGTH INTERCEPT",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(45.dp))
                                .background(ElectricCyan.copy(alpha = 0.15f))
                                .border(BorderStroke(2.dp, ElectricCyan), RoundedCornerShape(45.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Active Microphone Waveform",
                                tint = ElectricCyan,
                                modifier = Modifier.size((48 * scale).dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 150.dp)
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                                .background(CosmicNavy)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = chatVoiceTranscript.ifEmpty { "Calibrating systems... Say your message clearly." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (chatVoiceTranscript.isEmpty()) TextMuted else CoolWhite,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Added simulator/manual input fallback so users in low audio or emulator environments can type and test easily
                        var mockVal by remember { mutableStateOf("") }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = mockVal,
                                onValueChange = { mockVal = it },
                                textStyle = TextStyle(color = CoolWhite, fontSize = 14.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(4.dp))
                                    .background(CosmicNavy)
                                    .padding(8.dp)
                                    .height(36.dp),
                                decorationBox = { innerTextField ->
                                    if (mockVal.isEmpty()) {
                                        Text("Manual fallback key...", color = TextMuted, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (mockVal.trim().isNotEmpty()) {
                                        speechRecognizer?.cancel()
                                        speechRecognizer?.destroy()
                                        speechRecognizer = null
                                        viewModel.stopChatVoiceAndSend(mockVal)
                                    }
                                },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("SIMULATE", color = OnPrimaryColor, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    speechRecognizer?.cancel()
                                    speechRecognizer?.destroy()
                                    speechRecognizer = null
                                    viewModel.cancelChatVoice()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HotPink),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_voice_cancel_button")
                            ) {
                                Text("CANCEL FREQUENCY INTERCEPT", color = CoolWhite, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(message: ChatMessage, onLongClick: () -> Unit) {
    val isUser = message.sender == "user"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        }

        val borderStroke = if (isUser) BorderStroke(1.dp, ElectricCyan) else null
        val backgroundC = if (isUser) Color(0xFF1A3A6A) else CosmicCard

        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .clip(bubbleShape)
                .background(backgroundC)
                .then(if (borderStroke != null) Modifier.border(borderStroke, bubbleShape) else Modifier),
            verticalAlignment = Alignment.Top
        ) {
            // Give Kaelen messages a cyan left accent border
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically)
                        .background(ElectricCyan)
                )
            }
            Column(
                modifier = Modifier
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isUser) "HARMEET" else "KAELEN [${message.mode.uppercase()}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) ElectricCyan else TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolWhite
                )
            }
        }
    }
}

// ==================== BUDGET SCREEN ====================
@Composable
fun BudgetScreen(viewModel: KaelenViewModel) {
    val list by viewModel.expenses.collectAsState()
    val goal by viewModel.monthlyGoal.collectAsState()
    val totalSpent = list.sumOf { it.amount }
    val isOver80 = totalSpent >= goal * 0.8

    var amtVal by remember { mutableStateOf("") }
    var noteVal by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Food") }
    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Other")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // OVERALL STATS & PROGRESS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "BUDGET INTEL DIRECTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "TOTAL SPENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOver80) HotPink else ElectricCyan
                        )
                        Text(
                            "₹${String.format(Locale.getDefault(), "%,.2f", totalSpent)}",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (isOver80) HotPink else CoolWhite
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "MONTHLY CAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            "₹${String.format(Locale.getDefault(), "%,.0f", goal)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CoolWhite
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Gradient Progress Bar
                val progressFraction = if (goal > 0) (totalSpent / goal).toFloat() else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(BorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (isOver80) listOf(HotPink, HotPink) else LocalAppColors.current.primaryGradient
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${(progressFraction * 100).toInt()}% USED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOver80) HotPink else ElectricCyan
                    )
                    if (isOver80) {
                        Text(
                            "WARNING: COGNITIVE OVERSPEND EXCEEDED 80%",
                            style = MaterialTheme.typography.labelSmall,
                            color = HotPink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // INPUTS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "LOG DIRECT EXPENSE",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amtVal,
                    onValueChange = { amtVal = it },
                    label = { Text("Amount (₹)", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("budget_amt_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteVal,
                    onValueChange = { noteVal = it },
                    label = { Text("Expense Note", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("budget_note_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable category select pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selectedCat == cat) DeepViolet else BorderColor)
                                .clickable { selectedCat = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = CoolWhite)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.drop(3).forEach { cat ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selectedCat == cat) DeepViolet else BorderColor)
                                .clickable { selectedCat = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = CoolWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val amt = amtVal.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.requestAction(
                                PendingAction.AddExpense(amount = amt, category = selectedCat, note = noteVal)
                            )
                            amtVal = ""
                            noteVal = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth().testTag("budget_add_button")
                ) {
                    Text("ADD EXPENSE", color = OnPrimaryColor, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIST
        Text(
            "HISTORIC EXPENSES RECORD",
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list) { expense ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DeepViolet)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        expense.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoolWhite,
                                        fontSize = 8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    expense.note,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CoolWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                            Text(
                                sdf.format(Date(expense.timestamp)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "₹${expense.amount}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = HotPink,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        viewModel.requestAction(PendingAction.DeleteExpense(expense))
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== TASKS SCREEN ====================
@Composable
fun TasksScreen(viewModel: KaelenViewModel) {
    val list by viewModel.tasks.collectAsState()
    val pendingTasks = list.filter { !it.isCompleted }
    val completedTasks = list.filter { it.isCompleted }

    var taskTitle by remember { mutableStateOf("") }
    var taskNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "CONSTRUCT TASK OBJECTIVE",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task Title", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = taskNote,
                    onValueChange = { taskNote = it },
                    label = { Text("Task Notes / Description (Optional)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("task_note_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (taskTitle.trim().isNotEmpty()) {
                            viewModel.requestAction(
                                PendingAction.AddTask(title = taskTitle, note = taskNote.takeIf { it.isNotBlank() })
                            )
                            taskTitle = ""
                            taskNote = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth().testTag("task_add_button")
                ) {
                    Text("DEPLOY OBJECTIVE", color = OnPrimaryColor, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTIONS FOR PENDING VS COMPLETED
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PENDING SECTION
            item {
                Text(
                    "PENDING SYSTEM ACTIONS (${pendingTasks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
            }
            if (pendingTasks.isEmpty()) {
                item {
                    Text(
                        "All directories checked. Zero tasks pending, Harmeet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(pendingTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        viewModel = viewModel,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // COMPLETED SECTION
            item {
                Text(
                    "COMPLETED HISTORY ARCHIVE (${completedTasks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
            }
            if (completedTasks.isEmpty()) {
                item {
                    Text(
                        "Zero completed objectives archived.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(completedTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        viewModel = viewModel,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, viewModel: KaelenViewModel, modifier: Modifier = Modifier) {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    
    // Smooth transitions for toggling task state
    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted) TextMuted else CoolWhite,
        animationSpec = tween(durationMillis = 350),
        label = "task_text_color"
    )
    
    val cardOpacity by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.65f else 1.0f,
        animationSpec = tween(durationMillis = 350),
        label = "task_card_opacity"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardOpacity)
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmicCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { checked ->
                    viewModel.requestAction(PendingAction.ToggleTaskComplete(task, checked))
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = ElectricCyan,
                    uncheckedColor = TextMuted,
                    checkmarkColor = OnPrimaryColor
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                task.note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        "Added: ${sdf.format(Date(task.addedDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                    if (task.isCompleted && task.completedDate != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Done: ${sdf.format(Date(task.completedDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            IconButton(
                onClick = { viewModel.requestAction(PendingAction.DeleteTask(task)) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = HotPink
                )
            }
        }
    }
}

// ==================== PROGRESS SCREEN ====================
@Composable
fun ProgressScreen(viewModel: KaelenViewModel) {
    val list by viewModel.projects.collectAsState()

    var projName by remember { mutableStateOf("") }
    var projNote by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Not Started") }
    val statuses = listOf("Not Started", "In Progress", "On Hold", "Completed")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "DEPLOY NEW STRATEGIC PROJECT",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = projName,
                    onValueChange = { projName = it },
                    label = { Text("Project Name", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("proj_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = projNote,
                    onValueChange = { projNote = it },
                    label = { Text("Project Parameters (Optional)", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("proj_note_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Grid for Project Status Inits
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.take(2).forEach { stat ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selectedStatus == stat) ElectricCyan else BorderColor)
                                .clickable { selectedStatus = stat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedStatus == stat) OnPrimaryColor else CoolWhite
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.drop(2).forEach { stat ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selectedStatus == stat) ElectricCyan else BorderColor)
                                .clickable { selectedStatus = stat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedStatus == stat) OnPrimaryColor else CoolWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (projName.trim().isNotEmpty()) {
                            viewModel.requestAction(
                                PendingAction.AddProject(name = projName, status = selectedStatus, note = projNote.takeIf { it.isNotBlank() })
                            )
                            projName = ""
                            projNote = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth().testTag("proj_add_button")
                ) {
                    Text("DEPLOY PROJECT BOARD", color = OnPrimaryColor, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Premium Circular Strategic Dial
        if (list.isNotEmpty()) {
            val totalProjects = list.size
            val completedProjects = list.count { it.status == "Completed" }
            val completionRatio = if (totalProjects > 0) completedProjects.toFloat() / totalProjects else 0f
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CosmicCard)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            "STRATEGY VELOCITY DIAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "PROJECT STATUS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CoolWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$completedProjects OF $totalProjects ACTIONS CLOSED",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val strokeWidth = 10.dp
                        val colorSchemeGradient = LocalAppColors.current.primaryGradient
                        val trackColor = BorderColor
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw background circle track
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                            )
                            // Draw sweeping gradient progress ring
                            val sweepGradient = Brush.sweepGradient(
                                colors = colorSchemeGradient + colorSchemeGradient.first()
                            )
                            drawArc(
                                brush = sweepGradient,
                                startAngle = -90f,
                                sweepAngle = completionRatio * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        Text(
                            text = "${(completionRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = CoolWhite
                        )
                    }
                }
            }
        }

        Text(
            "ACTIVE PROJECT STRATEGY DIRECTORY",
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list) { project ->
                val statusColor = when (project.status) {
                    "Not Started" -> TextMuted
                    "In Progress" -> ElectricCyan
                    "On Hold" -> HotPink
                    "Completed" -> DeepViolet
                    else -> ElectricCyan
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    project.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = CoolWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                project.note?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.requestAction(PendingAction.DeleteProject(project)) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = HotPink
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render status picker inline on actual project board to cycle status (with confirmation!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("STATUS INDEX:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                statuses.forEach { s ->
                                    val isSelected = project.status == s
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) statusColor else BorderColor)
                                            .clickable {
                                                if (project.status != s) {
                                                    viewModel.requestAction(
                                                        PendingAction.UpdateProjectStatus(project, s)
                                                    )
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = s,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) OnPrimaryColor else CoolWhite,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== NOTES SCREEN ====================
@Composable
fun NotesScreen(viewModel: KaelenViewModel) {
    val notesList by viewModel.notes.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val testTranscript by viewModel.voiceTranscript.collectAsState()
    val suggestedItems by viewModel.voiceSuggestions.collectAsState()

    var manualTitle by remember { mutableStateOf("") }
    var manualContent by remember { mutableStateOf("") }

    // Typeable simulator transcript string
    var simulationInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // COGNITIVE VOICE TRANSCRIBER CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "COGNITIVE INTEL SPEECH TRANSCRIPTION",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Securely intercept voice frequencies, filter strategic workspace signals, and automatically distribute to Note, Task or Project boards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (voiceState) {
                    "IDLE" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                                .background(CosmicNavy)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "CONVERSATION INTERCEPT SIMULATOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                BasicTextField(
                                    value = simulationInputText,
                                    onValueChange = { simulationInputText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    textStyle = TextStyle(color = CoolWhite, fontSize = 14.sp),
                                    decorationBox = { innerTextField ->
                                        if (simulationInputText.isEmpty()) {
                                            Text(
                                                "Type oral discussions: e.g. \"Harmeet discussed doing Quantum project checks, completing logs, creating manual details.\"",
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.startVoiceListening()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Mic", tint = OnPrimaryColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LISTEN FEED", color = OnPrimaryColor, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            if (simulationInputText.trim().isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.stopVoiceAndAnalyze(simulationInputText)
                                        simulationInputText = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepViolet),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("PROCESS TEXT", color = CoolWhite, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    "LISTENING" -> {
                        // Pulsing Voice Interception Animation
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(BorderStroke(1.dp, HotPink), RoundedCornerShape(8.dp))
                                .background(CosmicNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Active Mic Pulse",
                                    tint = HotPink,
                                    modifier = Modifier
                                        .size((48 * scale).dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "KAELEN ACTIVE AUDIT HEADSET OPEN...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HotPink,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Default simulation transcript text if mic is not fully attached
                                val defaultTranscript = "Meeting summary: We need to complete building the QA backend project, deploy task check logs and register a Note describing Quantum architecture variables."
                                viewModel.stopVoiceAndAnalyze(defaultTranscript)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("FINALIZE WAVE CAPTURE", color = OnPrimaryColor, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    "PROCESSING" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ElectricCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "ANALYSPHERE DISPERSING DATA VECTORS...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                    "SUGGESTION_READY" -> {
                        Column {
                            Text(
                                "INTELLISENSE DISPATCH RECOMMENDATIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(suggestedItems) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(6.dp)),
                                        colors = CardDefaults.cardColors(containerColor = CosmicNavy)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(DeepViolet)
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            item.type.uppercase(),
                                                            color = CoolWhite,
                                                            fontSize = 7.sp,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        item.title,
                                                        color = CoolWhite,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    item.body,
                                                    color = TextMuted,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.requestAction(
                                                        PendingAction.SaveVoiceSuggestion(
                                                            type = item.type,
                                                            title = item.title,
                                                            body = item.body,
                                                            extra = item.extra
                                                        )
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "CONFIRM",
                                                    color = OnPrimaryColor,
                                                    fontSize = 9.sp,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.dismissVoiceSuggestions() },
                                colors = ButtonDefaults.buttonColors(containerColor = HotPink),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("CLEAR WORKSPACE DISPATCHES", color = CoolWhite)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MANUAL NOTES CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ENLOG MANUAL DIRECT FACT",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualTitle,
                    onValueChange = { manualTitle = it },
                    label = { Text("Note Title", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualContent,
                    onValueChange = { manualContent = it },
                    label = { Text("Content / Facts details", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (manualTitle.trim().isNotEmpty()) {
                            viewModel.requestAction(
                                PendingAction.AddNote(title = manualTitle, content = manualContent)
                            )
                            manualTitle = ""
                            manualContent = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth().testTag("note_add_button")
                ) {
                    Text("SAVE FACT TO MEMORY", color = OnPrimaryColor, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIST
        Text(
            "LOCAL INTEL HISTORY DATA",
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notesList) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                note.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = CoolWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                note.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted
                            )
                        }
                        IconButton(onClick = { viewModel.requestAction(PendingAction.DeleteNote(note)) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = HotPink
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== PROFILE SCREEN ====================
@Composable
fun ProfileScreen(viewModel: KaelenViewModel) {
    val context = LocalContext.current
    val currentProfile by viewModel.userProfile.collectAsState()

    var nameVal by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var roleVal by remember(currentProfile) { mutableStateOf(currentProfile.role) }
    var cityVal by remember(currentProfile) { mutableStateOf(currentProfile.city) }
    var focusProjVal by remember(currentProfile) { mutableStateOf(currentProfile.currentProjects) }
    var prefsVal by remember(currentProfile) { mutableStateOf(currentProfile.preferences) }
    var briefingEnabled by remember(currentProfile) { mutableStateOf(currentProfile.briefingEnabled) }
    var briefingHourVal by remember(currentProfile) { mutableStateOf(currentProfile.briefingHour.toString()) }
    var briefingMinuteVal by remember(currentProfile) { mutableStateOf(currentProfile.briefingMinute.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "HARMEET PERSONAL IDENTITY CONFIGURATION",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameVal,
                    onValueChange = { nameVal = it },
                    label = { Text("Name", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = roleVal,
                    onValueChange = { roleVal = it },
                    label = { Text("Active Role / Designation", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_role_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cityVal,
                    onValueChange = { cityVal = it },
                    label = { Text("Base Location City", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_city_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = focusProjVal,
                    onValueChange = { focusProjVal = it },
                    label = { Text("Active Focus Projects", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_projects_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prefsVal,
                    onValueChange = { prefsVal = it },
                    label = { Text("Cognitive Response Preferences", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_prefs_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "DAILY MORNING BRIEFING SCHEDULE",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Activate Background Scheduler",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolWhite
                    )
                    Switch(
                        checked = briefingEnabled,
                        onCheckedChange = { briefingEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimaryColor,
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CosmicCard
                        ),
                        modifier = Modifier.testTag("briefing_enabled_switch")
                    )
                }

                if (briefingEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = briefingHourVal,
                            onValueChange = { briefingHourVal = it },
                            label = { Text("Hour (0-23)", color = TextMuted) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = CoolWhite,
                                unfocusedTextColor = CoolWhite,
                                focusedIndicatorColor = ElectricCyan,
                                unfocusedIndicatorColor = BorderColor
                            ),
                            modifier = Modifier.weight(1f).testTag("briefing_hour_input")
                        )
                        OutlinedTextField(
                            value = briefingMinuteVal,
                            onValueChange = { briefingMinuteVal = it },
                            label = { Text("Minute (0-59)", color = TextMuted) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = CoolWhite,
                                unfocusedTextColor = CoolWhite,
                                focusedIndicatorColor = ElectricCyan,
                                unfocusedIndicatorColor = BorderColor
                            ),
                            modifier = Modifier.weight(1f).testTag("briefing_minute_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val hour = briefingHourVal.toIntOrNull() ?: 8
                        val minute = briefingMinuteVal.toIntOrNull() ?: 30
                        
                        if (hour !in 0..23 || minute !in 0..59) {
                            Toast.makeText(context, "Invalid time parameters. Please insert hour between 0-23 and minute between 0-59.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val updated = UserProfile(
                            id = 1,
                            name = nameVal,
                            role = roleVal,
                            city = cityVal,
                            currentProjects = focusProjVal,
                            preferences = prefsVal,
                            briefingHour = hour,
                            briefingMinute = minute,
                            briefingEnabled = briefingEnabled
                        )
                        viewModel.requestAction(PendingAction.UpdateProfile(updated))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth().testTag("profile_save_button")
                ) {
                    Text("SYNCHRONIZE PROFILE VECTOR", color = OnPrimaryColor, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger briefing
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "KAELEN COGNITIVE SIGNAL UTILITIES",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Test systemic triggers including high priority push notification morning briefings instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.triggerMorningBriefing() },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Alert", tint = CoolWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TRIGGER SYSTEM BRIEFING NOW", color = CoolWhite, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ==================== UNIVERSAL CONFIRMATION DIALOG ====================
@Composable
fun ConfirmationDialog(
    action: PendingAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val description = when (action) {
        is PendingAction.AddExpense -> "Log secure expense: ₹${action.amount} categorized under ${action.category}?"
        is PendingAction.DeleteExpense -> "Permanently purge expense logged as: \"${action.expense.note}\" totaling ₹${action.expense.amount}?"
        is PendingAction.AddTask -> "Inject action directive: \"${action.title}\" to pending tasks directory?"
        is PendingAction.DeleteTask -> "Purge objective task \"${action.task.title}\" from records?"
        is PendingAction.ToggleTaskComplete -> "Update project objective completed vector status to: ${action.completed}?"
        is PendingAction.AddProject -> "Establish board and register new project path: \"${action.name}\"?"
        is PendingAction.UpdateProjectStatus -> "Shift project progress board status of \"${action.project.name}\" to: ${action.newStatus}?"
        is PendingAction.DeleteProject -> "Erase project trajectory board \"${action.project.name}\" completely?"
        is PendingAction.AddNote -> "Verify fact insert: \"${action.title}\" into core manual knowledge?"
        is PendingAction.DeleteNote -> "Purge local intel note file \"${action.note.title}\"?"
        is PendingAction.UpdateProfile -> "Synchronize profile directories to match updated configuration vector?"
        is PendingAction.SaveVoiceSuggestion -> "Confirm individually parsed intel item. Deposit suggested \"${action.title}\" of type [${action.type.uppercase()}] into database?"
        is PendingAction.ClearChat -> "Purge mental memories and reset chat context logs?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "USER APPROVAL CONFIRMATION REQUIRED",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )
        },
        text = {
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = CoolWhite
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("VERIFY APPROVAL", color = OnPrimaryColor, style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("DISMISS", color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
            }
        },
        containerColor = CosmicCard,
        modifier = Modifier.border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp))
    )
}
