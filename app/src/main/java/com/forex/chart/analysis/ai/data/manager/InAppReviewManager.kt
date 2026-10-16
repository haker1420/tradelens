package com.forex.chart.analysis.ai.data.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

class InAppReviewManager(private val context: Context) {

    private val reviewManager = ReviewManagerFactory.create(context)

    /**
     * Launches the Google Play In-App Review flow.
     * If the review request fails or cannot be shown (e.g. quota limit, unreleased app),
     * it falls back to the provided fallback or completes gracefully.
     */
    fun launchReviewFlow(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    // Google Play In-App Review finished (user may have submitted, cancelled, or quota hit)
                    onComplete(true)
                }
            } else {
                Log.w("InAppReviewManager", "In-App review request failed: ${task.exception?.message}")
                onComplete(false)
            }
        }
    }

    /**
     * Direct fallback to open Google Play Store listing page
     */
    fun openPlayStore(activity: Activity? = null) {
        val ctx = activity ?: context
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${ctx.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        }
    }
}
