package it.streaminghub.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.ui.theme.AccentGold
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextDim
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBillboard(
    item: MediaItemDto?,
    modifier: Modifier = Modifier,
    serverUrl: String? = null,
    isFavorite: Boolean = false,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val backdropUrl = item?.getFormattedBackdropUrl(serverUrl)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        // Crossfading Backdrop Image
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(400),
            label = "hero_crossfade"
        ) { url ->
            if (!url.isNullOrEmpty()) {
                AsyncImage(
                    model = url,
                    contentDescription = item?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary)
                )
            }
        }

        // Horizontal & Vertical Cinema Vignette Gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BgPrimary.copy(alpha = 0.98f),
                            BgPrimary.copy(alpha = 0.75f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 1400f
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
                            BgPrimary.copy(alpha = 0.5f),
                            BgPrimary
                        ),
                        startY = 150f
                    )
                )
        )

        // Content
        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 48.dp, bottom = 24.dp)
                    .width(580.dp)
            ) {
                // Type & Certification badges
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
                        Text(
                            text = item.year.toString(),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
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

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = item.title,
                    color = TextMain,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Plot excerpt
                Text(
                    text = item.description ?: "Nessuna trama disponibile per questo titolo.",
                    color = TextDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvButton(
                        text = "Guarda Ora",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = true,
                        onClick = onPlayClick
                    )

                    TvButton(
                        text = "Dettagli",
                        icon = Icons.Default.Info,
                        onClick = onDetailsClick
                    )

                    TvButton(
                        text = if (isFavorite) "Nei Preferiti" else "Aggiungi",
                        icon = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        onClick = onToggleFavorite
                    )
                }
            }
        }
    }
}
