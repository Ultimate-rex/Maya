package com.maya.assistant.core

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/**
 * Records real microphone audio to a temp file using Android's MediaRecorder,
 * for upload to the backend's /voice/transcribe endpoint. Requires
 * RECORD_AUDIO permission to already be granted before calling start().
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        val file = File(context.cacheDir, "maya_recording_${System.currentTimeMillis()}.m4a")
        outputFile = file

        @Suppress("DEPRECATION")
        val r = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        return file
    }

    fun stop(): File? {
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // stop() throws if called too soon after start(); safe to ignore for a short clip
        }
        recorder?.release()
        recorder = null
        return outputFile
    }
}
