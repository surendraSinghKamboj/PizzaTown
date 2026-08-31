package com.pizzatown.admin.core.cloudinary

/**
 * Centralized Cloudinary configuration. Change these two values ONLY —
 * they are not duplicated anywhere else in the codebase.
 *
 * Cloudinary's free tier is used instead of Firebase Storage (which
 * requires a paid Blaze plan) — Firebase Auth + Firestore stay on the
 * free Spark plan, and images go through Cloudinary's free 25GB/month
 * tier instead.
 *
 * Setup (see README §5 for full steps):
 *   1. Create a free account at https://cloudinary.com
 *   2. Dashboard → copy your "Cloud name" → paste into [cloudName]
 *   3. Settings → Upload → Upload presets → Add upload preset →
 *      set "Signing Mode" to UNSIGNED → save → copy its name into
 *      [uploadPreset]
 *
 * Unsigned upload presets are Cloudinary's supported way to let a
 * mobile app upload directly without embedding any API secret in the
 * APK. You can restrict what an unsigned preset allows (folder, max
 * file size, allowed formats) from the Cloudinary dashboard.
 */
object CloudinaryConfig {
    const val cloudName: String = "lnlv61qw" // TODO: replace with your real Cloudinary cloud name
    const val uploadPreset: String = "ml_default" // TODO: replace with your real unsigned upload preset name

    fun uploadUrl(): String = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    /** True once both placeholders have been replaced with real values (see README §5). */
    fun isConfigured(): Boolean = cloudName != "your-cloud-name" && uploadPreset.isNotBlank()
}
