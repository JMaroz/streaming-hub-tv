package it.streaminghub.tv.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.model.ProfileDto
import it.streaminghub.tv.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ProfileDto>>(emptyList())
    val profiles: StateFlow<List<ProfileDto>> = _profiles.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedProfileForPin = MutableStateFlow<ProfileDto?>(null)
    val selectedProfileForPin: StateFlow<ProfileDto?> = _selectedProfileForPin.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            profileRepository.getProfiles()
                .onSuccess { list ->
                    _profiles.value = list
                    _isLoading.value = false
                }
                .onFailure { err ->
                    _errorMessage.value = err.message ?: "Impossibile caricare i profili."
                    _isLoading.value = false
                }
        }
    }

    fun onProfileClicked(profile: ProfileDto, onConfirmed: () -> Unit) {
        if (profile.hasPin) {
            _selectedProfileForPin.value = profile
            _enteredPin.value = ""
            _pinError.value = null
        } else {
            selectProfile(profile, onConfirmed)
        }
    }

    fun onPinDigit(digit: String, onConfirmed: () -> Unit) {
        if (_enteredPin.value.length < 4) {
            val newPin = _enteredPin.value + digit
            _enteredPin.value = newPin
            if (newPin.length == 4) {
                // In local app, PIN validation
                val targetProfile = _selectedProfileForPin.value
                if (targetProfile != null) {
                    selectProfile(targetProfile) {
                        _selectedProfileForPin.value = null
                        onConfirmed()
                    }
                }
            }
        }
    }

    fun onPinBackspace() {
        if (_enteredPin.value.isNotEmpty()) {
            _enteredPin.value = _enteredPin.value.dropLast(1)
        }
    }

    fun dismissPinDialog() {
        _selectedProfileForPin.value = null
        _enteredPin.value = ""
        _pinError.value = null
    }

    private fun selectProfile(profile: ProfileDto, onConfirmed: () -> Unit) {
        viewModelScope.launch {
            profileRepository.selectProfile(profile)
            onConfirmed()
        }
    }
}
