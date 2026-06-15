package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

class AudioEngine {

    companion object {
        private const val TAG = "AudioEngine"
        const val SAMPLE_RATE = 21000 // Lightweight sample rate for fast mathematical loop
        private const val CHANNELS = 2 // Stereo
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    // Thread orchestration
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isRunning = false

    // Real-time parameters (Volatiles or Atomic for lock-free loop performance)
    @Volatile var bpm: Int = 120
    @Volatile var drive: Float = 0.0f          // 0.0 to 2.0 Overdrive
    @Volatile var delayFeedback: Float = 0.2f  // 0.0 to 0.9 Echo
    @Volatile var reverbDecay: Float = 0.3f    // 0.0 to 0.95 Reverb
    @Volatile var wideness: Float = 0.5f       // 0.0 to 1.0 Stereo Widener
    @Volatile var compressThreshold: Float = -20f // -60f to 0f
    @Volatile var compressRatio: Float = 4.0f     // 1.0 to 20.0
    @Volatile var vocalsGain: Float = 1.0f     // Synth volume modulator
    @Volatile var beatsGain: Float = 1.0f      // Drum volume modulator

    // 10 EQ Bands in dB (-12.0f to 12.0f)
    // Map: 31Hz(0), 62Hz(1), 125Hz(2), 250Hz(3), 500Hz(4), 1kHz(5), 2kHz(6), 4kHz(7), 8kHz(8), 16kHz(9)
    val eqBands = FloatArray(10) { 0.0f }

    // Live Voice frequency trigger
    @Volatile var activeMidiNote: Float = 0f // 0 = off, else is frequency in Hz

    // State flows to feed the 60FPS dynamic spectrum analyzer Canvas
    private val _spectrumFlow = MutableStateFlow(FloatArray(10) { 0.1f })
    val spectrumFlow = _spectrumFlow.asStateFlow()

    private val _vuLeftFlow = MutableStateFlow(0.0f)
    val vuLeftFlow = _vuLeftFlow.asStateFlow()

    private val _vuRightFlow = MutableStateFlow(0.0f)
    val vuRightFlow = _vuRightFlow.asStateFlow()

    // Recording buffer capability
    @Volatile var isRecordingToMemory = false
    private val recordingByteStream = ByteArrayOutputStream()

    // Biquad filters list (one filter per band for Left & Right channels)
    private val filtersLeft = Array(10) { BiquadFilter() }
    private val filtersRight = Array(10) { BiquadFilter() }

    // Echo Line buffer (Stereo)
    private val delayBufferMax = SAMPLE_RATE * 2 // Max 2 seconds delay
    private val delayBufferLeft = FloatArray(delayBufferMax)
    private val delayBufferRight = FloatArray(delayBufferMax)
    private var delayWritePointer = 0

    // Simple Reverb comb filters pointers and buffers
    private val rev1Buffer = FloatArray(441)
    private val rev2Buffer = FloatArray(343)
    private val rev3Buffer = FloatArray(511)
    private val rev4Buffer = FloatArray(289)
    private var rev1Ptr = 0
    private var rev2Ptr = 0
    private var rev3Ptr = 0
    private var rev4Ptr = 0

    init {
        recalculateFilterBands()
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            ENCODING
        )
        val finalBufferSize = countPowerOfTwo(minBufSize * 2).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                ENCODING,
                finalBufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioTrack", e)
            isRunning = false
            return
        }

        playbackJob = scope.launch {
            audioLoop()
        }
    }

    fun stop() {
        isRunning = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
        audioTrack = null
    }

    fun playTone(freq: Float) {
        activeMidiNote = freq
    }

    fun stopTone() {
        activeMidiNote = 0f
    }

    fun triggerRecordingStart() {
        recordingByteStream.reset()
        isRecordingToMemory = true
    }

    fun triggerRecordingStop(): ByteArray {
        isRecordingToMemory = false
        val bytes = recordingByteStream.toByteArray()
        recordingByteStream.reset()
        return bytes
    }

    // Design Peaking-EQ Coefficient Formula
    fun recalculateFilterBands() {
        val frequencies = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
        val qValue = 1.2f // Bandwidth Q factor

        for (i in 0 until 10) {
            val dbGain = eqBands[i]
            filtersLeft[i].setPeakingEq(frequencies[i], dbGain, qValue)
            filtersRight[i].setPeakingEq(frequencies[i], dbGain, qValue)
        }
    }

    // Helper: Next immediate high buffer block
    private fun countPowerOfTwo(n: Int): Int {
        var num = 2
        while (num < n) {
            num = num shl 1
        }
        return num
    }

    private suspend fun audioLoop() {
        val bufferLength = 1024 // Double channel buffer: write stereo frames
        val writeBuffer = ShortArray(bufferLength * 2)

        // Wave phase counters
        var mainPhase = 0.0
        var leadPhase = 0.0
        var hihatPhase = 0.0

        var stepCounter = 0
        var totalSamplesProcessed = 0L

        // Envelope tracker for compressor
        var compressorEnvelope = 0.0f

        while (isRunning) {
            // Recalculate duration of sixteenth steps
            // Each 16th note step = (60.0s / BPM) / 4 = 15.0 / BPM
            val stepSizeInSamples = (SAMPLE_RATE * (15.0 / bpm)).toInt()

            for (sampleIdx in 0 until bufferLength) {
                // Determine step sequencer phase
                val absoluteSample = totalSamplesProcessed + sampleIdx
                val currentStep = ((absoluteSample / stepSizeInSamples) % 16).toInt()

                // Check step trigger
                val isNewStep = (absoluteSample % stepSizeInSamples) == 0L

                // 2. SYNTHESIZE INSTRUMENT SOUNDS
                // Step sequencer rhythm drums (Kick, Snare, Hihat)
                var kickLeft = 0.0
                var snareLeft = 0.0
                var hihatLeft = 0.0

                // A. Mathematical Sub-Bass Kick
                // Step sequence: Kick plays on 0, 8, 12
                val sampleInKick = (absoluteSample % stepSizeInSamples).toDouble()
                if (currentStep == 0 || currentStep == 8 || currentStep == 12) {
                    val kickDuration = stepSizeInSamples * 0.75
                    if (sampleInKick < kickDuration) {
                        // Exponential pitch glide from 150Hz down to 45Hz
                        val fraction = sampleInKick / kickDuration
                        val freqGlide = 150.0 * (1.1 - fraction) + 40.0
                        val phaseKick = 2.0 * PI * freqGlide * (sampleInKick / SAMPLE_RATE)
                        // Descending envelope amplitude
                        val env = (1.0 - fraction) * (1.0 - fraction)
                        kickLeft = env * sin(phaseKick)
                    }
                }

                // B. Mathematical Snare
                // Snare plays on 4, 12
                if (currentStep == 4 || currentStep == 12) {
                    val snareDuration = stepSizeInSamples * 0.6
                    if (sampleInKick < snareDuration) {
                        val fraction = sampleInKick / snareDuration
                        // Snare is composed of low-frequency sine snap + high white noise snap
                        val snapFreq = 180.0 * (1.0 - fraction) + 120.0
                        val sinePhase = 2.0 * PI * snapFreq * (sampleInKick / SAMPLE_RATE)
                        val snapVal = sin(sinePhase) * 0.5

                        // White noise
                        val whiteNoise = (Math.random() * 2.0 - 1.0)
                        val env = (1.0 - fraction) * (1.0 - fraction)
                        snareLeft = env * (snapVal * 0.4 + whiteNoise * 0.6)
                    }
                }

                // C. Mathematical Hihat
                // Hihat plays on 2, 6, 10, 14
                if (currentStep % 4 == 2) {
                    val hatDuration = stepSizeInSamples * 0.15
                    if (sampleInKick < hatDuration) {
                        val fraction = sampleInKick / hatDuration
                        // High pass filtered hi-hat white noise
                        val whiteNoise = (Math.random() * 2.0 - 1.0)
                        val env = (1.0 - fraction) * (1.0 - fraction)
                        hihatLeft = env * whiteNoise * 0.25
                    }
                }

                // Cumulative Drum Stem
                var beatSource = (kickLeft + snareLeft + hihatLeft) * beatsGain

                // D. Lead Synth Oscillator (Interactively configured)
                var synthSource = 0.0
                if (activeMidiNote > 0f) {
                    // Interactive keyboard notes synthesizer
                    // We generate a warm organic sawtooth / triangle waveform with sweet vibrato
                    val frequency = activeMidiNote
                    val vibratoOffset = 1.0 + 0.012 * sin(2.0 * PI * 6.5 * (absoluteSample / SAMPLE_RATE))
                    leadPhase += (2.0 * PI * (frequency * vibratoOffset)) / SAMPLE_RATE
                    if (leadPhase > 2.0 * PI) leadPhase -= 2.0 * PI

                    // Warm triangle-saw wave blend
                    val sawtooth = (leadPhase / PI) - 1.0
                    val triangle = if (leadPhase < PI) {
                        (2.0 * leadPhase / PI) - 1.0
                    } else {
                        3.0 - (2.0 * leadPhase / PI)
                    }

                    // Dynamic wave shaper
                    synthSource = (sawtooth * 0.4 + triangle * 0.6) * 0.7 * vocalsGain
                } else {
                    // Let's generate an ambient backing chord progression based on current beat block steps!
                    // Chord cycles every 4 bars: Step 0-15 Am, 16-31 C, 32-47 G, 48-63 F
                    val barCycle = (absoluteSample / (stepSizeInSamples * 16)) % 4
                    val chords = when (barCycle.toInt()) {
                        0 -> floatArrayOf(220f, 261.6f, 329.6f) // A3, C4, E4 (A minor)
                        1 -> floatArrayOf(261.6f, 329.6f, 392f) // C4, E4, G4 (C major)
                        2 -> floatArrayOf(196f, 246.9f, 293.7f) // G3, B3, D4 (G major)
                        else -> floatArrayOf(174.6f, 220f, 261.6f) // F3, A3, C4 (F major)
                    }

                    // Three beautiful oscillators playing Chord pads with an atmospheric slow LFO
                    val lfo = 0.4 + 0.2 * sin(2.0 * PI * 0.2 * (absoluteSample / SAMPLE_RATE))
                    for (f in chords) {
                        // Blend phases
                        val phaseIncrement = (2.0 * PI * f) / SAMPLE_RATE
                        val oscVal = sin((absoluteSample * phaseIncrement) % (2.0 * PI))
                        synthSource += oscVal * 0.18 * lfo * vocalsGain
                    }
                }

                // Combines raw audio sources (mix desk)
                var leftFrameRaw = beatSource + synthSource
                var rightFrameRaw = beatSource + synthSource

                // --- 2. DSP ENGINE SIGNAL CHAIN ---

                // FX Stage 1: Overdrive Drive Saturation CLI
                if (drive > 0.01f) {
                    val boost = 1.0f + drive * 2.0f
                    leftFrameRaw = tanh(leftFrameRaw * boost) / (boost * 0.8)
                    rightFrameRaw = tanh(rightFrameRaw * boost) / (boost * 0.8)
                }

                // FX Stage 2: Stereo Widener (panning delay phases)
                val wideFactor = wideness.coerceIn(0f, 1f)
                val leftWidened = leftFrameRaw * (1.0f - wideFactor * 0.3f) + rightFrameRaw * (wideFactor * 0.3f)
                val rightWidened = rightFrameRaw * (1.0f - wideFactor * 0.3f) + leftFrameRaw * (wideFactor * 0.3f)

                var leftProcessed = leftWidened
                var rightProcessed = rightWidened

                // FX Stage 3: Real 10-Band Graphic/Parametric Equalizer
                // Compute Left and Right biquads cascade
                for (b in 0 until 10) {
                    leftProcessed = filtersLeft[b].process(leftProcessed)
                    rightProcessed = filtersRight[b].process(rightProcessed)
                }

                // FX Stage 4: Echo Delay Feedback
                val delayTimeSec = 0.37f // 370ms standard delay
                val delaySamplesCount = (SAMPLE_RATE * delayTimeSec).toInt().coerceAtMost(delayBufferMax - 1)
                val echoReadPtr = (delayWritePointer - delaySamplesCount + delayBufferMax) % delayBufferMax

                val echoLeft = delayBufferLeft[echoReadPtr]
                val echoRight = delayBufferRight[echoReadPtr]

                // Inject delay feedback
                val currentDelayFeedback = delayFeedback.coerceIn(0.0f, 0.9f)
                leftProcessed += echoLeft * currentDelayFeedback
                rightProcessed += echoRight * currentDelayFeedback

                // Store in Delay line
                delayBufferLeft[delayWritePointer] = leftProcessed.toFloat()
                delayBufferRight[delayWritePointer] = rightProcessed.toFloat()
                delayWritePointer = (delayWritePointer + 1) % delayBufferMax

                // FX Stage 5: Reverb Room Space (Physical atmospheric taps)
                val revDecayVal = reverbDecay.coerceIn(0f, 0.95f)
                
                // Comb 1
                val r1Out = rev1Buffer[rev1Ptr]
                rev1Buffer[rev1Ptr] = (leftProcessed + r1Out * revDecayVal).toFloat()
                rev1Ptr = (rev1Ptr + 1) % rev1Buffer.size

                // Comb 2
                val r2Out = rev2Buffer[rev2Ptr]
                rev2Buffer[rev2Ptr] = (rightProcessed + r2Out * revDecayVal).toFloat()
                rev2Ptr = (rev2Ptr + 1) % rev2Buffer.size

                // Comb 3
                val r3Out = rev3Buffer[rev3Ptr]
                rev3Buffer[rev3Ptr] = (leftProcessed + r3Out * (revDecayVal * 0.85f)).toFloat()
                rev3Ptr = (rev3Ptr + 1) % rev3Buffer.size

                // Comb 4
                val r4Out = rev4Buffer[rev4Ptr]
                rev4Buffer[rev4Ptr] = (rightProcessed + r4Out * (revDecayVal * 0.85f)).toFloat()
                rev4Ptr = (rev4Ptr + 1) % rev4Buffer.size

                val reverbLeft = (r1Out + r3Out) * 0.25
                val reverbRight = (r2Out + r4Out) * 0.25

                leftProcessed += reverbLeft
                rightProcessed += reverbRight

                // FX Stage 6: Enterprise Dynamic Compressor
                // Envelope follower (with simple leak rate)
                val inputAbs = sqrt(leftProcessed * leftProcessed + rightProcessed * rightProcessed).toFloat()
                val alpha = 0.05f // Attack/release filter coefficient
                compressorEnvelope = alpha * inputAbs + (1.0f - alpha) * compressorEnvelope

                // Compress gain if amplitude exceeds threshold
                val thresholdAmp = Math.pow(10.0, compressThreshold / 20.0).toFloat()
                var compressionGain = 1.0f
                if (compressorEnvelope > thresholdAmp) {
                    val excessDb = 20.0f * Math.log10((compressorEnvelope / thresholdAmp).toDouble()).toFloat()
                    val targetReductionDb = excessDb * (1.0f - 1.0f / compressRatio)
                    compressionGain = Math.pow(10.0, (-targetReductionDb / 20.0).toDouble()).toFloat()
                }

                leftProcessed *= compressionGain
                rightProcessed *= compressionGain

                // Limiter Guard: absolute hard clipper to prevent audio clipping distortion on android output
                val finalL = leftProcessed.coerceIn(-0.99, 0.99)
                val finalR = rightProcessed.coerceIn(-0.99, 0.99)

                // Convert FLOAT to 16-bit PCM Short values (-32768 to 32767)
                writeBuffer[sampleIdx * 2] = (finalL * 32767.0).toInt().toShort()
                writeBuffer[sampleIdx * 2 + 1] = (finalR * 32767.0).toInt().toShort()
            }

            // Write final short buffers to audio track
            audioTrack?.write(writeBuffer, 0, bufferLength * 2)

            // Save recording to memory if recording trigger active
            if (isRecordingToMemory) {
                for (s in writeBuffer) {
                    // convert Short to 2 little-endian bytes mapping
                    recordingByteStream.write(s.toInt() and 0xFF)
                    recordingByteStream.write((s.toInt() shr 8) and 0xFF)
                }
            }

            // Parse spectrum arrays and VU coordinates for the 60FPS analyzer
            totalSamplesProcessed += bufferLength

            // Calculate current peaks for VU meter states
            var maxL = 0f
            var maxR = 0f
            for (i in 0 until bufferLength) {
                val valL = Math.abs(writeBuffer[i * 2] / 32767.0f)
                val valR = Math.abs(writeBuffer[i * 2 + 1] / 32767.0f)
                if (valL > maxL) maxL = valL
                if (valR > maxR) maxR = valR
            }

            _vuLeftFlow.value = maxL
            _vuRightFlow.value = maxR

            // Dynamic modeling of FFT-like Spectrum coefficients driven directly by EQ bands + active signals
            // Real-time animation logic: we blend dynamic randomized noise + active synth/drum patterns
            val activeFreqPower = if (activeMidiNote > 0f) 0.8f else 0.2f
            val baseSpectrum = FloatArray(10) { i ->
                val eqBoost = eqBands[i] / 12.0f // normalized modifier -1..1
                val scale = when (i) {
                    0, 1 -> 0.45f + (beatsGain * 0.35f) // Bass bands (Kick / Snare rhythm)
                    2, 3 -> 0.35f + (beatsGain * 0.15f)
                    4, 5 -> 0.25f + activeFreqPower // Speech / Synth frequencies
                    6, 7 -> 0.18f + (vocalsGain * 0.2f)
                    else -> 0.12f // Hihat / High frequencies
                }
                val randomJitter = (Math.random().toFloat() * 0.12f) + 0.04f
                val freqResponse = (1.0f + eqBoost * 0.7f) * (scale + randomJitter)
                freqResponse.coerceIn(0.01f, 1.0f)
            }
            _spectrumFlow.value = baseSpectrum
        }
    }
}

// Custom optimized Biquad implementation to perform actual physical filtering calculations.
class BiquadFilter {
    // Coefficients
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // History registers
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    // Set peaking EQ filter coefficients using standard formulas
    fun setPeakingEq(frequency: Float, dbGain: Float, q: Float) {
        val omega = 2.0 * PI * frequency / AudioEngine.SAMPLE_RATE
        val cosOmega = Math.cos(omega)
        val sinOmega = Math.sin(omega)
        val alpha = sinOmega / (2.0 * q)
        val a = Math.pow(10.0, dbGain / 40.0) // square root of gain factor

        b0 = 1.0 + alpha * a
        b1 = -2.0 * cosOmega
        b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        a1 = -2.0 * cosOmega
        a2 = 1.0 - alpha / a

        // Normalize coefficients by dividing by a0
        b0 /= a0
        b1 /= a0
        b2 /= a0
        a1 /= a0
        a2 /= a0
    }
}
