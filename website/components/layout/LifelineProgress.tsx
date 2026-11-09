"use client";

import { useEffect, useState } from "react";

export function LifelineProgress() {
  const [scrollProgress, setScrollProgress] = useState(0);

  useEffect(() => {
    const handleScroll = () => {
      const totalScroll = document.documentElement.scrollTop;
      const windowHeight =
        document.documentElement.scrollHeight - document.documentElement.clientHeight;
      if (windowHeight > 0) {
        const scroll = (totalScroll / windowHeight) * 100;
        setScrollProgress(Math.min(100, Math.max(0, scroll)));
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div
      className="fixed top-0 left-0 right-0 h-1 z-60 bg-transparent pointer-events-none"
      aria-hidden="true"
    >
      {/* Living Lifeline Track */}
      <div
        className="h-full bg-gradient-to-r from-[#D62839] via-[#BF2030] to-[#D62839] transition-all duration-150 ease-out relative"
        style={{ width: `${scrollProgress}%` }}
      >
        {/* Trailing Pulse Node */}
        <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 w-3 h-3 rounded-full bg-[#D62839] shadow-[0_0_8px_#D62839] animate-ping opacity-75" />
        <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 w-2 h-2 rounded-full bg-white border border-[#D62839]" />
      </div>
    </div>
  );
}
