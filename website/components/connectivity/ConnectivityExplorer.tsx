"use client";

import { useState } from "react";
import {
  Wifi,
  WifiOff,
  Signal,
  SignalZero,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  HelpCircle,
  ShieldAlert,
  Mic,
  QrCode,
  Compass,
  HeartPulse,
  MapPin,
  Send,
  PhoneCall,
  Cloud,
} from "lucide-react";

export function ConnectivityExplorer() {
  const [hasInternet, setHasInternet] = useState(false);
  const [hasCellular, setHasCellular] = useState(true);

  const capabilities = [
    {
      id: "voice",
      name: "On-Device Voice Distress Detection",
      category: "Distress Input",
      icon: Mic,
      requiresInternet: false,
      requiresCellular: false,
      description: "Acoustic keyword spotter runs completely offline via local Android audio buffers.",
    },
    {
      id: "sos",
      name: "Manual One-Tap & Slide-to-SOS",
      category: "Trigger",
      icon: ShieldAlert,
      requiresInternet: false,
      requiresCellular: false,
      description: "Direct touch interaction activates emergency state machine inside local memory.",
    },
    {
      id: "card",
      name: "Rescue Card & Medical QR Generator",
      category: "Medical ID",
      icon: QrCode,
      requiresInternet: false,
      requiresCellular: false,
      description: "Generates offline 2D QR bitmaps readable by bystander smartphone cameras.",
    },
    {
      id: "journey",
      name: "Safety Journey Scheduled Check-In",
      category: "Travel Guard",
      icon: Compass,
      requiresInternet: false,
      requiresCellular: false,
      description: "Employs Android AlarmManager.setExactAndAllowWhileIdle to survive Doze mode offline.",
    },
    {
      id: "guides",
      name: "Bilingual Emergency First Aid Guides",
      category: "Knowledge",
      icon: HeartPulse,
      requiresInternet: false,
      requiresCellular: false,
      description: "14 authoritative CPR, typhoon, flood, and earthquake protocols bundled in app storage.",
    },
    {
      id: "gps",
      name: "Hardware GPS Location Acquisition",
      category: "Location",
      icon: MapPin,
      requiresInternet: false,
      requiresCellular: false,
      description: "Hardware GNSS satellite receivers lock latitude & longitude independently of mobile data.",
    },
    {
      id: "sms",
      name: "Trusted-Contact Emergency SMS Alerts",
      category: "Alerting",
      icon: Send,
      requiresInternet: false,
      requiresCellular: true,
      description: "Dispatches SMS directly over GSM/CDMA cellular towers without data plan balance.",
    },
    {
      id: "call",
      name: "Direct Emergency Hotline Phone Calls",
      category: "Hotlines",
      icon: PhoneCall,
      requiresInternet: false,
      requiresCellular: true,
      description: "Launches Android system dialer (911, 143, 117) via cellular voice band.",
    },
    {
      id: "telemetry",
      name: "Optional Impact Telemetry Synchronization",
      category: "Analytics",
      icon: Cloud,
      requiresInternet: true,
      requiresCellular: false,
      description: "Syncs voluntary, anonymous aggregate statistics only when internet reconnects.",
    },
  ];

  return (
    <section className="py-20 bg-[#FAF9F7]" id="connectivity">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-14">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#187A61]/15 text-[#187A61] text-xs font-bold uppercase tracking-wider">
            <WifiOff className="w-3.5 h-3.5" />
            <span>Zero-Cloud Resilience</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            Interactive Connectivity Matrix:{" "}
            <span className="text-[#187A61]">Engineered for Offline Reality</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            Typhoons, earthquakes, and remote provinces frequently wipe out 4G/5G data networks. Toggle simulated network conditions below to discover what works seamlessly without internet.
          </p>
        </div>

        {/* Network State Toggles Bar */}
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#E8E5DD] shadow-xs max-w-4xl mx-auto mb-12">
          <div className="text-center mb-6">
            <span className="text-xs font-bold uppercase tracking-wider text-[#667085]">
              Simulate Environmental Connectivity Conditions
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {/* Internet (Wi-Fi / Mobile Data) Toggle */}
            <div className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                    hasInternet ? "bg-[#187A61]/15 text-[#187A61]" : "bg-[#D62839]/15 text-[#D62839]"
                  }`}
                >
                  {hasInternet ? <Wifi className="w-5 h-5" /> : <WifiOff className="w-5 h-5" />}
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[#17212D]">Mobile Internet / Wi-Fi</h4>
                  <span className="text-xs text-[#667085]">
                    {hasInternet ? "Online (4G / 5G / Broadband)" : "Disconnected (0 KB Data / Blackout)"}
                  </span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setHasInternet(!hasInternet)}
                className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors ${
                  hasInternet
                    ? "bg-[#187A61] text-white"
                    : "bg-[#17212D] text-white"
                }`}
                aria-label={`Toggle mobile internet state, currently ${hasInternet ? "online" : "offline"}`}
              >
                {hasInternet ? "Switch to Offline" : "Switch to Online"}
              </button>
            </div>

            {/* Cellular Service (GSM Reception) Toggle */}
            <div className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                    hasCellular ? "bg-[#187A61]/15 text-[#187A61]" : "bg-[#D62839]/15 text-[#D62839]"
                  }`}
                >
                  {hasCellular ? <Signal className="w-5 h-5" /> : <SignalZero className="w-5 h-5" />}
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[#17212D]">Cellular Tower Reception</h4>
                  <span className="text-xs text-[#667085]">
                    {hasCellular ? "Connected (GSM / 2G / 3G / LTE)" : "No Service / Emergency Only"}
                  </span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setHasCellular(!hasCellular)}
                className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors ${
                  hasCellular
                    ? "bg-[#187A61] text-white"
                    : "bg-[#17212D] text-white"
                }`}
                aria-label={`Toggle cellular reception state, currently ${hasCellular ? "connected" : "no service"}`}
              >
                {hasCellular ? "Cut Cellular" : "Restore Cellular"}
              </button>
            </div>
          </div>

          {/* Environmental Context Summary Callout */}
          <div className="mt-6 pt-4 border-t border-[#E8E5DD] flex flex-wrap items-center justify-between gap-3 text-xs text-[#667085]">
            <span>
              Simulated Scenario:{" "}
              <strong className="text-[#17212D]">
                {!hasInternet && hasCellular
                  ? "Typhoon Data Outage / Rural Island (No Internet, Basic SMS Functional)"
                  : !hasInternet && !hasCellular
                  ? "Total Catastrophic Infrastructure Blackout (Strictly Local Device Features)"
                  : hasInternet && !hasCellular
                  ? "Wi-Fi Only Tablet / Airplane Mode with Wi-Fi"
                  : "Normal Full Connectivity"}
              </strong>
            </span>
            <span className="text-[#187A61] font-semibold">
              {capabilities.filter((c) => {
                const needsNet = c.requiresInternet && !hasInternet;
                const needsCell = c.requiresCellular && !hasCellular;
                return !needsNet && !needsCell;
              }).length}{" "}
              of 9 Capabilities Available
            </span>
          </div>
        </div>

        {/* Dynamic Capability Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {capabilities.map((item) => {
            const isBlockedByNet = item.requiresInternet && !hasInternet;
            const isBlockedByCell = item.requiresCellular && !hasCellular;
            const isAvailable = !isBlockedByNet && !isBlockedByCell;

            return (
              <div
                key={item.id}
                className={`p-6 rounded-2xl border transition-all ${
                  isAvailable
                    ? "bg-white border-[#E8E5DD] shadow-2xs hover:shadow-sm"
                    : "bg-[#F3F1EC]/60 border-[#E8E5DD] opacity-70"
                }`}
              >
                <div className="flex items-center justify-between mb-4">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      isAvailable
                        ? "bg-[#187A61]/15 text-[#187A61]"
                        : "bg-[#667085]/15 text-[#667085]"
                    }`}
                  >
                    <item.icon className="w-5 h-5" />
                  </div>
                  {isAvailable ? (
                    <span className="inline-flex items-center gap-1 text-[11px] font-bold text-[#187A61] bg-[#E7F6F1] px-2.5 py-1 rounded-full border border-[#187A61]/20">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>OPERATIONAL</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-[11px] font-bold text-[#667085] bg-[#E8E5DD] px-2.5 py-1 rounded-full">
                      <XCircle className="w-3.5 h-3.5" />
                      <span>UNAVAILABLE</span>
                    </span>
                  )}
                </div>

                <div className="text-[11px] font-semibold uppercase tracking-wider text-[#667085] mb-1">
                  {item.category}
                </div>
                <h3 className="text-base font-bold text-[#17212D] mb-2">{item.name}</h3>
                <p className="text-xs text-[#667085] leading-relaxed mb-4">{item.description}</p>

                <div className="pt-3 border-t border-[#E8E5DD] flex items-center justify-between text-[11px] font-medium text-[#667085]">
                  <span>Requires Internet:</span>
                  <span className={item.requiresInternet ? "text-[#D62839] font-bold" : "text-[#187A61] font-bold"}>
                    {item.requiresInternet ? "Yes" : "No (Offline)"}
                  </span>
                </div>
                <div className="pt-1 flex items-center justify-between text-[11px] font-medium text-[#667085]">
                  <span>Requires Cellular GSM:</span>
                  <span className={item.requiresCellular ? "text-[#D97706] font-bold" : "text-[#187A61] font-bold"}>
                    {item.requiresCellular ? "Yes (Cellular SMS/Voice)" : "No (Standalone)"}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
