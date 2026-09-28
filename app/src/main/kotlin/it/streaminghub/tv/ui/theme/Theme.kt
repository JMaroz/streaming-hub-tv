package it.streaminghub.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorPalette = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = TextMain,
    primaryContainer = PrimaryHover,
    onPrimaryContainer = TextMain,
    secondary = AccentGold,
    onSecondary = BgPrimary,
    background = BgPrimary,
    onBackground = TextMain,
    surface = BgSecondary,
    onSurface = TextMain,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextMuted,
    border = BorderColor
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvStreamingHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        content = content
    )
}
