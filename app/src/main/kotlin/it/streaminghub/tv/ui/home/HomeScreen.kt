package it.streaminghub.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.ui.components.FluidTvTopBar
import it.streaminghub.tv.ui.components.TvAccordionCarousel
import it.streaminghub.tv.ui.components.TvAmbientBackdrop
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.TvLoadingSpinner
import it.streaminghub.tv.ui.components.TvShelfRow
import it.streaminghub.tv.ui.theme.AccentRed
import it.streaminghub.tv.ui.theme.BgPrimary

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

    LaunchedEffect(uiState.isLoading, uiState.carouselItems) {
        if (!uiState.isLoading && uiState.carouselItems.isNotEmpty() && !hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            kotlinx.coroutines.delay(150)
            try {
                topBarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 1. Full-screen Cinematic Ambient Wallpaper (debounced follow on carousel focus)
        TvAmbientBackdrop(
            backdropUrl = uiState.ambientBackdropUrl,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Main Content Layout
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fluid Glassmorphism Navigation Bar
            FluidTvTopBar(
                profileName = uiState.activeProfileName,
                selectedSection = uiState.selectedSection,
                activeTabFocusRequester = topBarFocusRequester,
                onSelectSection = { viewModel.selectSection(it) },
                onSearchClick = onNavigateToSearch,
                onProfileClick = onNavigateToProfiles,
                onSettingsClick = onNavigateToSettings
            )

            if (uiState.isLoading && uiState.carouselItems.isEmpty() && uiState.heroItem == null) {
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
            } else if (uiState.errorMessage != null && uiState.carouselItems.isEmpty() && uiState.heroItem == null) {
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
                    // 1. Accordion Hero Carousel (Dynamic per section)
                    if (uiState.carouselItems.isNotEmpty()) {
                        item(key = "accordion_carousel_${uiState.selectedSection.id}") {
                            TvAccordionCarousel(
                                items = uiState.carouselItems,
                                serverUrl = uiState.serverUrl,
                                onItemFocused = { item -> viewModel.onCarouselItemFocused(item) },
                                onItemClick = { item -> onNavigateToDetails(item.type, item.id) }
                            )
                        }
                    }

                    // 2. Continue Watching Shelf (Filtered for current section)
                    if (uiState.continueWatching.isNotEmpty()) {
                        item(key = "shelf_continue_watching") {
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

                    // 3. Favorites Shelf (Filtered for current section)
                    if (uiState.favorites.isNotEmpty()) {
                        item(key = "shelf_favorites") {
                            TvShelfRow(
                                title = "I Tuoi Preferiti",
                                items = uiState.favorites,
                                serverUrl = uiState.serverUrl,
                                onItemFocused = { viewModel.onCardFocused(it) },
                                onItemClick = { onNavigateToDetails(it.type, it.id) }
                            )
                        }
                    }

                    // 4. Section Specific Category Shelves (Film, Serie TV, Anime, etc.)
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

                    // 5. Dynamic Editorial Carousels (for Home/Tutti or Anime when in carousel mode)
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
                            item(key = "shelf_fallback_latest") {
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
                            item(key = "shelf_fallback_movies") {
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
                            item(key = "shelf_fallback_series") {
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
                            item(key = "shelf_fallback_animation") {
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
                            item(key = "shelf_fallback_action") {
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
                        Spacer(modifier = Modifier.height(56.dp))
                    }
                }
            }
        }
    }
}
