package it.streaminghub.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.R
import it.streaminghub.tv.ui.home.HomeSection
import it.streaminghub.tv.ui.theme.BrandCyan
import it.streaminghub.tv.ui.theme.BrandGradient
import it.streaminghub.tv.ui.theme.FocusBorderColor
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

/**
 * Fluid Glassmorphism Top Navigation Bar for Android TV.
 * Styled after Apple tvOS and Free My TV headers with translucent capsule tabs and high-contrast D-pad focus.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FluidTvTopBar(
    profileName: String,
    selectedSection: HomeSection,
    modifier: Modifier = Modifier,
    activeTabFocusRequester: FocusRequester? = null,
    onSelectSection: (HomeSection) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Logo & Typography
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_logo),
                contentDescription = "Streaming Hub",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BrandCyan.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Streaming",
                    color = TextMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Hub",
                    style = TextStyle(
                        brush = BrandGradient,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Center: Glassmorphism Capsule Tabs Container
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x331E293B))
                .border(BorderStroke(1.dp, Color(0x2BFFFFFF)), RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeSection.entries.forEach { section ->
                    GlassTabPill(
                        title = section.title,
                        isSelected = section == selectedSection,
                        focusRequester = if (section == selectedSection) activeTabFocusRequester else null,
                        onClick = { onSelectSection(section) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right Action Items: Glass Circles & Profile Pill
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassRoundIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Cerca",
                onClick = onSearchClick
            )

            GlassProfilePill(
                name = profileName,
                onClick = onProfileClick
            )

            GlassRoundIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Impostazioni",
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun GlassTabPill(
    title: String,
    isSelected: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isSelected -> PrimaryIndigo
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "tab_bg"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFF0F172A)
            isSelected -> Color.White
            else -> TextMuted
        },
        animationSpec = tween(durationMillis = 200),
        label = "tab_text"
    )

    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                border = BorderStroke(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) FocusBorderColor else Color.Transparent
                ),
                shape = shape
            )
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GlassRoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = CircleShape
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0x33FFFFFF),
        animationSpec = tween(durationMillis = 180),
        label = "icon_btn_bg"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF0F172A) else TextMain,
        animationSpec = tween(durationMillis = 180),
        label = "icon_color"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                border = BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) FocusBorderColor else Color(0x22FFFFFF)
                ),
                shape = shape
            )
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GlassProfilePill(
    name: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0x33FFFFFF),
        animationSpec = tween(durationMillis = 180),
        label = "profile_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF0F172A) else TextMain,
        animationSpec = tween(durationMillis = 180),
        label = "profile_text"
    )

    Row(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                border = BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) FocusBorderColor else Color(0x22FFFFFF)
                ),
                shape = shape
            )
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = if (isFocused) PrimaryIndigo else PrimaryIndigo,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
