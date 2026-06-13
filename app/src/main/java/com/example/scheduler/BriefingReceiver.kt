package com.example.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.KaelenDatabase
import com.example.data.model.Expense
import com.example.data.model.Task
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class BriefingReceiver : BroadcastReceiver() {
    private val TAG = "BriefingReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive triggered with action: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = KaelenDatabase.getDatabase(context)
                val appDao = db.appDao

                // Retrieve Profile Configuration
                val profile = appDao.getUserProfileOneOff() ?: UserProfile()

                if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
                    // Reschedule alarm on boot if enabled
                    if (profile.briefingEnabled) {
                        BriefingScheduler.scheduleDailyBriefing(
                            context.applicationContext,
                            profile.briefingHour,
                            profile.briefingMinute
                        )
                        Log.d(TAG, "Rescheduled daily briefing after boot at ${profile.briefingHour}:${profile.briefingMinute}")
                    }
                } else if (action == "com.example.ACTION_TRIGGER_BRIEFING") {
                    // Pull current stats
                    val tasks = appDao.getAllTasksOneOff()
                    val expenses = appDao.getAllExpensesOneOff()

                    val pendingTasks = tasks.filter { !it.isCompleted }
                    val totalPendingCount = pendingTasks.size
                    
                    // Spent today calculations
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startOfToday = cal.timeInMillis

                    val spentToday = expenses.filter { it.timestamp >= startOfToday }.sumOf { it.amount }
                    val totalSpent = expenses.sumOf { it.amount }

                    // Construct Briefing content list
                    val taskSummary = pendingTasks.take(3).joinToString(separator = ", ") { it.title }.ifEmpty { "No immediate tasks" }
                    val briefingGreeting = "Good Morning, ${profile.name}!"
                    val localOutlookSummary = "Today's outlook: You have $totalPendingCount pending tasks (including $taskSummary). Budget status: Today's spending is ₹${String.format(Locale.getDefault(), "%.2f", spentToday)} out of ₹${String.format(Locale.getDefault(), "%.2f", totalSpent)} cumulative spend. Keep focused on ${profile.currentProjects}!"

                    var briefingMessage = localOutlookSummary

                    // Try to fetch personalized summary from Gemini if key is registered
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey.trim().isNotBlank()) {
                        try {
                            val systemPrompt = "You are KAELEN, an elite, deeply analytical workspace advisor. You provide short, high-priority morning briefings under 100 words. Summarize the user's focus, pending tasks, and budget metrics with direct, motivating, system-driven guidance."
                            val userPrompt = """
                                Please generate a personalized workspace briefing summary for ${profile.name}.
                                Stats:
                                - Role: ${profile.role}
                                - Base Location: ${profile.city}
                                - Projects: ${profile.currentProjects}
                                - Preferences: ${profile.preferences}
                                - Pending Task Count: $totalPendingCount
                                - Next 3 Tasks: $taskSummary
                                - Today's Spent Budget: ₹$spentToday
                                - Total Logged Expenses: ₹$totalSpent
                                
                                Write the briefing directly to ${profile.name}. Keep it very clean, focused, professional, and inspiring. Must be extremely concise (under 250 characters so it fits comfortably in a standard Android push notification expandable view).
                            """.trimIndent()

                            val request = GenerateContentRequest(
                                contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                            )

                            val response = RetrofitClient.service.generateContent(apiKey, request)
                            val aiReply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!aiReply.isNullOrBlank()) {
                                briefingMessage = aiReply.trim()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to fetch briefing from Gemini API, falling back to local synthesis.", e)
                        }
                    }

                    // Send the alarm notification
                    sendBriefingNotification(context, briefingGreeting, briefingMessage)

                    // Reschedule alarm for next day
                    if (profile.briefingEnabled) {
                        BriefingScheduler.scheduleDailyBriefing(
                            context.applicationContext,
                            profile.briefingHour,
                            profile.briefingMinute
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error compiling background morning briefing: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendBriefingNotification(context: Context, title: String, text: String) {
        val id = "kaelen_briefings_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, "KAELEN Morning Briefing Service", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Delivers high-priority system briefing diagnostics daily"
            }
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, id)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(2002, builder.build())
    }
}
