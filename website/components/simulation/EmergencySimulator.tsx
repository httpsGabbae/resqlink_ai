"use client";

import { useState, useEffect } from "react";
import {
  Mic,
  AlertCircle,
  Send,
  RotateCcw,
  ShieldCheck,
  CheckCircle2,
  Clock,
  MapPin,
  MessageSquare,
  Sparkles,
  Info,
  Radio,
} from "lucide-react";

export function EmergencySimulator() {
  const [currentStage, setCurrentStage] = useState<1 | 2 | 3>(1);
  const [countdown, setCountdown] = useState<number>(10);
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const [simulatedLogs, setSimulatedLogs] = useState<string[]>([
    "[SIM-INIT] Safe browser simulation loaded. No real SMS or cellular calls will be transmitted.",
  ]);

  // Handle simulated 10-second countdown
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (isRunning && currentStage === 2 && countdown > 0) {
      timer = setTimeout(() => {
        setCountdown((prev) => prev - 1);
        setSimulatedLogs((prev) => [
          `[T-${countdown - 1}s] Awaiting user confirmation or cancellation...`,
          ...prev.slice(0, 8),
        ]);
      }, 1000);
    } else if (isRunning && currentStage === 2 && countdown === 0) {
      // Escalation timeout reached: transitions to Stage 3
      setCurrentStage(3);
      setSimulatedLogs((prev) => [
        "[SAFETY POLICY] 10s confirmation expired. Escalating to pre-authorized SMS dispatch.",
        "[ESC-CONTACTS] Dispatching simulated SMS with Google Maps coordinates to 2 trusted contacts.",
        ...prev.slice(0, 8),
      ]);
    }
    return () => clearTimeout(timer);
  }, [isRunning, currentStage, countdown]);

  const handleStartDistress = () => {
    setIsRunning(true);
    setCurrentStage(2);
    setCountdown(10);
    setSimulatedLogs((prev) => [
      "[TRIGGER] Simulated voice hotword matched: 'Resqlink Tulong!'",
      "[DETECTOR] Acoustic energy peak verified on-device without cloud API.",
      "[STATE] Transitioned to AWAITING_CONFIRMATION (10-second window)",
      ...prev.slice(0, 8),
    ]);
  };

  const handleManualConfirm = () => {
    setCurrentStage(3);
    setSimulatedLogs((prev) => [
      "[USER-ACTION] User explicitly tapped 'CONFIRM EMERGENCY NOW'",
      "[DISPATCH] Packaging incident INC-20261109-847291 with GPS fix ±4.2m",
      "[CONTACTS] Dispatching simulated SMS alerts...",
      ...prev.slice(0, 8),
    ]);
  };

  const handleCancel = () => {
    setIsRunning(false);
    setCurrentStage(1);
    setCountdown(10);
    setSimulatedLogs((prev) => [
      "[USER-ACTION] User tapped 'I'M SAFE' — Incident safely aborted.",
      "[STANDBY] Returning system to Guardian Listening Mode.",
      ...prev.slice(0, 8),
    ]);
  };

  const handleReset = () => {
    setIsRunning(false);
    setCurrentStage(1);
    setCountdown(10);
    setSimulatedLogs([
      "[SIM-RESET] Educational simulation restarted. Ready for testing.",
    ]);
  };

  return (
    <section className="py-20 bg-white border-y border-[#E8E5DD]" id="simulation">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#F8E8EA] text-[#D62839] text-xs font-bold uppercase tracking-wider">
            <Radio className="w-3.5 h-3.5 animate-pulse" />
            <span>Interactive Educational Sandbox</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            How ResQLink Protects You:{" "}
            <span className="text-[#D62839]">Detect → Confirm → Connect</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            Experience the three-stage emergency escalation lifecycle directly in your browser. This is an educational simulation — no real calls or SMS messages are sent.
          </p>
        </div>

        {/* 3-Step Interactive Process Visualizer */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12 relative">
          {/* Stage 1 Card */}
          <div
            className={`p-6 rounded-2xl border-2 transition-all ${
              currentStage === 1
                ? "bg-[#FAF9F7] border-[#D62839] shadow-md"
                : "bg-white border-[#E8E5DD] opacity-80"
            }`}
          >
            <div className="flex items-center justify-between mb-4">
              <span className="w-8 h-8 rounded-full bg-[#D62839] text-white flex items-center justify-center font-bold text-sm">
                1
              </span>
              <span
                className={`text-xs font-semibold px-2 py-0.5 rounded ${
                  currentStage === 1
                    ? "bg-[#D62839]/15 text-[#D62839]"
                    : "bg-[#E8E5DD] text-[#667085]"
                }`}
              >
                DETECT
              </span>
            </div>
            <h3 className="text-lg font-bold text-[#17212D] mb-1">Distress Recognition</h3>
            <p className="text-xs text-[#667085] leading-relaxed mb-4">
              The on-device acoustic model continuously evaluates ambient audio for distress phrases such as <em>"Resqlink Tulong!"</em> without streaming audio to cloud servers.
            </p>
            {currentStage === 1 && (
              <button
                type="button"
                onClick={handleStartDistress}
                className="w-full py-2.5 px-4 bg-[#D62839] hover:bg-[#BF2030] text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-2"
              >
                <Mic className="w-4 h-4" />
                <span>Simulate Voice Keyword Match</span>
              </button>
            )}
          </div>

          {/* Stage 2 Card */}
          <div
            className={`p-6 rounded-2xl border-2 transition-all ${
              currentStage === 2
                ? "bg-[#FAF9F7] border-[#D62839] shadow-md"
                : "bg-white border-[#E8E5DD] opacity-80"
            }`}
          >
            <div className="flex items-center justify-between mb-4">
              <span className="w-8 h-8 rounded-full bg-[#17212D] text-white flex items-center justify-center font-bold text-sm">
                2
              </span>
              <span
                className={`text-xs font-semibold px-2 py-0.5 rounded ${
                  currentStage === 2
                    ? "bg-[#D62839] text-white"
                    : "bg-[#E8E5DD] text-[#667085]"
                }`}
              >
                CONFIRM (10s)
              </span>
            </div>
            <h3 className="text-lg font-bold text-[#17212D] mb-1">Safety Escalation Policy</h3>
            <p className="text-xs text-[#667085] leading-relaxed mb-4">
              A 10-second audible countdown initiates. The user can dismiss false alarms with one tap. If unresponsive, ResQLink proceeds to pre-authorized SMS alerts.
            </p>
            {currentStage === 2 ? (
              <div className="space-y-2">
                <div className="text-center py-2 bg-white rounded-lg border border-[#D62839]/30">
                  <span className="text-2xl font-black text-[#D62839]">{countdown}s</span>
                  <span className="block text-[10px] text-[#667085]">Remaining to dispatch</span>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={handleManualConfirm}
                    className="py-2 bg-[#D62839] text-white font-bold text-xs rounded-lg shadow-2xs hover:bg-[#BF2030]"
                  >
                    Confirm Now
                  </button>
                  <button
                    type="button"
                    onClick={handleCancel}
                    className="py-2 bg-[#17212D] text-white font-semibold text-xs rounded-lg hover:bg-[#2A3644]"
                  >
                    I'm Safe
                  </button>
                </div>
              </div>
            ) : (
              <div className="text-xs text-[#667085] italic">Awaiting Stage 1 activation...</div>
            )}
          </div>

          {/* Stage 3 Card */}
          <div
            className={`p-6 rounded-2xl border-2 transition-all ${
              currentStage === 3
                ? "bg-[#FAF9F7] border-[#187A61] shadow-md"
                : "bg-white border-[#E8E5DD] opacity-80"
            }`}
          >
            <div className="flex items-center justify-between mb-4">
              <span className="w-8 h-8 rounded-full bg-[#187A61] text-white flex items-center justify-center font-bold text-sm">
                3
              </span>
              <span
                className={`text-xs font-semibold px-2 py-0.5 rounded ${
                  currentStage === 3
                    ? "bg-[#187A61] text-white"
                    : "bg-[#E8E5DD] text-[#667085]"
                }`}
              >
                CONNECT
              </span>
            </div>
            <h3 className="text-lg font-bold text-[#17212D] mb-1">Trusted-Contact Alert</h3>
            <p className="text-xs text-[#667085] leading-relaxed mb-4">
              SMS messages containing current GPS coordinates are transmitted directly over GSM/cellular networks without needing mobile internet data.
            </p>
            {currentStage === 3 ? (
              <div className="space-y-2">
                <div className="p-2.5 bg-[#E7F6F1] rounded-lg border border-[#187A61]/30 text-xs text-[#187A61] font-semibold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                  <span>2 Mock Contact Alerts Dispatched</span>
                </div>
                <button
                  type="button"
                  onClick={handleReset}
                  className="w-full py-2 bg-white border border-[#E8E5DD] hover:border-[#17212D] text-[#17212D] font-semibold text-xs rounded-lg flex items-center justify-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Restart Simulation</span>
                </button>
              </div>
            ) : (
              <div className="text-xs text-[#667085] italic">Follows Stage 2 completion...</div>
            )}
          </div>
        </div>

        {/* Live Simulation Console & SMS Preview Area */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 bg-[#17212D] rounded-3xl p-6 sm:p-8 text-white shadow-xl">
          {/* Left: SMS Preview Card */}
          <div className="lg:col-span-6 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <MessageSquare className="w-4 h-4 text-[#D62839]" />
                <h4 className="font-bold text-sm tracking-wide text-white">
                  Simulated SMS Payload Preview
                </h4>
              </div>
              <span className="text-[10px] text-[#187A61] bg-[#187A61]/20 px-2 py-0.5 rounded-full font-mono">
                GSM-7 Cellular Safe
              </span>
            </div>

            <div className="bg-[#101C36] rounded-2xl p-4 border border-[#263964] space-y-3 font-mono text-xs">
              <div className="flex items-center justify-between text-[#94A3B8] text-[11px] border-b border-[#263964] pb-2">
                <span>To: Maria Dela Cruz (+63 917 888 1234)</span>
                <span>Carrier: Smart / Globe</span>
              </div>
              <p className="text-slate-100 leading-relaxed font-sans text-xs">
                🚨 <strong className="text-white">EMERGENCY SOS:</strong> Juan Dela Cruz triggered distress assist near Quezon Memorial Circle, QC.
                <br />
                📍 <strong className="text-white">GPS Fix:</strong> 14.6507° N, 121.0509° E (±4.2m)
                <br />
                🗺️ <span className="text-[#38BDF8] underline">https://maps.google.com/?q=14.6507,121.0509</span>
                <br />
                🩺 <strong className="text-white">Rescue Card:</strong> Blood Type O+, Penicillin Allergy.
              </p>
              <div className="text-[10px] text-[#64748B] flex items-center justify-between pt-1">
                <span>Characters: 148 / 160 (1 SMS Segment)</span>
                <span>Zero Data Plan Required</span>
              </div>
            </div>

            <div className="p-3 bg-[#2A3644] rounded-xl text-xs text-[#98A2B3] flex items-start gap-2">
              <Info className="w-4 h-4 text-[#D62839] shrink-0 mt-0.5" />
              <p>
                <strong>Critical Invariant:</strong> ResQLink <em>never</em> automatically dials 911 or emergency hotlines on timeouts. Public dispatch calls remain 100% human-initiated to prevent emergency service spam.
              </p>
            </div>
          </div>

          {/* Right: Real-Time Event Audit Terminal */}
          <div className="lg:col-span-6 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-[#187A61] animate-pulse" />
                <h4 className="font-bold text-sm tracking-wide text-white">
                  State Machine Audit Log
                </h4>
              </div>
              <button
                type="button"
                onClick={handleReset}
                className="text-[11px] text-[#94A3B8] hover:text-white flex items-center gap-1"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Clear</span>
              </button>
            </div>

            <div className="bg-[#081226] rounded-2xl p-4 border border-[#263964] h-60 overflow-y-auto font-mono text-[11px] space-y-1.5 custom-scrollbar">
              {simulatedLogs.map((log, index) => (
                <div
                  key={index}
                  className={`leading-relaxed ${
                    log.includes("[USER-ACTION]")
                      ? "text-[#38BDF8]"
                      : log.includes("[TRIGGER]") || log.includes("[SAFETY POLICY]")
                      ? "text-[#D62839]"
                      : log.includes("[DISPATCH]") || log.includes("[ESC-CONTACTS]")
                      ? "text-[#10B981]"
                      : "text-[#94A3B8]"
                  }`}
                >
                  <span className="text-[#64748B] select-none mr-1.5">›</span>
                  {log}
                </div>
              ))}
            </div>

            <div className="text-[11px] text-[#64748B] text-center">
              Event Coordinator Model: <code>EmergencyEventManager.kt</code> (Idempotent Incident IDs)
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
