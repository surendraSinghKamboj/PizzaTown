package com.pizzatown.admin.presentation.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.domain.model.DeliveryPartner
import com.pizzatown.admin.domain.repository.DeliveryPartnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DeliveryPartnersViewModel @Inject constructor(
    private val repository: DeliveryPartnerRepository
) : ViewModel() {

    private val _partners = MutableStateFlow<List<DeliveryPartner>>(emptyList())
    val partners: StateFlow<List<DeliveryPartner>> = _partners.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                repository.getPartners()
            }.onSuccess {
                _partners.value = it.sortedBy { partner -> partner.name.lowercase() }
                _message.value = null
            }.onFailure {
                _message.value = it.message ?: "Unable to load delivery partners."
            }
            _loading.value = false
        }
    }

    fun createPartner(
        name: String,
        email: String,
        phone: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (
            name.isBlank() ||
            email.isBlank() ||
            phone.isBlank() ||
            password.length < 6
        ) {
            _message.value = "Enter all details. Password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            _loading.value = true

            runCatching {
                repository.createPartner(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password
                )
            }.onSuccess {
                _message.value = "Delivery partner created successfully."
                refresh()
                onSuccess()
            }.onFailure {
                _message.value = it.message ?: "Unable to create delivery partner."
            }

            _loading.value = false
        }
    }

    fun updatePartner(
        partner: DeliveryPartner,
        name: String,
        email: String,
        phone: String,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            _message.value = "Name, email and phone are required."
            return
        }

        viewModelScope.launch {
            _loading.value = true

            runCatching {
                repository.updatePartner(
                    uid = partner.id,
                    name = name,
                    email = email,
                    phone = phone
                )
            }.onSuccess {
                _message.value = "Delivery partner updated successfully."
                refresh()
                onSuccess()
            }.onFailure {
                _message.value =
                    it.message ?: "Unable to update delivery partner."
            }

            _loading.value = false
        }
    }

    fun resetPassword(
        partner: DeliveryPartner,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (password.length < 6) {
            _message.value =
                "Password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            _loading.value = true

            runCatching {
                repository.resetPassword(
                    uid = partner.id,
                    password = password
                )
            }.onSuccess {
                _message.value =
                    "Password reset successfully."
                onSuccess()
            }.onFailure {
                _message.value =
                    it.message ?: "Unable to reset password."
            }

            _loading.value = false
        }
    }

    fun deletePartner(
        partner: DeliveryPartner,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true

            runCatching {
                repository.deletePartner(partner.id)
            }.onSuccess {
                _message.value =
                    "Delivery partner deleted successfully."
                refresh()
                onSuccess()
            }.onFailure {
                _message.value =
                    it.message ?: "Unable to delete delivery partner."
            }

            _loading.value = false
        }
    }

    fun toggleActive(partner: DeliveryPartner) {
        viewModelScope.launch {
            runCatching {
                repository.setPartnerActive(
                    uid = partner.id,
                    active = !partner.active
                )
            }.onSuccess {
                refresh()
            }.onFailure {
                _message.value = it.message ?: "Unable to update account."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
