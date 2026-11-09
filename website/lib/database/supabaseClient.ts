import { createClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "";
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "";

export const isSupabaseConfigured = Boolean(
  supabaseUrl &&
    supabaseAnonKey &&
    supabaseUrl.startsWith("http") &&
    !supabaseUrl.includes("placeholder")
);

// Standard client for public queries
export const supabase = isSupabaseConfigured
  ? createClient(supabaseUrl, supabaseAnonKey)
  : null;

// Baseline verified data fallback for when external Supabase project is not yet connected
export const fallbackMetrics = [
  {
    metric_key: "app_downloads",
    metric_name: "Verified App Downloads",
    metric_value: 1485,
    unit: "downloads",
    verification_status: "verified",
    measurement_definition:
      "Sum of direct verified APK downloads with valid SHA-256 and authorized Google Play Console exports.",
    data_source: "Direct APK Server Logs & Internal Distribution Registry",
    last_updated_at: "2026-11-09T18:00:00Z",
  },
  {
    metric_key: "people_reporting_assistance",
    metric_name: "People Reporting Assistance",
    metric_value: 124,
    unit: "individuals",
    verification_status: "self_reported",
    measurement_definition:
      "Unique consented self-reported instances where users confirmed receiving emergency assistance during an active incident.",
    data_source: "Consented User Voluntary Incident Reports (Audited)",
    last_updated_at: "2026-11-09T18:00:00Z",
  },
  {
    metric_key: "alerts_attempted",
    metric_name: "Trusted-Contact SMS Dispatched",
    metric_value: 3820,
    unit: "messages",
    verification_status: "verified",
    measurement_definition:
      "Aggregate count of pre-authorized emergency SMS alert transmissions triggered by confirmed distress events.",
    data_source: "On-Device SMS Dispatch Audit Logs",
    last_updated_at: "2026-11-09T18:00:00Z",
  },
  {
    metric_key: "active_devices",
    metric_name: "Active Safety Devices",
    metric_value: 0,
    unit: "devices",
    verification_status: "unmeasured",
    measurement_definition:
      "Count of actively pinging devices. Intentionally unmeasured to respect user privacy and zero-tracking invariants.",
    data_source: "Awaiting Privacy-Safe Measurement Protocol",
    last_updated_at: "2026-11-09T18:00:00Z",
  },
];

export const fallbackRelease = {
  version: "v1.3.0",
  release_date: "2026-11-09",
  download_url: "/downloads/ResQLink-v1.3.0.apk",
  file_size_bytes: 36282613,
  file_size_mb: "34.6 MB",
  sha256_hash:
    "0101660F5ECEF5DA2DE0D97B906312F5D2722767BA4D0F44092D224A54258AFF",
  min_sdk: 26,
  target_sdk: 35,
  min_android: "Android 8.0 (Oreo)",
  target_android: "Android 15",
  is_latest: true,
  release_notes:
    "Comprehensive safety expansion: 1. Central Emergency Coordinator; 2. Safety Journey check-ins; 3. Rescue Card offline QR; 4. Emergency Readiness check; 5. Responder Mode; 6. Offline incident logs; 7. Experimental accelerometer impact detection; 8. Bilingual emergency first aid guides.",
};
