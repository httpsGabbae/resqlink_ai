package com.resqlink.ai.ui.emergency

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resqlink.ai.guides.EmergencyGuide
import com.resqlink.ai.guides.EmergencyGuideRepository
import com.resqlink.ai.guides.GuideCategory
import com.resqlink.ai.guides.GuideLanguage
import com.resqlink.ai.incidents.IncidentRecord
import com.resqlink.ai.readiness.DiagnosticItem
import com.resqlink.ai.readiness.DiagnosticStatus
import com.resqlink.ai.readiness.ReadinessReport
import com.resqlink.ai.rescuecard.RescueCardQrHelper
import com.resqlink.ai.rescuecard.RescueProfile

/**
 * 2x2 Quick Preparedness Tools Grid displayed on HomeScreen.
 */
@Composable
fun PreparednessToolsGrid(
    onOpenRescueCard: () -> Unit,
    onOpenGuides: () -> Unit,
    onOpenReadiness: () -> Unit,
    onOpenIncidents: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SAFETY TOOLS & RESCUE PREPAREDNESS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolTile(
                icon = Icons.Default.QrCode,
                title = "Rescue Card & QR",
                subtitle = "Medical vitals & offline ID",
                iconTint = EmeraldSafe,
                onClick = onOpenRescueCard,
                modifier = Modifier.weight(1f)
            )

            ToolTile(
                icon = Icons.Default.MenuBook,
                title = "Offline Guides",
                subtitle = "CPR, Quake, Typhoon...",
                iconTint = CyanTravel,
                onClick = onOpenGuides,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolTile(
                icon = Icons.Default.FactCheck,
                title = "Readiness Check",
                subtitle = "Hardware & safe test run",
                iconTint = AmberWarning,
                onClick = onOpenReadiness,
                modifier = Modifier.weight(1f)
            )

            ToolTile(
                icon = Icons.Default.History,
                title = "Incident Logs",
                subtitle = "Audit timeline & export",
                iconTint = Color(0xFFA78BFA),
                onClick = onOpenIncidents,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NavySurface,
        border = BorderStroke(1.dp, NavyCardBorder),
        modifier = modifier
            .height(86.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    color = TextPearl,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2
                )
            }
        }
    }
}

// ==========================================
// 1. RESCUE CARD & OFFLINE QR DIALOG
// ==========================================

@Composable
fun RescueCardDialog(
    profile: RescueProfile,
    onUpdateVisibility: (blood: Boolean?, allergies: Boolean?, meds: Boolean?, conditions: Boolean?, notes: Boolean?, contacts: Boolean?) -> Unit,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(profile) {
        try {
            RescueCardQrHelper.createQrBitmap(profile.toQrPayload(), dimensionPx = 360)
        } catch (e: Exception) {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NavyBackground,
            border = BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = EmeraldSafe,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "RESCUE CARD & QR",
                            color = TextPearl,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "First responders and bystanders can scan this offline QR code to instantly access your vital medical data and ICE contacts without unlocking your phone.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                // QR Code Center Box
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Rescue Card QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                // Summary Text Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NavySurface,
                    border = BorderStroke(1.dp, NavyCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "EMERGENCY RESPONDER VIEW",
                            color = EmeraldSafe,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = profile.toSanitizedEmergencySummary(),
                            color = TextPearl,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Granular Visibility Toggles
                Text(
                    text = "RESPONDER PRIVACY VISIBILITY",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                VisibilityToggleRow(
                    label = "Show Blood Type (${profile.bloodType})",
                    checked = profile.isBloodTypeVisible,
                    onCheckedChange = { onUpdateVisibility(it, null, null, null, null, null) }
                )

                VisibilityToggleRow(
                    label = "Show Allergies (${profile.allergies})",
                    checked = profile.isAllergiesVisible,
                    onCheckedChange = { onUpdateVisibility(null, it, null, null, null, null) }
                )

                VisibilityToggleRow(
                    label = "Show Medical Conditions (${profile.medicalConditions})",
                    checked = profile.isConditionsVisible,
                    onCheckedChange = { onUpdateVisibility(null, null, null, it, null, null) }
                )

                VisibilityToggleRow(
                    label = "Show Emergency Instructions",
                    checked = profile.isInstructionsVisible,
                    onCheckedChange = { onUpdateVisibility(null, null, null, null, it, null) }
                )

                VisibilityToggleRow(
                    label = "Show Emergency Contacts",
                    checked = profile.isContactsVisible,
                    onCheckedChange = { onUpdateVisibility(null, null, null, null, null, it) }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun VisibilityToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (checked) TextPearl else TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldSafe,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = NavySurface
            )
        )
    }
}

// ==========================================
// 2. OFFLINE EMERGENCY GUIDES DIALOG
// ==========================================

@Composable
fun OfflineGuidesDialog(
    onDismiss: () -> Unit
) {
    var selectedLang by remember { mutableStateOf(GuideLanguage.FILIPINO) }
    var selectedCategory by remember { mutableStateOf(GuideCategory.CPR) }
    var searchQuery by remember { mutableStateOf("") }

    val guides = remember(selectedLang, searchQuery) {
        if (searchQuery.isBlank()) {
            EmergencyGuideRepository.getGuidesByLanguage(selectedLang)
        } else {
            EmergencyGuideRepository.searchGuides(searchQuery, selectedLang)
        }
    }

    val activeGuide = guides.firstOrNull { it.category == selectedCategory } ?: guides.firstOrNull()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NavyBackground,
            border = BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = CyanTravel,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "OFFLINE GUIDES",
                            color = TextPearl,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // Language toggle tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedLang == GuideLanguage.FILIPINO) CyanTravel.copy(alpha = 0.2f) else NavySurface,
                        border = BorderStroke(1.dp, if (selectedLang == GuideLanguage.FILIPINO) CyanTravel else NavyCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedLang = GuideLanguage.FILIPINO }
                    ) {
                        Text(
                            text = "🇵🇭 Filipino (Tagalog)",
                            color = if (selectedLang == GuideLanguage.FILIPINO) CyanTravel else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedLang == GuideLanguage.ENGLISH) CyanTravel.copy(alpha = 0.2f) else NavySurface,
                        border = BorderStroke(1.dp, if (selectedLang == GuideLanguage.ENGLISH) CyanTravel else NavyCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedLang = GuideLanguage.ENGLISH }
                    ) {
                        Text(
                            text = "🇬🇧 English",
                            color = if (selectedLang == GuideLanguage.ENGLISH) CyanTravel else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Category selector scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GuideCategory.values().forEach { cat ->
                        val isSelected = activeGuide?.category == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CrimsonPrimary.copy(alpha = 0.2f) else NavySurface,
                            border = BorderStroke(1.dp, if (isSelected) CrimsonPrimary else NavyCardBorder),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = if (selectedLang == GuideLanguage.FILIPINO) cat.displayNameFil else cat.displayNameEn,
                                color = if (isSelected) TextPearl else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Guide Content Viewer
                if (activeGuide != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = activeGuide.title,
                            color = TextPearl,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = activeGuide.summary,
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // Source citation badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavySurface,
                            border = BorderStroke(1.dp, NavyCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = EmeraldSafe,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Authoritative Source: ${activeGuide.authoritativeSource}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Numbered Steps
                        Text(
                            text = "STEP-BY-STEP ACTIONS",
                            color = EmeraldSafe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        activeGuide.steps.forEach { step ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NavySurface,
                                border = BorderStroke(1.dp, NavyCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSafe.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = step.stepNumber.toString(),
                                            color = EmeraldSafe,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = step.title,
                                            color = TextPearl,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = step.instruction,
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                        if (step.criticalAlert != null) {
                                            Text(
                                                text = "⚠️ ${step.criticalAlert}",
                                                color = AmberWarning,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // What Not to Do
                        if (activeGuide.whatNotToDo.isNotEmpty()) {
                            Text(
                                text = "CRITICAL WARNINGS (WHAT NOT TO DO)",
                                color = CrimsonGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            activeGuide.whatNotToDo.forEach { warning ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CrimsonPrimary.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⛔ $warning",
                                        color = TextPearl,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. READINESS CHECK & SIMULATION DIALOG
// ==========================================

@Composable
fun ReadinessCheckDialog(
    report: ReadinessReport?,
    onRunDiagnostics: () -> Unit,
    onStartSimulation: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        onRunDiagnostics()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NavyBackground,
            border = BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SYSTEM READINESS CHECK",
                            color = TextPearl,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Performs deep diagnostics across hardware sensors, background services, telephony, and SMS pipelines to verify emergency readiness.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                // Overall status card
                if (report != null) {
                    val pass = report.overallPass
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (pass) EmeraldSafe.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (pass) EmeraldSafe else AmberWarning),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (pass) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (pass) EmeraldSafe else AmberWarning,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (pass) "ALL CRITICAL SYSTEMS OPERATIONAL" else "SOME SUBSYSTEMS NEED ATTENTION",
                                    color = if (pass) EmeraldSafe else AmberWarning,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${report.items.count { it.status == DiagnosticStatus.PASS }} of ${report.items.size} checks passed",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Diagnostics List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(report?.items ?: emptyList()) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NavySurface,
                            border = BorderStroke(1.dp, NavyCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = TextPearl,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.detail,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (item.status) {
                                        DiagnosticStatus.PASS -> EmeraldSafe.copy(alpha = 0.2f)
                                        DiagnosticStatus.WARNING -> AmberWarning.copy(alpha = 0.2f)
                                        DiagnosticStatus.FAIL -> CrimsonPrimary.copy(alpha = 0.2f)
                                        else -> NavyCardBorder
                                    }
                                ) {
                                    Text(
                                        text = item.status.label,
                                        color = when (item.status) {
                                            DiagnosticStatus.PASS -> EmeraldSafe
                                            DiagnosticStatus.WARNING -> AmberWarning
                                            DiagnosticStatus.FAIL -> CrimsonGlow
                                            else -> TextMuted
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions: Refresh + Safe Simulation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRunDiagnostics,
                        border = BorderStroke(1.dp, NavyCardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPearl, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RE-TEST", color = TextPearl, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onStartSimulation()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanTravelDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAFE TEST RUN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. INCIDENT LOGS & EXPORT DIALOG
// ==========================================

@Composable
fun IncidentLogsDialog(
    incidents: List<IncidentRecord>,
    onExportReport: (IncidentRecord) -> Unit,
    onDeleteIncident: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NavyBackground,
            border = BorderStroke(1.dp, NavyCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "INCIDENT AUDIT LOGS",
                            color = TextPearl,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Chronological records of emergency triggers, check-in timelines, and SMS dispatch status. Saved securely on device.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                if (incidents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No emergency incident logs recorded yet.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(incidents) { inc ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NavySurface,
                                border = BorderStroke(1.dp, NavyCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = inc.incidentId,
                                            color = TextPearl,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (inc.isSimulated) CyanTravel.copy(alpha = 0.2f) else CrimsonPrimary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (inc.isSimulated) "SIMULATION" else inc.finalState,
                                                color = if (inc.isSimulated) CyanTravel else CrimsonGlow,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Trigger: ${inc.triggerSource} • SMS Sent: ${inc.contactsSent}/${inc.contactsAttempted}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { onExportReport(inc) }) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = CyanTravel, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Export", color = CyanTravel, fontSize = 11.sp)
                                        }

                                        TextButton(onClick = { onDeleteIncident(inc.incidentId) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete", color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
