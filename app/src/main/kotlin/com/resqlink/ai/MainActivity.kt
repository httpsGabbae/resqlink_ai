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

enum class ResQTab(val title: String, val icon: ImageVector) {
    SOS("Emergency", Icons.Default.Warning),
    MAP("5km Map", Icons.Default.Map),
    DIRECTORY("Hotlines", Icons.Default.LocalHospital),
    CONTACTS("Contacts", Icons.Default.ContactPhone)
}

class MainActivity : ComponentActivity() {

    private val emergencyViewModel: EmergencyViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
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

        requestAppPermissions()
        handleIncomingIntent(intent)

        // Schedule 2-hour periodic background hotline AI discovery
        com.resqlink.ai.service.HotlineSyncWorker.schedulePeriodicSync(applicationContext)

        enableEdgeToEdge()
        setContent {
            ResQLinkAITheme {
                val userProfile by emergencyViewModel.userProfile.collectAsState()
                var currentScreen by remember { mutableStateOf(AppScreen.TABS) }
                var targetSettingsSubSection by remember { mutableStateOf(com.resqlink.ai.ui.settings.SettingsSubSection.NONE) }

                AnimatedContent(
                    targetState = userProfile.isLoggedIn to currentScreen,
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
                ) { (isLoggedIn, screen) ->
                    if (!isLoggedIn) {
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
                        when (screen) {
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = emergencyViewModel,
                                    onBack = { currentScreen = AppScreen.TABS },
                                    initialSubSection = targetSettingsSubSection
                                )
                            }
                            AppScreen.MAP -> {
                                MapScreen(
                                    viewModel = emergencyViewModel,
                                    onBack = { currentScreen = AppScreen.TABS }
                                )
                            }
                            AppScreen.TABS -> {
                                MainAppScaffold(
                                    viewModel = emergencyViewModel,
                                    onOpenSettings = { sub ->
                                        targetSettingsSubSection = sub
                                        currentScreen = AppScreen.SETTINGS
                                    },
                                    onOpenMap = {
                                        currentScreen = AppScreen.MAP
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
            val hasAudio = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasAudio && !GuardianForegroundService.isRunning) {
                val intent = Intent(this, GuardianForegroundService::class.java).apply {
                    action = GuardianForegroundService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra(GuardianForegroundService.EXTRA_CONFIRM_IMMEDIATELY, false)) {
            GuardianForegroundService.dismissEmergencyDistressPopup(this)
            emergencyViewModel.confirmEmergency("Emergency Confirmed via Notification")
            return
        }

        if (intent.getBooleanExtra(GuardianForegroundService.EXTRA_EMERGENCY_DETECTED, false)) {
            val phrase = intent.getStringExtra(GuardianForegroundService.EXTRA_KEYWORD_LABEL) ?: "Distress Voice"
            if (phrase.equals("Cancel", ignoreCase = true)) {
                GuardianForegroundService.dismissEmergencyDistressPopup(this)
                emergencyViewModel.cancelSos()
            } else {
                emergencyViewModel.onVoiceDistressDetected(phrase)
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
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
    var selectedTab by remember { mutableStateOf(ResQTab.SOS) }

    // Intercept physical phone back button or swipe-to-back gesture
    // When on Hotlines or Contacts tabs, back brings user to the main SOS dashboard tab instead of exiting the app
    BackHandler(enabled = selectedTab != ResQTab.SOS) {
        selectedTab = ResQTab.SOS
    }

    Scaffold(
        containerColor = NavyBackground,
        contentColor = TextPearl,
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                contentColor = TextPearl
            ) {
                ResQTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = {
                            Text(
                                tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
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
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = NavyBackground
        ) {
            when (selectedTab) {
                ResQTab.SOS -> HomeScreen(
                    viewModel = viewModel,
                    onOpenSettings = onOpenSettings,
                    onOpenMap = { selectedTab = ResQTab.MAP }
                )
                ResQTab.MAP -> MapScreen(
                    viewModel = viewModel,
                    onBack = { selectedTab = ResQTab.SOS }
                )
                ResQTab.DIRECTORY -> DirectoryScreen(viewModel = viewModel)
                ResQTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
            }
        }
    }
}
