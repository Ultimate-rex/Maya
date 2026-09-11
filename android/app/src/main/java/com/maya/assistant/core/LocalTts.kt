package com.maya.assistant.core

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Wraps Android's built-in offline-capable TextToSpeech engine, used for
 * short trigger lines ("Boss, battery is low") so those don't need a round
 * trip to the backend server. The main conversational voice still goes
 * through the backend's TTS provider for higher quality.
 */
class LocalTts(context: Context) {
    private var ready = false
    private val engine: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    init {
        engine.language = Locale.getDefault()
    }

    fun speak(text: String) {
        if (ready) {
            engine.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    fun shutdown() {
        engine.shutdown()
    }
}
