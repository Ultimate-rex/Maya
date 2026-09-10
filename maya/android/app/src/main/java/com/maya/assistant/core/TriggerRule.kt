package com.maya.assistant.core

import org.json.JSONArray
import org.json.JSONObject

enum class TriggerEvent { CHARGING_STARTED, CHARGING_STOPPED, BATTERY_BELOW, BATTERY_ABOVE, WIFI_CONNECTED, WIFI_DISCONNECTED, SCREEN_ON, SCREEN_OFF }
enum class TriggerActionType { SPEAK_LINE, OPEN_APP }

data class TriggerRule(
    val id: String,
    val event: TriggerEvent,
    val threshold: Int? = null,           // used by BATTERY_BELOW / BATTERY_ABOVE
    val actionType: TriggerActionType,
    val actionParam: String,              // the line to speak, or the app name to open
    val enabled: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("event", event.name)
        threshold?.let { put("threshold", it) }
        put("actionType", actionType.name)
        put("actionParam", actionParam)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(obj: JSONObject): TriggerRule = TriggerRule(
            id = obj.getString("id"),
            event = TriggerEvent.valueOf(obj.getString("event")),
            threshold = if (obj.has("threshold")) obj.getInt("threshold") else null,
            actionType = TriggerActionType.valueOf(obj.getString("actionType")),
            actionParam = obj.getString("actionParam"),
            enabled = obj.optBoolean("enabled", true),
        )
    }
}

/** Persists trigger rules as JSON in the app's on-device storage - no database. */
class TriggerRuleStore(private val store: LocalJsonStore) {
    private val fileKey = "triggers"

    fun all(): List<TriggerRule> {
        val arr: JSONArray = try { store.readArray(fileKey) } catch (e: Exception) { JSONArray() }
        return (0 until arr.length()).map { TriggerRule.fromJson(arr.getJSONObject(it)) }
    }

    fun add(rule: TriggerRule) = persist(all() + rule)

    fun remove(id: String) = persist(all().filterNot { it.id == id })

    fun setEnabled(id: String, enabled: Boolean) =
        persist(all().map { if (it.id == id) it.copy(enabled = enabled) else it })

    private fun persist(rules: List<TriggerRule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(it.toJson()) }
        store.writeArray(fileKey, arr)
    }
}
