package com.maya.assistant.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps Android's built-in SpeechRecognizer for one-shot voice-to-text.
 * This runs via Google's on-device/system speech service (the same one
 * used by Google Assistant and the keyboard's mic button) - no backend
 * server or extra API key needed for voice input.
 */
class SpeechToText(private val context: Context) {

    suspend fun listenOnce(): String = suspendCancellableCoroutine { continuation ->
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            continuation.resumeWithException(IllegalStateException("Speech recognition isn't available on this device."))
            return@suspendCancellableCoroutine
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                recognizer.destroy()
                if (continuation.isActive) continuation.resume(text)
            }

            override fun onError(error: Int) {
                recognizer.destroy()
                if (continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException("Speech recognition error code $error"))
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        continuation.invokeOnCancellation { recognizer.destroy() }
        recognizer.startListening(intent)
    }
}
