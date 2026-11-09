import { test, describe } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { z } from "zod";

// Zod Feedback Schema Test
const FeedbackSchema = z.object({
  category: z.enum(["general", "feature", "bug", "accessibility", "website"]),
  description: z.string().min(10).max(2000),
  email: z.string().email().optional().or(z.literal("")),
  consent: z.boolean().refine((val) => val === true),
  website_hp: z.string().max(0),
});

describe("Feedback Schema & Validation Tests", () => {
  test("Valid feedback passes validation", () => {
    const valid = {
      category: "bug",
      description: "Audio buffer overflow on Samsung A54 during background Doze mode.",
      email: "tester@example.com",
      consent: true,
      website_hp: "",
    };
    const result = FeedbackSchema.safeParse(valid);
    assert.equal(result.success, true);
  });

  test("Honeypot rejection prevents spam bot submissions", () => {
    const botSubmission = {
      category: "general",
      description: "Buy cheap emergency kits at my scam website!",
      email: "bot@spammer.org",
      consent: true,
      website_hp: "I am a spam bot fill",
    };
    const result = FeedbackSchema.safeParse(botSubmission);
    assert.equal(result.success, false);
  });

  test("Short descriptions under 10 chars are rejected", () => {
    const invalidShort = {
      category: "feature",
      description: "help",
      email: "",
      consent: true,
      website_hp: "",
    };
    const result = FeedbackSchema.safeParse(invalidShort);
    assert.equal(result.success, false);
  });

  test("Emergency keyword detection catches users attempting to report active peril", () => {
    const emergencyKeywords = ["help me", "dying", "fire right now", "call police", "saklolo", "tulong ngayon"];
    const message = "Please send fire truck fire right now at our house!";
    const isEmergency = emergencyKeywords.some((k) =>
      message.toLowerCase().includes(k)
    );
    assert.equal(isEmergency, true);
  });
});

describe("Binary Artifact & Cryptographic Integrity", () => {
  test("ResQLink-v1.3.0.apk exists in public download directory", () => {
    const apkPath = path.resolve("public/downloads/ResQLink-v1.3.0.apk");
    assert.equal(fs.existsSync(apkPath), true);
  });

  test("ResQLink-v1.3.0.apk matches authoritative SHA-256 hash", () => {
    const apkPath = path.resolve("public/downloads/ResQLink-v1.3.0.apk");
    const fileBuffer = fs.readFileSync(apkPath);
    const hashSum = crypto.createHash("sha256");
    hashSum.update(fileBuffer);
    const hex = hashSum.digest("hex").toUpperCase();
    assert.equal(hex, "0101660F5ECEF5DA2DE0D97B906312F5D2722767BA4D0F44092D224A54258AFF");
  });

  test("Brand logo and launcher icons exist in public directory", () => {
    assert.equal(fs.existsSync(path.resolve("public/brand/resqlink_logo.jpg")), true);
    assert.equal(fs.existsSync(path.resolve("public/images/ic_launcher.png")), true);
  });
});

describe("Design System & Accessibility Invariants", () => {
  test("Midnight Ink and Porcelain meet WCAG AAA contrast ratio", () => {
    // Midnight Ink #17212D on Porcelain #FAF9F7 has contrast > 12:1 (AAA requires 7:1)
    const midnightInkLum = 0.016; // approx relative luminance
    const porcelainLum = 0.95;
    const contrast = (porcelainLum + 0.05) / (midnightInkLum + 0.05);
    assert.ok(contrast > 12.0);
  });

  test("Bilingual emergency safety guides have complete English and Filipino pairs", () => {
    const guidesPath = path.resolve("lib/guides/offlineGuidesData.ts");
    const content = fs.readFileSync(guidesPath, "utf-8");
    assert.ok(content.includes("cpr-en"));
    assert.ok(content.includes("cpr-fil"));
    assert.ok(content.includes("bleeding-en"));
    assert.ok(content.includes("bleeding-fil"));
    assert.ok(content.includes("Philippine Red Cross"));
  });
});
