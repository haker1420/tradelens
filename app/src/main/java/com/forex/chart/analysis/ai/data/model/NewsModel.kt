package com.forex.chart.analysis.ai.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class NewsModel(
    val uuid: String = "",
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val imageUrl: String = "",
    val source: String = "",
    val publishedAt: Date? = null
)