# ResQLink AI — Master Feature Expansion & Implementation Document

**Document Version:** 2.0.0 (Master Expansion Complete)  
**Target Application:** ResQLink AI (`com.resqlink.ai`)  
**Architecture Standard:** Offline-First, Safety-Critical, Android 8.0 (API 26) to Android 15 (API 35)  
**Compilation Status:** BUILD SUCCESSFUL (`assembleDebug` & `testDebugUnitTest`)  
**Test Suite:** 15/15 Unit Tests Passing (0 Failures)  

---

## 1. Executive Summary & Non-Negotiable Invariants

The existing **ResQLink AI** safety application has been comprehensively expanded with seven safety-critical modules. All expansions strictly adhere to the project's foundational constraints:

1. **Existing UI Preserved 100%:** No existing layouts, screens (`HomeScreen.kt`, `DirectoryScreen.kt`, `ContactsScreen.kt`, `SettingsScreen.kt`, `MapScreen.kt`), typography, icons, colors, or navigation graphs were visually rewritten or redesigned.
2. **Offline-First Resilience:** Zero cloud backends, zero external APIs (Firebase, Supabase, AWS), and zero internet requirements are introduced for core emergency triggers, scheduling, guides, rescue card, readiness check, or reporting.
3. **Safety-Critical Policy Enforcements:**
   - **Never Automatically Dial 911 on Timeouts:** Missed safety check-ins and experimental sensor triggers notify authorized trusted contacts via SMS only. Public emergency calls remain strictly human-initiated.
   - **Experimental Isolation:** Motion-assisted distress detection is disabled by default, explicitly flagged as EXPERIMENTAL, and gated behind a 10-second loud alarm confirmation window with one-tap cancellation.
   - **Privacy Filtering:** Rescue Card exposes emergency medical attributes (blood type, allergies, instructions) strictly according to the user's granular opt-in visibility toggles.
   - **No Simulated False Alerts:** Emergency Simulation Mode runs isolated test lifecycles with mock coordinates and SMS previews without transmitting real cellular signals or calling 911.

---

## 2. Feature Inventory & Implementation Breakdown

| Feature | Scope / Responsibility | Components Created / Integrated | Safety Policy & Invariant | Status |
| :--- | :--- | :--- | :--- | :---: |
| **0. Central Emergency Coordinator** | Event deduplication, typed lifecycle state machine, incident IDs (`INC-`, `SIM-`) | `EmergencyEventManager.kt`, `EmergencyEvent.kt` | Corroborating signals log to existing incident rather than spawning duplicate SMS threads or overlapping alarms. | **COMPLETE** |
| **1. Safety Journey** | Personal check-in sessions, exact alarm scheduling, grace periods, contact escalation | `SafetyJourney.kt`, `SafetyJourneyRepository.kt`, `JourneyScheduler.kt`, `JourneyAlarmReceiver.kt` | Missed check-in notifies authorized contacts with last known GPS via SMS; NEVER auto-dials 911. | **COMPLETE** |
| **2. Rescue Card** | Offline emergency profile, privacy toggles, offline QR payload/bitmap generator | `RescueProfile.kt`, `RescueCardRepository.kt`, `RescueCardQrHelper.kt` | Granular visibility opt-in. Private medical details remain hidden from bystanders unless enabled. | **COMPLETE** |
| **3. Emergency Readiness Check** | 8 hardware/service diagnostics + isolated safe simulation mode | `DiagnosticResult.kt`, `ReadinessCheckManager.kt`, `EmergencySimulationManager.kt` | Hardware & permission separation; dry-run simulation never transmits real SMS or places phone calls. | **COMPLETE** |
| **4. Responder Mode** | First-responder & Good Samaritan coordinator; displays sanitized vitals, GPS, status | `ResponderState.kt`, `ResponderCoordinator.kt` | Hotline dialing (911, 117, 143, 165-02) is strictly user/responder-initiated via system dialer. | **COMPLETE** |
| **5. Incident Report Export** | Chronological audit logging, unique IDs, structured JSON & plain text export, user deletion | `IncidentRecord.kt`, `IncidentRepository.kt`, `IncidentExporter.kt`, `HotlineDatabaseHelper.kt` (v7) | Saved locally to SQLite (`incident_logs`); shareable via standard Android Sharesheet (`ACTION_SEND`). | **COMPLETE** |
| **6. Experimental Motion Detection** | Accelerometer impact spike ($>2.8g$) followed by 3.5s stillness confirmation | `MotionDistressDetector.kt`, `SessionManager.kt` | Disabled by default; opt-in required; triggers 10s countdown alarm; never dials 911. | **COMPLETE** |
| **7. Offline Emergency Guides** | Bilingual (EN/FIL) offline reference library for CPR, Bleeding, Choking, Quake, Fire, Typhoon, Flood | `EmergencyGuide.kt`, `EmergencyGuideRepository.kt` | Authoritative Philippine agencies: PRC (143), PHIVOLCS, PAGASA, BFP, DOH. | **COMPLETE** |

---

## 3. Detailed Component Architecture

### 3.1 Central Emergency Event Manager (`com.resqlink.ai.emergency`)
- **`EmergencyEventManager.kt`**: Central singleton coordinator ensuring only one primary incident is active at any time. When multiple triggers arrive simultaneously (e.g. voice distress keyword detected while user taps SOS button), the event manager appends a timestamped corroborating signal rather than resetting the emergency or spawning duplicate SMS dispatches.
- **Incident Lifecycle States**: `IDLE` -> `SUSPECTED` -> `AWAITING_CONFIRMATION` (10s) -> `CONFIRMED` -> `CONTACT_ALERTING` -> `ACTIVE_ASSIST` -> `RESOLVED` / `CANCELLED`.
- **Incident IDs**: `INC-YYYYMMDD-XXXXXX` for live incidents; `SIM-YYYYMMDD-XXXXXX` for dry-run simulation mode.

### 3.2 Safety Journey (`com.resqlink.ai.safetyjourney`)
- **`SafetyJourney.kt`**: Encapsulates destination, notes, duration (15m, 30m, 1h, 2h), deadline, and configurable grace period (default 5m).
- **`JourneyScheduler.kt`**: Uses `AlarmManager.setExactAndAllowWhileIdle()` with `FLAG_IMMUTABLE` PendingIntents to survive backgrounding and Android Doze mode.
- **`JourneyAlarmReceiver.kt`**: High-priority broadcast receiver posting an insistent heads-up notification with sound, vibration, and interactive notification action buttons:
  - `"I'M SAFE"`: Resolves journey, cancels alarms, archives journey record.
  - `"+15 MINS"`: Extends journey deadline, reschedules alarm.
  - `"EMERGENCY SOS"`: Immediately confirms emergency and triggers contact alert dispatch.
- **Escalation Policy**: If grace period expires without check-in, dispatches SMS with last known Google Maps coordinates to pre-authorized trusted contacts. Never dials 911.

### 3.3 Rescue Card & Offline QR (`com.resqlink.ai.rescuecard`)
- **`RescueProfile.kt`**: Contains full name, birth year/age, blood type, allergies, medications, medical conditions, emergency instructions, organ donor status, and ICE contacts.
- **Privacy Opt-In Controls**: Explicit boolean visibility flags (`isBloodTypeVisible`, `isAllergiesVisible`, `isMedicationsVisible`, `isConditionsVisible`, `isInstructionsVisible`, `isContactsVisible`, `isOrganDonorVisible`).
- **`RescueCardQrHelper.kt`**: Generates a compact offline QR payload (`v=1.0;app=ResQLink;name=...`) and renders a high-contrast 2D bitmap offline using zero cloud dependencies.

### 3.4 Responder Mode (`com.resqlink.ai.responder`)
- **`ResponderCoordinator.kt`**: Collects real-time state from `EmergencyEventManager`, `RescueCardRepository`, and `LocationProvider`.
- **State Exposed**:
  - Active incident ID, trigger description, and elapsed timer.
  - Live GPS coordinates with accuracy (in meters) and fix age (flags coordinates older than 5 minutes as stale).
  - Sanitized emergency medical card.
  - Trusted contact alert delivery counts (attempted vs. sent).
  - Official Philippine Emergency Hotlines (`911`, `117`, `143` / `02-8527-0000`, `165-02`).
  - Public emergency calls remain strictly human-initiated through standard dialer intents (`Intent.ACTION_DIAL`).

### 3.5 Incident Report Export (`com.resqlink.ai.incidents`)
- **`IncidentRecord.kt`**: Full audit model with trigger source, initial phrase, duration, start/end timestamps, coordinates, contact SMS counts, call status, and chronological timeline events.
- **`IncidentRepository.kt` & `HotlineDatabaseHelper.kt` (v7)**: SQLite table `incident_logs` stores all completed and cancelled emergency sessions permanently offline.
- **`IncidentExporter.kt`**: Formats reports into human-readable text and valid structured JSON; dispatches via Android Sharesheet (`Intent.ACTION_SEND`).

### 3.6 Experimental Motion-Assisted Distress Detection (`com.resqlink.ai.motion`)
- **`MotionDistressDetector.kt`**: Listens to `Sensor.TYPE_ACCELEROMETER`. Computes 3D vector acceleration: $a = \sqrt{a_x^2 + a_y^2 + a_z^2}$.
- **Heuristic**:
  1. Detects high-G impact spike exceeding $27.5 \text{ m/s}^2$ ($~2.8g$).
  2. Initiates a 3.5-second post-impact observation window.
  3. If user movement deviation remains near rest ($< 1.8 \text{ m/s}^2$), triggers potential distress.
  4. If user moves normally (picks up phone, keeps walking), the impact is discarded as a non-injury drop.
- **Safety**: Opt-in only (disabled by default in `SessionManager`). Feeds `EmergencyEventManager` with 10-second countdown and siren; allows instant user dismissal; never dials 911.

### 3.7 Offline Emergency First Aid & Disaster Guides (`com.resqlink.ai.guides`)
- **`EmergencyGuideRepository.kt`**: Complete offline library bundled into the app binary:
  1. **CPR & Cardiac Arrest** (Hands-Only CPR, 100-120 bpm) — EN & FIL (Philippine Red Cross guidelines).
  2. **Severe Bleeding & Wound Management** (Direct pressure, wound packing, tourniquet) — EN & FIL (PRC / DOH).
  3. **Choking First Aid** (5 Back Blows + 5 Abdominal Thrusts / Heimlich) — EN & FIL (PRC).
  4. **Earthquake Protocol** (Duck, Cover, Hold, post-quake evacuation) — EN & FIL (PHIVOLCS / NDRRMC).
  5. **Fire Evacuation** (Crawl low under smoke, Stop Drop & Roll) — EN & FIL (Bureau of Fire Protection - BFP).
  6. **Typhoon & Storm Surge** (Signal levels, preemptive evacuation, storm eye safety) — EN & FIL (PAGASA).
  7. **Flood Safety & Water Hazards** (Electrical shut-off, leptospirosis prevention) — EN & FIL (DOH / MMDA).
- Searchable by keyword, filterable by language, 100% offline.

---

## 4. File Mapping & Directory Structure

```
app/src/main/kotlin/com/resqlink/ai/
├── emergency/
│   ├── EmergencyEvent.kt                 [Added: Incident, Trigger Sources, Lifecycle States]
│   └── EmergencyEventManager.kt          [Added: Central coordinator, deduplication]
├── safetyjourney/
│   ├── SafetyJourney.kt                  [Added: Journey model, statuses, deadlines]
│   ├── SafetyJourneyRepository.kt        [Added: Local storage for active/archived journeys]
│   ├── JourneyScheduler.kt               [Added: AlarmManager exact alarm scheduler]
│   └── JourneyAlarmReceiver.kt           [Added: Heads-up reminder & contact-only escalation]
├── rescuecard/
│   ├── RescueProfile.kt                  [Added: Medical profile, granular privacy toggles]
│   ├── RescueCardRepository.kt           [Added: Offline preference storage]
│   └── RescueCardQrHelper.kt             [Added: Offline compact QR payload & 2D bitmap generator]
├── readiness/
│   ├── DiagnosticResult.kt               [Added: Diagnostic items & ReadinessReport model]
│   ├── ReadinessCheckManager.kt          [Added: 8 hardware & subsystem diagnostic checks]
│   └── EmergencySimulationManager.kt     [Added: Safe dry-run test simulation engine]
├── responder/
│   ├── ResponderState.kt                 [Added: Bystander/responder state model]
│   └── ResponderCoordinator.kt           [Added: Responder mode state manager & hotline dialer]
├── incidents/
│   ├── IncidentRecord.kt                 [Added: Chronological audit record & text formatter]
│   ├── IncidentRepository.kt             [Added: SQLite repository for incident reports]
│   └── IncidentExporter.kt               [Added: JSON / Text export & Android Sharesheet]
├── motion/
│   └── MotionDistressDetector.kt         [Added: Accelerometer impact & inactivity detection]
├── guides/
│   ├── EmergencyGuide.kt                 [Added: Guide model, steps, Do's and Don'ts]
│   └── EmergencyGuideRepository.kt       [Added: 14 bilingual guides with Philippine agency citations]
├── data/local/
│   ├── HotlineDatabaseHelper.kt          [Modified: DB v7 with incident_logs table]
│   └── SessionManager.kt                 [Modified: Added experimental motion toggle]
├── ui/emergency/
│   └── EmergencyViewModel.kt             [Modified: Connected all 7 feature controllers & states]
└── AndroidManifest.xml                   [Modified: SCHEDULE_EXACT_ALARM, JourneyAlarmReceiver]
```

---

## 5. Automated Unit Test Verification

The entire test suite was executed via `./gradlew.bat testDebugUnitTest` and passed cleanly:

```
> Task :app:compileDebugKotlin UP-TO-DATE
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:testDebugUnitTest

EmergencyEventManagerTest:
  [PASS] testRegisterVoiceTrigger_createsAwaitingConfirmationIncident
  [PASS] testRegisterManualSos_createsConfirmedIncident
  [PASS] testCorroboratingTrigger_deduplicatesWithoutDuplicateIncident
  [PASS] testSimulationTrigger_usesSimPrefix
  [PASS] testCancelIncident_transitionsStateAndArchives

SafetyJourneyTest:
  [PASS] testJourneyCreation_calculatesDeadlinesProperly
  [PASS] testJourneyOverdue_whenCurrentTimePassesDeadline
  [PASS] testJourneyInGracePeriod_whenOverdueBeforeGraceDeadline

RescueProfileTest:
  [PASS] testSanitizedSummary_includesOnlyPermittedFields
  [PASS] testQrPayload_formatsCompactKeyValue

EmergencyGuideTest:
  [PASS] testAllGuides_containsBilingualPairsForEveryCategory
  [PASS] testSearchGuides_findsMatchingKeywords
  [PASS] testPhilippineAgencyAttributions

IncidentRecordTest:
  [PASS] testFormattedText_containsRequiredSections
  [PASS] testJsonSerialization_roundTripPreservesFields

BUILD SUCCESSFUL in 19s
26 actionable tasks: 2 executed, 24 up-to-date
```

---

## 6. Build & Delivery Artifacts

- **Debug Binary:** `app/build/outputs/apk/debug/app-debug.apk`
- **Web Download Binary:** `ResQLink-v1.3.0.apk`
- **Configuration Cache:** Preserved and validated.
- **Target SDK:** 35 (Android 15), Compile SDK: 36, Min SDK: 26 (Android 8.0).
