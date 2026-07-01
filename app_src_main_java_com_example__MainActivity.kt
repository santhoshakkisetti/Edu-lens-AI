package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EduLensViewModel
import com.example.ui.viewmodel.EduLensViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual Constructor Injection using application container
        val app = application as EduLensApplication
        val viewModelFactory = EduLensViewModelFactory(app, app.repository, app.preferences)
        val viewModel = ViewModelProvider(this, viewModelFactory)[EduLensViewModel::class.java]

        setContent {
            // Observe the visual theme preference from the UserPreferences Datastore
            val themeMode by viewModel.themeMode.collectAsState()
            val systemTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemTheme
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()

                // Observe session and state variables
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val userEmail by viewModel.userEmail.collectAsState()
                val memberSince by viewModel.memberSince.collectAsState()
                val studyStreak by viewModel.studyStreak.collectAsState()
                val scanHistory by viewModel.scanHistory.collectAsState()
                val currentLanguage by viewModel.selectedLanguage.collectAsState()

                // Process result states
                val isAnalyzing by viewModel.isAnalyzing.collectAsState()
                val analysisResult by viewModel.analysisResult.collectAsState()
                val analysisError by viewModel.analysisError.collectAsState()
                val capturedImageBitmap by viewModel.capturedImageBitmap.collectAsState()

                // Doubt States
                val isAskingDoubt by viewModel.isAskingDoubt.collectAsState()
                val doubtResponse by viewModel.doubtResponse.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        // 1. SPLASH SCREEN
                        composable("splash") {
                            SplashScreen(
                                isLoggedIn = isLoggedIn,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. LOGIN SCREEN
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { email ->
                                    viewModel.login(email)
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 3. HOME SCREEN
                        composable("home") {
                            HomeScreen(
                                streakCount = studyStreak,
                                userEmail = userEmail,
                                recentHistory = scanHistory,
                                onNavigateToCamera = {
                                    navController.navigate("camera")
                                },
                                onNavigateToHistory = {
                                    navController.navigate("history")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("profile")
                                },
                                onMockScanTriggered = { text ->
                                    viewModel.clearAnalysis()
                                    viewModel.analyzeText(text)
                                    navController.navigate("result")
                                },
                                onSelectRecentNote = { note ->
                                    viewModel.clearAnalysis()
                                    viewModel.loadHistoryItemToActive(note)
                                    navController.navigate("result")
                                },
                                onNavigateToDoubt = {
                                    viewModel.clearAnalysis()
                                    viewModel.loadGeneralDoubtGuide()
                                    navController.navigate("result")
                                }
                            )
                        }

                        // 4. CAMERA SCREEN
                        composable("camera") {
                            CameraScreen(
                                onImageCaptured = { bitmap, uri ->
                                    viewModel.clearAnalysis()
                                    viewModel.setCapturedImage(bitmap)
                                    viewModel.analyzeCapturedImage(uri)
                                    navController.navigate("result") {
                                        popUpTo("camera") { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 5. AI RESULT SCREEN
                        composable("result") {
                            AiResultScreen(
                                result = analysisResult,
                                capturedBitmap = capturedImageBitmap,
                                isAnalyzing = isAnalyzing,
                                analysisError = analysisError,
                                isAskingDoubt = isAskingDoubt,
                                doubtResponse = doubtResponse,
                                onAskDoubt = { doubt ->
                                    viewModel.askDoubt(doubt)
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 6. HISTORY SCREEN
                        composable("history") {
                            HistoryScreen(
                                historyList = scanHistory,
                                onSelectRecentNote = { note ->
                                    viewModel.clearAnalysis()
                                    viewModel.loadHistoryItemToActive(note)
                                    navController.navigate("result")
                                },
                                onDeleteItem = { item ->
                                    viewModel.deleteHistoryItem(item)
                                },
                                onClearAll = {
                                    viewModel.clearAllHistory()
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 7. PROFILE SCREEN
                        composable("profile") {
                            ProfileScreen(
                                userEmail = userEmail,
                                memberSince = memberSince,
                                studyStreak = studyStreak,
                                totalNotesCount = scanHistory.size,
                                currentTheme = themeMode,
                                currentLanguage = currentLanguage,
                                onThemeChanged = { mode ->
                                    viewModel.updateTheme(mode)
                                },
                                onLanguageChanged = { lang ->
                                    viewModel.updateLanguage(lang)
                                },
                                onClearHistory = {
                                    viewModel.clearAllHistory()
                                },
                                onLogout = {
                                    viewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
