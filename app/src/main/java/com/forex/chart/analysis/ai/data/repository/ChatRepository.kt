package com.forex.chart.analysis.ai.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.forex.chart.analysis.ai.data.model.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatRepository {

    private val db = FirebaseFirestore.getInstance("trade-chart-db")
    private val auth = FirebaseAuth.getInstance()

    private var generativeModel: GenerativeModel? = null
    private var activeApiKey: String? = null
    private var activeModelName: String? = null

    private val chatSessionHistory = mutableListOf<com.google.ai.client.generativeai.type.Content>()
    private var initialSystemContext: com.google.ai.client.generativeai.type.Content? = null

    init {
        // ✅ Real-time listener for Gemini configuration from Firestore database "trade-chart-db" -> app_settings / gemini_config
        db.collection("app_settings")
            .document("gemini_config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening to gemini_config: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    applyGeminiConfig(snapshot)
                }
            }
    }

    private fun applyGeminiConfig(snapshot: DocumentSnapshot) {
        val apiKey = snapshot.getString("api_key")?.trim() ?: snapshot.getString("apiKey")?.trim()
        val modelName = snapshot.getString("model_name")?.trim() ?: snapshot.getString("modelName")?.trim() ?: "gemini-3.1-flash-lite"

        if (!apiKey.isNullOrBlank()) {
            activeApiKey = apiKey
            activeModelName = modelName
            generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )
            Log.d("ChatRepository", "Gemini Model updated from DB: $modelName")
        }
    }

    private suspend fun getOrFetchGenerativeModel(): GenerativeModel = withContext(Dispatchers.IO) {
        generativeModel?.let { return@withContext it }

        // Fallback: fetch directly from DB if not loaded via listener yet
        val snapshot = db.collection("app_settings")
            .document("gemini_config")
            .get()
            .await()

        val apiKey = snapshot.getString("api_key")?.trim() ?: snapshot.getString("apiKey")?.trim()
        val modelName = snapshot.getString("model_name")?.trim() ?: snapshot.getString("modelName")?.trim() ?: "gemini-3.1-flash-lite"

        if (!apiKey.isNullOrBlank()) {
            activeApiKey = apiKey
            activeModelName = modelName
            val model = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )
            generativeModel = model
            Log.d("ChatRepository", "Gemini Model fetched from DB: $modelName")
            return@withContext model
        }

        throw IllegalStateException("API key not found in app_settings/gemini_config")
    }

    fun initializeChat(analysisContext: String) {
        chatSessionHistory.clear()
        initialSystemContext = content("user") { text("System Context: You are an expert crypto/stock AI analyst. Here is the chart context: $analysisContext. Use this context to answer user questions accurately.") }

        chatSessionHistory.add(initialSystemContext!!)
        chatSessionHistory.add(content("model") { text("Understood. I have the context and am ready to answer questions.") })
    }

    // ✅ FIX: ডাটাবেস থেকে মেসেজ রিস্টোর করার সময় পুরনো কনটেক্সট যাতে মুছে না যায়
    fun restoreHistoryFromDb(messages: List<ChatMessage>) {
        chatSessionHistory.clear()

        if (initialSystemContext != null) {
            chatSessionHistory.add(initialSystemContext!!)
            chatSessionHistory.add(content("model") { text("Understood. I have the context and am ready to answer questions.") })
        }

        messages.forEach { msg ->
            if (msg.text.isNotBlank()) {
                val role = if (msg.isUser) "user" else "model"
                chatSessionHistory.add(content(role) { text(msg.text) })
            }
        }
    }

    fun getMessages(analysisId: String): Flow<List<ChatMessage>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.collection("users")
            .document(userId)
            .collection("analyzed_charts")
            .document(analysisId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.toObjects(ChatMessage::class.java)
                trySend(messages)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveMessageToDb(analysisId: String, message: ChatMessage) {
        val userId = auth.currentUser?.uid ?: return
        try {
            db.collection("users")
                .document(userId)
                .collection("analyzed_charts")
                .document(analysisId)
                .collection("messages")
                .add(message)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendMessageToGemini(message: String, image: Bitmap? = null): String = withContext(Dispatchers.IO) {
        try {
            val model = getOrFetchGenerativeModel()
            val chat = model.startChat(history = chatSessionHistory)

            val response = if (image != null) {
                chat.sendMessage(content { image(image); text(message) })
            } else {
                chat.sendMessage(message)
            }

            chatSessionHistory.add(content("user") { text(message) })

            val responseText = response.text ?: "Sorry, no response."
            chatSessionHistory.add(content("model") { text(responseText) })

            return@withContext responseText
        } catch (e: Exception) {
            Log.e("ChatRepository", "sendMessageToGemini error", e)
            return@withContext "Error: ${e.localizedMessage}"
        }
    }
}