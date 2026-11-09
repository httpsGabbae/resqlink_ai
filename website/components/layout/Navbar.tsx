"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import { Menu, X, Download, ShieldAlert, Activity, WifiOff } from "lucide-react";

export function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const navLinks = [
    { name: "Experience Demo", href: "#simulation", icon: Activity },
    { name: "Offline Engine", href: "#connectivity", icon: WifiOff },
    { name: "Features", href: "#features" },
    { name: "Live Impact", href: "#impact" },
    { name: "Architecture", href: "#technology" },
    { name: "Safety Guides", href: "#safety" },
    { name: "Updates", href: "#updates" },
  ];

  return (
    <header
      className={`sticky top-0 z-50 transition-all duration-300 ${
        scrolled
          ? "bg-[#FAF9F7]/95 backdrop-blur-md shadow-xs border-b border-[#E8E5DD]"
          : "bg-[#FAF9F7] border-b border-transparent"
      }`}
    >
      {/* Top emergency awareness strip */}
      <div className="bg-[#17212D] text-[#FAF9F7] text-xs py-1.5 px-4 text-center font-medium flex items-center justify-center gap-2">
        <span className="inline-block w-2 h-2 rounded-full bg-[#187A61] animate-pulse" />
        <span>Philippine Emergency Hotlines: National 911 • Red Cross 143 • BFP Fire (02) 8426-0219</span>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-18">
          {/* Logo & Brand */}
          <Link
            href="/"
            className="flex items-center gap-3 group focus-visible:ring-2 focus-visible:ring-[#D62839] rounded-lg p-1"
            aria-label="ResQLink AI Home"
          >
            <div className="relative w-10 h-10 rounded-xl overflow-hidden shadow-xs border border-[#E8E5DD] bg-white flex items-center justify-center group-hover:border-[#D62839] transition-colors">
              <Image
                src="/brand/resqlink_logo.jpg"
                alt="ResQLink AI Logo"
                width={40}
                height={40}
                className="object-cover"
                priority
              />
            </div>
            <div className="flex flex-col">
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-lg text-[#17212D] tracking-tight group-hover:text-[#D62839] transition-colors">
                  ResQLink
                </span>
                <span className="text-xs font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-[#F8E8EA] text-[#D62839]">
                  AI
                </span>
              </div>
              <span className="text-[11px] text-[#667085] font-medium leading-none">
                Offline-First Life Safety
              </span>
            </div>
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden lg:flex items-center gap-1 xl:gap-2" aria-label="Main Navigation">
            {navLinks.map((link) => (
              <a
                key={link.name}
                href={link.href}
                className="px-3 py-2 text-sm font-medium text-[#17212D]/80 hover:text-[#D62839] hover:bg-[#F8E8EA]/60 rounded-md transition-colors"
              >
                {link.name}
              </a>
            ))}
          </nav>

          {/* Right CTA */}
          <div className="hidden sm:flex items-center gap-3">
            <a
              href="#simulation"
              className="px-3.5 py-2 text-sm font-semibold text-[#17212D] hover:text-[#D62839] bg-white border border-[#E8E5DD] hover:border-[#D62839] rounded-lg transition-all shadow-2xs"
            >
              Simulate SOS
            </a>
            <a
              href="#download"
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-[#D62839] hover:bg-[#BF2030] active:bg-[#9E1927] rounded-lg shadow-sm hover:shadow-md transition-all"
            >
              <Download className="w-4 h-4" />
              <span>Get APK v1.3.0</span>
            </a>
          </div>

          {/* Mobile Menu Toggle */}
          <div className="flex sm:hidden items-center gap-2">
            <a
              href="#download"
              className="p-2 text-white bg-[#D62839] rounded-lg shadow-xs"
              aria-label="Download ResQLink APK"
            >
              <Download className="w-5 h-5" />
            </a>
            <button
              type="button"
              onClick={() => setIsOpen(!isOpen)}
              className="p-2 text-[#17212D] hover:text-[#D62839] rounded-lg border border-[#E8E5DD] bg-white"
              aria-expanded={isOpen}
              aria-label="Toggle navigation menu"
            >
              {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Drawer */}
      {isOpen && (
        <div className="sm:hidden bg-[#FAF9F7] border-b border-[#E8E5DD] px-4 pt-3 pb-6 space-y-2 shadow-lg animate-fadeIn">
          {navLinks.map((link) => (
            <a
              key={link.name}
              href={link.href}
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 px-3 py-2.5 text-base font-medium text-[#17212D] hover:text-[#D62839] hover:bg-[#F8E8EA] rounded-lg"
            >
              {link.icon && <link.icon className="w-4 h-4 text-[#D62839]" />}
              <span>{link.name}</span>
            </a>
          ))}
          <div className="pt-3 border-t border-[#E8E5DD] flex flex-col gap-2">
            <a
              href="#simulation"
              onClick={() => setIsOpen(false)}
              className="w-full text-center py-2.5 text-sm font-semibold text-[#17212D] bg-white border border-[#E8E5DD] rounded-lg"
            >
              Simulate Emergency Workflow
            </a>
            <a
              href="#download"
              onClick={() => setIsOpen(false)}
              className="w-full flex items-center justify-center gap-2 py-2.5 text-sm font-semibold text-white bg-[#D62839] rounded-lg shadow-sm"
            >
              <Download className="w-4 h-4" />
              <span>Download ResQLink v1.3.0</span>
            </a>
          </div>
        </div>
      )}
    </header>
  );
}
