"use client";

import { useState } from "react";
import {
  HeartPulse,
  Flame,
  Waves,
  Wind,
  ShieldAlert,
  Search,
  BookOpen,
  PhoneCall,
  CheckCircle2,
  AlertTriangle,
  Languages,
} from "lucide-react";
import { offlineGuides, EmergencyGuide } from "@/lib/guides/offlineGuidesData";

export function SafetyGuideViewer() {
  const [selectedLang, setSelectedLang] = useState<"en" | "fil">("en");
  const [selectedCategory, setSelectedCategory] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [expandedGuideId, setExpandedGuideId] = useState<string>("cpr-en");

  const categories = [
    { id: "all", label: "All Safety Protocols" },
    { id: "cpr", label: "CPR & Cardiac" },
    { id: "bleeding", label: "Bleeding & Trauma" },
    { id: "earthquake", label: "Earthquake (PHIVOLCS)" },
    { id: "typhoon", label: "Typhoon (PAGASA)" },
  ];

  const filteredGuides = offlineGuides.filter((guide) => {
    const matchesLang = guide.language === selectedLang;
    const matchesCat =
      selectedCategory === "all" || guide.category === selectedCategory;
    const matchesSearch =
      searchQuery.trim() === "" ||
      guide.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      guide.summary.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesLang && matchesCat && matchesSearch;
  });

  return (
    <section className="py-20 bg-[#FAF9F7]" id="safety">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-14">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#187A61]/15 text-[#187A61] text-xs font-bold uppercase tracking-wider">
            <BookOpen className="w-3.5 h-3.5" />
            <span>Authoritative Philippine Protocols</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            Safety Knowledge Center:{" "}
            <span className="text-[#187A61]">Offline First Aid & Disaster Guides</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            Verified life-safety guidance from the Philippine Red Cross (PRC), PHIVOLCS, and PAGASA. Bundled directly into the Android application binary for instant offline retrieval.
          </p>
        </div>

        {/* Filter Controls: Language Switcher & Category Tabs */}
        <div className="bg-white rounded-3xl p-4 sm:p-6 border border-[#E8E5DD] shadow-xs max-w-4xl mx-auto mb-10 flex flex-col md:flex-row items-center justify-between gap-4">
          {/* Category Tabs */}
          <div className="flex flex-wrap items-center gap-1.5 w-full md:w-auto">
            {categories.map((cat) => (
              <button
                key={cat.id}
                type="button"
                onClick={() => setSelectedCategory(cat.id)}
                className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-all ${
                  selectedCategory === cat.id
                    ? "bg-[#17212D] text-white shadow-xs"
                    : "bg-[#FAF9F7] text-[#667085] hover:text-[#17212D] border border-[#E8E5DD]"
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>

          {/* Language Switcher Pill */}
          <div className="flex items-center gap-1.5 bg-[#FAF9F7] p-1 rounded-2xl border border-[#E8E5DD] shrink-0">
            <button
              type="button"
              onClick={() => {
                setSelectedLang("en");
                setExpandedGuideId("cpr-en");
              }}
              className={`px-3 py-1 text-xs font-bold rounded-xl transition-all ${
                selectedLang === "en"
                  ? "bg-[#D62839] text-white shadow-xs"
                  : "text-[#667085] hover:text-[#17212D]"
              }`}
            >
              English
            </button>
            <button
              type="button"
              onClick={() => {
                setSelectedLang("fil");
                setExpandedGuideId("cpr-fil");
              }}
              className={`px-3 py-1 text-xs font-bold rounded-xl transition-all ${
                selectedLang === "fil"
                  ? "bg-[#D62839] text-white shadow-xs"
                  : "text-[#667085] hover:text-[#17212D]"
              }`}
            >
              Filipino / Tagalog
            </button>
          </div>
        </div>

        {/* Guides Accordion / List */}
        <div className="max-w-4xl mx-auto space-y-4">
          {filteredGuides.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-3xl border border-[#E8E5DD] text-[#667085]">
              No guides found matching this criteria. Try selecting "All Safety Protocols".
            </div>
          ) : (
            filteredGuides.map((guide) => {
              const isExpanded = expandedGuideId === guide.id;
              return (
                <div
                  key={guide.id}
                  className="bg-white rounded-3xl border border-[#E8E5DD] overflow-hidden shadow-2xs transition-all"
                >
                  <button
                    type="button"
                    onClick={() =>
                      setExpandedGuideId(isExpanded ? "" : guide.id)
                    }
                    className="w-full p-6 text-left flex items-center justify-between gap-4 hover:bg-[#FAF9F7]/60 transition-colors"
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-2xl bg-[#E7F6F1] text-[#187A61] flex items-center justify-center shrink-0">
                        <HeartPulse className="w-6 h-6" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-[#187A61] bg-[#187A61]/15 px-2 py-0.5 rounded">
                            {guide.category.toUpperCase()}
                          </span>
                          <span className="text-xs text-[#667085]">
                            Source: {guide.authoritativeSource}
                          </span>
                        </div>
                        <h3 className="text-lg font-bold text-[#17212D] leading-tight">
                          {guide.title}
                        </h3>
                      </div>
                    </div>

                    <span className="text-xl font-bold text-[#667085]">
                      {isExpanded ? "−" : "+"}
                    </span>
                  </button>

                  {isExpanded && (
                    <div className="px-6 pb-6 pt-2 border-t border-[#E8E5DD] space-y-6">
                      <p className="text-sm text-[#667085] leading-relaxed">
                        {guide.summary}
                      </p>

                      {/* Step by step cards */}
                      <div className="space-y-3">
                        <h4 className="text-xs font-bold text-[#17212D] uppercase tracking-wider">
                          Action Steps:
                        </h4>
                        <div className="grid grid-cols-1 gap-2.5">
                          {guide.steps.map((step) => (
                            <div
                              key={step.stepNumber}
                              className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] flex items-start gap-3.5"
                            >
                              <span className="w-7 h-7 rounded-full bg-[#17212D] text-white flex items-center justify-center text-xs font-bold shrink-0 mt-0.5">
                                {step.stepNumber}
                              </span>
                              <div className="space-y-1">
                                <h5 className="font-bold text-sm text-[#17212D]">
                                  {step.title}
                                </h5>
                                <p className="text-xs text-[#667085] leading-relaxed">
                                  {step.instruction}
                                </p>
                                {step.criticalNote && (
                                  <div className="pt-1 text-[11px] font-semibold text-[#D62839]">
                                    ⚠️ {step.criticalNote}
                                  </div>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* What NOT to do */}
                      {guide.whatNotToDo.length > 0 && (
                        <div className="p-4 rounded-2xl bg-[#F8E8EA]/60 border border-[#D62839]/20 space-y-2">
                          <div className="flex items-center gap-2 text-xs font-bold text-[#D62839] uppercase tracking-wider">
                            <AlertTriangle className="w-4 h-4" />
                            <span>Critical Errors to Avoid (Do NOT):</span>
                          </div>
                          <ul className="space-y-1 text-xs text-[#17212D] list-disc list-inside">
                            {guide.whatNotToDo.map((item, idx) => (
                              <li key={idx} className="leading-relaxed">
                                {item}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}

                      {/* Agency Hotline Footnote */}
                      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-2 text-xs text-[#667085]">
                        <span>Official Agency Citation: <strong>{guide.authoritativeSource}</strong></span>
                        <span className="font-bold text-[#D62839]">Direct Hotline: {guide.emergencyHotlineNumber}</span>
                      </div>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </section>
  );
}
