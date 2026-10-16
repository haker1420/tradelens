package com.forex.chart.analysis.ai.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.forex.chart.analysis.ai.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: FirebaseUser? = null,
    val isAnonymous: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance("trade-chart-db")
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        updateUserStateFromFirebase()
    }

    fun updateUserStateFromFirebase() {
        val currentUser = auth.currentUser
        currentUser?.reload()?.addOnCompleteListener {
            val updatedUser = auth.currentUser
            _uiState.value = _uiState.value.copy(
                user = updatedUser,
                isAnonymous = updatedUser?.isAnonymous ?: true
            )
        }
    }


    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = authRepository.signInAnonymously()

            if (result.isSuccess) {

                updateUserStateFromFirebase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Logged out successfully. Switched to Guest mode."
                )
            } else {

                updateUserStateFromFirebase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Logout successful, but failed to start a new Guest session."
                )
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
                _uiState.value = ProfileUiState()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = task.exception?.message)
            }
        }
    }


    fun linkGoogleAccount(credential: AuthCredential) {
        val currentUser = auth.currentUser ?: return


        if (!currentUser.isAnonymous) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Already logged in.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true) // LOADING


        auth.signOut()


        auth.signInWithCredential(credential)
            .addOnSuccessListener { signinResult ->
                val googleUser = signinResult.user


                updateUserProfile(googleUser, isMigration = false)
            }
            .addOnFailureListener { signinError ->

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google Sign-in failed: ${signinError.message}"
                )
            }
    }


    private fun migrateAnonymousData(oldUid: String, newUid: String, onComplete: () -> Unit) {
        onComplete()
    }

    private fun updateUserProfile(user: FirebaseUser?, isMigration: Boolean) {

        val name = user?.displayName ?: user?.email?.substringBefore('@') ?: "New Pro User"
        val email = user?.email ?: ""

        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        user?.updateProfile(profileUpdates)

        val userData = hashMapOf("displayName" to name, "email" to email, "isPremium" to false)

        user?.uid?.let { uid ->
            db.collection("users").document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    updateUserStateFromFirebase()
                    val message = "Login successful! Welcome $name."
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = message) // SUCCESS

                }
                .addOnFailureListener { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Profile update failed: ${e.message}")
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}