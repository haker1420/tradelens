package com.forex.chart.analysis.ai.ui.screens.premium_analysis

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.forex.chart.analysis.ai.data.repository.AnalysisRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AnalysisStep {
    IDLE,
    SCANNING_VISION,
    ANALYZING_DEEPSEEK,
    GENERATING_CHART,
    FINALIZING,
    COMPLETED
}

class PremiumAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository(application.applicationContext)

    private val db = FirebaseFirestore.getInstance("trade-chart-db")
    private val auth = FirebaseAuth.getInstance()

    private val _currentStep = MutableStateFlow(AnalysisStep.IDLE)
    val currentStep: StateFlow<AnalysisStep> = _currentStep.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResultModel?>(null)
    val analysisResult: StateFlow<AnalysisResultModel?> = _analysisResult.asStateFlow()

    // ✅ NEW: রেটিং ট্র্যাক করার জন্য স্টেট
    private val _showRatingDialog = MutableStateFlow(false)
    val showRatingDialog: StateFlow<Boolean> = _showRatingDialog.asStateFlow()
    private var hasRated = false

    init {
        checkRatingStatus()
    }

    // ✅ NEW: ফায়ারবেস থেকে চেক করা হচ্ছে ইউজার আগে রেটিং দিয়েছে কি না
    private fun checkRatingStatus() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                hasRated = doc.getBoolean("hasRated") ?: false
            }
        }
    }

    fun startProAnalysis(uriString: String) {
        viewModelScope.launch {
            val uri = Uri.parse(uriString)

            _currentStep.value = AnalysisStep.SCANNING_VISION

            val uploadResult = repository.uploadImage(uri, isPro = true)

            if (uploadResult.isSuccess) {
                val fileName = uploadResult.getOrNull() ?: ""

                launch {
                    delay(4000)
                    if (_currentStep.value < AnalysisStep.ANALYZING_DEEPSEEK) _currentStep.value = AnalysisStep.ANALYZING_DEEPSEEK
                    delay(5000)
                    if (_currentStep.value < AnalysisStep.GENERATING_CHART) _currentStep.value = AnalysisStep.GENERATING_CHART
                    delay(6000)
                    if (_currentStep.value < AnalysisStep.FINALIZING) _currentStep.value = AnalysisStep.FINALIZING
                }

                repository.listenToProAnalysis(fileName).collect { response ->
                    if (response != null && response.status == "completed") {
                        _analysisResult.value = response
                        _currentStep.value = AnalysisStep.COMPLETED

                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            try {
                                db.collection("users").document(uid).update("freeProScanCount", 0)
                            } catch (e: Exception) {
                                // Error handling silently
                            }
                        }

                        // ✅ NEW: প্রো স্ক্যান শেষে রেটিং না দিয়ে থাকলে পপআপ ওপেন হবে
                        if (!hasRated) {
                            _showRatingDialog.value = true
                        }
                    }
                }
            } else {
                _currentStep.value = AnalysisStep.IDLE
            }
        }
    }


    fun setHistoryResult(result: AnalysisResultModel) {
        _analysisResult.value = result
        _currentStep.value = AnalysisStep.COMPLETED
    }

    fun resetAnalysis() {
        _currentStep.value = AnalysisStep.IDLE
        _analysisResult.value = null
    }

    fun dismissRatingDialog() {
        _showRatingDialog.value = false
    }

    fun markUserAsRated() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("hasRated", true)
        hasRated = true
        _showRatingDialog.value = false
    }
}