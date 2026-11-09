"use client";

import { useState } from "react";
import {
  Mic,
  AlertCircle,
  Users,
  Compass,
  QrCode,
  ShieldCheck,
  HeartHandshake,
  FileText,
  Activity,
  BookOpen,
  ArrowRight,
  Sparkles,
  Lock,
} from "lucide-react";

export function FeatureBentoGrid() {
  const [selectedCategory, setSelectedCategory] = useState<string>("all");

  const features = [
    {
      id: "voice",
      title: "On-Device Voice Distress Radar",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "ai",
      icon: Mic,
      summary:
        "Recognizes distress phrases like 'Resqlink Tulong!' using an on-device lightweight acoustic classifier. Operates 24/7 in Guardian Mode without streaming audio bytes to external cloud servers.",
      technical: "Voice Activity Detection (VAD) + On-Device Keyword Spotter • Memory: <25MB • Zero Cloud Latency",
      span: "lg:col-span-8",
    },
    {
      id: "sos",
      title: "Manual SOS & Slide-to-SOS",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "trigger",
      icon: AlertCircle,
      summary:
        "High-contrast tactile controls engineered to prevent accidental activations. Features a 3-second audible grace window with instantaneous cancellation before SMS transmission.",
      technical: "Dual Confirmation Slider • Full-Screen Heads-Up Display • Audio Siren",
      span: "lg:col-span-4",
    },
    {
      id: "contacts",
      title: "Trusted-Contact Cellular Dispatch",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "alert",
      icon: Users,
      summary:
        "Automatically coordinates your chosen emergency circle. Transmits standardized GSM-7 SMS messages with clickable Google Maps coordinates directly through carrier towers.",
      technical: "TelephonyManager SMS Dispatcher • Lat/Long Precision to ±4.2m • Multi-Recipient Sequencing",
      span: "lg:col-span-4",
    },
    {
      id: "journey",
      title: "Safety Journey Travel Timer",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "travel",
      icon: Compass,
      summary:
        "Heading home late or traversing high-risk areas? Set a 15m to 2h journey with configurable grace periods. Missed check-ins automatically alert contacts with your last known GPS.",
      technical: "AlarmManager.setExactAndAllowWhileIdle • Survives Android Doze • Never Auto-Dials 911",
      span: "lg:col-span-8",
    },
    {
      id: "card",
      title: "Rescue Card & Offline Medical QR",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "medical",
      icon: QrCode,
      summary:
        "Provides bystanders and paramedics instant access to your blood type, allergies, and ICE contacts via a locally generated QR bitmap. Granular privacy toggles protect private records.",
      technical: "Zero Cloud QR Matrix Generator • Encrypted SharedPreferences • Bystander Scanner Compatible",
      span: "lg:col-span-6",
    },
    {
      id: "readiness",
      title: "Emergency Readiness Diagnostics",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "diagnostic",
      icon: ShieldCheck,
      summary:
        "Runs an 8-point hardware diagnostic test verifying microphone access, GPS lock, SMS permissions, battery optimization exemptions, and carrier signal health before emergencies occur.",
      technical: "ReadinessCheckManager • Safe Dry-Run Simulation Mode • Automated Remediation Advice",
      span: "lg:col-span-6",
    },
    {
      id: "responder",
      title: "Responder Mode (Good Samaritan)",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "responder",
      icon: HeartHandshake,
      summary:
        "Turns any passerby or first responder into an informed helper. Displays sanitized emergency vitals, elapsed emergency timers, verified GPS fix age, and one-tap hotline dialers.",
      technical: "Sanitized Medical View • Official Hotlines (911, 143, 117) • Coordinates Verification",
      span: "lg:col-span-4",
    },
    {
      id: "incidents",
      title: "Offline Incident Audit & Export",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "reporting",
      icon: FileText,
      summary:
        "Maintains a tamper-evident chronological timeline of every emergency trigger, cancellation, and alert delivery. Exportable in structured JSON and human-readable text for police or insurance.",
      technical: "Local SQLite (incident_logs v7) • Android Sharesheet Integration • Idempotent Incident IDs",
      span: "lg:col-span-4",
    },
    {
      id: "motion",
      title: "Experimental Motion Detection",
      badge: "Experimental (Opt-In)",
      badgeColor: "bg-[#D97706]/15 text-[#D97706] border-[#D97706]/30",
      category: "experimental",
      icon: Activity,
      summary:
        "Heuristic impact detector measuring tri-axial accelerometer spikes (>2.8g) followed by stillness. Disabled by default; gated behind a loud 10-second confirmation siren.",
      technical: "Sensor.TYPE_ACCELEROMETER • 3.5s Rest Observation Window • Strict Non-Dialer Invariant",
      span: "lg:col-span-4",
    },
    {
      id: "guides",
      title: "Bilingual Emergency Knowledge Base",
      badge: "Available in v1.3.0",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      category: "guides",
      icon: BookOpen,
      summary:
        "14 step-by-step life-saving guides for CPR, severe bleeding, choking, typhoons, earthquakes, and floods in English and Filipino. Verified against Philippine Red Cross, PHIVOLCS, and PAGASA.",
      technical: "100% Bundled Local Repository • Zero Data Cost • Official Agency Citations",
      span: "lg:col-span-12",
    },
  ];

  const filteredFeatures =
    selectedCategory === "all"
      ? features
      : features.filter((f) => f.category === selectedCategory || (selectedCategory === "experimental" && f.category === "experimental"));

  return (
    <section className="py-20 bg-white border-b border-[#E8E5DD]" id="features">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-12">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#F8E8EA] text-[#D62839] text-xs font-bold uppercase tracking-wider">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Actual Android Implementation Scope</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            10 Safety Modules Engineered for{" "}
            <span className="text-[#D62839]">Reliability Under Stress</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            Every capability described below is verified in the compiled Kotlin source code (`com.resqlink.ai`). We never market planned features as finished.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {features.map((feature) => (
            <div
              key={feature.id}
              className={`${feature.span} bg-[#FAF9F7] rounded-3xl p-6 sm:p-8 border border-[#E8E5DD] hover:border-[#D62839]/40 hover:shadow-md transition-all duration-200 flex flex-col justify-between group`}
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div className="w-12 h-12 rounded-2xl bg-white border border-[#E8E5DD] group-hover:border-[#D62839]/30 flex items-center justify-center text-[#D62839] shadow-2xs transition-colors">
                    <feature.icon className="w-6 h-6" />
                  </div>
                  <span
                    className={`text-[11px] font-bold px-3 py-1 rounded-full border ${feature.badgeColor}`}
                  >
                    {feature.badge}
                  </span>
                </div>

                <h3 className="text-xl font-bold text-[#17212D] mb-2 tracking-tight">
                  {feature.title}
                </h3>
                <p className="text-sm text-[#667085] leading-relaxed mb-6 font-normal">
                  {feature.summary}
                </p>
              </div>

              <div className="pt-4 border-t border-[#E8E5DD] flex items-center justify-between text-xs text-[#667085] font-mono">
                <span className="truncate pr-2">{feature.technical}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
