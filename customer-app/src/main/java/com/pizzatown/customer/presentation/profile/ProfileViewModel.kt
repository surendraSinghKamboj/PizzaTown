package com.pizzatown.customer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Address
import com.pizzatown.customer.domain.model.UserProfile
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileEditState(
    val fullName: String = "",
    val mobile: String = "",
    val pendingImageBytes: ByteArray? = null,
    val dateOfBirth: Long = 0L,
    val anniversaryDate: Long = 0L
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId

    private val _profileState = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val profileState: StateFlow<UiState<UserProfile>> = _profileState.asStateFlow()

    private val _editState = MutableStateFlow(ProfileEditState())
    val editState: StateFlow<ProfileEditState> = _editState.asStateFlow()

    private val _saveInProgress = MutableStateFlow(false)
    val saveInProgress: StateFlow<Boolean> = _saveInProgress.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init { load() }

    private fun load() {
        val uid = userId ?: return
        viewModelScope.launch {
            profileRepository.getProfile(uid)
                .onSuccess { profile ->
                    _profileState.value = UiState.Success(profile)
                    _editState.value = ProfileEditState(profile.fullName, profile.mobile, dateOfBirth = profile.dateOfBirth, anniversaryDate = profile.anniversaryDate)
                }
                .onFailure { _profileState.value = UiState.Error(it.message ?: "Unable to load profile.") }
        }
    }

    fun onNameChange(v: String) { _editState.value = _editState.value.copy(fullName = v) }
    fun onMobileChange(v: String) { _editState.value = _editState.value.copy(mobile = v) }
    fun onPendingImage(bytes: ByteArray) { _editState.value = _editState.value.copy(pendingImageBytes = bytes) }
    fun onDateOfBirthChange(epochMillis: Long) {
        // Once saved on the server, date of birth is locked — ignore further changes.
        val savedDob = (_profileState.value as? UiState.Success)?.data?.dateOfBirth ?: 0L
        if (savedDob > 0L) return
        _editState.value = _editState.value.copy(dateOfBirth = epochMillis)
    }
    fun onAnniversaryDateChange(epochMillis: Long) {
        val savedAnniversary = (_profileState.value as? UiState.Success)?.data?.anniversaryDate ?: 0L
        if (savedAnniversary > 0L) return
        _editState.value = _editState.value.copy(anniversaryDate = epochMillis)
    }

    fun save() {
        val uid = userId ?: return
        val current = (_profileState.value as? UiState.Success)?.data ?: return
        val edit = _editState.value

        viewModelScope.launch {
            _saveInProgress.value = true
            var imageUrl = current.profileImageUrl
            if (edit.pendingImageBytes != null) {
                profileRepository.uploadProfileImage(uid, edit.pendingImageBytes)
                    .onSuccess { imageUrl = it }
            }
            val updated = current.copy(
                fullName = edit.fullName.trim(),
                mobile = edit.mobile.trim(),
                profileImageUrl = imageUrl,
                // Defense in depth: once set on the server these are locked,
                // regardless of what edit state holds (see onDateOfBirthChange /
                // onAnniversaryDateChange, and the Firestore rule that rejects
                // this update server-side too).
                dateOfBirth = if (current.dateOfBirth > 0L) current.dateOfBirth else edit.dateOfBirth,
                anniversaryDate = if (current.anniversaryDate > 0L) current.anniversaryDate else edit.anniversaryDate
            )
            profileRepository.updateProfile(updated)
                .onSuccess {
                    _profileState.value = UiState.Success(updated)
                    _saveInProgress.value = false
                    _saveSuccess.value = true
                }
                .onFailure { _saveInProgress.value = false }
        }
    }

    fun consumeSaveSuccess() { _saveSuccess.value = false }

    // ---- Address management ----

    fun addAddress(label: String, fullAddress: String) {
        val current = (_profileState.value as? UiState.Success)?.data ?: return
        if (fullAddress.isBlank()) return
        val newAddress = Address(
            id = UUID.randomUUID().toString(),
            label = label.ifBlank { "Address" },
            fullAddress = fullAddress.trim(),
            isDefault = current.addresses.isEmpty() // first address becomes default automatically
        )
        persistAddresses(current, current.addresses + newAddress)
    }

    fun updateAddress(addressId: String, label: String, fullAddress: String) {
        val current = (_profileState.value as? UiState.Success)?.data ?: return
        val updated = current.addresses.map {
            if (it.id == addressId) it.copy(label = label.ifBlank { "Address" }, fullAddress = fullAddress.trim()) else it
        }
        persistAddresses(current, updated)
    }

    fun deleteAddress(addressId: String) {
        val current = (_profileState.value as? UiState.Success)?.data ?: return
        val remaining = current.addresses.filterNot { it.id == addressId }
        // if we deleted the default one, promote the first remaining address
        val fixed = if (remaining.none { it.isDefault } && remaining.isNotEmpty()) {
            remaining.mapIndexed { index, address -> if (index == 0) address.copy(isDefault = true) else address }
        } else remaining
        persistAddresses(current, fixed)
    }

    fun setDefaultAddress(addressId: String) {
        val current = (_profileState.value as? UiState.Success)?.data ?: return
        val updated = current.addresses.map { it.copy(isDefault = it.id == addressId) }
        persistAddresses(current, updated)
    }

    private fun persistAddresses(current: UserProfile, addresses: List<Address>) {
        val updatedProfile = current.copy(addresses = addresses)
        viewModelScope.launch {
            profileRepository.updateProfile(updatedProfile).onSuccess {
                _profileState.value = UiState.Success(updatedProfile)
            }
        }
    }

    fun logout() = authRepository.logout()
}
