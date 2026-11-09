"use client";

import { useState } from "react";
import {
  Cpu,
  Mic,
  Shield,
  Clock,
  MapPin,
  Send,
  Lock,
  WifiOff,
  CheckCircle2,
  AlertCircle,
  FileCode,
  ArrowRight,
  Info,
} from "lucide-react";

interface NodeDetails {
  id: string;
  name: string;
  stage: string;
  offlineStatus: "100% Offline" | "Cellular Required" | "Optional Cloud";
  permissions: string[];
  description: string;
  safetyPolicy: string;
  limitations: string;
}

export function ArchitectureExplorer() {
  const [activeNodeId, setActiveNodeId] = useState<string>("coordinator");

  const nodes: NodeDetails[] = [
    {
      id: "input",
      name: "Device Sensors & Audio Buffers",
      stage: "Stage 0: Input Capture",
      offlineStatus: "100% Offline",
      permissions: ["android.permission.RECORD_AUDIO", "Sensor.TYPE_ACCELEROMETER"],
      description:
        "Continuously samples 16kHz mono audio directly into ring buffers managed by GuardianForegroundService. No raw audio data is written to disk or transmitted across network interfaces.",
      safetyPolicy:
        "Microphone input is discarded immediately after spectral analysis. Audio buffers never leave RAM.",
      limitations:
        "Extreme ambient noise (>85dB), heavy crowds, or covered microphones can degrade acoustic detection sensitivity.",
    },
    {
      id: "classifier",
      name: "On-Device Voice Classifier",
      stage: "Stage 1: Keyword Spotting",
      offlineStatus: "100% Offline",
      permissions: ["Zero Network Permissions"],
      description:
        "A lightweight neural acoustic feature extractor and Voice Activity Detector (VAD). Compares spectrogram energy patterns against targeted Tagalog & English distress phrases such as 'Resqlink Tulong!'.",
      safetyPolicy:
        "Operates independently of Google Play Services or internet connectivity. Inference latency is <120ms.",
      limitations:
        "Cannot understand freeform conversational context; specifically tuned to high-energy distress phrases.",
    },
    {
      id: "coordinator",
      name: "Central Event Coordinator",
      stage: "Stage 2: State Machine",
      offlineStatus: "100% Offline",
      permissions: ["com.resqlink.ai.emergency.EmergencyEventManager"],
      description:
        "Central singleton coordinator ensuring only one primary incident is active. Corroborating signals (e.g. voice match + physical SOS tap) append to the existing incident rather than spawning duplicate SMS threads or overlapping sirens.",
      safetyPolicy:
        "Generates typed lifecycle states: IDLE → SUSPECTED → AWAITING_CONFIRMATION → CONFIRMED → CONTACT_ALERTING → ACTIVE_ASSIST → RESOLVED.",
      limitations:
        "Device restart clears volatile incident memory; permanent audit records are safely persisted in SQLite.",
    },
    {
      id: "confirmation",
      name: "10s Safety Confirmation Window",
      stage: "Stage 3: Escalation Guard",
      offlineStatus: "100% Offline",
      permissions: ["android.permission.VIBRATE", "FLAG_TURN_SCREEN_ON"],
      description:
        "Activates a high-priority heads-up countdown with audible sirens and tactile pulses. Empowers the user to dismiss accidental detections with a single tap of 'I'M SAFE'.",
      safetyPolicy:
        "NON-NEGOTIABLE POLICY: Expired countdowns notify pre-authorized trusted contacts via SMS only. The app NEVER automatically dials 911 on timeouts.",
      limitations:
        "If phone is completely inaccessible or submerged, the countdown will elapse and notify trusted contacts.",
    },
    {
      id: "location",
      name: "Hardware GNSS Satellite Lock",
      stage: "Stage 4: Location Acquisition",
      offlineStatus: "100% Offline",
      permissions: ["android.permission.ACCESS_FINE_LOCATION"],
      description:
        "Interfaces directly with hardware GPS and GLONASS satellites to acquire accurate latitude, longitude, and accuracy radius. Flags coordinates older than 5 minutes as stale.",
      safetyPolicy:
        "Does not query third-party location reverse-geocoding servers if offline; packages raw coordinates and standard map links.",
      limitations:
        "Deep indoor basements, subways, and metal roof structures may degrade GPS accuracy or delay fix time.",
    },
    {
      id: "dispatch",
      name: "Telephony SMS & Hotline Dialer",
      stage: "Stage 5: Alerting & Dispatch",
      offlineStatus: "Cellular Required",
      permissions: ["android.permission.SEND_SMS", "android.intent.action.DIAL"],
      description:
        "Dispatches standard GSM-7 text alerts with Google Maps coordinates to all verified ICE contacts. Launches the standard Android dialer for official hotlines (911, 143, 117).",
      safetyPolicy:
        "Emergency phone calls to 911 or hotlines remain strictly human-initiated. No blind programmatic dial calls.",
      limitations:
        "Requires cellular carrier signal reception (2G/3G/4G/5G). Standard carrier SMS rates may apply depending on plan.",
    },
  ];

  const activeNode = nodes.find((n) => n.id === activeNodeId) || nodes[2];

  return (
    <section className="py-20 bg-[#FAF9F7]" id="technology">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-14">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#17212D] text-white text-xs font-bold uppercase tracking-wider">
            <Cpu className="w-3.5 h-3.5 text-[#D62839]" />
            <span>Architecture & System Design</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            How Emergency Processing Works:{" "}
            <span className="text-[#D62839]">The On-Device Pipeline</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            Explore the exact software layers executing on your smartphone. Click on any architecture node to inspect its safety invariants, permissions, and offline mechanisms.
          </p>
        </div>

        {/* Node Pipeline Diagram (Horizontal on Desktop, Stacked on Mobile) */}
        <div className="mb-12 overflow-x-auto pb-4 custom-scrollbar">
          <div className="flex items-center justify-between min-w-[850px] gap-2 px-2">
            {nodes.map((node, index) => {
              const isSelected = node.id === activeNodeId;
              return (
                <div key={node.id} className="flex items-center flex-1">
                  <button
                    type="button"
                    onClick={() => setActiveNodeId(node.id)}
                    className={`w-full p-4 rounded-2xl border text-left transition-all relative ${
                      isSelected
                        ? "bg-[#17212D] text-white border-[#17212D] shadow-lg scale-102"
                        : "bg-white text-[#17212D] border-[#E8E5DD] hover:border-[#D62839]/50 shadow-2xs"
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                          isSelected ? "bg-[#D62839] text-white" : "bg-[#F8E8EA] text-[#D62839]"
                        }`}
                      >
                        Step {index + 1}
                      </span>
                      <span className="text-[10px] opacity-75 font-mono">
                        {node.offlineStatus === "100% Offline" ? "Offline" : "Cellular"}
                      </span>
                    </div>
                    <div className="font-bold text-xs truncate mb-1">{node.name}</div>
                    <div
                      className={`text-[10px] truncate ${
                        isSelected ? "text-[#94A3B8]" : "text-[#667085]"
                      }`}
                    >
                      {node.stage}
                    </div>

                    {isSelected && (
                      <div className="absolute -bottom-2.5 left-1/2 -translate-x-1/2 w-4 h-4 bg-[#17212D] rotate-45" />
                    )}
                  </button>

                  {index < nodes.length - 1 && (
                    <ArrowRight className="w-4 h-4 text-[#D62839] shrink-0 mx-1.5 opacity-60" />
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Selected Node Detailed Inspector Card */}
        <div className="bg-white rounded-3xl p-6 sm:p-10 border border-[#E8E5DD] shadow-sm max-w-4xl mx-auto">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-6 border-b border-[#E8E5DD]">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="text-xs font-bold uppercase tracking-wider text-[#D62839]">
                  {activeNode.stage}
                </span>
                <span className="w-1.5 h-1.5 rounded-full bg-[#187A61]" />
                <span className="text-xs font-semibold text-[#187A61]">
                  {activeNode.offlineStatus}
                </span>
              </div>
              <h3 className="text-2xl font-bold text-[#17212D]">{activeNode.name}</h3>
            </div>

            <div className="flex flex-wrap gap-1.5">
              {activeNode.permissions.map((p, i) => (
                <span
                  key={i}
                  className="px-2.5 py-1 rounded-md bg-[#FAF9F7] text-[11px] font-mono font-medium text-[#667085] border border-[#E8E5DD]"
                >
                  {p}
                </span>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 pt-6">
            <div className="space-y-4">
              <div>
                <h4 className="text-xs font-bold text-[#17212D] uppercase tracking-wider mb-1.5">
                  Component Responsibility
                </h4>
                <p className="text-sm text-[#667085] leading-relaxed">
                  {activeNode.description}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#F8E8EA]/60 border border-[#D62839]/20">
                <div className="flex items-center gap-2 text-[#D62839] font-bold text-xs uppercase tracking-wider mb-1">
                  <Shield className="w-4 h-4" />
                  <span>Enforced Safety Policy</span>
                </div>
                <p className="text-xs text-[#17212D] leading-relaxed">
                  {activeNode.safetyPolicy}
                </p>
              </div>
            </div>

            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD]">
                <div className="flex items-center gap-2 text-[#667085] font-bold text-xs uppercase tracking-wider mb-1">
                  <AlertCircle className="w-4 h-4 text-[#D97706]" />
                  <span>Known Edge Cases & Constraints</span>
                </div>
                <p className="text-xs text-[#667085] leading-relaxed">
                  {activeNode.limitations}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-[#E7F6F1] border border-[#187A61]/20">
                <div className="flex items-center gap-2 text-[#187A61] font-bold text-xs uppercase tracking-wider mb-1">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Audited Privacy Standard</span>
                </div>
                <p className="text-xs text-[#187A61] leading-relaxed">
                  Zero telemetric tracking of raw voice, Medical Rescue Card, or private incident logs to public web endpoints.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
