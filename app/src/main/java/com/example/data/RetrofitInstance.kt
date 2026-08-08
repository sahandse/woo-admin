package com.example.data

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object RetrofitInstance {

    private const val DEFAULT_BASE_URL = "https://example.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun getRetrofit(baseUrl: String): Retrofit {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun getWooCommerceApi(baseUrl: String): WooCommerceApiService {
        return getRetrofit(baseUrl).create(WooCommerceApiService::class.java)
    }

    // Keep backward compatibility for existing service
    private val retrofit = Retrofit.Builder()
        .baseUrl(DEFAULT_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val wooCommerceApi: WooCommerceApiService = retrofit.create(WooCommerceApiService::class.java)

    // Cached API instance for the current store
    private var cachedBaseUrl: String? = null
    private var cachedApi: WooCommerceApiService? = null

    fun getOrCreateWooCommerceApi(baseUrl: String): WooCommerceApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedBaseUrl != normalizedUrl || cachedApi == null) {
            cachedBaseUrl = normalizedUrl
            cachedApi = getRetrofit(normalizedUrl).create(WooCommerceApiService::class.java)
        }
        return cachedApi!!
    }
}
