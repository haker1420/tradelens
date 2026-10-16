package com.forex.chart.analysis.ai.data.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobManager(private val context: Context) {

    // === Companion Object ===
    companion object {
        private var rewardedAd: RewardedAd? = null
        private var interstitialAd: InterstitialAd? = null

        private var isInitialized = false
        private var isRewardedLoading = false
        private var isInterstitialLoading = false

        private const val TAG = "AdMobManager"
    }

    // Google AdMob Rewarded Ad ID
    private val rewardedAdUnitId = "ca-app-pub-3708468446766365/3237696216"

    // Google AdMob Interstitial Ad ID
    private val interstitialAdUnitId = "ca-app-pub-3708468446766365/2151711803"

    init {
        if (!isInitialized) {
            MobileAds.initialize(context) {
                Log.d(TAG, "AdMob Initialized")
            }
            loadRewardedAd(context)
            loadInterstitialAd(context)
            isInitialized = true
        }
    }

    // ==========================================
    // 1. REWARDED AD LOGIC
    // ==========================================
    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, rewardedAdUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Rewarded Ad Failed to Load: ${adError.message}")
                rewardedAd = null
                isRewardedLoading = false
            }
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded Ad Loaded Successfully")
                rewardedAd = ad
                isRewardedLoading = false
            }
        })
    }

    fun showRewardedAd(activity: Activity, isPremium: Boolean = false, onRewardEarned: () -> Unit) {
        if (isPremium) {
            onRewardEarned()
            return
        }
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad Dismissed")
                    rewardedAd = null
                    loadRewardedAd(activity)
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Rewarded Ad Failed to Show: ${adError.message}")
                    rewardedAd = null
                    loadRewardedAd(activity)
                }
            }
            rewardedAd?.show(activity) { onRewardEarned() }
        } else {
            Log.d(TAG, "Rewarded Ad not ready yet. Earning reward directly.")
            onRewardEarned()
            loadRewardedAd(activity)
        }
    }

    // ==========================================
    // 2. INTERSTITIAL AD LOGIC
    // ==========================================
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, interstitialAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Interstitial Ad Failed to Load: ${adError.message}")
                interstitialAd = null
                isInterstitialLoading = false
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial Ad Loaded Successfully")
                interstitialAd = ad
                isInterstitialLoading = false
            }
        })
    }

    fun showInterstitialAd(activity: Activity, isPremium: Boolean = false, onAdDismissed: () -> Unit) {
        if (isPremium) {
            onAdDismissed()
            return
        }

        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad Dismissed")
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial Ad Failed to Show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial Ad not ready yet. Proceeding without ad.")
            onAdDismissed()
            loadInterstitialAd(activity)
        }
    }
}