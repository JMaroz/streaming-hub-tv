package it.streaminghub.tv.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.R
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.ui.components.HeroBillboard
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.TvLoadingSpinner
import it.streaminghub.tv.ui.components.TvShelfRow
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.AccentRed
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

    val topBarFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.activeProfileName) {
        hasRequestedInitialFocus = false
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isFirstResume by remember { mutableStateOf(true) }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (!isFirstResume) {
                    viewModel.refreshContinueWatchingAndUserData()
                } else {
                    isFirstResume = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.selectedSection) {
        try {
            listState.scrollToItem(0, 0)
        } catch (_: Exception) {}
    }

    LaunchedEffect(uiState.isLoading, uiState.heroItem) {
        if (!uiState.isLoading && uiState.heroItem != null && !hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            kotlinx.coroutines.delay(150)
            try {
                topBarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // Persistent Top Navigation Bar with Section Tabs
        TopTvNavigationBar(
            profileName = uiState.activeProfileName,
            selectedSection = uiState.selectedSection,
            activeTabFocusRequester = topBarFocusRequester,
            onSelectSection = { viewModel.selectSection(it) },
            onSearchClick = onNavigateToSearch,
            onProfileClick = onNavigateToProfiles,
            onSettingsClick = onNavigateToSettings
        )

        if (uiState.isLoading && uiState.heroItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TvLoadingSpinner(
                    text = "Caricamento catalogo per ${uiState.activeProfileName}…"
                )
            }
        } else if (uiState.errorMessage != null && uiState.heroItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage ?: "Errore durante il caricamento del catalogo",
                        color = AccentRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TvButton(
                        text = "Riprova",
                        onClick = { viewModel.loadHomeData() }
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // 1. Hero Billboard (Stable per section, fully visible)
                item(key = "hero_billboard") {
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

                // 3. Continue Watching Shelf (Filtered for current section)
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

                // 4. Favorites Shelf (Filtered for current section)
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

                // 5. Section Specific Category Shelves (Film, Serie TV, Anime, etc.)
                if (uiState.categoryShelves.isNotEmpty()) {
                    uiState.categoryShelves.forEach { (shelfTitle, shelfItems) ->
                        if (shelfItems.isNotEmpty()) {
                            item(key = "shelf_${uiState.selectedSection.id}_$shelfTitle") {
                                TvShelfRow(
                                    title = shelfTitle,
                                    items = shelfItems,
                                    serverUrl = uiState.serverUrl,
                                    onItemFocused = { viewModel.onCardFocused(it) },
                                    onItemClick = { onNavigateToDetails(it.type, it.id) }
                                )
                            }
                        }
                    }
                }

                // 6. Dynamic Editorial Carousels (for Home/Tutti or Anime when in carousel mode)
                if (uiState.isCarouselsMode && uiState.carousels.isNotEmpty()) {
                    uiState.carousels.forEach { carousel ->
                        if (carousel.items.isNotEmpty()) {
                            item(key = "carousel_${carousel.id}") {
                                TvShelfRow(
                                    title = carousel.title,
                                    items = carousel.items,
                                    serverUrl = uiState.serverUrl,
                                    onItemFocused = { viewModel.onCardFocused(it) },
                                    onItemClick = { onNavigateToDetails(it.type, it.id) }
                                )
                            }
                        }
                    }
                } else if (uiState.categoryShelves.isEmpty()) {
                    // Fallback Shelves if no category shelves are populated
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
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopTvNavigationBar(
    profileName: String,
    selectedSection: HomeSection,
    activeTabFocusRequester: FocusRequester? = null,
    onSelectSection: (HomeSection) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Official Brand Logo
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

        // Section Navigation Tabs (Tutti, Film, Serie TV, Anime)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 28.dp)
        ) {
            HomeSection.entries.forEach { section ->
                NavTabButton(
                    title = section.title,
                    isSelected = section == selectedSection,
                    focusRequester = if (section == selectedSection) activeTabFocusRequester else null,
                    onClick = { onSelectSection(section) }
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

@Composable
private fun NavTabButton(
    title: String,
    isSelected: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(if (isSelected) PrimaryIndigo else Color(0x26FFFFFF))
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else TextMain,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
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
