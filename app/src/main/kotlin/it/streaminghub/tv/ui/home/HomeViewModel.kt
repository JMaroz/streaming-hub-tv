package it.streaminghub.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.data.repository.CatalogRepository
import it.streaminghub.tv.data.repository.FavoritesRepository
import it.streaminghub.tv.data.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val serverUrl: String? = null,
    val activeProfileId: String = "default",
    val activeProfileName: String = "Principale",
    val heroItem: MediaItemDto? = null,
    val isHeroFavorite: Boolean = false,
    val continueWatching: List<MediaItemDto> = emptyList(),
    val continueProgressMap: Map<String, Float> = emptyMap(),
    val favorites: List<MediaItemDto> = emptyList(),
    val latestTitles: List<MediaItemDto> = emptyList(),
    val latestMovies: List<MediaItemDto> = emptyList(),
    val latestSeries: List<MediaItemDto> = emptyList(),
    val animationTitles: List<MediaItemDto> = emptyList(),
    val actionTitles: List<MediaItemDto> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val playbackRepository: PlaybackRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observePreferencesAndLoad()
    }

    private fun observePreferencesAndLoad() {
        viewModelScope.launch {
            preferences.serverUrl.collectLatest { sUrl ->
                _uiState.value = _uiState.value.copy(serverUrl = sUrl)
            }
        }

        viewModelScope.launch {
            preferences.activeProfileId.collectLatest { profileId ->
                val profileName = preferences.activeProfileName.firstOrNull() ?: "Principale"
                _uiState.value = _uiState.value.copy(
                    activeProfileId = profileId,
                    activeProfileName = profileName
                )
                loadHomeDataForProfile(profileId, profileName)
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val profileId = _uiState.value.activeProfileId
            val profileName = _uiState.value.activeProfileName
            loadHomeDataForProfile(profileId, profileName)
        }
    }

    private suspend fun loadHomeDataForProfile(profileId: String, profileName: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Parallel loading using async with strict profileId scoping
        val continueDeferred = viewModelScope.async { playbackRepository.getContinueWatching(profileId) }
        val favoritesDeferred = viewModelScope.async { favoritesRepository.getFavorites(profileId) }
        val latestDeferred = viewModelScope.async { catalogRepository.getLatest(type = "all", page = 1, profileId = profileId) }
        val moviesDeferred = viewModelScope.async { catalogRepository.getLatest(type = "movie", page = 1, profileId = profileId) }
        val seriesDeferred = viewModelScope.async { catalogRepository.getLatest(type = "tv", page = 1, profileId = profileId) }
        val animDeferred = viewModelScope.async { catalogRepository.getByGenre("Animazione", type = "movie", page = 1, profileId = profileId) }
        val actionDeferred = viewModelScope.async { catalogRepository.getByGenre("Azione", type = "movie", page = 1, profileId = profileId) }

        val continueRes = continueDeferred.await()
        val favRes = favoritesDeferred.await()
        val latestRes = latestDeferred.await()
        val moviesRes = moviesDeferred.await()
        val seriesRes = seriesDeferred.await()
        val animRes = animDeferred.await()
        val actionRes = actionDeferred.await()

        val continueList = mutableListOf<MediaItemDto>()
        val progressMap = mutableMapOf<String, Float>()
        continueRes.getOrNull()?.forEach { item ->
            continueList.add(
                MediaItemDto(
                    id = item.mediaId,
                    type = item.mediaType,
                    title = item.title,
                    posterUrl = item.posterUrl
                )
            )
            progressMap[item.mediaId] = item.progressPercent
        }

        val favList = favRes.getOrDefault(emptyList())
        val latestList = latestRes.getOrNull()?.results ?: emptyList()
        val movieList = moviesRes.getOrNull()?.results ?: emptyList()
        val seriesList = seriesRes.getOrNull()?.results ?: emptyList()
        val animList = animRes.getOrNull()?.results ?: emptyList()
        val actionList = actionRes.getOrNull()?.results ?: emptyList()

        val defaultHero = latestList.firstOrNull() ?: movieList.firstOrNull()
        val isHeroFav = defaultHero?.let { h -> favList.any { it.id == h.id } } ?: false

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            activeProfileId = profileId,
            activeProfileName = profileName,
            heroItem = defaultHero,
            isHeroFavorite = isHeroFav,
            continueWatching = continueList,
            continueProgressMap = progressMap,
            favorites = favList,
            latestTitles = latestList,
            latestMovies = movieList,
            latestSeries = seriesList,
            animationTitles = animList,
            actionTitles = actionList
        )
    }

    fun onCardFocused(item: MediaItemDto) {
        val isFav = _uiState.value.favorites.any { it.id == item.id }
        _uiState.value = _uiState.value.copy(
            heroItem = item,
            isHeroFavorite = isFav
        )
    }

    fun toggleFavoriteHero() {
        val currentHero = _uiState.value.heroItem ?: return
        viewModelScope.launch {
            val profileId = _uiState.value.activeProfileId
            val newFavStatus = favoritesRepository.toggleFavorite(
                titleId = currentHero.id,
                mediaType = currentHero.type,
                title = currentHero.title,
                posterUrl = currentHero.posterUrl,
                profileId = profileId
            ).getOrDefault(!_uiState.value.isHeroFavorite)

            _uiState.value = _uiState.value.copy(isHeroFavorite = newFavStatus)
            favoritesRepository.getFavorites(profileId).onSuccess { list ->
                _uiState.value = _uiState.value.copy(favorites = list)
            }
        }
    }
}
