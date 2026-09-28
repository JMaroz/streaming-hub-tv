package it.streaminghub.tv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.data.model.ProviderSourceDto
import it.streaminghub.tv.data.model.TvEpisodeDto
import it.streaminghub.tv.data.model.TvSeasonDto
import it.streaminghub.tv.data.repository.CatalogRepository
import it.streaminghub.tv.data.repository.FavoritesRepository
import it.streaminghub.tv.data.repository.PlaybackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val item: MediaItemDto? = null,
    val serverUrl: String? = null,
    val isFavorite: Boolean = false,
    val selectedSource: ProviderSourceDto? = null,
    val selectedSeasonNumber: Int = 1,
    val currentSeasonEpisodes: List<TvEpisodeDto> = emptyList(),
    val resumeSeconds: Long = 0L,
    val errorMessage: String? = null
)

class DetailsViewModel(
    private val mediaType: String,
    private val titleId: String,
    private val catalogRepository: CatalogRepository,
    private val playbackRepository: PlaybackRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadTitleDetails()
    }

    fun loadTitleDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
            val serverUrl = preferences.serverUrl.firstOrNull()

            catalogRepository.getTitleDetails(mediaType, titleId, profileId)
                .onSuccess { details ->
                    val defaultSource = details.sources.firstOrNull { it.available } ?: details.sources.firstOrNull()
                    val isFav = details.isFavorite

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        item = details,
                        serverUrl = serverUrl,
                        isFavorite = isFav,
                        selectedSource = defaultSource
                    )

                    // If TV series, load Season 1 episodes
                    if (details.type == "tv") {
                        val firstSeasonNumber = details.seasons.firstOrNull()?.number ?: 1
                        selectSeason(firstSeasonNumber)
                    }

                    // Check watch history for resume point
                    playbackRepository.getContinueWatching(profileId).onSuccess { list ->
                        val matched = list.firstOrNull { it.mediaId == titleId }
                        if (matched != null && matched.progressSeconds > 10) {
                            _uiState.value = _uiState.value.copy(
                                resumeSeconds = matched.progressSeconds.toLong()
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Impossibile recuperare i dettagli del titolo."
                    )
                }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        viewModelScope.launch {
            val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
            _uiState.value = _uiState.value.copy(selectedSeasonNumber = seasonNumber)

            catalogRepository.getSeasonEpisodes(titleId, seasonNumber, profileId)
                .onSuccess { seasonDto ->
                    _uiState.value = _uiState.value.copy(
                        currentSeasonEpisodes = seasonDto.episodes
                    )
                }
        }
    }

    fun selectSource(source: ProviderSourceDto) {
        _uiState.value = _uiState.value.copy(selectedSource = source)
    }

    fun toggleFavorite() {
        val currentItem = _uiState.value.item ?: return
        viewModelScope.launch {
            val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
            val newStatus = favoritesRepository.toggleFavorite(
                titleId = currentItem.id,
                mediaType = currentItem.type,
                title = currentItem.title,
                posterUrl = currentItem.posterUrl,
                profileId = profileId
            ).getOrDefault(!_uiState.value.isFavorite)

            _uiState.value = _uiState.value.copy(isFavorite = newStatus)
        }
    }
}
