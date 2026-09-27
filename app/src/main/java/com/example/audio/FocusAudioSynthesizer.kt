package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class AmbientSoundType(val displayName: String) {
    NONE("Silent"),
    RAIN("Gentle Rain"),
    WHITE_NOISE("Soft Noise"),
    OCEAN("Ocean Waves")
}

class FocusAudioSynthesizer {
    private var ambientTrack: AudioTrack? = null
    private var ambientJob: Job? = null
    private var currentType: AmbientSoundType = AmbientSoundType.NONE

    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startAmbient(type: AmbientSoundType) {
        if (currentType == type && ambientJob?.isActive == true) return
        stopAmbient()

        if (type == AmbientSoundType.NONE) {
            currentType = AmbientSoundType.NONE
            return
        }

        currentType = type
        ambientJob = scope.launch {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(sampleRate / 2)

                ambientTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                ambientTrack?.play()

                val buffer = ShortArray(bufferSize / 2)
                val random = Random()
                var filterState = 0.0
                var phase = 0.0

                while (isActive) {
                    when (type) {
                        AmbientSoundType.WHITE_NOISE -> {
                            for (i in buffer.indices) {
                                val white = (random.nextDouble() * 2.0 - 1.0) * 0.15
                                // Low-pass filter for smooth soft white noise
                                filterState = filterState * 0.85 + white * 0.15
                                buffer[i] = (filterState * 32767.0).toInt().toShort()
                            }
                        }
                        AmbientSoundType.RAIN -> {
                            for (i in buffer.indices) {
                                val raw = random.nextDouble() * 2.0 - 1.0
                                val drop = if (random.nextDouble() < 0.02) (random.nextDouble() * 0.6) else 0.0
                                filterState = filterState * 0.90 + (raw * 0.12 + drop) * 0.10
                                buffer[i] = (filterState * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                            }
                        }
                        AmbientSoundType.OCEAN -> {
                            for (i in buffer.indices) {
                                phase += 0.00015
                                if (phase > 2 * PI) phase -= 2 * PI
                                val swell = (sin(phase) + 1.0) * 0.5 * 0.35 + 0.08
                                val raw = (random.nextDouble() * 2.0 - 1.0) * swell
                                filterState = filterState * 0.93 + raw * 0.07
                                buffer[i] = (filterState * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                            }
                        }
                        AmbientSoundType.NONE -> break
                    }

                    ambientTrack?.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    ambientTrack?.stop()
                    ambientTrack?.release()
                } catch (_: Exception) {}
                ambientTrack = null
            }
        }
    }

    fun stopAmbient() {
        currentType = AmbientSoundType.NONE
        ambientJob?.cancel()
        ambientJob = null
        try {
            ambientTrack?.stop()
            ambientTrack?.release()
        } catch (_: Exception) {}
        ambientTrack = null
    }

    fun playCompletionChime() {
        scope.launch {
            try {
                val chimeSampleRate = 44100
                val durationSeconds = 1.6
                val totalSamples = (chimeSampleRate * durationSeconds).toInt()
                val buffer = ShortArray(totalSamples)

                val freq1 = 523.25 // C5
                val freq2 = 659.25 // E5
                val freq3 = 783.99 // G5

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / chimeSampleRate
                    val env = exp(-2.5 * t) // decay envelope
                    val sample = (
                        0.4 * sin(2 * PI * freq1 * t) +
                        0.35 * sin(2 * PI * freq2 * t) +
                        0.25 * sin(2 * PI * freq3 * t)
                    ) * env

                    buffer[i] = (sample * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(chimeSampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                // Sleep for chime playback then release
                kotlinx.coroutines.delay(1800)
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun getCurrentAmbient(): AmbientSoundType = currentType
}
