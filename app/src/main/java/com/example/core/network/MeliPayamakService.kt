package com.example.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object MeliPayamakService {

    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendSms(
        username: String,
        password: String,
        to: String,
        from: String,
        text: String
    ): SmsResult = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank() || to.isBlank() || text.isBlank()) {
            return@withContext SmsResult.Error("نام کاربری، کلمه عبور، گیرنده یا متن پیام نباید خالی باشد.")
        }

        // Clean up Persian / Arabic numbers to Standard English numbers for phone number sending
        val cleanedTo = to.replace("۰", "0")
            .replace("۱", "1")
            .replace("۲", "2")
            .replace("۳", "3")
            .replace("۴", "4")
            .replace("۵", "5")
            .replace("۶", "6")
            .replace("۷", "7")
            .replace("۸", "8")
            .replace("۹", "9")

        val url = "https://rest.payamak-panel.com/api/SendSMS/SendSimpleSMS"
        
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("to", cleanedTo)
            put("from", from.ifBlank { "500040001015" })
            put("text", text)
        }

        val requestBody = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                Log.d("MeliPayamakService", "Response Code: ${response.code}, Body: $bodyString")
                if (response.isSuccessful) {
                    // Responses from MeliPayamak usually return a numeric transaction code like "50001234567" on success or standard JSON codes.
                    // If it contains numerical string or positive response, it succeeded.
                    if (bodyString.contains("Value") || (bodyString.trim().toDoubleOrNull() ?: 0.0) > 100) {
                        SmsResult.Success("پیامک از طریق وب‌سرویس ملی‌پيامک با موفقیت ارسال شد.", bodyString)
                    } else if (bodyString.trim().toIntOrNull() != null && bodyString.trim().toInt() < 100) {
                        // Numeric error code returned
                        val errorDesc = getMeliPayamakErrorDescription(bodyString.trim().toInt())
                        SmsResult.Error("ملی‌پيامک خطای کد $bodyString را برگرداند: $errorDesc")
                    } else {
                        SmsResult.Success("پیامک ارسال شد.", bodyString)
                    }
                } else {
                    SmsResult.Error("خطا در برقراری ارتباط با پورتال ملی‌پیامک (کد ${response.code})")
                }
            }
        } catch (e: Exception) {
            Log.e("MeliPayamakService", "MeliPayamak API send failed", e)
            SmsResult.Error("خطای غیرمنتظره در شبکه: ${e.localizedMessage ?: "عدم اتصال به اینترنت"}")
        }
    }

    private fun getMeliPayamakErrorDescription(code: Int): String {
        return when (code) {
            0 -> "کاربر غیر فعال است"
            1 -> "ارسال با موفقیت انجام شد"
            2 -> "اعتبار کافی نیست"
            3 -> "کد کاربر یا رمز عبور اشتباه است"
            4 -> "خطا در سیستم یا شماره پیامک اشتباه است"
            5 -> "شماره فرستنده نامعتبر است"
            6 -> "محدودیت زمانی در ارسال وجود دارد"
            7 -> "متن پیام حاوی کلمات فیلتر شده است"
            11 -> "فرستنده فعال نیست"
            12 -> "کاربر مسدود شده است"
            else -> "خطای ناشناخته در پنل کاربری ملی‌پیامک. (کد خطا: $code)"
        }
    }
}

sealed class SmsResult {
    data class Success(val message: String, val rawResponse: String) : SmsResult()
    data class Error(val errorMessage: String) : SmsResult()
}
