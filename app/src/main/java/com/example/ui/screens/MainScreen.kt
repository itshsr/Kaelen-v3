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
        val palette = when (ThemeManager.activeVariant.value) {
            AppThemeVariant.INFERNO -> InfernoPalette
            AppThemeVariant.NEXUS -> NexusPalette
            AppThemeVariant.ARCTIC_FOX -> ArcticFoxPalette
            AppThemeVariant.CRIMSON_WOLF -> CrimsonWolfPalette
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = palette.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "KAELEN COGNITIVE NUCLEUS BOOTING...",
                    color = palette.text,
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
        "ARCTIC FOX", "ARCTIC_FOX" -> AppThemeVariant.ARCTIC_FOX
        "CRIMSON WOLF", "CRIMSON_WOLF" -> AppThemeVariant.CRIMSON_WOLF
        "NEXUS" -> AppThemeVariant.NEXUS
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
        
        // Daily Habits circular rings display section
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DAILY HABIT TRACKER",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val habitNames = listOf("Morning Routine", "Evening Review", "Daily Reading")
                    habitNames.forEach { name ->
                        val h = habits.firstOrNull { it.name == name } ?: Habit(name, emptySet(), 0)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val isCompleted = h.completedDates.contains(todayStr)
                        
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                                .border(BorderStroke(1.dp, if(isCompleted) palette.primary.copy(alpha = 0.3f) else palette.border), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = palette.card)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Box(
                                    modifier = Modifier.size(54.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = palette.border.copy(alpha = 0.3f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = if (isCompleted) palette.primary else palette.border,
                                            startAngle = -90f,
                                            sweepAngle = if (isCompleted) 360f else 0f,
                                            useCenter = false,
                                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Text(
                                        text = if (isCompleted) "✓" else "🔥${h.streak}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) palette.primary else palette.text
                                    )
                                }
                                
                                Button(
                                    onClick = { viewModel.toggleHabit(name) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCompleted) palette.primary.copy(alpha = 0.2f) else palette.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isCompleted) "UNDO" else "DONE",
                                        color = if (isCompleted) palette.primary else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
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
                val isKaelen = agent == "KAELEN"
                
                val chipBg = if (isKaelen) {
                    if (isSelected) palette.primary else palette.primary.copy(alpha = 0.15f)
                } else if (isSelected) {
                    if (agent == "BASIM") palette.tertiary else palette.secondary
                } else {
                    palette.card
                }
                
                val chipBorder = if (isKaelen) {
                    BorderStroke(1.5.dp, palette.primary)
                } else {
                    BorderStroke(1.dp, if (isSelected) Color.Transparent else palette.border)
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .border(chipBorder, RoundedCornerShape(20.dp))
                        .clickable { viewModel.selectChatMode(agent) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isKaelen) {
                            Text(
                                text = "🧠 ",
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = agent,
                            color = if (isSelected) Color.White else palette.text.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isKaelen) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isKaelen) {
                            Text(
                                text = " • Core",
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else palette.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
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
    val profile by viewModel.userProfile.collectAsState()
    val defaultFocusSecs = profile.preferredFocusMinutes * 60
    val defaultBreakSecs = profile.preferredBreakMinutes * 60

    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(-1) }
    var isBreakMode by remember { mutableStateOf(false) }

    val targetDefault = if (isBreakMode) defaultBreakSecs else defaultFocusSecs
    if (!timerRunning && (timerSeconds == -1 || timerSeconds == defaultFocusSecs || timerSeconds == defaultBreakSecs || timerSeconds == 300 || timerSeconds == 1500)) {
        timerSeconds = targetDefault
    }
    
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
                timerSeconds = defaultBreakSecs
            } else {
                Toast.makeText(context, "Break over! Time to construct code.", Toast.LENGTH_SHORT).show()
                isBreakMode = false
                timerSeconds = defaultFocusSecs
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
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!timerRunning) {
                            Text("SET TIMER DURATIONS", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var focusInputVal by remember { mutableStateOf(profile.preferredFocusMinutes.toString()) }
                            var breakInputVal by remember { mutableStateOf(profile.preferredBreakMinutes.toString()) }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = focusInputVal,
                                    onValueChange = { focusInputVal = it },
                                    label = { Text("Focus Mins", color = palette.muted, fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = palette.text),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = palette.text,
                                        unfocusedTextColor = palette.text,
                                        focusedBorderColor = palette.primary,
                                        unfocusedBorderColor = palette.border
                                    )
                                )
                                OutlinedTextField(
                                    value = breakInputVal,
                                    onValueChange = { breakInputVal = it },
                                    label = { Text("Break Mins", color = palette.muted, fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = palette.text),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = palette.text,
                                        unfocusedTextColor = palette.text,
                                        focusedBorderColor = palette.primary,
                                        unfocusedBorderColor = palette.border
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val fMin = focusInputVal.toIntOrNull() ?: 25
                                    val bMin = breakInputVal.toIntOrNull() ?: 5
                                    viewModel.updatePreferredTimer(fMin, bMin)
                                    timerSeconds = (if (isBreakMode) bMin else fMin) * 60
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("APPLY & SAVE TIMER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            // Quick option chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(15, 25, 30, 45, 60).forEach { mins ->
                                    val isSel = profile.preferredFocusMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                             .clip(RoundedCornerShape(8.dp))
                                             .background(if (isSel) palette.primary else palette.border.copy(alpha = 0.3f))
                                             .clickable {
                                                 viewModel.updatePreferredTimer(mins, profile.preferredBreakMinutes)
                                                 timerSeconds = mins * 60
                                                 focusInputVal = mins.toString()
                                             }
                                             .padding(vertical = 6.dp),
                                         contentAlignment = Alignment.Center
                                    ) {
                                         Text("${mins}m", color = if (isSel) Color.White else palette.text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val maxSecs = if (isBreakMode) (if (defaultBreakSecs > 0) defaultBreakSecs else 300) else (if (defaultFocusSecs > 0) defaultFocusSecs else 1500)
                            val pct = if (maxSecs > 0) timerSeconds.toFloat() / maxSecs.toFloat() else 0f
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
                            
                            val minutes = if (timerSeconds >= 0) timerSeconds / 60 else 0
                            val seconds = if (timerSeconds >= 0) timerSeconds % 60 else 0
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
                                    timerSeconds = if (isBreakMode) (if (defaultBreakSecs > 0) defaultBreakSecs else 300) else (if (defaultFocusSecs > 0) defaultFocusSecs else 1500)
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
    var selectedPersonId by remember { mutableStateOf<Int?>(null) }
    
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
                        val people by viewModel.peopleProfiles.collectAsState()
                        val currentPerson = people.find { it.id == selectedPersonId }
                            ?: people.find { it.relationship.lowercase() == "self" }
                            ?: people.firstOrNull()

                        Text("SELECT PROFILE FOR KUNDLI MAP", color = palette.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            people.forEach { person ->
                                val isSelected = currentPerson?.id == person.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) palette.primary else palette.border.copy(alpha = 0.4f))
                                        .clickable { selectedPersonId = person.id }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${person.photoEmoji} ${person.name}",
                                        color = if (isSelected) Color.White else palette.text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        if (currentPerson == null || currentPerson.dateOfBirth.isBlank() || currentPerson.birthTime.isBlank() || currentPerson.birthPlace.isBlank()) {
                            Text(
                                text = "To map Kundli birth chart, please configure birth date, time, and birth place inside this person's profile card in the USER tab settings.",
                                color = palette.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            val zodiacName = viewModel.run { calculateZodiac(currentPerson.dateOfBirth) }
                            Text("KUNDLI BIRTH MATRIX", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Derived from profile detail coordinates: ${currentPerson.dateOfBirth} @ ${currentPerson.birthTime} Place: ${currentPerson.birthPlace}", color = palette.muted, fontSize = 11.sp)
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
                                Text(zodiacName.uppercase(), modifier = Modifier.align(Alignment.Center), color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Rashi planetary positioning for ${currentPerson.name}: Jupiter in 11th House of gains (Excellent for technology role). Moon transit indicates mental sprint capability.",
                                color = palette.text,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
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
                    var showCapDialog by remember { mutableStateOf(false) }
                    var capInputVal by remember { mutableStateOf(goal.toInt().toString()) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Cap Threshold: ₹${goal.toInt()}", color = palette.text, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            capInputVal = goal.toInt().toString()
                            showCapDialog = true
                        }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                contentDescription = "Edit cap",
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showCapDialog) {
                        AlertDialog(
                            onDismissRequest = { showCapDialog = false },
                            title = { Text("EDIT BUDGET CAP", color = palette.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("Enter your new monthly spending budget cap limit:", color = palette.text, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    OutlinedTextField(
                                        value = capInputVal,
                                        onValueChange = { capInputVal = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = palette.text,
                                            unfocusedTextColor = palette.text,
                                            focusedBorderColor = palette.primary,
                                            unfocusedBorderColor = palette.border
                                        )
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val amt = capInputVal.toDoubleOrNull()
                                    if (amt != null && amt > 0) {
                                        viewModel.updateMonthlyGoal(amt)
                                    }
                                    showCapDialog = false
                                }) {
                                    Text("SAVE", color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCapDialog = false }) {
                                    Text("CANCEL", color = palette.muted)
                                }
                            },
                            containerColor = palette.card
                        )
                    }
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
    val peopleProfiles by viewModel.peopleProfiles.collectAsState()
    val unlockedProfileIds by viewModel.unlockedProfileIds.collectAsState()
    val compatibilityResult by viewModel.compatibilityResult.collectAsState()
    
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
                            viewModel.requestAction(
                                PendingAction.UpdateProfileDirectives(
                                    name = nameVal.trim(),
                                    role = roleVal.trim(),
                                    city = cityVal.trim(),
                                    customGeminiApiKey = customApiKeyVal.trim(),
                                    birthDate = birthDateVal.trim(),
                                    birthTime = birthTimeVal.trim(),
                                    birthPlace = birthPlaceVal.trim()
                                )
                            )
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
                        ThemeOption("NEXUS", "Nexus Cyber Slate", listOf(Color(0xFF004D4D), Color(0xFFFF4500)), Color(0xFFF5F0E8)),
                        ThemeOption("ARCTIC FOX", "Arctic Fox Feminine White", listOf(Color(0xFFFF6B9D), Color(0xFFC44DFF)), Color(0xFFFF6B9D)),
                        ThemeOption("CRIMSON WOLF", "Crimson Wolf Dominant Red", listOf(Color(0xFFFF0000), Color(0xFFFF6B00)), Color(0xFFFF3D00))
                    )
                    
                    themeOptions.forEach { opt ->
                        val isSelected = profile.selectedTheme.uppercase() == opt.key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.requestAction(PendingAction.UpdateTheme(opt.key))
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

        // --- PEOPLE SECTION ---
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "COGNITIVE PEOPLE NETWORK",
                    color = palette.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                var showAddPersonDialog by remember { mutableStateOf(false) }
                IconButton(onClick = { showAddPersonDialog = true }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Add Person",
                        tint = palette.primary
                    )
                }

                if (showAddPersonDialog) {
                    var newName by remember { mutableStateOf("") }
                    var newRel by remember { mutableStateOf("Friend") }
                    var newDob by remember { mutableStateOf("") }
                    var newTime by remember { mutableStateOf("") }
                    var newPlace by remember { mutableStateOf("") }
                    var newCity by remember { mutableStateOf("") }
                    var newNotes by remember { mutableStateOf("") }
                    var newEmoji by remember { mutableStateOf("👤") }
                    var requiresPinToggle by remember { mutableStateOf(false) }
                    var newPinValue by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAddPersonDialog = false },
                        title = { Text("Initialize Person Profile", color = palette.text, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        containerColor = palette.card,
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        val encryptedPinStr = if (requiresPinToggle && newPinValue.isNotBlank()) {
                                            viewModel.run { encryptPin(newPinValue) }
                                        } else ""
                                        val newProg = com.example.data.model.PersonProfile(
                                            name = newName.trim(),
                                            relationship = newRel,
                                            dateOfBirth = newDob.trim(),
                                            birthTime = newTime.trim(),
                                            birthPlace = newPlace.trim(),
                                            city = newCity.trim(),
                                            notes = newNotes.trim(),
                                            photoEmoji = newEmoji,
                                            isPinLocked = requiresPinToggle,
                                            encryptedPin = encryptedPinStr
                                        )
                                        viewModel.savePersonProfile(newProg)
                                        showAddPersonDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                            ) {
                                Text("Add", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddPersonDialog = false }) {
                                Text("Cancel", color = palette.muted)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    label = { Text("Name (Required)", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                OutlinedTextField(
                                    value = newRel,
                                    onValueChange = { newRel = it },
                                    label = { Text("Relationship Tag (Partner, Spouse, Friend...)", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                OutlinedTextField(
                                    value = newDob,
                                    onValueChange = { newDob = it },
                                    label = { Text("DOB (YYYY-MM-DD)", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                OutlinedTextField(
                                    value = newTime,
                                    onValueChange = { newTime = it },
                                    label = { Text("Birth Time (HH:MM)", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                OutlinedTextField(
                                    value = newPlace,
                                    onValueChange = { newPlace = it },
                                    label = { Text("Birth Place", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                OutlinedTextField(
                                    value = newCity,
                                    onValueChange = { newCity = it },
                                    label = { Text("Current City", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                OutlinedTextField(
                                    value = newNotes,
                                    onValueChange = { newNotes = it },
                                    label = { Text("Personal Notes", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                OutlinedTextField(
                                    value = newEmoji,
                                    onValueChange = { newEmoji = it },
                                    label = { Text("Avatar Emoji (e.g. 🦊)", color = palette.muted) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("PIN Gate Lock Profile", color = palette.text, fontSize = 12.sp)
                                    Switch(
                                        checked = requiresPinToggle,
                                        onCheckedChange = { requiresPinToggle = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = palette.primary)
                                    )
                                }
                                if (requiresPinToggle) {
                                    OutlinedTextField(
                                        value = newPinValue,
                                        onValueChange = { newPinValue = it },
                                        label = { Text("Enter Profile PIN", color = palette.muted) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        if (peopleProfiles.isEmpty()) {
            item {
                Text("No people profiles created. Tap '+' above to register.", color = palette.muted, fontSize = 12.sp)
            }
        } else {
            items(peopleProfiles) { person ->
                val isUnlocked = !person.isPinLocked || unlockedProfileIds.contains(person.id)
                var selectedReportType by remember { mutableStateOf("") }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = palette.card)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(palette.border.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(person.photoEmoji.ifBlank { "👤" }, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(person.name, fontWeight = FontWeight.Bold, color = palette.text, fontSize = 15.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(palette.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(person.relationship.uppercase(), color = palette.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (person.isPinLocked) {
                                    Icon(
                                        imageVector = if (isUnlocked) androidx.compose.material.icons.Icons.Default.LockOpen else androidx.compose.material.icons.Icons.Default.Lock,
                                        contentDescription = "Pin locked status",
                                        tint = if (isUnlocked) Color.Green else palette.primary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                if (isUnlocked) {
                                                    viewModel.lockProfile(person.id)
                                                    selectedReportType = ""
                                                    Toast.makeText(context, "Profile session locked.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                
                                IconButton(onClick = { viewModel.deletePersonProfile(person) }) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!isUnlocked) {
                            Text("This profile is highly confidential. Enter correct PIN access sequence:", color = palette.muted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            var enteredPinStr by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = enteredPinStr,
                                    onValueChange = { enteredPinStr = it },
                                    placeholder = { Text("Unlock PIN Phrase", color = palette.muted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = palette.text),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = palette.text,
                                        focusedBorderColor = palette.primary,
                                        unfocusedBorderColor = palette.border
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val pass = viewModel.unlockProfile(person.id, enteredPinStr)
                                        if (pass) {
                                            Toast.makeText(context, "Decrypted entry authorized.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Sequence mismatched. Warning logged.", Toast.LENGTH_SHORT).show()
                                        }
                                        enteredPinStr = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Authorize", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        } else {
                            val displayAge = if (person.dateOfBirth.isNotBlank()) {
                                try {
                                    val dbDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(person.dateOfBirth)
                                    if (dbDate != null) {
                                        val diff = Date().time - dbDate.time
                                        val ageYears = (diff / (1000L * 60 * 60 * 24 * 365)).toInt()
                                        "$ageYears yrs"
                                    } else "Unknown"
                                } catch (e: Exception) { "Unknown" }
                            } else "Unknown"

                            val zodiacName = if (person.dateOfBirth.isNotBlank()) {
                                viewModel.run { calculateZodiac(person.dateOfBirth) }
                            } else "Unknown Position"

                            Text("Zodiac Sign: $zodiacName  |  Age: $displayAge", color = palette.tertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (person.city.isNotBlank()) {
                                Text("Current Coordinate: ${person.city}", color = palette.text, fontSize = 11.sp)
                            }
                            if (person.notes.isNotBlank()) {
                                Text("Directives / Notes: ${person.notes}", color = palette.muted, fontSize = 11.sp, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            var showEditZone by remember { mutableStateOf(false) }
                            Text(
                                text = if (showEditZone) "HIDE SETTINGS" else "CONFIGURE PROFILE SETTINGS",
                                color = palette.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showEditZone = !showEditZone }
                                    .padding(vertical = 4.dp)
                            )

                            if (showEditZone) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    var editName by remember { mutableStateOf(person.name) }
                                    var editRel by remember { mutableStateOf(person.relationship) }
                                    var editDob by remember { mutableStateOf(person.dateOfBirth) }
                                    var editTime by remember { mutableStateOf(person.birthTime) }
                                    var editPlace by remember { mutableStateOf(person.birthPlace) }
                                    var editCity by remember { mutableStateOf(person.city) }
                                    var editNotes by remember { mutableStateOf(person.notes) }
                                    var editEmoji by remember { mutableStateOf(person.photoEmoji) }
                                    var editPinVal by remember { mutableStateOf("") }
                                    var pinLockedToggle by remember { mutableStateOf(person.isPinLocked) }

                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Name", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    OutlinedTextField(
                                        value = editRel,
                                        onValueChange = { editRel = it },
                                        label = { Text("Relationship", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    OutlinedTextField(
                                        value = editDob,
                                        onValueChange = { editDob = it },
                                        label = { Text("DOB (YYYY-MM-DD)", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    OutlinedTextField(
                                        value = editTime,
                                        onValueChange = { editTime = it },
                                        label = { Text("Birth Time (HH:MM)", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    OutlinedTextField(
                                        value = editPlace,
                                        onValueChange = { editPlace = it },
                                        label = { Text("Birth Place", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    OutlinedTextField(
                                        value = editCity,
                                        onValueChange = { editCity = it },
                                        label = { Text("Current City", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    OutlinedTextField(
                                        value = editNotes,
                                        onValueChange = { editNotes = it },
                                        label = { Text("Notes", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    OutlinedTextField(
                                        value = editEmoji,
                                        onValueChange = { editEmoji = it },
                                        label = { Text("Avatar Emoji", color = palette.muted, fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Pin Lock Gate Enabled", color = palette.text, fontSize = 11.sp)
                                        Switch(
                                            checked = pinLockedToggle,
                                            onCheckedChange = { pinLockedToggle = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = palette.primary)
                                        )
                                    }
                                    if (pinLockedToggle) {
                                        OutlinedTextField(
                                            value = editPinVal,
                                            onValueChange = { editPinVal = it },
                                            label = { Text("Update PIN (Optional)", color = palette.muted, fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = palette.text, focusedBorderColor = palette.primary)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val cryptStr = if (pinLockedToggle) {
                                                if (editPinVal.isNotBlank()) viewModel.run { encryptPin(editPinVal) } else person.encryptedPin
                                            } else ""
                                            val saved = person.copy(
                                                name = editName.trim(),
                                                relationship = editRel.trim(),
                                                dateOfBirth = editDob.trim(),
                                                birthTime = editTime.trim(),
                                                birthPlace = editPlace.trim(),
                                                city = editCity.trim(),
                                                notes = editNotes.trim(),
                                                photoEmoji = editEmoji.trim(),
                                                isPinLocked = pinLockedToggle,
                                                encryptedPin = cryptStr
                                            )
                                            viewModel.savePersonProfile(saved)
                                            showEditZone = false
                                            Toast.makeText(context, "Profile configurations synchronized.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Settings", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.drawTarotForPerson(person)
                                        selectedReportType = "TAROT"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.border),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Tarot", color = palette.text, fontSize = 9.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.generateHoroscopeForPerson(person)
                                        selectedReportType = "HOROSCOPE"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.border),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Horoscope", color = palette.text, fontSize = 9.sp)
                                }
                                Button(
                                    onClick = {
                                        selectedReportType = "KUNDLI"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.border),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Kundli", color = palette.text, fontSize = 9.sp)
                                }
                                if (person.relationship.lowercase() != "self") {
                                    Button(
                                        onClick = {
                                            viewModel.performCompatibilityReading(person)
                                            selectedReportType = "COMPATIBILITY"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                        modifier = Modifier.weight(1.2f).height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Text("Compatibility", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }

                            val reportContent = when (selectedReportType) {
                                "TAROT" -> if (person.tarotReading.isNotEmpty()) "${person.tarotCard}\n\n${person.tarotReading}" else "No active Tarot reading drawn yet."
                                "HOROSCOPE" -> if (person.dailyHoroscope.isNotEmpty()) person.dailyHoroscope else "No daily horoscope generated yet."
                                "KUNDLI" -> if (person.dateOfBirth.isBlank() || person.birthTime.isBlank() || person.birthPlace.isBlank()) {
                                    "Birth location coords, time, or date missing from this operator profile vector."
                                } else {
                                    "KUNDLI ASTROGRAPH MAP COORDINATES:\n" +
                                    "Sun Sign Position: ${zodiacName}\n" +
                                    "Ascendant Degree: 18° Virgo\n" +
                                    "Moon Positioning: Taurus (4th Celestial House)\n" +
                                    "Birth Location Coordinates: ${person.birthPlace} at ${person.birthTime}"
                                }
                                "COMPATIBILITY" -> compatibilityResult
                                else -> ""
                            }

                            if (reportContent.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = palette.border.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("BASIM COSMIC REPORT RESULTS:", color = palette.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(reportContent, color = palette.text, fontSize = 11.sp, style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "CLEAR REPORT",
                                            color = palette.muted,
                                            fontSize = 9.sp,
                                            modifier = Modifier
                                                .clickable { selectedReportType = "" }
                                                .padding(vertical = 2.dp)
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

private data class ThemeOption(
    val key: String,
    val label: String,
    val gradColors: List<Color>,
    val accent: Color
)
