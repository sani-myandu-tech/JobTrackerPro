package com.jobtracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jobtracker.presentation.auth.login.LoginScreen
import com.jobtracker.presentation.auth.register.RegisterScreen
import com.jobtracker.presentation.addeditjob.AddEditJobScreen
import com.jobtracker.presentation.aianalysis.AiAnalysisScreen
import com.jobtracker.presentation.analytics.AnalyticsScreen
import com.jobtracker.presentation.chatbot.ChatbotScreen
import com.jobtracker.presentation.dashboard.DashboardScreen
import com.jobtracker.presentation.joblist.JobListScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object JobList : Screen("job_list")
    object AddJob : Screen("add_job")
    object EditJob : Screen("edit_job/{jobId}") {
        fun createRoute(jobId: String) = "edit_job/$jobId"
    }
    object AiAnalysis : Screen("ai_analysis/{jobId}") {
        fun createRoute(jobId: String) = "ai_analysis/$jobId"
    }
    object Analytics : Screen("analytics")
    object Chatbot : Screen("chatbot")
}

@Composable
fun JobTrackerNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToJobList = { navController.navigate(Screen.JobList.route) },
                onNavigateToAddJob = { navController.navigate(Screen.AddJob.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToChatbot = { navController.navigate(Screen.Chatbot.route) },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.JobList.route) {
            JobListScreen(
                onNavigateToAddJob = { navController.navigate(Screen.AddJob.route) },
                onNavigateToEditJob = { jobId -> navController.navigate(Screen.EditJob.createRoute(jobId)) },
                onNavigateToAiAnalysis = { jobId -> navController.navigate(Screen.AiAnalysis.createRoute(jobId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddJob.route) {
            AddEditJobScreen(
                jobId = null,
                onNavigateBack = { navController.popBackStack() },
                onJobSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditJob.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            AddEditJobScreen(
                jobId = backStackEntry.arguments?.getString("jobId"),
                onNavigateBack = { navController.popBackStack() },
                onJobSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AiAnalysis.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            AiAnalysisScreen(
                jobId = backStackEntry.arguments?.getString("jobId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Chatbot.route) {
            ChatbotScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
