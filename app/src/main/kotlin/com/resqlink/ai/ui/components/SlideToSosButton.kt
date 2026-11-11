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

/**
 * =========================================================================================
 * SLIDE TO SOS BUTTON
 * =========================================================================================
 * Educational Overview:
 * In safety-critical UI design, accidental emergency calls (pocket dialing) are a serious risk.
 * A simple click button can easily misfire inside a purse, pocket, or during panic shaking.
 *
 * This component implements a high-reliability physical gesture slider:
 * 1. DRAG DISTANCE THRESHOLD:
 *    The circular slider thumb must be dragged at least 70% of the total track width before
 *    the action is acknowledged.
 *
 * 2. SPRING RETURN PHYSICS:
 *    If released anywhere before the 70% threshold, the thumb springs back to zero using a
 *    bouncy physics spring, canceling the accidental drag attempt.
 *
 * 3. DYNAMIC VISUAL FEEDBACK:
 *    As the user drags, the track dynamically fills with an emergency red gradient and the
 *    instruction text fades out smoothly.
 * =========================================================================================
 */
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
    val screenDensity = LocalDensity.current

    val thumbDiameterDp = 54.dp
    val trackHeightDp = 62.dp
    val trackPaddingDp = 4.dp

    var totalTrackWidthPx by remember { mutableStateOf(0) }
    val thumbDiameterPx = with(screenDensity) { thumbDiameterDp.toPx() }
    val trackPaddingPx = with(screenDensity) { trackPaddingDp.toPx() }

    val maximumDragDistancePx = remember(totalTrackWidthPx, thumbDiameterPx, trackPaddingPx) {
        maxOf(0f, totalTrackWidthPx - thumbDiameterPx - (trackPaddingPx * 2))
    }

    val currentDragOffsetPx = remember { Animatable(0f) }
    var isSlideSuccessfullyCompleted by remember { mutableStateOf(false) }

    // Shimmering animation on chevrons to invite user interaction
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val chevronShimmerAlpha by shimmerTransition.animateFloat(
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
            .height(trackHeightDp)
            .clip(RoundedCornerShape(32.dp))
            .background(trackColor)
            .border(1.dp, Color(0xFF263964), RoundedCornerShape(32.dp))
            .onSizeChanged { totalTrackWidthPx = it.width }
            .padding(trackPaddingDp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Active filled red gradient track as thumb is being dragged
        if (currentDragOffsetPx.value > 2f && maximumDragDistancePx > 0) {
            val filledTrackWidthDp = with(screenDensity) { (currentDragOffsetPx.value + thumbDiameterPx).toDp() }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(filledTrackWidthDp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(activeGradient)
            )
        }

        // Instructional Text in center, fades out as thumb moves right
        val textAlpha = if (maximumDragDistancePx > 0) {
            (1f - (currentDragOffsetPx.value / (maximumDragDistancePx * 0.45f))).coerceIn(0f, 1f)
        } else 1f

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = thumbDiameterDp + 6.dp, end = 16.dp)
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
                tint = Color.White.copy(alpha = chevronShimmerAlpha),
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = (chevronShimmerAlpha * 0.7f)),
                modifier = Modifier.size(16.dp)
            )
        }

        // Draggable Circular Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(currentDragOffsetPx.value.roundToInt(), 0) }
                .size(thumbDiameterDp)
                .clip(CircleShape)
                .background(thumbColor)
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .pointerInput(maximumDragDistancePx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (maximumDragDistancePx > 0 && currentDragOffsetPx.value >= maximumDragDistancePx * 0.70f) {
                                coroutineScope.launch {
                                    isSlideSuccessfullyCompleted = true
                                    // Complete glide to the right border
                                    currentDragOffsetPx.animateTo(
                                        maximumDragDistancePx,
                                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                                    )
                                    delay(120)
                                    onSlideComplete()
                                    // Reset thumb position after completion
                                    delay(400)
                                    currentDragOffsetPx.snapTo(0f)
                                    isSlideSuccessfullyCompleted = false
                                }
                            } else {
                                coroutineScope.launch {
                                    // Spring back to start position
                                    currentDragOffsetPx.animateTo(
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
                                currentDragOffsetPx.animateTo(0f)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isSlideSuccessfullyCompleted) {
                                val nextDragPosition = (currentDragOffsetPx.value + dragAmount).coerceIn(0f, maximumDragDistancePx)
                                coroutineScope.launch {
                                    currentDragOffsetPx.snapTo(nextDragPosition)
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
