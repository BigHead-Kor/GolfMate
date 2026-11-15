package com.selfbell.golfmate

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

internal class BriefingSpeech(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val engine = TextToSpeech(context, Mode.ON_DEVICE)
    private var connected = false
    @Volatile private var playbackGeneration = 0L
    private var closed = false
    var status by mutableStateOf(SpeechStatus.Loading)
        private set

    private val isPlayingOrStarting: Boolean
        get() = status == SpeechStatus.Starting || status == SpeechStatus.Playing

    private val listener = object : EventListener {
        override fun onReady() { Log.d(TAG, "onReady") }
        override fun onStart() = report(SpeechStatus.Playing)
        override fun onDone() = report(SpeechStatus.Done)
        override fun onStop() = report(SpeechStatus.Stopped)
        override fun onUpdatedRms(rms: Double) = Unit

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "Speech error: $errorType")
            report(SpeechStatus.Error)
        }
    }

    init {
        engine.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                main.post {
                    if (!closed) {
                        engine.addEventListener(listener)
                        engine.setLocale(Locale.KOREA)
                        connected = true
                        status = SpeechStatus.Ready
                        Log.i(TAG, "Initialized ON_DEVICE: ko-KR")
                    }
                }
            }

            override fun onFailure(errorType: ErrorType) {
                main.post {
                    if (!closed) {
                        status = SpeechStatus.Unavailable
                        Log.w(TAG, "Initialization failed: $errorType")
                    }
                }
            }
        })
    }

    private fun report(next: SpeechStatus) {
        val generation = playbackGeneration
        main.post {
            if (closed || generation != playbackGeneration) return@post
            Log.d(TAG, "Playback callback: $next (current: $status)")
            status = status.afterCallback(next)
        }
    }

    fun play(text: String) {
        if (closed || !connected || isPlayingOrStarting ||
            status == SpeechStatus.Stopping || text.isBlank()
        ) return

        playbackGeneration++
        status = SpeechStatus.Starting
        engine.speak(text)
    }

    fun stop() {
        if (!isPlayingOrStarting) return
        status = SpeechStatus.Stopping
        engine.stop()
    }

    fun close() {
        if (closed) return
        closed = true
        stop()
        if (connected) engine.removeEventListener(listener)
        engine.release()
    }

    private companion object {
        const val TAG = "GolfMatePleosTTS"
    }
}
