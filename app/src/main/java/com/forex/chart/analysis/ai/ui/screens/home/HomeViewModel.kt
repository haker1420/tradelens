package com.forex.chart.analysis.ai.ui.screens.home

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.forex.chart.analysis.ai.data.manager.BillingManager
import com.forex.chart.analysis.ai.data.model.AnalysisResponse
import com.forex.chart.analysis.ai.data.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HomeUiState(
    val selectedImages: List<Uri> = emptyList(),
    val isUploading: Boolean = false,
    val analysisResult: AnalysisResponse? = null,
    val errorMessage: String? = null,
    val isChatLocked: Boolean = true,
    val showAdTrigger: Boolean = false,
    val showPremiumPopup: Boolean = false,
    val showAnalysisChoicePopup: Boolean = false,
    val showRatingDialog: Boolean = false,
    val hasRated: Boolean = false,
    val weeklyPrice: String = "Loading...",
    val monthlyPrice: String = "Loading...",
    val yearlyPrice: String = "Loading...",
    val isProUser: Boolean = false,
    val isAnonymous: Boolean = true,
    val freeProScanCount: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "HomeViewModel"
    private val repository = AnalysisRepository(application.applicationContext)
    private val db = FirebaseFirestore.getInstance("trade-chart-db") // নোট: ডাটাবেসের নাম আগেরটাই আছে, দরকার হলে পরিবর্তন করে নেবেন
    private val auth = FirebaseAuth.getInstance()
    private val billingManager = BillingManager(application)

    // ✅ FIX: MainScreen এর জন্য isBillingClientReady expose করা হলো
    val isBillingClientReady: StateFlow<Boolean> = billingManager.isBillingClientReady

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _freeAnalysisCount = MutableStateFlow(0)
    val freeAnalysisCount: StateFlow<Int> = _freeAnalysisCount.asStateFlow()

    var pendingPlanId: String? = null

    init {
        // ✅ FIX: সরাসরি loadUserData() কল না করে, আগে আইডি কনফার্ম করা হচ্ছে
        initializeUserAndData()
        observeBillingData()
    }

    // ✅ NEW: প্রথমবার অ্যাপ ওপেন করলে আগে লগইন নিশ্চিত করবে, তারপর ডাটাবেস লোড করবে
    private fun initializeUserAndData() {
        val user = auth.currentUser
        if (user == null) {
            auth.signInAnonymously().addOnSuccessListener {
                checkUserStatus()
                loadUserData()
            }.addOnFailureListener {
                Log.e(TAG, "Anonymous Login Failed: ${it.message}")
            }
        } else {
            checkUserStatus()
            loadUserData()
        }
    }

    fun checkUserStatus() {
        val user = auth.currentUser
        _uiState.update { it.copy(isAnonymous = user?.isAnonymous ?: true) }
    }

    fun onGoogleLoginSuccess(credential: AuthCredential, activity: Activity) {
        val currentUser = auth.currentUser
        _uiState.update { it.copy(isUploading = true) }

        val onSuccess = {
            _uiState.update { it.copy(isUploading = false) }
            checkUserStatus()
            loadUserData()
            billingManager.refreshPrices()

            if (pendingPlanId != null) {
                triggerPurchase(activity, pendingPlanId!!)
                pendingPlanId = null
            } else {
                _uiState.update { it.copy(showPremiumPopup = true) }
            }
        }

        if (currentUser != null && currentUser.isAnonymous) {
            currentUser.linkWithCredential(credential)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener {
                    auth.signInWithCredential(credential).addOnSuccessListener { onSuccess() }
                }
        } else {
            auth.signInWithCredential(credential).addOnSuccessListener { onSuccess() }
        }
    }

    private fun observeBillingData() {
        viewModelScope.launch {
            launch {
                billingManager.isProUser.collect { isPro ->
                    _uiState.update {
                        it.copy(isProUser = isPro, isChatLocked = !isPro)
                    }
                    if (!isPro) checkFreeChatUsageFromFirebase()
                }
            }
            launch {
                billingManager.productPrices.collect { prices ->
                    val wPrice = prices[billingManager.weeklyPlanId] ?: "Loading..."
                    val mPrice = prices[billingManager.monthlyPlanId] ?: "Loading..."
                    val yPrice = prices[billingManager.yearlyPlanId] ?: "Loading..."
                    _uiState.update { it.copy(weeklyPrice = wPrice, monthlyPrice = mPrice, yearlyPrice = yPrice) }
                }
            }
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).addSnapshotListener { doc, error ->
            if (error != null) {
                Log.e(TAG, "Failed to listen to user data: ${error.message}")
                return@addSnapshotListener
            }

            if (doc != null && doc.exists()) {
                val count = doc.getLong("freeAnalysisCount")?.toInt() ?: 0
                val proCount = doc.getLong("freeProScanCount")?.toInt() ?: 0
                val rated = doc.getBoolean("hasRated") ?: false

                _freeAnalysisCount.value = count
                _uiState.update { it.copy(freeProScanCount = proCount, hasRated = rated) }
            } else {
                // 🎉 First Time Install: Give 1 Free Pro Scan
                val initialData = mapOf(
                    "freeAnalysisCount" to 0,
                    "freeProScanCount" to 0,
                    "hasRated" to false
                )
                db.collection("users").document(uid).set(initialData)

                // ✅ FIX: ডাটাবেসে সেভ হওয়ার সাথে সাথেই UI কে জানিয়ে দেওয়া হলো যাতে সঙ্গে সঙ্গে প্রো কার্ড দেখায়!
                _freeAnalysisCount.value = 0
                _uiState.update { it.copy(freeProScanCount = 0, hasRated = false) }
            }
        }
    }

    fun incrementFreeAnalysisCount(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val newCount = _freeAnalysisCount.value + amount
                db.collection("users").document(uid).update("freeAnalysisCount", newCount).await()
                _freeAnalysisCount.value = newCount
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update free analysis count: ${e.message}")
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        val isPro = _uiState.value.isProUser
        val currentCount = _uiState.value.selectedImages.size

        if (!isPro && (currentCount >= 1 || uris.size > 1)) {
            requestProDialog()
            return
        }

        val currentList = _uiState.value.selectedImages.toMutableList()
        val remainingSlots = 5 - currentList.size
        if (remainingSlots > 0) {
            currentList.addAll(uris.take(remainingSlots))
            _uiState.update { it.copy(selectedImages = currentList) }
        }
    }

    fun removeImage(uri: Uri) {
        val list = _uiState.value.selectedImages.toMutableList()
        list.remove(uri)
        _uiState.update { it.copy(selectedImages = list) }
    }

    fun clearImages() {
        _uiState.update { it.copy(selectedImages = emptyList()) }
    }

    fun startAnalysisProcess() {
        val user = auth.currentUser
        if (user == null) {
            auth.signInAnonymously().addOnSuccessListener { startAnalysisProcess() }
            return
        }
        val userId = user.uid
        if (_uiState.value.isProUser) {
            performAnalysis()
            return
        }

        if (_freeAnalysisCount.value > 0) {
            useFreeAnalysisCredit(userId)
        } else {
            _uiState.update { it.copy(showAnalysisChoicePopup = true) }
        }
    }

    private fun useFreeAnalysisCredit(userId: String) {
        viewModelScope.launch {
            try {
                val newCount = _freeAnalysisCount.value - 1
                db.collection("users").document(userId).update("freeAnalysisCount", newCount).await()
                _freeAnalysisCount.value = newCount
                performAnalysis()
            } catch (e: Exception) {
                performAnalysis()
            }
        }
    }

    fun requestProDialog() {
        billingManager.refreshPrices()
        _uiState.update { it.copy(showPremiumPopup = true) }
    }

    fun onChoiceProClicked() {
        billingManager.refreshPrices()
        _uiState.update { it.copy(showAnalysisChoicePopup = false, showPremiumPopup = true) }
    }

    fun onChoiceAdClicked() {
        _uiState.update { it.copy(showAnalysisChoicePopup = false, showAdTrigger = true) }
    }

    fun dismissAnalysisChoicePopup() {
        _uiState.update { it.copy(showAnalysisChoicePopup = false) }
    }

    fun onAdWatched() {
        _uiState.update { it.copy(showAdTrigger = false) }
        performAnalysis()
    }

    private fun performAnalysis() {
        _uiState.update { it.copy(isUploading = true, errorMessage = null) }

        viewModelScope.launch {
            val firstImage = _uiState.value.selectedImages.firstOrNull() ?: return@launch

            val uploadResult = repository.uploadImage(firstImage)

            if (uploadResult.isSuccess) {
                val fileName = uploadResult.getOrNull() ?: ""

                repository.listenToAnalysis(fileName).collect { response ->
                    if (response != null && response.status == "completed") {
                        _uiState.update { it.copy(isUploading = false, analysisResult = response, showRatingDialog = !it.hasRated) }
                    }
                }
            } else {
                _uiState.update { it.copy(isUploading = false, errorMessage = "Upload failed") }
            }
        }
    }

    private fun checkFreeChatUsageFromFirebase() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val hasUsedChat = document.getBoolean("hasUsedFreeChat") ?: false
            if (!_uiState.value.isProUser) {
                _uiState.update { it.copy(isChatLocked = hasUsedChat) }
            }
        }
    }

    fun onChatClicked(onNavigate: () -> Unit) {
        if (_uiState.value.isProUser) {
            onNavigate()
        } else if (!_uiState.value.isChatLocked) {
            onNavigate()
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId).set(mapOf("hasUsedFreeChat" to true), SetOptions.merge())
                _uiState.update { it.copy(isChatLocked = true) }
            }
        } else {
            requestProDialog()
        }
    }

    fun triggerPurchase(activity: Activity, planId: String) {
        when {
            planId.contains("weekly", ignoreCase = true) -> billingManager.purchaseWeeklyPlan(activity)
            planId.contains("monthly", ignoreCase = true) -> billingManager.purchaseMonthlyPlan(activity)
            planId.contains("yearly", ignoreCase = true) -> billingManager.purchaseYearlyPlan(activity)
        }
    }

    fun dismissPremiumPopup() {
        _uiState.update { it.copy(showPremiumPopup = false) }
    }

    fun dismissRatingDialog() {
        _uiState.update { it.copy(showRatingDialog = false) }
    }

    fun markUserAsRated() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("hasRated", true)
        _uiState.update { it.copy(showRatingDialog = false, hasRated = true) }
    }

    fun resetAnalysis() {
        val currentProStatus = _uiState.value.isProUser
        _uiState.value = HomeUiState(
            isProUser = currentProStatus,
            isChatLocked = !currentProStatus,
            hasRated = _uiState.value.hasRated,
            freeProScanCount = _uiState.value.freeProScanCount
        )
        if (!currentProStatus) checkFreeChatUsageFromFirebase()
    }
}