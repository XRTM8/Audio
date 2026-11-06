package com.otium.audio.audio.dsp

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * One peaking biquad section, transposed direct form II.
 *
 * Coefficients follow the RBJ audio EQ cookbook. Transposed form II is used because it is
 * numerically well behaved with 32-bit floats and needs only two state values per channel.
 */
class Biquad {

    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    private var z1 = 0f
    private var z2 = 0f

    fun configurePeaking(sampleRateHz: Int, centreHz: Float, gainDb: Float, q: Float) {
        val amp = 10f.pow(gainDb / 40f)
        val omega = 2.0 * Math.PI * centreHz / sampleRateHz
        val sinOmega = sin(omega).toFloat()
        val cosOmega = cos(omega).toFloat()
        val alpha = sinOmega / (2f * q)

        val a0 = 1f + alpha / amp
        b0 = (1f + alpha * amp) / a0
        b1 = (-2f * cosOmega) / a0
        b2 = (1f - alpha * amp) / a0
        a1 = (-2f * cosOmega) / a0
        a2 = (1f - alpha / amp) / a0
    }

    fun configureGain(gainDb: Float) {
        val linear = 10f.pow(gainDb / 20f)
        b0 = linear
        b1 = 0f
        b2 = 0f
        a1 = 0f
        a2 = 0f
    }

    fun reset() {
        z1 = 0f
        z2 = 0f
    }

    fun process(input: Float): Float {
        val output = b0 * input + z1
        z1 = b1 * input - a1 * output + z2
        z2 = b2 * input - a2 * output
        return output
    }
}
