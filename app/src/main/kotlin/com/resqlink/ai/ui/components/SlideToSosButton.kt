package com.resqlink.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SlideToSosButton(
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "SLIDE FOR EMERGENCY CALL",
    trackColor: Color = Color(0xFF162544),
    activeGradient: Brush = Brush.horizontalGradient(
        listOf(Color(0xFFB71C1C), Color(0xFFE53935), Color(0xFFFF5252))
    ),
    thumbColor: Color = Color(0xFFFFFFFF),
    iconTint: Color = Color(0xFFD32F2F)
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val thumbSize = 54.dp
    val trackHeight = 62.dp
    val padding = 4.dp

    var trackWidthPx by remember { mutableStateOf(0) }
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val paddingPx = with(density) { padding.toPx() }

    val maxDragPx = remember(trackWidthPx, thumbSizePx, paddingPx) {
        maxOf(0f, trackWidthPx - thumbSizePx - (paddingPx * 2))
    }

    val dragOffset = remember { Animatable(0f) }
    var isCompleted by remember { mutableStateOf(false) }

    // Shimmer effect for chevron / text
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(32.dp))
            .background(trackColor)
            .border(1.dp, Color(0xFF263964), RoundedCornerShape(32.dp))
            .onSizeChanged { trackWidthPx = it.width }
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Active filled red background as user drags (NEVER show red when idle)
        if (dragOffset.value > 2f && maxDragPx > 0) {
            val filledWidth = with(density) { (dragOffset.value + thumbSizePx).toDp() }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(filledWidth)
                    .clip(RoundedCornerShape(32.dp))
                    .background(activeGradient)
            )
        }

        // Center Instruction Text (smoothly fades out as user drags)
        val textAlpha = if (maxDragPx > 0) {
            (1f - (dragOffset.value / (maxDragPx * 0.45f))).coerceIn(0f, 1f)
        } else 1f

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = thumbSize + 6.dp, end = 16.dp)
                .alpha(textAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.0.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = shimmerAlpha),
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = (shimmerAlpha * 0.7f)),
                modifier = Modifier.size(16.dp)
            )
        }

        // Draggable Circular Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .pointerInput(maxDragPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (maxDragPx > 0 && dragOffset.value >= maxDragPx * 0.70f) {
                                coroutineScope.launch {
                                    isCompleted = true
                                    // Animate all the way to the very end
                                    dragOffset.animateTo(
                                        maxDragPx,
                                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                                    )
                                    // Brief pause so the user visually sees the thumb reach the end
                                    delay(120)
                                    onSlideComplete()
                                    // Hold at end briefly then smoothly reset
                                    delay(400)
                                    dragOffset.snapTo(0f)
                                    isCompleted = false
                                }
                            } else {
                                coroutineScope.launch {
                                    dragOffset.animateTo(
                                        0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                dragOffset.animateTo(0f)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isCompleted) {
                                val newTarget = (dragOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                                coroutineScope.launch {
                                    dragOffset.snapTo(newTarget)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Slide Thumb",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
