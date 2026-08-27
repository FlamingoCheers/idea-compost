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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** 与 OnboardingViewModel/ProviderStore 同一实例（加密存储），冷启动读取首启旗标。 */
    @Inject lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
