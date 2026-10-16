package com.forex.chart.analysis.ai.ui.screens.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import com.forex.chart.analysis.ai.data.model.ChatMessage
import com.forex.chart.analysis.ai.data.repository.ChatRepository
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.AnalysisResultModel // ✅ PRO Model Import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messageCount = MutableStateFlow(0)
    val messageCount: StateFlow<Int> = _messageCount.asStateFlow()

    // ✅ For Standard Free Analysis
    private val _currentAnalysis = MutableStateFlow<AnalysisResponse?>(null)
    val currentAnalysis: StateFlow<AnalysisResponse?> = _currentAnalysis.asStateFlow()

    // ✅ NEW: For Premium Pro Analysis
    private val _currentProAnalysis = MutableStateFlow<AnalysisResultModel?>(null)
    val currentProAnalysis: StateFlow<AnalysisResultModel?> = _currentProAnalysis.asStateFlow()

    private var currentSessionId: String? = null // ✅ Track active Session ID
    private val MAX_MESSAGES = 5

    // Open chat session for free scan
    fun startSession(analysisData: AnalysisResponse, newSession: Boolean = false) {
        val sessionId = if (newSession) {
            "${analysisData.id}_${System.currentTimeMillis()}"
        } else {
            analysisData.id
        }
        if (!newSession && currentSessionId == sessionId) return

        _currentAnalysis.value = analysisData
        _currentProAnalysis.value = null
        currentSessionId = sessionId

        val contextString = "Analysis Context: ${analysisData.data?.prediction?.signal}, Trend: ${analysisData.data?.keyInsights?.trend}, Plan: ${analysisData.data?.gamePlan}"
        repository.initializeChat(contextString)
        loadMessagesFromDb(sessionId)
    }

    // ✅ NEW: Open chat session for Pro scan
    fun startProSession(proData: AnalysisResultModel, docId: String) {
        if (currentSessionId == docId) return

        _currentProAnalysis.value = proData
        _currentAnalysis.value = null
        currentSessionId = docId

        val contextString = "PRO Analysis Context: ${proData.tradeSignal} SIGNAL, Confidence: ${proData.confidenceScore}, Timeframe: ${proData.timeframe}, Entry: ${proData.entryPrice}, Target: ${proData.takeProfit}, Risk/Reward: ${proData.riskRewardRatio}"
        repository.initializeChat(contextString)
        loadMessagesFromDb(docId)
    }

    private fun loadMessagesFromDb(sessionId: String) {
        viewModelScope.launch {
            repository.getMessages(sessionId)
                .catch { e ->
                    // Handle error safely without crashing
                }
                .collect { savedMessages ->
                if (savedMessages.isEmpty()) {
                    val welcomeText = if (_currentProAnalysis.value != null) {
                        "Hello! I am your Quantum Pro AI. I've deeply analyzed this chart's institutional order flow and precision targets. What would you like to know?"
                    } else {
                        "Hello! I've analyzed this chart. I know about the Game Plan, Support/Resistance, and Strategy. Ask me anything!"
                    }

                    val welcomeMsg = ChatMessage(text = welcomeText, isUser = false, timestamp = java.util.Date())
                    _messages.value = listOf(welcomeMsg)
                    repository.saveMessageToDb(sessionId, welcomeMsg)
                } else {
                    _messages.value = savedMessages
                    _messageCount.value = savedMessages.count { it.isUser }
                }

                if (savedMessages.isNotEmpty()) {
                    repository.restoreHistoryFromDb(savedMessages)
                }
            }
        }
    }

    fun sendMessage(text: String, image: Bitmap? = null) {
        if (_messageCount.value >= MAX_MESSAGES) return

        val sessionId = currentSessionId ?: return
        _isLoading.value = true

        viewModelScope.launch {
            val userMsg = ChatMessage(text = text, isUser = true, image = image, timestamp = java.util.Date())
            _messageCount.value += 1
            repository.saveMessageToDb(sessionId, userMsg)

            val responseText = repository.sendMessageToGemini(text, image)

            val aiMsg = ChatMessage(text = responseText, isUser = false, timestamp = java.util.Date())
            repository.saveMessageToDb(sessionId, aiMsg)

            _isLoading.value = false
        }
    }
}