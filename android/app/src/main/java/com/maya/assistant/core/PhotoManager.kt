package com.maya.assistant.core

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore

data class PhotoRef(val id: Long, val uri: android.net.Uri, val displayName: String)

/**
 * Reads and deletes photos through Android's MediaStore. On Android 10+,
 * deleting someone's own photos still requires the user to approve a real
 * system confirmation dialog (MediaStore.createDeleteRequest) - Maya cannot
 * silently wipe photos; it can only trigger that same OS-level prompt any
 * gallery app would.
 */
class PhotoManager(private val context: Context) {

    fun recentPhotos(limit: Int = 20, folderNameContains: String? = null): List<PhotoRef> {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.RELATIVE_PATH)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit"
        val results = mutableListOf<PhotoRef>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol) ?: ""
                if (folderNameContains != null && !path.contains(folderNameContains, ignoreCase = true)) continue
                val id = cursor.getLong(idCol)
                val uri = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                results.add(PhotoRef(id, uri, cursor.getString(nameCol)))
            }
        }
        return results
    }

    /**
     * Returns an IntentSender to launch via
     * ActivityResultContracts.StartIntentSenderForResult - the system shows
     * its own delete-confirmation UI; Maya never bypasses it.
     */
    fun requestDelete(photos: List<PhotoRef>): IntentSender? {
        if (photos.isEmpty()) return null
        val uris = photos.map { it.uri }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        } else {
            try {
                context.contentResolver.delete(photos.first().uri, null, null)
                null
            } catch (e: RecoverableSecurityException) {
                e.userAction.actionIntent.intentSender
            }
        }
    }
}
