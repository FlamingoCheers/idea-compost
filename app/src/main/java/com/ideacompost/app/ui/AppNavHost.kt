package com.ideacompost.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ideacompost.app.ui.crumbs.CrumbsScreen
import com.ideacompost.app.ui.onboard.OnboardingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val CRUMBS = "crumbs"
}

@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Routes.CRUMBS) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CRUMBS) {
            CrumbsScreen()
        }
    }
}
