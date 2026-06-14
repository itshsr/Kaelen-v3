package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.KaelenViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: KaelenViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val logs by viewModel.databaseLogs.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicNavy)
    ) {
        val availableWidth = maxWidth
        val isWideScreen = availableWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Dashboard Heading
            Spacer(modifier = Modifier.height(12.dp))
            DashboardHeaderSection(profileName = userProfile.name, profileRole = userProfile.role)
            Spacer(modifier = Modifier.height(16.dp))

            if (isWideScreen) {
                // Responsive Multi-column Grid for Tablets / Foldables
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column (System Status + Task Schedules)
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatusCard(
                            tasksCount = tasks.size,
                            notesCount = notes.size,
                            projectsCount = projects.size,
                            expensesCount = expenses.size,
                            onTriggerBriefing = { viewModel.triggerMorningBriefing() },
                            modifier = Modifier.weight(1.1f)
                        )
                        TaskSchedulesCard(
                            tasks = tasks,
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    // Right Column (Recent Logs Terminal)
                    Column(
                        modifier = Modifier
                            .weight(1.4f)
                            .fillMaxHeight()
                    ) {
                        RecentLogsCard(
                            logs = logs,
                            onClearLogs = { viewModel.clearAllDatabaseLogs() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // Fluid Stack (Scrollable Column) for Compact Mobile Interfaces
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        SystemStatusCard(
                            tasksCount = tasks.size,
                            notesCount = notes.size,
                            projectsCount = projects.size,
                            expensesCount = expenses.size,
                            onTriggerBriefing = { viewModel.triggerMorningBriefing() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        TaskSchedulesCard(
                            tasks = tasks,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        RecentLogsCard(
                            logs = logs,
                            onClearLogs = { viewModel.clearAllDatabaseLogs() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeaderSection(profileName: String, profileRole: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "KAELEN COGNITIVE INTERFACE",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Welcome, $profileName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CoolWhite
            )
            Text(
                text = "Authorized Personnel / Lead $profileRole",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.5.dp, ElectricCyan), RoundedCornerShape(12.dp)),
            color = CosmicCard,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "System Secured Badge",
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SystemStatusCard(
    tasksCount: Int,
    notesCount: Int,
    projectsCount: Int,
    expensesCount: Int,
    onTriggerBriefing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp))
            .testTag("system_status_card"),
        colors = CardDefaults.cardColors(containerColor = CosmicCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Database Nodes Icon",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CORE TELEMETRY",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoolWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ElectricCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            // Count Metric Modules in a responsive row layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricWidget(title = "TASKS", count = tasksCount, color = ElectricCyan, modifier = Modifier.weight(1f))
                MetricWidget(title = "PROJECTS", count = projectsCount, color = DeepViolet, modifier = Modifier.weight(1f))
                MetricWidget(title = "NOTES", count = notesCount, color = HotPink, modifier = Modifier.weight(1f))
                MetricWidget(title = "VLT DEBITS", count = expensesCount, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Trigger Button
            Button(
                onClick = onTriggerBriefing,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("trigger_briefing_button")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Trigger Briefing Icon",
                    tint = OnPrimaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TRIGGER MORNING SYSTEM BRIEFING",
                    color = OnPrimaryColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MetricWidget(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CosmicNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                color = TextMuted,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun TaskSchedulesCard(
    tasks: List<Task>,
    modifier: Modifier = Modifier
) {
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks
    
    val ratio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val ratioPct = (ratio * 100).toInt()

    Card(
        modifier = modifier
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp))
            .testTag("task_schedules_card"),
        colors = CardDefaults.cardColors(containerColor = CosmicCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = "Schedules Icon",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TASK STATS & SCHEDULES SUMMARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoolWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Visual completion progress donut
                val resolvedBorderColor = BorderColor
                val gradientColors = LocalAppColors.current.primaryGradient
                val brush = remember(gradientColors) { Brush.sweepGradient(gradientColors) }

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = resolvedBorderColor,
                            radius = size.minDimension / 2,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = brush,
                            startAngle = -90f,
                            sweepAngle = 360f * ratio,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$ratioPct%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = CoolWhite
                        )
                        Text(
                            text = "COMPLETED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.SemiBold),
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Structured details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScheduleItem(
                        icon = Icons.Default.PendingActions,
                        title = "Pending Cycles",
                        count = pendingTasks,
                        color = HotPink
                    )
                    ScheduleItem(
                        icon = Icons.Default.CheckCircleOutline,
                        title = "Completed Nodes",
                        count = completedTasks,
                        color = Color(0xFF10B981)
                    )
                    ScheduleItem(
                        icon = Icons.Default.Assessment,
                        title = "Total System Ingests",
                        count = totalTasks,
                        color = ElectricCyan
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(icon: ImageVector, title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = CoolWhite
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun RecentLogsCard(
    logs: List<DatabaseLog>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp))
            .testTag("recent_logs_card"),
        colors = CardDefaults.cardColors(containerColor = CosmicCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Database Logs Core",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECENT DATABASE TRANSACTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoolWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Refresh log button
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(24.dp).testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clean Logs Cache",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                        .background(CosmicNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "No Logs Icon",
                            tint = TextMuted,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NO TELEMETRY TRANSACTION RECORDED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Database is currently idle. Modify data entries around Vault, Tasks, or Core Chat to register telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp)),
                    color = CosmicNavy
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs.take(30)) { log ->
                            LogItemRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemRow(log: DatabaseLog) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = sdf.format(Date(log.timestamp))

    val colors = LocalAppColors.current

    val badgeColor = when (log.action) {
        "INSERT" -> Color(0xFF10B981) // Emerald Green
        "DELETE" -> colors.tertiary
        "UPDATE" -> Color(0xFFF59E0B) // Amber
        else -> colors.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Monospace index timestamp
        Text(
            text = "[$formattedTime]",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextMuted,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Action badge element
        Text(
            text = log.action,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = badgeColor,
            modifier = Modifier
                .width(55.dp)
                .padding(end = 4.dp)
        )

        // Column table / context spec
        Text(
            text = "(${log.tableName})",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = colors.secondary,
            modifier = Modifier
                .width(90.dp)
                .padding(end = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Human details
        Text(
            text = log.description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            ),
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
    }
}
