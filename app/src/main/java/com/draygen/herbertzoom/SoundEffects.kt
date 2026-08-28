package com.draygen.herbertzoom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.math.PI
import kotlin.math.sin

class SoundEffects(context: Context) {

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var isLoaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        // Generate clean synthesized wav sounds in memory and load into soundpool
        try {
            soundMap["pickup"] = loadPcmSound(generateTonePcm(880.0, 0.08, 0.3)) // High cheerful blip
            soundMap["nearmiss"] = loadPcmSound(generateSlideTonePcm(600.0, 1200.0, 0.12, 0.25)) // Swoosh
            soundMap["jump"] = loadPcmSound(generateSlideTonePcm(350.0, 700.0, 0.15, 0.35)) // Boing
            soundMap["zoomie"] = loadPcmSound(generateChordPcm(listOf(523.25, 659.25, 783.99, 1046.50), 0.45, 0.4)) // Happy fanfare
            soundMap["flop"] = loadPcmSound(generateSlideTonePcm(400.0, 180.0, 0.25, 0.3)) // Wholesome thud/slide
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPcmSound(wavBytes: ByteArray): Int {
        // Write temporary wav to cache and load into soundpool
        val tempFile = java.io.File.createTempFile("snd", ".wav")
        tempFile.writeBytes(wavBytes)
        val id = soundPool.load(tempFile.absolutePath, 1)
        tempFile.deleteOnExit()
        return id
    }

    fun playPickup() = play("pickup", 0.7f)
    fun playJump() = play("jump", 0.65f)
    fun playNearMiss() = play("nearmiss", 0.6f)
    fun playZoomie() = play("zoomie", 0.85f)
    fun playFlop() = play("flop", 0.75f)

    private fun play(key: String, volume: Float) {
        val id = soundMap[key] ?: return
        soundPool.play(id, volume, volume, 1, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }

    companion object {
        private fun generateTonePcm(freq: Double, durationSec: Double, volume: Double): ByteArray {
            val sampleRate = 44100
            val numSamples = (durationSec * sampleRate).toInt()
            val pcm = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = (1.0 - (i.toDouble() / numSamples)) // linear fade out
                val sample = sin(2.0 * PI * freq * t) * envelope * volume
                pcm[i] = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            }
            return createWavFile(sampleRate, pcm)
        }

        private fun generateSlideTonePcm(startFreq: Double, endFreq: Double, durationSec: Double, volume: Double): ByteArray {
            val sampleRate = 44100
            val numSamples = (durationSec * sampleRate).toInt()
            val pcm = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * PI * currentFreq / sampleRate
                val envelope = (1.0 - progress)
                val sample = sin(phase) * envelope * volume
                pcm[i] = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            }
            return createWavFile(sampleRate, pcm)
        }

        private fun generateChordPcm(freqs: List<Double>, durationSec: Double, volume: Double): ByteArray {
            val sampleRate = 44100
            val numSamples = (durationSec * sampleRate).toInt()
            val pcm = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val envelope = (1.0 - progress)
                var combined = 0.0
                for (f in freqs) {
                    combined += sin(2.0 * PI * f * t)
                }
                combined = (combined / freqs.size) * envelope * volume
                pcm[i] = (combined * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            }
            return createWavFile(sampleRate, pcm)
        }

        private fun createWavFile(sampleRate: Int, samples: ShortArray): ByteArray {
            val byteStream = ByteArrayOutputStream()
            val out = DataOutputStream(byteStream)

            val dataSize = samples.size * 2
            val totalSize = 36 + dataSize

            // RIFF header
            out.writeBytes("RIFF")
            out.writeInt(Integer.reverseBytes(totalSize))
            out.writeBytes("WAVE")

            // fmt chunk
            out.writeBytes("fmt ")
            out.writeInt(Integer.reverseBytes(16)) // Chunk size
            out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // Format = PCM
            out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // Channels = Mono
            out.writeInt(Integer.reverseBytes(sampleRate))
            out.writeInt(Integer.reverseBytes(sampleRate * 2)) // Byte rate
            out.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt()) // Block align
            out.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt()) // Bits per sample

            // data chunk
            out.writeBytes("data")
            out.writeInt(Integer.reverseBytes(dataSize))
            for (s in samples) {
                out.writeShort(java.lang.Short.reverseBytes(s).toInt())
            }

            out.flush()
            return byteStream.toByteArray()
        }
    }
}
