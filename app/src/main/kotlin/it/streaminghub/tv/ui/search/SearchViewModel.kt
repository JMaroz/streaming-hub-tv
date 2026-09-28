package it.streaminghub.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.data.repository.CatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<MediaItemDto> = emptyList(),
    val serverUrl: String? = null,
    val errorMessage: String? = null
)

class SearchViewModel(
    private val catalogRepository: CatalogRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val serverUrl = preferences.serverUrl.firstOrNull()
            _uiState.value = _uiState.value.copy(serverUrl = serverUrl)
        }
    }

    fun setQuery(q: String) {
        _uiState.value = _uiState.value.copy(query = q)
        searchJob?.cancel()

        if (q.trim().length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400) // Debounce
                performSearch(q.trim())
            }
        } else if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, errorMessage = null)
        val profileId = preferences.activeProfileId.firstOrNull() ?: "default"

        catalogRepository.search(query = query, type = "all", profileId = profileId)
            .onSuccess { res ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    results = res.results
                )
            }
            .onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = err.message ?: "Errore durante la ricerca."
                )
            }
    }
}
