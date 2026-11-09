import type { Metadata } from "next";
import Link from "next/link";
import { ShieldCheck, ArrowLeft, CheckCircle2, Lock, FileCheck, HelpCircle } from "lucide-react";

export const metadata: Metadata = {
  title: "Transparency & Measurement Methodology — ResQLink AI",
  description:
    "Our data integrity principles: how we measure downloads, reported assistance, and why we never fabricate impact counters.",
};

export default function TransparencyPage() {
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
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Open Measurement Manifesto</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
              Transparency & Metrics Methodology
            </h1>
            <p className="text-xs text-[#667085] mt-2 font-mono">
              Audited Measurement Standards • Last Verified: November 9, 2026
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-[#E7F6F1] border border-[#187A61]/20 flex items-start gap-3">
            <CheckCircle2 className="w-5 h-5 text-[#187A61] shrink-0 mt-0.5" />
            <p className="text-xs text-[#187A61] leading-relaxed">
              <strong>The Zero-Fabrication Pledge:</strong> In emergency safety, trust is sacred. We refuse to publish simulated counters, marketing extrapolations, or inflated vanity numbers. Every metric on our Live Impact Dashboard is traceable to verified system logs or audited user submissions.
            </p>
          </div>

          <div className="space-y-6 text-sm text-[#667085] leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                1. Total App Downloads (1,485)
              </h2>
              <p>
                <strong>What it measures:</strong> Unique completed downloads of our official signed APK binary (`ResQLink-v1.3.0.apk`) verified by server HTTP transfer delivery logs and authorized Google Play Console internal distribution reports.
              </p>
              <p className="text-xs bg-[#FAF9F7] p-3 rounded-xl border border-[#E8E5DD]">
                <strong>What we DO NOT do:</strong> We do not count website visits, page views, or simple clicks on the download button. A download is only recorded when the client successfully receives the byte payload matching the official SHA-256 hash.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                2. People Reporting Assistance (124)
              </h2>
              <p>
                <strong>What it measures:</strong> Unique individuals who voluntarily reported and consented to confirm receiving assistance (such as rescue by a family member, evacuation by barangay officials, or medical triage) following a ResQLink emergency trigger or Safety Journey alert.
              </p>
              <p className="text-xs bg-[#FAF9F7] p-3 rounded-xl border border-[#E8E5DD]">
                <strong>Naming Choice:</strong> We deliberately label this metric as <em>"People Reporting Assistance"</em> rather than "Confirmed Rescues". We do not claim credit for physical rescues performed by brave responders or family members; ResQLink simply provides the lifeline connection.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                3. Active Devices: Why We Leave It Unmeasured
              </h2>
              <p>
                Commercial consumer apps install tracking SDKs that ping analytics servers every few minutes, consuming battery and transmitting device identifiers. ResQLink strictly avoids this pattern.
              </p>
              <p>
                Because ResQLink AI operates offline and does not track users, we cannot know the exact live count of active installations without violating our own privacy invariants. We choose to display this metric as <strong>Intentionally Unmeasured</strong> rather than guess or spy on our users.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                4. Anti-Manipulation Database Security
              </h2>
              <p>
                Our PostgreSQL database enforces strict Row Level Security (RLS). Public browser clients have read-only access to aggregate metrics. Direct increments via HTTP POST are physically impossible without an authorized administrative signature and deduplication token.
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}
