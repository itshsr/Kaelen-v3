package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.AppThemeVariant
import com.example.viewmodel.KaelenViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load starting theme variant to prevent cold launch flash
        val prefs = getSharedPreferences("kaelen_shared_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("selected_theme", "INFERNO") ?: "INFERNO"
        val variant = when (savedTheme.uppercase()) {
            "ARCTIC_FOX", "ARCTIC FOX" -> AppThemeVariant.ARCTIC_FOX
            "CRIMSON_WOLF", "CRIMSON WOLF" -> AppThemeVariant.CRIMSON_WOLF
            "NEXUS" -> AppThemeVariant.NEXUS
            else -> AppThemeVariant.INFERNO
        }
        ThemeManager.activeVariant.value = variant

        // Graceful runtime permission request for local notification triggers (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                val viewModel: KaelenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                MainScreen(viewModel)
            }
        }
    }
}
