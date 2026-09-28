package it.streaminghub.tv.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.data.model.ProfileDto
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.AccentGold
import it.streaminghub.tv.ui.theme.BgCard
import it.streaminghub.tv.ui.theme.BgModal
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextDim
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

// Profile Avatar Color Palette matching ha-streaming-hub CSS
private val AVATAR_COLORS = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF38BDF8), // Sky Blue
    Color(0xFFF43F5E), // Rose
    Color(0xFF8B5CF6)  // Violet
)

fun getAvatarColor(avatarStr: String): Color {
    val idx = (avatarStr.filter { it.isDigit() }.toIntOrNull() ?: 1) - 1
    return AVATAR_COLORS.getOrElse(idx % AVATAR_COLORS.size) { PrimaryIndigo }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    viewModel: ProfileViewModel,
    onProfileSelected: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pinProfile by viewModel.selectedProfileForPin.collectAsState()
    val enteredPin by viewModel.enteredPin.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Chi sta guardando?",
                color = TextMain,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seleziona il tuo profilo per accedere ai tuoi preferiti e alla tua cronologia",
                color = TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isLoading) {
                Text(
                    text = "Caricamento profili…",
                    color = TextDim,
                    fontSize = 16.sp
                )
            } else {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            onClick = {
                                viewModel.onProfileClicked(profile, onProfileSelected)
                            }
                        )
                    }
                }
            }
        }

        // PIN Dialog modal
        if (pinProfile != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgModal)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Profilo Protetto da PIN",
                        color = TextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Inserisci il PIN per ${pinProfile?.name}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Dots for PIN digits
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        for (i in 0 until 4) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < enteredPin.length) PrimaryIndigo else Color(0x33FFFFFF)
                                    )
                            )
                        }
                    }

                    // Numeric Pad (0-9)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("CLEAR", "0", "DEL")
                        )

                        for (row in rows) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (key in row) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BgCard)
                                            .tvFocusable(
                                                shape = RoundedCornerShape(8.dp),
                                                onClick = {
                                                    when (key) {
                                                        "DEL" -> viewModel.onPinBackspace()
                                                        "CLEAR" -> viewModel.dismissPinDialog()
                                                        else -> viewModel.onPinDigit(key, onProfileSelected)
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "DEL") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Cancella",
                                                tint = TextMain,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                color = TextMain,
                                                fontSize = if (key == "CLEAR") 11.sp else 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TvButton(
                        text = "Annulla",
                        onClick = { viewModel.dismissPinDialog() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: ProfileDto,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val avatarBgColor = getAvatarColor(profile.avatar)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(130.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .tvFocusable(shape = shape, onClick = onClick)
                .clip(shape)
                .background(avatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            // Display first letter of name as avatar text (matching HA app)
            Text(
                text = profile.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Age Rating Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color(0xDD000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = profile.ratingFilter,
                    color = AccentGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (profile.hasPin) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = profile.name,
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
