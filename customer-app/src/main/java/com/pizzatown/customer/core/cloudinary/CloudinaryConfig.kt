package com.pizzatown.customer.core.cloudinary

/**
 * Centralized Cloudinary configuration for the customer app (profile
 * photos). Same Cloudinary account/cloud name as the admin app, but you
 * can use a separate unsigned upload preset if you want separate
 * folders/limits for customer-uploaded vs admin-uploaded images.
 *
 * See README §5 for full setup steps.
 */
object CloudinaryConfig {
    const val cloudName: String = "lnlv61qw" // TODO: replace with your real Cloudinary cloud name
    const val uploadPreset: String = "ml_default" // TODO: replace with your real unsigned upload preset name

    fun uploadUrl(): String = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    /** True once both placeholders have been replaced with real values (see README §5). */
    fun isConfigured(): Boolean = cloudName != "your-cloud-name" && uploadPreset.isNotBlank()
}
