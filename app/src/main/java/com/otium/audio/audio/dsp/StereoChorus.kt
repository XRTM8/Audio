package com.otium.audio.audio.dsp

import kotlin.math.PI
import kotlin.math.sin

/**
 * Multi-Voice Studio Stereo Chorus.
 *
 * Utilizes twin circularly interpolated delay lines driven by a quadrature Low Frequency
 * Oscillator (90-degree stereo phase offset) with 1024-point wavetable lookups (zero trigonometric CPU overhead).
 * In-place processing guarantees zero heap allocations on the real-time audio thread.
 */
class StereoChorus {

    var strength: Int = 0 // 0 to 100%

    var outLeft: Float = 0f
        private set
    var outRight: Float = 0f
        private set

    private var sampleRateHz: Int = 44_100

    private val bufferSize = 4096
    private val bufferMask = bufferSize - 1
    private val bufferL = FloatArray(bufferSize)
    private val bufferR = FloatArray(bufferSize)
    private var writeIndex = 0

    // LFO parameters
    private var lfoPhase = 0.0
    private val lfoRateHz = 1.15 // Gentle, organic studio modulation rate

    // Nominal delay: ~14ms, modulation depth: ~3.5ms
    private var baseDelaySamples = 617f
    private var depthSamples = 154f

    companion object {
        private const val TABLE_SIZE = 1024
        private const val TABLE_MASK = TABLE_SIZE - 1
        private val SINE_TABLE = FloatArray(TABLE_SIZE) { i ->
            sin(i * 2.0 * PI / TABLE_SIZE).toFloat()
        }
    }

    fun configure(sampleRate: Int) {
        if (sampleRate > 0) {
            sampleRateHz = sampleRate
            baseDelaySamples = 0.014f * sampleRate
            depthSamples = 0.0035f * sampleRate
        }
    }

    fun reset() {
        bufferL.fill(0f)
        bufferR.fill(0f)
        writeIndex = 0
        lfoPhase = 0.0
        outLeft = 0f
        outRight = 0f
    }

    fun process(left: Float, right: Float) {
        if (strength <= 0) {
            outLeft = left
            outRight = right
            return
        }

        val wetMix = (strength / 100f).coerceIn(0f, 1f)

        // Write input samples to circular buffer
        bufferL[writeIndex] = left
        bufferR[writeIndex] = right

        // Update LFO
        val phaseIncrement = (2.0 * PI * lfoRateHz) / sampleRateHz
        lfoPhase += phaseIncrement
        if (lfoPhase >= 2.0 * PI) {
            lfoPhase -= 2.0 * PI
        }

        // Fast wavetable lookup: 1 CPU cycle instead of costly Math.sin()
        val indexL = ((lfoPhase * (TABLE_SIZE / (2.0 * PI))).toInt()) and TABLE_MASK
        val indexR = (indexL + 256) and TABLE_MASK // 90-degree quadrature stereo phase shift

        val modL = SINE_TABLE[indexL]
        val modR = SINE_TABLE[indexR]

        val delayL = baseDelaySamples + depthSamples * modL
        val delayR = baseDelaySamples + depthSamples * modR

        val chorusL = readInterpolated(bufferL, delayL)
        val chorusR = readInterpolated(bufferR, delayR)

        // Advance write pointer
        writeIndex = (writeIndex + 1) and bufferMask

        val dryMix = 1f - (wetMix * 0.25f)
        val wet = wetMix * 0.65f

        outLeft = left * dryMix + chorusL * wet
        outRight = right * dryMix + chorusR * wet
    }

    private fun readInterpolated(buffer: FloatArray, delay: Float): Float {
        var readPos = writeIndex - delay
        while (readPos < 0f) readPos += bufferSize
        while (readPos >= bufferSize) readPos -= bufferSize

        val index0 = readPos.toInt()
        val frac = readPos - index0
        val index1 = (index0 + 1) and bufferMask

        val s0 = buffer[index0]
        val s1 = buffer[index1]
        return s0 + frac * (s1 - s0)
    }
}
