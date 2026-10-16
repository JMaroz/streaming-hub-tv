package it.streaminghub.tv.ui.player

import android.app.Activity
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.WindowManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.AccentGold
import it.streaminghub.tv.ui.theme.AccentRed
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
    val bingeState by viewModel.bingeState.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var quickSeekIndicator by remember { mutableStateOf<String?>(null) }
    var userInteractionTrigger by remember { mutableIntStateOf(0) }

    val rootPlayerFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    // Intercept Back button:
    // If controls are shown, hide them first. If controls are already hidden, exit the player.
    BackHandler {
        when {
            bingeState.isInsideCredits -> viewModel.dismissNextEpisodePrompt()
            showControls -> {
                showControls = false
                try {
                    rootPlayerFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
            else -> {
                viewModel.reportProgress()
                onExit()
            }
        }
    }

    // Auto-dismiss quick seek HUD badge after 2 seconds
    LaunchedEffect(quickSeekIndicator) {
        if (quickSeekIndicator != null) {
            delay(2000)
            quickSeekIndicator = null
        }
    }

    // OLED Screensaver & Window keep-screen-on:
    // Keep screen awake dynamically during active playback; release in pause or on exit.
    val context = LocalContext.current
    DisposableEffect(playerUiState.isPlaying) {
        val activity = context as? Activity ?: (context as? ContextWrapper)?.baseContext as? Activity
        val window = activity?.window
        if (playerUiState.isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Lifecycle handler: pause playback and report progress when user exits app or turns off screen/TV
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pausePlayback()
                    viewModel.reportProgress()
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.pausePlayback()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-hide controls after 6 seconds of inactivity when playing
    LaunchedEffect(showControls, playerUiState.isPlaying, userInteractionTrigger) {
        if (showControls && playerUiState.isPlaying) {
            delay(6000)
            showControls = false
        }
    }

    // Focus management when toggling controls
    LaunchedEffect(showControls) {
        if (showControls) {
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            try {
                rootPlayerFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootPlayerFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode

                    // 1. Intercept BACK in preview (tunneling) phase:
                    // Prevents Compose FocusOwner from simply clearing focus and leaving controls trapped!
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (bingeState.isInsideCredits) {
                            viewModel.dismissNextEpisodePrompt()
                            return@onPreviewKeyEvent true
                        }
                        if (showControls) {
                            showControls = false
                            try {
                                rootPlayerFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                            return@onPreviewKeyEvent true
                        }
                        // Allow bubble to BackHandler to report progress and exit safely
                        return@onPreviewKeyEvent false
                    }

                    userInteractionTrigger++

                    // 2. Hardware media keys (active both when controls are open or hidden)
                    when (keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            viewModel.togglePlayPause()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            viewModel.pausePlayback()
                            viewModel.reportProgress()
                            onExit()
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            viewModel.seekBack()
                            if (!showControls) quickSeekIndicator = "-10s"
                            return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            viewModel.seekForward()
                            if (!showControls) quickSeekIndicator = "+10s"
                            return@onPreviewKeyEvent true
                        }
                    }

                    // 3. Direct shortcuts when controls are HIDDEN
                    if (!showControls) {
                        if (bingeState.isInsideIntro && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                            viewModel.skipIntro()
                            return@onPreviewKeyEvent true
                        }
                        if (bingeState.isInsideCredits && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                            viewModel.playNextEpisode()
                            return@onPreviewKeyEvent true
                        }

                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                viewModel.seekBack()
                                quickSeekIndicator = "-10s"
                                return@onPreviewKeyEvent true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                viewModel.seekForward()
                                quickSeekIndicator = "+10s"
                                return@onPreviewKeyEvent true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                quickSeekIndicator = null
                                showControls = true
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
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

        // Custom TV OSD Overlay (Netflix Style)
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
                    Text(
                        text = viewModel.mediaTitle,
                        color = TextMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom Gradient Vignette with Timeline, Playback Controls, Audio & Subtitles
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xF20B0F19), Color(0xFF0B0F19))
                            )
                        )
                        .padding(horizontal = 48.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Timeline Time & Remote Shortcuts Hint
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatDuration(playerUiState.currentPositionMs)} / ${formatDuration(playerUiState.durationMs)}",
                                color = TextMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "◄◄ 10s   |   OK: Seleziona   |   10s ►►",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        // 2. Custom Timeline Bar
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = bufferedFraction)
                                    .height(6.dp)
                                    .background(Color(0x66FFFFFF))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(6.dp)
                                    .background(PrimaryIndigo)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Quick Playback Action Buttons Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvButton(
                                text = "-10s",
                                icon = Icons.Default.Replay10,
                                onClick = {
                                    userInteractionTrigger++
                                    viewModel.seekBack()
                                }
                            )

                            TvButton(
                                text = if (playerUiState.isPlaying) "Pausa" else "Riproduci",
                                icon = if (playerUiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                isPrimary = true,
                                modifier = Modifier.focusRequester(playPauseFocusRequester),
                                onClick = {
                                    userInteractionTrigger++
                                    viewModel.togglePlayPause()
                                }
                            )

                            TvButton(
                                text = "+10s",
                                icon = Icons.Default.Forward10,
                                onClick = {
                                    userInteractionTrigger++
                                    viewModel.seekForward()
                                }
                            )

                            if (bingeState.isInsideIntro) {
                                TvButton(
                                    text = "Salta Intro",
                                    icon = Icons.Default.FastForward,
                                    onClick = {
                                        userInteractionTrigger++
                                        viewModel.skipIntro()
                                    }
                                )
                            }

                            if (bingeState.isInsideCredits) {
                                TvButton(
                                    text = "Prossimo Episodio",
                                    icon = Icons.Default.SkipNext,
                                    onClick = {
                                        userInteractionTrigger++
                                        viewModel.playNextEpisode()
                                    }
                                )
                            }
                        }

                        // 4. Audio Tracks Row (Netflix Style)
                        if (playerUiState.audioTracks.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AUDIO",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(90.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(playerUiState.audioTracks) { track ->
                                        TrackChip(
                                            label = track.label,
                                            isSelected = track.isSelected,
                                            icon = Icons.Default.Audiotrack,
                                            onClick = {
                                                userInteractionTrigger++
                                                viewModel.selectAudio(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Subtitle Tracks Row (Netflix Style)
                        if (playerUiState.subtitleTracks.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SOTTOTITOLI",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(90.dp)
                                )
                                val noneSelected = playerUiState.subtitleTracks.none { it.isSelected }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        TrackChip(
                                            label = "Disattivati",
                                            isSelected = noneSelected,
                                            icon = Icons.Default.SubtitlesOff,
                                            onClick = {
                                                userInteractionTrigger++
                                                viewModel.selectSubtitle(null)
                                            }
                                        )
                                    }
                                    items(playerUiState.subtitleTracks) { track ->
                                        TrackChip(
                                            label = track.label,
                                            isSelected = track.isSelected,
                                            icon = Icons.Default.Subtitles,
                                            onClick = {
                                                userInteractionTrigger++
                                                viewModel.selectSubtitle(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Seek Indicator Floating Badge (-10s / +10s)
        AnimatedVisibility(
            visible = quickSeekIndicator != null && !showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xD90B0F19))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (quickSeekIndicator == "-10s") Icons.Default.Replay10 else Icons.Default.Forward10,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${quickSeekIndicator}   ${formatDuration(playerUiState.currentPositionMs)}",
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Skip Intro Floating Button (visible when controls are hidden)
        AnimatedVisibility(
            visible = bingeState.isInsideIntro && !showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp, end = 48.dp)
        ) {
            TvButton(
                text = "Salta Intro",
                icon = Icons.Default.FastForward,
                isPrimary = true,
                onClick = { viewModel.skipIntro() }
            )
        }

        // Next Episode Transition Banner (visible when controls are hidden)
        AnimatedVisibility(
            visible = bingeState.isInsideCredits && !showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp, end = 48.dp)
        ) {
            val next = bingeState.nextEpisode
            if (next != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xF0182030))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        val countText = bingeState.nextCountdownSeconds?.let { " (${it}s)" } ?: ""
                        Text(
                            text = "PROSSIMO EPISODIO$countText",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "S${next.seasonNumber}:E${next.episodeNumber} • ${next.episode.title.ifEmpty { "Episodio ${next.episodeNumber}" }}",
                            color = TextMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TvButton(
                            text = "Riproduci",
                            icon = Icons.Default.PlayArrow,
                            isPrimary = true,
                            onClick = { viewModel.playNextEpisode() }
                        )
                        TvButton(
                            text = "Annulla",
                            onClick = { viewModel.dismissNextEpisodePrompt() }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        rootPlayerFocusRequester.requestFocus()
    }
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TrackChip(
    label: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.35f) else Color(0x22FFFFFF))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            color = if (isSelected) PrimaryIndigo else TextMain,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
