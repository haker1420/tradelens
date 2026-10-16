package com.forex.chart.analysis.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()


    fun getCurrentUser(): FirebaseUser? = auth.currentUser


    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {

                Result.success(currentUser)
            } else {

                val authResult = auth.signInAnonymously().await()
                val user = authResult.user
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Authentication failed: User is null"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}