package com.resqlink.ai.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.HotlineCategory
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.ui.emergency.EmergencyViewModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Filter categories for stations rendered on the interactive map.
 */
enum class MapFilterCategory(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Public),
    HOSPITALS("Hospitals", Icons.Default.LocalHospital),
    POLICE("Police", Icons.Default.LocalPolice),
    FIRE("Fire", Icons.Default.LocalFireDepartment),
    BARANGAY("Barangay / DRRM", Icons.Default.Apartment),
    CONTACTS("ICE Contacts", Icons.Default.People)
}

/**
 * =========================================================================================
 * 5KM EMERGENCY MAP SCREEN
 * =========================================================================================
 * Educational Overview:
 * This screen renders a tactical, dark-themed offline map displaying:
 * 1. The user's live GPS beacon with pulsating concentric rings.
 * 2. Range rings denoting 1km, 3km, and 5km defense zones around the user.
 * 3. Color-coded emergency responder pins (Hospitals, Police, Fire, Barangay/DRRMO).
 * 4. Offline Leaflet JavaScript engine bundled directly inside assets/leaflet.
 *    If the device is completely offline with no cellular data, it displays a tactical
 *    radar grid with exact coordinate markers, ensuring 100% availability in typhoons
 *    and network blackouts.
 * 5. JavaScript bridge (`ResQBridge`) enabling two-way communication between Leaflet
 *    HTML markers and native Android dialer/navigation intents.
 * =========================================================================================
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    viewModel: EmergencyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDeviceLocation by viewModel.currentLocation.collectAsState()
    val allHotlinesList by viewModel.allHotlines.collectAsState()
    val nearestHotlinesList by viewModel.nearestHotlines.collectAsState()
    val trustedContactsList by viewModel.trustedContacts.collectAsState()
    val userProfileData by viewModel.userProfile.collectAsState()

    var selectedFilterCategory by remember { mutableStateOf(MapFilterCategory.ALL) }
    var selectedHotlinePin by remember { mutableStateOf<Hotline?>(null) }
    var selectedContactPin by remember { mutableStateOf<TrustedContact?>(null) }
    var webViewInstanceReference by remember { mutableStateOf<WebView?>(null) }

    // Intercept hardware and gesture back button to dismiss inspection panels first
    BackHandler {
        if (selectedHotlinePin != null || selectedContactPin != null) {
            selectedHotlinePin = null
            selectedContactPin = null
        } else {
            onBack()
        }
    }

    val userLatitude = currentDeviceLocation?.latitude ?: 14.6507
    val userLongitude = currentDeviceLocation?.longitude ?: 121.0509
    val userAddressLabel = currentDeviceLocation?.address ?: "Current Location"

    // Load bundled local Leaflet CSS and JS (Guarantees zero-network offline functionality)
    val localLeafletCss = remember { LeafletAssetLoader.getCss(context) }
    val localLeafletJs = remember { LeafletAssetLoader.getJs(context) }

    // All hotlines loaded into the map coordinate space (persisted across categories)
    val allUniqueMapHotlines = remember(allHotlinesList, nearestHotlinesList) {
        (nearestHotlinesList + allHotlinesList).distinctBy { it.id }
    }

    // Deduplicated hotlines list for bottom drawer carousel (filtered by category)
    val displayedDrawerHotlines = remember(allHotlinesList, nearestHotlinesList, selectedFilterCategory) {
        val mergedList = (nearestHotlinesList + allHotlinesList).distinctBy { it.id }
        when (selectedFilterCategory) {
            MapFilterCategory.ALL -> mergedList
            MapFilterCategory.HOSPITALS -> mergedList.filter { it.category == HotlineCategory.MEDICAL_HOSPITAL }
            MapFilterCategory.POLICE -> mergedList.filter { it.category == HotlineCategory.POLICE_PNP }
            MapFilterCategory.FIRE -> mergedList.filter { it.category == HotlineCategory.FIRE_BFP }
            MapFilterCategory.BARANGAY -> mergedList.filter {
                it.category == HotlineCategory.BARANGAY || it.category == HotlineCategory.DISASTER_DRRMO
            }
            MapFilterCategory.CONTACTS -> emptyList()
        }
    }

    // Build self-contained Leaflet HTML (recomputed only when GPS coordinates or stations update)
    val completeMapHtmlDocument = remember(userLatitude, userLongitude, allUniqueMapHotlines, trustedContactsList, localLeafletCss, localLeafletJs) {
        generateLeafletMapHtml(
            leafletCss = localLeafletCss,
            leafletJs = localLeafletJs,
            userLat = userLatitude,
            userLng = userLongitude,
            userAddress = userAddressLabel,
            hotlines = allUniqueMapHotlines,
            contacts = trustedContactsList,
            primaryContactName = userProfileData.primaryContactName,
            primaryContactPhone = userProfileData.primaryContactPhone,
            mapboxToken = com.resqlink.ai.data.local.AppConfig.MAPBOX_ACCESS_TOKEN
        )
    }

    // JavaScript Bridge to receive clicks and actions from within the web canvas
    val javaScriptBridge = remember {
        object {
            @JavascriptInterface
            fun onStationClick(stationId: String) {
                val matchedStation = (nearestHotlinesList + allHotlinesList).find { it.id == stationId }
                selectedHotlinePin = matchedStation
                selectedContactPin = null
            }

            @JavascriptInterface
            fun onContactClick(contactId: Long) {
                val matchedContact = trustedContactsList.find { it.id == contactId }
                selectedContactPin = matchedContact
                selectedHotlinePin = null
            }

            @JavascriptInterface
            fun callPhone(phoneNumber: String) {
                viewModel.directCallHotline(phoneNumber)
            }

            @JavascriptInterface
            fun openNavigation(latitude: Double, longitude: Double, stationName: String) {
                try {
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($stationName)")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(mapIntent)
                } catch (e: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF081226))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Top Header Bar with re-centering and external map shortcut
            MapTopBar(
                userAddress = userAddressLabel,
                onBack = onBack,
                onRecenter = {
                    webViewInstanceReference?.evaluateJavascript("recenterUser($userLatitude, $userLongitude);", null)
                },
                onOpenExternalMaps = {
                    try {
                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$userLatitude,$userLongitude?q=$userLatitude,$userLongitude(My+Location)")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$userLatitude,$userLongitude")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    }
                }
            )

            // 2. Category Filter Chips (Dynamic JavaScript marker filter with zero page reload)
            CategoryChipsRow(
                selected = selectedFilterCategory,
                onSelect = { categoryChoice ->
                    selectedFilterCategory = categoryChoice
                    val javaScriptCategoryFilter = when (categoryChoice) {
                        MapFilterCategory.ALL -> "ALL"
                        MapFilterCategory.HOSPITALS -> "MEDICAL_HOSPITAL"
                        MapFilterCategory.POLICE -> "POLICE_PNP"
                        MapFilterCategory.FIRE -> "FIRE_BFP"
                        MapFilterCategory.BARANGAY -> "BARANGAY"
                        MapFilterCategory.CONTACTS -> "CONTACTS"
                    }
                    webViewInstanceReference?.evaluateJavascript("if (window.filterCategory) { filterCategory('$javaScriptCategoryFilter'); }", null)
                }
            )

            // 3. Interactive Leaflet Map Box
            var lastLoadedHtmlString by remember { mutableStateOf("") }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { viewContext ->
                        WebView(viewContext).apply {
                            setBackgroundColor(0xFF0B1528.toInt())
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                allowContentAccess = true
                                allowFileAccess = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = false
                                displayZoomControls = false
                            }
                            addJavascriptInterface(javaScriptBridge, "ResQBridge")
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val destinationUrl = request?.url?.toString() ?: ""
                                    if (destinationUrl.startsWith("tel:")) {
                                        val phoneNumberToDial = destinationUrl.removePrefix("tel:")
                                        viewModel.directCallHotline(phoneNumberToDial)
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadDataWithBaseURL("file:///android_asset/leaflet/", completeMapHtmlDocument, "text/html", "UTF-8", null)
                            lastLoadedHtmlString = completeMapHtmlDocument
                            webViewInstanceReference = this
                        }
                    },
                    update = { activeWebView ->
                        webViewInstanceReference = activeWebView
                        if (lastLoadedHtmlString != completeMapHtmlDocument) {
                            lastLoadedHtmlString = completeMapHtmlDocument
                            activeWebView.loadDataWithBaseURL("file:///android_asset/leaflet/", completeMapHtmlDocument, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 5km Radius Badge overlay
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF101C36).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFF263964)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "5km Defense Zone",
                            color = Color(0xFFF8FAFC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Bottom Detail Drawer & Horizontal Carousel
            MapBottomDrawer(
                selectedHotline = selectedHotlinePin,
                selectedContact = selectedContactPin,
                hotlines = displayedDrawerHotlines,
                contacts = trustedContactsList,
                primaryContactName = userProfileData.primaryContactName,
                primaryContactPhone = userProfileData.primaryContactPhone,

                onDismissSelection = {
                    selectedHotlinePin = null
                    selectedContactPin = null
                },
                onSelectHotline = { hotline ->
                    selectedHotlinePin = hotline
                    selectedContactPin = null
                    webViewInstanceReference?.evaluateJavascript("panToCoordinates(${hotline.latitude}, ${hotline.longitude});", null)
                },
                onSelectContact = { contact ->
                    selectedContactPin = contact
                    selectedHotlinePin = null
                },
                onCall = { phone -> viewModel.directCallHotline(phone) },
                onNavigate = { targetLat, targetLng, targetLabel ->
                    try {
                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$targetLat,$targetLng?q=$targetLat,$targetLng($targetLabel)")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$targetLat,$targetLng")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    }
                }
            )
        }
    }
}

/**
 * Top app bar displaying user locality and map navigation actions.
 */
@Composable
fun MapTopBar(
    userAddress: String,
    onBack: () -> Unit,
    onRecenter: () -> Unit,
    onOpenExternalMaps: () -> Unit
) {
    Surface(
        color = Color(0xFF101C36),
        border = BorderStroke(1.dp, Color(0xFF263964)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF162544))
                        .border(1.dp, Color(0xFF263964), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFF8FAFC),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "5km Emergency Map",
                        color = Color(0xFFF8FAFC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = userAddress,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Re-center button: re-focuses map on current user beacon
                IconButton(
                    onClick = onRecenter,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF162544))
                        .border(1.dp, Color(0xFF263964), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Re-center",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Launch external Google Maps navigation
                IconButton(
                    onClick = onOpenExternalMaps,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF162544))
                        .border(1.dp, Color(0xFF263964), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in Google Maps",
                        tint = Color(0xFFFF334B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Filter row displaying category selector chips.
 */
@Composable
fun CategoryChipsRow(
    selected: MapFilterCategory,
    onSelect: (MapFilterCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1B2A))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapFilterCategory.values().forEach { category ->
            val isSelected = selected == category
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Color(0xFFE53935) else Color(0xFF162544),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF334B) else Color(0xFF263964)),
                modifier = Modifier.clickable { onSelect(category) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = category.label,
                        color = if (isSelected) Color.White else Color(0xFFF8FAFC),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Bottom drawer displaying either a detailed card for a tapped station/contact,
 * or a horizontal carousel of all discovered emergency stations and ICE family contacts.
 */
@Composable
fun MapBottomDrawer(
    selectedHotline: Hotline?,
    selectedContact: TrustedContact?,
    hotlines: List<Hotline>,
    contacts: List<TrustedContact>,
    primaryContactName: String,
    primaryContactPhone: String,

    onDismissSelection: () -> Unit,
    onSelectHotline: (Hotline) -> Unit,
    onSelectContact: (TrustedContact) -> Unit,
    onCall: (String) -> Unit,
    onNavigate: (Double, Double, String) -> Unit
) {
    Surface(
        color = Color(0xFF101C36),
        border = BorderStroke(1.dp, Color(0xFF263964)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (selectedHotline != null) {
                // Expanded detail card for tapped hotline
                HotlineDetailCard(
                    hotline = selectedHotline,
                    onDismiss = onDismissSelection,
                    onCall = { onCall(selectedHotline.primaryNumber) },
                    onNavigate = { onNavigate(selectedHotline.latitude, selectedHotline.longitude, selectedHotline.name) }
                )
            } else if (selectedContact != null) {
                // Expanded detail card for tapped contact
                ContactDetailCard(
                    contact = selectedContact,
                    onDismiss = onDismissSelection,
                    onCall = { onCall(selectedContact.phoneNumber) }
                )
            } else {
                // Horizontal Carousel of Discovered Stations and Contacts
                Text(
                    text = "DISCOVERED STATIONS & ICE CONTACTS (${hotlines.size + contacts.size + 1})",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ICE Primary Contact Card
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                            border = BorderStroke(1.dp, Color(0xFF4338CA)),
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { onCall(primaryContactPhone) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ICE FAMILY",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Icon(
                                        imageVector = Icons.Default.FamilyRestroom,
                                        contentDescription = null,
                                        tint = Color(0xFFA5B4FC),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = primaryContactName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = primaryContactPhone,
                                    color = Color(0xFFE0E7FF),
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onCall(primaryContactPhone) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                ) {
                                    Text("Call Direct", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Hotlines
                    items(hotlines) { hotline ->
                        val badgeColor = when (hotline.category) {
                            HotlineCategory.MEDICAL_HOSPITAL -> Color(0xFFE53935)
                            HotlineCategory.POLICE_PNP -> Color(0xFF2563EB)
                            HotlineCategory.FIRE_BFP -> Color(0xFFEA580C)
                            HotlineCategory.BARANGAY, HotlineCategory.DISASTER_DRRMO -> Color(0xFF059669)
                            else -> Color(0xFF94A3B8)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF162544)),
                            border = BorderStroke(1.dp, Color(0xFF263964)),
                            modifier = Modifier
                                .width(185.dp)
                                .clickable { onSelectHotline(hotline) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = hotline.category.name.replace("_", " "),
                                        color = badgeColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = hotline.distanceKm?.let { "${it}km" } ?: "",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = hotline.name,
                                    color = Color(0xFFF8FAFC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = hotline.primaryNumber,
                                    color = Color(0xFFFF334B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = { onCall(hotline.primaryNumber) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(26.dp)
                                    ) {
                                        Text("Call", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onSelectHotline(hotline) },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF263964)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(26.dp)
                                    ) {
                                        Text("Pin", color = Color(0xFFF8FAFC), fontSize = 10.sp)
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

/**
 * Detailed view for an individual emergency station.
 */
@Composable
fun HotlineDetailCard(
    hotline: Hotline,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onNavigate: () -> Unit
) {
    val categoryColor = when (hotline.category) {
        HotlineCategory.MEDICAL_HOSPITAL -> Color(0xFFE53935)
        HotlineCategory.POLICE_PNP -> Color(0xFF2563EB)
        HotlineCategory.FIRE_BFP -> Color(0xFFEA580C)
        HotlineCategory.BARANGAY, HotlineCategory.DISASTER_DRRMO -> Color(0xFF059669)
        else -> Color(0xFF94A3B8)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162544)),
        border = BorderStroke(1.dp, categoryColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = categoryColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, categoryColor)
                ) {
                    Text(
                        text = hotline.category.name.replace("_", " "),
                        color = categoryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    hotline.distanceKm?.let { distance ->
                        Text(
                            text = "${distance}km away",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = hotline.name,
                color = Color(0xFFF8FAFC),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${hotline.cityMunicipality}, ${hotline.province}",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Call ${hotline.primaryNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Navigate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Detailed view for an individual ICE contact.
 */
@Composable
fun ContactDetailCard(
    contact: TrustedContact,
    onDismiss: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF6366F1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF6366F1))
                ) {
                    Text(
                        text = "ICE EMERGENCY CONTACT",
                        color = Color(0xFFA5B4FC),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${contact.relationship} • Authorized for Emergency Dispatch",
                color = Color(0xFFC7D2FE),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCall,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Call ${contact.name} (${contact.phoneNumber})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Thread-safe cached asset loader for locally bundled Leaflet library files.
 * Guarantees zero network latency, instant DOM execution, and 100% offline uptime.
 */
object LeafletAssetLoader {
    @Volatile
    private var cachedCss: String? = null
    @Volatile
    private var cachedJs: String? = null

    fun getCss(context: Context): String {
        return cachedCss ?: synchronized(this) {
            cachedCss ?: try {
                context.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
                    .also { cachedCss = it }
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun getJs(context: Context): String {
        return cachedJs ?: synchronized(this) {
            cachedJs ?: try {
                context.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
                    .also { cachedJs = it }
            } catch (e: Exception) {
                ""
            }
        }
    }
}

/**
 * Generates self-contained Leaflet HTML document containing:
 * - Embedded CSS & JavaScript.
 * - Mapbox or CartoDB Dark Matter tile layer with OpenStreetMap fallback.
 * - Concentric defense rings (1km, 3km, 5km).
 * - Custom SVG pin icons for hospitals, police, fire, and barangay stations.
 * - Interactive popups with direct call & external navigation links.
 */
fun generateLeafletMapHtml(
    leafletCss: String,
    leafletJs: String,
    userLat: Double,
    userLng: Double,
    userAddress: String,
    hotlines: List<Hotline>,
    contacts: List<TrustedContact>,
    primaryContactName: String,
    primaryContactPhone: String,
    mapboxToken: String = ""
): String {
    val hotlinesJson = JSONArray().apply {
        for (h in hotlines) {
            put(JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("category", h.category.name)
                put("phone", h.primaryNumber)
                put("lat", h.latitude)
                put("lng", h.longitude)
                put("distance", h.distanceKm ?: 0.0)
                put("city", h.cityMunicipality)
            })
        }
    }.toString()

    return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
      <style>
        $leafletCss

        html, body {
          margin: 0;
          padding: 0;
          width: 100%;
          height: 100%;
          background-color: #0b1528;
          overflow: hidden;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        #map {
          position: absolute;
          top: 0;
          bottom: 0;
          left: 0;
          right: 0;
          background-color: #0b1528;
          background-image: 
            radial-gradient(circle at center, rgba(37, 99, 235, 0.18) 0%, transparent 70%),
            linear-gradient(rgba(56, 189, 248, 0.08) 1px, transparent 1px),
            linear-gradient(90deg, rgba(56, 189, 248, 0.08) 1px, transparent 1px);
          background-size: 100% 100%, 36px 36px, 36px 36px;
        }
        .leaflet-container {
          background: #0b1528;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        
        /* Dark Popups */
        .leaflet-popup-content-wrapper {
          background: #162544;
          color: #F8FAFC;
          border: 1px solid #263964;
          border-radius: 12px;
          box-shadow: 0 8px 24px rgba(0,0,0,0.6);
          padding: 4px;
        }
        .leaflet-popup-tip { background: #162544; border: 1px solid #263964; }
        .popup-title { font-size: 13px; font-weight: 800; color: #F8FAFC; margin-bottom: 2px; }
        .popup-cat { font-size: 9px; font-weight: 800; text-transform: uppercase; margin-bottom: 4px; }
        .popup-dist { font-size: 10px; color: #10B981; font-weight: 700; margin-bottom: 6px; }
        .popup-btn {
          display: block;
          width: 100%;
          text-align: center;
          padding: 7px 0;
          background: #1B5E20;
          color: white;
          text-decoration: none;
          font-size: 11px;
          font-weight: 800;
          border-radius: 6px;
          margin-top: 4px;
          border: none;
          cursor: pointer;
        }
        .popup-nav-btn {
          background: #0284C7;
          margin-top: 4px;
        }
        
        /* Pulse Animation for User Beacon */
        .user-pulse-marker {
          position: relative;
        }
        .user-pulse-dot {
          width: 14px;
          height: 14px;
          background: #10B981;
          border: 2px solid #FFFFFF;
          border-radius: 50%;
          box-shadow: 0 0 12px #10B981;
        }
        .user-pulse-ring {
          position: absolute;
          top: -7px;
          left: -7px;
          width: 28px;
          height: 28px;
          border-radius: 50%;
          border: 2px solid #10B981;
          animation: mapPulse 1.8s infinite ease-out;
        }
        @keyframes mapPulse {
          0% { transform: scale(0.5); opacity: 1; }
          100% { transform: scale(1.6); opacity: 0; }
        }
      </style>
      <script>
        $leafletJs
      </script>
    </head>
    <body>
      <div id="map"></div>
      <script>
        function initMap() {
          if (typeof L === 'undefined') {
            console.error('Leaflet failed to load');
            return;
          }

          const userLat = $userLat;
          const userLng = $userLng;
          const hotlines = $hotlinesJson;

          const map = L.map('map', {
            zoomControl: false,
            attributionControl: false,
            minZoom: 10,
            maxZoom: 19
          }).setView([userLat, userLng], 14);

          window.resqMap = map;

          // CartoDB Dark Matter tiles with Mapbox and OpenStreetMap fallback
          const mapboxToken = "$mapboxToken";
          const darkTiles = L.tileLayer((mapboxToken && mapboxToken.length > 10) ? 'https://api.mapbox.com/styles/v1/mapbox/dark-v11/tiles/256/{z}/{x}/{y}@2x?access_token=' + mapboxToken : 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
            maxZoom: 19,
            subdomains: 'abcd',
            errorTileUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'
          });
          darkTiles.on('tileerror', function() {
            if (!window._osmFallback) {
              window._osmFallback = true;
              L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                errorTileUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'
              }).addTo(map);
            }
          });
          darkTiles.addTo(map);

          // Range Rings: 1km, 3km, 5km Defense Zone
          L.circle([userLat, userLng], {
            radius: 1000,
            color: '#38BDF8',
            weight: 1,
            dashArray: '4, 4',
            fill: false
          }).addTo(map);

          L.circle([userLat, userLng], {
            radius: 3000,
            color: '#38BDF8',
            weight: 1,
            dashArray: '6, 6',
            fill: false
          }).addTo(map);

          // 5km Radius Defense Perimeter
          L.circle([userLat, userLng], {
            radius: 5000,
            color: '#E53935',
            weight: 2,
            dashArray: '6, 6',
            fillColor: '#E53935',
            fillOpacity: 0.06
          }).addTo(map);

          // User Pulse Marker Icon
          const userIcon = L.divIcon({
            className: 'user-pulse-marker',
            html: '<div class="user-pulse-ring"></div><div class="user-pulse-dot"></div>',
            iconSize: [14, 14],
            iconAnchor: [7, 7]
          });

          L.marker([userLat, userLng], { icon: userIcon, zIndexOffset: 1000 })
            .addTo(map)
            .bindPopup('<div class="popup-title">Your GPS Location</div><div class="popup-dist">$userAddress</div>');

          // Helper to create category SVG icon
          function createCategoryPin(color, emoji) {
            const svg = `
              <svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36">
                <path d="M14 0C6.27 0 0 6.27 0 14c0 10.5 14 22 14 22s14-11.5 14-22c0-7.73-6.27-14-14-14z" fill="` + color + `" stroke="#FFFFFF" stroke-width="1.5"/>
                <text x="14" y="18" font-size="12" text-anchor="middle" dominant-baseline="central">` + emoji + `</text>
              </svg>
            `;
            return L.divIcon({
              className: 'custom-cat-pin',
              html: svg,
              iconSize: [28, 36],
              iconAnchor: [14, 36],
              popupAnchor: [0, -32]
            });
          }

          const iconHospital = createCategoryPin('#E53935', '🏥');
          const iconPolice = createCategoryPin('#2563EB', '👮');
          const iconFire = createCategoryPin('#EA580C', '🚒');
          const iconBarangay = createCategoryPin('#059669', '🏛️');

          const markers = [];

          hotlines.forEach(h => {
            let icon = iconHospital;
            let catColor = '#E53935';
            let catLabel = 'Hospital / ER';
            if (h.category.indexOf('POLICE') !== -1) {
              icon = iconPolice;
              catColor = '#2563EB';
              catLabel = 'Police Precinct';
            } else if (h.category.indexOf('FIRE') !== -1) {
              icon = iconFire;
              catColor = '#EA580C';
              catLabel = 'Fire Station';
            } else if (h.category.indexOf('BARANGAY') !== -1 || h.category.indexOf('DISASTER') !== -1) {
              icon = iconBarangay;
              catColor = '#059669';
              catLabel = 'Barangay / DRRM';
            }

            const safeName = (h.name || '').replace(/['"]/g, '');
            const safePhone = (h.phone || '').replace(/['"]/g, '');
            const distLabel = h.distance ? (h.distance + ' km away') : (h.city || '');

            const popupContent = `
              <div>
                <div class="popup-cat" style="color: ` + catColor + `;">` + catLabel + `</div>
                <div class="popup-title">` + safeName + `</div>
                <div class="popup-dist">` + distLabel + `</div>
                <button class="popup-btn" onclick="callPhone('` + safePhone + `')">📞 Call ` + safePhone + `</button>
                <button class="popup-btn popup-nav-btn" onclick="navigateStation(` + h.lat + `, ` + h.lng + `, '` + safeName + `')">🧭 Open in Maps</button>
              </div>
            `;

            const m = L.marker([h.lat, h.lng], { icon: icon })
              .addTo(map)
              .bindPopup(popupContent);

            m.on('click', function() {
              if (window.ResQBridge) {
                window.ResQBridge.onStationClick(h.id);
              }
            });

            markers.push({ marker: m, category: h.category });
          });

          window.filterCategory = function(cat) {
            markers.forEach(function(item) {
              var match = false;
              if (cat === 'ALL') match = true;
              else if (cat === 'MEDICAL_HOSPITAL' && item.category.indexOf('HOSPITAL') !== -1) match = true;
              else if (cat === 'POLICE_PNP' && item.category.indexOf('POLICE') !== -1) match = true;
              else if (cat === 'FIRE_BFP' && item.category.indexOf('FIRE') !== -1) match = true;
              else if (cat === 'BARANGAY' && (item.category.indexOf('BARANGAY') !== -1 || item.category.indexOf('DISASTER') !== -1)) match = true;
              else if (cat === 'CONTACTS') match = false;

              if (match) {
                if (!map.hasLayer(item.marker)) map.addLayer(item.marker);
              } else {
                if (map.hasLayer(item.marker)) map.removeLayer(item.marker);
              }
            });
          };

          // Invalidate size to ensure proper rendering inside WebView
          setTimeout(function() { map.invalidateSize(); }, 150);
          setTimeout(function() { map.invalidateSize(); }, 400);
        }

        function callPhone(number) {
          if (window.ResQBridge) {
            window.ResQBridge.callPhone(number);
          } else {
            window.location.href = 'tel:' + number;
          }
        }

        function navigateStation(lat, lng, label) {
          if (window.ResQBridge) {
            window.ResQBridge.openNavigation(lat, lng, label);
          }
        }

        window.recenterUser = function(lat, lng) {
          if (window.resqMap) {
            window.resqMap.flyTo([lat, lng], 14, { animate: true, duration: 1.0 });
          }
        };

        window.panToCoordinates = function(lat, lng) {
          if (window.resqMap) {
            window.resqMap.flyTo([lat, lng], 16, { animate: true, duration: 0.8 });
          }
        };

        if (document.readyState === 'loading') {
          document.addEventListener('DOMContentLoaded', initMap);
        } else {
          initMap();
        }
      </script>
    </body>
    </html>
    """.trimIndent()
}
