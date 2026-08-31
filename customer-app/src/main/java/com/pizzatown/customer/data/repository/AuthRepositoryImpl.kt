package com.pizzatown.customer.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.customer.core.firebase.FirestoreCollections
import com.pizzatown.customer.data.model.UserProfileDto
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.AuthResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            // Tag crash reports with the signed-in user so a crash can be
            // traced back to an account without storing any PII in Crashlytics.
            FirebaseCrashlytics.getInstance().setUserId(user?.uid.orEmpty())
            trySend(user != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    override suspend fun login(email: String, password: String): AuthResult = try {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        AuthResult.Success
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        AuthResult.Failure("Incorrect email or password.")
    } catch (e: Exception) {
        AuthResult.Failure(e.message ?: "Unable to sign in. Check your connection and try again.")
    }

    override suspend fun register(
        fullName: String,
        mobile: String,
        email: String,
        password: String
    ): AuthResult = try {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: return AuthResult.Failure("Registration failed. Please try again.")

        val profile = UserProfileDto(fullName = fullName, mobile = mobile, email = email)
        firestore.collection(FirestoreCollections.USERS).document(uid).set(profile).await()

        AuthResult.Success
    } catch (e: FirebaseAuthUserCollisionException) {
        AuthResult.Failure("An account with this email already exists.")
    } catch (e: Exception) {
        AuthResult.Failure(e.message ?: "Unable to register. Check your connection and try again.")
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        AuthResult.Success
    } catch (e: Exception) {
        AuthResult.Failure(e.message ?: "Unable to send reset email.")
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
