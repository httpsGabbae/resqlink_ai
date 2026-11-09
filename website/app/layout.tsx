import type { Metadata, Viewport } from "next";
import { Manrope, Inter } from "next/font/google";
import "./globals.css";
import { Navbar } from "@/components/layout/Navbar";
import { Footer } from "@/components/layout/Footer";
import { LifelineProgress } from "@/components/layout/LifelineProgress";

const manrope = Manrope({
  variable: "--font-manrope",
  subsets: ["latin"],
  display: "swap",
  weight: ["500", "600", "700", "800"],
});

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
  weight: ["400", "500", "600", "700"],
});

export const viewport: Viewport = {
  themeColor: "#D62839",
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
};

export const metadata: Metadata = {
  metadataBase: new URL("https://resqlink.ai"),
  title: "ResQLink AI — Offline-First Emergency Assistance for the Philippines",
  description:
    "A connection that matters when it matters most. ResQLink AI is an offline-first Android safety companion designed to recognize distress, help people request assistance, and connect them with trusted contacts across the Philippines.",
  keywords: [
    "ResQLink",
    "ResQLink AI",
    "offline emergency app Philippines",
    "distress voice detection",
    "emergency SOS android",
    "Philippine emergency hotlines 911 143",
    "disaster preparedness Philippines",
    "offline first safety",
  ],
  authors: [{ name: "ResQLink AI Engineering Team" }],
  creator: "ResQLink AI",
  publisher: "ResQLink AI",
  robots: "index, follow",
  openGraph: {
    type: "website",
    locale: "en_PH",
    url: "https://resqlink.ai",
    title: "ResQLink AI — The Living Impact Platform",
    description:
      "Offline-first Android emergency safety companion. On-device voice distress detection, manual SOS, offline Rescue Card, and trusted-contact coordination for the Philippines.",
    siteName: "ResQLink AI",
    images: [
      {
        url: "/brand/resqlink_logo.jpg",
        width: 800,
        height: 800,
        alt: "ResQLink AI Logo",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "ResQLink AI — The Living Impact Platform",
    description:
      "Offline-first Android emergency assistance companion for the Philippines. Detect, confirm, and connect when connectivity is compromised.",
    images: ["/brand/resqlink_logo.jpg"],
  },
  icons: {
    icon: "/brand/resqlink_logo.jpg",
    apple: "/images/ic_launcher.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${manrope.variable} ${inter.variable} scroll-smooth`}>
      <body className="min-h-screen flex flex-col bg-[#FAF9F7] text-[#17212D] antialiased">
        {/* Skip to Content for Screen Readers & Keyboard Access */}
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-100 focus:px-4 focus:py-2 focus:bg-[#D62839] focus:text-white focus:rounded-md focus:shadow-lg focus:outline-none"
        >
          Skip to main content
        </a>

        {/* Ambient Living Lifeline Scroll Indicator */}
        <LifelineProgress />

        {/* Sticky Header */}
        <Navbar />

        {/* Main Content Landmark */}
        <main id="main-content" className="flex-1">
          {children}
        </main>

        {/* Comprehensive Footer */}
        <Footer />
      </body>
    </html>
  );
}
