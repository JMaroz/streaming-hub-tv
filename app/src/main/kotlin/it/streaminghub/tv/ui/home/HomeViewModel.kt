package it.streaminghub.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.HomeCarouselDto
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

enum class HomeSection(val id: String, val title: String) {
    ALL("all", "Tutti"),
    MOVIES("movie", "Film"),
    SERIES("tv", "Serie TV"),
    ANIME("anime", "Anime")
}

data class SectionData(
    val heroItem: MediaItemDto? = null,
    val isCarouselsMode: Boolean = false,
    val carousels: List<HomeCarouselDto> = emptyList(),
    val latestTitles: List<MediaItemDto> = emptyList(),
    val categoryShelves: List<Pair<String, List<MediaItemDto>>> = emptyList()
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val serverUrl: String? = null,
    val activeProfileId: String = "default",
    val activeProfileName: String = "Principale",
    val selectedSection: HomeSection = HomeSection.ALL,
    val heroItem: MediaItemDto? = null,
    val isHeroFavorite: Boolean = false,
    val isCarouselsMode: Boolean = false,
    val carousels: List<HomeCarouselDto> = emptyList(),
    val continueWatching: List<MediaItemDto> = emptyList(),
    val continueProgressMap: Map<String, Float> = emptyMap(),
    val favorites: List<MediaItemDto> = emptyList(),
    val latestTitles: List<MediaItemDto> = emptyList(),
    val categoryShelves: List<Pair<String, List<MediaItemDto>>> = emptyList(),
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

    private val sectionCache = mutableMapOf<HomeSection, SectionData>()
    private var allContinueWatching: List<MediaItemDto> = emptyList()
    private var allProgressMap: Map<String, Float> = emptyMap()
    private var allFavorites: List<MediaItemDto> = emptyList()

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
                sectionCache.clear()
                allContinueWatching = emptyList()
                allProgressMap = emptyMap()
                allFavorites = emptyList()

                _uiState.value = _uiState.value.copy(
                    activeProfileId = profileId,
                    activeProfileName = profileName,
                    selectedSection = HomeSection.ALL,
                    isLoading = true,
                    heroItem = null,
                    carousels = emptyList(),
                    continueWatching = emptyList(),
                    favorites = emptyList(),
                    latestTitles = emptyList(),
                    categoryShelves = emptyList(),
                    latestMovies = emptyList(),
                    latestSeries = emptyList()
                )
                loadInitialDataForProfile(profileId, profileName)
            }
        }
    }

    fun loadHomeData() {
        val profileId = _uiState.value.activeProfileId
        val profileName = _uiState.value.activeProfileName
        sectionCache.clear()
        viewModelScope.launch {
            loadInitialDataForProfile(profileId, profileName)
        }
    }

    fun selectSection(section: HomeSection) {
        if (_uiState.value.selectedSection == section && !_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(selectedSection = section)
        filterPersonalizedShelvesForSection(section)

        val cached = sectionCache[section]
        if (cached != null) {
            applySectionData(section, cached)
        } else {
            viewModelScope.launch {
                loadSectionData(section, _uiState.value.activeProfileId)
            }
        }
    }

    private suspend fun loadInitialDataForProfile(profileId: String, profileName: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val continueDeferred = viewModelScope.async { playbackRepository.getContinueWatching(profileId) }
        val favoritesDeferred = viewModelScope.async { favoritesRepository.getFavorites(profileId) }

        val continueRes = continueDeferred.await()
        val favRes = favoritesDeferred.await()

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

        allContinueWatching = continueList
        allProgressMap = progressMap
        allFavorites = favRes.getOrDefault(emptyList())

        filterPersonalizedShelvesForSection(_uiState.value.selectedSection)
        loadSectionData(HomeSection.ALL, profileId)
    }

    private suspend fun loadSectionData(section: HomeSection, profileId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        when (section) {
            HomeSection.ALL -> {
                val homeCatalogRes = catalogRepository.getHomeCatalog(source = "all", profileId = profileId).getOrNull()
                if (homeCatalogRes != null && homeCatalogRes.mode == "carousels" && homeCatalogRes.carousels.isNotEmpty()) {
                    val hero = homeCatalogRes.hero ?: homeCatalogRes.carousels.firstOrNull()?.items?.firstOrNull()
                    val data = SectionData(
                        heroItem = hero,
                        isCarouselsMode = true,
                        carousels = homeCatalogRes.carousels
                    )
                    sectionCache[HomeSection.ALL] = data
                    applySectionData(HomeSection.ALL, data)
                } else {
                    val latestDef = viewModelScope.async { catalogRepository.getLatest(type = "all", page = 1, profileId = profileId) }
                    val moviesDef = viewModelScope.async { catalogRepository.getLatest(type = "movie", page = 1, profileId = profileId) }
                    val seriesDef = viewModelScope.async { catalogRepository.getLatest(type = "tv", page = 1, profileId = profileId) }
                    val animDef = viewModelScope.async { catalogRepository.getByGenre("Animazione", type = "movie", page = 1, profileId = profileId) }
                    val actionDef = viewModelScope.async { catalogRepository.getByGenre("Azione", type = "movie", page = 1, profileId = profileId) }

                    val latestList = latestDef.await().getOrNull()?.results ?: emptyList()
                    val movieList = moviesDef.await().getOrNull()?.results ?: emptyList()
                    val seriesList = seriesDef.await().getOrNull()?.results ?: emptyList()
                    val animList = animDef.await().getOrNull()?.results ?: emptyList()
                    val actionList = actionDef.await().getOrNull()?.results ?: emptyList()

                    val hero = homeCatalogRes?.hero ?: latestList.firstOrNull() ?: movieList.firstOrNull()
                    val shelves = listOf(
                        "Ultimi Arrivi" to latestList,
                        "Film Recenti" to movieList,
                        "Serie TV Popolari" to seriesList,
                        "Film d'Animazione & Famiglia" to animList,
                        "Azione e Avventura" to actionList
                    ).filter { it.second.isNotEmpty() }

                    val data = SectionData(
                        heroItem = hero,
                        isCarouselsMode = false,
                        latestTitles = latestList,
                        categoryShelves = shelves
                    )
                    sectionCache[HomeSection.ALL] = data
                    applySectionData(HomeSection.ALL, data)
                }
            }

            HomeSection.MOVIES -> {
                val latestDef = viewModelScope.async { catalogRepository.getLatest(type = "movie", page = 1, profileId = profileId) }
                val actionDef = viewModelScope.async { catalogRepository.getByGenre("Azione", type = "movie", page = 1, profileId = profileId) }
                val comedyDef = viewModelScope.async { catalogRepository.getByGenre("Commedia", type = "movie", page = 1, profileId = profileId) }
                val animDef = viewModelScope.async { catalogRepository.getByGenre("Animazione", type = "movie", page = 1, profileId = profileId) }
                val thrillerDef = viewModelScope.async { catalogRepository.getByGenre("Thriller", type = "movie", page = 1, profileId = profileId) }

                val latestList = latestDef.await().getOrNull()?.results ?: emptyList()
                val actionList = actionDef.await().getOrNull()?.results ?: emptyList()
                val comedyList = comedyDef.await().getOrNull()?.results ?: emptyList()
                val animList = animDef.await().getOrNull()?.results ?: emptyList()
                val thrillerList = thrillerDef.await().getOrNull()?.results ?: emptyList()

                val shelves = listOf(
                    "Film Recenti" to latestList,
                    "Azione e Avventura" to actionList,
                    "Commedie" to comedyList,
                    "Animazione e Famiglia" to animList,
                    "Thriller & Suspense" to thrillerList
                ).filter { it.second.isNotEmpty() }

                val data = SectionData(
                    heroItem = latestList.firstOrNull() ?: actionList.firstOrNull(),
                    isCarouselsMode = false,
                    latestTitles = latestList,
                    categoryShelves = shelves
                )
                sectionCache[HomeSection.MOVIES] = data
                applySectionData(HomeSection.MOVIES, data)
            }

            HomeSection.SERIES -> {
                val latestDef = viewModelScope.async { catalogRepository.getLatest(type = "tv", page = 1, profileId = profileId) }
                val dramaDef = viewModelScope.async { catalogRepository.getByGenre("Dramma", type = "tv", page = 1, profileId = profileId) }
                val comedyDef = viewModelScope.async { catalogRepository.getByGenre("Commedia", type = "tv", page = 1, profileId = profileId) }
                val sciFiDef = viewModelScope.async { catalogRepository.getByGenre("Sci-Fi & Fantasy", type = "tv", page = 1, profileId = profileId) }
                val crimeDef = viewModelScope.async { catalogRepository.getByGenre("Crime", type = "tv", page = 1, profileId = profileId) }

                val latestList = latestDef.await().getOrNull()?.results ?: emptyList()
                val dramaList = dramaDef.await().getOrNull()?.results ?: emptyList()
                val comedyList = comedyDef.await().getOrNull()?.results ?: emptyList()
                val sciFiList = sciFiDef.await().getOrNull()?.results ?: emptyList()
                val crimeList = crimeDef.await().getOrNull()?.results ?: emptyList()

                val shelves = listOf(
                    "Serie TV Recenti" to latestList,
                    "Serie TV Drammatiche" to dramaList,
                    "Commedie TV" to comedyList,
                    "Fantascienza & Mistero" to sciFiList,
                    "Crime & Investigazione" to crimeList
                ).filter { it.second.isNotEmpty() }

                val data = SectionData(
                    heroItem = latestList.firstOrNull() ?: dramaList.firstOrNull(),
                    isCarouselsMode = false,
                    latestTitles = latestList,
                    categoryShelves = shelves
                )
                sectionCache[HomeSection.SERIES] = data
                applySectionData(HomeSection.SERIES, data)
            }

            HomeSection.ANIME -> {
                val animeCatalogDef = viewModelScope.async { catalogRepository.getHomeCatalog(source = "anime", profileId = profileId) }
                val animeLatestDef = viewModelScope.async { catalogRepository.getLatest(type = "anime", source = "anime", page = 1, profileId = profileId) }

                val animeCatalogRes = animeCatalogDef.await().getOrNull()
                val animeLatestList = animeLatestDef.await().getOrNull()?.results ?: emptyList()

                val hasCarousels = animeCatalogRes != null && animeCatalogRes.carousels.isNotEmpty()
                val hero = animeCatalogRes?.hero ?: animeLatestList.firstOrNull() ?: animeCatalogRes?.carousels?.firstOrNull()?.items?.firstOrNull()

                val shelves = if (!hasCarousels && animeLatestList.isNotEmpty()) {
                    listOf("Ultimi Episodi Anime" to animeLatestList)
                } else emptyList()

                val data = SectionData(
                    heroItem = hero,
                    isCarouselsMode = hasCarousels,
                    carousels = animeCatalogRes?.carousels ?: emptyList(),
                    latestTitles = animeLatestList,
                    categoryShelves = shelves
                )
                sectionCache[HomeSection.ANIME] = data
                applySectionData(HomeSection.ANIME, data)
            }
        }
    }

    private fun filterPersonalizedShelvesForSection(section: HomeSection) {
        val filteredContinue: List<MediaItemDto>
        val filteredFavorites: List<MediaItemDto>

        when (section) {
            HomeSection.ALL -> {
                filteredContinue = allContinueWatching
                filteredFavorites = allFavorites
            }
            HomeSection.MOVIES -> {
                filteredContinue = allContinueWatching.filter { it.type == "movie" }
                filteredFavorites = allFavorites.filter { it.type == "movie" }
            }
            HomeSection.SERIES -> {
                filteredContinue = allContinueWatching.filter { it.type == "tv" }
                filteredFavorites = allFavorites.filter { it.type == "tv" }
            }
            HomeSection.ANIME -> {
                filteredContinue = allContinueWatching.filter { it.type == "anime" }
                filteredFavorites = allFavorites.filter { it.type == "anime" }
            }
        }

        _uiState.value = _uiState.value.copy(
            continueWatching = filteredContinue,
            continueProgressMap = allProgressMap,
            favorites = filteredFavorites
        )
    }

    private fun applySectionData(section: HomeSection, data: SectionData) {
        val isFav = data.heroItem?.let { h -> allFavorites.any { it.id == h.id } } ?: false
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            selectedSection = section,
            heroItem = data.heroItem,
            isHeroFavorite = isFav,
            isCarouselsMode = data.isCarouselsMode,
            carousels = data.carousels,
            latestTitles = data.latestTitles,
            categoryShelves = data.categoryShelves
        )
    }

    fun onCardFocused(item: MediaItemDto) {
        // ponytail: Keep hero stable to eliminate layout shifts and recomposition jank across rows
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
                allFavorites = list
                filterPersonalizedShelvesForSection(_uiState.value.selectedSection)
            }
        }
    }
}
