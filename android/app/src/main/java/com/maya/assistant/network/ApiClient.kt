package com.maya.assistant.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the user's own Gemini API key locally on-device, encrypted with
 * Android's hardware-backed Keystore (EncryptedSharedPreferences) rather
 * than in plain SharedPreferences or hardcoded in source.
 *
 * This app calls Gemini directly from the phone - there is no backend
 * server. That means the key lives inside this app's encrypted storage. If
 * you ever share this APK with someone else, they'd be using your key and
 * your quota, so keep it personal - the same tradeoff we discussed earlier.
 */
object SecureConfigStore {
    private const val FILE_NAME = "maya_secure_config"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveGeminiApiKey(context: Context, apiKey: String) {
        prefs(context).edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    fun getGeminiApiKey(context: Context): String? = prefs(context).getString(KEY_GEMINI_API_KEY, null)

    fun setWakeWordEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
    }

    fun isWakeWordEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_WAKE_WORD_ENABLED, false)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
