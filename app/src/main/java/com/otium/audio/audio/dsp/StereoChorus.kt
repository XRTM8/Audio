package com.otium.audio.audio.dsp

import kotlin.math.PI
import kotlin.math.sin

/**
 * Multi-Voice Studio Stereo Chorus.
 *
 * Utilizes twin circularly interpolated delay lines driven by a quadrature Low Frequency
 * Oscillator (90-degree stereo phase offset). Creates rich, warm vocal doubling,
 * acoustic instrument dimension, and 3D stereo width.
 */
class StereoChorus {

    var strength: Int = 0 // 0 to 100%

    private var sampleRateHz: Int = 44_100

    private val bufferSize = 4096
    private val bufferL = FloatArray(bufferSize)
    private val bufferR = FloatArray(bufferSize)
    private var writeIndex = 0

    // LFO parameters
    private var lfoPhase = 0.0
    private val lfoRateHz = 1.15 // Gentle, organic studio modulation rate

    // Nominal delay: ~14ms, modulation depth: ~3.5ms
    private var baseDelaySamples = 617f
    private var depthSamples = 154f

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
    }

    fun process(left: Float, right: Float): Pair<Float, Float> {
        if (strength <= 0) return Pair(left, right)

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

        // Left LFO: sin(phase), Right LFO: sin(phase + PI/2) [90-degree stereo spread]
        val modL = sin(lfoPhase).toFloat()
        val modR = sin(lfoPhase + PI * 0.5).toFloat()

        val delayL = baseDelaySamples + depthSamples * modL
        val delayR = baseDelaySamples + depthSamples * modR

        val chorusL = readInterpolated(bufferL, delayL)
        val chorusR = readInterpolated(bufferR, delayR)

        // Advance write pointer
        writeIndex = (writeIndex + 1) and (bufferSize - 1)

        val dryMix = 1f - (wetMix * 0.25f)
        val wet = wetMix * 0.65f

        return Pair(
            left * dryMix + chorusL * wet,
            right * dryMix + chorusR * wet
        )
    }

    private fun readInterpolated(buffer: FloatArray, delay: Float): Float {
        var readPos = writeIndex - delay
        while (readPos < 0f) readPos += bufferSize
        while (readPos >= bufferSize) readPos -= bufferSize

        val index0 = readPos.toInt()
        val frac = readPos - index0
        val index1 = (index0 + 1) and (bufferSize - 1)

        val s0 = buffer[index0]
        val s1 = buffer[index1]
        return s0 + frac * (s1 - s0)
    }
}
