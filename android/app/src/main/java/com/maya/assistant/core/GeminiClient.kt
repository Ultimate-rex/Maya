package com.maya.assistant.core

import android.content.Context
import com.maya.assistant.network.SecureConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatResult(val intent: String, val parameters: Map<String, Any?>, val say: String)

/**
 * Calls Google's Gemini API directly from the phone using the API key the
 * user pasted into Maya's settings screen. There is no backend server in
 * this architecture - the key lives only in this app's encrypted local
 * storage (see SecureConfigStore) and this class's HTTPS request to Google.
 */
class GeminiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are Maya, a calm, friendly, intelligent personal voice assistant running on
        the user's own Android phone. You are talkative only when it helps; otherwise concise.

        When the user's request maps to a device action, respond with ONLY a JSON object
        (no prose, no markdown fences) shaped like:
        {"intent": "OPEN_APP", "parameters": {"app": "Chrome"}, "say": "Sure, opening Chrome."}

        Valid intents: OPEN_APP, WEB_SEARCH, MEDIA_PLAY_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS,
        ALARM_SET, TIMER_SET, FLASHLIGHT, BRIGHTNESS, VOLUME, BATTERY_STATUS,
        GENERATE_PASSWORD, GENERATE_QR, NONE.

        For GENERATE_QR, put the text/URL to encode in parameters.content.
        For GENERATE_PASSWORD, parameters may include "length" (int) and "symbols" (true/false).
        For TIMER_SET, parameters.seconds is the duration.
        For ALARM_SET, parameters.hour and parameters.minute (24-hour).

        Use intent "NONE" with only a "say" field for plain conversation that requires no
        device action. Never invent an intent outside this list. Always include a natural,
        non-robotic "say" field.

        You never ask for or store PIN numbers, passwords for other accounts, or OTPs, and
        you never claim to perform payments or read someone's messages/notifications.
    """.trimIndent()

    suspend fun chat(userText: String, history: List<Pair<String, String>>): ChatResult =
        withContext(Dispatchers.IO) {
            val apiKey = SecureConfigStore.getGeminiApiKey(context)
                ?: throw IllegalStateException("No Gemini API key set. Add it in Maya's settings screen.")

            val contents = JSONArray()
            history.forEach { (role, text) ->
                contents.put(
                    JSONObject().apply {
                        put("role", if (role == "assistant") "model" else "user")
                        put("parts", JSONArray().put(JSONObject().put("text", text)))
                    }
                )
            }
            contents.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userText)))
                }
            )

            val payload = JSONObject().apply {
                put("contents", contents)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 512)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Gemini API error (${response.code}): $raw")
                }
                val json = JSONObject(raw)
                val text = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                parseCommand(text)
            }
        }

    private fun parseCommand(rawText: String): ChatResult {
        val cleaned = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val obj = JSONObject(cleaned)
            val paramsJson = obj.optJSONObject("parameters") ?: JSONObject()
            val params = mutableMapOf<String, Any?>()
            paramsJson.keys().forEach { key ->
                val value = paramsJson.get(key)
                params[key] = if (value is Number) value.toDouble() else value
            }
            ChatResult(
                intent = obj.optString("intent", "NONE"),
                parameters = params,
                say = obj.optString("say", cleaned),
            )
        } catch (e: Exception) {
            // Model replied with plain text instead of JSON - treat it as conversation.
            ChatResult(intent = "NONE", parameters = emptyMap(), say = rawText)
        }
    }
}
