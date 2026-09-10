package com.maya.assistant.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Wraps Android's real runtime-permission flow. Nothing here is faked -
 * these are the actual system permission dialogs (the same ones shown in
 * your screenshots for location, etc.), and Maya only gets a capability
 * once the user has actually granted it.
 */
class PermissionManager(private val activity: ComponentActivity) {

    private var onResult: ((Map<String, Boolean>) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> onResult?.invoke(results) }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    fun requestCorePermissions(onResult: (Map<String, Boolean>) -> Unit) {
        this.onResult = onResult
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(permissions.toTypedArray())
    }

    companion object {
        fun missingPermissions(context: Context, permissions: List<String>): List<String> =
            permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
    }
}
