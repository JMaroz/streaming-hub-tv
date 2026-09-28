package it.streaminghub.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.R
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.ui.components.HeroBillboard
import it.streaminghub.tv.ui.components.TvShelfRow
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextMain

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetails: (mediaType: String, titleId: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onQuickPlay: (MediaItemDto) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        TvLazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Navigation Bar
            item {
                TopTvNavigationBar(
                    profileName = uiState.activeProfileName,
                    onSearchClick = onNavigateToSearch,
                    onProfileClick = onNavigateToProfiles,
                    onSettingsClick = onNavigateToSettings
                )
            }

            // 2. Hero Billboard
            item {
                HeroBillboard(
                    item = uiState.heroItem,
                    serverUrl = uiState.serverUrl,
                    isFavorite = uiState.isHeroFavorite,
                    onPlayClick = {
                        uiState.heroItem?.let { onQuickPlay(it) }
                    },
                    onDetailsClick = {
                        uiState.heroItem?.let {
                            onNavigateToDetails(it.type, it.id)
                        }
                    },
                    onToggleFavorite = {
                        viewModel.toggleFavoriteHero()
                    }
                )
            }

            // 3. Continue Watching Shelf
            if (uiState.continueWatching.isNotEmpty()) {
                item {
                    TvShelfRow(
                        title = "Continua a guardare",
                        items = uiState.continueWatching,
                        serverUrl = uiState.serverUrl,
                        progressMap = uiState.continueProgressMap,
                        onItemFocused = { viewModel.onCardFocused(it) },
                        onItemClick = { onNavigateToDetails(it.type, it.id) }
                    )
                }
            }

            // 4. Favorites Shelf
            if (uiState.favorites.isNotEmpty()) {
                item {
                    TvShelfRow(
                        title = "I Tuoi Preferiti",
                        items = uiState.favorites,
                        serverUrl = uiState.serverUrl,
                        onItemFocused = { viewModel.onCardFocused(it) },
                        onItemClick = { onNavigateToDetails(it.type, it.id) }
                    )
                }
            }

            // 5. Latest Arrivals
            if (uiState.latestTitles.isNotEmpty()) {
                item {
                    TvShelfRow(
                        title = "Ultimi Arrivi",
                        items = uiState.latestTitles,
                        serverUrl = uiState.serverUrl,
                        onItemFocused = { viewModel.onCardFocused(it) },
                        onItemClick = { onNavigateToDetails(it.type, it.id) }
                    )
                }
            }

            // 6. Latest Movies
            if (uiState.latestMovies.isNotEmpty()) {
                item {
                    TvShelfRow(
                        title = "Film Recenti",
                        items = uiState.latestMovies,
                        serverUrl = uiState.serverUrl,
                        onItemFocused = { viewModel.onCardFocused(it) },
                        onItemClick = { onNavigateToDetails(it.type, it.id) }
                    )
                }
            }

            // 7. Latest Series
            if (uiState.latestSeries.isNotEmpty()) {
                item {
                    TvShelfRow(
                        title = "Serie TV Popolari",
                        items = uiState.latestSeries,
                        serverUrl = uiState.serverUrl,
                        onItemFocused = { viewModel.onCardFocused(it) },
                        onItemClick = { onNavigateToDetails(it.type, it.id) }
                    )
                }
            }

            // 8. Animation Genre Shelf
            if (uiState.animationTitles.isNotEmpty()) {
                item {
                    TvShelfRow(
                        title = "Film d'Animazione & Famiglia",
                        items = uiState.animationTitles,
                        serverUrl = uiState.serverUrl,
                        onItemFocused = { viewModel.onCardFocused(it) },
                        onItemClick = { onNavigateToDetails(it.type, it.id) }
                    )
                }
            }

            // 9. Action Genre Shelf
            if (uiState.actionTitles.isNotEmpty()) {
                item {
                    TvShelfRow(
                        title = "Azione e Avventura",
                        items = uiState.actionTitles,
                        serverUrl = uiState.serverUrl,
                        onItemFocused = { viewModel.onCardFocused(it) },
                        onItemClick = { onNavigateToDetails(it.type, it.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopTvNavigationBar(
    profileName: String,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Official Brand Logo matching Home Assistant Streaming Hub
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_brand_logo),
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Row {
                Text(
                    text = "Streaming",
                    color = TextMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Hub",
                    color = PrimaryIndigo,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIconButton(
                icon = Icons.Default.Search,
                label = "Cerca",
                onClick = onSearchClick
            )

            NavProfilePill(
                name = profileName,
                onClick = onProfileClick
            )

            NavIconButton(
                icon = Icons.Default.Settings,
                label = "Impostazioni",
                onClick = onSettingsClick
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(38.dp)
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(Color(0x33FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextMain,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavProfilePill(
    name: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(Color(0x33FFFFFF))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = PrimaryIndigo,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            color = TextMain,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
