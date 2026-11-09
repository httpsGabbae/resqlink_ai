"use client";

import { useState } from "react";
import Image from "next/image";
import {
  Download,
  ShieldCheck,
  FileCheck,
  Smartphone,
  Copy,
  Check,
  ExternalLink,
  QrCode,
  Info,
  AlertTriangle,
} from "lucide-react";

export function DownloadSection() {
  const [copied, setCopied] = useState(false);
  const [showQrModal, setShowQrModal] = useState(false);

  const sha256 =
    "0101660F5ECEF5DA2DE0D97B906312F5D2722767BA4D0F44092D224A54258AFF";

  const handleCopyHash = () => {
    navigator.clipboard.writeText(sha256);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <section className="py-20 bg-[#FAF9F7]" id="download">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#187A61]/15 text-[#187A61] text-xs font-bold uppercase tracking-wider">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Cryptographically Verified Distribution</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            Download ResQLink AI:{" "}
            <span className="text-[#D62839]">Your Offline Safety Companion</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            Obtain the official Android application package (APK). Every binary release is cryptographically signed and SHA-256 verified to protect against tampering.
          </p>
        </div>

        {/* Central Download Card */}
        <div className="max-w-4xl mx-auto bg-white rounded-3xl p-6 sm:p-10 border border-[#E8E5DD] shadow-lg">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-center">
            {/* Left: App Identity & Download CTA */}
            <div className="md:col-span-7 space-y-6">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl overflow-hidden bg-white p-1 shadow-xs border border-[#E8E5DD]">
                  <Image
                    src="/brand/resqlink_logo.jpg"
                    alt="ResQLink AI"
                    width={64}
                    height={64}
                    className="rounded-xl object-cover"
                  />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-2xl font-black text-[#17212D]">ResQLink AI</h3>
                    <span className="px-2 py-0.5 rounded-full bg-[#E7F6F1] text-[#187A61] font-bold text-xs border border-[#187A61]/30">
                      v1.3.0
                    </span>
                  </div>
                  <p className="text-xs text-[#667085]">
                    com.resqlink.ai • Native Android Kotlin Release
                  </p>
                </div>
              </div>

              {/* Technical Specifications Specs Grid */}
              <div className="grid grid-cols-2 gap-3 text-xs bg-[#FAF9F7] p-4 rounded-2xl border border-[#E8E5DD]">
                <div>
                  <span className="text-[#667085] block">File Size</span>
                  <strong className="text-[#17212D]">34.6 MB</strong>
                </div>
                <div>
                  <span className="text-[#667085] block">Compatibility</span>
                  <strong className="text-[#17212D]">Android 8.0 to 15 (API 26-35)</strong>
                </div>
                <div>
                  <span className="text-[#667085] block">Release Date</span>
                  <strong className="text-[#17212D]">November 9, 2026</strong>
                </div>
                <div>
                  <span className="text-[#667085] block">Target Architecture</span>
                  <strong className="text-[#17212D]">arm64-v8a / armeabi-v7a / x86_64</strong>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
                <a
                  href="/downloads/ResQLink-v1.3.0.apk"
                  download="ResQLink-v1.3.0.apk"
                  className="w-full sm:w-auto flex-1 inline-flex items-center justify-center gap-2.5 px-6 py-4 text-base font-bold text-white bg-[#D62839] hover:bg-[#BF2030] active:bg-[#9E1927] rounded-xl shadow-md hover:shadow-lg transition-all"
                >
                  <Download className="w-5 h-5" />
                  <span>Download APK (34.6 MB)</span>
                </a>

                <button
                  type="button"
                  onClick={() => setShowQrModal(true)}
                  className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-4 py-4 text-sm font-semibold text-[#17212D] bg-[#FAF9F7] hover:bg-[#F3F1EC] border border-[#E8E5DD] rounded-xl transition-colors"
                >
                  <QrCode className="w-4 h-4 text-[#D62839]" />
                  <span>Scan to Phone</span>
                </button>
              </div>

              {/* Google Play Honest Status Disclosure */}
              <div className="p-3 bg-[#FAF9F7] rounded-xl border border-[#E8E5DD] flex items-center justify-between text-xs text-[#667085]">
                <div className="flex items-center gap-2">
                  <Smartphone className="w-4 h-4 text-[#17212D]" />
                  <span>Google Play Store Listing:</span>
                </div>
                <span className="font-semibold text-[#D97706] bg-[#FEF3C7] px-2 py-0.5 rounded">
                  Pending Review & Certification
                </span>
              </div>
            </div>

            {/* Right: Security Checksum & Step-by-Step Installation */}
            <div className="md:col-span-5 bg-[#FAF9F7] p-6 rounded-2xl border border-[#E8E5DD] space-y-4">
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs font-bold text-[#17212D] uppercase tracking-wider flex items-center gap-1.5">
                    <FileCheck className="w-4 h-4 text-[#187A61]" />
                    <span>SHA-256 Checksum</span>
                  </span>
                  <button
                    type="button"
                    onClick={handleCopyHash}
                    className="text-[11px] text-[#D62839] hover:underline font-semibold flex items-center gap-1"
                  >
                    {copied ? <Check className="w-3 h-3 text-[#187A61]" /> : <Copy className="w-3 h-3" />}
                    <span>{copied ? "Copied" : "Copy"}</span>
                  </button>
                </div>
                <div className="p-2.5 bg-white rounded-xl border border-[#E8E5DD] font-mono text-[10px] text-[#667085] break-all select-all leading-tight">
                  {sha256}
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-[#E8E5DD]">
                <h4 className="text-xs font-bold text-[#17212D] uppercase tracking-wider">
                  How to Install the APK:
                </h4>
                <ol className="space-y-1.5 text-xs text-[#667085] list-decimal list-inside leading-relaxed">
                  <li>Tap <strong>Download APK</strong> above or scan the QR code with your phone.</li>
                  <li>When prompted by Android, tap <strong>Download anyway</strong>.</li>
                  <li>Open the completed download in your notifications.</li>
                  <li>If prompted, enable <em>"Allow from this source"</em> in your browser settings.</li>
                  <li>Tap <strong>Install</strong> to complete setup.</li>
                </ol>
              </div>

              <div className="p-3 bg-[#E7F6F1] rounded-xl text-[11px] text-[#187A61] flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 shrink-0 mt-0.5" />
                <span>
                  <strong>Safety Promise:</strong> ResQLink contains zero adware, zero tracking libraries, and requires no account creation or internet connection to function.
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* QR Code Modal for Phone Camera */}
        {showQrModal && (
          <div className="fixed inset-0 z-50 bg-[#17212D]/60 backdrop-blur-xs flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-sm w-full shadow-2xl border border-[#E8E5DD] text-center space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-base font-bold text-[#17212D]">Direct Mobile Download</h4>
                <button
                  type="button"
                  onClick={() => setShowQrModal(false)}
                  className="text-[#667085] hover:text-[#17212D] text-lg font-bold p-1"
                >
                  ✕
                </button>
              </div>

              <div className="bg-[#FAF9F7] p-6 rounded-2xl border border-[#E8E5DD] flex justify-center">
                {/* SVG QR Code Illustration */}
                <div className="w-48 h-48 bg-white p-3 rounded-xl border border-[#E8E5DD] shadow-inner flex flex-col items-center justify-center relative">
                  <QrCode className="w-36 h-36 text-[#17212D]" />
                  <div className="absolute inset-0 flex items-center justify-center">
                    <div className="w-8 h-8 rounded-lg bg-white p-0.5 border border-[#D62839] shadow-xs">
                      <Image
                        src="/brand/resqlink_logo.jpg"
                        alt="Logo"
                        width={32}
                        height={32}
                        className="rounded object-cover"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <p className="text-xs text-[#667085]">
                Open your Android Camera or QR reader and point at the screen to start the direct APK download on your device.
              </p>

              <button
                type="button"
                onClick={() => setShowQrModal(false)}
                className="w-full py-2.5 bg-[#17212D] text-white text-xs font-bold rounded-xl"
              >
                Close
              </button>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
