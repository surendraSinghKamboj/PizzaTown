package com.pizzatown.customer.core.cloudinary

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Uploads a customer profile photo directly to Cloudinary (unsigned preset, no secret in the app). */
@Singleton
class CloudinaryUploader @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun uploadImage(imageBytes: ByteArray, publicIdHint: String): Result<String> = runCatching {
        if (!CloudinaryConfig.isConfigured()) {
            throw IllegalStateException(
                "Cloudinary isn't configured yet. Set cloudName and uploadPreset in CloudinaryConfig.kt (see README §5)."
            )
        }
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", CloudinaryConfig.uploadPreset)
            .addFormDataPart("public_id", "profile_$publicIdHint")
            .addFormDataPart(
                "file", "$publicIdHint.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(CloudinaryConfig.uploadUrl())
            .post(requestBody)
            .build()

        val responseBody = executeAsync(request)
        val json = JSONObject(responseBody)
        if (json.has("error")) {
            val message = json.getJSONObject("error").optString("message", "Upload failed")
            throw IOException("Cloudinary upload failed: $message")
        }
        json.getString("secure_url")
    }

    private suspend fun executeAsync(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (!it.isSuccessful && body.isBlank()) {
                        continuation.resumeWithException(IOException("Upload failed with HTTP ${it.code}"))
                    } else {
                        continuation.resume(body)
                    }
                }
            }
        })
    }
}
