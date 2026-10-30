package com.lifepath.app.auth

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LoginResult(
    val accessToken: String,
    val refreshToken: String?
)

object ApiClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun login(context: Context, email: String, password: String): LoginResult {
        val apiUrl = AuthStore.getApiUrl(context) ?: throw IllegalStateException("API URL not configured")
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$apiUrl/api/v1/auth/login")
            .post(payload)
            .header("Content-Type", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Login failed (${response.code})")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IllegalStateException("Login failed: empty response")
            val json = JSONObject(body)
            val access = json.optString("access_token", "")
            if (access.isBlank()) throw IllegalStateException("Login failed: missing access token")
            val refresh = json.optString("refresh_token", "").ifBlank { null }
            return LoginResult(accessToken = access, refreshToken = refresh)
        }
    }

    fun get(context: Context, path: String): String {
        return execute(context, path, "GET", null, true)
    }

    private fun execute(
        context: Context,
        path: String,
        method: String,
        body: String?,
        retryOn401: Boolean
    ): String {
        val apiUrl = AuthStore.getApiUrl(context) ?: throw IllegalStateException("API URL not configured")
        val accessToken = AuthStore.getAccessToken(context) ?: throw IllegalStateException("Not authenticated")
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val requestBuilder = Request.Builder()
            .url("$apiUrl/api/v1$normalizedPath")
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")

        when (method) {
            "POST" -> requestBuilder.post((body ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> requestBuilder.patch((body ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> requestBuilder.delete((body ?: "").toRequestBody(jsonMediaType))
            else -> requestBuilder.get()
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 401 && retryOn401 && refreshAccessToken(context)) {
                return execute(context, path, method, body, false)
            }
            if (!response.isSuccessful) {
                throw IllegalStateException("Request failed (${response.code})")
            }
            return response.body?.string().orEmpty()
        }
    }

    private fun refreshAccessToken(context: Context): Boolean {
        val apiUrl = AuthStore.getApiUrl(context) ?: return false
        val refresh = AuthStore.getRefreshToken(context) ?: return false
        val payload = JSONObject()
            .put("refreshToken", refresh)
            .toString()
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$apiUrl/api/v1/auth/refresh")
            .post(payload)
            .header("Content-Type", "application/json")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return false
                val json = JSONObject(body)
                val access = json.optString("access_token", "")
                if (access.isBlank()) return false
                val nextRefresh = json.optString("refresh_token", "").ifBlank { refresh }
                AuthStore.saveTokens(context, access, nextRefresh)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
