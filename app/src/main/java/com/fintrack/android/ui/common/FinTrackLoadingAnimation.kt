package com.fintrack.android.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LogoBlue = Color(0xFF007BFF)

/**
 * Compose recreation of the FinTrack loading animation: a blue rounded-square "logo" with five
 * translucent bars pulsing (scaleY, staggered) behind a bold white "FT", where the F and T
 * themselves also gently pulse like chart bars. Used anywhere the app would otherwise show a
 * plain spinner (see [LoadingBox]).
 */
@Composable
fun FinTrackLoadingAnimation(modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val transition = rememberInfiniteTransition(label = "ft-loading")

    // Background bars: base heights 40/70/55/90/60 out of a 160px reference box, each staggered
    // by 0/0.2/0.4/0.6/0.8s, scaleY 1 -> 1.25 -> 1 over a 1.8s cycle (900ms each way, reversing).
    val barBaseHeightFractions = listOf(0.25f, 0.4375f, 0.34375f, 0.5625f, 0.375f)
    val barDelaysMs = listOf(0, 200, 400, 600, 800)
    val barScales = barDelaysMs.map { delayMs ->
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delayMs, StartOffsetType.FastForward)
            ),
            label = "bar$delayMs"
        )
        scale
    }

    // "FT" letters: scaleY 1 -> 1.35 -> 1 over 1.4s (700ms each way); T lags 0.3s behind F.
    val fScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(0)),
        label = "letterF"
    )
    val tScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(300, StartOffsetType.FastForward)),
        label = "letterT"
    )

    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.1f)).background(LogoBlue),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(bottom = size * 0.08f),
            horizontalArrangement = Arrangement.spacedBy(size * 0.06f, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
        ) {
            barBaseHeightFractions.forEachIndexed { i, heightFraction ->
                Box(
                    modifier = Modifier
                        .width(size * 0.11f)
                        .height(size * heightFraction)
                        .graphicsLayer {
                            scaleY = barScales[i]
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "F",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.44f).sp,
                modifier = Modifier.graphicsLayer {
                    scaleY = fScale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
            )
            Spacer(Modifier.width(size * 0.075f))
            Text(
                "T",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.44f).sp,
                modifier = Modifier.graphicsLayer {
                    scaleY = tScale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
            )
        }
    }
}
