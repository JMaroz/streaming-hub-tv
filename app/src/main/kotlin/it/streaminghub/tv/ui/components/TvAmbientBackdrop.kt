package it.streaminghub.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.BgSecondary

/**
 * Ambient full-bleed cinematic wallpaper for TV.
 * Follows the focused item in the Hero Carousel with smooth crossfade and gentle downward vignette.
 */
@Composable
fun TvAmbientBackdrop(
    backdropUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(durationMillis = 500),
            label = "ambient_backdrop_crossfade"
        ) { url ->
            if (!url.isNullOrBlank()) {
                val context = LocalContext.current
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .allowHardware(true)
                    .build()

                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(BgSecondary, BgPrimary)
                            )
                        )
                )
            }
        }

        // Horizontal vignette (enhances left/right readability for TV safe zones)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BgPrimary.copy(alpha = 0.85f),
                            BgPrimary.copy(alpha = 0.40f),
                            Color.Transparent,
                            BgPrimary.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // Vertical cinema gradient: soft at top, darkening progressively downwards
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BgPrimary.copy(alpha = 0.35f),
                            BgPrimary.copy(alpha = 0.55f),
                            BgPrimary.copy(alpha = 0.90f),
                            BgPrimary
                        ),
                        startY = 0f,
                        endY = 1200f
                    )
                )
        )
    }
}
