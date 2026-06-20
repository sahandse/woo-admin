package com.sahand.wooadmin.core.network

import android.util.Log
import com.sahand.wooadmin.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiManager {

    private const val TAG = "GeminiManager"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Moshi representations of standard Gemini REST API
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
    data class GenerateContentRequest(val contents: List<Content>)

    data class GenerateContentResponse(val candidates: List<Candidate>?)
    data class Candidate(val content: Content?)

    suspend fun generateAnalysis(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "خطا: کلید Gemini API تنظیم نشده است. لطفاً از پنل Secrets آن را پیکربندی کنید."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
        
        // Construct the payload objects
        val requestObj = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "شما تحلیل‌گر ارشد هوش هوشمند مدیریت ووکامرس هستید. به تمام درخواست‌ها با زبان فارسی شیک، رسا، دقیق، و متقاعدکننده پاسخ دهید. پاسخ شما مستقیماً به مدیر فروشگاه نشان داده می‌شود. بدون تکست انگلیسی اضافی.\n\nدرخواست مدیر:\n$prompt")
                    )
                )
            )
        )

        try {
            val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
            val jsonPayload = jsonAdapter.toJson(requestObj)

            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini call failed: $errBody")
                    return@withContext "خطا در ارتباط با هوش مصنوعی (کد ${response.code}). اتصال خود را بررسی کنید یا دوباره تلاش کنید."
                }

                val respBody = response.body?.string() ?: ""
                val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
                val responseObj = responseAdapter.fromJson(respBody)

                val generatedText = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!generatedText.isNullOrBlank()) {
                    return@withContext generatedText.trim()
                } else {
                    return@withContext "هیچ پاسخی از هوش مصنوعی دریافت نشد."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateAnalysis", e)
            return@withContext "خطایی رخ داد: ${e.localizedMessage ?: "عدم امکان اتصال به سرور هوش مصنوعی"}"
        }
    }
}
