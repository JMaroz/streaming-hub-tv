package it.streaminghub.tv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.ProgressRequestDto
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

class PlayerViewModel(
    application: Application,
    private val pageUrl: String,
    val mediaTitle: String,
    private val mediaId: String,
    private val mediaType: String,
    private val seasonNumber: Int?,
    private val episodeNumber: Int?,
    private val initialSeekSeconds: Long,
    private val playbackRepository: PlaybackRepository,
    private val preferences: AppPreferences
) : AndroidViewModel(application) {

    val playerManager = VideoPlayerManager(application, viewModelScope)
    val playerUiState: StateFlow<PlayerUiState> = playerManager.uiState

    private val _resolutionState = MutableStateFlow(StreamResolutionState())
    val resolutionState: StateFlow<StreamResolutionState> = _resolutionState.asStateFlow()

    private var heartbeatJob: Job? = null

    init {
        resolveAndPlay()
        startProgressHeartbeat()
    }

    private fun resolveAndPlay() {
        viewModelScope.launch {
            _resolutionState.value = StreamResolutionState(isResolving = true)
            playbackRepository.resolveStream(
                pageUrl = pageUrl,
                providerId = null,
                mediaId = mediaId,
                quality = "1080p",
                preferFhd = true
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
                    startPositionMs = initialSeekSeconds * 1000L
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
                        seasonNumber = if (seasonNumber != null && seasonNumber > 0) seasonNumber else null,
                        episodeNumber = if (episodeNumber != null && episodeNumber > 0) episodeNumber else null,
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
    fun selectSubtitle(track: TrackInfo?) = playerManager.selectSubtitleTrack(track)

    override fun onCleared() {
        reportProgress()
        heartbeatJob?.cancel()
        playerManager.release()
        super.onCleared()
    }
}
