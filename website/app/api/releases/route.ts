import { NextResponse } from "next/server";
import { fetchLatestRelease } from "@/lib/database/dbClient";

export async function GET() {
  try {
    const release = await fetchLatestRelease();
    return NextResponse.json({
      success: true,
      release,
    });
  } catch {
    return NextResponse.json(
      { success: false, error: "Failed to fetch release info" },
      { status: 500 }
    );
  }
}
