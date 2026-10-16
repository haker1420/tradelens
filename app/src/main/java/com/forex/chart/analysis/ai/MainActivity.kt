package com.forex.chart.analysis.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log // ✅ Import for FCM logs
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.forex.chart.analysis.ai.ui.theme.TradeChatAITheme
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging // ✅ Import for FCM subscriptions
import kotlinx.coroutines.delay

private const val PREFS_NAME = "AppPrefs"
private const val KEY_START_TIME = "startTime"
private const val UPDATE_REQUEST_CODE = 1001

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Firebase and Update Manager
        firebaseAnalytics = Firebase.analytics
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // ✅ 5. Firebase topic subscription (to receive notifications)
        FirebaseMessaging.getInstance().subscribeToTopic("market_news")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_TOPIC", "Successfully subscribed to market_news")
                } else {
                    Log.e("FCM_TOPIC", "Topic subscription failed")
                }
            }

        // 2. Initialize AdMob SDK (prevents blocking main thread)
        MobileAds.initialize(this) {}

        // 3. Check for in-app updates
        checkForAppUpdate()

        // 4. First app open tracking logic
        handleFirstOpenTracking()

        setContent {
            TradeChatAITheme {
                val context = LocalContext.current

                // ✅ Notification permission logic (Android 13+)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        // Handle user denial here if needed
                    }
                }

                LaunchedEffect(Unit) {
                    // Slight delay so dialog does not block main thread immediately on app load
                    delay(1000)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Main navigation screen
                    MainScreen()
                }
            }
        }
    }

    private fun handleFirstOpenTracking() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getLong(KEY_START_TIME, 0L) == 0L) {
            val currentTime = System.currentTimeMillis()
            prefs.edit().putLong(KEY_START_TIME, currentTime).apply()
            val bundle = Bundle()
            bundle.putString("first_open_time", currentTime.toString())
            firebaseAnalytics.logEvent("app_first_install_launch", bundle)
        }
    }

    // --- In-App Update Functions ---
    private fun checkForAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for updates when app returns from background
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // If update not performed, app checks again
                checkForAppUpdate()
            }
        }
    }
}