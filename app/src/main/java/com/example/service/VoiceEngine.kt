package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.PodcastLine
import com.example.model.PodcastSpeaker
import java.util.Locale

/**
 * Dual-voice Vietnamese Android TTS engine (distinct pitch/rate for Host Minh vs TS. Lan)
 * + Android SpeechRecognizer wrapper for Oral Viva & AI Tutor voice input.
 */
class VoiceEngine(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var currentQueue: List<PodcastLine> = emptyList()
    private var currentIndex: Int = -1
    private var baseSpeed: Float = 1.0f
    private var onLineChanged: ((Int) -> Unit)? = null
    private var onPlaybackFinished: (() -> Unit)? = null

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val viLocale = Locale("vi", "VN")
                val res = tts?.setLanguage(viLocale)
                isTtsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isTtsReady) {
                    tts?.setLanguage(Locale.getDefault())
                    isTtsReady = true
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId?.startsWith("podcast_") == true) {
                            val nextIdx = currentIndex + 1
                            if (nextIdx < currentQueue.size) {
                                speakPodcastLine(nextIdx)
                            } else {
                                currentIndex = -1
                                onPlaybackFinished?.invoke()
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onPlaybackFinished?.invoke()
                    }
                })
            }
        }
    }

    fun setBaseSpeed(speed: Float) {
        baseSpeed = speed.coerceIn(0.6f, 1.8f)
    }

    fun speakSingle(text: String, speaker: PodcastSpeaker = PodcastSpeaker.TS_LAN) {
        if (!isTtsReady) return
        applySpeakerVoice(speaker)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "single_${System.currentTimeMillis()}")
    }

    fun playPodcastLines(
        lines: List<PodcastLine>,
        startIndex: Int = 0,
        speed: Float = 1.0f,
        onActiveLine: (Int) -> Unit,
        onComplete: () -> Unit
    ) {
        if (lines.isEmpty()) {
            onComplete()
            return
        }
        baseSpeed = speed
        currentQueue = lines
        onLineChanged = onActiveLine
        onPlaybackFinished = onComplete
        speakPodcastLine(startIndex.coerceIn(0, lines.lastIndex))
    }

    private fun speakPodcastLine(index: Int) {
        if (!isTtsReady || index !in currentQueue.indices) {
            onPlaybackFinished?.invoke()
            return
        }
        currentIndex = index
        val line = currentQueue[index]
        onLineChanged?.invoke(index)
        applySpeakerVoice(line.speaker)
        tts?.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, "podcast_$index")
    }

    private fun applySpeakerVoice(speaker: PodcastSpeaker) {
        when (speaker) {
            PodcastSpeaker.HOST_MINH -> {
                tts?.setPitch(1.16f)
                tts?.setSpeechRate((baseSpeed * 1.08f).coerceIn(0.6f, 2.0f))
            }
            PodcastSpeaker.TS_LAN -> {
                tts?.setPitch(0.86f)
                tts?.setSpeechRate((baseSpeed * 0.92f).coerceIn(0.6f, 2.0f))
            }
        }
    }

    fun stopSpeaking() {
        currentQueue = emptyList()
        currentIndex = -1
        tts?.stop()
    }

    fun startVietnameseListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopListening()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Thiết bị chưa hỗ trợ nhận diện giọng nói Google.")
            return
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                onError("Không nghe rõ giọng nói (mã $error). Hãy thử nói lại gần mic hơn.")
            }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = list?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) onResult(spoken) else onError("Không nhận diện được lời nói.")
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    fun shutdown() {
        stopSpeaking()
        stopListening()
        tts?.shutdown()
        tts = null
    }
}
