package com.resqlink.ai.data.local

/**
 * Internal hardcoded configuration for ResQLink AI.
 *
 * Dedicated API Keys:
 * 1. GEMINI_LOCATION_API_KEY -> 5km AI Hotline Scanner & Location Discovery Radar
 * 2. GEMINI_VOICE_API_KEY    -> Backup Gemini 2.0 Audio Voice Engine
 * 3. ELEVENLABS_API_KEY      -> ElevenLabs Multilingual v2 Soft Voice (Tagalog & English)
 *
 * Keys placed here are compiled directly into the application binary
 * and are completely hidden from all UI screens and settings forms.
 */
object AppConfig {
    /**
     * Gemini Key #1: Location & 5km Radar Discovery
     * Used by Gemini 3.1 Flash-Lite to scan and verify local Barangay halls,
     * PNP precincts, and 24/7 Trauma ER hospitals.
     */
    const val GEMINI_LOCATION_API_KEY: String = ""

    /**
     * Gemini Key #2: Voice Synthesis & Dispatcher Q&A Speech (Secondary / Fallback)
     * Used by Gemini 2.0 Flash Audio ("Aoede", "Kore", "Fenrir", "Charon", "Puck")
     */
    const val GEMINI_VOICE_API_KEY: String = ""

    /**
     * ElevenLabs API Key:
     * High-fidelity, soft bilingual voice for emergency broadcast & dispatcher Q&A.
     */
    const val ELEVENLABS_API_KEY: String = ""

    /**
     * Default Soft Voice: Sarah (EXAVITQu4vr4xnSDxMaL)
     * Reassuring, warm, gentle tone supporting Tagalog and English via eleven_multilingual_v2.
     */
    const val DEFAULT_ELEVENLABS_VOICE_ID: String = "EXAVITQu4vr4xnSDxMaL"

    /**
     * ElevenLabs Multilingual Model ID
     */
    const val ELEVENLABS_MODEL_ID: String = "eleven_multilingual_v2"

    /**
     * Mapbox Access Token (Optional):
     * Used by the 5km Interactive Defense Map to render ultra-sharp Mapbox Dark-v11 tiles.
     * If blank, falls back to CartoDB Dark Matter & local radar grid.
     */
    const val MAPBOX_ACCESS_TOKEN: String = ""

    /**
     * Backward-compatible alias for single-key setups.
     */
    val GEMINI_API_KEY: String
        get() = GEMINI_LOCATION_API_KEY.ifBlank { GEMINI_VOICE_API_KEY }

    /**
     * Prebuilt Gemini Voice Name:
     * Options: "Aoede" (Default - Clear & Warm), "Kore" (Natural), "Fenrir" (Deep Male), "Charon" (Steady Male), "Puck" (Urgent)
     */
    const val DEFAULT_GEMINI_VOICE: String = "Aoede"
}
