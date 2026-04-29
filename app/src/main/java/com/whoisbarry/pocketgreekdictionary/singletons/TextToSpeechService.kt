package com.whoisbarry.pocketgreekdictionary.singletons

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TextToSpeechService : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("el-GR"))
            // TODO: See if we can fix TextToSpeech.LANG_NOT_SUPPORTED status which I'm seeing
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
            }
        }
    }

    fun canSpeak(): Boolean {
        return isInitialized
    }

    fun speakText(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
        tts = null
    }
}
