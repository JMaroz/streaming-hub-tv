package it.streaminghub.tv.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import it.streaminghub.tv.ui.theme.AccentRed
import it.streaminghub.tv.ui.theme.BgCard
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPosterCard(
    item: MediaItemDto,
    modifier: Modifier = Modifier,
    serverUrl: String? = null,
    progressPercent: Float = 0f,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)
    val formattedPoster = item.getFormattedPosterUrl(serverUrl)

    Column(
        modifier = modifier
            .width(145.dp)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(145.dp)
                .height(215.dp)
                .tvFocusable(
                    shape = cardShape,
                    onFocusChanged = { if (it) onFocused?.invoke() },
                    onClick = onClick
                )
                .clip(cardShape)
                .background(BgCard)
        ) {
            // Poster Image with formatted URL
            if (!formattedPoster.isNullOrEmpty()) {
                AsyncImage(
                    model = formattedPoster,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.title.take(2).uppercase(),
                        color = TextMuted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Badges (Type and Rating)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge
                Text(
                    text = if (item.type == "tv") "SERIE" else "FILM",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            color = if (item.type == "tv") PrimaryIndigo else Color(0xCC1E293B),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Rating badge if available
                if (item.rating != null && item.rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xDD0B0F19), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = AccentGold,
                            modifier = Modifier.height(11.dp)
                        )
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = TextMain,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // Bottom Shadow gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xE60B0F19))
                        )
                    )
            )

            // Watch Progress Bar (Continua a guardare)
            if (progressPercent > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color(0x80FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (progressPercent / 100f).coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(AccentRed)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title and Year
        Text(
            text = item.title,
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val yearText = item.year?.toString() ?: ""
        if (yearText.isNotEmpty()) {
            Text(
                text = yearText,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
