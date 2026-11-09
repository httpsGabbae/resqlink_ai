import type { Metadata } from "next";
import Link from "next/link";
import { Eye, ArrowLeft, CheckCircle2, Sliders, Contrast, Volume2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Accessibility Statement — ResQLink AI",
  description:
    "ResQLink AI's commitment to accessibility, WCAG 2.2 Level AA compliance, and inclusive life-safety engineering.",
};

export default function AccessibilityPage() {
  return (
    <div className="py-16 sm:py-24 bg-[#FAF9F7]">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-xs font-bold text-[#D62839] hover:underline mb-8"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Home</span>
        </Link>

        <div className="bg-white rounded-3xl p-6 sm:p-12 border border-[#E8E5DD] shadow-xs space-y-8">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#187A61]/15 text-[#187A61] text-xs font-bold uppercase tracking-wider mb-3">
              <Eye className="w-3.5 h-3.5" />
              <span>WCAG 2.2 Level AA Targeted</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
              Accessibility Statement
            </h1>
            <p className="text-xs text-[#667085] mt-2 font-mono">
              Committed to Universal Emergency Access • Published Nov 2026
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-[#E7F6F1] border border-[#187A61]/20 flex items-start gap-3">
            <CheckCircle2 className="w-5 h-5 text-[#187A61] shrink-0 mt-0.5" />
            <p className="text-xs text-[#187A61] leading-relaxed">
              <strong>Inclusive Safety Principle:</strong> In an emergency, accessibility is not a feature — it is a life-or-death requirement. Both our Android application and this public website are designed to be accessible to people with diverse visual, auditory, cognitive, and motor abilities.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] space-y-2">
              <div className="flex items-center gap-2 text-[#17212D] font-bold text-sm">
                <Contrast className="w-4 h-4 text-[#D62839]" />
                <span>High-Contrast Surfaces</span>
              </div>
              <p className="text-xs text-[#667085] leading-relaxed">
                Midnight Ink (<code>#17212D</code>) on Porcelain (<code>#FAF9F7</code>) achieves a 12.8:1 contrast ratio, far surpassing the WCAG AA requirement of 4.5:1. Crimson text uses high-contrast borders and pill backgrounds.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] space-y-2">
              <div className="flex items-center gap-2 text-[#17212D] font-bold text-sm">
                <Sliders className="w-4 h-4 text-[#187A61]" />
                <span>Reduced Motion Respect</span>
              </div>
              <p className="text-xs text-[#667085] leading-relaxed">
                Our Living Lifeline animation and pulsing beacons automatically pause or switch to static representations whenever a user's operating system requests <code>prefers-reduced-motion: reduce</code>.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] space-y-2">
              <div className="flex items-center gap-2 text-[#17212D] font-bold text-sm">
                <CheckCircle2 className="w-4 h-4 text-[#38BDF8]" />
                <span>Color-Independent Cues</span>
              </div>
              <p className="text-xs text-[#667085] leading-relaxed">
                Critical states (active emergencies, offline badges, operational diagnostics) are never communicated solely by color. Every status indicator pairs a distinct icon with descriptive text.
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] space-y-2">
              <div className="flex items-center gap-2 text-[#17212D] font-bold text-sm">
                <Volume2 className="w-4 h-4 text-[#F59E0B]" />
                <span>Keyboard & Screen Readers</span>
              </div>
              <p className="text-xs text-[#667085] leading-relaxed">
                Every button, slider, and navigation tab features prominent focus outlines (2px solid #D62839) and ARIA attributes for NVDA, TalkBack, and VoiceOver screen readers.
              </p>
            </div>
          </div>

          <div className="pt-6 border-t border-[#E8E5DD] space-y-3 text-xs text-[#667085]">
            <h3 className="font-bold text-[#17212D] text-sm">Feedback on Accessibility</h3>
            <p>
              If you experience any accessibility barrier while browsing our site or using the Android app, please report it via our{" "}
              <Link href="/#feedback" className="text-[#D62839] font-bold hover:underline">
                Community Feedback Portal
              </Link>
              . We prioritize accessibility bug reports immediately.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
