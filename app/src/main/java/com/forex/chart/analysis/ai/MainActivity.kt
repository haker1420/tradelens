package com.forex.chart.analysis.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log // ✅ FCM লগের জন্য ইমপোর্ট
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
import com.google.firebase.messaging.FirebaseMessaging // ✅ FCM সাবস্ক্রিপশনের জন্য ইমপোর্ট
import kotlinx.coroutines.delay

private const val PREFS_NAME = "AppPrefs"
private const val KEY_START_TIME = "startTime"
private const val UPDATE_REQUEST_CODE = 1001

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ১. ফায়ারবেস এবং আপডেট ম্যানেজার ইনিশিয়ালাইজ
        firebaseAnalytics = Firebase.analytics
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // ✅ ৫. ফায়ারবেস টপিক সাবস্ক্রিপশন (নোটিফিকেশন রিসিভ করার জন্য)
        FirebaseMessaging.getInstance().subscribeToTopic("market_news")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_TOPIC", "Successfully subscribed to market_news")
                } else {
                    Log.e("FCM_TOPIC", "Topic subscription failed")
                }
            }

        // ২. অ্যাডমব SDK ইনিশিয়ালাইজ (এটি মেইন থ্রেড জ্যাম হওয়া আটকাবে)
        MobileAds.initialize(this) {}

        // ৩. ইন-অ্যাপ আপডেট চেক
        checkForAppUpdate()

        // ৪. ফার্স্ট ওপেন ট্র্যাকিং লজিক
        handleFirstOpenTracking()

        setContent {
            TradeChatAITheme {
                val context = LocalContext.current

                // ✅ নোটিফিকেশন পারমিশন লজিক (Android 13+)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        // ইউজার ডিনাই করলে এখানে হ্যান্ডেল করতে পারো
                    }
                }

                LaunchedEffect(Unit) {
                    // সামান্য ডিলে দেওয়া যাতে অ্যাপ লোড হওয়ার সাথে সাথেই পপআপ এসে থ্রেড ব্লক না করে
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
                    // তোমার মেইন নেভিগেশন স্ক্রিন
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

    // --- ইন-অ্যাপ আপডেট ফাংশন সমূহ ---
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
        // অ্যাপ যখন ব্যাকগ্রাউন্ড থেকে ফিরে আসে তখন আপডেট চেক
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
                // আপডেট না করলে অ্যাপ আবার চেক করবে
                checkForAppUpdate()
            }
        }
    }
}