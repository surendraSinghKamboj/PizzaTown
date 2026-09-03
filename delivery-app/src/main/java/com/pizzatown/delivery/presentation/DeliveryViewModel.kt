package com.pizzatown.delivery.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pizzatown.delivery.data.DeliveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val repository: DeliveryRepository
) : ViewModel() {

    private val _orders =
        MutableStateFlow(emptyList<com.pizzatown.delivery.domain.model.DeliveryOrder>())
    val orders: StateFlow<List<com.pizzatown.delivery.domain.model.DeliveryOrder>> = _orders

    private val _history =
        MutableStateFlow(
            emptyList<com.pizzatown.delivery.domain.model.DeliveryOrder>()
        )
    val history: StateFlow<List<com.pizzatown.delivery.domain.model.DeliveryOrder>> =
        _history

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    init {
        attachExistingSession()
    }

    private fun attachExistingSession() {
        val user = auth.currentUser ?: run {
            Log.d("DeliveryOrders", "No existing Firebase session")
            return
        }

        Log.d(
            "DeliveryOrders",
            "Existing Firebase session found uid=${user.uid}"
        )

        user.getIdToken(true)
            .addOnSuccessListener { result ->
                val role = result.claims["role"] as? String

                Log.d(
                    "DeliveryOrders",
                    "Existing session role=$role uid=${user.uid}"
                )

                if (role == "delivery") {
                    observeOrders(user.uid)
                } else {
                    Log.e(
                        "DeliveryOrders",
                        "Existing session is not delivery role: $role"
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.e(
                    "DeliveryOrders",
                    "Unable to refresh existing session token",
                    error
                )
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {

        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Enter email and password."
            return
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                val user = auth.currentUser

                if (user == null) {
                    _loginError.value = "Login succeeded but user session is missing."
                    return@addOnSuccessListener
                }

                user.getIdToken(true)
                    .addOnSuccessListener { result ->
                        val role = result.claims["role"] as? String

                        Log.d(
                            "DeliveryOrders",
                            "Login role=$role uid=${user.uid}"
                        )

                        if (role != "delivery") {
                            auth.signOut()
                            _loginError.value =
                                "This account is not a delivery account."
                        } else {
                            observeOrders(user.uid)
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { error ->
                        _loginError.value =
                            error.message ?: "Unable to verify delivery account."
                    }
            }
            .addOnFailureListener {
                _loginError.value = it.message ?: "Login failed."
            }
    }

    private fun observeOrders(uid: String) {

        Log.d(
            "DeliveryOrders",
            "Starting order observers uid=$uid"
        )

        viewModelScope.launch {
            try {
                repository.observeAssignedOrders(uid)
                    .collect {
                        Log.d(
                            "DeliveryOrders",
                            "Active order state received count=${it.size}"
                        )
                        _orders.value = it
                    }
            } catch (error: Throwable) {
                Log.e(
                    "DeliveryOrders",
                    "Active order observer failed",
                    error
                )
            }
        }

        viewModelScope.launch {
            try {
                repository.observeDeliveredOrders(uid)
                    .collect {
                        Log.d(
                            "DeliveryOrders",
                            "History state received count=${it.size}"
                        )
                        _history.value = it
                    }
            } catch (error: Throwable) {
                Log.e(
                    "DeliveryOrders",
                    "History observer failed",
                    error
                )
            }
        }
    }

    fun markPickedUp(
        orderId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.markPickedUp(orderId)
            }.onSuccess {
                onSuccess()
            }.onFailure {
                onFailure(
                    it.message ?: "Unable to pick up order."
                )
            }
        }
    }

    fun markDelivered(
        orderId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.markDelivered(orderId)
            }.onSuccess {
                onSuccess()
            }.onFailure {
                onFailure(
                    it.message ?: "Unable to mark order delivered."
                )
            }
        }
    }

    fun logout() {
        auth.signOut()
        _orders.value = emptyList()
        _history.value = emptyList()
        _loginError.value = null
    }
}
