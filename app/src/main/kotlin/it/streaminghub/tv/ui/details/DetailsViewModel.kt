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
    val resumeSeasonNumber: Int? = null,
    val resumeEpisodeNumber: Int? = null,
    val errorMessage: String? = null,
    val playbackFeedbackMessage: String? = null
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

    fun loadTitleDetails(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, playbackFeedbackMessage = null)
            val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
            val serverUrl = preferences.serverUrl.firstOrNull()

            catalogRepository.getTitleDetails(mediaType, titleId, profileId, refresh = refresh)
                .onSuccess { details ->
                    val defaultSource = details.resolvedSources.firstOrNull { it.available } ?: details.resolvedSources.firstOrNull()
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
                        selectSeason(firstSeasonNumber, refresh = refresh)
                    }

                    // Check watch history for resume point
                    playbackRepository.getContinueWatching(profileId).onSuccess { list ->
                        val matched = list.firstOrNull { it.mediaId == titleId }
                        if (matched != null && matched.progressSeconds > 10) {
                            _uiState.value = _uiState.value.copy(
                                resumeSeconds = matched.progressSeconds.toLong(),
                                resumeSeasonNumber = matched.seasonNumber,
                                resumeEpisodeNumber = matched.episodeNumber
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

    fun selectSeason(seasonNumber: Int, refresh: Boolean = false) {
        viewModelScope.launch {
            val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
            _uiState.value = _uiState.value.copy(selectedSeasonNumber = seasonNumber)

            catalogRepository.getSeasonEpisodes(titleId, seasonNumber, profileId, refresh = refresh)
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

    fun playTitle(
        fromBeginning: Boolean,
        onPlayMovie: (sourceUrl: String, title: String, mediaId: String, seekSeconds: Long) -> Unit,
        onPlayEpisode: (sourceUrl: String, seriesTitle: String, mediaId: String, season: Int, episode: Int, seekSeconds: Long) -> Unit
    ) {
        val currentItem = _uiState.value.item ?: return
        val resumeSec = if (fromBeginning) 0L else _uiState.value.resumeSeconds

        if (currentItem.type == "tv") {
            viewModelScope.launch {
                val targetSeason = if (!fromBeginning && _uiState.value.resumeSeasonNumber != null) {
                    _uiState.value.resumeSeasonNumber!!
                } else {
                    currentItem.seasons.firstOrNull()?.number ?: 1
                }
                val targetEpisode = if (!fromBeginning && _uiState.value.resumeEpisodeNumber != null) {
                    _uiState.value.resumeEpisodeNumber!!
                } else {
                    1
                }

                // Retrieve episodes for target season
                val episodes = if (_uiState.value.selectedSeasonNumber == targetSeason && _uiState.value.currentSeasonEpisodes.isNotEmpty()) {
                    _uiState.value.currentSeasonEpisodes
                } else {
                    val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
                    catalogRepository.getSeasonEpisodes(titleId, targetSeason, profileId).getOrNull()?.episodes ?: emptyList()
                }

                val ep = episodes.firstOrNull { it.episodeNumber == targetEpisode } ?: episodes.firstOrNull()
                if (ep != null) {
                    val epSource = ep.sources.firstOrNull { it.available } ?: ep.sources.firstOrNull()
                    if (epSource != null && epSource.pageUrl.isNotBlank()) {
                        onPlayEpisode(
                            epSource.pageUrl,
                            "${currentItem.title} - S${ep.seasonNumber}E${ep.episodeNumber}",
                            currentItem.id,
                            ep.seasonNumber,
                            ep.episodeNumber,
                            resumeSec
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            playbackFeedbackMessage = "Nessuna sorgente video disponibile per S${ep.seasonNumber}E${ep.episodeNumber}."
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        playbackFeedbackMessage = "Episodio non trovato per la stagione $targetSeason."
                    )
                }
            }
        } else {
            // Movie
            val source = _uiState.value.selectedSource ?: currentItem.resolvedSources.firstOrNull()
            if (source != null && source.pageUrl.isNotBlank()) {
                onPlayMovie(source.pageUrl, currentItem.title, currentItem.id, resumeSec)
            } else {
                _uiState.value = _uiState.value.copy(
                    playbackFeedbackMessage = "Nessuna sorgente video disponibile per questo film."
                )
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(playbackFeedbackMessage = null)
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
