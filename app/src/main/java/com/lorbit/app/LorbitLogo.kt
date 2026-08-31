package com.lorbit.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class SplashPhase {
    CENTER_SPLASH, // Initial hold in center
    GLIDING_TO_TOP, // Smooth travel to top bar
    EXPANDED_WORD,  // Expands to full '𝓛𝓸𝓻𝓫𝓲𝓽'
    COMPLETED       // Contracted to '𝓛' and home content cascades in
}

@Composable
fun LorbitMorphingHeader(
    onSplashComplete: () -> Unit = {}
) {
    var phase by remember { mutableStateOf(SplashPhase.CENTER_SPLASH) }
    val interactionSource = remember { MutableInteractionSource() }

    // Smooth Glide Progress [0.0 = exact center, 1.0 = top bar]
    val glideProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(600) // Initial splash pause in center
        phase = SplashPhase.GLIDING_TO_TOP

        // Smooth glide up to top
        glideProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )

        phase = SplashPhase.EXPANDED_WORD
        delay(1000) // Hold '𝓛𝓸𝓻𝓫𝓲𝓽' for exactly 1 second

        phase = SplashPhase.COMPLETED
        onSplashComplete()
    }

    val isExpanded = phase == SplashPhase.EXPANDED_WORD
    val isDocked = phase == SplashPhase.COMPLETED || phase == SplashPhase.EXPANDED_WORD

    // Interpolate vertical alignment from 0f (Center) to -1f (Top)
    val verticalBias = -1f * glideProgress.value

    // Scale down from large splash 2.2x to docked 1.0x
    val currentScale = 2.2f - (1.2f * glideProgress.value)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isDocked) Modifier.fillMaxHeight(0.65f)
                else Modifier.padding(top = 6.dp, bottom = 4.dp)
            ),
        contentAlignment = androidx.compose.ui.BiasAlignment(0f, verticalBias)
    ) {
        Row(
            modifier = Modifier
                .scale(currentScale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (phase == SplashPhase.COMPLETED) {
                        phase = SplashPhase.EXPANDED_WORD
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Capital Cursive '𝓛'
            Text(
                text = "𝓛",
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.95f)
            )

            // Flowing Cursive '𝓸𝓻𝓫𝓲𝓽'
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(350)) + slideInHorizontally(tween(350)) { -it / 3 },
                exit = fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -it / 3 }
            ) {
                Text(
                    text = "𝓸𝓻𝓫𝓲𝓽",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.offset(x = (-1).dp)
                )
            }
        }
    }
}