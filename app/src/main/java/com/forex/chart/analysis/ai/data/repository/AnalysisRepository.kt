package com.forex.chart.analysis.ai.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import com.forex.chart.analysis.ai.ui.screens.premium_analysis.AnalysisResultModel
import com.forex.chart.analysis.ai.utils.ImageResizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AnalysisRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance("trade-chart-db")

    // ==========================================
    // ১. ছবি আপলোড (Free ও Pro উভয়ের জন্য)
    // ==========================================
    suspend fun uploadImage(uri: Uri, isPro: Boolean = false): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            val fileName = "chart_${UUID.randomUUID()}.jpg"

            // প্রো ইউজার হলে pro_uploads, না হলে uploads
            val folderName = if (isPro) "pro_uploads" else "uploads"
            val storageRef = storage.reference.child("users/$userId/$folderName/$fileName")

            val compressedData = ImageResizer.reduceImageSize(context, uri)

            if (compressedData != null) {
                storageRef.putBytes(compressedData).await()
            } else {
                storageRef.putFile(uri).await()
            }

            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // ২. FREE SCAN: সাধারণ ডাটা লিসেনার
    // ==========================================
    fun listenToAnalysis(fileName: String): Flow<AnalysisResponse?> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        // শুধুমাত্র ফ্রি কালেকশনে খুঁজবে
        val query = db.collection("users")
            .document(userId)
            .collection("analyzed_charts")
            .whereEqualTo("fileName", fileName)
            .limit(1)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val document = snapshot.documents[0]
                val response = document.toObject(AnalysisResponse::class.java)
                trySend(response)
            }
        }
        awaitClose { registration.remove() }
    }

    // ==========================================
    // ৩. PRO SCAN: প্রো ডাটা লিসেনার
    // ==========================================
    fun listenToProAnalysis(fileName: String): Flow<AnalysisResultModel?> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            close(Exception("User not logged in"))
            return@callbackFlow
        }

        // শুধুমাত্র প্রো কালেকশনে খুঁজবে
        val query = db.collection("users")
            .document(userId)
            .collection("pro_analyzed_charts")
            .whereEqualTo("fileName", fileName)
            .limit(1)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val document = snapshot.documents[0]
                // প্রো মডেল এ কনভার্ট হচ্ছে
                val response = document.toObject(AnalysisResultModel::class.java)
                trySend(response)
            }
        }
        awaitClose { registration.remove() }
    }

    // ==========================================
    // ৪. FREE HISTORY: ফ্রি হিস্ট্রি নেওয়ার জন্য
    // ==========================================
    fun getHistory(): Flow<List<AnalysisResponse>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = db.collection("users")
            .document(userId)
            .collection("analyzed_charts")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(AnalysisResponse::class.java) }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    // ==========================================
    // ৫. PRO HISTORY: প্রো হিস্ট্রি নেওয়ার জন্য
    // ==========================================
    fun getProHistory(): Flow<List<AnalysisResultModel>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = db.collection("users")
            .document(userId)
            .collection("pro_analyzed_charts")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(AnalysisResultModel::class.java) }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }
}