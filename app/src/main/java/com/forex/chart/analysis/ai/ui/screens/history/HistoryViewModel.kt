package com.forex.chart.analysis.ai.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import com.forex.chart.analysis.ai.data.repository.AnalysisRepository
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.AnalysisResultModel // ✅ প্রো মডেল ইমপোর্ট করা হলো
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository(application.applicationContext)

    // ==========================================
    // ১. ফ্রি (Standard) হিস্ট্রির জন্য
    // ==========================================
    private val _historyList = MutableStateFlow<List<AnalysisResponse>>(emptyList())
    val historyList: StateFlow<List<AnalysisResponse>> = _historyList.asStateFlow()

    // ==========================================
    // ২. প্রো (Premium) হিস্ট্রির জন্য
    // ==========================================
    private val _proHistoryList = MutableStateFlow<List<AnalysisResultModel>>(emptyList())
    val proHistoryList: StateFlow<List<AnalysisResultModel>> = _proHistoryList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHistory()
        loadProHistory() // ✅ অ্যাপ চালুর সাথে সাথে প্রো হিস্ট্রিও লোড হবে
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getHistory()
                .catch {
                    _isLoading.value = false
                    _historyList.value = emptyList()
                }
                .collect { list ->
                    _historyList.value = list
                    _isLoading.value = false
                }
        }
    }

    private fun loadProHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getProHistory()
                .catch {
                    _isLoading.value = false
                    _proHistoryList.value = emptyList()
                }
                .collect { list ->
                    _proHistoryList.value = list
                    _isLoading.value = false
                }
        }
    }
}