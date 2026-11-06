package it.streaminghub.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.streaminghub.tv.ui.theme.FocusBorderColor

/**
 * Lightweight, GPU-friendly TV focus modifier.
 * Optimized for low-power TV chipsets:
 * - Default 1.0f scale (eliminates adjacent card shifts and spring oscillation)
 * - Zero dynamic shadows (eliminates expensive GPU blur/render passes)
 * - Crisp, high-contrast border for instant visibility
 * - Clean input dispatch without conflicting event consumers
 */
@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.0f,
    unfocusedScale: Float = 1.0f,
    focusedBorderWidth: Dp = 2.5.dp,
    focusedBorderColor: Color = FocusBorderColor,
    unfocusedBorderColor: Color = Color.Transparent,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val hasScale = focusedScale != 1.0f || unfocusedScale != 1.0f
    val scaleModifier = if (hasScale) {
        val scale by animateFloatAsState(
            targetValue = if (isFocused) focusedScale else unfocusedScale,
            animationSpec = tween(durationMillis = 100),
            label = "tv_focus_scale"
        )
        Modifier.scale(scale)
    } else {
        Modifier
    }

    return this
        .zIndex(if (isFocused) 1f else 0f)
        .then(scaleModifier)
        .border(
            width = focusedBorderWidth,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = shape
        )
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged?.invoke(focusState.isFocused)
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier.focusable(enabled = enabled, interactionSource = interactionSource)
            }
        )
}
