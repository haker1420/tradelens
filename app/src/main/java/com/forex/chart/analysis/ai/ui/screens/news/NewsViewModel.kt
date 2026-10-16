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

// ✅ Context পাওয়ার জন্য ViewModel থেকে AndroidViewModel করা হলো
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    // তোমার কাস্টম ডেটাবেসের নাম
    private val db = FirebaseFirestore.getInstance("trade-chart-db")

    // ✅ BillingManager ইনিশিয়ালাইজ করা হলো
    private val billingManager = BillingManager(application.applicationContext)

    private val _newsList = MutableStateFlow<List<NewsModel>>(emptyList())
    val newsList: StateFlow<List<NewsModel>> = _newsList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    init {
        fetchMarketNews()
        observePremiumStatus() // ✅ BillingManager থেকে লাইভ স্ট্যাটাস নেবে
    }

    private fun fetchMarketNews() {
        db.collection("market_news")
            .orderBy("publishedAt", Query.Direction.DESCENDING) // নতুন খবর আগে দেখাবে
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
            // BillingManager এর isProUser ফ্লো-কে অবজার্ভ করবে
            billingManager.isProUser.collect { isPro ->
                _isPremiumUser.value = isPro
            }
        }
    }
}