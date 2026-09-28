package it.streaminghub.tv.ui.details

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import it.streaminghub.tv.data.model.ProviderSourceDto
import it.streaminghub.tv.data.model.TvEpisodeDto
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.TvEpisodeCard
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.AccentGold
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

        TvLazyColumn(
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val resumeSec = uiState.resumeSeconds
                            val hasResume = resumeSec > 10

                            TvButton(
                                text = if (hasResume) "Riprendi da ${formatTime(resumeSec)}" else "Guarda",
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                onClick = {
                                    val source = uiState.selectedSource ?: item.sources.firstOrNull()
                                    if (source != null) {
                                        onPlayMovie(source.pageUrl, item.title, item.id, if (hasResume) resumeSec else 0L)
                                    }
                                }
                            )

                            if (hasResume) {
                                TvButton(
                                    text = "Dall'inizio",
                                    icon = Icons.Default.Replay,
                                    onClick = {
                                        val source = uiState.selectedSource ?: item.sources.firstOrNull()
                                        if (source != null) {
                                            onPlayMovie(source.pageUrl, item.title, item.id, 0L)
                                        }
                                    }
                                )
                            }

                            TvButton(
                                text = if (uiState.isFavorite) "Nei Preferiti" else "Aggiungi",
                                icon = if (uiState.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                onClick = { viewModel.toggleFavorite() }
                            )
                        }
                    }
                }

                // Source selector (if multiple sources available)
                if (item.sources.size > 1) {
                    item {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "Sorgente Provider Disponibile",
                                color = TextMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(item.sources) { src ->
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
                            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            TvLazyRow(
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
