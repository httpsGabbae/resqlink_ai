"use client";

import { useState } from "react";
import { GitCommit, Tag, CheckCircle2, Clock, Sparkles, ShieldCheck, ChevronDown, ChevronUp } from "lucide-react";

export function VersionTimeline() {
  const [expandedVersion, setExpandedVersion] = useState<string>("v1.3.0");

  const releases = [
    {
      version: "v1.3.0",
      status: "CURRENT PRODUCTION RELEASE",
      badgeColor: "bg-[#187A61]/15 text-[#187A61] border-[#187A61]/30",
      date: "November 2026",
      headline: "Master Safety Expansion & Offline Resilience",
      highlights: [
        "Added Central Emergency Event Coordinator (EmergencyEventManager.kt) with typed lifecycle states and incident deduplication.",
        "Added Safety Journey (JourneyScheduler.kt) with AlarmManager.setExactAndAllowWhileIdle for reliable check-ins surviving Android Doze.",
        "Added Rescue Card (RescueCardQrHelper.kt) generating offline 2D QR bitmaps with granular medical privacy toggles.",
        "Added Emergency Readiness Check (ReadinessCheckManager.kt) running 8 hardware diagnostics + isolated dry-run simulation mode.",
        "Added Responder Mode (ResponderCoordinator.kt) displaying sanitized vitals and human-initiated Philippine hotline dialers.",
        "Added Incident Report Export (incident_logs SQLite DB v7) formatting structured JSON and plain-text Android sharesheets.",
        "Added Experimental Motion Detection (MotionDistressDetector.kt) for impact spike detection (>2.8g) with 10s confirmation alarm.",
        "Bundled 14 bilingual offline emergency guides verified against Philippine Red Cross, PHIVOLCS, and PAGASA.",
      ],
      binary: "ResQLink-v1.3.0.apk (34.6 MB) • SHA-256 Verified",
    },
    {
      version: "v1.2.0",
      status: "PREVIOUS STABLE",
      badgeColor: "bg-[#667085]/15 text-[#667085] border-[#667085]/30",
      date: "October 2026",
      headline: "On-Device Acoustic Classifier & Slide-to-SOS",
      highlights: [
        "Implemented on-device Voice Activity Detection (VAD) and keyword spotter for Tagalog distress phrase 'Resqlink Tulong!'.",
        "Introduced Slide-to-SOS tactile emergency trigger preventing accidental pocket activations.",
        "Added offline Philippine emergency hotline database (PNP 117, Red Cross 143, BFP 8426-0219).",
        "Integrated high-precision GPS lock with coordinates formatting for cellular SMS.",
      ],
      binary: "ResQLink-v1.2.0.apk (31.2 MB)",
    },
    {
      version: "v1.0.0",
      status: "INITIAL RELEASE",
      badgeColor: "bg-[#667085]/15 text-[#667085] border-[#667085]/30",
      date: "September 2026",
      headline: "Core Foundation & Trusted-Contact Dispatch",
      highlights: [
        "Architected foundational offline-first Android application targeting Android 8.0 (API 26) through Android 15 (API 35).",
        "Implemented local SQLite database for encrypted emergency contact storage.",
        "Built direct GSM SMS dispatcher without external web-service requirements.",
      ],
      binary: "ResQLink-v1.0.0.apk (28.4 MB)",
    },
  ];

  return (
    <section className="py-20 bg-white border-b border-[#E8E5DD]" id="updates">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#17212D] text-white text-xs font-bold uppercase tracking-wider">
            <GitCommit className="w-3.5 h-3.5 text-[#187A61]" />
            <span>Traceable Release Changelog</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            Version History & Roadmap:{" "}
            <span className="text-[#D62839]">Active Continuous Engineering</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            ResQLink AI is actively maintained and rigorously tested (15/15 unit test suite passing). Inspect verified build milestones and release notes below.
          </p>
        </div>

        {/* Vertical Timeline */}
        <div className="max-w-3xl mx-auto space-y-6">
          {releases.map((rel) => {
            const isExpanded = expandedVersion === rel.version;
            return (
              <div
                key={rel.version}
                className="bg-[#FAF9F7] rounded-3xl border border-[#E8E5DD] overflow-hidden shadow-2xs hover:shadow-xs transition-all"
              >
                <button
                  type="button"
                  onClick={() =>
                    setExpandedVersion(isExpanded ? "" : rel.version)
                  }
                  className="w-full p-6 text-left flex items-center justify-between gap-4 hover:bg-[#F3F1EC] transition-colors"
                >
                  <div className="flex items-center gap-3.5">
                    <div className="w-10 h-10 rounded-2xl bg-white border border-[#E8E5DD] flex items-center justify-center text-[#17212D] font-bold text-xs shrink-0">
                      {rel.version}
                    </div>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span
                          className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${rel.badgeColor}`}
                        >
                          {rel.status}
                        </span>
                        <span className="text-xs text-[#667085] font-medium">
                          {rel.date}
                        </span>
                      </div>
                      <h3 className="text-base font-bold text-[#17212D]">
                        {rel.headline}
                      </h3>
                    </div>
                  </div>

                  <span className="text-sm text-[#667085]">
                    {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                  </span>
                </button>

                {isExpanded && (
                  <div className="px-6 pb-6 pt-2 border-t border-[#E8E5DD] space-y-4">
                    <div className="space-y-2">
                      <span className="text-xs font-bold uppercase tracking-wider text-[#17212D] block">
                        Engineered Changes & Invariants:
                      </span>
                      <ul className="space-y-1.5 text-xs text-[#667085]">
                        {rel.highlights.map((h, i) => (
                          <li key={i} className="flex items-start gap-2 leading-relaxed">
                            <CheckCircle2 className="w-3.5 h-3.5 text-[#187A61] shrink-0 mt-0.5" />
                            <span>{h}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    <div className="pt-3 border-t border-[#E8E5DD] flex items-center justify-between text-xs text-[#667085] font-mono">
                      <span>Artifact: {rel.binary}</span>
                      <a
                        href="#download"
                        className="text-[#D62839] font-bold hover:underline"
                      >
                        Inspect Checksum →
                      </a>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
