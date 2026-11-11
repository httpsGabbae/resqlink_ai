package com.resqlink.ai.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resqlink.ai.data.telephony.DetectedSim
import com.resqlink.ai.domain.model.CallPriority
import com.resqlink.ai.domain.model.EmergencyState
import com.resqlink.ai.domain.model.SimPreference
import com.resqlink.ai.domain.model.UserProfile
import com.resqlink.ai.ui.emergency.*

/**
 * Enumeration of sub-sections inside the Settings destination.
 */
enum class SettingsSubSection {
    NONE,
    CALL_PRIORITY,
    ACCOUNT_MEDICAL,
    PHONEBOOK,
    PERMISSIONS,
    GUARDIAN_AI,
    ABOUT_POLICY
}

/**
 * =========================================================================================
 * SETTINGS SCREEN
 * =========================================================================================
 * Educational Overview:
 * This screen manages all user preferences, emergency routing policies, and hardware
 * configurations for ResQLink AI:
 *
 * 1. EMERGENCY CALL ROUTING & DUAL-SIM:
 *    Configures whether hands-free SOS auto-dials the nearest 5km station, universal 911,
 *    or family first. Also configures which physical SIM card slot handles outgoing calls.
 *
 * 2. PERSONAL & MEDICAL PROFILE:
 *    Stores blood type, allergies, and critical health notes 100% locally on the phone.
 *
 * 3. GUARDIAN VOICE AI & SENSORS:
 *    Enables background acoustic keyword spotting ("Resqlink Tulong!") and experimental
 *    g-force impact detection.
 *
 * 4. SYSTEM PERMISSIONS & POLICY:
 *    Provides transparency on required Android system privileges (Microphone, Location,
 *    Phone, SMS) and details fail-soft emergency policies.
 * =========================================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: EmergencyViewModel,
    onBack: () -> Unit,
    initialSubSection: SettingsSubSection = SettingsSubSection.NONE,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfileData by viewModel.userProfile.collectAsState()
    val trustedContactsList by viewModel.trustedContacts.collectAsState()
    val currentDeviceLocation by viewModel.currentLocation.collectAsState()
    val currentEmergencyState by viewModel.emergencyState.collectAsState()
    val isGuardianServiceActive = currentEmergencyState is EmergencyState.GuardianActive || viewModel.isGuardianModeEnabled()
    val detectedHardwareSims = remember { viewModel.getDetectedSims() }

    var activeSettingsSubSection by remember { mutableStateOf(initialSubSection) }
    var isLogoutConfirmationVisible by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back button to navigate backwards hierarchically
    BackHandler {
        if (activeSettingsSubSection != SettingsSubSection.NONE) {
            activeSettingsSubSection = SettingsSubSection.NONE
        } else {
            onBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding()
    ) {
        // Fixed Top App Bar
        Surface(
            color = NavyBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (activeSettingsSubSection != SettingsSubSection.NONE) {
                            activeSettingsSubSection = SettingsSubSection.NONE
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NavyCard)
                        .border(1.dp, NavyCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPearl,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = when (activeSettingsSubSection) {
                        SettingsSubSection.NONE -> "Settings"
                        SettingsSubSection.CALL_PRIORITY -> "Emergency Call Routing"
                        SettingsSubSection.ACCOUNT_MEDICAL -> "Edit Medical Profile"
                        SettingsSubSection.PHONEBOOK -> "Emergency Phonebook"
                        SettingsSubSection.PERMISSIONS -> "Permissions & Privacy"
                        SettingsSubSection.GUARDIAN_AI -> "Guardian Mode & Voice AI"
                        SettingsSubSection.ABOUT_POLICY -> "About & Safety Policy"
                    },
                    color = TextPearl,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Animated slide transitions between main settings menu and sub-sections
            AnimatedContent(
                targetState = activeSettingsSubSection,
                transitionSpec = {
                    if (targetState != SettingsSubSection.NONE && initialState == SettingsSubSection.NONE) {
                        (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(200)))
                    } else if (targetState == SettingsSubSection.NONE) {
                        (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
                    } else {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    }
                },
                label = "settingsSubSectionAnim"
            ) { currentSubSection ->
                when (currentSubSection) {
                    SettingsSubSection.CALL_PRIORITY -> {
                        CallPrioritySubSectionView(
                            currentPriority = userProfileData.callPriority,
                            currentSimPreference = userProfileData.simPreference,
                            primaryContactName = userProfileData.primaryContactName,
                            detectedSims = detectedHardwareSims,
                            onSelectPriority = { newPriority ->
                                viewModel.updateCallPriority(newPriority)
                            },
                            onSelectSimPreference = { newSimPreference ->
                                viewModel.updateSimPreference(newSimPreference)
                            }
                        )
                    }

                    SettingsSubSection.ACCOUNT_MEDICAL -> {
                        EditAccountMedicalView(
                            profile = userProfileData,
                            onSave = { name, blood, allergies, notes, contactName, contactPhone ->
                                viewModel.updateMedicalProfile(name, blood, allergies, notes, contactName, contactPhone)
                                activeSettingsSubSection = SettingsSubSection.NONE
                            }
                        )
                    }

                    SettingsSubSection.PHONEBOOK -> {
                        PhonebookSubSectionView(
                            contacts = trustedContactsList,
                            onAddContact = { contactName, contactPhone, contactRelationship ->
                                viewModel.addTrustedContact(contactName, contactPhone, contactRelationship)
                            },
                            onDeleteContact = { contactId ->
                                viewModel.removeTrustedContact(contactId)
                            }
                        )
                    }

                    SettingsSubSection.PERMISSIONS -> {
                        PermissionsSubSectionView(
                            context = context,
                            hasLocation = currentDeviceLocation != null
                        )
                    }

                    SettingsSubSection.GUARDIAN_AI -> {
                        GuardianAiSubSectionView(
                            isActive = isGuardianServiceActive,
                            currentApiKey = viewModel.getGeminiApiKey(),
                            onSaveApiKey = { viewModel.setGeminiApiKey(it) },
                            currentElevenLabsKey = viewModel.getElevenLabsApiKey(),
                            onSaveElevenLabsKey = { viewModel.setElevenLabsApiKey(it) },
                            currentElevenLabsVoiceId = viewModel.getElevenLabsVoiceId(),
                            onSaveElevenLabsVoiceId = { viewModel.setElevenLabsVoiceId(it) },
                            onToggleGuardian = { viewModel.toggleGuardianMode() },
                            onSimulateVoice = {
                                viewModel.onVoiceDistressDetected("Resqlink Tulong!")
                                onBack()
                            }
                        )
                    }

                    SettingsSubSection.ABOUT_POLICY -> {
                        AboutPolicySubSectionView()
                    }

                    SettingsSubSection.NONE -> {
                        Column {
                            // Main settings menu items
                            Text(
                                text = "PREFERENCES & EMERGENCY DATA",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // 0: Emergency Call Routing & Dual-SIM
                                SettingsMenuItem(
                                    icon = Icons.AutoMirrored.Filled.PhoneCallback,
                                    title = "Emergency Call Routing & Dual-SIM",
                                    subtitle = "${userProfileData.callPriority.title} • ${userProfileData.simPreference.title}",
                                    badge = "Dual-SIM",
                                    badgeColor = CrimsonGlow,
                                    onClick = { activeSettingsSubSection = SettingsSubSection.CALL_PRIORITY }
                                )

                                // 1: Account & Medical
                                SettingsMenuItem(
                                    icon = Icons.Default.MedicalServices,
                                    title = "Account & Medical Profile",
                                    subtitle = "${userProfileData.displayName} • Blood ${userProfileData.bloodType} • Allergies",
                                    badge = "Edit",
                                    onClick = { activeSettingsSubSection = SettingsSubSection.ACCOUNT_MEDICAL }
                                )

                                // 2: Phonebook
                                SettingsMenuItem(
                                    icon = Icons.Default.ContactPhone,
                                    title = "Emergency Phonebook",
                                    subtitle = "${trustedContactsList.size} authorized safety contacts for GPS alerts",
                                    badge = "${trustedContactsList.size}",
                                    onClick = { activeSettingsSubSection = SettingsSubSection.PHONEBOOK }
                                )

                                // 3: Permissions
                                SettingsMenuItem(
                                    icon = Icons.Default.Security,
                                    title = "Permissions & Privacy",
                                    subtitle = "Microphone, Location, Telephony, SMS",
                                    badge = "Active",
                                    onClick = { activeSettingsSubSection = SettingsSubSection.PERMISSIONS }
                                )

                                // 4: Guardian Voice AI
                                SettingsMenuItem(
                                    icon = Icons.Default.Hearing,
                                    title = "Guardian Voice AI",
                                    subtitle = if (isGuardianServiceActive) "Always Listening • 'Resqlink Tulong!'" else "Monitoring Paused",
                                    badge = if (isGuardianServiceActive) "ON" else "OFF",
                                    badgeColor = if (isGuardianServiceActive) EmeraldSafe else TextMuted,
                                    onClick = { activeSettingsSubSection = SettingsSubSection.GUARDIAN_AI }
                                )

                                // 4b: Experimental Motion Distress Detection
                                var isMotionDetectionEnabled by remember { mutableStateOf(viewModel.isExperimentalMotionDetectionEnabled()) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = NavySurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(AmberWarning.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Vibration,
                                                    contentDescription = null,
                                                    tint = AmberWarning,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Motion Distress Detection",
                                                        color = TextPearl,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AmberWarning.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "EXPERIMENTAL",
                                                            color = AmberWarning,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Detects severe impacts (>2.8g) followed by stillness. Triggers 10s countdown. Never auto-calls 911.",
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = isMotionDetectionEnabled,
                                            onCheckedChange = { isChecked ->
                                                isMotionDetectionEnabled = isChecked
                                                viewModel.setExperimentalMotionDetectionEnabled(isChecked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = AmberWarning,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = NavySurface
                                            )
                                        )
                                    }
                                }

                                // 5: About & Policy
                                SettingsMenuItem(
                                    icon = Icons.Default.Info,
                                    title = "About & Emergency Policy",
                                    subtitle = "ResQLink AI v1.1.0 • Fail-soft dispatch rules",
                                    onClick = { activeSettingsSubSection = SettingsSubSection.ABOUT_POLICY }
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Sign-out action item
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = NavyCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isLogoutConfirmationVisible = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Log Out",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Log Out of Account",
                                        color = CrimsonPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (isLogoutConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isLogoutConfirmationVisible = false },
            containerColor = NavyCard,
            titleContentColor = TextPearl,
            textContentColor = TextMuted,
            title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
            text = { Text("You will be returned to the sign-in screen. Your emergency contacts and medical profile remain safely stored locally.") },
            confirmButton = {
                Button(
                    onClick = {
                        isLogoutConfirmationVisible = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { isLogoutConfirmationVisible = false }) {
                    Text("Cancel", color = TextPearl)
                }
            }
        )
    }
}

/**
 * Standard visual row for each settings option.
 */
@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = EmeraldSafe,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavySurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CrimsonGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = TextPearl,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Sub-Section 0: Call Priority & Dual-SIM Routing Editor
 */
@Composable
fun CallPrioritySubSectionView(
    currentPriority: CallPriority,
    currentSimPreference: SimPreference,
    primaryContactName: String,
    detectedSims: List<DetectedSim>,
    onSelectPriority: (CallPriority) -> Unit,
    onSelectSimPreference: (SimPreference) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Primary Emergency Call Sequence",
                        color = TextPearl,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CrimsonGlow.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Priority",
                            color = CrimsonGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Choose which party ResQLink dials first when you trigger SOS or speak a distress phrase hands-free.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                // Option 1: Nearest Local Station
                CallPriorityOptionCard(
                    title = "Nearest Local Station First (Recommended)",
                    description = "Direct cellular auto-call to nearest 5km DRRMO, police, or rescue station with zero clicks.",
                    isSelected = currentPriority == CallPriority.LOCAL_HOTLINE,
                    badge = "Fastest",
                    onClick = { onSelectPriority(CallPriority.LOCAL_HOTLINE) }
                )

                // Option 2: Universal 911 First
                CallPriorityOptionCard(
                    title = "Universal 911 First",
                    description = "Launches national 911 emergency dispatch prompt immediately.",
                    isSelected = currentPriority == CallPriority.UNIVERSAL_911,
                    badge = "National",
                    onClick = { onSelectPriority(CallPriority.UNIVERSAL_911) }
                )

                // Option 3: Family / ICE Contact First
                CallPriorityOptionCard(
                    title = "Family / ICE Contact First",
                    description = "Directly auto-calls your primary contact: $primaryContactName.",
                    isSelected = currentPriority == CallPriority.FAMILY_FIRST,
                    badge = "Personal",
                    onClick = { onSelectPriority(CallPriority.FAMILY_FIRST) }
                )
            }
        }

        // Section 2: Dual SIM Card Selection for Outgoing Emergency Calls
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Outgoing SIM Card (Dual SIM)",
                        color = TextPearl,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSafe.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Dual SIM",
                            color = EmeraldSafe,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Select which SIM card ResQLink uses for emergency calls and SMS. Choose the SIM with active signal or emergency credit load.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                // SIM Option 1: System Default / Prompt
                CallPriorityOptionCard(
                    title = "System Default / Prompt",
                    description = "Uses Android default voice SIM or displays the system dual-SIM chooser dialog.",
                    isSelected = currentSimPreference == SimPreference.SYSTEM_DEFAULT,
                    badge = "Auto",
                    onClick = { onSelectSimPreference(SimPreference.SYSTEM_DEFAULT) }
                )

                // SIM Option 2: SIM 1
                val sim1Carrier = detectedSims.firstOrNull { it.slotIndex == 0 }?.carrierName
                val sim1Title = if (sim1Carrier != null) "SIM 1 ($sim1Carrier)" else "SIM 1 (Slot 1)"
                CallPriorityOptionCard(
                    title = sim1Title,
                    description = "Always route emergency calls and alerts via SIM Card Slot 1 ($sim1Title).",
                    isSelected = currentSimPreference == SimPreference.SIM_1,
                    badge = "SIM 1",
                    onClick = { onSelectSimPreference(SimPreference.SIM_1) }
                )

                // SIM Option 3: SIM 2
                val sim2Carrier = detectedSims.firstOrNull { it.slotIndex == 1 }?.carrierName
                val sim2Title = if (sim2Carrier != null) "SIM 2 ($sim2Carrier)" else "SIM 2 (Slot 2)"
                CallPriorityOptionCard(
                    title = sim2Title,
                    description = "Always route emergency calls and alerts via SIM Card Slot 2 ($sim2Title).",
                    isSelected = currentSimPreference == SimPreference.SIM_2,
                    badge = "SIM 2",
                    onClick = { onSelectSimPreference(SimPreference.SIM_2) }
                )
            }
        }
    }
}

/**
 * Radio card for picking emergency call priorities or SIM preference.
 */
@Composable
fun CallPriorityOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) NavySurface else NavyCard,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) CrimsonGlow else NavyCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CrimsonGlow,
                    unselectedColor = TextMuted
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = TextPearl,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) CrimsonPrimary.copy(alpha = 0.2f) else NavyCardBorder.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = badge,
                            color = if (isSelected) CrimsonGlow else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Sub-Section 1: Interactive Medical Profile Editor
 */
@Composable
fun EditAccountMedicalView(
    profile: UserProfile,
    onSave: (name: String, blood: String, allergies: String, notes: String, contactName: String, contactPhone: String) -> Unit
) {
    var inputName by remember { mutableStateOf(profile.displayName) }
    var inputBloodType by remember { mutableStateOf(profile.bloodType) }
    var inputAllergies by remember { mutableStateOf(profile.allergies) }
    var inputMedicalNotes by remember { mutableStateOf(profile.medicalNotes) }
    var inputContactName by remember { mutableStateOf(profile.primaryContactName) }
    var inputContactPhone by remember { mutableStateOf(profile.primaryContactPhone) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Personal & Medical Information",
                color = TextPearl,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This data is stored 100% locally on your phone and is displayed on your main emergency screen for responders.",
                color = TextMuted,
                fontSize = 11.sp
            )

            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                label = { Text("Full Name") },
                singleLine = true,
                colors = defaultTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = inputBloodType,
                onValueChange = { inputBloodType = it },
                label = { Text("Blood Type (e.g. O+, A+, B+, AB-)") },
                singleLine = true,
                colors = defaultTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = inputAllergies,
                onValueChange = { inputAllergies = it },
                label = { Text("Allergies (e.g. Penicillin, Peanuts)") },
                singleLine = true,
                colors = defaultTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = inputMedicalNotes,
                onValueChange = { inputMedicalNotes = it },
                label = { Text("Medical Conditions / Notes") },
                colors = defaultTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = inputContactName,
                onValueChange = { inputContactName = it },
                label = { Text("Primary ICE Contact Name") },
                singleLine = true,
                colors = defaultTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = inputContactPhone,
                onValueChange = { inputContactPhone = it },
                label = { Text("Primary ICE Contact Phone") },
                singleLine = true,
                colors = defaultTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    onSave(inputName, inputBloodType, inputAllergies, inputMedicalNotes, inputContactName, inputContactPhone)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * Sub-Section 2: Phonebook Sub-View
 */
@Composable
fun PhonebookSubSectionView(
    contacts: List<com.resqlink.ai.domain.model.TrustedContact>,
    onAddContact: (name: String, phone: String, rel: String) -> Unit,
    onDeleteContact: (id: Long) -> Unit
) {
    var isAddContactDialogVisible by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emergency Contacts (${contacts.size})",
                    color = TextPearl,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { isAddContactDialogVisible = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Recipients who receive direct offline SMS with your GPS coordinates if you are unresponsive.",
                color = TextMuted,
                fontSize = 11.sp
            )

            contacts.forEach { contactItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(contactItem.name, color = TextPearl, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${contactItem.relationship} • ${contactItem.phoneNumber}", color = TextMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { onDeleteContact(contactItem.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (isAddContactDialogVisible) {
        AddContactSubDialog(
            onDismiss = { isAddContactDialogVisible = false },
            onAdd = { newName, newPhone, newRelationship ->
                onAddContact(newName, newPhone, newRelationship)
                isAddContactDialogVisible = false
            }
        )
    }
}

/**
 * Sub-Section 3: Permissions Sub-View
 */
@Composable
fun PermissionsSubSectionView(context: android.content.Context, hasLocation: Boolean) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Android System Permissions", color = TextPearl, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("These permissions are required for offline emergency operation.", color = TextMuted, fontSize = 11.sp)

            PermissionItemRow("Microphone (RECORD_AUDIO)", "Evaluated on-device, zero internet upload.", true)
            PermissionItemRow("Precise Location (ACCESS_FINE_LOCATION)", "Locates nearest 5km station.", hasLocation)
            PermissionItemRow("Phone Calls (CALL_PHONE)", "Programmatically connects you to local hotlines.", true)
            PermissionItemRow("Background SMS (SEND_SMS)", "Dispatches emergency coordinates to family.", true)

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Android System Settings", color = TextPearl, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Sub-Section 4: Guardian AI Sub-View
 */
@Composable
fun GuardianAiSubSectionView(
    isActive: Boolean,
    currentApiKey: String = "",
    onSaveApiKey: (String) -> Unit = {},
    currentElevenLabsKey: String = "",
    onSaveElevenLabsKey: (String) -> Unit = {},
    currentElevenLabsVoiceId: String = "",
    onSaveElevenLabsVoiceId: (String) -> Unit = {},
    onToggleGuardian: () -> Unit,
    onSimulateVoice: () -> Unit
) {
    var voiceSensitivityLevel by remember { mutableStateOf(1f) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Guardian Mode Service", color = TextPearl, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(if (isActive) "Always Listening Offline" else "Monitoring Paused", color = if (isActive) EmeraldSafe else TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { onToggleGuardian() },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldSafe, checkedTrackColor = EmeraldSafe.copy(alpha = 0.3f))
                    )
                }

                // Real-time acoustic soundwave visualizer animation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isActive) "ACOUSTIC VAD ACTIVE" else "ACOUSTIC STANDBY",
                            color = if (isActive) EmeraldSafe else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isActive) "16kHz Mono Stream Monitored" else "Microphone in low-power idle",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    VoiceWaveVisualizer(isActive = isActive)
                }

                HorizontalDivider(color = NavyCardBorder)

                Text("Monitored Keywords:", color = TextPearl, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("• Wake Phrase: \"Resqlink Tulong!\" or \"Resqlink Help!\"\n• Super Distress: \"Tulong!\" (shouting/noise rejected)", color = TextMuted, fontSize = 12.sp)

                Text("Voice Sensitivity:", color = TextPearl, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = voiceSensitivityLevel,
                    onValueChange = { voiceSensitivityLevel = it },
                    valueRange = 0f..2f,
                    steps = 1,
                    colors = SliderDefaults.colors(thumbColor = EmeraldSafe, activeTrackColor = EmeraldSafe)
                )

                Button(
                    onClick = onSimulateVoice,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Voice Recognition (\"Resqlink Tulong!\")", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Card 2: 5km Hotline AI Engine (Gemini 3.1 Flash-Lite)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("5km Hotline AI Engine", color = TextPearl, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavySurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NavyCardBorder)
                    ) {
                        Text(
                            text = "Gemini 3.1 Flash-Lite",
                            color = EmeraldSafe,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Powers the 2-hour periodic scan that discovers real Philippine Barangay halls, PNP precincts, and 24/7 emergency rooms within 5km of your live GPS coordinates.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldSafe.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemini 3.1 AI Engine: Preconfigured (Protected)",
                                color = TextPearl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "API keys are hardcoded in internal AppConfig and hidden from UI",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Card 3: Accident Voice AI (ElevenLabs Engine with Gemini Fallback)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            var selectedVoiceId by remember { mutableStateOf(currentElevenLabsVoiceId) }
            var isVoiceUpdatedNoticeVisible by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accident Voice AI", color = TextPearl, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavySurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NavyCardBorder)
                    ) {
                        Text(
                            text = "ElevenLabs Multilingual v2",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "Synthesizes high-fidelity, gentle bilingual voice (Tagalog & English) over speakerphone for unresponsive accident situations. API credentials are hardcoded and hidden from screen.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ElevenLabs Engine: Securely Compiled",
                                color = TextPearl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hardcoded in AppConfig.ELEVENLABS_API_KEY • Hidden from UI",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Voice Presets
                Text(
                    text = "SELECT VOICE PERSONA (TAGALOG & ENGLISH):",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                val voiceModelPresets = listOf(
                    Triple("Sarah", "EXAVITQu4vr4xnSDxMaL", "Active in All Calls • Soft Tagalog & English (Default)"),
                    Triple("Bella", "hpp4J3VqNfWAUOO0d1Us", "Warm & Professional"),
                    Triple("Rachel", "21m00Tcm4TlvDq8ikWAM", "Calm & Clear"),
                    Triple("Aoede", "Aoede", "Gemini 2.0 Audio Fallback")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    voiceModelPresets.forEach { (voiceName, voiceId, voiceDescription) ->
                        val isSelected = selectedVoiceId.trim().equals(voiceId, ignoreCase = true) || (selectedVoiceId.isBlank() && voiceId == "EXAVITQu4vr4xnSDxMaL")
                        Surface(
                            onClick = {
                                selectedVoiceId = voiceId
                                onSaveElevenLabsVoiceId(voiceId)
                                isVoiceUpdatedNoticeVisible = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0284C7) else NavySurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8) else NavyCardBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = if (isSelected) "✓ $voiceName" else voiceName,
                                    color = if (isSelected) Color.White else TextPearl,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = voiceDescription,
                                    color = if (isSelected) Color(0xFFE0F2FE) else TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                if (isVoiceUpdatedNoticeVisible) {
                    Text(
                        text = "✓ Voice model updated!",
                        color = EmeraldSafe,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Animated soundwave visualization component simulating live microphone activity.
 */
@Composable
fun VoiceWaveVisualizer(isActive: Boolean) {
    val waveTransition = rememberInfiniteTransition(label = "audioWave")
    val waveBarHeight1 by waveTransition.animateFloat(0.25f, 0.95f, infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1")
    val waveBarHeight2 by waveTransition.animateFloat(0.45f, 0.85f, infiniteRepeatable(tween(520, delayMillis = 60, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h2")
    val waveBarHeight3 by waveTransition.animateFloat(0.2f, 1.0f, infiniteRepeatable(tween(340, delayMillis = 120, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3")
    val waveBarHeight4 by waveTransition.animateFloat(0.6f, 0.9f, infiniteRepeatable(tween(600, delayMillis = 40, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h4")
    val waveBarHeight5 by waveTransition.animateFloat(0.35f, 1.0f, infiniteRepeatable(tween(440, delayMillis = 160, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h5")
    val waveBarHeight6 by waveTransition.animateFloat(0.25f, 0.75f, infiniteRepeatable(tween(480, delayMillis = 90, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h6")
    val waveBarHeight7 by waveTransition.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(360, delayMillis = 50, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h7")

    val waveHeightsList = listOf(waveBarHeight1, waveBarHeight2, waveBarHeight3, waveBarHeight4, waveBarHeight5, waveBarHeight6, waveBarHeight7)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        waveHeightsList.forEach { animatedFraction ->
            val actualFraction = if (isActive) animatedFraction else 0.2f
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight(actualFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) EmeraldSafe else TextMuted.copy(alpha = 0.4f))
            )
        }
    }
}

/**
 * Sub-Section 5: About & Emergency Fail-Soft Policy
 */
@Composable
fun AboutPolicySubSectionView() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ResQLink AI v1.1.0", color = TextPearl, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("An offline-first, hands-free emergency assistance app built natively for the Philippines.", color = TextMuted, fontSize = 12.sp)

            HorizontalDivider(color = NavyCardBorder)

            Text("Emergency Hotline Policy", color = CrimsonGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "• Unverified Silence: If distress keywords are detected but you do not respond during the 10-second countdown, the app will NOT auto-call 911 to prevent dispatch swamping. Instead, an unverified SMS with GPS coordinates is sent to your trusted contacts.\n" +
                "• Confirmed Emergency: Tapping SOS or speaking 'HELP' immediately initiates emergency dialing.",
                color = TextPearl,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Individual row displaying system permission item and active status badge.
 */
@Composable
fun PermissionItemRow(title: String, desc: String, isGranted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPearl, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = TextMuted, fontSize = 10.sp)
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isGranted) EmeraldSafe.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
        ) {
            Text(
                text = if (isGranted) "Active" else "Pending",
                color = if (isGranted) EmeraldSafe else AmberWarning,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Modal dialog inside phonebook to enter a new trusted safety contact.
 */
@Composable
fun AddContactSubDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, rel: String) -> Unit
) {
    var contactNameInput by remember { mutableStateOf("") }
    var contactPhoneInput by remember { mutableStateOf("") }
    var contactRelationshipInput by remember { mutableStateOf("Family") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        title = { Text("Add Emergency Contact", color = TextPearl, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = contactNameInput, onValueChange = { contactNameInput = it }, label = { Text("Name") }, colors = defaultTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactPhoneInput, onValueChange = { contactPhoneInput = it }, label = { Text("Phone Number") }, colors = defaultTextFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactRelationshipInput, onValueChange = { contactRelationshipInput = it }, label = { Text("Relationship") }, colors = defaultTextFieldColors(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (contactNameInput.isNotBlank() && contactPhoneInput.isNotBlank()) onAdd(contactNameInput, contactPhoneInput, contactRelationshipInput) }, colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

/**
 * Shared styling configuration for input text fields across the Settings screen.
 */
@Composable
fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPearl,
    unfocusedTextColor = TextPearl,
    focusedBorderColor = CrimsonGlow,
    unfocusedBorderColor = NavyCardBorder,
    focusedLabelColor = CrimsonGlow,
    unfocusedLabelColor = TextMuted
)
