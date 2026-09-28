package it.streaminghub.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.streaminghub.tv.ui.theme.FocusBorderColor
import it.streaminghub.tv.ui.theme.PrimaryGlow

@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.08f,
    focusedBorderWidth: Dp = 2.5.dp,
    focusedBorderColor: Color = FocusBorderColor,
    unfocusedBorderColor: Color = Color.Transparent,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "tv_focus_scale"
    )

    val shadowElevation by animateFloatAsState(
        targetValue = if (isFocused) 16f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "tv_focus_shadow"
    )

    val interactionSource = remember { MutableInteractionSource() }

    return this
        .scale(scale)
        .shadow(
            elevation = shadowElevation.dp,
            shape = shape,
            ambientColor = PrimaryGlow,
            spotColor = PrimaryGlow
        )
        .border(
            width = if (isFocused) focusedBorderWidth else 1.dp,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = shape
        )
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged?.invoke(focusState.isFocused)
        }
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}
