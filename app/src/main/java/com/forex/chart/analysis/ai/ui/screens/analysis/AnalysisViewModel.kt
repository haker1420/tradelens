package com.forex.chart.analysis.ai.ui.screens.analysis

import androidx.lifecycle.ViewModel
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnalysisViewModel : ViewModel() {

    private val _analysisData = MutableStateFlow<AnalysisResponse?>(null)
    val analysisData: StateFlow<AnalysisResponse?> = _analysisData.asStateFlow()


    fun setAnalysisData(data: AnalysisResponse) {
        _analysisData.value = data
    }
}