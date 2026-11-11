package com.resqlink.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.resqlink.ai.service.GuardianForegroundService
import com.resqlink.ai.ui.auth.LoginScreen
import com.resqlink.ai.ui.contacts.ContactsScreen
import com.resqlink.ai.ui.directory.DirectoryScreen
import com.resqlink.ai.ui.emergency.*
import com.resqlink.ai.ui.map.MapScreen
import com.resqlink.ai.ui.settings.SettingsScreen
import com.resqlink.ai.ui.theme.ResQLinkAITheme

/**
 * # ResQTab
 *
 * Bottom navigation destinations for ResQLink AI:
 * - [SOS]: The primary emergency cockpit, manual SOS triggers, and safe travel status.
 * - [MAP]: Offline 5km spatial radius radar and interactive station map.
 * - [DIRECTORY]: Verified Philippine police, fire, hospital, and barangay emergency hotlines.
 * - [CONTACTS]: In-Case-of-Emergency (ICE) family contact directory.
 */
enum class ResQTab(val title: String, val icon: ImageVector) {
    SOS("Emergency", Icons.Default.Warning),
    MAP("5km Map", Icons.Default.Map),
    DIRECTORY("Hotlines", Icons.Default.LocalHospital),
    CONTACTS("Contacts", Icons.Default.ContactPhone)
}

/**
 * # MainActivity
 *
 * Main application window entry point for ResQLink AI.
 *
 * Ordinary Human Overview:
 * When danger occurs or you cry out for help, seconds count.
 * This Activity:
 * 1. **Wakes Over Lockscreen**: Uses `setShowWhenLocked` and `setTurnScreenOn` so if your hands
 *    are pinned or you are in an accident, the emergency interface lights up immediately.
 * 2. **Requests Essential Safety Permissions**: Microphone (hands-free listening), GPS (5km defense radius),
 *    SMS (dispatching family alerts), and Phone (placing 911 calls).
 * 3. **Handles Incoming Hardware Alarms**: Intercepts emergency notification intents and voice wake events.
 * 4. **Coordinates Edge-to-Edge Jetpack Compose Navigation**: Smooth transitions between login, dashboard tabs,
 *    settings, and offline maps.
 */
class MainActivity : ComponentActivity() {

    private val emergencyViewModel: EmergencyViewModel by viewModels()

    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedPermissionsMap ->
        // Permissions evaluated
        emergencyViewModel.refreshLocation()
        checkAndStartGuardianService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen over lockscreen for hands-free emergency accessibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        requestRequiredPermissions()
        processIncomingIntent(intent)

        // Schedule 2-hour periodic background hotline AI discovery
        com.resqlink.ai.service.HotlineSyncWorker.schedulePeriodicSync(applicationContext)

        enableEdgeToEdge()
        setContent {
            ResQLinkAITheme {
                val userProfile by emergencyViewModel.userProfile.collectAsState()
                var currentActiveScreen by remember { mutableStateOf(AppScreen.TABS) }
                var targetSettingsSubSection by remember { mutableStateOf(com.resqlink.ai.ui.settings.SettingsSubSection.NONE) }

                AnimatedContent(
                    targetState = userProfile.isLoggedIn to currentActiveScreen,
                    transitionSpec = {
                        if (!initialState.first && targetState.first) {
                            // Login -> Dashboard
                            fadeIn(tween(400)) togetherWith fadeOut(tween(300))
                        } else if (targetState.second == AppScreen.SETTINGS) {
                            // Tabs -> Settings (Slide in from right)
                            (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                                    (slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(250)))
                        } else {
                            // Settings -> Tabs (Slide back to right)
                            (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350))) togetherWith
                                    (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(250)))
                        }
                    },
                    label = "screenTransition"
                ) { (isUserLoggedIn, activeScreen) ->
                    if (!isUserLoggedIn) {
                        LoginScreen(
                            onGoogleSignInClick = {
                                emergencyViewModel.signInWithGoogle(
                                    displayName = "User",
                                    email = ""
                                )
                            },
                            onGuestClick = {
                                emergencyViewModel.continueAsGuest()
                            }
                        )
                    } else {
                        when (activeScreen) {
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = emergencyViewModel,
                                    onBack = { currentActiveScreen = AppScreen.TABS },
                                    initialSubSection = targetSettingsSubSection
                                )
                            }
                            AppScreen.MAP -> {
                                MapScreen(
                                    viewModel = emergencyViewModel,
                                    onBack = { currentActiveScreen = AppScreen.TABS }
                                )
                            }
                            AppScreen.TABS -> {
                                MainAppScaffold(
                                    viewModel = emergencyViewModel,
                                    onOpenSettings = { subSection ->
                                        targetSettingsSubSection = subSection
                                        currentActiveScreen = AppScreen.SETTINGS
                                    },
                                    onOpenMap = {
                                        currentActiveScreen = AppScreen.MAP
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndStartGuardianService()
    }

    private fun checkAndStartGuardianService() {
        if (emergencyViewModel.isGuardianModeEnabled()) {
            val hasAudioRecordingPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasAudioRecordingPermission && !GuardianForegroundService.isRunning) {
                val serviceIntent = Intent(this, GuardianForegroundService::class.java).apply {
                    action = GuardianForegroundService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIncomingIntent(intent)
    }

    private fun processIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return

        if (incomingIntent.getBooleanExtra(GuardianForegroundService.EXTRA_CONFIRM_IMMEDIATELY, false)) {
            GuardianForegroundService.dismissEmergencyDistressPopup(this)
            emergencyViewModel.confirmEmergency("Emergency Confirmed via Notification")
            return
        }

        if (incomingIntent.getBooleanExtra(GuardianForegroundService.EXTRA_EMERGENCY_DETECTED, false)) {
            val spokenDistressPhrase = incomingIntent.getStringExtra(GuardianForegroundService.EXTRA_KEYWORD_LABEL) ?: "Distress Voice"
            if (spokenDistressPhrase.equals("Cancel", ignoreCase = true)) {
                GuardianForegroundService.dismissEmergencyDistressPopup(this)
                emergencyViewModel.cancelSos()
            } else {
                emergencyViewModel.onVoiceDistressDetected(spokenDistressPhrase)
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsList = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungrantedPermissions = permissionsList.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isNotEmpty()) {
            multiplePermissionsLauncher.launch(ungrantedPermissions.toTypedArray())
        }
    }
}

enum class AppScreen {
    TABS,
    SETTINGS,
    MAP
}

@Composable
fun MainAppScaffold(
    viewModel: EmergencyViewModel,
    onOpenSettings: (com.resqlink.ai.ui.settings.SettingsSubSection) -> Unit,
    onOpenMap: () -> Unit
) {
    var currentlySelectedTab by remember { mutableStateOf(ResQTab.SOS) }

    // Intercept physical phone back button or swipe-to-back gesture
    // When on Hotlines or Contacts tabs, back brings user to the main SOS dashboard tab instead of exiting the app
    BackHandler(enabled = currentlySelectedTab != ResQTab.SOS) {
        currentlySelectedTab = ResQTab.SOS
    }

    Scaffold(
        containerColor = NavyBackground,
        contentColor = TextPearl,
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                contentColor = TextPearl
            ) {
                ResQTab.values().forEach { tabItem ->
                    NavigationBarItem(
                        selected = currentlySelectedTab == tabItem,
                        onClick = { currentlySelectedTab = tabItem },
                        icon = { Icon(tabItem.icon, contentDescription = tabItem.title) },
                        label = {
                            Text(
                                tabItem.title,
                                fontWeight = if (currentlySelectedTab == tabItem) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = CrimsonGlow,
                            indicatorColor = CrimsonPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPaddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPaddingValues),
            color = NavyBackground
        ) {
            when (currentlySelectedTab) {
                ResQTab.SOS -> HomeScreen(
                    viewModel = viewModel,
                    onOpenSettings = onOpenSettings,
                    onOpenMap = { currentlySelectedTab = ResQTab.MAP }
                )
                ResQTab.MAP -> MapScreen(
                    viewModel = viewModel,
                    onBack = { currentlySelectedTab = ResQTab.SOS }
                )
                ResQTab.DIRECTORY -> DirectoryScreen(viewModel = viewModel)
                ResQTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
            }
        }
    }
}
