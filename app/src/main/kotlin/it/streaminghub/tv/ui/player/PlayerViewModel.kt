package it.streaminghub.tv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.local.AppPreferences
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import it.streaminghub.tv.data.model.NextEpisodeDetailDto
import it.streaminghub.tv.data.model.ProgressRequestDto
import it.streaminghub.tv.data.model.SkipIntervalDto
import it.streaminghub.tv.data.model.SkipSegmentsResponseDto
import it.streaminghub.tv.data.repository.CatalogRepository
import it.streaminghub.tv.data.repository.PlaybackRepository
import it.streaminghub.tv.player.PlayerUiState
import it.streaminghub.tv.player.TrackInfo
import it.streaminghub.tv.player.VideoPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class StreamResolutionState(
    val isResolving: Boolean = true,
    val streamUrl: String? = null,
    val token: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class PlayerBingeState(
    val skipSegments: SkipSegmentsResponseDto? = null,
    val isInsideIntro: Boolean = false,
    val currentIntro: SkipIntervalDto? = null,
    val isInsideCredits: Boolean = false,
    val currentOutro: SkipIntervalDto? = null,
    val nextEpisode: NextEpisodeDetailDto? = null,
    val nextCountdownSeconds: Int? = null,
    val isAutoDismissed: Boolean = false
)

class PlayerViewModel(
    application: Application,
    private var pageUrl: String,
    initialMediaTitle: String,
    private var mediaId: String,
    private val mediaType: String,
    private var seasonNumber: Int?,
    private var episodeNumber: Int?,
    private val initialSeekSeconds: Long,
    private val playbackRepository: PlaybackRepository,
    private val catalogRepository: CatalogRepository,
    private val preferences: AppPreferences
) : AndroidViewModel(application) {

    var mediaTitle: String = initialMediaTitle
        private set

    val playerManager = VideoPlayerManager(application, viewModelScope)
    val playerUiState: StateFlow<PlayerUiState> = playerManager.uiState

    private val _resolutionState = MutableStateFlow(StreamResolutionState())
    val resolutionState: StateFlow<StreamResolutionState> = _resolutionState.asStateFlow()

    private val _bingeState = MutableStateFlow(PlayerBingeState())
    val bingeState: StateFlow<PlayerBingeState> = _bingeState.asStateFlow()

    private var heartbeatJob: Job? = null
    private var positionObserverJob: Job? = null
    private var countdownJob: Job? = null

    init {
        resolveAndPlay()
        startProgressHeartbeat()
        startPositionObserver()
        if (mediaType == "tv" && seasonNumber != null && episodeNumber != null) {
            loadSkipSegmentsAndNextEpisode()
        }
    }

    private fun loadSkipSegmentsAndNextEpisode() {
        val sNum = seasonNumber ?: return
        val eNum = episodeNumber ?: return

        // 1. Fetch Skip Segments (Intro & Outro)
        viewModelScope.launch {
            playbackRepository.getSkipSegments(mediaId, sNum, eNum)
                .onSuccess { segments ->
                    if (segments.hasSegments) {
                        _bingeState.value = _bingeState.value.copy(
                            skipSegments = segments,
                            currentIntro = segments.intro,
                            currentOutro = segments.outro
                        )
                    }
                }
        }

        // 2. Pre-fetch Next Episode for sequential binge-watching
        viewModelScope.launch {
            catalogRepository.getNextEpisode(mediaId, sNum, eNum)
                .onSuccess { nextResp ->
                    if (nextResp.hasNext && nextResp.next != null) {
                        _bingeState.value = _bingeState.value.copy(nextEpisode = nextResp.next)
                    }
                }
        }
    }

    private fun startPositionObserver() {
        positionObserverJob?.cancel()
        positionObserverJob = viewModelScope.launch {
            playerUiState.collect { uiState ->
                val curSec = uiState.currentPositionMs / 1000.0
                val durSec = uiState.durationMs / 1000.0
                val bState = _bingeState.value

                // Intro detection
                val intro = bState.currentIntro
                val isIntro = intro != null && curSec >= intro.start && curSec < (intro.end - 0.5)

                // Outro / Credits detection
                val outro = bState.currentOutro
                val isOutro = if (outro != null && outro.start > 30) {
                    curSec >= outro.start && (outro.end <= 0 || curSec < outro.end)
                } else if (durSec > 60 && curSec >= (durSec - 25.0)) {
                    // Fallback to last 25s if duration is known and no outro marker
                    true
                } else {
                    false
                }

                val showNextPrompt = (isOutro || uiState.isEnded) && bState.nextEpisode != null && !bState.isAutoDismissed

                _bingeState.value = bState.copy(
                    isInsideIntro = isIntro,
                    isInsideCredits = showNextPrompt
                )

                if (showNextPrompt && countdownJob == null && bState.nextCountdownSeconds == null) {
                    startNextEpisodeCountdown()
                }
            }
        }
    }

    private fun startNextEpisodeCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = 12
            while (remaining >= 0 && isActive) {
                _bingeState.value = _bingeState.value.copy(nextCountdownSeconds = remaining)
                if (remaining == 0) {
                    playNextEpisode()
                    break
                }
                delay(1000)
                remaining--
            }
        }
    }

    fun dismissNextEpisodePrompt() {
        countdownJob?.cancel()
        countdownJob = null
        _bingeState.value = _bingeState.value.copy(
            isAutoDismissed = true,
            isInsideCredits = false,
            nextCountdownSeconds = null
        )
    }

    fun skipIntro() {
        val intro = _bingeState.value.currentIntro ?: return
        val targetMs = ((intro.end + 0.5) * 1000).toLong()
        playerManager.seekTo(targetMs)
        _bingeState.value = _bingeState.value.copy(isInsideIntro = false)
    }

    fun playNextEpisode() {
        countdownJob?.cancel()
        countdownJob = null
        val next = _bingeState.value.nextEpisode ?: return

        // 1. Report progress on current episode before switching
        reportProgress()

        // 2. Extract next episode source
        val nextEpisodeItem = next.episode
        val nextSource = nextEpisodeItem.sources.firstOrNull { it.available } ?: nextEpisodeItem.sources.firstOrNull()
        val nextUrl = nextSource?.pageUrl ?: ""

        if (nextUrl.isEmpty()) return

        // 3. Update state
        pageUrl = nextUrl
        seasonNumber = next.seasonNumber
        episodeNumber = next.episodeNumber
        mediaTitle = "${nextEpisodeItem.title.ifEmpty { "Episodio ${next.episodeNumber}" }}"

        _bingeState.value = PlayerBingeState()

        // 4. Resolve and play next episode
        resolveAndPlay(startPosMs = 0L)
        loadSkipSegmentsAndNextEpisode()
    }

    private fun resolveAndPlay(startPosMs: Long = initialSeekSeconds * 1000L) {
        viewModelScope.launch {
            _resolutionState.value = StreamResolutionState(isResolving = true)
            val serverUrl = preferences.serverUrl.firstOrNull()

            val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
            val isSubEnabled = preferences.isSubtitleEnabledForMedia(profileId, mediaId)
            val savedSubLang = if (isSubEnabled) preferences.getSubtitleLanguageForMedia(profileId, mediaId) else null
            playerManager.setInitialSubtitlePreference(enabled = isSubEnabled, preferredLanguage = savedSubLang)

            // Concurrently search subtitles if available
            val subtitleConfigList = mutableListOf<MediaItem.SubtitleConfiguration>()
            try {
                val subList = playbackRepository.searchSubtitles(
                    query = mediaTitle,
                    season = seasonNumber,
                    episode = episodeNumber
                ).getOrDefault(emptyList())

                subList.forEach { sub ->
                    val fullUrl = sub.getFullUrl(serverUrl)
                    val config = MediaItem.SubtitleConfiguration.Builder(Uri.parse(fullUrl))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(sub.language)
                        .setLabel(sub.label)
                        .setSelectionFlags(0)
                        .build()
                    subtitleConfigList.add(config)
                }
            } catch (_: Exception) {}

            val preferFhd = preferences.preferFhd.firstOrNull() ?: true
            playbackRepository.resolveStream(
                pageUrl = pageUrl,
                providerId = null,
                mediaId = mediaId,
                quality = if (preferFhd) "1080p" else "720p",
                preferFhd = preferFhd
            ).onSuccess { res ->
                _resolutionState.value = StreamResolutionState(
                    isResolving = false,
                    streamUrl = res.lanStreamUrl,
                    token = res.token,
                    headers = res.headers
                )
                playerManager.playStream(
                    url = res.lanStreamUrl,
                    headers = res.headers,
                    startPositionMs = startPosMs,
                    subtitleConfigurations = subtitleConfigList
                )
            }.onFailure { err ->
                _resolutionState.value = StreamResolutionState(
                    isResolving = false,
                    error = err.message ?: "Risoluzione del flusso video non riuscita."
                )
            }
        }
    }

    private fun startProgressHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000) // Send progress every 10 seconds
                reportProgress()
            }
        }
    }

    fun reportProgress() {
        viewModelScope.launch {
            val curMs = playerUiState.value.currentPositionMs
            val durMs = playerUiState.value.durationMs
            if (durMs > 0) {
                val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
                playbackRepository.reportProgress(
                    ProgressRequestDto(
                        mediaId = mediaId,
                        title = mediaTitle,
                        mediaType = mediaType,
                        seasonNumber = if (seasonNumber != null && seasonNumber!! > 0) seasonNumber else null,
                        episodeNumber = if (episodeNumber != null && episodeNumber!! > 0) episodeNumber else null,
                        progressSeconds = curMs / 1000.0,
                        durationSeconds = durMs / 1000.0,
                        profileId = profileId
                    )
                )
            }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun seekForward() = playerManager.seekForward(10_000L)
    fun seekBack() = playerManager.seekBack(10_000L)
    fun seekTo(posMs: Long) = playerManager.seekTo(posMs)
    fun selectAudio(track: TrackInfo) = playerManager.selectAudioTrack(track)
    fun selectSubtitle(track: TrackInfo?) {
        playerManager.selectSubtitleTrack(track)
        viewModelScope.launch {
            val profileId = preferences.activeProfileId.firstOrNull() ?: "default"
            if (track == null) {
                preferences.setSubtitleEnabledForMedia(profileId, mediaId, enabled = false)
            } else {
                preferences.setSubtitleEnabledForMedia(
                    profileId = profileId,
                    mediaId = mediaId,
                    enabled = true,
                    language = track.language ?: track.label
                )
            }
        }
    }

    override fun onCleared() {
        reportProgress()
        countdownJob?.cancel()
        positionObserverJob?.cancel()
        heartbeatJob?.cancel()
        playerManager.release()
        super.onCleared()
    }
}
