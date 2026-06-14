package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: KaelenViewModel) {
    val isReady by viewModel.isReady.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    if (!isReady || userProfile.id != 1) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F15)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF6B00),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "KAELEN COGNITIVE NUCLEUS BOOTING...",
                    color = Color(0xFFFF6B00),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
        return
    }

    val currentTab by viewModel.currentTab.collectAsState()
    
    // Choose active style dynamically based on theme preset stored in UserProfile
    val themeVariant = when (userProfile.selectedTheme.uppercase()) {
        "SOVEREIGN" -> AppThemeVariant.SOVEREIGN
        "NEXUS" -> AppThemeVariant.NEXUS
        "APEX" -> AppThemeVariant.APEX
        else -> AppThemeVariant.INFERNO
    }
    
    MyApplicationTheme(themeVariant = themeVariant) {
        val palette = LocalAppColors.current
        val animatedBg by animateColorAsState(targetValue = palette.bg)
        
        Scaffold(
            bottomBar = {
                KaelenBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            },
            containerColor = animatedBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                animatedBg,
                                palette.card.copy(alpha = 0.4f)
                            )
                        )
                    )
            ) {
                // Main Content Screen Router
                when (currentTab) {
                    AppTab.HOME -> DashboardScreen(viewModel, userProfile)
                    AppTab.CORE -> ChatScreen(viewModel, userProfile)
                    AppTab.FORGE -> ForgeScreen(viewModel)
                    AppTab.ORACLE -> OracleScreen(viewModel, userProfile)
                    AppTab.GRIMOIRE -> GrimoireScreen(viewModel)
                    AppTab.VAULT -> VaultScreen(viewModel)
                    AppTab.USER -> UserSettingsScreen(viewModel, userProfile)
                }
            }
        }
    }
}

// 7 Tabs Bottom Navigation Bar conforming with Edge-to-Edge window insets padding
@Composable
fun KaelenBottomNavigation(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val palette = LocalAppColors.current
    Surface(
        color = palette.card,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Ensures edge protection
            .border(BorderStroke(1.dp, palette.border.copy(alpha = 0.4f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                NavigationTab(AppTab.HOME, Icons.Default.Home, "HOME"),
                NavigationTab(AppTab.CORE, Icons.Default.Chat, "CORE"),
                NavigationTab(AppTab.FORGE, Icons.Default.Build, "FORGE"),
                NavigationTab(AppTab.ORACLE, Icons.Default.AutoAwesome, "ORACLE"),
                NavigationTab(AppTab.GRIMOIRE, Icons.Default.Book, "GRIMOIRE"),
                NavigationTab(AppTab.VAULT, Icons.Default.AccountBalanceWallet, "VAULT"),
                NavigationTab(AppTab.USER, Icons.Default.Person, "USER")
            )
            
            tabs.forEach { navTab ->
                val isSelected = currentTab == navTab.tab
                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) palette.primary else palette.muted
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(navTab.tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = navTab.icon,
                        contentDescription = navTab.label,
                        tint = tintColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = navTab.label,
                        color = tintColor,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class NavigationTab(
    val tab: AppTab,
    val icon: ImageVector,
    val label: String
)

// ==========================================
// 1. HOME tab dashboard implementation
// ==========================================
@Composable
fun DashboardScreen(viewModel: KaelenViewModel, profile: UserProfile) {
    val palette = LocalAppColors.current
    val tasks by viewModel.tasks.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val goal by viewModel.monthlyGoal.collectAsState()
    val habits by viewModel.habitsList.collectAsState()
    val focusStreak by viewModel.focusStreak.collectAsState()
    
    val pendingTasks = tasks.filter { !it.isCompleted }.size
    val totalSpent = expenses.sumOf { it.amount }
    
    // Formatting date and time
    val calendar = Calendar.getInstance()
    val dateSdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    val formattedDate = dateSdf.format(calendar.time)
    
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val greetingText = when {
        currentHour < 12 -> "Good morning, ${profile.name}"
        currentHour < 17 -> "Good afternoon, ${profile.name}"
        else -> "Good evening, ${profile.name}"
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Time, Day and Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = greetingText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.text,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        
        // KAELEN's Morning Briefing Core Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, palette.primary.copy(alpha = 0.2f)), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = palette.card),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(palette.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KAELEN COGNITIVE BRIEF",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hello Harmeet. The core networks are fully aligned. You have $pendingTasks critical items in Forge tracker. Basim notes today's tarot holds exceptional strategy markers. Let's execute block-to-block progress.",
                        color = palette.text.copy(alpha = 0.9f),
                        lineHeight = 20.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // BASIM's Tarot Card Pull Of the Day
        item {
            val dailyTarotStr = viewModel.getOrDrawTarotCardOfTheDay(profile)
            val parts = dailyTarotStr.split(":")
            val cardName = parts.firstOrNull() ?: "The Fool"
            val cardMeaning = parts.getOrNull(1) ?: "A new starting focus awaits your forge execution."
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, palette.tertiary.copy(alpha = 0.2f)), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = palette.card),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "BASIM'S TAROT OF THE DAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🃏 $cardName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cardMeaning.trim(),
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        
        // Activity & Habit circular progress gauges row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Habit Completion gauge
                val totalHabits = habits.size
                val completedHabits = habits.count { h ->
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    h.completedDates.contains(todayStr)
                }
                val habitsRatio = if (totalHabits > 0) completedHabits.toFloat() / totalHabits.toFloat() else 0f
                
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "HABIT RINGS",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier.size(70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = palette.border.copy(alpha = 0.3f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = palette.primary,
                                    startAngle = -90f,
                                    sweepAngle = habitsRatio * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(habitsRatio * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.text
                            )
                        }
                        Text(
                            text = "$completedHabits of $totalHabits Complete",
                            fontSize = 11.sp,
                            color = palette.muted
                        )
                    }
                }
                
                // Focus Streak count gauge
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FOCUS STREAK",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier.size(70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val activeProgress = focusStreak.coerceAtMost(5).toFloat() / 5f
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = palette.border.copy(alpha = 0.3f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = palette.tertiary,
                                    startAngle = -90f,
                                    sweepAngle = activeProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "🔥 $focusStreak",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.text
                            )
                        }
                        Text(
                            text = "Sessions accomplished today",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = palette.muted
                        )
                    }
                }
            }
        }
        
        // Ledger Budget spent capsule
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.card)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "VAULT SPENDING LEDGER",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${totalSpent.toInt()} / ₹${goal.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.text
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val spendRatio = if (goal > 0) (totalSpent / goal).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { spendRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (spendRatio >= 0.8f) Color.Red else palette.primary,
                        trackColor = palette.border.copy(alpha = 0.3f)
                    )
                }
            }
        }
        
        // Fast Action Buttons
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "QUICK LINKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.selectTab(AppTab.CORE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Text("Talk to KAELEN", color = OnPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.selectTab(AppTab.FORGE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.secondary)
                    ) {
                        Text("Start Focus", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.selectTab(AppTab.GRIMOIRE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.tertiary)
                    ) {
                        Text("Log Habit", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. CORE tab AI Chat Screen with agent selector, voice, camera simulation
// ==========================================
@Composable
fun ChatScreen(viewModel: KaelenViewModel, profile: UserProfile) {
    val palette = LocalAppColors.current
    val activeChatMode by viewModel.activeChatMode.collectAsState()
    val chatInputText by viewModel.chatInputText.collectAsState()
    val isSendingChat by viewModel.isSendingChat.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    
    // Voice status details
    val chatVoiceState by viewModel.chatVoiceState.collectAsState()
    
    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    
    // Camera file picker
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectImage(uri.toString(), "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=") // Simulated sample Base64
            Toast.makeText(context, "Hand Camera image integrated successfully.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Horizontal Scrollable Agent Selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val agents = listOf("KAELEN", "VERGIL", "MADARA", "KAKASHI", "BASIM", "EZIO", "KRATOS", "DANTE")
            agents.forEach { agent ->
                val isSelected = activeChatMode == agent
                val chipColor = if (isSelected) {
                    when (agent) {
                        "KAELEN" -> palette.primary
                        "BASIM" -> palette.tertiary
                        else -> palette.secondary
                    }
                } else {
                    palette.card
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipColor)
                        .clickable { viewModel.selectChatMode(agent) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = agent,
                        color = if (isSelected) Color.White else palette.text.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        // Chat dialogues column
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val messagesToShow = chatMessages.filter { it.mode == activeChatMode || it.mode == "DEFAULT" }.reversed()
            
            if (messagesToShow.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "State synchronized. Ask anything from $activeChatMode agent.",
                            color = palette.muted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            items(messagesToShow) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .border(
                                BorderStroke(1.dp, if (isUser) palette.primary.copy(alpha = 0.3f) else palette.border.copy(alpha = 0.3f)),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) palette.primary.copy(alpha = 0.15f) else palette.card
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = if (isUser) "Harmeet" else activeChatMode,
                                color = if (isUser) palette.primary else palette.tertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                color = palette.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        
        // Simulated selected camera attachments indicator
        if (selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.card.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Multimodal Hand Camera attachment active", color = palette.text, fontSize = 11.sp)
                }
                IconButton(onClick = { viewModel.clearSelectedImage() }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Chat bottom dialogue controller
        Surface(
            color = palette.card,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (chatVoiceState == "LISTENING") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🎤 Listening to voice input...", color = palette.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row {
                            Button(
                                onClick = { 
                                    // Trigger simple voice transcript simulate scripts
                                    val simulatedResult = when(activeChatMode) {
                                        "BASIM" -> "Analyze palm lines for destiny alignment."
                                        "EZIO" -> "Explain the summary outline of Book Odyssey."
                                        else -> "Start forge sprint for 25 minutes"
                                    }
                                    viewModel.stopChatVoiceAndSend(simulatedResult)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Speak done", color = Color.White, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { viewModel.cancelChatVoice() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red)
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Camera Button
                        IconButton(
                            onClick = { imageLauncher.launch("image/*") },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = palette.primary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera Integration")
                        }
                        
                        // Text input field
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { viewModel.updateChatInput(it) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 80.dp),
                            placeholder = { Text("Message $activeChatMode...", color = palette.muted, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text,
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.border
                            ),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Speak Microphone Button
                        IconButton(
                            onClick = { viewModel.startChatVoice() },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = palette.tertiary)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                        }
                        
                        // Send Button
                        IconButton(
                            onClick = {
                                viewModel.sendChatMessage()
                                keyboardController?.hide()
                            },
                            enabled = chatInputText.trim().isNotEmpty() && !isSendingChat,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = palette.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send message")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. FORGE tab Screen: Tasks, Projects & Focus Session timer
// ==========================================
@Composable
fun ForgeScreen(viewModel: KaelenViewModel) {
    val palette = LocalAppColors.current
    val tasks by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val focusStreak by viewModel.focusStreak.collectAsState()
    
    var subTab by remember { mutableStateOf("TASKS") } // TASKS, PROJECTS, TIMER
    
    val context = LocalContext.current
    var newTaskTitle by remember { mutableStateOf("") }
    var newProjName by remember { mutableStateOf("") }
    var newProjNote by remember { mutableStateOf("") }
    
    // Focus Timer States representation
    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(1500) } // 25 Min
    var isBreakMode by remember { mutableStateOf(false) }
    
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                timerSeconds--
            }
            // completed timer triggers automatic increment
            if (!isBreakMode) {
                viewModel.logFocusSession()
                Toast.makeText(context, "Focus session accomplished! Streak updated in Room DB.", Toast.LENGTH_LONG).show()
                isBreakMode = true
                timerSeconds = 300 // 5 min break
            } else {
                Toast.makeText(context, "Break over! Time to construct code.", Toast.LENGTH_SHORT).show()
                isBreakMode = false
                timerSeconds = 1500
            }
            timerRunning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Forge Custom selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(24.dp))
                .background(palette.card),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("TASKS", "PROJECTS", "FOCUS TIMER").forEach { t ->
                val isSel = subTab == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) palette.primary else Color.Transparent)
                        .clickable { subTab = t }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t,
                        color = if (isSel) Color.White else palette.text.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        when (subTab) {
            "TASKS" -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            placeholder = { Text("Define new task...", color = palette.muted) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text,
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.border
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTaskTitle.trim().isNotEmpty()) {
                                    viewModel.requestAction(PendingAction.AddTask(newTaskTitle.trim(), null))
                                    newTaskTitle = ""
                                    Toast.makeText(context, "Task added directly to Room.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Text("SAVE", color = OnPrimaryColor)
                        }
                    }
                }
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val activeTasks = tasks.filter { !it.isCompleted }
                    val complTasks = tasks.filter { it.isCompleted }
                    
                    item {
                        Text("ACTIVE FORGE ITEMS", style = MaterialTheme.typography.titleSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                    if (activeTasks.isEmpty()) {
                        item { Text("No active tasks. Forge some triggers.", color = palette.muted, fontSize = 12.sp) }
                    } else {
                        items(activeTasks) { t ->
                            TaskRow(t, viewModel)
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("COMPLETED ITEMS", style = MaterialTheme.typography.titleSmall, color = palette.muted, fontWeight = FontWeight.Bold)
                    }
                    if (complTasks.isEmpty()) {
                        item { Text("No completed items yet.", color = palette.muted, fontSize = 12.sp) }
                    } else {
                        items(complTasks) { t ->
                            TaskRow(t, viewModel)
                        }
                    }
                }
            }
            "PROJECTS" -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("NEW STRATEGIC PROJECT BOARD", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newProjName,
                            onValueChange = { newProjName = it },
                            placeholder = { Text("Project Title", color = palette.muted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text,
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.border
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newProjNote,
                            onValueChange = { newProjNote = it },
                            placeholder = { Text("Details & Objectives", color = palette.muted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text,
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.border
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newProjName.trim().isNotEmpty()) {
                                    viewModel.requestAction(PendingAction.AddProject(newProjName.trim(), "In Progress", newProjNote.trim()))
                                    newProjName = ""
                                    newProjNote = ""
                                    Toast.makeText(context, "System board initialized.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("DEPLOY PROJECT", color = OnPrimaryColor)
                        }
                    }
                }
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (projects.isEmpty()) {
                        item { Text("No active boards.", color = palette.muted, fontSize = 12.sp) }
                    } else {
                        items(projects) { proj ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = palette.card)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(proj.name, fontWeight = FontWeight.Bold, color = palette.text)
                                        if (!proj.note.isNullOrEmpty()) {
                                            Text(proj.note, color = palette.muted, fontSize = 11.sp)
                                        }
                                        Text("Status: ${proj.status}", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            val nextStatus = when (proj.status) {
                                                "Not Started" -> "In Progress"
                                                "In Progress" -> "Completed"
                                                else -> "Not Started"
                                            }
                                            viewModel.requestAction(PendingAction.UpdateProjectStatus(proj, nextStatus))
                                        }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Update Status", tint = palette.primary)
                                        }
                                        IconButton(onClick = { viewModel.requestAction(PendingAction.DeleteProject(proj)) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "FOCUS TIMER" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isBreakMode) "REST INTERVAL" else "POMODORO CONCENTRATION",
                            color = palette.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val maxSecs = if (isBreakMode) 300 else 1500
                            val pct = timerSeconds.toFloat() / maxSecs.toFloat()
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = palette.border.copy(alpha = 0.2f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = if (isBreakMode) palette.secondary else palette.primary,
                                    startAngle = -90f,
                                    sweepAngle = pct * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            
                            val minutes = timerSeconds / 60
                            val seconds = timerSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.text
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { timerRunning = !timerRunning },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                            ) {
                                Text(if (timerRunning) "PAUSE" else "START FOCUS", color = OnPrimaryColor)
                            }
                            Button(
                                onClick = {
                                    timerRunning = false
                                    timerSeconds = if (isBreakMode) 300 else 1500
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.border)
                            ) {
                                Text("RESET", color = palette.text)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Today's Total Focus Sessions Completed: $focusStreak", color = palette.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, viewModel: KaelenViewModel) {
    val palette = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.card)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { checked ->
                        viewModel.requestAction(PendingAction.ToggleTaskComplete(task, checked))
                    },
                    colors = CheckboxDefaults.colors(checkedColor = palette.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = task.title,
                    color = if (task.isCompleted) palette.muted else palette.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            IconButton(onClick = { viewModel.requestAction(PendingAction.DeleteTask(task)) }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ==========================================
// 4. ORACLE tab screen (BASIM's cosmic and astrological space)
// ==========================================
@Composable
fun OracleScreen(viewModel: KaelenViewModel, profile: UserProfile) {
    val palette = LocalAppColors.current
    val context = LocalContext.current
    var activeSubOption by remember { mutableStateOf("TAROT") } // TAROT, KUNDLI, ASTROLOGY, VASTU, NUMEROLOGY, PALMISTRY
    
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Hand Palm image successfully loaded. Basim is analyzing alignments...", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "BASIM'S ORACLE & COSMIC NETWORK",
            style = MaterialTheme.typography.titleSmall,
            color = palette.tertiary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            listOf("TAROT", "KUNDLI", "ASTROLOGY", "VASTU", "NUMEROLOGY", "PALMISTRY").forEach { key ->
                val isSel = activeSubOption == key
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) palette.tertiary else palette.card)
                        .clickable { activeSubOption = key }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = key,
                        color = if (isSel) Color.Black else palette.text.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Oracle content viewport
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            colors = CardDefaults.cardColors(containerColor = palette.card)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (activeSubOption) {
                    "TAROT" -> {
                        Text("3-CARD SPREAD & CELTIC SPREAD", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Past • Present • Future alignment index:\n\n" +
                                    "1. PAST: III - The Empress (Abundance) - Strategic roots established Harmeet's directives.\n" +
                                    "2. PRESENT: I - The Magician (Creation) - Your forge activities hold instant generation power.\n" +
                                    "3. FUTURE: VIII - Strength (Force) - Supreme architectural implementation wins the sprint.",
                            color = palette.text,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { Toast.makeText(context, "Celtic Cross spread mapped in basim logs.", Toast.LENGTH_SHORT).show() },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.tertiary)
                        ) {
                            Text("Draw Celtic Cross Spread", color = Color.Black)
                        }
                    }
                    "KUNDLI" -> {
                        Text("KUNDLI BIRTH MATRIX", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Derived from profile detail coordinates: ${profile.birthDate} @ ${profile.birthTime} Place: ${profile.birthPlace}", color = palette.muted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Kundli representation grid
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .align(Alignment.CenterHorizontally)
                                .border(BorderStroke(2.dp, palette.primary))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, size.height))
                                drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, 0f))
                                drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(size.width/2f, 0f), end = androidx.compose.ui.geometry.Offset(0f, size.height/2f))
                                drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(size.width/2f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2f))
                                drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(0f, size.height/2f), end = androidx.compose.ui.geometry.Offset(size.width/2f, size.height))
                                drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(size.width/2f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2f))
                            }
                            Text("Sun (Asc)", modifier = Modifier.align(Alignment.Center), color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Rashi planetary positioning: Jupiter in 11th House of gains (Excellent for technology role). Moon transit indicates mental sprint capability.",
                            color = palette.text,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    "ASTROLOGY" -> {
                        Text("ASTROLOGICAL TIMELINES", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🔹 DAILY HOROSCOPE: A perfect alignment of Mercury speeds up your analytical code integration today.\n\n" +
                                    "🔹 WEEKLY FORECAST: Excellent communication index. Perfect time for slides structure copy edits.\n\n" +
                                    "🔹 MONTHLY SPECTRUM: Great Saturn placement ensures that direct database writes of budget records succeed firmly.",
                            color = palette.text,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    "VASTU" -> {
                        Text("VASTU ROOM POSITION ADVISORY", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🚪 WORKPLACE / OFFICE (North-East): Align your monitors and desk towards the North-East axis to boost computational logic and clarity.\n\n" +
                                    "🍳 KITCHEN (South-East): Southern fire elements represent strong engine generation index.\n\n" +
                                    "🛌 BEDROOM (South-West): Ensures peaceful rest after high-energy Forge sprint sessions.",
                            color = palette.text,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    "NUMEROLOGY" -> {
                        Text("NUMEROLOGY MATRIX KEY", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Calculating numbers based on birthdate 1995-11-20
                        Text(
                            text = "🔢 LIFE PATH NUMBER: 1 (The Leader / Visionary)\n" +
                                    "Formed from 1995-11-20 -> 1+9+9+5+1+1+2+0 = 28 -> 2+8 = 10 -> 1.\n\n" +
                                    "🔢 DESTINY NUMBER: 3 (The Strategist / Creator)\n" +
                                    "Calculated from name Harmeet -> stands for supreme expression and focus vector.\n\n" +
                                    "🔢 SOUL URGE NUMBER: 7 (Analytical Intellect)\n" +
                                    "Reflects your internal pursuit of ultimate structural intelligence.",
                            color = palette.text,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                    "PALMISTRY" -> {
                        Text("PALMISTRY READING BY CAMERA", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Upload a photo of your palm. Basim will scan your Heart Line, Life Line and Head Line to deliver cryptic guidance.",
                            fontSize = 12.sp,
                            color = palette.text
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { imageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.tertiary)
                        ) {
                            Text("Open Hand Palm Scanner", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. GRIMOIRE tab Screen: Notes, Knowledge Base, Habits, Ebook Reader
// ==========================================
@Composable
fun GrimoireScreen(viewModel: KaelenViewModel) {
    val palette = LocalAppColors.current
    val notes by viewModel.notes.collectAsState()
    val habits by viewModel.habitsList.collectAsState()
    val ebooks by viewModel.ebooksList.collectAsState()
    
    var subTabMode by remember { mutableStateOf("NOTES") } // NOTES, HABITS, READER, KNOWLEDGE
    val context = LocalContext.current
    
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }
    var noteSearchQuery by remember { mutableStateOf("") }
    
    // Reader configuration
    var activeReadBook by remember { mutableStateOf<Ebook?>(null) }
    var readerFontSize by remember { mutableStateOf(14) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            listOf("NOTES", "HABITS", "EBOOK READER", "KNOWLEDGE BASE").forEach { tab ->
                val isSel = subTabMode == tab
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) palette.primary else palette.card)
                        .clickable { subTabMode = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isSel) Color.White else palette.text.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        when (subTabMode) {
            "NOTES" -> {
                // Search Input
                OutlinedTextField(
                    value = noteSearchQuery,
                    onValueChange = { noteSearchQuery = it },
                    placeholder = { Text("Search grimoire notes...", color = palette.muted) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.border
                    )
                )
                
                // Add Note Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("LOG COGNITIVE FACT", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            placeholder = { Text("Note Title", color = palette.muted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text,
                                focusedBorderColor = palette.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = noteBody,
                            onValueChange = { noteBody = it },
                            placeholder = { Text("Content...", color = palette.muted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text,
                                focusedBorderColor = palette.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (noteTitle.trim().isNotEmpty()) {
                                    viewModel.requestAction(PendingAction.AddNote(noteTitle.trim(), noteBody.trim()))
                                    noteTitle = ""
                                    noteBody = ""
                                    Toast.makeText(context, "Fact preserved in Room.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("SAVE FACT", color = OnPrimaryColor)
                        }
                    }
                }
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filteredNotes = notes.filter {
                        noteSearchQuery.isEmpty() || it.title.contains(noteSearchQuery, ignoreCase = true) || it.content.contains(noteSearchQuery, ignoreCase = true)
                    }
                    items(filteredNotes) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = palette.card)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(note.title, fontWeight = FontWeight.Bold, color = palette.text)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(note.content, color = palette.muted, fontSize = 11.sp)
                                }
                                IconButton(onClick = { viewModel.requestAction(PendingAction.DeleteNote(note)) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
            "HABITS" -> {
                var newHabitName by remember { mutableStateOf("") }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newHabitName,
                            onValueChange = { newHabitName = it },
                            placeholder = { Text("Log habit trace...", color = palette.muted) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.text,
                                focusedBorderColor = palette.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newHabitName.trim().isNotEmpty()) {
                                    viewModel.addHabit(newHabitName.trim())
                                    newHabitName = ""
                                    Toast.makeText(context, "Habit trace registered.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Text("ADD", color = OnPrimaryColor)
                        }
                    }
                }
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(habits) { h ->
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val isCompletedToday = h.completedDates.contains(todayStr)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = palette.card)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = isCompletedToday,
                                        onCheckedChange = { viewModel.toggleHabit(h.name) },
                                        colors = CheckboxDefaults.colors(checkedColor = palette.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(h.name, fontWeight = FontWeight.Bold, color = if (isCompletedToday) palette.muted else palette.text)
                                        Text("Streak today: ${h.streak} days", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteHabit(h.name) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Habit", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
            "EBOOK READER" -> {
                if (activeReadBook != null) {
                    val book = activeReadBook!!
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = palette.card)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(book.title, fontWeight = FontWeight.Bold, color = palette.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                IconButton(onClick = { activeReadBook = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.text)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Font adjustment toolbar
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Font Size:", color = palette.muted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A-", modifier = Modifier.clickable { readerFontSize = maxOf(10, readerFontSize - 2) }.padding(4.dp), color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(readerFontSize.toString(), color = palette.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A+", modifier = Modifier.clickable { readerFontSize = minOf(24, readerFontSize + 2) }.padding(4.dp), color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Book body text content
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Chapter ${book.lastReadPosition / 10 + 1}\n\nThis volume represents classic strategic wisdom formulated by historical minds. Alignment is central to block-by-block structures.\n\nExecute clear logical schedules, secure cash reservoirs, and let BASIM charts guide timing lines.",
                                    fontSize = readerFontSize.sp,
                                    color = palette.text,
                                    lineHeight = (readerFontSize + 6).sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Discuss book with Ezio button
                            Button(
                                onClick = {
                                    viewModel.selectChatMode("EZIO")
                                    viewModel.updateChatInput("Let's discuss Chapter summary of ${book.title}")
                                    viewModel.selectTab(AppTab.CORE)
                                    Toast.makeText(context, "Ezio is ready to discuss.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.secondary),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text("Ask EZIO about this volume", color = Color.White)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Page Progress: ${book.lastReadPosition} / ${book.totalPages}", color = palette.muted, fontSize = 11.sp)
                                Row {
                                    IconButton(
                                        onClick = { if (book.lastReadPosition > 1) viewModel.updateBookProgress(book.title, book.lastReadPosition - 1) }
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev")
                                    }
                                    IconButton(
                                        onClick = { if (book.lastReadPosition < book.totalPages) viewModel.updateBookProgress(book.title, book.lastReadPosition + 1) }
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column {
                        // Simulated EPUB/PDF Book Import
                        Button(
                            onClick = {
                                viewModel.importBook("Ezio's Renaissance Writing Handbook", "EPUB")
                                Toast.makeText(context, "Renaissance EPUB file imported dynamically.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text("📥 Import document from device storage (PDF / EPUB)", color = OnPrimaryColor)
                        }
                        
                        Text("LIBRARY CONSOLE", style = MaterialTheme.typography.titleSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ebooks) { book ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeReadBook = book },
                                    colors = CardDefaults.cardColors(containerColor = palette.card)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(book.coverIcon, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(book.title, fontWeight = FontWeight.Bold, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("By ${book.author} • ${book.format}", color = palette.muted, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { book.progress / 100f },
                                                color = palette.primary,
                                                trackColor = palette.border.copy(alpha = 0.3f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${book.progress}%", fontWeight = FontWeight.Bold, color = palette.primary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "KNOWLEDGE BASE" -> {
                Column {
                    Text("SHARED WISDOM CORE", style = MaterialTheme.typography.titleSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val wisdomData = listOf(
                        "💡 Focus Alignment: Aligning desk axis South-East raises core fire strategic markers.",
                        "🔗 Vergil Theory: Code constructs are highly secure of complete direct database persistence.",
                        "🛸 Basim Alignment: High alignment occurs on July moon phases. Maintain reserve ledger buffers."
                    )
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(wisdomData) { post ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = palette.card)
                            ) {
                                Text(
                                    text = post,
                                    modifier = Modifier.padding(12.dp),
                                    color = palette.text,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. VAULT tab Screen: Budget spent gauge and direct database saving
// ==========================================
@Composable
fun VaultScreen(viewModel: KaelenViewModel) {
    val palette = LocalAppColors.current
    val expenses by viewModel.expenses.collectAsState()
    val goal by viewModel.monthlyGoal.collectAsState()
    val context = LocalContext.current
    
    var expAmount by remember { mutableStateOf("") }
    var expCategory by remember { mutableStateOf("Food") }
    var expNote by remember { mutableStateOf("") }
    
    val totalSpent = expenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "VAULT SPENDING LEDGER",
                style = MaterialTheme.typography.titleMedium,
                color = palette.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Circular gauge for budget
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.card)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val ratio = if (goal > 0) (totalSpent / goal).toFloat().coerceIn(0f, 1f) else 0f
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = palette.border.copy(alpha = 0.2f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (ratio >= 0.8f) Color.Red else palette.primary,
                                startAngle = -90f,
                                sweepAngle = ratio * 360f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Spent", color = palette.muted, fontSize = 11.sp)
                            Text("₹${totalSpent.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.text)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cap Threshold: ₹${goal.toInt()}", color = palette.text, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Add expense input card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.card)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RECORD EXPENDITURE VECTOR", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = expAmount,
                        onValueChange = { expAmount = it },
                        placeholder = { Text("Amount (₹)", color = palette.muted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.text,
                            focusedBorderColor = palette.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Food", "Server", "Design", "Mystic").forEach { cat ->
                            val isSel = expCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) palette.primary else palette.border.copy(alpha = 0.3f))
                                        .clickable { expCategory = cat }
                                        .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, color = if (isSel) Color.White else palette.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = expNote,
                        onValueChange = { expNote = it },
                        placeholder = { Text("Transaction note details...", color = palette.muted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.text,
                            focusedBorderColor = palette.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            val amt = expAmount.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                viewModel.requestAction(PendingAction.AddExpense(amt, expCategory, expNote))
                                expAmount = ""
                                expNote = ""
                                Toast.makeText(context, "Expenditure posted to Room.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Define valid spending values.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SAVE TRANSACTIONS", color = OnPrimaryColor)
                    }
                }
            }
        }
        
        // Ledger Ledger transactions list
        item {
            Text("HISTORIC STATEMENT RECORDS", style = MaterialTheme.typography.titleMedium, color = palette.muted, fontWeight = FontWeight.Bold)
        }
        
        if (expenses.isEmpty()) {
            item {
                Text("Ledger statement parsed clean.", color = palette.muted, fontSize = 12.sp)
            }
        } else {
            items(expenses) { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(exp.category, fontWeight = FontWeight.Bold, color = palette.text)
                            if (exp.note.isNotEmpty()) {
                                Text(exp.note, color = palette.muted, fontSize = 11.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-₹${exp.amount.toInt()}", color = palette.tertiary, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.requestAction(PendingAction.DeleteExpense(exp)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. USER settings tab screen: API Override, Theme Switcher
// ==========================================
@Composable
fun UserSettingsScreen(viewModel: KaelenViewModel, profile: UserProfile) {
    val palette = LocalAppColors.current
    val context = LocalContext.current
    
    var nameVal by remember(profile) { mutableStateOf(profile.name) }
    var roleVal by remember(profile) { mutableStateOf(profile.role) }
    var cityVal by remember(profile) { mutableStateOf(profile.city) }
    var customApiKeyVal by remember(profile) { mutableStateOf(profile.customGeminiApiKey) }
    
    // Astrological Coordinates settings
    var birthDateVal by remember(profile) { mutableStateOf(profile.birthDate) }
    var birthTimeVal by remember(profile) { mutableStateOf(profile.birthTime) }
    var birthPlaceVal by remember(profile) { mutableStateOf(profile.birthPlace) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "USER PROFILE & settings SYSTEM",
                color = palette.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Primary directives card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.card)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("COGNITIVE METADATA DIRECTIVES", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = nameVal,
                        onValueChange = { nameVal = it },
                        placeholder = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = roleVal,
                        onValueChange = { roleVal = it },
                        placeholder = { Text("Strategic Role") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cityVal,
                        onValueChange = { cityVal = it },
                        placeholder = { Text("City Coordinates") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                    )
                }
            }
        }
        
        // Astrological Coordinates card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.card)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ASTROLOGICAL COORDINATES (BASIM KUNDLI)", color = palette.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = birthDateVal,
                        onValueChange = { birthDateVal = it },
                        placeholder = { Text("Birth Date (YYYY-MM-DD)", color = palette.muted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.tertiary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = birthTimeVal,
                        onValueChange = { birthTimeVal = it },
                        placeholder = { Text("Birth Time (HH:MM)", color = palette.muted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.tertiary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = birthPlaceVal,
                        onValueChange = { birthPlaceVal = it },
                        placeholder = { Text("Birth Place", color = palette.muted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.tertiary)
                    )
                }
            }
        }
        
        // API override override controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.card)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NEURAL GEOMETRICAL API DEPLOYMENTS", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = customApiKeyVal,
                        onValueChange = { customApiKeyVal = it },
                        placeholder = { Text("Enter Google Gemini API Key Overrides", color = palette.muted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Status text status banner rules
                    val activeKeyPresent = customApiKeyVal.trim().isNotEmpty()
                    val bannerText = if (activeKeyPresent) "Active Override: Using custom in-app Gemini API Key" else "Using system default envoy key"
                    val bannerColor = if (activeKeyPresent) ElectricCyan else TextMuted
                    
                    Text(
                        text = bannerText,
                        color = bannerColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Button(
                        onClick = {
                            val updated = profile.copy(
                                name = nameVal.trim(),
                                role = roleVal.trim(),
                                city = cityVal.trim(),
                                customGeminiApiKey = customApiKeyVal.trim(),
                                birthDate = birthDateVal.trim(),
                                birthTime = birthTimeVal.trim(),
                                birthPlace = birthPlaceVal.trim()
                            )
                            viewModel.requestAction(PendingAction.UpdateProfile(updated))
                            Toast.makeText(context, "Directives saved successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SYNCHRONIZE PROFILE VECTOR", color = OnPrimaryColor)
                    }
                }
            }
        }
        
        // Visual theme switcher card supporting INFERNO, SOVEREIGN, NEXUS, APEX
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.card)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("THEME SCHEMES CHANGER", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val themeOptions = listOf(
                        ThemeOption("INFERNO", "Inferno Warm Red", listOf(Color(0xFF8B0000), Color(0xFFFF6B00)), Color(0xFFFFD700)),
                        ThemeOption("SOVEREIGN", "Sovereign Deep Violet", listOf(Color(0xFF4A0080), Color(0xFFFF006E)), Color(0xFFFFC300)),
                        ThemeOption("NEXUS", "Nexus Cyber Slate", listOf(Color(0xFF004D4D), Color(0xFFFF4500)), Color(0xFFF5F0E8)),
                        ThemeOption("APEX", "Apex High Contrast", listOf(Color(0xFFFF0080), Color(0xFFFF6000)), Color(0xFFFFFFFF))
                    )
                    
                    themeOptions.forEach { opt ->
                        val isSelected = profile.selectedTheme.uppercase() == opt.key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = profile.copy(selectedTheme = opt.key)
                                    viewModel.requestAction(PendingAction.UpdateProfile(updated))
                                }
                                .background(if (isSelected) palette.border.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Color preview swatch
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.linearGradient(opt.gradColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(12.dp).background(opt.accent, CircleShape))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(opt.label, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = palette.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

private data class ThemeOption(
    val key: String,
    val label: String,
    val gradColors: List<Color>,
    val accent: Color
)
