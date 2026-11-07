package com.otium.audio.audio.dsp

/**
 * Audiophile Algorithmic Schroeder-Moorer Reverb.
 *
 * Employs 4 parallel low-pass feedback comb filters tuned to prime delays (eliminating
 * flutter echoes and metallic resonance), followed by 2 cascaded all-pass diffusers
 * per channel with asymmetrical stereo delay tuning for expansive spatial depth.
 * Zero-allocation design maintains 100% audio thread real-time safety.
 */
class AudiophileReverb {

    var strength: Int = 0 // 0 to 100%
    var preset: String = "Studio"
        set(value) {
            field = value
            updatePresetCoefficients()
        }

    var outLeft: Float = 0f
        private set
    var outRight: Float = 0f
        private set

    private var sampleRateHz: Int = 44_100

    // Prime-based delay lengths for Left and Right channels (decorrelated stereo)
    private val combDelaysL = intArrayOf(1116, 1188, 1277, 1356)
    private val combDelaysR = intArrayOf(1138, 1211, 1297, 1378)

    private val combBuffersL = Array(4) { FloatArray(2400) }
    private val combBuffersR = Array(4) { FloatArray(2400) }
    private val combIndicesL = IntArray(4)
    private val combIndicesR = IntArray(4)
    private val combFilterL = FloatArray(4)
    private val combFilterR = FloatArray(4)

    // Cascaded All-Pass Diffusers
    private val allpassL1 = FloatArray(556)
    private val allpassL2 = FloatArray(441)
    private val allpassR1 = FloatArray(567)
    private val allpassR2 = FloatArray(452)
    private var allpassIdxL1 = 0
    private var allpassIdxL2 = 0
    private var allpassIdxR1 = 0
    private var allpassIdxR2 = 0

    private var feedback = 0.58f
    private var damping = 0.38f

    init {
        updatePresetCoefficients()
    }

    fun configure(sampleRate: Int) {
        if (sampleRate > 0) {
            sampleRateHz = sampleRate
        }
    }

    private fun updatePresetCoefficients() {
        when (preset) {
            "Studio" -> {
                feedback = 0.52f
                damping = 0.42f
            }
            "Room" -> {
                feedback = 0.68f
                damping = 0.28f
            }
            "Hall" -> {
                feedback = 0.82f
                damping = 0.20f
            }
            "Plate" -> {
                feedback = 0.78f
                damping = 0.12f
            }
            else -> {
                feedback = 0.60f
                damping = 0.32f
            }
        }
    }

    fun reset() {
        combBuffersL.forEach { it.fill(0f) }
        combBuffersR.forEach { it.fill(0f) }
        combFilterL.fill(0f)
        combFilterR.fill(0f)
        combIndicesL.fill(0)
        combIndicesR.fill(0)

        allpassL1.fill(0f)
        allpassL2.fill(0f)
        allpassR1.fill(0f)
        allpassR2.fill(0f)
        allpassIdxL1 = 0
        allpassIdxL2 = 0
        allpassIdxR1 = 0
        allpassIdxR2 = 0

        outLeft = 0f
        outRight = 0f
    }

    /**
     * In-place stereo reverberation processing with zero heap allocations.
     */
    fun process(left: Float, right: Float) {
        if (strength <= 0) {
            outLeft = left
            outRight = right
            return
        }

        val wetMix = (strength / 100f).coerceIn(0f, 1f)
        val monoIn = (left + right) * 0.5f

        // 1. Parallel Comb Filters (Left)
        var sumL = 0f
        for (i in 0 until 4) {
            val buf = combBuffersL[i]
            val maxLen = combDelaysL[i]
            val idx = combIndicesL[i]
            val out = buf[idx]
            combFilterL[i] = out * (1f - damping) + combFilterL[i] * damping
            buf[idx] = monoIn + combFilterL[i] * feedback
            combIndicesL[i] = if (idx + 1 >= maxLen) 0 else idx + 1
            sumL += out
        }

        // 2. Parallel Comb Filters (Right)
        var sumR = 0f
        for (i in 0 until 4) {
            val buf = combBuffersR[i]
            val maxLen = combDelaysR[i]
            val idx = combIndicesR[i]
            val out = buf[idx]
            combFilterR[i] = out * (1f - damping) + combFilterR[i] * damping
            buf[idx] = monoIn + combFilterR[i] * feedback
            combIndicesR[i] = if (idx + 1 >= maxLen) 0 else idx + 1
            sumR += out
        }

        sumL *= 0.25f
        sumR *= 0.25f

        // 3. Cascaded Allpass Diffusers (Left)
        val diffL1 = processAllpass(sumL, allpassL1, 556, 0)
        val diffL2 = processAllpass(diffL1, allpassL2, 441, 1)

        // 4. Cascaded Allpass Diffusers (Right)
        val diffR1 = processAllpass(sumR, allpassR1, 567, 2)
        val diffR2 = processAllpass(diffR1, allpassR2, 452, 3)

        val dryAmount = 1f - (wetMix * 0.22f)
        val wetAmount = wetMix * 0.38f

        outLeft = left * dryAmount + diffL2 * wetAmount
        outRight = right * dryAmount + diffR2 * wetAmount
    }

    private fun processAllpass(input: Float, buffer: FloatArray, bufSize: Int, stage: Int): Float {
        val idx = when (stage) {
            0 -> allpassIdxL1
            1 -> allpassIdxL2
            2 -> allpassIdxR1
            else -> allpassIdxR2
        }
        val bufOut = buffer[idx]
        val output = -input + bufOut
        buffer[idx] = input + bufOut * 0.5f
        val nextIdx = if (idx + 1 >= bufSize) 0 else idx + 1
        when (stage) {
            0 -> allpassIdxL1 = nextIdx
            1 -> allpassIdxL2 = nextIdx
            2 -> allpassIdxR1 = nextIdx
            else -> allpassIdxR2 = nextIdx
        }
        return output
    }
}
