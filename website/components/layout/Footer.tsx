import Link from "next/link";
import Image from "next/image";
import { Shield, PhoneCall, AlertTriangle, ExternalLink, Heart, CheckCircle2 } from "lucide-react";

export function Footer() {
  return (
    <footer className="bg-[#17212D] text-[#FAF9F7] pt-16 pb-12 border-t border-[#2A3644]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Top Emergency Hotlines Banner */}
        <div className="bg-[#2A3644] rounded-2xl p-6 mb-12 border border-[#3D4A59]/60">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#D62839]/20 text-[#D62839] flex items-center justify-center shrink-0">
                <PhoneCall className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-base text-white">Philippine Emergency Directory</h4>
                <p className="text-xs text-[#98A2B3]">Immediate human-initiated dispatch numbers for emergency services.</p>
              </div>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 w-full md:w-auto text-xs">
              <a href="tel:911" className="bg-[#17212D] hover:bg-[#D62839] transition-colors p-2.5 rounded-lg border border-[#3D4A59] text-center">
                <span className="block text-[#98A2B3] text-[10px]">National</span>
                <span className="font-bold text-white text-sm">911</span>
              </a>
              <a href="tel:117" className="bg-[#17212D] hover:bg-[#D62839] transition-colors p-2.5 rounded-lg border border-[#3D4A59] text-center">
                <span className="block text-[#98A2B3] text-[10px]">PNP Police</span>
                <span className="font-bold text-white text-sm">117</span>
              </a>
              <a href="tel:143" className="bg-[#17212D] hover:bg-[#D62839] transition-colors p-2.5 rounded-lg border border-[#3D4A59] text-center">
                <span className="block text-[#98A2B3] text-[10px]">Red Cross</span>
                <span className="font-bold text-white text-sm">143 / 8527-000</span>
              </a>
              <a href="tel:16502" className="bg-[#17212D] hover:bg-[#D62839] transition-colors p-2.5 rounded-lg border border-[#3D4A59] text-center">
                <span className="block text-[#98A2B3] text-[10px]">DPWH Roads</span>
                <span className="font-bold text-white text-sm">165-02</span>
              </a>
            </div>
          </div>
        </div>

        {/* 4 Column Footer Navigation */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10 pb-12 border-b border-[#2A3644]">
          {/* Brand Col */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl overflow-hidden bg-white p-0.5">
                <Image
                  src="/brand/resqlink_logo.jpg"
                  alt="ResQLink AI"
                  width={40}
                  height={40}
                  className="rounded-lg object-cover"
                />
              </div>
              <span className="text-xl font-bold tracking-tight text-white">ResQLink AI</span>
            </div>
            <p className="text-sm text-[#98A2B3] leading-relaxed max-w-sm">
              The Living Impact Platform. An offline-first Android emergency safety companion designed to recognize distress, coordinate trusted alerts, and assist users across the Philippines without mandatory cloud reliance.
            </p>
            <div className="flex items-center gap-2 text-xs text-[#187A61] font-semibold bg-[#187A61]/10 px-3 py-1.5 rounded-lg w-fit border border-[#187A61]/20">
              <CheckCircle2 className="w-4 h-4" />
              <span>Offline-First • No Tracking • Zero Data Brokering</span>
            </div>
          </div>

          {/* Column 2: Platform */}
          <div>
            <h5 className="text-xs font-bold text-white uppercase tracking-wider mb-4">Platform</h5>
            <ul className="space-y-2.5 text-sm text-[#98A2B3]">
              <li><a href="#simulation" className="hover:text-white transition-colors">Emergency Simulation</a></li>
              <li><a href="#connectivity" className="hover:text-white transition-colors">Connectivity Matrix</a></li>
              <li><a href="#features" className="hover:text-white transition-colors">All 10 Core Modules</a></li>
              <li><a href="#technology" className="hover:text-white transition-colors">On-Device Architecture</a></li>
              <li><a href="#download" className="hover:text-white transition-colors">Download Center (v1.3.0)</a></li>
            </ul>
          </div>

          {/* Column 3: Trust & Impact */}
          <div>
            <h5 className="text-xs font-bold text-white uppercase tracking-wider mb-4">Trust & Impact</h5>
            <ul className="space-y-2.5 text-sm text-[#98A2B3]">
              <li><a href="#impact" className="hover:text-white transition-colors">Verified Public Metrics</a></li>
              <li><Link href="/transparency" className="hover:text-white transition-colors">Measurement Methodology</Link></li>
              <li><a href="#safety" className="hover:text-white transition-colors">Bilingual Safety Guides</a></li>
              <li><a href="#updates" className="hover:text-white transition-colors">Version History & Roadmap</a></li>
              <li><a href="#feedback" className="hover:text-white transition-colors">Submit Community Feedback</a></li>
            </ul>
          </div>

          {/* Column 4: Compliance & Legal */}
          <div>
            <h5 className="text-xs font-bold text-white uppercase tracking-wider mb-4">Governance</h5>
            <ul className="space-y-2.5 text-sm text-[#98A2B3]">
              <li><Link href="/privacy" className="hover:text-white transition-colors">Privacy Policy (DPA 2012)</Link></li>
              <li><Link href="/terms" className="hover:text-white transition-colors">Terms of Safe Use</Link></li>
              <li><Link href="/accessibility" className="hover:text-white transition-colors">Accessibility (WCAG 2.2)</Link></li>
              <li><a href="#download" className="hover:text-white transition-colors">APK Cryptographic Hash</a></li>
            </ul>
          </div>
        </div>

        {/* Safety Disclaimer Warning */}
        <div className="pt-8 pb-6 flex items-start gap-3 text-xs text-[#98A2B3] leading-relaxed">
          <AlertTriangle className="w-5 h-5 text-[#D62839] shrink-0 mt-0.5" />
          <p>
            <strong className="text-white">Emergency Disclaimer:</strong> ResQLink AI is an assistive software tool. It cannot guarantee physical rescue, cellular signal propagation, or emergency response outcomes. It is not an official public safety answering point (PSAP) and does not automatically dial 911 on countdown timeouts. In life-threatening emergencies, dial 911 or contact local authorities directly if you are able.
          </p>
        </div>

        {/* Bottom Credits */}
        <div className="pt-6 border-t border-[#2A3644] flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-[#667085]">
          <p>© 2026 ResQLink AI. Built with care for Philippine safety & resilience.</p>
          <p className="flex items-center gap-1">
            Engineered with <Heart className="w-3.5 h-3.5 text-[#D62839] fill-[#D62839]" /> for offline reliability.
          </p>
        </div>
      </div>
    </footer>
  );
}
