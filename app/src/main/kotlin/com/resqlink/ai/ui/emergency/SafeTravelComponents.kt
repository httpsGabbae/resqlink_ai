package com.resqlink.ai.ui.emergency

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resqlink.ai.safetyjourney.JourneyStatus
import com.resqlink.ai.safetyjourney.SafetyJourney
import kotlinx.coroutines.delay

val CyanTravel = Color(0xFF38BDF8)
val CyanTravelDark = Color(0xFF0284C7)

/**
 * # SafeTravelCard
 *
 * Prominent Safe Travel (Safety Journey) Card on the HomeScreen.
 * Provides instant visibility and control over personal safety check-in sessions.
 *
 * Ordinary Human Overview:
 * When you walk home late at night or take a cab alone:
 * - In **Idle State**: Allows you to pick a preset (e.g. "Heading Home", "Grab / Taxi") and set
 *   a travel timer (15, 30, 45, 60 mins).
 * - In **Active State**: Displays a real-time countdown timer, "+15 MINS" extension button, and
 *   a large "I'M SAFE" check-in button.
 * - In **Overdue State (Grace Period)**: Pulses in amber warning colors to warn you that an emergency
 *   SMS with your live GPS location will be dispatched to your family if you don't check in.
 */
@Composable
fun SafeTravelCard(
    journey: SafetyJourney?,
    onStartJourney: (destination: String, minutes: Int, notes: String) -> Unit,
    onCheckInSafe: () -> Unit,
    onExtend15m: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isJourneyRunning = journey != null && (journey.status == JourneyStatus.ACTIVE || journey.status == JourneyStatus.GRACE_PERIOD)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = NavyCard,
        border = BorderStroke(
            1.5.dp,
            when {
                journey?.status == JourneyStatus.GRACE_PERIOD -> AmberWarning
                isJourneyRunning -> EmeraldSafe
                else -> NavyCardBorder
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (journey != null && isJourneyRunning) {
                ActiveJourneyContent(
                    journey = journey,
                    onCheckInSafe = onCheckInSafe,
                    onExtend15m = onExtend15m,
                    onCancel = onCancel
                )
            } else {
                IdleJourneyContent(
                    onStartJourney = onStartJourney
                )
            }
        }
    }
}

@Composable
private fun IdleJourneyContent(
    onStartJourney: (destination: String, minutes: Int, notes: String) -> Unit
) {
    var selectedDestinationPreset by remember { mutableStateOf("Heading Home") }
    var selectedDurationMinutes by remember { mutableStateOf(30) }
    val travelPresetsList = listOf("Heading Home", "Commute / Transit", "Walking Alone", "Grab / Taxi")
    val durationMinuteOptions = listOf(15, 30, 45, 60)

    // Header row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(CyanTravel.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = CyanTravel,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "SAFE TRAVEL",
                    color = TextPearl,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Personal Safety Check-in Timer",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CyanTravel.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, CyanTravel.copy(alpha = 0.3f))
        ) {
            Text(
                text = "OFFLINE",
                color = CyanTravel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }

    Text(
        text = "Traveling or commuting? Set a safety deadline. If you don't check in safe, ResQLink automatically alerts your trusted contacts via SMS with your last known GPS.",
        color = TextMuted,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    // Preset destination selector
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "DESTINATION / PURPOSE",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            travelPresetsList.take(3).forEach { presetItem ->
                val isPresetSelected = (selectedDestinationPreset == presetItem)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPresetSelected) CyanTravel.copy(alpha = 0.2f) else NavySurface,
                    border = BorderStroke(1.dp, if (isPresetSelected) CyanTravel else NavyCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedDestinationPreset = presetItem }
                ) {
                    Text(
                        text = presetItem,
                        color = if (isPresetSelected) TextPearl else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }

    // Duration selector
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "CHECK-IN DURATION",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            durationMinuteOptions.forEach { minutesOption ->
                val isDurationSelected = (selectedDurationMinutes == minutesOption)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDurationSelected) EmeraldSafe.copy(alpha = 0.2f) else NavySurface,
                    border = BorderStroke(1.dp, if (isDurationSelected) EmeraldSafe else NavyCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedDurationMinutes = minutesOption }
                ) {
                    Text(
                        text = if (minutesOption >= 60) "${minutesOption / 60} hr" else "$minutesOption m",
                        color = if (isDurationSelected) EmeraldSafe else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isDurationSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Start Button
    Button(
        onClick = { onStartJourney(selectedDestinationPreset, selectedDurationMinutes, "") },
        colors = ButtonDefaults.buttonColors(containerColor = CyanTravelDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "START SAFE TRAVEL ($selectedDurationMinutes MINS)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ActiveJourneyContent(
    journey: SafetyJourney,
    onCheckInSafe: () -> Unit,
    onExtend15m: () -> Unit,
    onCancel: () -> Unit
) {
    var currentTimestampMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(journey.id) {
        while (true) {
            delay(1000L)
            currentTimestampMillis = System.currentTimeMillis()
        }
    }

    val isGracePeriodActive = journey.status == JourneyStatus.GRACE_PERIOD || (currentTimestampMillis > journey.deadlineMillis && currentTimestampMillis <= journey.graceDeadlineMillis)
    val remainingCountdownMillis = if (isGracePeriodActive) {
        (journey.graceDeadlineMillis - currentTimestampMillis).coerceAtLeast(0L)
    } else {
        (journey.deadlineMillis - currentTimestampMillis).coerceAtLeast(0L)
    }

    val remainingMinutes = (remainingCountdownMillis / 1000L) / 60
    val remainingSeconds = (remainingCountdownMillis / 1000L) % 60
    val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", remainingMinutes, remainingSeconds)

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsingAlphaAnimation by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Header row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGracePeriodActive) AmberWarning.copy(alpha = pulsingAlphaAnimation)
                        else EmeraldSafe.copy(alpha = pulsingAlphaAnimation)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGracePeriodActive) "CHECK-IN OVERDUE!" else "SAFE TRAVEL ACTIVE",
                color = if (isGracePeriodActive) AmberWarning else EmeraldSafe,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGracePeriodActive) AmberWarning.copy(alpha = 0.15f) else EmeraldSafe.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (isGracePeriodActive) AmberWarning else EmeraldSafe)
        ) {
            Text(
                text = if (isGracePeriodActive) "GRACE: $formattedTime" else formattedTime,
                color = if (isGracePeriodActive) AmberWarning else EmeraldSafe,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    // Destination & Details
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = CyanTravel,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = journey.destination,
                color = TextPearl,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (journey.extensionCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(Extended +${journey.extensionCount * 15}m)",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (isGracePeriodActive) {
            Text(
                text = "⚠️ Please confirm you are safe. If no response in $formattedTime, emergency SMS with your live GPS coordinates will be sent to your trusted contacts.",
                color = AmberWarning,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        } else {
            Text(
                text = "Alarm set to sound when timer expires. Tap 'I'm Safe' when you arrive.",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }

    // Big I'M SAFE primary button
    Button(
        onClick = onCheckInSafe,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isGracePeriodActive) AmberWarning else EmeraldSafe
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "I'M SAFE (CHECK IN)",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }

    // Secondary buttons row: Extend + Cancel
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onExtend15m,
            border = BorderStroke(1.dp, NavyCardBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Update,
                contentDescription = null,
                tint = TextPearl,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "+15 MINS",
                color = TextPearl,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onCancel,
            border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = CrimsonGlow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CANCEL",
                color = CrimsonGlow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
