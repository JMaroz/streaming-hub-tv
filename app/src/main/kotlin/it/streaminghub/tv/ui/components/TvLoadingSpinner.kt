package it.streaminghub.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextDim

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLoadingSpinner(
    modifier: Modifier = Modifier,
    text: String? = "Caricamento in corso…"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tv_spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tv_spinner_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx()
                drawArc(
                    color = PrimaryIndigo.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = PrimaryIndigo,
                    startAngle = 0f,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        if (!text.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                color = TextDim,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
