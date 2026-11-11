package com.resqlink.ai.ui.directory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.HotlineCategory
import com.resqlink.ai.ui.emergency.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * =========================================================================================
 * DIRECTORY SCREEN
 * =========================================================================================
 * Educational Overview:
 * This screen displays the emergency telephone directory for ResQLink AI. It serves two main
 * safety purposes:
 *
 * 1. NATIONAL HOTLINES BANNER:
 *    Displays verified Philippine national emergency contact numbers derived from official
 *    public safety references (e.g. Kaya Natin PH):
 *    - 911: National Emergency Hotline (Police, Fire, Medical dispatch)
 *    - 117: Philippine National Police (PNP)
 *    - 143 or (02) 8527-0000: Philippine Red Cross
 *    - 165-02: Department of Public Works and Highways (DPWH - Road hazards & flooding)
 *
 * 2. 5KM LOCAL EMERGENCY STATIONS:
 *    Displays nearby barangay halls, municipal police stations, fire stations (BFP), and
 *    hospitals within a 5-kilometer radius of the user's current GPS location.
 *
 * All telephone calls initiated from this screen are user-confirmed and user-initiated.
 * =========================================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    viewModel: EmergencyViewModel,
    modifier: Modifier = Modifier
) {
    // Collect reactive state flows from the ViewModel
    val allHotlinesList by viewModel.allHotlines.collectAsState()
    val nearestHotlinesList by viewModel.nearestHotlines.collectAsState()
    val isAiScanningActive by viewModel.isAiScanning.collectAsState()
    val lastScanTimestampMillis by viewModel.lastAiScanTime.collectAsState()
    val currentDeviceLocation by viewModel.currentLocation.collectAsState()

    // Local UI state for searching and filtering by category
    var textSearchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf<HotlineCategory?>(null) }

    // Prioritize 5km nearest stations if available; fall back to the complete directory only when actively searching
    val filteredHotlinesList = remember(allHotlinesList, nearestHotlinesList, textSearchQuery, selectedFilterCategory) {
        val baseHotlinesList = if (textSearchQuery.isBlank()) {
            if (nearestHotlinesList.isNotEmpty()) {
                nearestHotlinesList
            } else {
                allHotlinesList.filter { (it.distanceKm ?: 999.0) <= 2.5 }
            }
        } else {
            allHotlinesList
        }

        baseHotlinesList.filter { hotline ->
            val matchesCategory = selectedFilterCategory == null || hotline.category == selectedFilterCategory
            val matchesSearchText = textSearchQuery.isBlank() ||
                    hotline.name.contains(textSearchQuery, ignoreCase = true) ||
                    hotline.cityMunicipality.contains(textSearchQuery, ignoreCase = true) ||
                    hotline.province.contains(textSearchQuery, ignoreCase = true) ||
                    hotline.primaryNumber.contains(textSearchQuery)
            matchesCategory && matchesSearchText
        }.sortedWith(
            compareByDescending<Hotline> { it.isPinned }
                .thenBy { it.distanceKm ?: Double.MAX_VALUE }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header: Screen title, current approximate locality, and sync badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2.5km Local Hotlines",
                        fontWeight = FontWeight.Black,
                        color = TextPearl,
                        fontSize = 22.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = currentDeviceLocation?.address?.let { "Near $it" } ?: "Philippine local emergency stations within 2.5km",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSafe.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, EmeraldSafe)
                ) {
                    Text(
                        text = "2h Auto-Sync",
                        color = EmeraldSafe,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Hotline Scanner Action Card
            AiScannerControlCard(
                isScanning = isAiScanningActive,
                lastScanTime = lastScanTimestampMillis,
                onTriggerScan = { viewModel.triggerAiHotlineScan() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar for quick name or location lookup
            OutlinedTextField(
                value = textSearchQuery,
                onValueChange = { textSearchQuery = it },
                placeholder = { Text("Search barangay, police, hospital...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (textSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { textSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NavyCard,
                    unfocusedContainerColor = NavyCard,
                    focusedBorderColor = CrimsonGlow,
                    unfocusedBorderColor = NavyCardBorder,
                    focusedTextColor = TextPearl,
                    unfocusedTextColor = TextPearl
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips (Barangay, Police, Hospital, DRRMO, Fire)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip(
                    title = "All (2.5km)",
                    selected = selectedFilterCategory == null,
                    onClick = { selectedFilterCategory = null }
                )
                CategoryChip(
                    title = "Barangay",
                    selected = selectedFilterCategory == HotlineCategory.BARANGAY,
                    onClick = { selectedFilterCategory = if (selectedFilterCategory == HotlineCategory.BARANGAY) null else HotlineCategory.BARANGAY }
                )
                CategoryChip(
                    title = "Police (PNP)",
                    selected = selectedFilterCategory == HotlineCategory.POLICE_PNP,
                    onClick = { selectedFilterCategory = if (selectedFilterCategory == HotlineCategory.POLICE_PNP) null else HotlineCategory.POLICE_PNP }
                )
                CategoryChip(
                    title = "Hospital / ER",
                    selected = selectedFilterCategory == HotlineCategory.MEDICAL_HOSPITAL,
                    onClick = { selectedFilterCategory = if (selectedFilterCategory == HotlineCategory.MEDICAL_HOSPITAL) null else HotlineCategory.MEDICAL_HOSPITAL }
                )
                CategoryChip(
                    title = "DRRMO",
                    selected = selectedFilterCategory == HotlineCategory.DISASTER_DRRMO,
                    onClick = { selectedFilterCategory = if (selectedFilterCategory == HotlineCategory.DISASTER_DRRMO) null else HotlineCategory.DISASTER_DRRMO }
                )
                CategoryChip(
                    title = "Fire (BFP)",
                    selected = selectedFilterCategory == HotlineCategory.FIRE_BFP,
                    onClick = { selectedFilterCategory = if (selectedFilterCategory == HotlineCategory.FIRE_BFP) null else HotlineCategory.FIRE_BFP }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Render discovered station list or an informative empty state
            if (filteredHotlinesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No local stations found in this filter.",
                            color = TextPearl,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap 'Scan 5km Area with AI' above to discover stations.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // When no query is typed, pin the Philippine National Emergency Hotlines Card at the top
                    if (textSearchQuery.isBlank() && selectedFilterCategory == null) {
                        item(key = "national_hotlines_banner") {
                            NationalEmergencyHotlinesCard(
                                onCall = { targetPhoneNumber -> viewModel.directCallHotline(targetPhoneNumber) }
                            )
                        }
                    }

                    items(filteredHotlinesList, key = { it.id }) { hotline ->
                        HotlineCard(
                            hotline = hotline,
                            onCall = { viewModel.dialHotlineDirectly(hotline) },
                            onTogglePin = { viewModel.togglePinHotline(hotline) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * AI Scanner Control Card provides an on-demand button to trigger a 5km spatial scan
 * using Gemini 3.1 Flash-Lite to discover verified local responder facilities.
 */
@Composable
fun AiScannerControlCard(
    isScanning: Boolean,
    lastScanTime: Long,
    onTriggerScan: () -> Unit
) {
    val radarTransition = rememberInfiniteTransition(label = "aiScanPulse")
    val radarSweepAngle by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotate"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.dp, if (isScanning) CrimsonGlow else NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        .clip(CircleShape)
                        .background(if (isScanning) CrimsonPrimary.copy(alpha = 0.2f) else NavySurface)
                        .border(1.dp, if (isScanning) CrimsonPrimary else NavyCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Sync else Icons.Default.Radar,
                        contentDescription = "Radar",
                        tint = if (isScanning) CrimsonGlow else EmeraldSafe,
                        modifier = Modifier
                            .size(22.dp)
                            .then(if (isScanning) Modifier.rotate(radarSweepAngle) else Modifier)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isScanning) "Gemini AI Scanning 2.5km..." else "2.5km AI Station Scanner",
                        fontWeight = FontWeight.Bold,
                        color = TextPearl,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isScanning) "Discovering Barangay, Police & ER..." else formatLastScan(lastScanTime),
                        fontSize = 11.sp,
                        color = if (isScanning) CrimsonGlow else TextMuted
                    )
                }
            }

            Button(
                onClick = onTriggerScan,
                enabled = !isScanning,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonPrimary,
                    disabledContainerColor = CrimsonPrimary.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isScanning) "Scanning..." else "Scan AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Filter chip for categorizing hotlines by emergency department.
 */
@Composable
fun CategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(title, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CrimsonPrimary,
            selectedLabelColor = Color.White,
            containerColor = NavyCard,
            labelColor = TextPearl
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = NavyCardBorder,
            selectedBorderColor = CrimsonPrimary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

/**
 * Individual Hotline Card displaying station name, distance in kilometers,
 * jurisdiction, and direct call button.
 */
@Composable
fun HotlineCard(
    hotline: Hotline,
    onCall: () -> Unit,
    onTogglePin: () -> Unit
) {
    val categoryAccentColor = when (hotline.category) {
        HotlineCategory.BARANGAY -> EmeraldSafe
        HotlineCategory.POLICE_PNP -> Color(0xFF3B82F6) // Police Blue
        HotlineCategory.MEDICAL_HOSPITAL -> CrimsonPrimary // Hospital Red
        HotlineCategory.FIRE_BFP -> Color(0xFFF97316) // Fire Orange
        HotlineCategory.DISASTER_DRRMO -> AmberWarning
        else -> TextMuted
    }

    val categoryIcon = when (hotline.category) {
        HotlineCategory.BARANGAY -> Icons.Default.HolidayVillage
        HotlineCategory.POLICE_PNP -> Icons.Default.LocalPolice
        HotlineCategory.MEDICAL_HOSPITAL -> Icons.Default.LocalHospital
        HotlineCategory.FIRE_BFP -> Icons.Default.LocalFireDepartment
        HotlineCategory.DISASTER_DRRMO -> Icons.Default.Shield
        else -> Icons.Default.Phone
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(
            width = if (hotline.isPinned) 1.5.dp else 1.dp,
            color = if (hotline.isPinned) AmberWarning else NavyCardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Pinned indicator badge if user selected this station as primary target
            if (hotline.isPinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = AmberWarning,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PRIMARY SOS CALL TARGET (AUTO-DIALED FIRST)",
                        color = AmberWarning,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge & distance indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryAccentColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, categoryAccentColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = categoryAccentColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = hotline.category.displayName,
                                color = categoryAccentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    hotline.distanceKm?.let { distanceInKm ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NavySurface,
                            border = BorderStroke(0.5.dp, NavyCardBorder)
                        ) {
                            Text(
                                text = "${distanceInKm}km away",
                                color = TextPearl,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Pin / Favorite Toggle Button
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin as Primary",
                        tint = if (hotline.isPinned) AmberWarning else TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Station Name
            Text(
                text = hotline.name,
                fontWeight = FontWeight.Bold,
                color = TextPearl,
                fontSize = 15.sp,
                lineHeight = 19.sp
            )

            // Address / Geographic Jurisdiction
            Text(
                text = "${hotline.cityMunicipality}, ${hotline.province}",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone Numbers & Direct Call Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = hotline.primaryNumber,
                        fontWeight = FontWeight.Black,
                        color = CrimsonGlow,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                    hotline.alternateNumber?.let { alternatePhoneNumber ->
                        Text(
                            text = "Alt: $alternatePhoneNumber",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Helper to display human-friendly elapsed time since the last AI discovery scan.
 */
private fun formatLastScan(timestampMillis: Long): String {
    if (timestampMillis == 0L) return "Tap Scan AI to discover stations"
    val elapsedMillis = System.currentTimeMillis() - timestampMillis
    val elapsedMinutes = elapsedMillis / (1000 * 60)
    return when {
        elapsedMinutes < 1 -> "Last scanned: Just now"
        elapsedMinutes < 60 -> "Last scanned: ${elapsedMinutes}m ago"
        else -> {
            val elapsedHours = elapsedMinutes / 60
            "Last scanned: ${elapsedHours}h ago"
        }
    }
}

/**
 * =========================================================================================
 * PHILIPPINE NATIONAL EMERGENCY HOTLINES CARD
 * =========================================================================================
 * Formatted exactly according to official Philippine public safety guidelines:
 * 1. National Emergency Hotline: 911
 * 2. Philippine National Police: 117
 * 3. Philippine Red Cross: 143 or 8527-000 (02-8527-0000)
 * 4. DPWH (Road emergencies): 165-02
 * =========================================================================================
 */
@Composable
fun NationalEmergencyHotlinesCard(
    onCall: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF7F1D1D), CrimsonPrimary, Color(0xFF991B1B))
                        )
                    )
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EMERGENCY HOTLINES",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "IN CASE OF EMERGENCY, YOU MAY CONTACT THE FOLLOWING",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. National Emergency Hotline: 911
                NationalHotlineRow(
                    title = "NATIONAL EMERGENCY HOTLINE",
                    number = "911",
                    badgeColor = CrimsonPrimary,
                    onCall = { onCall("911") }
                )

                // 2. Philippine National Police: 117
                NationalHotlineRow(
                    title = "PHILIPPINE NATIONAL POLICE",
                    number = "117",
                    badgeColor = Color(0xFF1D4ED8),
                    onCall = { onCall("117") }
                )

                // 3. Philippine Red Cross: 143 or 8527-000
                NationalHotlineRow(
                    title = "PHILIPPINE RED CROSS",
                    number = "143 or 8527-000",
                    badgeColor = CrimsonPrimary,
                    onCall = { onCall("143") },
                    altNumber = "02-8527-0000",
                    onCallAlt = { onCall("0285270000") }
                )

                // 4. DPWH (Public Works & Roads): 165-02
                NationalHotlineRow(
                    title = "DPWH (PUBLIC WORKS & ROADS)",
                    number = "165-02",
                    badgeColor = Color(0xFFD97706),
                    onCall = { onCall("165-02") }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source: Kaya Natin PH",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "24/7 Philippines Toll-Free",
                        color = EmeraldSafe,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Compact row item for a National Emergency Hotline with primary and landline buttons.
 */
@Composable
private fun NationalHotlineRow(
    title: String,
    number: String,
    badgeColor: Color,
    onCall: () -> Unit,
    altNumber: String? = null,
    onCallAlt: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF070E1E),
        border = BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(badgeColor, RoundedCornerShape(4.dp))
                    .padding(vertical = 3.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = number,
                    color = TextPearl,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onCall,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DIAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    if (altNumber != null && onCallAlt != null) {
                        OutlinedButton(
                            onClick = onCallAlt,
                            border = BorderStroke(1.dp, NavyCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = NavySurface),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("LANDLINE", color = TextPearl, fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        }
    }
}
