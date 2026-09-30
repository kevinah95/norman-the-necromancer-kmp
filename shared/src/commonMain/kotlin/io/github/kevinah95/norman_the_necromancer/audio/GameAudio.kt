package io.github.kevinah95.norman_the_necromancer.audio

import korlibs.audio.sound.*
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

/**
 * Procedural Synthesizer & Dynamic Adaptive Soundtrack for Norman The Necromancer.
 *
 * Faithfully ports Dan Prince's Web Audio API procedural synthesis engine (sounds.ts):
 * - Ambient Organ: Periodic wave harmonic series [-0.8, 1, 0.8, 0.8, -0.8, -0.8, -1]
 *   arpeggiating A1 (55Hz) and A0 (27.5Hz) with cathedral convolution reverb.
 * - Procedural Bass: Sawtooth wave running an algorithmic A-Harmonic-Minor bassline.
 * - Sub Kick: 808-style pitch-dropping sine wave (150Hz -> 0Hz) lowpassed at 80Hz.
 * - The King's Boss Theme: 3-part Gothic organ counterpoint (soprano, tenor, pedal bass).
 *
 * Dynamic level progression:
 * - Level 0: Ambient Organ drone
 * - Level 1: Bassline enters
 * - Level 2+: Kick drum joins the beat
 * - Shop: Kick drum drops out for calm shopping ambience
 * - Level 9 (The King): Normal synths fade out; The King's Gothic Organ theme takes over!
 * - King Phase 4: Kick drum re-enters for the climactic rush!
 */
object GameAudio {
    var enabled: Boolean = true
    private val scope = CoroutineScope(Dispatchers.Default)

    private var musicStream: NormanMusicStream? = null
    private var soundChannel: SoundChannel? = null
    private var isPlaying = false

    // Music constants
    const val BPM: Double = 240.0
    const val SAMPLE_RATE: Int = 44100

    fun play() {
        if (!enabled || isPlaying) return
        isPlaying = true
        scope.launch {
            try {
                val stream = NormanMusicStream(SAMPLE_RATE)
                musicStream = stream
                soundChannel = stream.toSound().play()
                useLevelSynths(0)
            } catch (_: Throwable) {
                // Platform audio fallback
            }
        }
    }

    fun useLevelSynths(level: Int) {
        val s = musicStream ?: return
        if (level >= 9) {
            // Boss theme: normal synths exit, King's theme starts
            s.targetOrganVol = 0.0f
            s.targetBassVol = 0.0f
            s.targetKickVol = 0.0f
            s.targetKingVol = 0.55f
        } else {
            s.targetKingVol = 0.0f
            s.targetOrganVol = 0.45f
            s.targetBassVol = if (level >= 1) 0.5f else 0.0f
            s.targetKickVol = if (level >= 2) 0.8f else 0.0f
        }
    }

    fun useShopSynths() {
        val s = musicStream ?: return
        // In the shop, the kick drum exits
        s.targetKickVol = 0.0f
    }

    fun onKingPhase4() {
        val s = musicStream ?: return
        // Kick re-enters during King Phase 4
        s.targetKickVol = 0.8f
    }

    fun stop() {
        try {
            soundChannel?.stop()
        } catch (_: Throwable) {}
        soundChannel = null
        musicStream = null
        isPlaying = false
    }

    fun playCast() {}
    fun playResurrect() {}
    fun playBuy() {}
    fun playHit() {}

    // Note frequencies: fn = 440 * 2^(step / 12)
    fun freq(step: Int): Double = 440.0 * 2.0.pow(step.toDouble() / 12.0)
}

class NormanMusicStream(
    val audioRate: Int = GameAudio.SAMPLE_RATE
) : AudioStream(audioRate, 2) {

    override val finished: Boolean get() = false
    override suspend fun seek(position: Duration) {}
    override suspend fun clone(): AudioStream = this

    var targetKickVol: Float = 0.0f
    var targetOrganVol: Float = 0.0f
    var targetBassVol: Float = 0.0f
    var targetKingVol: Float = 0.0f

    var currentKickVol: Float = 0.0f
    var currentOrganVol: Float = 0.0f
    var currentBassVol: Float = 0.0f
    var currentKingVol: Float = 0.0f

    var masterGain: Float = 0.0f
    var targetMasterGain: Float = 0.5f

    private var sampleClock: Long = 0L

    val kickBuffer: FloatArray by lazy { generateKick(audioRate) }
    val organBuffer: FloatArray by lazy { generateAmbientOrgan(audioRate) }
    val bassBuffer: FloatArray by lazy { generateBass(audioRate) }
    val kingBuffer: FloatArray by lazy { generateKingTheme(audioRate) }

    override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
        val kick = kickBuffer
        val organ = organBuffer
        val bass = bassBuffer
        val king = kingBuffer

        // Volume ramp alpha (~0.8s transition time)
        val rampAlpha = 1.0f / (audioRate.toFloat() * 0.8f)

        for (i in 0 until length) {
            val t = sampleClock++

            currentKickVol += (targetKickVol - currentKickVol) * rampAlpha
            currentOrganVol += (targetOrganVol - currentOrganVol) * rampAlpha
            currentBassVol += (targetBassVol - currentBassVol) * rampAlpha
            currentKingVol += (targetKingVol - currentKingVol) * rampAlpha
            masterGain += (targetMasterGain - masterGain) * (rampAlpha * 0.25f)

            val k = kick[(t % kick.size).toInt()] * currentKickVol
            val o = organ[(t % organ.size).toInt()] * currentOrganVol
            val b = bass[(t % bass.size).toInt()] * currentBassVol
            val kg = king[(t % king.size).toInt()] * currentKingVol

            val mixed = ((k + o + b + kg) * masterGain).coerceIn(-1.0f, 1.0f)
            val sampleVal = (mixed * 32767.0f).toInt().toShort()
            val sample = AudioSample(sampleVal)

            out.set(0, offset + i, sample)
            out.set(1, offset + i, sample)
        }
        return length
    }

    companion object {
        // Semitones relative to A4 (0 = 440Hz)
        const val A3 = -12
        const val B3 = -10
        const val C3 = -9
        const val D3 = -7
        const val E3 = -5
        const val F3 = -4
        const val Ab4 = -1
        const val A4 = 0
        const val B4 = 2
        const val C4 = 3
        const val D4 = 5
        const val E4 = 7
        const val F4 = 8
        const val Ab5 = 11
        const val A5 = 12

        // Note lengths at 240 BPM (1 beat = 0.25s)
        const val W = 1.0   // Whole note = 4 beats = 1.0s
        const val H = 2.0   // Half note = 2 beats = 0.5s
        const val Q = 4.0   // Quarter note = 1 beat = 0.25s
        const val E_LEN = 8.0 // Eighth note = 0.5 beat = 0.125s
        const val W2 = 0.5  // W/2 = 8 beats = 2.0s

        val A_HARMONIC_MINOR = intArrayOf(
            A3, B3, C3, D3, F3, E3, Ab4, A4,
            A4, B4, C4, D4, F4, E4, Ab5, A5
        )

        // Web Audio periodic wave Fourier harmonics for Dan Prince's organ
        val ORGAN_WEIGHTS = doubleArrayOf(-0.8, 1.0, 0.8, 0.8, -0.8, -0.8, -1.0)

        fun organWave(phase: Double): Double {
            var sum = 0.0
            for (k in 1..7) {
                val hp = 2.0 * PI * k * phase
                sum += ORGAN_WEIGHTS[k - 1] * (cos(hp) + sin(hp))
            }
            return sum * 0.18
        }

        fun generateKick(rate: Int): FloatArray {
            // 1 measure = 1.0s = 44100 samples
            val totalSamples = rate
            val out = FloatArray(totalSamples)
            val noteDuration = (rate * 0.5).toInt() // Half note H

            var phase = 0.0
            var lowpass = 0.0
            val alpha = (2.0 * PI * 80.0) / rate // 80 Hz lowpass filter

            for (i in 0 until noteDuration) {
                val t = i.toDouble() / rate
                val decay = exp(-14.0 * t)
                val freq = 150.0 * decay
                phase += (2.0 * PI * freq) / rate

                val gain = exp(-10.0 * t)
                val raw = sin(phase) * gain
                lowpass += alpha * (raw - lowpass)
                out[i] = (lowpass * 1.5).toFloat()
            }
            return out
        }

        fun generateAmbientOrgan(rate: Int): FloatArray {
            // Pattern: [A4, E, A3, E] at retune -36
            // A4 - 36 = -36 (A1 = 55.0Hz)
            // A3 - 36 = -48 (A0 = 27.5Hz)
            // Loops every 0.25s. We pre-render 4.0s (16 loops)
            val totalSamples = rate * 4
            val out = FloatArray(totalSamples)
            val noteSamples = (rate * 0.125).toInt() // Eighth note = 0.125s

            val fA1 = GameAudio.freq(A4 - 36)
            val fA0 = GameAudio.freq(A3 - 36)

            var phase = 0.0
            var hpPrevIn = 0.0
            var hpPrevOut = 0.0
            val hpAlpha = 1.0 / (1.0 + 2.0 * PI * 200.0 / rate) // 200Hz highpass

            for (i in 0 until totalSamples) {
                val stepInLoop = i % (noteSamples * 2)
                val currentFreq = if (stepInLoop < noteSamples) fA1 else fA0
                phase += currentFreq / rate

                val raw = organWave(phase)
                // Highpass filter
                val hpOut = hpAlpha * (hpPrevOut + raw - hpPrevIn)
                hpPrevIn = raw
                hpPrevOut = hpOut

                out[i] = hpOut.toFloat()
            }

            // Apply lush cathedral reverb
            applyReverb(out, rate, decaySeconds = 2.5, mix = 0.45f)
            return out
        }

        fun generateBass(rate: Int): FloatArray {
            // Pattern [a, a, a, b] = 4 measures = 4.0s
            val totalSamples = rate * 4
            val out = FloatArray(totalSamples)

            // Generate deterministic groovy pattern using Dan Prince's random algorithm
            val rng = Random(1337)
            fun createPattern(beats: Int, notes: List<Int>): List<Pair<Int, Double>> {
                var remaining = beats.toDouble()
                val list = mutableListOf<Pair<Int, Double>>()
                val lengths = doubleArrayOf(E_LEN, Q)
                while (remaining > 0.001) {
                    val len = lengths[rng.nextInt(lengths.size)]
                    val dur = 1.0 / (len / 4.0) // E_LEN: 0.5 beat, Q: 1.0 beat
                    if (remaining - dur < -0.001) continue
                    remaining -= dur
                    val note = notes[rng.nextInt(notes.size)]
                    list.add(Pair(note, dur * 0.25)) // seconds
                }
                return list
            }

            val notesA = A_HARMONIC_MINOR.toList() + listOf(A3, A3, A3, A3, A3)
            val notesB = A_HARMONIC_MINOR.toList()
            val patternA = createPattern(4, notesA)
            val patternB = createPattern(4, notesB)
            val fullPattern = patternA + patternA + patternA + patternB

            var samplePos = 0
            for ((note, durationSec) in fullPattern) {
                val f = GameAudio.freq(note - 24) // 2 octaves down
                val noteLenSamples = (durationSec * rate).toInt()
                var phase = 0.0

                for (i in 0 until noteLenSamples) {
                    if (samplePos + i >= totalSamples) break
                    val t = i.toDouble() / rate
                    phase += f / rate
                    if (phase > 1.0) phase -= floor(phase)

                    // Sawtooth wave
                    val saw = 2.0 * (phase - 0.5)
                    // Pluck envelope: attack 0.25, exponential decay
                    val envelope = 0.25 * exp(-t / 0.16)
                    out[samplePos + i] += (saw * envelope).toFloat()
                }
                samplePos += noteLenSamples
            }

            applyReverb(out, rate, decaySeconds = 1.8, mix = 0.35f)
            return out
        }

        fun generateKingTheme(rate: Int): FloatArray {
            // 8 measures = 8.0s
            val totalSamples = rate * 8
            val out = FloatArray(totalSamples)

            val p1 = listOf(Pair(A4, H), Pair(B4, H), Pair(C4, H), Pair(B4, H))
            val p2 = listOf(Pair(A4, H), Pair(B4, H), Pair(C4, H), Pair(D4, H))
            val melody = p1 + p1 + p1 + p2 // 8 measures (32 beats)
            val bass = listOf(Pair(A4, W2), Pair(B4, W2), Pair(C4, W2), Pair(B4, W2)) // 8 measures

            // Render Layer 1: Organ 1 (retune 0)
            renderMelody(out, melody, 0, rate, gain = 0.25)
            // Render Layer 2: Organ 2 (retune -12, 1 octave lower)
            renderMelody(out, melody, -12, rate, gain = 0.20)
            // Render Layer 3: King's Bass (retune -36, pedal bass)
            renderMelody(out, bass, -36, rate, gain = 0.30)

            applyReverb(out, rate, decaySeconds = 3.0, mix = 0.5f)
            return out
        }

        private fun renderMelody(
            out: FloatArray,
            pattern: List<Pair<Int, Double>>,
            retune: Int,
            rate: Int,
            gain: Double
        ) {
            var samplePos = 0
            for ((note, len) in pattern) {
                val durationSec = (60.0 / GameAudio.BPM) * (1.0 / (len / 4.0))
                val samples = (durationSec * rate).toInt()
                val f = GameAudio.freq(note + retune)
                var phase = 0.0

                for (i in 0 until samples) {
                    if (samplePos + i >= out.size) break
                    phase += f / rate
                    val wave = organWave(phase)
                    out[samplePos + i] += (wave * gain).toFloat()
                }
                samplePos += samples
            }
        }

        private fun applyReverb(buffer: FloatArray, rate: Int, decaySeconds: Double, mix: Float) {
            // Multi-tap Schroeder comb feedback filter for lush digital reverb
            val delays = intArrayOf(
                (rate * 0.0297).toInt(),
                (rate * 0.0371).toInt(),
                (rate * 0.0411).toInt(),
                (rate * 0.0437).toInt()
            )
            val feedback = exp(-3.0 / (decaySeconds * rate)).toFloat().coerceIn(0.5f, 0.85f)

            for (delay in delays) {
                val combBuf = FloatArray(delay)
                var combIdx = 0
                for (i in buffer.indices) {
                    val input = buffer[i]
                    val delayed = combBuf[combIdx]
                    val output = input + delayed * feedback
                    combBuf[combIdx] = output
                    combIdx = (combIdx + 1) % delay
                    buffer[i] = buffer[i] * (1.0f - mix) + output * (mix * 0.25f)
                }
            }
        }
    }
}
