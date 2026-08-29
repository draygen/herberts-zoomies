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
            soundMap["scratch"] = loadPcmSound(generateScratchPcm(0.75, 0.32)) // Rhythmic scritch-scritch on floorboard

            // --- Kitty boss encounter ---
            // Low, felt-more-than-heard rumble as she rises into frame
            soundMap["bossrumble"] = loadPcmSound(generateRumblePcm(1.6, 0.42))
            // Two-note warning that an attack has begun winding up
            soundMap["telegraph"] = loadPcmSound(generateSlideTonePcm(760.0, 520.0, 0.18, 0.22))
            // The paw landing: a soft, heavy, entirely harmless whump
            soundMap["bossslam"] = loadPcmSound(generateSlideTonePcm(220.0, 90.0, 0.28, 0.36))
            // Herbert's eye beams: a rising, ridiculous zap
            soundMap["beam"] = loadPcmSound(generateSlideTonePcm(420.0, 1900.0, 0.42, 0.4))
            // Kitty, startled and unimpressed in equal measure
            soundMap["kittyshock"] = loadPcmSound(generateSlideTonePcm(520.0, 880.0, 0.22, 0.3))
            // She gives up and leaves
            soundMap["bossvictory"] = loadPcmSound(
                generateChordPcm(listOf(392.00, 523.25, 659.25, 783.99, 1046.50), 0.9, 0.42)
            )
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
    fun playScratch() = play("scratch", 0.7f)

    fun playBossRumble() = play("bossrumble", 0.9f)
    fun playTelegraph() = play("telegraph", 0.45f)
    fun playBossSlam() = play("bossslam", 0.7f)
    fun playBeam() = play("beam", 0.85f)
    fun playKittyShock() = play("kittyshock", 0.6f)
    fun playBossVictory() = play("bossvictory", 0.9f)

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

        /**
         * Rhythmic "scritch scritch scritch" - filtered noise pulsed at the same
         * rate Herbert's paws windmill, so the sound lines up with the animation.
         */
        private fun generateScratchPcm(durationSec: Double, volume: Double): ByteArray {
            val sampleRate = 44100
            val numSamples = (durationSec * sampleRate).toInt()
            val pcm = ShortArray(numSamples)

            var seed = 0x5EED_1234L // deterministic noise, no allocation churn
            var lowpass = 0.0
            val rakesPerSec = 27.0 / (2.0 * PI) * 2.0 // matches the paw circle rate

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples

                // xorshift noise in -1..1
                seed = seed xor (seed shl 13)
                seed = seed xor (seed ushr 7)
                seed = seed xor (seed shl 17)
                val white = ((seed and 0xFFFF).toDouble() / 32768.0) - 1.0

                // Band-limit it so it reads as wood, not hiss
                lowpass += (white - lowpass) * 0.35
                val body = lowpass - white * 0.25

                // Pulse envelope: sharp rake, quick decay, repeated
                val rakePhase = (t * rakesPerSec) % 1.0
                val rake = Math.exp(-rakePhase * 7.0)
                val overall = 1.0 - progress * 0.55

                val sample = body * rake * overall * volume
                pcm[i] = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            }
            return createWavFile(sampleRate, pcm)
        }

        /**
         * The boss reveal: a low sine swell with a little band-limited noise on
         * top, so it reads as an enormous cat standing up rather than a tone.
         */
        private fun generateRumblePcm(durationSec: Double, volume: Double): ByteArray {
            val sampleRate = 44100
            val numSamples = (durationSec * sampleRate).toInt()
            val pcm = ShortArray(numSamples)

            var seed = 0x51E5_7A11L
            var lowpass = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples

                // Pitch creeps up as she rises; the swell peaks near the end.
                val freq = 42.0 + progress * 26.0
                val body = sin(2.0 * PI * freq * t) + 0.4 * sin(2.0 * PI * freq * 2.0 * t)

                seed = seed xor (seed shl 13)
                seed = seed xor (seed ushr 7)
                seed = seed xor (seed shl 17)
                val white = ((seed and 0xFFFF).toDouble() / 32768.0) - 1.0
                lowpass += (white - lowpass) * 0.05 // heavy low-pass: floor grumble

                val swell = Math.sin(PI * progress).coerceAtLeast(0.0)
                val sample = (body * 0.8 + lowpass * 0.5) * swell * volume
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
