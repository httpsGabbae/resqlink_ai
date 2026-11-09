package com.resqlink.ai.ui.emergency

import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resqlink.ai.R
import com.resqlink.ai.domain.model.EmergencyState
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.UserProfile
import com.resqlink.ai.ui.components.SlideToSosButton

// Premium Color Palette matching Logo & Navy
val NavyBackground = Color(0xFF081226)
val NavySurface = Color(0xFF101C36)
val NavyCard = Color(0xFF162544)
val NavyCardBorder = Color(0xFF263964)
val CrimsonPrimary = Color(0xFFE53935)
val CrimsonGlow = Color(0xFFFF334B)
val EmeraldSafe = Color(0xFF10B981)
val TextPearl = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val AmberWarning = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EmergencyViewModel,
    onOpenSettings: (com.resqlink.ai.ui.settings.SettingsSubSection) -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emergencyState by viewModel.emergencyState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val nearestHotlines by viewModel.nearestHotlines.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val activeJourney by viewModel.activeSafetyJourney.collectAsState()
    val rescueProfile by viewModel.rescueProfile.collectAsState()
    val incidentRecords by viewModel.incidentRecords.collectAsState()
    val simulationState by viewModel.simulationState.collectAsState()

    var showRescueCardDialog by remember { mutableStateOf(false) }
    var showGuidesDialog by remember { mutableStateOf(false) }
    var showReadinessDialog by remember { mutableStateOf(false) }
    var showIncidentsDialog by remember { mutableStateOf(false) }
    var readinessReport by remember { mutableStateOf<com.resqlink.ai.readiness.ReadinessReport?>(null) }

    val primaryHotline = nearestHotlines.firstOrNull()

    // Cancel emergency when hardware/gesture back button is pressed during an active emergency HUD
    BackHandler(enabled = emergencyState !is EmergencyState.Idle && emergencyState !is EmergencyState.GuardianActive) {
        viewModel.cancelSos()
    }

    // Full-screen emergency overlays
    when (val state = emergencyState) {
        is EmergencyState.ConfirmationPending -> {
            EmergencyCountdownOverlay(
                title = "VOICE DISTRESS DETECTED",
                subtitle = "Detected: \"${state.detectedPhrase}\" • Target: ${state.targetName}",
                remainingSeconds = state.remainingSeconds,
                totalSeconds = 10,
                callPriority = state.callPriority,
                targetName = state.targetName,
                targetNumber = state.targetNumber,
                hotline = state.nearestHotline,
                isVoicePrompt = true,
                onCancel = { viewModel.cancelSos() },
                onConfirmNow = { viewModel.confirmEmergency(reason = "Voice: ${state.detectedPhrase}") },
                onActivateVoiceAssist = { viewModel.startAccidentVoiceAssist("Can't Speak Requested by User") }
            )
            return
        }
        is EmergencyState.ManualSosGracePeriod -> {
            EmergencyCountdownOverlay(
                title = "EMERGENCY SOS ACTIVATED",
                subtitle = "Calling ${state.targetName} (${state.targetNumber})",
                remainingSeconds = state.remainingSeconds,
                totalSeconds = 3,
                callPriority = state.callPriority,
                targetName = state.targetName,
                targetNumber = state.targetNumber,
                hotline = state.nearestHotline,
                isVoicePrompt = false,
                onCancel = { viewModel.cancelSos() },
                onConfirmNow = { viewModel.confirmEmergency(reason = "Manual SOS Trigger") },
                onActivateVoiceAssist = { viewModel.startAccidentVoiceAssist("Can't Speak Requested by User") }
            )
            return
        }
        is EmergencyState.VoiceAssistActive -> {
            InCallVoiceAssistHud(
                targetName = state.targetName,
                targetNumber = state.targetNumber,
                liveTranscript = state.liveTranscript,
                cycleCount = state.cycleCount,
                isHandoverComplete = state.isHandoverComplete,
                detectedUserWords = state.detectedUserWords,
                lastDispatcherQuestion = state.lastDispatcherQuestion,
                lastAiAnswer = state.lastAiAnswer,
                userProfile = userProfile,
                location = state.location,
                onAskDispatcherQuestion = { question -> viewModel.answerDispatcherQuestion(question) },
                onManualTakeOver = { viewModel.performHandoverToUser(userWords = "Patient Tapped 'I Am Conscious'") },
                onEndCall = { viewModel.endEmergencyCall() }
            )
            return
        }
        is EmergencyState.TimeoutEscalation -> {
            EmergencyOutcomeDialog(
                title = "Incapacitation Protocol Dispatched",
                description = "No response was detected. An unverified emergency SMS with your GPS coordinates was sent to ${state.smsSentCount} pre-authorized contacts.\n\n911 was not auto-dialed to prevent false dispatches.",
                targetName = state.nearestHotline?.name ?: "Local Station",
                targetNumber = state.nearestHotline?.primaryNumber ?: "122",
                onDismiss = { viewModel.cancelSos() },
                onCall = { state.nearestHotline?.let { viewModel.dialHotlineDirectly(it) } }
            )
        }
        is EmergencyState.EmergencyConfirmed -> {
            EmergencyOutcomeDialog(
                title = "Emergency Call Initiated",
                description = "Placing emergency call to ${state.targetName} (${state.targetNumber}).\nAlert SMS dispatched to ${state.smsSentCount} trusted contacts.",
                targetName = state.targetName,
                targetNumber = state.targetNumber,
                onDismiss = { viewModel.cancelSos() },
                onCall = { viewModel.executeConfiguredCall() }
            )
        }
        else -> {
            // Normal Home UI
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {

            // 1. Top Brand Header ("Offline Emergency Guardian" removed)
            HeaderBrandSection(
                onOpenSettings = { onOpenSettings(com.resqlink.ai.ui.settings.SettingsSubSection.NONE) }
            )

            // Safe Test Simulation Indicator Banner
            if (simulationState.isRunning) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyanTravelDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "SAFE TEST SIMULATION ACTIVE",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${simulationState.stepDescription} (${simulationState.remainingSeconds}s)",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.cancelSafeSimulation() }) {
                            Text("STOP", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Minimal GPS Active Status
            MinimalGpsBadge(
                location = currentLocation,
                onRefresh = { viewModel.refreshLocation() }
            )

            // 2b. Always Listening Guardian Mode Quick Bar
            GuardianAlwaysListeningBar(
                isGuardianActive = viewModel.isGuardianModeEnabled(),
                onToggle = { viewModel.toggleGuardianMode() }
            )

            // 3. Two Cards: Card 1 (Location & Mini-Map) + Card 2 (Nearest Station)
            TwoCardLocationGrid(
                location = currentLocation,
                hotline = primaryHotline,
                onOpenMap = onOpenMap,
                onCallHotline = { primaryHotline?.let { viewModel.dialHotlineDirectly(it) } }
            )

            // 4. Configured Emergency Call Routing Priority Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NavySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSettings(com.resqlink.ai.ui.settings.SettingsSubSection.CALL_PRIORITY) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (userProfile.callPriority) {
                                com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> Icons.Default.FamilyRestroom
                                com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> Icons.Default.Warning
                                com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> Icons.Default.LocalHospital
                            },
                            contentDescription = null,
                            tint = CrimsonGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "EMERGENCY CALL ROUTE",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = when (userProfile.callPriority) {
                                    com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> "1st: Family (${userProfile.primaryContactName}) • ${userProfile.simPreference.title}"
                                    com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> "1st: Universal 911 Dispatch • ${userProfile.simPreference.title}"
                                    com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> "1st: Local (${primaryHotline?.primaryNumber ?: "122"}) • ${userProfile.simPreference.title}"
                                },
                                color = TextPearl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Change",
                        color = CrimsonGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 5. Hero Big Glowing SOS Call Button
            BigSosCallButton(
                onClick = { viewModel.onManualSosTapped() }
            )

            // 6. The Sliding SOS Call (Dynamic according to Call Priority)
            val sliderText = when (userProfile.callPriority) {
                com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> "SLIDE TO CALL FAMILY (${userProfile.primaryContactName})"
                com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> "SLIDE TO CALL 911"
                com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> "SLIDE TO CALL ${primaryHotline?.primaryNumber ?: "122"}"
            }
            SlideToSosButton(
                onSlideComplete = { viewModel.confirmEmergency(reason = "Slide to SOS") },
                text = sliderText,
                trackColor = NavyCard,
                thumbColor = Color.White,
                iconTint = CrimsonPrimary
            )

            // Safe Travel (Personal Safety Check-in Journey)
            SafeTravelCard(
                journey = activeJourney,
                onStartJourney = { destination, minutes, notes ->
                    viewModel.startSafetyJourney(destination, minutes, 5, notes)
                },
                onCheckInSafe = { viewModel.checkInSafetyJourneySafe() },
                onExtend15m = { viewModel.extendSafetyJourney(15) },
                onCancel = { viewModel.cancelSafetyJourney() }
            )

            // Safety Tools & Preparedness Grid (Rescue Card, Offline Guides, Readiness, Logs)
            PreparednessToolsGrid(
                onOpenRescueCard = { showRescueCardDialog = true },
                onOpenGuides = { showGuidesDialog = true },
                onOpenReadiness = { showReadinessDialog = true },
                onOpenIncidents = { showIncidentsDialog = true }
            )

            // 7. Basic Emergency Profile Card (Replaced Guardian Mode on main page)
            BasicEmergencyProfileCard(
                profile = userProfile,
                onEditProfile = { onOpenSettings(com.resqlink.ai.ui.settings.SettingsSubSection.ACCOUNT_MEDICAL) }
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // --- Dialogs for Safety & Preparedness Features ---
        if (showRescueCardDialog) {
            RescueCardDialog(
                profile = rescueProfile,
                onUpdateVisibility = { blood, allergies, meds, conditions, notes, contacts ->
                    viewModel.updateRescueCardVisibility(
                        isBloodTypeVisible = blood,
                        isAllergiesVisible = allergies,
                        isMedicationsVisible = meds,
                        isConditionsVisible = conditions,
                        isInstructionsVisible = notes,
                        isContactsVisible = contacts
                    )
                },
                onDismiss = { showRescueCardDialog = false }
            )
        }

        if (showGuidesDialog) {
            OfflineGuidesDialog(
                onDismiss = { showGuidesDialog = false }
            )
        }

        if (showReadinessDialog) {
            ReadinessCheckDialog(
                report = readinessReport,
                onRunDiagnostics = {
                    readinessReport = viewModel.runReadinessCheck()
                },
                onStartSimulation = {
                    viewModel.startSafeSimulation()
                },
                onDismiss = { showReadinessDialog = false }
            )
        }

        if (showIncidentsDialog) {
            IncidentLogsDialog(
                incidents = incidentRecords,
                onExportReport = { record ->
                    viewModel.exportIncidentReport(record, asJson = false)
                },
                onDeleteIncident = { id ->
                    viewModel.deleteIncidentLog(id)
                },
                onDismiss = { showIncidentsDialog = false }
            )
        }
    }
}

@Composable
fun HeaderBrandSection(
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.resqlink_logo),
                contentDescription = "ResQLink AI Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, NavyCardBorder, RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Brand title only (Removed "Offline Emergency Guardian" subtitle per user request)
            Text(
                text = "ResQLink AI",
                color = TextPearl,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 0.5.sp
            )
        }

        // Dedicated Settings Button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NavyCard)
                .border(1.dp, NavyCardBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextPearl,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MinimalGpsBadge(
    location: com.resqlink.ai.domain.model.LocationSnapshot?,
    onRefresh: () -> Unit
) {
    var refreshCount by remember { mutableIntStateOf(0) }
    val rotationAngle by animateFloatAsState(
        targetValue = refreshCount * 360f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "rotateRefresh"
    )

    val beaconTransition = rememberInfiniteTransition(label = "gpsBeacon")
    val beaconScale by beaconTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconScale"
    )
    val beaconAlpha by beaconTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(14.dp)
                ) {
                    // Pulsing beacon halo
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(beaconScale)
                            .clip(CircleShape)
                            .background(
                                (if (location != null) EmeraldSafe else CrimsonPrimary).copy(alpha = beaconAlpha)
                            )
                    )
                    // Solid core dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (location != null) EmeraldSafe else CrimsonPrimary)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = location?.let {
                        "GPS Active • ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)} (±${it.accuracyMeters.toInt()}m)"
                    } ?: "Offline Mode • Preloaded 5km Directory Active",
                    color = TextPearl,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh GPS",
                tint = TextMuted,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotationAngle)
                    .clickable {
                        refreshCount++
                        onRefresh()
                    }
            )
        }
    }
}

@Composable
fun GuardianAlwaysListeningBar(
    isGuardianActive: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NavySurface,
        border = BorderStroke(1.dp, if (isGuardianActive) EmeraldSafe.copy(alpha = 0.45f) else NavyCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGuardianActive) EmeraldSafe else TextMuted)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isGuardianActive) "Always Listening: ACTIVE" else "Always Listening: PAUSED",
                            color = if (isGuardianActive) TextPearl else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isGuardianActive) EmeraldSafe.copy(alpha = 0.18f) else Color(0x2294A3B8)
                        ) {
                            Text(
                                text = if (isGuardianActive) "ON" else "OFF",
                                color = if (isGuardianActive) EmeraldSafe else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isGuardianActive) "Offline Wake Keyword: \"Resqlink Tulong!\"" else "Tap to resume offline voice distress detection",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = isGuardianActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EmeraldSafe,
                    checkedTrackColor = EmeraldSafe.copy(alpha = 0.35f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = NavyBackground
                ),
                modifier = Modifier.scale(0.75f)
            )
        }
    }
}

@Composable
fun LiveGoogleMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    // Retained for backward API compatibility; visual rendering is handled natively by GoogleMapWithRadarOverlay
    Box(modifier = modifier.background(Color(0xFF091428)))
}

@Composable
fun GoogleMapWithRadarOverlay(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val radarTransition = rememberInfiniteTransition(label = "googleMapOverlay")
    val sweepAngle by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val pulseFraction by radarTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF091428)),
        contentAlignment = Alignment.Center
    ) {
        // High-Precision Native Tactical Defense Radar Canvas (Zero WebView, Instant Load, 100% Reliable Offline)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.44f

            // 1. Tactical Grid Lines
            val gridStep = 24.dp.toPx()
            var x = (center.x % gridStep)
            while (x < size.width) {
                drawLine(
                    color = Color(0x1538BDF8),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = (center.y % gridStep)
            while (y < size.height) {
                drawLine(
                    color = Color(0x1538BDF8),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            // 2. Concentric Defense Rings (1km inner, 3km mid, 5km outer perimeter)
            drawCircle(
                color = Color(0x99EF4444),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            )
            drawCircle(
                color = Color(0x5538BDF8),
                radius = maxRadius * 0.65f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0x6638BDF8),
                radius = maxRadius * 0.35f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Coordinate Crosshairs
            drawLine(
                color = Color(0x4438BDF8),
                start = Offset(center.x - maxRadius * 1.08f, center.y),
                end = Offset(center.x + maxRadius * 1.08f, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0x4438BDF8),
                start = Offset(center.x, center.y - maxRadius * 1.08f),
                end = Offset(center.x, center.y + maxRadius * 1.08f),
                strokeWidth = 1.dp.toPx()
            )

            // 4. Animated Radar Sweep Cone
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color(0x0038BDF8),
                    0.80f to Color(0x1838BDF8),
                    1.0f to Color(0x6638BDF8),
                    center = center
                ),
                startAngle = sweepAngle - 50f,
                sweepAngle = 50f,
                useCenter = true,
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                size = Size(maxRadius * 2, maxRadius * 2)
            )

            // 5. Tactical Station Blips in 5km perimeter
            // Hospital (Red blip North-East ~2.5km)
            drawCircle(
                color = Color(0xFFEF4444),
                radius = 2.5.dp.toPx(),
                center = Offset(center.x + maxRadius * 0.45f, center.y - maxRadius * 0.42f)
            )
            // Police (Blue blip South-West ~3.2km)
            drawCircle(
                color = Color(0xFF3B82F6),
                radius = 2.5.dp.toPx(),
                center = Offset(center.x - maxRadius * 0.50f, center.y + maxRadius * 0.35f)
            )
            // Fire Station (Orange blip North-West ~1.8km)
            drawCircle(
                color = Color(0xFFF97316),
                radius = 2.2.dp.toPx(),
                center = Offset(center.x - maxRadius * 0.30f, center.y - maxRadius * 0.28f)
            )
            // Barangay / Rescue (Green blip South-East ~4.1km)
            drawCircle(
                color = Color(0xFF10B981),
                radius = 2.2.dp.toPx(),
                center = Offset(center.x + maxRadius * 0.60f, center.y + maxRadius * 0.50f)
            )

            // 6. Center User Beacon (Emerald pulsing beacon)
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 1.0f - pulseFraction),
                radius = 5.dp.toPx() + (14.dp.toPx() * pulseFraction),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF10B981),
                radius = 4.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 1.8.dp.toPx(),
                center = center
            )
        }

        // Top-End Badge: RADAR ACTIVE • 5KM
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF101C36).copy(alpha = 0.94f),
            border = BorderStroke(0.5.dp, Color(0xFF263964)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "RADAR • 5KM ACTIVE",
                    color = Color(0xFF38BDF8),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Top-Start Cardinal Indicator
        Text(
            text = "N ▲",
            color = Color(0xFF38BDF8).copy(alpha = 0.8f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 4.dp)
        )

        // Bottom-Start Coordinates Telemetry Tag
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color(0xFF081226).copy(alpha = 0.88f), RoundedCornerShape(bottomStart = 8.dp, topEnd = 4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                color = Color(0xFF94A3B8),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TwoCardLocationGrid(
    location: com.resqlink.ai.domain.model.LocationSnapshot?,
    hotline: Hotline?,
    onOpenMap: () -> Unit,
    onCallHotline: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "distPulse")
    val distPulse by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "distPulse"
    )

    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Live Google Maps Preview (Tapping opens dedicated Map Screen)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier
                .weight(1f)
                .height(178.dp)
                .clickable { onOpenMap() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "LIVE LOCATION",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldSafe)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MAPS",
                            color = EmeraldSafe,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Google Map View with 5km Defense Radar Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF081226))
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (location != null) {
                        GoogleMapWithRadarOverlay(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = EmeraldSafe,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Acquiring GPS...",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = location?.address ?: location?.let { "GPS: ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}" } ?: "Acquiring GPS...",
                        color = TextPearl,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CrimsonPrimary.copy(alpha = 0.22f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.70f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMap() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = CrimsonGlow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Full 5km Map",
                                    color = TextPearl,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "OPEN ↗",
                                color = CrimsonGlow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Nearest 5km Station
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier
                .weight(1f)
                .height(178.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "NEAREST HOTLINE",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldSafe.copy(alpha = 0.15f),
                        modifier = Modifier.scale(distPulse)
                    ) {
                        Text(
                            text = hotline?.distanceKm?.let { "${it}km" } ?: "Direct",
                            color = EmeraldSafe,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = hotline?.name ?: "Universal 911 Dispatch",
                        color = TextPearl,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Text(
                        text = "Hotline: ${hotline?.primaryNumber ?: "911"}",
                        color = CrimsonGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onCallHotline,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Call ${hotline?.primaryNumber ?: "911"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BigSosCallButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sosScale"
    )

    val outerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val middleRingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "middleRingScale"
    )

    var isPressed by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sosTapScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(230.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(CrimsonPrimary.copy(alpha = outerGlowAlpha))
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(middleRingScale)
                    .clip(CircleShape)
                    .background(CrimsonGlow.copy(alpha = 0.28f))
                    .padding(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(tapScale)
                        .clip(CircleShape)
                        .shadow(elevation = 20.dp, shape = CircleShape, spotColor = CrimsonGlow)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF334B), Color(0xFFC62828), Color(0xFF8E0000))
                            )
                        )
                        .clickable {
                            isPressed = true
                            onClick()
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency SOS",
                            tint = Color.White,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SOS",
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "TAP FOR HELP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Basic Emergency Profile Card
 * Placed on the main page per user request, replacing the Guardian Mode box.
 * Gives first responders and users immediate access to Blood Type, Allergies, and Emergency Notes.
 */
@Composable
fun BasicEmergencyProfileCard(
    profile: UserProfile,
    onEditProfile: () -> Unit
) {
    val heartbeatTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartScale by heartbeatTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1.0f at 0
                1.18f at 180
                1.0f at 360
                1.14f at 540
                1.0f at 720
                1.0f at 1400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartScale"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditProfile() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .scale(heartScale)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CrimsonPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = CrimsonGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EMERGENCY MEDICAL ID",
                            color = TextPearl,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = profile.displayName,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Edit",
                        color = CrimsonGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit Profile",
                        tint = CrimsonGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Medical Info Badges Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Blood Type Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("BLOOD TYPE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = profile.bloodType,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = CrimsonGlow
                        )
                    }
                }

                // Allergies Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ALLERGIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = profile.allergies,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning,
                            maxLines = 1
                        )
                    }
                }

                // Condition / ICE Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NavySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ICE CONTACT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = profile.primaryContactName.split(" ").firstOrNull() ?: "ICE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSafe,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Notes: ${profile.medicalNotes}",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmergencyCountdownOverlay(
    title: String,
    subtitle: String,
    remainingSeconds: Int,
    totalSeconds: Int,
    callPriority: com.resqlink.ai.domain.model.CallPriority = com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE,
    targetName: String = "Local Emergency Station",
    targetNumber: String = "122",
    hotline: Hotline? = null,
    isVoicePrompt: Boolean,
    onCancel: () -> Unit,
    onConfirmNow: () -> Unit,
    onActivateVoiceAssist: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF8E0000)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Big Animated Countdown Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            ) {
                Text(
                    text = "$remainingSeconds",
                    color = Color.White,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isVoicePrompt) {
                    Text(
                        text = "Say \"CANCEL\" to abort, or \"HELP\" to dial now.",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                val buttonLabel = when (callPriority) {
                    com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> "CALL $targetName ($targetNumber) NOW"
                    com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> "CALL 911 NOW"
                    com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> "CALL $targetNumber ($targetName) NOW"
                }

                Button(
                    onClick = onConfirmNow,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                if (onActivateVoiceAssist != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onActivateVoiceAssist,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CAN'T TALK (AI VOICE ASSIST)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CANCEL EMERGENCY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                val noteText = when (callPriority) {
                    com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> "Configured priority: Dials $targetName first. If no answer, SMS with GPS sent."
                    com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> "Configured priority: Prompts national 911 dispatch. If timeout, SMS with GPS sent."
                    com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> "Configured priority: Direct dials $targetName ($targetNumber). 911 not auto-dialed on silence."
                }
                Text(
                    text = noteText,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EmergencyOutcomeDialog(
    title: String,
    description: String,
    targetName: String,
    targetNumber: String,
    onDismiss: () -> Unit,
    onCall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(text = description)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Emergency Target: $targetName ($targetNumber)",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onCall) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Again ($targetNumber)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
