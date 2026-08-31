package com.pizzatown.admin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pizzatown.admin.domain.repository.AdminAuthRepository
import com.pizzatown.admin.domain.repository.AdminAuthResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Admin authorization is NOT decided by a Firestore field or any value
 * read/writable from the client. It is decided by the `admin: true`
 * Firebase custom claim, which can only be set server-side (Cloud
 * Function / Admin SDK). Firestore & Storage security rules re-check
 * this same claim, so even a compromised/modified APK cannot grant
 * itself admin access.
 */
class AdminAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AdminAuthRepository {

    override val isAdminSignedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            FirebaseCrashlytics.getInstance().setUserId(user?.uid.orEmpty())
            if (user == null) {
                trySend(false)
            } else {
                user.getIdToken(true).addOnSuccessListener { result ->
                    trySend(result.claims["admin"] == true)
                }.addOnFailureListener {
                    trySend(false)
                }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): AdminAuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return AdminAuthResult.Failure("Login failed. Please try again.")
            val tokenResult = user.getIdToken(true).await()
            val isAdmin = tokenResult.claims["admin"] == true
            if (isAdmin) {
                AdminAuthResult.Success
            } else {
                firebaseAuth.signOut()
                AdminAuthResult.NotAuthorized
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AdminAuthResult.Failure("Incorrect email or password.")
        } catch (e: FirebaseAuthInvalidUserException) {
            AdminAuthResult.Failure("No admin account found for this email.")
        } catch (e: Exception) {
            AdminAuthResult.Failure(e.message ?: "Unable to sign in. Check your connection and try again.")
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
