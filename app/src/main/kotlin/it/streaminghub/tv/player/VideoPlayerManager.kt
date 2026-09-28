package it.streaminghub.tv.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val errorMessage: String? = null
)

class VideoPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters()
                .setPreferredAudioLanguage("ita")
                .setPreferredTextLanguage("ita")
                .setSelectUndeterminedTextLanguage(false)
        )
    }

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000, // minBufferMs
            50_000, // maxBufferMs
            2_500,  // bufferForPlaybackMs
            5_000   // bufferForPlaybackAfterRebufferMs
        )
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(
            DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        )
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isEnded = playbackState == Player.STATE_ENDED
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L

                _uiState.value = _uiState.value.copy(
                    isBuffering = isBuffering,
                    isEnded = isEnded,
                    durationMs = duration
                )

                if (playbackState == Player.STATE_READY) {
                    updateTracks(exoPlayer.currentTracks)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks(tracks)
            }
        })
    }

    fun playStream(url: String, headers: Map<String, String> = emptyMap(), startPositionMs: Long = 0L) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        if (headers.isNotEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(headers)
        }

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        val mediaSource = HlsMediaSource.Factory(httpDataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        if (startPositionMs > 0L) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekForward(deltaMs: Long = 10_000L) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceAtMost(exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        updateCurrentProgress()
    }

    fun seekBack(deltaMs: Long = 10_000L) {
        val target = (exoPlayer.currentPosition - deltaMs).coerceAtLeast(0L)
        exoPlayer.seekTo(target)
        updateCurrentProgress()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        updateCurrentProgress()
    }

    fun selectAudioTrack(trackInfo: TrackInfo) {
        val tracks = exoPlayer.currentTracks
        val group = tracks.groups.getOrNull(trackInfo.groupIndex) ?: return
        trackSelector.parameters = trackSelector.parameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, trackInfo.trackIndex)
            )
            .build()
    }

    fun selectSubtitleTrack(trackInfo: TrackInfo?) {
        if (trackInfo == null) {
            // Disable subtitles
            trackSelector.parameters = trackSelector.parameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val tracks = exoPlayer.currentTracks
            val group = tracks.groups.getOrNull(trackInfo.groupIndex) ?: return
            trackSelector.parameters = trackSelector.parameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, trackInfo.trackIndex)
                )
                .build()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                updateCurrentProgress()
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
        updateCurrentProgress()
    }

    private fun updateCurrentProgress() {
        val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = if (exoPlayer.duration > 0) exoPlayer.duration else _uiState.value.durationMs
        val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
        _uiState.value = _uiState.value.copy(
            currentPositionMs = cur,
            durationMs = dur,
            bufferedPositionMs = buf
        )
    }

    private fun updateTracks(tracks: Tracks) {
        val audios = mutableListOf<TrackInfo>()
        val subtitles = mutableListOf<TrackInfo>()

        for (gIdx in 0 until tracks.groups.size) {
            val group = tracks.groups[gIdx]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (tIdx in 0 until group.length) {
                    val format = group.getTrackFormat(tIdx)
                    val label = format.label ?: format.language ?: "Traccia Audio ${tIdx + 1}"
                    audios.add(
                        TrackInfo(
                            groupIndex = gIdx,
                            trackIndex = tIdx,
                            label = label,
                            language = format.language,
                            isSelected = group.isTrackSelected(tIdx)
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (tIdx in 0 until group.length) {
                    val format = group.getTrackFormat(tIdx)
                    val label = format.label ?: format.language ?: "Sottotitolo ${tIdx + 1}"
                    subtitles.add(
                        TrackInfo(
                            groupIndex = gIdx,
                            trackIndex = tIdx,
                            label = label,
                            language = format.language,
                            isSelected = group.isTrackSelected(tIdx)
                        )
                    )
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            audioTracks = audios,
            subtitleTracks = subtitles
        )
    }

    fun release() {
        stopProgressTracker()
        exoPlayer.stop()
        exoPlayer.release()
    }
}
