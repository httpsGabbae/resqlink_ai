import { NextResponse } from "next/server";
import { fetchPublicMetrics } from "@/lib/database/dbClient";

export async function GET() {
  try {
    const { source, metrics } = await fetchPublicMetrics();

    return NextResponse.json(
      {
        success: true,
        source,
        last_updated: new Date().toISOString(),
        metrics: metrics.map((m) => ({
          key: m.metric_key,
          name: m.metric_name,
          value: m.metric_value,
          unit: m.unit,
          status: m.verification_status,
          definition: m.measurement_definition,
          source: m.data_source,
          updated_at: m.last_updated_at,
        })),
        methodology_url: "/transparency",
      },
      {
        headers: {
          "Cache-Control": "public, s-maxage=60, stale-while-revalidate=300",
        },
      }
    );
  } catch (error) {
    return NextResponse.json(
      {
        success: false,
        error: "Failed to retrieve public metrics",
      },
      { status: 500 }
    );
  }
}
