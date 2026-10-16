package com.forex.chart.analysis.ai.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forex.chart.analysis.ai.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        signInAnonymously()
    }

    private fun signInAnonymously() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.signInAnonymously()

            if (result.isSuccess) {
                val userId = result.getOrNull()?.uid
                Log.d("AuthViewModel", "Success! Anonymous User ID: $userId")
                _authState.value = AuthState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown Error"
                Log.e("AuthViewModel", "Failed: $errorMsg")
                _authState.value = AuthState.Error(errorMsg)
            }
        }
    }
}