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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import it.streaminghub.tv.data.model.TvEpisodeDto
import it.streaminghub.tv.ui.theme.BgCard
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvEpisodeCard(
    episode: TvEpisodeDto,
    modifier: Modifier = Modifier,
    serverUrl: String? = null,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)
    val formattedPoster = episode.getFormattedPosterUrl(serverUrl)

    Column(
        modifier = modifier
            .width(230.dp)
            .padding(end = 16.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(230.dp)
                .height(130.dp)
                .tvFocusable(
                    shape = cardShape,
                    onClick = onClick
                )
                .clip(cardShape)
                .background(BgCard)
        ) {
            if (!formattedPoster.isNullOrEmpty()) {
                AsyncImage(
                    model = formattedPoster,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Episode Number Tag
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color(0xCC0B0F19), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "EP. ${episode.episodeNumber}",
                    color = PrimaryIndigo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Play Icon Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Episode",
                    tint = Color.White,
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = episode.title.ifEmpty { "Episodio ${episode.episodeNumber}" },
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!episode.description.isNullOrEmpty()) {
            Text(
                text = episode.description,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
