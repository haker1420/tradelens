package com.forex.chart.analysis.ai.ui.screens.news

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.forex.chart.analysis.ai.data.manager.BillingManager
import com.forex.chart.analysis.ai.data.model.NewsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ✅ Converted ViewModel to AndroidViewModel to access application context
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    // Custom database name
    private val db = FirebaseFirestore.getInstance("trade-chart-db")

    // ✅ Initialize BillingManager
    private val billingManager = BillingManager(application.applicationContext)

    private val _newsList = MutableStateFlow<List<NewsModel>>(emptyList())
    val newsList: StateFlow<List<NewsModel>> = _newsList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    init {
        fetchMarketNews()
        observePremiumStatus() // ✅ Observe live status from BillingManager
    }

    private fun fetchMarketNews() {
        db.collection("market_news")
            .orderBy("publishedAt", Query.Direction.DESCENDING) // Order by newest first
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NewsViewModel", "Error fetching news", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val news = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NewsModel::class.java)
                    }
                    _newsList.value = news
                    _isLoading.value = false
                }
            }
    }


    private fun observePremiumStatus() {
        viewModelScope.launch {
            // Observe BillingManager isProUser flow
            billingManager.isProUser.collect { isPro ->
                _isPremiumUser.value = isPro
            }
        }
    }
}