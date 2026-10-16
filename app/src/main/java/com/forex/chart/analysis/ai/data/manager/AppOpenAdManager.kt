package com.forex.chart.analysis.ai.data.manager

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(private val application: Application) : DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false

    // Google AdMob App Open Ad ID
    private val AD_UNIT_ID = "ca-app-pub-3708468446766365/6282748648"

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        showAdIfAvailable()
    }

    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(context, AD_UNIT_ID, request, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                isLoadingAd = false
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isLoadingAd = false
            }
        })
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    private fun showAdIfAvailable() {
        // If ad is already showing or not loaded, do not display
        if (isShowingAd || !isAdAvailable()) {
            loadAd(application)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd(application) // Preload for the next time
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }

        // Show ad in current foreground activity
        currentActivity?.let {
            appOpenAd?.show(it)
        }
    }

    // Helper logic to track current foreground activity
    companion object {
        var currentActivity: Activity? = null
    }
}