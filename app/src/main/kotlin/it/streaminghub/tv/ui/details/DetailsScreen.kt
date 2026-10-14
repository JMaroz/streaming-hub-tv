package it.streaminghub.tv.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import it.streaminghub.tv.data.model.ProviderSourceDto
import it.streaminghub.tv.data.model.TvEpisodeDto
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.TvEpisodeCard
import it.streaminghub.tv.ui.components.TvLoadingSpinner
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.AccentGold
import it.streaminghub.tv.ui.theme.AccentRed
import it.streaminghub.tv.ui.theme.BgCard
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextDim
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBackClick: () -> Unit,
    onPlayMovie: (sourceUrl: String, title: String, mediaId: String, seekSeconds: Long) -> Unit,
    onPlayEpisode: (sourceUrl: String, seriesTitle: String, mediaId: String, season: Int, episode: Int, seekSeconds: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val item = uiState.item

    val playButtonFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    LaunchedEffect(item) {
        if (item != null && !hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            kotlinx.coroutines.delay(100)
            try {
                playButtonFocusRequester.requestFocus()
            } catch (_: Exception) {
                kotlinx.coroutines.delay(150)
                try {
                    playButtonFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // Background Backdrop with Cinematic Dark Vignette
        val backdropUrl = item?.getFormattedBackdropUrl(uiState.serverUrl)
        if (!backdropUrl.isNullOrEmpty()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                BgPrimary.copy(alpha = 0.98f),
                                BgPrimary.copy(alpha = 0.85f),
                                BgPrimary.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, BgPrimary),
                            startY = 200f
                        )
                    )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 28.dp)
        ) {
            // Back Button
            item {
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusable(shape = RoundedCornerShape(18.dp), onClick = onBackClick)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TextMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (item != null) {
                // Header Info
                item {
                    Column(modifier = Modifier.width(680.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (item.type == "tv") "SERIE TV" else "FILM",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(PrimaryIndigo, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            if (!item.certification.isNullOrEmpty()) {
                                Text(
                                    text = item.certification,
                                    color = TextMain,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .background(Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (item.year != null) {
                                Text(text = item.year.toString(), color = TextMuted, fontSize = 12.sp)
                            }

                            if (item.duration != null && item.duration > 0) {
                                Text(text = "• ${item.duration} min", color = TextMuted, fontSize = 12.sp)
                            }

                            if (item.rating != null && item.rating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.height(13.dp)
                                    )
                                    Text(
                                        text = " ${String.format("%.1f", item.rating)}",
                                        color = TextMain,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title
                        Text(
                            text = item.title,
                            color = TextMain,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        // Genres
                        if (item.genres.isNotEmpty()) {
                            Text(
                                text = item.genres.joinToString(" • "),
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Plot
                        Text(
                            text = item.description ?: "Nessuna descrizione disponibile.",
                            color = TextDim,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
                        )

                        // Action Buttons
                        val resumeSec = uiState.resumeSeconds
                        val hasResumeSec = resumeSec > 10
                        val resumeSeason = uiState.resumeSeasonNumber
                        val resumeEp = uiState.resumeEpisodeNumber

                        val resumeLabel = if (item.type == "tv") {
                            when {
                                resumeSeason != null && resumeEp != null && hasResumeSec ->
                                    "Riprendi S${resumeSeason}:E${resumeEp} (${formatTime(resumeSec)})"
                                resumeSeason != null && resumeEp != null ->
                                    "Riprendi S${resumeSeason}:E${resumeEp}"
                                else -> {
                                    val firstSeason = item.seasons.firstOrNull()?.number ?: 1
                                    val firstEp = item.seasons.firstOrNull()?.episodes?.firstOrNull()?.episodeNumber ?: 1
                                    "Inizia S${firstSeason}:E${firstEp}"
                                }
                            }
                        } else {
                            if (hasResumeSec) {
                                "Riprendi da ${formatTime(resumeSec)}"
                            } else {
                                "Inizia"
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TvButton(
                                text = resumeLabel,
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                modifier = Modifier.focusRequester(playButtonFocusRequester),
                                onClick = {
                                    viewModel.playTitle(
                                        fromBeginning = false,
                                        onPlayMovie = onPlayMovie,
                                        onPlayEpisode = onPlayEpisode
                                    )
                                }
                            )

                            if (hasResumeSec) {
                                TvButton(
                                    text = "Dall'inizio",
                                    icon = Icons.Default.Replay,
                                    onClick = {
                                        viewModel.playTitle(
                                            fromBeginning = true,
                                            onPlayMovie = onPlayMovie,
                                            onPlayEpisode = onPlayEpisode
                                        )
                                    }
                                )
                            }

                            TvButton(
                                text = if (uiState.isFavorite) "Nei Preferiti" else "Aggiungi",
                                icon = if (uiState.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                onClick = { viewModel.toggleFavorite() }
                            )
                        }

                        if (!uiState.playbackFeedbackMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.playbackFeedbackMessage!!,
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Streaming Availability (Zero Extra Cost)
                        val streamingProviders = item.streamingAvailability?.groupedProviders ?: emptyList()
                        if (streamingProviders.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    text = "Disponibile in streaming su:",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    streamingProviders.forEach { provider ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x33FFFFFF))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val logo = provider.logoUrl
                                            if (!logo.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = logo,
                                                    contentDescription = provider.providerName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                )
                                            }
                                            Text(
                                                text = provider.providerName,
                                                color = TextMain,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Source selector (if multiple sources available)
                if (item.type != "tv" && item.resolvedSources.size > 1) {
                    item {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "Sorgente Provider Disponibile",
                                color = TextMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(item.resolvedSources) { src ->
                                    val isSelected = uiState.selectedSource?.id == src.id
                                    SourceChip(
                                        source = src,
                                        isSelected = isSelected,
                                        onClick = { viewModel.selectSource(src) }
                                    )
                                }
                            }
                        }
                    }
                }

                // If TV Series: Seasons tabs and Episode cards
                if (item.type == "tv" && item.seasons.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 28.dp)) {
                            Text(
                                text = "Stagioni ed Episodi",
                                color = TextMain,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Seasons Tabs
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(item.seasons) { season ->
                                    val isSelected = uiState.selectedSeasonNumber == season.number
                                    SeasonTab(
                                        number = season.number,
                                        isSelected = isSelected,
                                        onClick = { viewModel.selectSeason(season.number) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Episodes Shelf
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(uiState.currentSeasonEpisodes) { ep ->
                                    TvEpisodeCard(
                                        episode = ep,
                                        serverUrl = uiState.serverUrl,
                                        onClick = {
                                            val epSource = ep.sources.firstOrNull { it.available } ?: ep.sources.firstOrNull()
                                            if (epSource != null) {
                                                onPlayEpisode(
                                                    epSource.pageUrl,
                                                    "${item.title} - S${ep.seasonNumber}E${ep.episodeNumber}",
                                                    item.id,
                                                    ep.seasonNumber,
                                                    ep.episodeNumber,
                                                    0L
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Cast & Director
                item {
                    Column(modifier = Modifier.padding(top = 28.dp, bottom = 48.dp)) {
                        if (!item.director.isNullOrEmpty()) {
                            Text(
                                text = "Regia: ${item.director}",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                        if (item.cast.isNotEmpty()) {
                            Text(
                                text = "Cast: ${item.cast.take(6).joinToString(", ")}",
                                color = TextDim,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TvLoadingSpinner(text = "Caricamento dettagli in corso…")
                    }
                }
            } else if (uiState.errorMessage != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Errore caricamento dettagli",
                            color = AccentRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TvButton(
                            text = "Riprova",
                            onClick = { viewModel.loadTitleDetails(manualForceRefresh = true) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceChip(
    source: ProviderSourceDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(if (isSelected) PrimaryIndigo else BgCard)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${source.providerName} (${source.quality ?: "HD"})",
            color = TextMain,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SeasonTab(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(if (isSelected) PrimaryIndigo else Color(0x33FFFFFF))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Stagione $number",
            color = TextMain,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
