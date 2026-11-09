import type { Metadata } from "next";
import Link from "next/link";
import { AlertTriangle, ArrowLeft, ShieldAlert, CheckCircle2 } from "lucide-react";

export const metadata: Metadata = {
  title: "Terms of Safe Use — ResQLink AI",
  description:
    "Terms of safe use, operational boundaries, and emergency service disclaimers for ResQLink AI.",
};

export default function TermsPage() {
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
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#F8E8EA] text-[#D62839] text-xs font-bold uppercase tracking-wider mb-3">
              <AlertTriangle className="w-3.5 h-3.5" />
              <span>Operational Terms & Limitations</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
              Terms of Safe Use & Emergency Disclaimers
            </h1>
            <p className="text-xs text-[#667085] mt-2 font-mono">
              Last Updated: November 9, 2026 • Version 2.0.0
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-[#F8E8EA] border border-[#D62839]/30 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-[#D62839] shrink-0 mt-0.5" />
            <p className="text-xs text-[#17212D] leading-relaxed">
              <strong>CRITICAL DISCLAIMER:</strong> ResQLink AI is an assistive technology application and is <em>NOT</em> a certified emergency dispatch system (PSAP). In any immediate life-threatening emergency, always dial <strong>911</strong> or contact your local barangay emergency hotline directly if you are able to do so.
            </p>
          </div>

          <div className="space-y-6 text-sm text-[#667085] leading-relaxed">
            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                1. Acceptance of Terms
              </h2>
              <p>
                By downloading, installing, or interacting with ResQLink AI or this website, you agree to comply with these Terms of Safe Use. If you do not accept these terms, do not install or use the software.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                2. No Guarantee of Emergency Rescue
              </h2>
              <p>
                ResQLink AI facilitates emergency communication between your device and designated trusted contacts. However, the software cannot guarantee:
              </p>
              <ul className="list-disc list-inside space-y-1 text-xs pl-2">
                <li>Cellular tower signal availability or SMS transmission speed by telecommunication carriers (Smart, Globe, DITO).</li>
                <li>Satellite visibility or immediate GPS fix accuracy in dense urban canyons, metal roofs, or basements.</li>
                <li>Physical rescue, response times, or actions taken by third-party emergency services or contacts.</li>
                <li>Continuous battery operation if phone power is exhausted or damaged during trauma.</li>
              </ul>
            </section>

            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                3. The 911 Non-Dialing Invariant
              </h2>
              <p>
                You explicitly acknowledge and agree that ResQLink AI follows an unyielding safety design policy: <strong>countdowns and timeout events will notify your authorized emergency contacts via SMS only</strong>. The software will never automatically place an outbound phone call to 911 or other emergency hotlines without your physical, direct tap on the screen.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                4. Experimental Features (Motion Detection)
              </h2>
              <p>
                Certain features within ResQLink AI, specifically accelerometer-assisted fall/crash impact detection, are designated as <strong>Experimental</strong>. These modules are disabled by default and require deliberate user opt-in. The creators of ResQLink AI disclaim liability for false positives or unrecorded impacts.
              </p>
            </section>

            <section className="space-y-2">
              <h2 className="text-lg font-bold text-[#17212D]">
                5. User Responsibilities
              </h2>
              <p>
                You are solely responsible for ensuring that:
              </p>
              <ul className="list-disc list-inside space-y-1 text-xs pl-2">
                <li>Your emergency contacts are informed and have consented to receive your emergency SMS notifications.</li>
                <li>Your cellular SIM has active calling/SMS privileges and sufficient regular load/balance if required by your carrier.</li>
                <li>Essential Android permissions (Location, SMS, Microphone, Exact Alarms) are maintained in an authorized state.</li>
              </ul>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}
