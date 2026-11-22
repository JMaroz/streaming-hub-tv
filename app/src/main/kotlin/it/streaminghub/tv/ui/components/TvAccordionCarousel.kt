package it.streaminghub.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.ui.theme.AccentGold
import it.streaminghub.tv.ui.theme.BgCard
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.FocusBorderColor
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextDim
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

/**
 * Accordion Hero Carousel for Android TV.
 * Focused card expands horizontally to widescreen presentation showing full backdrop and synopsis,
 * while adjacent cards remain in compact poster aspect.
 * Pressing OK directly navigates to the title's details.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAccordionCarousel(
    items: List<MediaItemDto>,
    modifier: Modifier = Modifier,
    serverUrl: String? = null,
    onItemFocused: (MediaItemDto) -> Unit,
    onItemClick: (MediaItemDto) -> Unit
) {
    if (items.isEmpty()) return

    val listState = rememberLazyListState()
    var activeIndex by remember { mutableIntStateOf(0) }

    // Keep the active expanded card in a comfortable viewport view
    LaunchedEffect(activeIndex) {
        val targetScrollIndex = (activeIndex - 1).coerceAtLeast(0)
        listState.animateScrollToItem(targetScrollIndex)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                AccordionCardItem(
                    item = item,
                    index = index,
                    isExpanded = index == activeIndex,
                    serverUrl = serverUrl,
                    onFocus = {
                        activeIndex = index
                        onItemFocused(item)
                    },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccordionCardItem(
    item: MediaItemDto,
    index: Int,
    isExpanded: Boolean,
    serverUrl: String?,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }

    // Smooth horizontal expansion from 160dp to 580dp
    val cardWidth by animateDpAsState(
        targetValue = if (isExpanded) 580.dp else 160.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "accordion_width"
    )

    val posterUrl = item.getFormattedPosterUrl(serverUrl)
    val backdropUrl = item.getFormattedBackdropUrl(serverUrl)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(260.dp)
            .zIndex(if (isExpanded) 2f else 1f)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocus()
                }
            }
            .border(
                border = BorderStroke(
                    width = if (isExpanded) 2.5.dp else 1.dp,
                    color = if (isExpanded) FocusBorderColor else Color(0x22FFFFFF)
                ),
                shape = cardShape
            )
            .clip(cardShape)
            .background(BgCard)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
    ) {
        if (isExpanded) {
            // Expanded Widescreen Presentation
            val backdropRequest = ImageRequest.Builder(context)
                .data(backdropUrl ?: posterUrl)
                .crossfade(true)
                .allowHardware(true)
                .build()

            AsyncImage(
                model = backdropRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )

            // Horizontal & Vertical Readability Vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                BgPrimary.copy(alpha = 0.95f),
                                BgPrimary.copy(alpha = 0.75f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 900f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BgPrimary.copy(alpha = 0.50f),
                                BgPrimary.copy(alpha = 0.95f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Content Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Meta Badges
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

                    if (item.year != null) {
                        Text(
                            text = item.year.toString(),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (item.rating != null && item.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", item.rating),
                                color = TextMain,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Middle: Title and Synopsis Excerpt
                Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Text(
                        text = item.title,
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            color = TextDim,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom: Action Pill Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Premi OK per la Scheda",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            // Compact Portrait Poster Presentation
            val posterRequest = ImageRequest.Builder(context)
                .data(posterUrl)
                .crossfade(true)
                .allowHardware(true)
                .build()

            AsyncImage(
                model = posterRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle bottom vignette with compact title
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC0B0F19),
                                Color(0xF20B0F19)
                            ),
                            startY = 140f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = item.title,
                    color = TextMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.year != null) {
                    Text(
                        text = item.year.toString(),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
