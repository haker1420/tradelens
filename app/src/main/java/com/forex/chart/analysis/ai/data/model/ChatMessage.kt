package com.forex.chart.analysis.ai.data.model

import android.graphics.Bitmap
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",


    @get:PropertyName("isUser") @set:PropertyName("isUser")
    var isUser: Boolean = false,

    @ServerTimestamp val timestamp: Date? = null,

    @get:Exclude var image: Bitmap? = null
)