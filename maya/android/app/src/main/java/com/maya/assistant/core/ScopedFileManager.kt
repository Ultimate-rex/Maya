package com.maya.assistant.core

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Manages files only inside a folder the user has explicitly picked and
 * granted access to via Android's Storage Access Framework
 * (ACTION_OPEN_DOCUMENT_TREE). Maya cannot silently roam the whole device -
 * the system file picker is the same one every app uses, and access
 * persists only for the folder the user chose.
 */
class ScopedFileManager(private val context: Context) {

    fun persistAccess(treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    fun listFiles(treeUri: Uri): List<DocumentFile> {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return dir.listFiles().toList()
    }

    fun searchFiles(treeUri: Uri, query: String): List<DocumentFile> =
        listFiles(treeUri).filter { it.name?.contains(query, ignoreCase = true) == true }

    fun deleteFile(file: DocumentFile): Boolean = file.delete()

    fun renameFile(file: DocumentFile, newName: String): Boolean = file.renameTo(newName)

    fun createFolder(treeUri: Uri, name: String): DocumentFile? {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        return dir.createDirectory(name)
    }

    /** Copies a file's bytes into a new file in the same tree, then optionally deletes the original (move). */
    fun copyOrMove(source: DocumentFile, destinationDir: DocumentFile, move: Boolean): DocumentFile? {
        val mime = source.type ?: "application/octet-stream"
        val newFile = destinationDir.createFile(mime, source.name ?: "file") ?: return null
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
        if (move) source.delete()
        return newFile
    }
}
