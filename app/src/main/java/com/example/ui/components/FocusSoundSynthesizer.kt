package com.example.ui.components

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class SoundPreset {
    NONE,
    BROWN_NOISE,     // Deep rumble (waterfall/thunder)
    WHITE_NOISE,     // Steady static (shhh)
    BINAURAL_BEATS,  // 40Hz Gamma Focus (left/right phase shift)
    COSMIC_SPACE     // Soft sweeping cosmic pad
}

object FocusSoundSynthesizer {
    private const val TAG = "SoundSynthesizer"
    private const val SAMPLE_RATE = 44100
    
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var currentPreset = SoundPreset.NONE
        private set

    var volume: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            try {
                audioTrack?.let { track ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        track.setVolume(field)
                    } else {
                        @Suppress("DEPRECATION")
                        track.setStereoVolume(field, field)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting volume", e)
            }
        }

    fun start(preset: SoundPreset) {
        if (currentPreset == preset) return
        stop()
        
        currentPreset = preset
        if (preset == SoundPreset.NONE) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
            )
            
            audioTrack?.let { track ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    track.setVolume(volume)
                } else {
                    @Suppress("DEPRECATION")
                    track.setStereoVolume(volume, volume)
                }
                track.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack", e)
            return
        }

        synthJob = scope.launch {
            val bufferSize = 8192 // Standard chunk size
            val audioBuffer = ShortArray(bufferSize)
            
            // Synthesis states
            var brownLastSample = 0.0
            var phaseLeft = 0.0
            var phaseRight = 0.0
            var sweepPhase = 0.0
            
            while (isActive) {
                val track = audioTrack ?: break
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    delay(50)
                    continue
                }

                when (currentPreset) {
                    SoundPreset.WHITE_NOISE -> {
                        for (i in 0 until bufferSize step 2) {
                            val noise = (Random.nextFloat() * 2f - 1f) * 0.25f // Scale down to avoid clipping
                            val shortVal = (noise * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            audioBuffer[i] = shortVal     // Left
                            audioBuffer[i + 1] = shortVal // Right
                        }
                    }
                    SoundPreset.BROWN_NOISE -> {
                        for (i in 0 until bufferSize step 2) {
                            // Brownian is white noise filtered (integrated) over time with a leaking coefficient
                            val white = Random.nextFloat() * 2f - 1f
                            brownLastSample = (0.985 * brownLastSample) + (0.015 * white)
                            // Boost and soft limit
                            val raw = brownLastSample * 2.5
                            val shortVal = (raw * Short.MAX_VALUE * 0.2).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            audioBuffer[i] = shortVal
                            audioBuffer[i + 1] = shortVal
                        }
                    }
                    SoundPreset.BINAURAL_BEATS -> {
                        // Left: 150Hz, Right: 190Hz (40Hz difference - Gamma focus state)
                        val freqLeft = 150.0
                        val freqRight = 190.0
                        for (i in 0 until bufferSize step 2) {
                            val sampleLeft = sin(phaseLeft) * 0.3
                            val sampleRight = sin(phaseRight) * 0.3
                            
                            phaseLeft += (2.0 * Math.PI * freqLeft) / SAMPLE_RATE
                            phaseRight += (2.0 * Math.PI * freqRight) / SAMPLE_RATE
                            
                            // Prevent phase overflow
                            if (phaseLeft > 2.0 * Math.PI) phaseLeft -= 2.0 * Math.PI
                            if (phaseRight > 2.0 * Math.PI) phaseRight -= 2.0 * Math.PI

                            audioBuffer[i] = (sampleLeft * Short.MAX_VALUE).toInt().toShort()
                            audioBuffer[i + 1] = (sampleRight * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                    SoundPreset.COSMIC_SPACE -> {
                        // Slowly sweeping sweep oscillator modulate main frequencies
                        for (i in 0 until bufferSize step 2) {
                            val modulation = sin(sweepPhase) * 15.0 // Sweeping freq modulation
                            sweepPhase += (2.0 * Math.PI * 0.05) / SAMPLE_RATE // Very slow sweep (0.05 Hz)
                            if (sweepPhase > 2.0 * Math.PI) sweepPhase -= 2.0 * Math.PI

                            val freqL = 110.0 + modulation
                            val freqR = 110.5 - modulation // Slight stereo separation

                            val sampleL = (sin(phaseLeft) + sin(phaseLeft * 1.5) * 0.4) * 0.25
                            val sampleR = (sin(phaseRight) + sin(phaseRight * 1.5) * 0.4) * 0.25

                            phaseLeft += (2.0 * Math.PI * freqL) / SAMPLE_RATE
                            phaseRight += (2.0 * Math.PI * freqR) / SAMPLE_RATE

                            if (phaseLeft > 2.0 * Math.PI) phaseLeft -= 2.0 * Math.PI
                            if (phaseRight > 2.0 * Math.PI) phaseRight -= 2.0 * Math.PI

                            audioBuffer[i] = (sampleL * Short.MAX_VALUE).toInt().toShort()
                            audioBuffer[i + 1] = (sampleR * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                    else -> {
                        // Empty silence
                        audioBuffer.fill(0)
                    }
                }
                
                track.write(audioBuffer, 0, bufferSize)
            }
        }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        currentPreset = SoundPreset.NONE
        try {
            audioTrack?.let { track ->
                track.stop()
                track.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        audioTrack = null
    }
}
