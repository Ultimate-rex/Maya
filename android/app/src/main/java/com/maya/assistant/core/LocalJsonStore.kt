package com.maya.assistant.core

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local JSON storage for on-device data (recent conversation cache, UI
 * preferences, plugin toggles). This writes to the app's own external
 * storage directory, e.g.
 *   /storage/emulated/0/Android/data/com.maya.assistant/files/Maya/
 * which shows up in a normal file manager and survives simple app updates,
 * but is removed if the user uninstalls Maya or taps "Clear data" on the
 * "Maya" app entry (same as any other app-scoped storage).
 *
 * No SQLite/Room here by design for v1, per the JSON-first requirement.
 */
class LocalJsonStore(context: Context) {

    private val root: File = File(
        context.getExternalFilesDir(null),
        "Maya"
    ).apply { mkdirs() }

    private fun fileFor(name: String): File {
        val f = File(root, "$name.json")
        if (!f.exists()) f.writeText(if (name.endsWith("s")) "[]" else "{}")
        return f
    }

    fun readObject(name: String): JSONObject =
        JSONObject(fileFor(name).readText().ifBlank { "{}" })

    fun writeObject(name: String, obj: JSONObject) {
        fileFor(name).writeText(obj.toString(2))
    }

    fun readArray(name: String): JSONArray =
        JSONArray(fileFor(name).readText().ifBlank { "[]" })

    fun writeArray(name: String, array: JSONArray) {
        fileFor(name).writeText(array.toString(2))
    }

    fun appendToArray(name: String, entry: JSONObject) {
        val arr = readArray(name)
        arr.put(entry)
        fileFor(name).writeText(arr.toString(2))
    }

    fun folderPath(): String = root.absolutePath
}
