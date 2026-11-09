"use client";

import { useState } from "react";
import { MessageSquareQuote, Heart, MapPin, Calendar, ShieldCheck, Sparkles, Send } from "lucide-react";

interface Story {
  id: string;
  title: string;
  summary: string;
  content: string;
  location: string;
  author: string;
  date: string;
  verified: boolean;
}

export function CommunityStories() {
  const [stories] = useState<Story[]>([
    {
      id: "story-1",
      title: "Flash Flood Stoppage During Typhoon Carina",
      summary: "Used offline flood safety protocol and trusted SMS while mobile data networks collapsed in Marikina.",
      content:
        "When the flash floods hit our street in Marikina, 4G internet went completely dead within 20 minutes. I couldn't open messaging apps or browse online. I pulled up ResQLink's offline flood guide to check electrical shut-off steps and triggered a trusted-contact alert. My brother received my exact coordinates via cellular SMS and picked us up with a boat.",
      location: "Marikina, Metro Manila",
      author: "Mark R. (Verified Commuter)",
      date: "Oct 2026",
      verified: true,
    },
    {
      id: "story-2",
      title: "Late Night Commute Peace of Mind",
      summary: "University student uses Safety Journey 30-minute timer during late-night jeepney travel.",
      content:
        "As a female student taking late night jeepneys in QC, I always worry about unlit roads or sudden trouble. Setting a 30-minute Safety Journey with a 5-minute grace period gives me real reassurance. If I don't tap 'I'm Safe' when arriving at my dorm, my mom gets an automated SMS with my last location. I've never had to trigger SOS, but knowing it's there is life-changing.",
      location: "Quezon City",
      author: "Janelle M. (College Student)",
      date: "Nov 2026",
      verified: true,
    },
    {
      id: "story-3",
      title: "Paramedic Scanned Offline Rescue Card",
      summary: "First responder checked blood type and penicillin allergy directly via QR without needing patient unlock.",
      content:
        "I was on scene for a motorcycle skid in Cebu. The rider was disoriented. We saw the ResQLink Rescue Card QR sticker on his helmet and lock screen. Our camera scanned it instantly offline and showed his Type B+ blood and severe penicillin allergy. Having that offline data on-device prevented a critical medication error before the hospital handover.",
      location: "Cebu City",
      author: "Capt. Danilo T. (Community Volunteer EMT)",
      date: "Nov 2026",
      verified: true,
    },
  ]);

  return (
    <section className="py-20 bg-white border-b border-[#E8E5DD]" id="stories">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4 mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#F8E8EA] text-[#D62839] text-xs font-bold uppercase tracking-wider">
            <Heart className="w-3.5 h-3.5 fill-[#D62839]" />
            <span>Community Experiences</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-[#17212D] tracking-tight">
            Beyond the Numbers:{" "}
            <span className="text-[#D62839]">Every Number Has a Human Story</span>
          </h2>
          <p className="text-base sm:text-lg text-[#667085]">
            Consented, reviewed experiences from people across the Philippines who rely on ResQLink AI during typhoons, late-night travels, and medical emergencies.
          </p>
        </div>

        {/* Stories Editorial Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {stories.map((story) => (
            <div
              key={story.id}
              className="bg-[#FAF9F7] rounded-3xl p-6 sm:p-8 border border-[#E8E5DD] shadow-2xs hover:shadow-md hover:border-[#D62839]/30 transition-all flex flex-col justify-between"
            >
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-2xl bg-white border border-[#E8E5DD] flex items-center justify-center text-[#D62839]">
                    <MessageSquareQuote className="w-5 h-5" />
                  </div>
                  {story.verified && (
                    <span className="inline-flex items-center gap-1 text-[11px] font-bold text-[#187A61] bg-[#E7F6F1] px-2.5 py-0.5 rounded-full border border-[#187A61]/20">
                      <ShieldCheck className="w-3 h-3" />
                      <span>Consented & Audited</span>
                    </span>
                  )}
                </div>

                <h3 className="text-lg font-bold text-[#17212D] leading-snug">
                  {story.title}
                </h3>

                <p className="text-xs text-[#667085] leading-relaxed italic">
                  "{story.content}"
                </p>
              </div>

              <div className="pt-6 mt-6 border-t border-[#E8E5DD] flex items-center justify-between text-xs text-[#667085]">
                <div className="space-y-0.5">
                  <span className="font-bold text-[#17212D] block">{story.author}</span>
                  <div className="flex items-center gap-1 text-[11px]">
                    <MapPin className="w-3 h-3 text-[#D62839]" />
                    <span>{story.location}</span>
                  </div>
                </div>
                <div className="flex items-center gap-1 text-[11px] font-mono">
                  <Calendar className="w-3 h-3" />
                  <span>{story.date}</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Moderation Disclosure */}
        <div className="mt-12 p-4 rounded-2xl bg-[#FAF9F7] border border-[#E8E5DD] max-w-2xl mx-auto text-center text-xs text-[#667085]">
          <span className="font-bold text-[#17212D]">Moderation & Privacy Protection:</span> All stories undergo human review before publication. Private details, phone numbers, medical histories, and precise coordinates are strictly excluded from public display.
        </div>
      </div>
    </section>
  );
}
