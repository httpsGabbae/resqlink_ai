import { HeroSection } from "@/components/hero/HeroSection";
import { EmergencySimulator } from "@/components/simulation/EmergencySimulator";
import { ConnectivityExplorer } from "@/components/connectivity/ConnectivityExplorer";
import { FeatureBentoGrid } from "@/components/features/FeatureBentoGrid";
import { ImpactDashboard } from "@/components/impact/ImpactDashboard";
import { ArchitectureExplorer } from "@/components/technology/ArchitectureExplorer";
import { CommunityStories } from "@/components/community/CommunityStories";
import { SafetyGuideViewer } from "@/components/safety/SafetyGuideViewer";
import { VersionTimeline } from "@/components/updates/VersionTimeline";
import { DownloadSection } from "@/components/download/DownloadSection";
import { FeedbackSection } from "@/components/feedback/FeedbackSection";

export default function HomePage() {
  return (
    <div className="flex flex-col">
      {/* 1. Hero & Signature Living Lifeline */}
      <HeroSection />

      {/* 2. Educational Emergency Simulation */}
      <EmergencySimulator />

      {/* 3. Interactive Connectivity Matrix */}
      <ConnectivityExplorer />

      {/* 4. Complete 10-Module Feature Bento Grid */}
      <FeatureBentoGrid />

      {/* 5. Live Public Impact Dashboard */}
      <ImpactDashboard />

      {/* 6. On-Device Architecture Explorer */}
      <ArchitectureExplorer />

      {/* 7. Beyond the Numbers: Community Stories */}
      <CommunityStories />

      {/* 8. Safety Knowledge Center */}
      <SafetyGuideViewer />

      {/* 9. Chronological Version History & Roadmap */}
      <VersionTimeline />

      {/* 10. Official Download Center */}
      <DownloadSection />

      {/* 11. Feedback & Bug Reporting */}
      <FeedbackSection />
    </div>
  );
}
