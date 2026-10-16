package com.forex.chart.analysis.ai.navigation

import android.net.Uri
import com.forex.chart.analysis.ai.R

/**
 * Data class for defining items in the bottom navigation bar.
 */
data class BottomNavItem(
    val route: String,
    val iconResId: Int, // Drawable Resource ID (R.drawable.xyz)
    val label: String
)

/**
 * Object to hold all navigation routes and bottom navigation items.
 */
object NavRoute {
    // ==========================================
    // 1. Core Screens (Bottom Navigation)
    // ==========================================
    const val HOME = "home_screen"
    const val HISTORY = "history_screen"
    const val NEWS = "news_screen"
    const val PROFILE = "profile_screen"

    // ==========================================
    // 2. Action & Utility Screens
    // ==========================================
    const val CHAT_SESSION = "chat_session_screen"
    const val CAMERA_SCREEN = "camera_screen"
    const val WEB_VIEW_SCREEN_TEMPLATE = "web_view_screen/{title}/{url}"

    // ==========================================
    // 3. Premium AI Analysis Screens (Pro Features)
    // ==========================================
    const val PREMIUM_ANALYSIS = "premium_analysis_screen"
    const val PREMIUM_RESULT = "premium_result_screen"

    // ==========================================
    // 4. Premium Onboarding Screen (✅ NEW)
    // ==========================================
    const val PREMIUM_ONBOARDING = "premium_onboarding_screen"

    // ==========================================
    // Bottom Navigation List
    // ==========================================
    val bottomNavItems = listOf(
        BottomNavItem(HOME, R.drawable.home, "Home"),
        BottomNavItem(HISTORY, R.drawable.history, "History"),
        BottomNavItem(NEWS, R.drawable.news, "News"),
        BottomNavItem(PROFILE, R.drawable.profile, "Profile")
    )

    /**
     * Helper function to build the web view route with URL encoding.
     */
    fun createWebViewRoute(title: String, url: String): String {
        return "web_view_screen/${Uri.encode(title)}/${Uri.encode(url)}"
    }
}