"use client";

import { useState } from "react";
import Image from "next/image";
import {
  Shield,
  PhoneCall,
  MapPin,
  Mic,
  QrCode,
  HeartPulse,
  Activity,
  AlertCircle,
  Wifi,
  Battery,
  Signal,
  CheckCircle2,
  Clock,
  Compass,
} from "lucide-react";

interface AndroidMockupProps {
  interactiveState?: "idle" | "detecting" | "countdown" | "alerted";
  onStateChange?: (state: "idle" | "detecting" | "countdown" | "alerted") => void;
}

export function AndroidMockup({
  interactiveState = "idle",
  onStateChange,
}: AndroidMockupProps) {
  const [activeTab, setActiveTab] = useState<"home" | "card" | "guides">("home");

  return (
    <div className="relative mx-auto w-[310px] sm:w-[340px] md:w-[360px] select-none">
      {/* Outer ambient glow */}
      <div
        className="absolute -inset-4 bg-gradient-to-tr from-[#D62839]/20 via-[#F8E8EA]/40 to-[#187A61]/15 rounded-[56px] blur-2xl -z-10 transform scale-95"
        aria-hidden="true"
      />

      {/* Modern Android Phone Bezel */}
      <div className="relative rounded-[48px] p-3 bg-gradient-to-b from-[#2A3644] via-[#17212D] to-[#0D1520] shadow-[0_25px_60px_-15px_rgba(23,33,45,0.4),0_0_0_1px_rgba(255,255,255,0.1)]">
        {/* Antenna bands & buttons */}
        <div className="absolute -left-1 top-28 w-1 h-12 bg-[#3D4A59] rounded-l-sm" />
        <div className="absolute -left-1 top-44 w-1 h-12 bg-[#3D4A59] rounded-l-sm" />
        <div className="absolute -right-1 top-32 w-1 h-16 bg-[#D62839] rounded-r-sm" />

        {/* Screen Glass */}
        <div className="relative rounded-[38px] overflow-hidden bg-[#081226] text-[#F8FAFC] border border-[#263964]/70 h-[670px] flex flex-col font-sans">
          {/* Status Bar */}
          <div className="h-7 px-5 pt-1.5 flex items-center justify-between text-[11px] text-[#94A3B8] font-medium bg-[#081226]/80 backdrop-blur-xs z-30">
            <span>19:55</span>
            {/* Front Camera Hole-punch */}
            <div className="w-3.5 h-3.5 rounded-full bg-black border border-[#17212D] mx-auto shadow-inner" />
            <div className="flex items-center gap-1.5">
              <Signal className="w-3 h-3 text-[#10B981]" />
              <Wifi className="w-3 h-3 text-[#10B981]" />
              <Battery className="w-3.5 h-3.5 text-[#10B981]" />
            </div>
          </div>

          {/* App Header */}
          <div className="px-4 py-2.5 bg-[#101C36] border-b border-[#263964] flex items-center justify-between z-20">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg overflow-hidden bg-white p-0.5 shadow-xs">
                <Image
                  src="/brand/resqlink_logo.jpg"
                  alt="ResQLink"
                  width={28}
                  height={28}
                  className="rounded object-cover"
                />
              </div>
              <div>
                <div className="flex items-center gap-1">
                  <span className="font-bold text-xs text-white">ResQLink AI</span>
                  <span className="text-[9px] font-semibold bg-[#D62839]/20 text-[#D62839] px-1 rounded">
                    v1.3.0
                  </span>
                </div>
                <span className="text-[10px] text-[#94A3B8] block leading-none">
                  PH Offline Emergency System
                </span>
              </div>
            </div>

            {/* Offline GPS Fix Pill */}
            <div className="flex items-center gap-1 bg-[#10B981]/15 text-[#10B981] px-2 py-0.5 rounded-full text-[10px] font-medium border border-[#10B981]/30">
              <span className="w-1.5 h-1.5 rounded-full bg-[#10B981] animate-pulse" />
              <span>GPS: ±4m Fix</span>
            </div>
          </div>

          {/* Screen Body Content based on Interactive State */}
          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3 relative z-10 custom-scrollbar">
            {interactiveState === "detecting" ? (
              /* State: Voice Detection Detected */
              <div className="bg-[#162544] rounded-2xl p-4 border border-[#D62839] animate-fadeIn space-y-3">
                <div className="flex items-center gap-2 text-[#D62839]">
                  <Mic className="w-5 h-5 animate-pulse" />
                  <span className="text-xs font-bold uppercase tracking-wider">
                    Voice Keyword Detected
                  </span>
                </div>
                <div className="bg-[#081226] p-3 rounded-xl border border-[#263964] text-center">
                  <p className="text-sm font-semibold text-white">"Resqlink Tulong!"</p>
                  <p className="text-[10px] text-[#94A3B8] mt-1">
                    On-device acoustic classifier match • Offline confidence: 96.8%
                  </p>
                </div>
                <div className="w-full h-8 flex items-center justify-center gap-1">
                  {[40, 75, 100, 60, 90, 45, 80, 100, 70, 30].map((h, i) => (
                    <span
                      key={i}
                      className="w-1 bg-[#D62839] rounded-full transition-all duration-300"
                      style={{ height: `${h}%` }}
                    />
                  ))}
                </div>
                <div className="text-[11px] text-center text-[#94A3B8]">
                  Escalating to 10s Confirmation Window...
                </div>
              </div>
            ) : interactiveState === "countdown" ? (
              /* State: 10s Emergency Countdown Confirmation */
              <div className="bg-[#162544] rounded-2xl p-4 border-2 border-[#D62839] text-center space-y-3 animate-fadeIn">
                <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-[#D62839]/20 border-2 border-[#D62839] text-[#D62839] font-black text-2xl animate-pulse">
                  7s
                </div>
                <div>
                  <h4 className="font-bold text-sm text-white">ARE YOU IN DANGER?</h4>
                  <p className="text-[11px] text-[#94A3B8] mt-0.5">
                    Dispatching SMS alerts to 2 trusted contacts in 7 seconds.
                  </p>
                </div>
                <div className="flex flex-col gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => onStateChange?.("alerted")}
                    className="w-full py-2.5 bg-[#D62839] hover:bg-[#BF2030] text-white font-bold text-xs rounded-xl shadow-md transition-colors"
                  >
                    SEND SOS NOW (CONFIRM)
                  </button>
                  <button
                    type="button"
                    onClick={() => onStateChange?.("idle")}
                    className="w-full py-2 bg-[#263964] hover:bg-[#3D4A59] text-[#F8FAFC] font-semibold text-xs rounded-xl transition-colors"
                  >
                    I'M SAFE (CANCEL)
                  </button>
                </div>
                <p className="text-[9px] text-[#64748B]">
                  Policy: ResQLink never automatically dials 911 on timeouts.
                </p>
              </div>
            ) : interactiveState === "alerted" ? (
              /* State: Contact Alerting & Sent State */
              <div className="bg-[#162544] rounded-2xl p-4 border border-[#10B981] space-y-3 animate-fadeIn">
                <div className="flex items-center gap-2 text-[#10B981]">
                  <CheckCircle2 className="w-5 h-5" />
                  <span className="text-xs font-bold uppercase tracking-wider">
                    Emergency Alert Dispatched
                  </span>
                </div>
                <div className="bg-[#081226] p-2.5 rounded-xl border border-[#263964] space-y-1.5 text-xs">
                  <div className="flex justify-between text-[11px]">
                    <span className="text-[#94A3B8]">Maria Dela Cruz (Mom)</span>
                    <span className="text-[#10B981] font-medium">SMS Sent ✓</span>
                  </div>
                  <div className="flex justify-between text-[11px]">
                    <span className="text-[#94A3B8]">Juan Dela Cruz Jr. (Brother)</span>
                    <span className="text-[#10B981] font-medium">SMS Sent ✓</span>
                  </div>
                  <div className="pt-1 border-t border-[#263964] text-[10px] text-[#94A3B8]">
                    📍 Quezon City (14.6507°N, 121.0509°E)
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onStateChange?.("idle")}
                  className="w-full py-2 bg-[#10B981] hover:bg-[#0E9F6E] text-white font-bold text-xs rounded-xl transition-colors"
                >
                  Return to Standby
                </button>
              </div>
            ) : (
              /* State: Normal Home View (Faithful to Kotlin HomeScreen.kt) */
              <>
                {/* Guardian Mode Banner */}
                <div className="bg-[#162544] rounded-2xl p-3 border border-[#263964] flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-xl bg-[#10B981]/20 text-[#10B981] flex items-center justify-center shrink-0">
                      <Shield className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-bold text-white">Guardian Mode</span>
                        <span className="w-2 h-2 rounded-full bg-[#10B981] animate-pulse" />
                      </div>
                      <span className="text-[10px] text-[#94A3B8]">
                        Active • On-Device Voice Radar
                      </span>
                    </div>
                  </div>
                  <span className="text-[10px] font-semibold text-[#10B981] bg-[#10B981]/15 px-2 py-0.5 rounded-md border border-[#10B981]/30">
                    PROTECTED
                  </span>
                </div>

                {/* Central Slide-to-SOS Centerpiece */}
                <div className="bg-gradient-to-b from-[#1E2D4F] to-[#101C36] rounded-2xl p-4 border border-[#D62839]/40 text-center relative overflow-hidden shadow-lg">
                  <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(214,40,57,0.15)_0%,transparent_70%)] pointer-events-none" />
                  
                  <div className="inline-flex p-1.5 rounded-full bg-[#D62839]/20 text-[#D62839] mb-2">
                    <Activity className="w-4 h-4 animate-pulse" />
                  </div>
                  <h3 className="text-xs font-bold text-white tracking-wide uppercase">
                    Distress Hotword Radar
                  </h3>
                  <p className="text-[11px] text-[#94A3B8] mt-0.5">
                    Say <strong className="text-white">"Resqlink Tulong!"</strong> or activate SOS
                  </p>

                  {/* SOS One-Tap Button */}
                  <div className="my-3 flex justify-center">
                    <button
                      type="button"
                      onClick={() => onStateChange?.("countdown")}
                      className="w-24 h-24 rounded-full bg-gradient-to-tr from-[#9E1927] via-[#D62839] to-[#FF334B] text-white flex flex-col items-center justify-center shadow-[0_0_25px_rgba(214,40,57,0.5)] border-2 border-white/20 active:scale-95 transition-transform"
                      aria-label="Tap to simulate emergency SOS"
                    >
                      <AlertCircle className="w-7 h-7 text-white" />
                      <span className="text-xs font-black tracking-wider mt-0.5">SOS</span>
                    </button>
                  </div>

                  <div className="text-[10px] text-[#94A3B8] flex items-center justify-center gap-1">
                    <Clock className="w-3 h-3 text-[#D62839]" />
                    <span>3s Grace Window Before Alerting</span>
                  </div>
                </div>

                {/* 2x2 Feature Quick-Access Grid */}
                <div className="grid grid-cols-2 gap-2 text-left">
                  {/* Safety Journey */}
                  <div className="bg-[#162544] p-2.5 rounded-xl border border-[#263964] hover:border-[#D62839]/50 transition-colors">
                    <div className="flex items-center gap-1.5 text-[#10B981] mb-1">
                      <Compass className="w-3.5 h-3.5" />
                      <span className="text-[10px] font-bold text-white">Safety Journey</span>
                    </div>
                    <span className="text-[10px] text-[#94A3B8] block">
                      Auto check-in timer
                    </span>
                  </div>

                  {/* Rescue Card */}
                  <div className="bg-[#162544] p-2.5 rounded-xl border border-[#263964] hover:border-[#D62839]/50 transition-colors">
                    <div className="flex items-center gap-1.5 text-[#D62839] mb-1">
                      <QrCode className="w-3.5 h-3.5" />
                      <span className="text-[10px] font-bold text-white">Rescue Card</span>
                    </div>
                    <span className="text-[10px] text-[#94A3B8] block">
                      Offline Medical QR
                    </span>
                  </div>

                  {/* Readiness Check */}
                  <div className="bg-[#162544] p-2.5 rounded-xl border border-[#263964] hover:border-[#D62839]/50 transition-colors">
                    <div className="flex items-center gap-1.5 text-[#38BDF8] mb-1">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span className="text-[10px] font-bold text-white">Readiness</span>
                    </div>
                    <span className="text-[10px] text-[#94A3B8] block">
                      8 Diagnostics OK
                    </span>
                  </div>

                  {/* First Aid Guides */}
                  <div className="bg-[#162544] p-2.5 rounded-xl border border-[#263964] hover:border-[#D62839]/50 transition-colors">
                    <div className="flex items-center gap-1.5 text-[#F59E0B] mb-1">
                      <HeartPulse className="w-3.5 h-3.5" />
                      <span className="text-[10px] font-bold text-white">Offline Guides</span>
                    </div>
                    <span className="text-[10px] text-[#94A3B8] block">
                      CPR, Flood, Quake
                    </span>
                  </div>
                </div>

                {/* Nearest Philippine Hotline Card */}
                <div className="bg-[#101C36] p-2.5 rounded-xl border border-[#263964] flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-[#D62839]/20 text-[#D62839] flex items-center justify-center">
                      <PhoneCall className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <span className="text-[11px] font-bold text-white block">
                        Philippine Red Cross
                      </span>
                      <span className="text-[9px] text-[#94A3B8]">24/7 Hotline: 143</span>
                    </div>
                  </div>
                  <span className="text-[10px] font-bold text-[#10B981] bg-[#10B981]/15 px-2 py-0.5 rounded">
                    1-Tap Call
                  </span>
                </div>
              </>
            )}
          </div>

          {/* Android Bottom Navigation Bar */}
          <div className="h-12 bg-[#101C36] border-t border-[#263964] px-6 flex items-center justify-around z-20 text-[10px]">
            <button
              type="button"
              onClick={() => setActiveTab("home")}
              className={`flex flex-col items-center gap-0.5 ${
                activeTab === "home" ? "text-[#D62839]" : "text-[#94A3B8]"
              }`}
            >
              <Shield className="w-4 h-4" />
              <span>SOS Home</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("card")}
              className={`flex flex-col items-center gap-0.5 ${
                activeTab === "card" ? "text-[#D62839]" : "text-[#94A3B8]"
              }`}
            >
              <QrCode className="w-4 h-4" />
              <span>Rescue Card</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("guides")}
              className={`flex flex-col items-center gap-0.5 ${
                activeTab === "guides" ? "text-[#D62839]" : "text-[#94A3B8]"
              }`}
            >
              <HeartPulse className="w-4 h-4" />
              <span>Guides</span>
            </button>
          </div>

          {/* Android Gesture Bar */}
          <div className="h-4 bg-[#081226] flex items-center justify-center z-20">
            <div className="w-28 h-1 bg-white/30 rounded-full" />
          </div>
        </div>
      </div>
    </div>
  );
}
