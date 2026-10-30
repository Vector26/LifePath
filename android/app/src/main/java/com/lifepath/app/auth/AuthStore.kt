package com.lifepath.app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AuthStore {
    private const val PREF_NAME = "lifepath_prefs"
    private const val ENCRYPTED_PREF_NAME = "lifepath_creds_enc"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_API_URL = "api_url"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_SAVED_EMAIL = "saved_email"
    private const val KEY_SAVED_PASSWORD = "saved_password"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun encryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(ENCRYPTED_PREF_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getWebUrl(context: Context): String? {
        return prefs(context).getString(KEY_SERVER_URL, null)?.trim()?.trimEnd('/')
    }

    fun getApiUrl(context: Context): String? {
        val p = prefs(context)
        val api = p.getString(KEY_API_URL, null)?.trim()?.trimEnd('/')
        if (!api.isNullOrBlank()) return api
        return p.getString(KEY_SERVER_URL, null)?.trim()?.trimEnd('/')
    }

    fun getAccessToken(context: Context): String? {
        return prefs(context).getString(KEY_AUTH_TOKEN, null)?.trim()
    }

    fun getRefreshToken(context: Context): String? {
        return prefs(context).getString(KEY_REFRESH_TOKEN, null)?.trim()
    }

    fun isConfigured(context: Context): Boolean {
        return !getWebUrl(context).isNullOrBlank() && !getApiUrl(context).isNullOrBlank()
    }

    fun isAuthenticated(context: Context): Boolean {
        return !getAccessToken(context).isNullOrBlank()
    }

    fun saveUrls(context: Context, webUrl: String, apiUrl: String?) {
        val editor = prefs(context).edit()
            .putString(KEY_SERVER_URL, webUrl.trim().trimEnd('/'))
        val cleanApi = apiUrl?.trim()?.trimEnd('/').orEmpty()
        if (cleanApi.isBlank()) {
            editor.remove(KEY_API_URL)
        } else {
            editor.putString(KEY_API_URL, cleanApi)
        }
        editor.apply()
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String?) {
        val editor = prefs(context).edit()
            .putString(KEY_AUTH_TOKEN, accessToken)
        if (!refreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()
    }

    fun clearTokens(context: Context) {
        prefs(context).edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    data class SavedCredentials(val email: String, val password: String)

    fun saveCredentials(context: Context, email: String, password: String) {
        encryptedPrefs(context).edit()
            .putString(KEY_SAVED_EMAIL, email.trim())
            .putString(KEY_SAVED_PASSWORD, password)
            .apply()
    }

    fun getSavedCredentials(context: Context): SavedCredentials? {
        val enc = encryptedPrefs(context)
        val email = enc.getString(KEY_SAVED_EMAIL, null)?.trim()
        val password = enc.getString(KEY_SAVED_PASSWORD, null)
        if (email.isNullOrBlank() || password.isNullOrBlank()) return null
        return SavedCredentials(email, password)
    }

    fun hasSavedCredentials(context: Context): Boolean = getSavedCredentials(context) != null

    fun clearSavedCredentials(context: Context) {
        encryptedPrefs(context).edit()
            .remove(KEY_SAVED_EMAIL)
            .remove(KEY_SAVED_PASSWORD)
            .apply()
    }
}
