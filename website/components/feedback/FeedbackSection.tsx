"use client";

import { useState } from "react";
import {
  MessageSquare,
  AlertTriangle,
  Send,
  CheckCircle2,
  HelpCircle,
  Bug,
  Lightbulb,
  Eye,
  Globe,
} from "lucide-react";

export function FeedbackSection() {
  const [category, setCategory] = useState<
    "general" | "feature" | "bug" | "accessibility" | "website"
  >("general");
  const [description, setDescription] = useState("");
  const [email, setEmail] = useState("");
  const [consent, setConsent] = useState(true);
  const [websiteHp, setWebsiteHp] = useState(""); // Honeypot
  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isError, setIsError] = useState(false);
  const [isEmergencyAlert, setIsEmergencyAlert] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setStatusMessage(null);
    setIsError(false);
    setIsEmergencyAlert(false);

    try {
      const res = await fetch("/api/feedback", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          category,
          description,
          email,
          consent,
          website_hp: websiteHp,
        }),
      });

      const json = await res.json();

      if (!res.ok) {
        if (json.emergency_warning) {
          setIsEmergencyAlert(true);
        }
        setIsError(true);
        setStatusMessage(
          json.message ||
            "Please check the form fields and ensure the description is at least 10 characters."
        );
      } else {
        setIsError(false);
        setStatusMessage(
          "Maraming salamat! Your feedback has been safely submitted to the ResQLink engineering team."
        );
        setDescription("");
        setEmail("");
      }
    } catch {
      setIsError(true);
      setStatusMessage("Failed to submit feedback. Please check your network and try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="py-20 bg-white border-t border-[#E8E5DD]" id="feedback">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center space-y-4 mb-12">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#F8E8EA] text-[#D62839] text-xs font-bold uppercase tracking-wider">
            <MessageSquare className="w-3.5 h-3.5" />
            <span>Community Feedback & Bug Reporting</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            Help Shape <span className="text-[#D62839]">ResQLink AI</span>
          </h2>
          <p className="text-base text-[#667085] max-w-2xl mx-auto">
            Encountered an issue or have an architectural suggestion for offline resilience? Share your thoughts directly with the development team.
          </p>
        </div>

        {/* Form Container */}
        <div className="bg-[#FAF9F7] rounded-3xl p-6 sm:p-10 border border-[#E8E5DD] shadow-xs">
          {/* Non-Emergency Critical Safety Alert */}
          <div className="p-4 rounded-2xl bg-[#F8E8EA] border border-[#D62839]/30 text-xs text-[#17212D] mb-8 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-[#D62839] shrink-0 mt-0.5" />
            <div>
              <strong className="text-[#D62839] block text-sm">
                NOT AN EMERGENCY DISPATCH SERVICE
              </strong>
              <span>
                Do not submit ongoing distress or emergency incidents through this form. This website does not dispatch emergency personnel. If you need immediate rescue, dial <strong>911</strong> or Red Cross <strong>143</strong> right now.
              </span>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Honeypot field (hidden from legitimate users) */}
            <div className="hidden" aria-hidden="true">
              <label htmlFor="website_hp">Leave this empty</label>
              <input
                id="website_hp"
                type="text"
                value={websiteHp}
                onChange={(e) => setWebsiteHp(e.target.value)}
                tabIndex={-1}
                autoComplete="off"
              />
            </div>

            {/* Category Pills */}
            <div>
              <label className="text-xs font-bold text-[#17212D] uppercase tracking-wider block mb-2">
                Feedback Category:
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                {[
                  { id: "general", label: "General", icon: HelpCircle },
                  { id: "feature", label: "Feature Idea", icon: Lightbulb },
                  { id: "bug", label: "Bug Report", icon: Bug },
                  { id: "accessibility", label: "Accessibility", icon: Eye },
                  { id: "website", label: "Website Issue", icon: Globe },
                ].map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setCategory(item.id as any)}
                    className={`p-3 rounded-xl border text-xs font-semibold flex flex-col items-center justify-center gap-1.5 transition-all ${
                      category === item.id
                        ? "bg-[#17212D] text-white border-[#17212D] shadow-xs"
                        : "bg-white text-[#667085] border-[#E8E5DD] hover:border-[#D62839]"
                    }`}
                  >
                    <item.icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Description Area */}
            <div>
              <label
                htmlFor="feedback-description"
                className="text-xs font-bold text-[#17212D] uppercase tracking-wider block mb-1.5"
              >
                Detailed Description: <span className="text-[#D62839]">*</span>
              </label>
              <textarea
                id="feedback-description"
                rows={4}
                required
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe your feedback, observed bug, device model (e.g., Samsung A54, Android 14), or feature recommendation..."
                className="w-full p-4 rounded-xl bg-white border border-[#E8E5DD] text-sm text-[#17212D] focus:border-[#D62839] focus:ring-1 focus:ring-[#D62839] outline-none transition-all placeholder:text-[#98A2B3]"
              />
              <span className="text-[11px] text-[#667085] mt-1 block">
                Minimum 10 characters. Avoid entering sensitive personal medical details.
              </span>
            </div>

            {/* Optional Email */}
            <div>
              <label
                htmlFor="feedback-email"
                className="text-xs font-bold text-[#17212D] uppercase tracking-wider block mb-1.5"
              >
                Contact Email <span className="text-[#667085] font-normal">(Optional for follow-up)</span>:
              </label>
              <input
                id="feedback-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@example.com"
                className="w-full p-3 rounded-xl bg-white border border-[#E8E5DD] text-sm text-[#17212D] focus:border-[#D62839] focus:ring-1 focus:ring-[#D62839] outline-none transition-all placeholder:text-[#98A2B3]"
              />
            </div>

            {/* Consent Checkbox */}
            <div className="flex items-start gap-2.5 pt-1">
              <input
                id="feedback-consent"
                type="checkbox"
                required
                checked={consent}
                onChange={(e) => setConsent(e.target.checked)}
                className="mt-1 w-4 h-4 text-[#D62839] border-[#E8E5DD] rounded focus:ring-[#D62839]"
              />
              <label htmlFor="feedback-consent" className="text-xs text-[#667085] leading-relaxed">
                I acknowledge that this form is strictly for software feedback and bug reports, not emergency assistance, and consent to the review of this feedback in accordance with the Philippine Data Privacy Act of 2012.
              </label>
            </div>

            {/* Status Message Notification */}
            {statusMessage && (
              <div
                className={`p-4 rounded-xl text-xs flex items-start gap-2.5 ${
                  isError
                    ? isEmergencyAlert
                      ? "bg-[#D62839] text-white font-bold"
                      : "bg-[#F8E8EA] text-[#D62839] border border-[#D62839]/30"
                    : "bg-[#E7F6F1] text-[#187A61] border border-[#187A61]/30 font-semibold"
                }`}
              >
                {isError ? (
                  <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                ) : (
                  <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
                )}
                <span>{statusMessage}</span>
              </div>
            )}

            {/* Submit Button */}
            <div>
              <button
                type="submit"
                disabled={loading}
                className="w-full sm:w-auto px-8 py-3.5 bg-[#17212D] hover:bg-[#2A3644] active:bg-[#0D1520] text-white font-bold text-sm rounded-xl shadow-xs transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {loading ? (
                  <span>Submitting Feedback...</span>
                ) : (
                  <>
                    <Send className="w-4 h-4 text-[#D62839]" />
                    <span>Send Message to Team</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
  );
}
