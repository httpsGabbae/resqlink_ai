# ResQLink AI — Offline-First Emergency Response & Guardian Platform

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web-blue.svg)](https://github.com/httpsGabbae/resqlink_ai)
[![Language](https://img.shields.io/badge/Language-Kotlin%20%2F%20TypeScript-purple.svg)](https://kotlinlang.org)
[![Framework](https://img.shields.io/badge/Framework-Jetpack%20Compose%20%2F%20Next.js-emerald.svg)](https://nextjs.org)
[![Architecture](https://img.shields.io/badge/Architecture-Offline--First%20Safety--Critical-crimson.svg)](#architecture)
[![License](https://img.shields.io/badge/License-Proprietary-gray.svg)](#license)

**ResQLink AI** is an emergency response and guardian platform built for mission-critical personal safety. Designed with an **offline-first philosophy**, ResQLink operates reliably without cellular data or Wi-Fi, providing real-time voice distress detection, fall/motion impact sensing, 5km defense perimeter mapping, and emergency escalation workflows.

---

## Key Modules & Capabilities

### 1. Android Mobile Application (`/app`)
- **Offline Voice Distress Detection**: On-device keyword spotting for *"ResQLink Tulong!"* using local audio energy thresholding, Voice Activity Detection (VAD), and bandpass filtering with zero cloud latency.
- **5km Defense Radar & Interactive Station Map**:
  - Bundled local Leaflet engine with zero external CDN dependencies.
  - Tactical defense grid displaying Police (PNP), Hospital (ER), Fire (BFP), and Barangay DRRM emergency stations.
  - One-tap emergency calling and navigation routing.
- **Fall & Crash Impact Detection**: Multi-axis accelerometer anomaly processing detecting free-falls and sudden impact shocks.
- **Safe Travel (Journey Guardian)**: Time-bounded safety check-in sessions with dead-man's timers, grace periods, and escalation routing.
- **Medical Emergency Rescue Card**: Locally encrypted health profile and QR code for emergency first responders (blood type, allergies, ICE contacts).
- **In-Call AI Voice Assist HUD**: Head-Up Display providing real-time scripted triage guidance during emergency calls.
- **Offline Emergency Directory & Guides**: Curated nationwide emergency hotlines (911, PNP 117, Red Cross 143, DPWH 165-02) and offline medical protocol handbooks.

### 2. Companion Web Distribution Portal (`/website`)
- **Next.js 15 & Tailwind CSS**: Full-stack web application designed for direct, cryptographic APK distribution.
- **Direct APK Download**: Verified binary delivery (`ResQLink-v1.3.0.apk`) with live SHA-256 integrity verification.
- **Interactive Emergency Simulator**: Web-based preview of the escalation workflow and emergency response engine.
- **Offline Guides & Impact Dashboard**: Community safety transparency metrics, verified assistance telemetry, and offline guide reader.

---

## Repository Structure

```text
resqlink_ai/
├── app/                      # Android Native Application (Kotlin + Jetpack Compose)
│   ├── src/main/kotlin/      # Architecture, ViewModels, Services, AI Detectors
│   ├── src/main/assets/      # Bundled Leaflet and offline map runtime
│   └── src/main/res/         # Drawables, layouts, and system configurations
├── website/                  # Web Distribution Portal (Next.js 15 + TypeScript)
│   ├── app/                  # App router pages (Download, Transparency, Privacy)
│   ├── components/           # UI components, Simulators, Hero, Download Section
│   ├── lib/                  # Database clients and offline guide data
│   └── public/               # Static assets, branding, and APK release binaries
├── docs/                     # Architecture plans, master guides, and expansion docs
├── ResQLink-v1.3.0.apk       # Current signed production binary release
└── .gitignore                # Security-hardened git exclusion manifest
```

---

## Security & Privacy Non-Negotiables

ResQLink AI is built on strict safety-critical guidelines:
1. **Never Automatically Call 911 on False Alarms**: Outgoing cellular emergency calls remain user-confirmed to prevent public service strain.
2. **Zero Plaintext Secrets**: Local machine paths, `.env` files, keystores, and connection credentials are unconditionally excluded via `.gitignore`.
3. **No Cloud Dependency for Life-Saving Actions**: Voice keyword detection, SOS dispatch, safety journeys, and emergency hotlines function 100% offline.

---

## Getting Started

### Android Development
```bash
# Clone the repository
git clone https://github.com/httpsGabbae/resqlink_ai.git
cd resqlink_ai

# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

### Web Portal Development
```bash
cd website

# Install dependencies
npm install

# Start development server
npm run dev
```

---

## License

Proprietary © 2026 ResQLink AI. All rights reserved.
