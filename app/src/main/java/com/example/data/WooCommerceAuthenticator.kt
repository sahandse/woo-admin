package com.example.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow

class WooCommerceAuthenticator(private val context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        "wc_auth_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var consumerKey: String
        get() = prefs.getString("consumer_key", "") ?: ""
        set(value) = prefs.edit().putString("consumer_key", value).apply()

    var consumerSecret: String
        get() = prefs.getString("consumer_secret", "") ?: ""
        set(value) = prefs.edit().putString("consumer_secret", value).apply()

    var storeUrl: String
        get() = prefs.getString("store_url", "") ?: ""
        set(value) = prefs.edit().putString("store_url", value).apply()

    var jwtToken: String?
        get() = prefs.getString("jwt_token", null)
        set(value) = prefs.edit().putString("jwt_token", value).apply()

    var tokenExpiry: Long
        get() = prefs.getLong("token_expiry", 0)
        set(value) = prefs.edit().putLong("token_expiry", value).apply()

    fun isLoggedIn(): Boolean {
        return consumerKey.isNotBlank() && consumerSecret.isNotBlank() && storeUrl.isNotBlank()
    }

    fun clearAuth() {
        prefs.edit().clear().apply()
    }

    fun isTokenValid(): Boolean {
        val token = jwtToken ?: return false
        return System.currentTimeMillis() < tokenExpiry
    }
}
