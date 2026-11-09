import { neon } from "@neondatabase/serverless";
import { fallbackMetrics, fallbackRelease } from "@/lib/database/supabaseClient";

const databaseUrl = process.env.DATABASE_URL || "";

export const isNeonConfigured = Boolean(
  databaseUrl &&
    databaseUrl.startsWith("postgres") &&
    !databaseUrl.includes("placeholder")
);

// Returns Neon SQL query runner if DATABASE_URL is configured
export function getSql() {
  if (!isNeonConfigured) return null;
  return neon(databaseUrl);
}

// 1. Fetch Public Aggregate Metrics
export async function fetchPublicMetrics() {
  const sql = getSql();
  if (!sql) {
    return {
      source: "audited_local_baseline",
      metrics: fallbackMetrics,
    };
  }

  try {
    const rows = await sql`
      SELECT 
        metric_key,
        metric_name,
        metric_value,
        unit,
        verification_status,
        measurement_definition,
        data_source,
        last_updated_at
      FROM public_metrics
      ORDER BY metric_key ASC
    `;

    if (rows && rows.length > 0) {
      return {
        source: "neon_serverless_postgresql",
        metrics: rows.map((r: any) => ({
          metric_key: r.metric_key,
          metric_name: r.metric_name,
          metric_value: Number(r.metric_value),
          unit: r.unit,
          verification_status: r.verification_status,
          measurement_definition: r.measurement_definition,
          data_source: r.data_source,
          last_updated_at: r.last_updated_at,
        })),
      };
    }
  } catch (error) {
    console.error("Neon query error (falling back to baseline):", error);
  }

  return {
    source: "audited_local_baseline",
    metrics: fallbackMetrics,
  };
}

// 2. Fetch Latest Release Info
export async function fetchLatestRelease() {
  const sql = getSql();
  if (!sql) {
    return fallbackRelease;
  }

  try {
    const rows = await sql`
      SELECT * FROM app_releases WHERE is_latest = true LIMIT 1
    `;
    if (rows && rows.length > 0) {
      const r = rows[0] as any;
      return {
        version: r.version,
        release_date: r.release_date,
        download_url: r.download_url,
        file_size_bytes: Number(r.file_size_bytes),
        file_size_mb: `${(Number(r.file_size_bytes) / (1024 * 1024)).toFixed(1)} MB`,
        sha256_hash: r.sha256_hash,
        min_sdk: r.min_sdk,
        target_sdk: r.target_sdk,
        min_android: `Android ${r.min_sdk === 26 ? "8.0 (Oreo)" : r.min_sdk}`,
        target_android: `Android ${r.target_sdk === 35 ? "15" : r.target_sdk}`,
        is_latest: r.is_latest,
        release_notes: r.release_notes,
      };
    }
  } catch (error) {
    console.error("Neon release query error:", error);
  }

  return fallbackRelease;
}

// 3. Save Feedback Submission
export async function saveFeedback(data: {
  category: string;
  description: string;
  email?: string;
  ipHash: string;
  consent: boolean;
}) {
  const sql = getSql();
  if (!sql) {
    return { saved: false, reason: "local_only" };
  }

  try {
    await sql`
      INSERT INTO feedback_submissions (
        category,
        description,
        contact_email,
        ip_hash,
        consent_acknowledged
      ) VALUES (
        ${data.category},
        ${data.description},
        ${data.email || null},
        ${data.ipHash},
        ${data.consent}
      )
    `;
    return { saved: true };
  } catch (error) {
    console.error("Failed to insert feedback into Neon:", error);
    return { saved: false, error };
  }
}
