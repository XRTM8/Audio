package com.otium.audio.audio.dsp

import kotlin.math.abs

/**
 * Clarity & De-Harshing Audiophile Processor.
 *
 * Designed to address high-frequency listening fatigue ("الصوت المؤذي"):
 * 1. Precision De-Harsh Peaking Notch at 6.2 kHz (the primary ear fatigue & harsh sibilance resonance band).
 * 2. Ultra-smooth Air Presence High-Shelf at 12 kHz (crystalline detail, breath, and spatial nuance).
 * 3. Analog soft-knee transient saturation to eliminate brittle digital edge.
 */
class ClarityEnhancer {

    var strength: Int = 0 // 0 to 100%

    private var sampleRateHz: Int = 44_100

    private val deHarshFilters = Array(2) { Biquad() }
    private val airFilters = Array(2) { Biquad() }

    private var appliedStrength = -1

    fun configure(sampleRate: Int) {
        if (sampleRate > 0) {
            sampleRateHz = sampleRate
            appliedStrength = -1
            recomputeFilters()
        }
    }

    fun reset() {
        deHarshFilters.forEach { it.reset() }
        airFilters.forEach { it.reset() }
    }

    private fun recomputeFilters() {
        val fraction = (strength / 100f).coerceIn(0f, 1f)
        if (strength == appliedStrength) return
        appliedStrength = strength

        if (strength <= 0) {
            for (ch in 0 until 2) {
                deHarshFilters[ch].configureGain(0f)
                airFilters[ch].configureGain(0f)
            }
            return
        }

        // De-harsh: gentle notch up to -3.2 dB at 6200 Hz (tames piercing fatigue)
        val deHarshGainDb = -3.2f * fraction
        // Air presence: gentle high lift up to +2.4 dB at 12000 Hz (clarity and breath)
        val airGainDb = 2.4f * fraction

        for (ch in 0 until 2) {
            deHarshFilters[ch].configurePeaking(
                sampleRateHz = sampleRateHz,
                centreHz = 6200f,
                gainDb = deHarshGainDb,
                q = 1.8f
            )
            airFilters[ch].configurePeaking(
                sampleRateHz = sampleRateHz,
                centreHz = 12000f,
                gainDb = airGainDb,
                q = 1.2f
            )
        }
    }

    fun process(sample: Float, channel: Int): Float {
        if (strength <= 0) return sample
        if (strength != appliedStrength) {
            recomputeFilters()
        }

        val ch = if (channel in 0..1) channel else 0

        // Step 1: Smooth ear-piercing sibilance
        var out = deHarshFilters[ch].process(sample)

        // Step 2: Elevate crystalline air & precision micro-detail
        out = airFilters[ch].process(out)

        // Step 3: Gentle analog saturation curve for transients > 0.82 magnitude
        val absVal = abs(out)
        if (absVal > 0.82f) {
            val delta = absVal - 0.82f
            val compressed = 0.82f + delta / (1f + delta * 1.5f)
            out = if (out > 0f) compressed else -compressed
        }

        return out
    }
}
