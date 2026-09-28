package it.streaminghub.tv.ui.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.player.TrackInfo
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.AccentRed
import it.streaminghub.tv.ui.theme.BgModal
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextDim
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onExit: () -> Unit
) {
    val resolutionState by viewModel.resolutionState.collectAsState()
    val playerUiState by viewModel.playerUiState.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showTrackDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Intercept Back button: Dialog -> Controls -> Exit Player
    BackHandler {
        when {
            showTrackDialog -> showTrackDialog = false
            showControls -> showControls = false
            else -> {
                viewModel.reportProgress()
                onExit()
            }
        }
    }

    // Auto-hide controls after 3.5s of inactivity when playing
    LaunchedEffect(showControls, playerUiState.isPlaying) {
        if (showControls && playerUiState.isPlaying && !showTrackDialog) {
            delay(3500)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    // If controls are hidden, any key press reveals controls without triggering action
                    if (!showControls) {
                        showControls = true
                        true
                    } else {
                        // Controls are visible: process D-pad navigation
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                viewModel.togglePlayPause()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                viewModel.seekBack()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                viewModel.seekForward()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                showTrackDialog = true
                                true
                            }
                            else -> false
                        }
                    }
                } else false
            }
    ) {
        // ExoPlayer Surface
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.playerManager.exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Indicator while resolving or buffering
        if (resolutionState.isResolving || playerUiState.isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (resolutionState.isResolving) "Risoluzione stream HLS in corso…" else "Buffering video…",
                        color = TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Error Banner
        if (resolutionState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPrimary),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(450.dp)
                ) {
                    Text(
                        text = "Errore Riproduzione",
                        color = AccentRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = resolutionState.error ?: "",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                    )
                    TvButton(
                        text = "Torna ai Dettagli",
                        onClick = onExit
                    )
                }
            }
        }

        // Custom TV OSD Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Vignette with Title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xCC000000), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.mediaTitle,
                            color = TextMain,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Audio & Subtitles button
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .tvFocusable(shape = CircleShape, onClick = { showTrackDialog = true })
                                    .clip(CircleShape)
                                    .background(Color(0x44FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = "Audio/Sottotitoli",
                                    tint = TextMain,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Gradient Vignette with Timeline & Playback Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xEE000000))
                            )
                        )
                        .padding(horizontal = 48.dp, vertical = 20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Time & Action indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (playerUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${formatDuration(playerUiState.currentPositionMs)} / ${formatDuration(playerUiState.durationMs)}",
                                color = TextMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "◄◄ 10s   |   OK: Play/Pausa   |   10s ►►   |   ▲ Audio/Sub",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Timeline Bar
                        val duration = playerUiState.durationMs
                        val progressFraction = if (duration > 0) {
                            (playerUiState.currentPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        val bufferedFraction = if (duration > 0) {
                            (playerUiState.bufferedPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            // Buffer bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = bufferedFraction)
                                    .height(6.dp)
                                    .background(Color(0x66FFFFFF))
                            )
                            // Play progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(6.dp)
                                    .background(PrimaryIndigo)
                            )
                        }
                    }
                }
            }
        }

        // Audio & Subtitle Track Chooser Modal
        if (showTrackDialog) {
            TrackSelectionDialog(
                audioTracks = playerUiState.audioTracks,
                subtitleTracks = playerUiState.subtitleTracks,
                onSelectAudio = {
                    viewModel.selectAudio(it)
                    showTrackDialog = false
                },
                onSelectSubtitle = {
                    viewModel.selectSubtitle(it)
                    showTrackDialog = false
                },
                onDismiss = { showTrackDialog = false }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackSelectionDialog(
    audioTracks: List<TrackInfo>,
    subtitleTracks: List<TrackInfo>,
    onSelectAudio: (TrackInfo) -> Unit,
    onSelectSubtitle: (TrackInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgModal)
                .padding(24.dp)
        ) {
            Text(
                text = "Audio & Sottotitoli",
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Audio Tracks
            Text(
                text = "Traccia Audio",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            for (track in audioTracks) {
                TrackRow(
                    label = track.label,
                    isSelected = track.isSelected,
                    onClick = { onSelectAudio(track) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Tracks
            Text(
                text = "Sottotitoli",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val noneSelected = subtitleTracks.none { it.isSelected }
            TrackRow(
                label = "Disattivati",
                isSelected = noneSelected,
                onClick = { onSelectSubtitle(null) }
            )

            for (track in subtitleTracks) {
                TrackRow(
                    label = track.label,
                    isSelected = track.isSelected,
                    onClick = { onSelectSubtitle(track) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            TvButton(
                text = "Chiudi",
                onClick = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.25f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isSelected) PrimaryIndigo else TextMain,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
