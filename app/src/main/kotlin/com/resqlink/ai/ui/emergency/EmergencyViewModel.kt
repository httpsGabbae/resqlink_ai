package com.resqlink.ai.ui.emergency

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.data.local.SessionManager
import com.resqlink.ai.data.location.LocationProvider
import com.resqlink.ai.data.repository.EmergencyRepositoryImpl
import com.resqlink.ai.data.telephony.CallManager
import com.resqlink.ai.data.telephony.DetectedSim
import com.resqlink.ai.data.telephony.SmsDispatcher
import com.resqlink.ai.domain.model.CallPriority
import com.resqlink.ai.domain.model.EmergencyState
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.domain.model.SimPreference
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.emergency.EmergencyEventManager
import com.resqlink.ai.guides.EmergencyGuide
import com.resqlink.ai.guides.EmergencyGuideRepository
import com.resqlink.ai.guides.GuideCategory
import com.resqlink.ai.guides.GuideLanguage
import com.resqlink.ai.incidents.IncidentExporter
import com.resqlink.ai.incidents.IncidentRecord
import com.resqlink.ai.incidents.IncidentRepository
import com.resqlink.ai.motion.MotionDistressDetector
import com.resqlink.ai.readiness.DiagnosticItem
import com.resqlink.ai.readiness.EmergencySimulationManager
import com.resqlink.ai.readiness.ReadinessCheckManager
import com.resqlink.ai.readiness.ReadinessReport
import com.resqlink.ai.rescuecard.RescueCardRepository
import com.resqlink.ai.rescuecard.RescueProfile
import com.resqlink.ai.responder.ResponderCoordinator
import com.resqlink.ai.safetyjourney.JourneyScheduler
import com.resqlink.ai.safetyjourney.JourneyStatus
import com.resqlink.ai.safetyjourney.SafetyJourney
import com.resqlink.ai.safetyjourney.SafetyJourneyRepository
import com.resqlink.ai.service.CrashTelemetryProvider
import com.resqlink.ai.service.DispatcherQuestionClassifier
import com.resqlink.ai.service.DispatcherResponseSynthesizer
import com.resqlink.ai.service.GuardianForegroundService
import com.resqlink.ai.service.SpeechHandoverDetector
import com.resqlink.ai.service.VoiceAssistManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * # EmergencyViewModel
 *
 * Central architecture coordinator and state holder powering ResQLink AI's UI screens.
 *
 * Ordinary Human Overview:
 * When you press the big red SOS button, or when your phone hears "Resqlink Tulong!" in Guardian Mode,
 * this ViewModel coordinates the entire response:
 * 1. **Immediate Feedback**: Starts loud emergency sirens and continuous vibration to alert bystanders.
 * 2. **10-Second Countdown**: Gives you a grace period to cancel accidental false alarms.
 * 3. **GPS Location**: Fetches your real-time coordinates and matches the closest emergency hotlines within 5km.
 * 4. **SMS Broadcast**: Sends multi-part SMS messages with Google Maps links to your authorized ICE contacts.
 * 5. **Phone Dialing**: Launches emergency calls according to your SIM and Call Priority settings (Family first, 911, or Local DRRMO).
 * 6. **In-Call Voice Assist**: If you are unresponsive, AI speaks directly to the 911 dispatcher on speakerphone.
 */
class EmergencyViewModel(application: Application) : AndroidViewModel(application) {

    private val applicationContext: Context get() = getApplication<Application>().applicationContext

    // Core Data & Telephony Services
    private val databaseHelper = HotlineDatabaseHelper(applicationContext)
    private val emergencyRepository = EmergencyRepositoryImpl(databaseHelper)
    private val locationProvider = LocationProvider(applicationContext)
    private val callManager = CallManager(applicationContext)
    private val smsDispatcher = SmsDispatcher(applicationContext)
    private val sessionManager = SessionManager(applicationContext)

    // Voice Assist & Kinematics Subsystems
    private val voiceAssistManager = VoiceAssistManager(applicationContext)
    private val crashTelemetryProvider = CrashTelemetryProvider(applicationContext)
    private val dispatcherQuestionClassifier = DispatcherQuestionClassifier()
    private val dispatcherResponseSynthesizer = DispatcherResponseSynthesizer()
    private var speechHandoverDetector: SpeechHandoverDetector? = null

    // Master Feature Expansion Components
    val emergencyEventManager = EmergencyEventManager.getInstance()
    val activeIncident = emergencyEventManager.currentIncident

    private val safetyJourneyRepository = SafetyJourneyRepository.getInstance(applicationContext)
    val activeSafetyJourney = safetyJourneyRepository.activeJourney
    val safetyJourneyHistory = safetyJourneyRepository.journeyHistory
    private val journeyScheduler = JourneyScheduler(applicationContext)

    private val rescueCardRepository = RescueCardRepository.getInstance(applicationContext)
    val rescueProfile = rescueCardRepository.profile

    val readinessCheckManager = ReadinessCheckManager(applicationContext, databaseHelper)
    val simulationManager = EmergencySimulationManager(emergencyEventManager)
    val simulationState = simulationManager.simulationState

    val responderCoordinator = ResponderCoordinator(applicationContext, viewModelScope)
    val responderState = responderCoordinator.responderState

    val incidentRepo = IncidentRepository.getInstance(applicationContext)
    val incidentRecords = incidentRepo.incidents

    val motionDetector = MotionDistressDetector(applicationContext, viewModelScope)

    val emergencyGuides = EmergencyGuideRepository.getAllGuides()

    val userProfile = sessionManager.userProfile

    // Observable StateFlows for UI
    private val _emergencyState = MutableStateFlow<EmergencyState>(EmergencyState.Idle)
    val emergencyState = _emergencyState.asStateFlow()

    private val _allHotlines = MutableStateFlow<List<Hotline>>(emptyList())
    val allHotlines = _allHotlines.asStateFlow()

    private val _trustedContacts = MutableStateFlow<List<TrustedContact>>(emptyList())
    val trustedContacts = _trustedContacts.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationSnapshot?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _nearestHotlines = MutableStateFlow<List<Hotline>>(emptyList())
    val nearestHotlines = _nearestHotlines.asStateFlow()

    private val _isAiScanning = MutableStateFlow(false)
    val isAiScanning = _isAiScanning.asStateFlow()

    private val _lastAiScanTime = MutableStateFlow(sessionManager.getLastAiScanTime())
    val lastAiScanTime = _lastAiScanTime.asStateFlow()

    // Safe Test / Demo Mode (disabled by default so real emergency calling works according to user's route)
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode = _isDemoMode.asStateFlow()

    private var countdownCoroutineJob: Job? = null
    private var alarmMediaPlayer: MediaPlayer? = null

    init {
        if (sessionManager.isExperimentalMotionDetectionEnabled()) {
            motionDetector.startMonitoring()
        }
        motionDetector.onImpactDistressDetected = { peakGForce ->
            onVoiceDistressDetected("Experimental Motion Impact ($peakGForce m/s²)")
        }

        viewModelScope.launch {
            incidentRepo.refresh()
        }

        voiceAssistManager.onTranscriptUpdate = { transcript, cycle ->
            val currentState = _emergencyState.value
            if (currentState is EmergencyState.VoiceAssistActive) {
                _emergencyState.value = currentState.copy(
                    liveTranscript = transcript,
                    cycleCount = cycle
                )
            }
        }

        loadData()
        refreshLocation()
    }

    // ==========================================
    // AUTHENTICATION & USER PROFILE
    // ==========================================

    fun signInWithGoogle(displayName: String, email: String, photoUrl: String? = null) {
        sessionManager.saveGoogleSession(displayName, email, photoUrl)
    }

    fun continueAsGuest() {
        sessionManager.continueAsGuest()
    }

    fun logout() {
        sessionManager.logout()
    }

    fun updateMedicalProfile(
        displayName: String,
        bloodType: String,
        allergies: String,
        medicalNotes: String,
        primaryContactName: String,
        primaryContactPhone: String
    ) {
        sessionManager.updateMedicalProfile(
            displayName,
            bloodType,
            allergies,
            medicalNotes,
            primaryContactName,
            primaryContactPhone
        )
    }

    fun updateCallPriority(callPriority: CallPriority) {
        sessionManager.updateCallPriority(callPriority)
    }

    fun updateSimPreference(simPreference: SimPreference) {
        sessionManager.updateSimPreference(simPreference)
    }

    fun getDetectedSims(): List<DetectedSim> {
        return callManager.getDetectedSims()
    }

    fun toggleDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    // ==========================================
    // API KEYS CONFIGURATION
    // ==========================================

    fun getGeminiApiKey(): String = sessionManager.getGeminiApiKey()

    fun setGeminiApiKey(key: String) {
        sessionManager.setGeminiApiKey(key)
    }

    fun getElevenLabsApiKey(): String = sessionManager.getElevenLabsApiKey()

    fun setElevenLabsApiKey(key: String) {
        sessionManager.setElevenLabsApiKey(key)
    }

    fun getElevenLabsVoiceId(): String = sessionManager.getElevenLabsVoiceId()

    fun setElevenLabsVoiceId(voiceId: String) {
        sessionManager.setElevenLabsVoiceId(voiceId)
    }

    // ==========================================
    // DATA LOADING & GPS REFRESH
    // ==========================================

    fun loadData() {
        viewModelScope.launch {
            _allHotlines.value = emergencyRepository.getAllHotlines()
            _trustedContacts.value = emergencyRepository.getTrustedContacts()
        }
    }

    fun refreshLocation(autoAiScan: Boolean = true) {
        viewModelScope.launch {
            val freshLocationSnapshot = locationProvider.getCurrentOrCachedLocation(timeoutMs = 4000L)
            _currentLocation.value = freshLocationSnapshot
            if (freshLocationSnapshot != null) {
                val currentNearbyStations = emergencyRepository.getNearestHotlines(
                    freshLocationSnapshot.latitude,
                    freshLocationSnapshot.longitude,
                    radiusKm = 2.5
                )
                _nearestHotlines.value = currentNearbyStations

                val hasAiDiscoveredStation = currentNearbyStations.any { it.source == "AI_GEMINI" && (it.distanceKm ?: 99.0) <= 6.5 }
                val lastScanTimestamp = sessionManager.getLastAiScanTime()
                val isScanDataStale = (System.currentTimeMillis() - lastScanTimestamp) > (30 * 60 * 1000L)

                if (autoAiScan && (!hasAiDiscoveredStation || isScanDataStale)) {
                    triggerAiHotlineScan()
                }
            } else {
                _nearestHotlines.value = listOf(emergencyRepository.getUniversalFallback())
            }
        }
    }

    fun triggerAiHotlineScan() {
        if (_isAiScanning.value) return
        viewModelScope.launch {
            _isAiScanning.value = true
            try {
                val targetLocation = _currentLocation.value ?: locationProvider.getCurrentOrCachedLocation()
                if (targetLocation != null) {
                    val hotlineAiService = com.resqlink.ai.ai.HotlineAiService()
                    val apiKey = sessionManager.getGeminiApiKey()
                    val discoveredHotlinesList = hotlineAiService.discover5kmHotlines(
                        latitude = targetLocation.latitude,
                        longitude = targetLocation.longitude,
                        resolvedCity = targetLocation.address,
                        apiKey = apiKey
                    )
                    if (discoveredHotlinesList.isNotEmpty()) {
                        emergencyRepository.upsertHotlines(discoveredHotlinesList)
                        val currentTimeMillis = System.currentTimeMillis()
                        sessionManager.setLastAiScanTime(currentTimeMillis)
                        _lastAiScanTime.value = currentTimeMillis
                    }
                    _nearestHotlines.value = emergencyRepository.getNearestHotlines(targetLocation.latitude, targetLocation.longitude, radiusKm = 2.5)
                    _allHotlines.value = emergencyRepository.getAllHotlines()
                }
            } catch (exception: Exception) {
                Log.e("EmergencyViewModel", "Manual AI hotline scan error: ${exception.message}", exception)
            } finally {
                _isAiScanning.value = false
            }
        }
    }

    fun togglePinHotline(hotline: Hotline) {
        viewModelScope.launch {
            emergencyRepository.setPinnedHotline(hotline.id, !hotline.isPinned)
            val currentLocationSnapshot = _currentLocation.value
            if (currentLocationSnapshot != null) {
                _nearestHotlines.value = emergencyRepository.getNearestHotlines(currentLocationSnapshot.latitude, currentLocationSnapshot.longitude, radiusKm = 2.5)
            }
            _allHotlines.value = emergencyRepository.getAllHotlines()
        }
    }

    fun directCallHotline(phoneNumber: String) {
        callManager.dialFamilyMember(phoneNumber, userProfile.value.simPreference)
    }

    fun dialHotlineDirectly(hotline: Hotline) {
        callManager.initiateEmergencyCall(hotline, userProfile.value.simPreference)
    }

    /**
     * Resolves the target name and phone number dynamically based on user's active CallPriority.
     */
    fun resolveCallTarget(): Pair<String, String> {
        val currentProfile = userProfile.value
        val nearestHotlineStation = _nearestHotlines.value.firstOrNull() ?: databaseHelper.getUniversalFallback911()
        return when (currentProfile.callPriority) {
            CallPriority.FAMILY_FIRST -> {
                val familyContactName = currentProfile.primaryContactName.ifBlank { "Family (ICE)" }
                val familyPhoneNumber = currentProfile.primaryContactPhone.ifBlank { nearestHotlineStation.primaryNumber }
                Pair(familyContactName, familyPhoneNumber)
            }
            CallPriority.UNIVERSAL_911 -> {
                Pair("Universal 911 Dispatch", "911")
            }
            CallPriority.LOCAL_HOTLINE -> {
                val localStationName = nearestHotlineStation.name.ifBlank { "Local Emergency Station" }
                val localStationPhone = nearestHotlineStation.primaryNumber.ifBlank { "122" }
                Pair(localStationName, localStationPhone)
            }
        }
    }

    // ==========================================
    // GUARDIAN MODE PERSISTENT BACKGROUND LISTENER
    // ==========================================

    fun isGuardianModeEnabled(): Boolean = sessionManager.isGuardianModeEnabled()

    fun toggleGuardianMode() {
        val willEnableGuardian = !sessionManager.isGuardianModeEnabled()
        sessionManager.setGuardianModeEnabled(willEnableGuardian)

        val serviceIntent = Intent(applicationContext, GuardianForegroundService::class.java)
        if (!willEnableGuardian) {
            serviceIntent.action = GuardianForegroundService.ACTION_STOP
            applicationContext.startService(serviceIntent)
            _emergencyState.value = EmergencyState.Idle
        } else {
            serviceIntent.action = GuardianForegroundService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
            _emergencyState.value = EmergencyState.GuardianActive(isMonitoring = true)
        }
    }

    // ==========================================
    // EMERGENCY TRIGGER DISPATCH & COUNTDOWNS
    // ==========================================

    /**
     * Triggered by Voice SOS (Distress keyword detected by GuardianForegroundService)
     */
    fun onVoiceDistressDetected(phrase: String) {
        countdownCoroutineJob?.cancel()
        startContinuousEmergencyAlarm()

        val locationSnapshot = _currentLocation.value
        val nearestStation = _nearestHotlines.value.firstOrNull() ?: databaseHelper.getUniversalFallback911()
        val (targetName, targetNumber) = resolveCallTarget()

        _emergencyState.value = EmergencyState.ConfirmationPending(
            detectedPhrase = phrase,
            remainingSeconds = 10,
            location = locationSnapshot,
            nearestHotline = nearestStation,
            callPriority = userProfile.value.callPriority,
            targetName = targetName,
            targetNumber = targetNumber
        )

        countdownCoroutineJob = viewModelScope.launch {
            for (secondsRemaining in 9 downTo 1) {
                delay(1000L)
                val currentState = _emergencyState.value
                if (currentState is EmergencyState.ConfirmationPending) {
                    _emergencyState.value = currentState.copy(remainingSeconds = secondsRemaining)
                } else {
                    return@launch
                }
            }

            delay(1000L)
            // 10 seconds expired with NO RESPONSE: Incapacitation Escalation
            handleTimeoutIncapacitation()
        }
    }

    /**
     * Triggered by One-Tap SOS (Large Red Button)
     */
    fun onManualSosTapped() {
        countdownCoroutineJob?.cancel()
        startContinuousEmergencyAlarm()

        val locationSnapshot = _currentLocation.value
        val nearestStation = _nearestHotlines.value.firstOrNull() ?: databaseHelper.getUniversalFallback911()
        val (targetName, targetNumber) = resolveCallTarget()

        _emergencyState.value = EmergencyState.ManualSosGracePeriod(
            remainingSeconds = 3,
            location = locationSnapshot,
            nearestHotline = nearestStation,
            callPriority = userProfile.value.callPriority,
            targetName = targetName,
            targetNumber = targetNumber
        )

        countdownCoroutineJob = viewModelScope.launch {
            for (secondsRemaining in 2 downTo 1) {
                delay(1000L)
                val currentState = _emergencyState.value
                if (currentState is EmergencyState.ManualSosGracePeriod) {
                    _emergencyState.value = currentState.copy(remainingSeconds = secondsRemaining)
                } else {
                    return@launch
                }
            }
            delay(1000L)
            // 3-second grace elapsed without cancellation -> execute confirmed emergency call
            confirmEmergency(reason = "Manual One-Tap SOS")
        }
    }

    fun confirmEmergency(reason: String = "User Confirmed SOS") {
        countdownCoroutineJob?.cancel()
        stopContinuousEmergencyAlarm()
        GuardianForegroundService.dismissEmergencyDistressPopup(applicationContext)

        viewModelScope.launch {
            val locationSnapshot = _currentLocation.value ?: locationProvider.getCurrentOrCachedLocation(timeoutMs = 2000L)
            val nearestHotline = _nearestHotlines.value.firstOrNull() ?: emergencyRepository.getUniversalFallback()
            val contactsList = _trustedContacts.value
            val currentProfile = userProfile.value
            val (targetName, targetNumber) = resolveCallTarget()

            val effectiveReason = if (_isDemoMode.value) "[DEMO SIMULATION] $reason" else reason

            val successfullySentSmsCount = if (_isDemoMode.value) {
                contactsList.count { it.isAuthorizedForSafetyCheck }
            } else {
                smsDispatcher.dispatchUnresponsiveAlerts(
                    contacts = contactsList,
                    location = locationSnapshot,
                    nearestHotline = nearestHotline
                )
            }

            // Immediately initiate call according to user's configured CallPriority & Outgoing SIM Preference!
            when (currentProfile.callPriority) {
                CallPriority.FAMILY_FIRST -> {
                    val familyPhoneNumber = currentProfile.primaryContactPhone
                    if (familyPhoneNumber.isNotBlank()) {
                        callManager.dialFamilyMember(familyPhoneNumber, currentProfile.simPreference)
                    } else {
                        callManager.initiateEmergencyCall(nearestHotline, currentProfile.simPreference)
                    }
                }
                CallPriority.UNIVERSAL_911 -> {
                    callManager.dial911(currentProfile.simPreference)
                }
                CallPriority.LOCAL_HOTLINE -> {
                    callManager.initiateEmergencyCall(nearestHotline, currentProfile.simPreference)
                }
            }

            _emergencyState.value = EmergencyState.EmergencyConfirmed(
                reason = effectiveReason,
                callPriority = currentProfile.callPriority,
                targetName = targetName,
                targetNumber = targetNumber,
                dialedHotline = nearestHotline,
                smsSentCount = successfullySentSmsCount,
                location = locationSnapshot
            )
        }
    }

    /**
     * Re-dials the active priority target directly with user's SIM Preference.
     */
    fun executeConfiguredCall() {
        val currentProfile = userProfile.value
        val nearestHotline = _nearestHotlines.value.firstOrNull() ?: databaseHelper.getUniversalFallback911()
        when (currentProfile.callPriority) {
            CallPriority.FAMILY_FIRST -> {
                val familyPhone = currentProfile.primaryContactPhone
                if (familyPhone.isNotBlank()) {
                    callManager.dialFamilyMember(familyPhone, currentProfile.simPreference)
                } else {
                    callManager.initiateEmergencyCall(nearestHotline, currentProfile.simPreference)
                }
            }
            CallPriority.UNIVERSAL_911 -> {
                callManager.dial911(currentProfile.simPreference)
            }
            CallPriority.LOCAL_HOTLINE -> {
                callManager.initiateEmergencyCall(nearestHotline, currentProfile.simPreference)
            }
        }
    }

    /**
     * Activates Voice Assist when the user cannot speak (e.g. accident, unconscious, trauma).
     * Automatically dials the emergency target, activates speakerphone, and broadcasts
     * patient vitals, GPS coordinates, and medical alerts.
     */
    fun startAccidentVoiceAssist(reason: String = "Accident Incapacitation") {
        countdownCoroutineJob?.cancel()
        stopContinuousEmergencyAlarm()
        GuardianForegroundService.dismissEmergencyDistressPopup(applicationContext)

        viewModelScope.launch {
            val locationSnapshot = _currentLocation.value ?: locationProvider.getCurrentOrCachedLocation(timeoutMs = 2000L)
            val nearestHotline = _nearestHotlines.value.firstOrNull() ?: emergencyRepository.getUniversalFallback()
            val contactsList = _trustedContacts.value
            val currentProfile = userProfile.value
            val (targetName, targetNumber) = resolveCallTarget()

            // 1. Dispatch SMS alert with GPS to pre-authorized safety contacts
            if (!_isDemoMode.value) {
                smsDispatcher.dispatchUnresponsiveAlerts(
                    contacts = contactsList,
                    location = locationSnapshot,
                    nearestHotline = nearestHotline
                )
            }

            // 2. Initiate cellular call according to user's CallPriority & SIM Preference
            executeConfiguredCall()

            // 3. Update UI state to VoiceAssistActive HUD
            _emergencyState.value = EmergencyState.VoiceAssistActive(
                targetName = targetName,
                targetNumber = targetNumber,
                liveTranscript = "Initializing Voice Assist broadcast...",
                cycleCount = 1,
                isHandoverComplete = false,
                location = locationSnapshot
            )

            // 4. Start speech recognition in background to listen for user consciousness
            speechHandoverDetector?.destroy()
            speechHandoverDetector = SpeechHandoverDetector(applicationContext) { heardSpeech ->
                performHandoverToUser(heardSpeech)
            }
            speechHandoverDetector?.startListening()

            // 5. Start Voice Assist emergency broadcast on speakerphone (ElevenLabs with Offline TTS fallback)
            val streetAddress = locationSnapshot?.address?.ifBlank { null } ?: "Metro Manila vicinity"
            voiceAssistManager.startEmergencyBroadcast(
                userName = currentProfile.displayName,
                address = streetAddress,
                latitude = locationSnapshot?.latitude ?: 14.6507,
                longitude = locationSnapshot?.longitude ?: 121.0509,
                bloodType = currentProfile.bloodType,
                allergies = currentProfile.allergies,
                elevenLabsApiKey = sessionManager.getElevenLabsApiKey(),
                elevenLabsVoiceId = sessionManager.getElevenLabsVoiceId(),
                geminiApiKey = sessionManager.getGeminiVoiceApiKey(),
                geminiVoiceName = sessionManager.getGeminiVoiceName()
            )
        }
    }

    /**
     * Hands over the call to the patient when coherent speech is detected or manual button is tapped.
     */
    fun performHandoverToUser(userWords: String? = null) {
        val currentState = _emergencyState.value
        if (currentState is EmergencyState.VoiceAssistActive && !currentState.isHandoverComplete) {
            speechHandoverDetector?.stopListening()
            voiceAssistManager.performHandover(
                elevenLabsApiKey = sessionManager.getElevenLabsApiKey(),
                elevenLabsVoiceId = sessionManager.getElevenLabsVoiceId()
            ) {
                val updatedState = _emergencyState.value
                if (updatedState is EmergencyState.VoiceAssistActive) {
                    _emergencyState.value = updatedState.copy(
                        isHandoverComplete = true,
                        detectedUserWords = userWords,
                        liveTranscript = "[Handover complete. Patient speaking live with dispatcher.]"
                    )
                }
            }
        }
    }

    /**
     * Answers a follow-up inquiry from the emergency dispatcher using the dual-tier SLM engine.
     * Grounded strictly in real-time crash telemetry and verified medical profile.
     * In-person physical scene questions return the strict unconfirmed safety response.
     */
    fun answerDispatcherQuestion(question: String) {
        val currentState = _emergencyState.value
        if (currentState is EmergencyState.VoiceAssistActive && !currentState.isHandoverComplete) {
            val crashTelemetrySnapshot = crashTelemetryProvider.getSnapshot()
            val currentProfile = userProfile.value
            val classifiedIntent = dispatcherQuestionClassifier.classifyQuestion(question)
            val synthesizedAnswer = dispatcherResponseSynthesizer.synthesizeResponse(
                intent = classifiedIntent,
                telemetry = crashTelemetrySnapshot,
                userProfile = currentProfile,
                location = currentState.location
            )

            _emergencyState.value = currentState.copy(
                lastDispatcherQuestion = question,
                lastAiAnswer = synthesizedAnswer,
                liveTranscript = "Dispatcher: \"$question\"\nResQLink AI: \"$synthesizedAnswer\""
            )

            voiceAssistManager.speakDispatcherAnswer(
                answerText = synthesizedAnswer,
                elevenLabsApiKey = sessionManager.getElevenLabsApiKey(),
                elevenLabsVoiceId = sessionManager.getElevenLabsVoiceId(),
                geminiApiKey = sessionManager.getGeminiVoiceApiKey(),
                geminiVoiceName = sessionManager.getGeminiVoiceName()
            )
        }
    }

    /**
     * Ends the Voice Assist emergency call and resets audio modes.
     */
    fun endEmergencyCall() {
        stopContinuousEmergencyAlarm()
        GuardianForegroundService.dismissEmergencyDistressPopup(applicationContext)
        voiceAssistManager.stop()
        speechHandoverDetector?.stopListening()
        _emergencyState.value = if (GuardianForegroundService.isRunning) {
            EmergencyState.GuardianActive(isMonitoring = true)
        } else {
            EmergencyState.Idle
        }
    }

    private fun handleTimeoutIncapacitation() {
        // Automatically engage Voice Assist when 10-second countdown expires with no user response!
        startAccidentVoiceAssist("Accident Incapacitation: 10s Unresponsive Timeout")
    }

    fun cancelSos() {
        countdownCoroutineJob?.cancel()
        stopContinuousEmergencyAlarm()
        GuardianForegroundService.dismissEmergencyDistressPopup(applicationContext)
        voiceAssistManager.stop()
        speechHandoverDetector?.stopListening()
        _emergencyState.value = if (GuardianForegroundService.isRunning) {
            EmergencyState.GuardianActive(isMonitoring = true)
        } else {
            EmergencyState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownCoroutineJob?.cancel()
        stopContinuousEmergencyAlarm()
        voiceAssistManager.release()
        speechHandoverDetector?.destroy()
        crashTelemetryProvider.stop()
        motionDetector.stopMonitoring()
    }

    // ==========================================
    // TRUSTED EMERGENCY CONTACTS
    // ==========================================

    fun addTrustedContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            emergencyRepository.saveTrustedContact(
                TrustedContact(
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship,
                    isAuthorizedForSafetyCheck = true
                )
            )
            _trustedContacts.value = emergencyRepository.getTrustedContacts()
        }
    }

    fun removeTrustedContact(id: Long) {
        viewModelScope.launch {
            emergencyRepository.deleteTrustedContact(id)
            _trustedContacts.value = emergencyRepository.getTrustedContacts()
        }
    }

    // ==========================================
    // CONTINUOUS AUDITORY & HAPTIC ALARMS
    // ==========================================

    /**
     * Starts continuous repeating ringing vibration (800ms on, 400ms off) and
     * loud emergency alarm siren audio (USAGE_ALARM to bypass silent/DND mode).
     */
    fun startContinuousEmergencyAlarm() {
        // 1. Continuous Repeating Vibration (repeat = 0 loops indefinitely)
        try {
            val vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(vibrationPattern, 0)
                }
            }
        } catch (exception: Exception) {
            Log.e("EmergencyViewModel", "Continuous vibration error: ${exception.message}")
        }

        // 2. Loud Emergency Alarm Siren (bypasses DND with USAGE_ALARM)
        try {
            stopAlarmSound()
            val alarmSoundRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            alarmMediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmSoundRingtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (exception: Exception) {
            Log.e("EmergencyViewModel", "Alarm siren audio error: ${exception.message}")
        }
    }

    /**
     * Immediately stops the continuous vibration and cancels the emergency alarm audio.
     */
    fun stopContinuousEmergencyAlarm() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (ignored: Exception) {}
        stopAlarmSound()
    }

    private fun stopAlarmSound() {
        try {
            alarmMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (ignored: Exception) {
        } finally {
            alarmMediaPlayer = null
        }
    }

    // ==========================================
    // SAFETY JOURNEY CONTROLLER
    // ==========================================

    fun startSafetyJourney(
        destination: String,
        durationMinutes: Int,
        gracePeriodMinutes: Int = 5,
        notes: String = ""
    ): SafetyJourney {
        return journeyScheduler.startJourney(
            destination = destination,
            durationMinutes = durationMinutes,
            gracePeriodMinutes = gracePeriodMinutes,
            notes = notes
        )
    }

    fun extendSafetyJourney(additionalMinutes: Int = 15): SafetyJourney? {
        return journeyScheduler.extendJourney(additionalMinutes)
    }

    fun checkInSafetyJourneySafe(): SafetyJourney? {
        val currentJourney = safetyJourneyRepository.activeJourney.value ?: return null
        journeyScheduler.cancelAllAlarms()
        val safeJourney = currentJourney.copy(
            status = JourneyStatus.RESOLVED_SAFE,
            completedTimeMillis = System.currentTimeMillis()
        )
        safetyJourneyRepository.archiveJourney(safeJourney)
        return safeJourney
    }

    fun cancelSafetyJourney(): SafetyJourney? {
        val currentJourney = safetyJourneyRepository.activeJourney.value ?: return null
        journeyScheduler.cancelAllAlarms()
        val cancelledJourney = currentJourney.copy(
            status = JourneyStatus.CANCELLED,
            completedTimeMillis = System.currentTimeMillis()
        )
        safetyJourneyRepository.archiveJourney(cancelledJourney)
        return cancelledJourney
    }

    // ==========================================
    // RESCUE CARD PROFILE CONTROLLER
    // ==========================================

    fun saveRescueProfile(profile: RescueProfile) {
        rescueCardRepository.saveProfile(profile)
    }

    fun updateRescueCardVisibility(
        isBloodTypeVisible: Boolean? = null,
        isAllergiesVisible: Boolean? = null,
        isMedicationsVisible: Boolean? = null,
        isConditionsVisible: Boolean? = null,
        isInstructionsVisible: Boolean? = null,
        isContactsVisible: Boolean? = null,
        isOrganDonorVisible: Boolean? = null
    ) {
        rescueCardRepository.updateVisibility(
            isBloodTypeVisible,
            isAllergiesVisible,
            isMedicationsVisible,
            isConditionsVisible,
            isInstructionsVisible,
            isContactsVisible,
            isOrganDonorVisible
        )
    }

    fun resetRescueCardToDefault() {
        rescueCardRepository.resetToDefault()
    }

    // ==========================================
    // READINESS CHECK & SAFE SIMULATION
    // ==========================================

    fun runReadinessCheck(): ReadinessReport {
        return readinessCheckManager.runDiagnostics()
    }

    fun startSafeSimulation() {
        simulationManager.startSimulation()
    }

    fun cancelSafeSimulation() {
        simulationManager.stopSimulation()
    }

    // ==========================================
    // INCIDENT LOGS & SAF EXPORT
    // ==========================================

    fun refreshIncidentLogs() {
        viewModelScope.launch {
            incidentRepo.refresh()
        }
    }

    fun exportIncidentReport(record: IncidentRecord, asJson: Boolean = false) {
        IncidentExporter.shareReport(applicationContext, record, asJson)
    }

    fun deleteIncidentLog(incidentId: String) {
        viewModelScope.launch {
            incidentRepo.deleteIncident(incidentId)
        }
    }

    fun clearAllIncidentLogs() {
        viewModelScope.launch {
            incidentRepo.clearAll()
        }
    }

    // ==========================================
    // EXPERIMENTAL MOTION DETECTION
    // ==========================================

    fun isExperimentalMotionDetectionEnabled(): Boolean {
        return sessionManager.isExperimentalMotionDetectionEnabled()
    }

    fun setExperimentalMotionDetectionEnabled(enabled: Boolean) {
        sessionManager.setExperimentalMotionDetectionEnabled(enabled)
        if (enabled) {
            motionDetector.startMonitoring()
        } else {
            motionDetector.stopMonitoring()
        }
    }

    // ==========================================
    // OFFLINE EMERGENCY GUIDES
    // ==========================================

    fun getGuidesByLanguage(language: GuideLanguage): List<EmergencyGuide> {
        return EmergencyGuideRepository.getGuidesByLanguage(language)
    }

    fun getGuideByCategory(category: GuideCategory, language: GuideLanguage = GuideLanguage.ENGLISH): EmergencyGuide? {
        return EmergencyGuideRepository.getGuidesByCategory(category, language)
    }

    fun searchGuides(query: String, language: GuideLanguage? = null): List<EmergencyGuide> {
        return EmergencyGuideRepository.searchGuides(query, language)
    }
}
