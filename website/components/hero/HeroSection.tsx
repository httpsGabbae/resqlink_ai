"use client";

import { useState } from "react";
import Link from "next/link";
import { Download, Play, ShieldCheck, WifiOff, FileCheck, PhoneCall, Sparkles } from "lucide-react";
import { AndroidMockup } from "@/components/hero/AndroidMockup";

export function HeroSection() {
  const [mockupState, setMockupState] = useState<"idle" | "detecting" | "countdown" | "alerted">("idle");

  return (
    <section className="relative pt-8 pb-16 lg:pt-16 lg:pb-24 overflow-hidden bg-[#FAF9F7]" id="hero">
      {/* Signature Living Lifeline SVG Background Path */}
      <div className="absolute inset-0 pointer-events-none -z-0 overflow-hidden" aria-hidden="true">
        <svg
          className="w-full h-full min-h-[700px] opacity-35"
          viewBox="0 0 1440 800"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M-50 200 C 300 200, 450 50, 720 280 C 990 510, 1150 150, 1500 350"
            stroke="url(#lifelineGradient)"
            strokeWidth="3"
            strokeDasharray="8 6"
            className="animate-lifeline-pulse"
          />
          <path
            d="M-50 450 C 350 450, 520 620, 800 400 C 1080 180, 1300 500, 1500 480"
            stroke="#D62839"
            strokeWidth="1.5"
            strokeOpacity="0.25"
          />
          <defs>
            <linearGradient id="lifelineGradient" x1="0" y1="0" x2="1440" y2="800" gradientUnits="userSpaceOnUse">
              <stop stopColor="#D62839" stopOpacity="0.8" />
              <stop offset="0.5" stopColor="#187A61" stopOpacity="0.6" />
              <stop offset="1" stopColor="#D62839" stopOpacity="0.9" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Column: Hero Copy & Actions */}
          <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
            {/* Identity Badge */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#F8E8EA] border border-[#D62839]/20 text-[#D62839] text-xs font-semibold shadow-2xs">
              <span className="w-2 h-2 rounded-full bg-[#D62839] animate-ping" />
              <span>🇵🇭 Official Android Release v1.3.0 • Offline-First Lifeline</span>
            </div>

            {/* Editorial Main Headline */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-[#17212D] tracking-tight leading-[1.1]">
              A Connection That Matters When It{" "}
              <span className="text-[#D62839] underline decoration-[#F8E8EA] decoration-wavy decoration-2 underline-offset-8">
                Matters Most.
              </span>
            </h1>

            {/* Supporting Copy */}
            <p className="text-lg sm:text-xl text-[#667085] leading-relaxed max-w-2xl mx-auto lg:mx-0 font-normal">
              ResQLink AI is an offline-first Android safety companion designed to recognize possible distress, help people request emergency assistance, and coordinate trusted contacts — even in zero-internet environments across the Philippines.
            </p>

            {/* Primary Action Buttons */}
            <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-2">
              <a
                href="#download"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-6 py-3.5 text-base font-bold text-white bg-[#D62839] hover:bg-[#BF2030] active:bg-[#9E1927] rounded-xl shadow-md hover:shadow-lg transition-all duration-200 transform hover:-translate-y-0.5"
              >
                <Download className="w-5 h-5" />
                <span>Download APK (v1.3.0)</span>
              </a>

              <a
                href="#simulation"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3.5 text-base font-semibold text-[#17212D] hover:text-[#D62839] bg-white border border-[#E8E5DD] hover:border-[#D62839] rounded-xl shadow-2xs hover:shadow-sm transition-all duration-200"
              >
                <Play className="w-4 h-4 text-[#D62839]" />
                <span>Simulate Emergency Workflow</span>
              </a>
            </div>

            {/* Interactive Phone State Selector Pills for Hero */}
            <div className="pt-2">
              <div className="text-xs font-semibold text-[#667085] mb-2 uppercase tracking-wider">
                Interactive Preview States:
              </div>
              <div className="flex flex-wrap items-center justify-center lg:justify-start gap-2">
                <button
                  type="button"
                  onClick={() => setMockupState("idle")}
                  className={`px-3 py-1 text-xs font-medium rounded-lg border transition-all ${
                    mockupState === "idle"
                      ? "bg-[#17212D] text-white border-[#17212D]"
                      : "bg-white text-[#667085] border-[#E8E5DD] hover:border-[#17212D]"
                  }`}
                >
                  Standby Home
                </button>
                <button
                  type="button"
                  onClick={() => setMockupState("detecting")}
                  className={`px-3 py-1 text-xs font-medium rounded-lg border transition-all ${
                    mockupState === "detecting"
                      ? "bg-[#D62839] text-white border-[#D62839]"
                      : "bg-white text-[#667085] border-[#E8E5DD] hover:border-[#D62839]"
                  }`}
                >
                  Voice Distress Match
                </button>
                <button
                  type="button"
                  onClick={() => setMockupState("countdown")}
                  className={`px-3 py-1 text-xs font-medium rounded-lg border transition-all ${
                    mockupState === "countdown"
                      ? "bg-[#D62839] text-white border-[#D62839]"
                      : "bg-white text-[#667085] border-[#E8E5DD] hover:border-[#D62839]"
                  }`}
                >
                  10s Confirmation
                </button>
                <button
                  type="button"
                  onClick={() => setMockupState("alerted")}
                  className={`px-3 py-1 text-xs font-medium rounded-lg border transition-all ${
                    mockupState === "alerted"
                      ? "bg-[#187A61] text-white border-[#187A61]"
                      : "bg-white text-[#667085] border-[#E8E5DD] hover:border-[#187A61]"
                  }`}
                >
                  SMS Alerts Dispatched
                </button>
              </div>
            </div>

            {/* Verification Metadata Footnote */}
            <div className="pt-4 border-t border-[#E8E5DD] grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs text-[#667085]">
              <div className="flex items-center gap-2">
                <WifiOff className="w-4 h-4 text-[#187A61]" />
                <span>100% Offline Core</span>
              </div>
              <div className="flex items-center gap-2">
                <FileCheck className="w-4 h-4 text-[#187A61]" />
                <span>SHA-256 Verified Binary</span>
              </div>
              <div className="flex items-center gap-2 col-span-2 sm:col-span-1">
                <ShieldCheck className="w-4 h-4 text-[#187A61]" />
                <span>Android 8.0 to 15 Ready</span>
              </div>
            </div>
          </div>

          {/* Right Column: Realistic Android Device Presentation */}
          <div className="lg:col-span-5 flex justify-center items-center">
            <AndroidMockup
              interactiveState={mockupState}
              onStateChange={setMockupState}
            />
          </div>
        </div>
      </div>
    </section>
  );
}
