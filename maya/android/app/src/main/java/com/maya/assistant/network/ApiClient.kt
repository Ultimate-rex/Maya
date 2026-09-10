package com.maya.assistant.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Stores the backend URL and access token in Android's EncryptedSharedPreferences
 * (hardware-backed on most devices) rather than in plain SharedPreferences or
 * hardcoded in source, and attaches the token as a Bearer header on every request.
 */
object SecureConfigStore {
    private const val FILE_NAME = "maya_secure_config"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_TOKEN = "access_token"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveServerConfig(context: Context, baseUrl: String, token: String) {
        prefs(context).edit()
            .putString(KEY_BASE_URL, baseUrl)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getBaseUrl(context: Context): String? = prefs(context).getString(KEY_BASE_URL, null)
    fun getToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)
}

object ApiClient {
    fun create(context: Context): MayaApi {
        val baseUrl = SecureConfigStore.getBaseUrl(context)
            ?: throw IllegalStateException("Backend URL not configured yet - set it in Maya's settings screen.")
        val token = SecureConfigStore.getToken(context).orEmpty()

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MayaApi::class.java)
    }
}
