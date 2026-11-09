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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.data.location.LocationProvider
import com.resqlink.ai.data.repository.EmergencyRepositoryImpl
import com.resqlink.ai.data.telephony.CallManager
import com.resqlink.ai.data.telephony.SmsDispatcher
import com.resqlink.ai.domain.model.EmergencyState
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.emergency.EmergencyEventManager
import com.resqlink.ai.emergency.EmergencyTriggerSource
import com.resqlink.ai.emergency.IncidentLifecycleState
import com.resqlink.ai.guides.EmergencyGuide
import com.resqlink.ai.guides.EmergencyGuideRepository
import com.resqlink.ai.guides.GuideCategory
import com.resqlink.ai.guides.GuideLanguage
import com.resqlink.ai.incidents.IncidentExporter
import com.resqlink.ai.incidents.IncidentRecord
import com.resqlink.ai.incidents.IncidentRepository
import com.resqlink.ai.motion.MotionDistressDetector
import com.resqlink.ai.readiness.DiagnosticItem
import com.resqlink.ai.readiness.DiagnosticStatus
import com.resqlink.ai.readiness.EmergencySimulationManager
import com.resqlink.ai.readiness.ReadinessCheckManager
import com.resqlink.ai.readiness.ReadinessReport
import com.resqlink.ai.rescuecard.RescueCardRepository
import com.resqlink.ai.rescuecard.RescueProfile
import com.resqlink.ai.responder.ResponderCoordinator
import com.resqlink.ai.responder.ResponderState
import com.resqlink.ai.safetyjourney.JourneyScheduler
import com.resqlink.ai.safetyjourney.SafetyJourney
import com.resqlink.ai.safetyjourney.SafetyJourneyRepository
import com.resqlink.ai.service.GuardianForegroundService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmergencyViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val dbHelper = HotlineDatabaseHelper(context)
    private val repository = EmergencyRepositoryImpl(dbHelper)
    private val locationProvider = LocationProvider(context)
    private val callManager = CallManager(context)
    private val smsDispatcher = SmsDispatcher(context)
    private val sessionManager = com.resqlink.ai.data.local.SessionManager(context)
    private val voiceAssistManager = com.resqlink.ai.service.VoiceAssistManager(context)
    private val crashTelemetryProvider = com.resqlink.ai.service.CrashTelemetryProvider(context)
    private val dispatcherClassifier = com.resqlink.ai.service.DispatcherQuestionClassifier()
    private val responseSynthesizer = com.resqlink.ai.service.DispatcherResponseSynthesizer()
    private var speechHandoverDetector: com.resqlink.ai.service.SpeechHandoverDetector? = null

    // --- Master Feature Expansion Components ---
    val emergencyEventManager = EmergencyEventManager.getInstance()
    val activeIncident = emergencyEventManager.currentIncident

    private val safetyJourneyRepo = SafetyJourneyRepository.getInstance(context)
    val activeSafetyJourney = safetyJourneyRepo.activeJourney
    val safetyJourneyHistory = safetyJourneyRepo.journeyHistory
    private val journeyScheduler = JourneyScheduler(context)

    private val rescueCardRepo = RescueCardRepository.getInstance(context)
    val rescueProfile = rescueCardRepo.profile

    val readinessCheckManager = ReadinessCheckManager(context, dbHelper)
    val simulationManager = EmergencySimulationManager(emergencyEventManager)
    val simulationState = simulationManager.simulationState

    val responderCoordinator = ResponderCoordinator(context, viewModelScope)
    val responderState = responderCoordinator.responderState

    val incidentRepo = IncidentRepository.getInstance(context)
    val incidentRecords = incidentRepo.incidents

    val motionDetector = MotionDistressDetector(context, viewModelScope)

    val emergencyGuides = EmergencyGuideRepository.getAllGuides()

    val userProfile = sessionManager.userProfile

    init {
        if (sessionManager.isExperimentalMotionDetectionEnabled()) {
            motionDetector.startMonitoring()
        }
        motionDetector.onImpactDistressDetected = { peakG ->
            onVoiceDistressDetected("Experimental Motion Impact ($peakG m/s²)")
        }
        viewModelScope.launch {
            incidentRepo.refresh()
        }

        voiceAssistManager.onTranscriptUpdate = { transcript, cycle ->
            val current = _emergencyState.value
            if (current is EmergencyState.VoiceAssistActive) {
                _emergencyState.value = current.copy(
                    liveTranscript = transcript,
                    cycleCount = cycle
                )
            }
        }
    }

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

    fun updateCallPriority(callPriority: com.resqlink.ai.domain.model.CallPriority) {
        sessionManager.updateCallPriority(callPriority)
    }

    fun updateSimPreference(simPreference: com.resqlink.ai.domain.model.SimPreference) {
        sessionManager.updateSimPreference(simPreference)
    }

    fun getDetectedSims(): List<com.resqlink.ai.data.telephony.DetectedSim> {
        return callManager.getDetectedSims()
    }

    private val _isAiScanning = MutableStateFlow(false)
    val isAiScanning = _isAiScanning.asStateFlow()

    private val _lastAiScanTime = MutableStateFlow(sessionManager.getLastAiScanTime())
    val lastAiScanTime = _lastAiScanTime.asStateFlow()

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

    fun triggerAiHotlineScan() {
        if (_isAiScanning.value) return
        viewModelScope.launch {
            _isAiScanning.value = true
            try {
                val loc = _currentLocation.value ?: locationProvider.getCurrentOrCachedLocation()
                if (loc != null) {
                    val aiService = com.resqlink.ai.ai.HotlineAiService()
                    val key = sessionManager.getGeminiApiKey()
                    val discovered = aiService.discover5kmHotlines(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        resolvedCity = loc.address,
                        apiKey = key
                    )
                    if (discovered.isNotEmpty()) {
                        repository.upsertHotlines(discovered)
                        val now = System.currentTimeMillis()
                        sessionManager.setLastAiScanTime(now)
                        _lastAiScanTime.value = now
                    }
                    // Refresh hotlines list with new cached stations
                    _nearestHotlines.value = repository.getNearestHotlines(loc.latitude, loc.longitude, radiusKm = 5.0)
                    _allHotlines.value = repository.getAllHotlines()
                }
            } catch (e: Exception) {
                android.util.Log.e("EmergencyViewModel", "Manual AI hotline scan error: ${e.message}", e)
            } finally {
                _isAiScanning.value = false
            }
        }
    }

    fun togglePinHotline(hotline: Hotline) {
        viewModelScope.launch {
            repository.setPinnedHotline(hotline.id, !hotline.isPinned)
            val loc = _currentLocation.value
            if (loc != null) {
                _nearestHotlines.value = repository.getNearestHotlines(loc.latitude, loc.longitude, radiusKm = 5.0)
            }
            _allHotlines.value = repository.getAllHotlines()
        }
    }

    fun directCallHotline(phoneNumber: String) {
        callManager.dialFamilyMember(phoneNumber, userProfile.value.simPreference)
    }

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

    // Safe Test / Demo Mode (disabled by default so real emergency calling works according to user's route)
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode = _isDemoMode.asStateFlow()

    fun toggleDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    private var countdownJob: Job? = null

    init {
        loadData()
        refreshLocation()
    }

    fun loadData() {
        viewModelScope.launch {
            _allHotlines.value = repository.getAllHotlines()
            _trustedContacts.value = repository.getTrustedContacts()
        }
    }

    fun refreshLocation(autoAiScan: Boolean = true) {
        viewModelScope.launch {
            val loc = locationProvider.getCurrentOrCachedLocation(timeoutMs = 4000L)
            _currentLocation.value = loc
            if (loc != null) {
                val currentStations = repository.getNearestHotlines(loc.latitude, loc.longitude, radiusKm = 5.0)
                _nearestHotlines.value = currentStations

                val hasAiDiscovered = currentStations.any { it.source == "AI_GEMINI" && (it.distanceKm ?: 99.0) <= 6.5 }
                val lastScan = sessionManager.getLastAiScanTime()
                val isStale = (System.currentTimeMillis() - lastScan) > (30 * 60 * 1000L)

                if (autoAiScan && (!hasAiDiscovered || isStale)) {
                    triggerAiHotlineScan()
                }
            } else {
                _nearestHotlines.value = listOf(repository.getUniversalFallback())
            }
        }
    }

    /**
     * Resolves the target name and phone number dynamically based on user's active CallPriority.
     */
    fun resolveCallTarget(): Pair<String, String> {
        val profile = userProfile.value
        val nearest = _nearestHotlines.value.firstOrNull() ?: dbHelper.getUniversalFallback911()
        return when (profile.callPriority) {
            com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> {
                val name = profile.primaryContactName.ifBlank { "Family (ICE)" }
                val phone = profile.primaryContactPhone.ifBlank { nearest.primaryNumber }
                Pair(name, phone)
            }
            com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> {
                Pair("Universal 911 Dispatch", "911")
            }
            com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> {
                val name = nearest.name.ifBlank { "Local Emergency Station" }
                val phone = nearest.primaryNumber.ifBlank { "122" }
                Pair(name, phone)
            }
        }
    }

    fun isGuardianModeEnabled(): Boolean = sessionManager.isGuardianModeEnabled()

    fun toggleGuardianMode() {
        val willEnable = !sessionManager.isGuardianModeEnabled()
        sessionManager.setGuardianModeEnabled(willEnable)

        val intent = Intent(context, GuardianForegroundService::class.java)
        if (!willEnable) {
            intent.action = GuardianForegroundService.ACTION_STOP
            context.startService(intent)
            _emergencyState.value = EmergencyState.Idle
        } else {
            intent.action = GuardianForegroundService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _emergencyState.value = EmergencyState.GuardianActive(isMonitoring = true)
        }
    }

    /**
     * Triggered by Voice SOS (Distress keyword detected by GuardianForegroundService)
     */
    fun onVoiceDistressDetected(phrase: String) {
        countdownJob?.cancel()
        startContinuousEmergencyAlarm()

        val loc = _currentLocation.value
        val nearest = _nearestHotlines.value.firstOrNull() ?: dbHelper.getUniversalFallback911()
        val (targetName, targetNumber) = resolveCallTarget()

        _emergencyState.value = EmergencyState.ConfirmationPending(
            detectedPhrase = phrase,
            remainingSeconds = 10,
            location = loc,
            nearestHotline = nearest,
            callPriority = userProfile.value.callPriority,
            targetName = targetName,
            targetNumber = targetNumber
        )

        countdownJob = viewModelScope.launch {
            for (sec in 9 downTo 1) {
                delay(1000L)
                val current = _emergencyState.value
                if (current is EmergencyState.ConfirmationPending) {
                    _emergencyState.value = current.copy(remainingSeconds = sec)
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
        countdownJob?.cancel()
        startContinuousEmergencyAlarm()

        val loc = _currentLocation.value
        val nearest = _nearestHotlines.value.firstOrNull() ?: dbHelper.getUniversalFallback911()
        val (targetName, targetNumber) = resolveCallTarget()

        _emergencyState.value = EmergencyState.ManualSosGracePeriod(
            remainingSeconds = 3,
            location = loc,
            nearestHotline = nearest,
            callPriority = userProfile.value.callPriority,
            targetName = targetName,
            targetNumber = targetNumber
        )

        countdownJob = viewModelScope.launch {
            for (sec in 2 downTo 1) {
                delay(1000L)
                val current = _emergencyState.value
                if (current is EmergencyState.ManualSosGracePeriod) {
                    _emergencyState.value = current.copy(remainingSeconds = sec)
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
        countdownJob?.cancel()
        stopContinuousEmergencyAlarm()
        GuardianForegroundService.dismissEmergencyDistressPopup(context)
        viewModelScope.launch {
            val loc = _currentLocation.value ?: locationProvider.getCurrentOrCachedLocation(timeoutMs = 2000L)
            val hotline = _nearestHotlines.value.firstOrNull() ?: repository.getUniversalFallback()
            val contacts = _trustedContacts.value
            val profile = userProfile.value
            val (targetName, targetNumber) = resolveCallTarget()

            val effectiveReason = if (_isDemoMode.value) "[DEMO SIMULATION] $reason" else reason

            val sentCount = if (_isDemoMode.value) {
                contacts.count { it.isAuthorizedForSafetyCheck }
            } else {
                smsDispatcher.dispatchUnresponsiveAlerts(
                    contacts = contacts,
                    location = loc,
                    nearestHotline = hotline
                )
            }

            // Immediately initiate call according to user's configured CallPriority & Outgoing SIM Preference!
            when (profile.callPriority) {
                com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> {
                    val familyPhone = profile.primaryContactPhone
                    if (familyPhone.isNotBlank()) {
                        callManager.dialFamilyMember(familyPhone, profile.simPreference)
                    } else {
                        callManager.initiateEmergencyCall(hotline, profile.simPreference)
                    }
                }
                com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> {
                    callManager.dial911(profile.simPreference)
                }
                com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> {
                    callManager.initiateEmergencyCall(hotline, profile.simPreference)
                }
            }

            _emergencyState.value = EmergencyState.EmergencyConfirmed(
                reason = effectiveReason,
                callPriority = profile.callPriority,
                targetName = targetName,
                targetNumber = targetNumber,
                dialedHotline = hotline,
                smsSentCount = sentCount,
                location = loc
            )
        }
    }

    /**
     * Re-dials the active priority target directly with user's SIM Preference.
     */
    fun executeConfiguredCall() {
        val profile = userProfile.value
        val hotline = _nearestHotlines.value.firstOrNull() ?: dbHelper.getUniversalFallback911()
        when (profile.callPriority) {
            com.resqlink.ai.domain.model.CallPriority.FAMILY_FIRST -> {
                val familyPhone = profile.primaryContactPhone
                if (familyPhone.isNotBlank()) {
                    callManager.dialFamilyMember(familyPhone, profile.simPreference)
                } else {
                    callManager.initiateEmergencyCall(hotline, profile.simPreference)
                }
            }
            com.resqlink.ai.domain.model.CallPriority.UNIVERSAL_911 -> {
                callManager.dial911(profile.simPreference)
            }
            com.resqlink.ai.domain.model.CallPriority.LOCAL_HOTLINE -> {
                callManager.initiateEmergencyCall(hotline, profile.simPreference)
            }
        }
    }

    /**
     * Activates Voice Assist when the user cannot speak (e.g. accident, unconscious, trauma).
     * Automatically dials the emergency target, activates speakerphone, and broadcasts
     * patient vitals, GPS coordinates, and medical alerts.
     */
    fun startAccidentVoiceAssist(reason: String = "Accident Incapacitation") {
        countdownJob?.cancel()
        stopContinuousEmergencyAlarm()
        GuardianForegroundService.dismissEmergencyDistressPopup(context)

        viewModelScope.launch {
            val loc = _currentLocation.value ?: locationProvider.getCurrentOrCachedLocation(timeoutMs = 2000L)
            val hotline = _nearestHotlines.value.firstOrNull() ?: repository.getUniversalFallback()
            val contacts = _trustedContacts.value
            val profile = userProfile.value
            val (targetName, targetNumber) = resolveCallTarget()

            // 1. Dispatch SMS alert with GPS to pre-authorized safety contacts
            if (!_isDemoMode.value) {
                smsDispatcher.dispatchUnresponsiveAlerts(
                    contacts = contacts,
                    location = loc,
                    nearestHotline = hotline
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
                location = loc
            )

            // 4. Start speech recognition in background to listen for user consciousness
            speechHandoverDetector?.destroy()
            speechHandoverDetector = com.resqlink.ai.service.SpeechHandoverDetector(context) { heardSpeech ->
                performHandoverToUser(heardSpeech)
            }
            speechHandoverDetector?.startListening()

            // 5. Start Voice Assist emergency broadcast on speakerphone (ElevenLabs with Offline TTS fallback)
            val addr = loc?.address?.ifBlank { null } ?: "Metro Manila vicinity"
            voiceAssistManager.startEmergencyBroadcast(
                userName = profile.displayName,
                address = addr,
                latitude = loc?.latitude ?: 14.6507,
                longitude = loc?.longitude ?: 121.0509,
                bloodType = profile.bloodType,
                allergies = profile.allergies,
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
        val current = _emergencyState.value
        if (current is EmergencyState.VoiceAssistActive && !current.isHandoverComplete) {
            speechHandoverDetector?.stopListening()
            voiceAssistManager.performHandover(
                elevenLabsApiKey = sessionManager.getElevenLabsApiKey(),
                elevenLabsVoiceId = sessionManager.getElevenLabsVoiceId()
            ) {
                val updated = _emergencyState.value
                if (updated is EmergencyState.VoiceAssistActive) {
                    _emergencyState.value = updated.copy(
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
        val current = _emergencyState.value
        if (current is EmergencyState.VoiceAssistActive && !current.isHandoverComplete) {
            val telemetry = crashTelemetryProvider.getSnapshot()
            val profile = userProfile.value
            val intent = dispatcherClassifier.classifyQuestion(question)
            val answer = responseSynthesizer.synthesizeResponse(
                intent = intent,
                telemetry = telemetry,
                userProfile = profile,
                location = current.location
            )

            _emergencyState.value = current.copy(
                lastDispatcherQuestion = question,
                lastAiAnswer = answer,
                liveTranscript = "Dispatcher: \"$question\"\nResQLink AI: \"$answer\""
            )

            voiceAssistManager.speakDispatcherAnswer(
                answerText = answer,
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
        GuardianForegroundService.dismissEmergencyDistressPopup(context)
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
        countdownJob?.cancel()
        stopContinuousEmergencyAlarm()
        GuardianForegroundService.dismissEmergencyDistressPopup(context)
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
        countdownJob?.cancel()
        stopContinuousEmergencyAlarm()
        voiceAssistManager.release()
        speechHandoverDetector?.destroy()
        crashTelemetryProvider.stop()
        motionDetector.stopMonitoring()
    }

    fun dialHotlineDirectly(hotline: Hotline) {
        callManager.initiateEmergencyCall(hotline, userProfile.value.simPreference)
    }

    fun addTrustedContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            repository.saveTrustedContact(
                TrustedContact(
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship,
                    isAuthorizedForSafetyCheck = true
                )
            )
            _trustedContacts.value = repository.getTrustedContacts()
        }
    }

    fun removeTrustedContact(id: Long) {
        viewModelScope.launch {
            repository.deleteTrustedContact(id)
            _trustedContacts.value = repository.getTrustedContacts()
        }
    }

    private var alarmMediaPlayer: MediaPlayer? = null

    /**
     * Starts continuous repeating ringing vibration (800ms on, 400ms off) and
     * loud emergency alarm siren audio (USAGE_ALARM to bypass silent/DND mode).
     */
    fun startContinuousEmergencyAlarm() {
        // 1. Continuous Repeating Vibration (repeat = 0 loops indefinitely)
        try {
            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EmergencyViewModel", "Continuous vibration error: ${e.message}")
        }

        // 2. Loud Emergency Alarm Siren (bypasses DND with USAGE_ALARM)
        try {
            stopAlarmSound()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            alarmMediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
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
        } catch (e: Exception) {
            android.util.Log.e("EmergencyViewModel", "Alarm siren audio error: ${e.message}")
        }
    }

    /**
     * Immediately stops the continuous vibration and cancels the emergency alarm audio.
     */
    fun stopContinuousEmergencyAlarm() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            // Ignored
        }
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
        } catch (e: Exception) {
            // Ignored
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
        val current = safetyJourneyRepo.activeJourney.value ?: return null
        journeyScheduler.cancelAllAlarms()
        val resolved = current.copy(
            status = com.resqlink.ai.safetyjourney.JourneyStatus.RESOLVED_SAFE,
            completedTimeMillis = System.currentTimeMillis()
        )
        safetyJourneyRepo.archiveJourney(resolved)
        return resolved
    }

    fun cancelSafetyJourney(): SafetyJourney? {
        val current = safetyJourneyRepo.activeJourney.value ?: return null
        journeyScheduler.cancelAllAlarms()
        val cancelled = current.copy(
            status = com.resqlink.ai.safetyjourney.JourneyStatus.CANCELLED,
            completedTimeMillis = System.currentTimeMillis()
        )
        safetyJourneyRepo.archiveJourney(cancelled)
        return cancelled
    }

    // ==========================================
    // RESCUE CARD PROFILE CONTROLLER
    // ==========================================

    fun saveRescueProfile(profile: RescueProfile) {
        rescueCardRepo.saveProfile(profile)
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
        rescueCardRepo.updateVisibility(
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
        rescueCardRepo.resetToDefault()
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
        IncidentExporter.shareReport(context, record, asJson)
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
