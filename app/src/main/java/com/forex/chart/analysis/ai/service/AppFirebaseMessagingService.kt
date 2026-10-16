package com.forex.chart.analysis.ai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.forex.chart.analysis.ai.MainActivity
import com.forex.chart.analysis.ai.R

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New Firebase Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "📈 Live Market Update"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "New News"

        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "forex_ai_news_channel_v2"
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/${R.raw.news}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Market News Alerts", // ব্যবহারকারী এই নামটি সেটিংসে দেখতে পাবেন
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                setSound(soundUri, audioAttributes) // নতুন চ্যানেলে সাউন্ড যুক্ত করা হলো
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // প্রয়োজনে এখানে আপনার নোটিফিকেশন আইকন (যেমন: R.drawable.ic_notification) দিন
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri) // অ্যান্ড্রয়েড ৮.০ এর আগের ভার্সনের জন্য
            .setVibrate(longArrayOf(1000, 1000))
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}