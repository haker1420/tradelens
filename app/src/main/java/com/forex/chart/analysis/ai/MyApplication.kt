package com.forex.chart.analysis.ai

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.forex.chart.analysis.ai.data.manager.AppOpenAdManager
import com.google.android.gms.ads.MobileAds

class MyApplication : Application(), Application.ActivityLifecycleCallbacks {

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}

        // অ্যাপ ওপেন অ্যাড ম্যানেজার শুরু করা
        appOpenAdManager = AppOpenAdManager(this)
    }

    // নিচের মেথডগুলো বর্তমান অ্যাক্টিভিটি ট্র্যাক করতে সাহায্য করে
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        AppOpenAdManager.currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        AppOpenAdManager.currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        AppOpenAdManager.currentActivity = null
    }
}