package com.ideacompost.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ideacompost.app.ui.composts.CompostsScreen
import com.ideacompost.app.ui.crumbs.CrumbsScreen
import com.ideacompost.app.ui.onboard.OnboardingScreen
import com.ideacompost.app.ui.output.OutputScreen
import com.ideacompost.app.ui.settings.SettingsScreen
import com.ideacompost.app.ui.setup.SetupScreen
import com.ideacompost.app.ui.wait.WaitScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val CRUMBS = "crumbs"
    const val COMPOSTS = "composts"
    const val SETTINGS = "settings"
    const val SETUP = "setup/{ids}"
    const val WAIT = "wait/{id}"
    const val OUTPUT = "output/{id}"
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
            CrumbsScreen(
                onTabCompost = { navController.navigate(Routes.COMPOSTS) },
                onSetup = { ids -> navController.navigate("setup/${ids.joinToString(",")}") },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.COMPOSTS) {
            CompostsScreen(
                onOpen = { id -> navController.navigate("wait/$id") },
                onNewCompost = {
                    navController.navigate(Routes.CRUMBS) {
                        popUpTo(Routes.CRUMBS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.SETUP,
            arguments = listOf(navArgument("ids") { type = NavType.StringType })
        ) {
            SetupScreen(
                onFired = { id -> navController.navigate("wait/$id") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.WAIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            WaitScreen(
                onDone = { compostId ->
                    navController.navigate("output/$compostId") {
                        popUpTo(Routes.CRUMBS)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.OUTPUT,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            OutputScreen(onBack = { navController.popBackStack() })
        }
    }
}
