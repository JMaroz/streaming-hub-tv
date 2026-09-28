package it.streaminghub.tv.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.api.DiscoveryState
import it.streaminghub.tv.data.api.ServerDiscoveryManager
import it.streaminghub.tv.data.local.AppPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SetupViewModel(
    private val preferences: AppPreferences,
    private val discoveryManager: ServerDiscoveryManager = ServerDiscoveryManager()
) : ViewModel() {

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _manualUrl = MutableStateFlow("http://192.168.1.100:8099")
    val manualUrl: StateFlow<String> = _manualUrl.asStateFlow()

    private val _isManualTesting = MutableStateFlow(false)
    val isManualTesting: StateFlow<Boolean> = _isManualTesting.asStateFlow()

    private val _manualError = MutableStateFlow<String?>(null)
    val manualError: StateFlow<String?> = _manualError.asStateFlow()

    private var scanJob: Job? = null

    init {
        startDiscovery()
    }

    fun startDiscovery() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            discoveryManager.discoverServer().collect { state ->
                _discoveryState.value = state
            }
        }
    }

    fun setManualUrl(url: String) {
        _manualUrl.value = url
        _manualError.value = null
    }

    fun connectToServer(url: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isManualTesting.value = true
            _manualError.value = null

            val result = discoveryManager.testConnection(url)
            if (result.isSuccess) {
                preferences.setServerUrl(url)
                _isManualTesting.value = false
                onSuccess()
            } else {
                _isManualTesting.value = false
                _manualError.value = result.exceptionOrNull()?.message ?: "Connessione al server non riuscita."
            }
        }
    }
}
