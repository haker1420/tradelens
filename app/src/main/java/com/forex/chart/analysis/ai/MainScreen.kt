package com.forex.chart.analysis.ai

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forex.chart.analysis.ai.R
import com.forex.chart.analysis.ai.data.manager.AdMobManager
import com.forex.chart.analysis.ai.navigation.NavRoute
import com.forex.chart.analysis.ai.ui.components.AppBottomNavigation
import com.forex.chart.analysis.ai.ui.screens.HistoryScreen
import com.forex.chart.analysis.ai.ui.screens.HomeScreen
import com.forex.chart.analysis.ai.ui.screens.ProfileScreen
import com.forex.chart.analysis.ai.ui.screens.PremiumOnboardingScreen
import com.forex.chart.analysis.ai.ui.screens.WebViewScreen
import com.forex.chart.analysis.ai.ui.screens.camera.CameraScreen
import com.forex.chart.analysis.ai.ui.screens.chat.ChatScreen
import com.forex.chart.analysis.ai.ui.screens.chat.ChatViewModel
import com.forex.chart.analysis.ai.ui.screens.home.HomeViewModel
import com.forex.chart.analysis.ai.ui.screens.news.NewsScreen
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.PremiumAnalysisScreen
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.PremiumAnalysisViewModel
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.AnalysisResultScreen
import com.forex.chart.analysis.ai.ui.theme.AppBackground
import com.forex.chart.analysis.ai.ui.theme.ButtonGold
import kotlinx.coroutines.delay

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // === ViewModels & Managers ===
    val chatViewModel: ChatViewModel = viewModel()
    val premiumViewModel: PremiumAnalysisViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel() // ✅ Hoisted for routing
    val adMobManager = remember { AdMobManager(context) } // ✅ AdMob Manager

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showBottomBar by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            NavRoute.HISTORY, NavRoute.PROFILE, NavRoute.NEWS, NavRoute.HOME -> {
                showBottomBar = true
            }
            // === splash_routing_screen এবং PREMIUM_ONBOARDING এ বটম বার হাইড থাকবে ===
            NavRoute.CAMERA_SCREEN, NavRoute.CHAT_SESSION, NavRoute.WEB_VIEW_SCREEN_TEMPLATE,
            NavRoute.PREMIUM_ANALYSIS, NavRoute.PREMIUM_RESULT,
            NavRoute.PREMIUM_ONBOARDING, "splash_routing_screen" -> {
                showBottomBar = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        val screenPadding = if (showBottomBar) innerPadding else PaddingValues(0.dp)

        NavHost(
            navController = navController,
            startDestination = "splash_routing_screen", // ✅ শুরুতেই স্প্ল্যাশ স্ক্রিনে যাবে
            modifier = Modifier.padding(screenPadding)
        ) {

            // ==========================================
            // === 1. SPLASH ROUTING SCREEN (FIXED) ===
            // ==========================================
            composable("splash_routing_screen") {
                val isReady by homeViewModel.isBillingClientReady.collectAsState()
                val uiState by homeViewModel.uiState.collectAsState()

                // ✅ FIX: isReady এবং uiState.isProUser দুটোর উপরেই নির্ভর করবে, যাতে ডেটা সিঙ্ক হতে একটু সময় পেলেও সমস্যা না হয়
                LaunchedEffect(isReady, uiState.isProUser) {
                    if (isReady) {
                        delay(500) // লোগোটা জাস্ট আধা সেকেন্ড দেখানোর জন্য
                        if (uiState.isProUser) {
                            // প্রো ইউজার হলে সোজা হোমে
                            navController.navigate(NavRoute.HOME) {
                                popUpTo("splash_routing_screen") { inclusive = true }
                            }
                        } else {
                            // ফ্রি ইউজার হলে প্রিমিয়াম অনবোর্ডিং স্ক্রিনে
                            navController.navigate(NavRoute.PREMIUM_ONBOARDING) {
                                popUpTo("splash_routing_screen") { inclusive = true }
                            }
                        }
                    }
                }

                // ডার্ক ব্যাকগ্রাউন্ডের ওপর লোগো এবং লোডার
                Box(
                    modifier = Modifier.fillMaxSize().background(AppBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.name_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxWidth(0.6f),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        CircularProgressIndicator(color = ButtonGold)
                    }
                }
            }

            // ==========================================
            // === 2. PREMIUM ONBOARDING (NEW) ===
            // ==========================================
            composable(NavRoute.PREMIUM_ONBOARDING) {
                PremiumOnboardingScreen(
                    viewModel = homeViewModel,
                    adMobManager = adMobManager,
                    onNavigateToHome = {
                        navController.navigate(NavRoute.HOME) {
                            popUpTo(NavRoute.PREMIUM_ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            // ==========================================
            // === 3. CORE SCREENS ===
            // ==========================================
            composable(NavRoute.HOME) { entry ->
                val capturedUri = entry.savedStateHandle.get<String>("captured_image_uri")
                HomeScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    chatViewModel = chatViewModel,
                    newlyCapturedImageUri = capturedUri,
                    onImageHandled = { entry.savedStateHandle.remove<String>("captured_image_uri") },
                    onToggleBottomBar = { shouldShow -> showBottomBar = shouldShow }
                )
            }

            composable(NavRoute.HISTORY) {
                HistoryScreen(
                    navController = navController,
                    chatViewModel = chatViewModel,
                    premiumViewModel = premiumViewModel,
                    onToggleBottomBar = { shouldShow -> showBottomBar = shouldShow }
                )
            }

            composable(NavRoute.NEWS) {
                NewsScreen(navController = navController)
            }

            composable(NavRoute.PROFILE) {
                ProfileScreen(navController = navController)
            }

            composable(NavRoute.CHAT_SESSION) {
                ChatScreen(
                    navController = navController,
                    viewModel = chatViewModel
                )
            }

            // ==========================================
            // === 4. PREMIUM ANALYSIS ROUTES ===
            // ==========================================
            composable(NavRoute.PREMIUM_ANALYSIS) {
                PremiumAnalysisScreen(
                    viewModel = premiumViewModel,
                    onAnalysisCompleted = {
                        navController.navigate(NavRoute.PREMIUM_RESULT) {
                            popUpTo(NavRoute.PREMIUM_ANALYSIS) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoute.PREMIUM_RESULT) {
                AnalysisResultScreen(
                    viewModel = premiumViewModel,
                    onBack = {
                        premiumViewModel.resetAnalysis()
                        navController.popBackStack(NavRoute.HOME, false)
                    },
                    onOpenChat = {
                        val proData = premiumViewModel.analysisResult.value
                        if (proData != null) {
                            val sessionId = "pro_session_${System.currentTimeMillis()}"
                            chatViewModel.startProSession(proData, sessionId)
                            navController.navigate(NavRoute.CHAT_SESSION)
                        }
                    }
                )
            }

            // ==========================================
            // === 5. UTILITY SCREENS ===
            // ==========================================
            composable(NavRoute.CAMERA_SCREEN) {
                CameraScreen(
                    onImageCaptured = { uri ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("captured_image_uri", uri.toString())
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoute.WEB_VIEW_SCREEN_TEMPLATE,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("url") { type = NavType.StringType }
                )
            ) { entry ->
                val title = entry.arguments?.getString("title") ?: "Web View"
                val url = entry.arguments?.getString("url") ?: "about:blank"

                WebViewScreen(
                    navController = navController,
                    title = Uri.decode(title),
                    url = Uri.decode(url)
                )
            }
        }
    }
}