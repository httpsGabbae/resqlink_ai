import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { saveFeedback } from "@/lib/database/dbClient";

const FeedbackSchema = z.object({
  category: z.enum(["general", "feature", "bug", "accessibility", "website"]),
  description: z.string().min(10, "Description must be at least 10 characters").max(2000),
  email: z.string().email("Invalid email address").optional().or(z.literal("")),
  consent: z.boolean().refine((val) => val === true, {
    message: "You must consent to feedback processing",
  }),
  website_hp: z.string().max(0, "Bot detected"), // Honeypot field must remain empty
});

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const result = FeedbackSchema.safeParse(body);

    if (!result.success) {
      return NextResponse.json(
        {
          success: false,
          errors: result.error.flatten().fieldErrors,
        },
        { status: 400 }
      );
    }

    const { category, description, email, consent } = result.data;

    // Detect if user might be trying to report an ongoing active emergency
    const emergencyKeywords = ["help me", "dying", "fire right now", "call police", "saklolo", "tulong ngayon"];
    const isEmergency = emergencyKeywords.some((k) =>
      description.toLowerCase().includes(k)
    );

    if (isEmergency) {
      return NextResponse.json(
        {
          success: false,
          emergency_warning: true,
          message:
            "CRITICAL: This form is NOT monitored as an emergency dispatch service. If you or someone else is in immediate danger, DIAL 911 OR 143 (RED CROSS) IMMEDIATELY.",
        },
        { status: 422 }
      );
    }

    const clientIp = req.headers.get("x-forwarded-for") || "127.0.0.1";
    await saveFeedback({
      category,
      description,
      email: email || undefined,
      ipHash: "hash-" + clientIp.slice(0, 8),
      consent,
    });

    return NextResponse.json({
      success: true,
      message:
        "Maraming salamat! Your feedback has been safely received by the ResQLink engineering team.",
    });
  } catch (error) {
    return NextResponse.json(
      {
        success: false,
        message: "An error occurred while submitting feedback. Please try again.",
      },
      { status: 500 }
    );
  }
}
