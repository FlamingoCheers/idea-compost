package com.ideacompost.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ideacompost.app.ui.AppNavHost
import com.ideacompost.app.ui.Routes
import com.ideacompost.app.ui.theme.IdeaCompostTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("ideacompost", MODE_PRIVATE)
        setContent {
            IdeaCompostTheme {
                var start by remember { mutableStateOf(
                    if (prefs.getBoolean("onboarded", false)) Routes.CRUMBS else Routes.ONBOARDING
                ) }
                AppNavHost(startDestination = start)
            }
        }
    }
}
