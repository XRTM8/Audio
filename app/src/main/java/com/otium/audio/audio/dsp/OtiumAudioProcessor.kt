package com.otium.audio.audio.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.otium.audio.playback.EqualizerController
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.exp

/**
 * The whole playback chain in one processor: pre-amp, ten band equalizer, clarity enhancer,
 * stereo chorus, audiophile reverb, and a transparent peak limiter. It runs inside Media3's sink,
 * so the decoder and the output stay platform code while everything that shapes the sound belongs to us.
 */
@UnstableApi
class OtiumAudioProcessor(
    private val controller: EqualizerController
) : BaseAudioProcessor() {

    private var channelCount = 2
    private var sampleRateHz = 44_100
    private var encoding = C.ENCODING_PCM_16BIT

    private val filters = Array(BAND_FREQUENCIES_HZ.size) { Array(CHANNELS_MAX) { Biquad() } }
    private val bassBoostFilter = Array(CHANNELS_MAX) { Biquad() }
    private var preAmp = Biquad()
    private var limiter = PeakLimiter()

    private val reverb = AudiophileReverb()
    private val chorus = StereoChorus()
    private val clarity = ClarityEnhancer()

    private var appliedGains = FloatArray(BAND_FREQUENCIES_HZ.size)
    private var appliedBassBoost = -1
    private var appliedVirtualizer = -1
    private var bypass = true
    private var appliedPreAmpDb = Float.NaN
    private var nativeEnginePtr: Long = 0L
    private var nativeSampleRateHz: Int = 0
    private var lastSyncedState: com.otium.audio.playback.EqualizerState? = null
    private var lastSpectrumTimeMs: Long = 0L
    private val spectrumSampleBuffer = FloatArray(512)
    private val scratchPeakBuffer = FloatArray(4)

    override fun onConfigure(inputAudioFormat: androidx.media3.common.audio.AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        channelCount = inputAudioFormat.channelCount
        sampleRateHz = inputAudioFormat.sampleRate
        encoding = inputAudioFormat.encoding
        if (channelCount > CHANNELS_MAX) return AudioProcessor.AudioFormat.NOT_SET

        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        appliedPreAmpDb = Float.NaN
        lastSyncedState = null
        rebuildFilters()
        limiter.configure(sampleRateHz)
        reverb.configure(sampleRateHz)
        chorus.configure(sampleRateHz)
        clarity.configure(sampleRateHz)

        if (NativeAudioDsp.isAvailable) {
            if (nativeEnginePtr == 0L || nativeSampleRateHz != sampleRateHz) {
                if (nativeEnginePtr != 0L) {
                    NativeAudioDsp.destroyEngine(nativeEnginePtr)
                }
                nativeEnginePtr = NativeAudioDsp.createEngine(sampleRateHz)
                nativeSampleRateHz = sampleRateHz
            } else {
                NativeAudioDsp.resetEngine(nativeEnginePtr)
            }
        }

        return inputAudioFormat
    }

    override fun onFlush() {
        lastSyncedState = null
        VisualizerBridge.reset()
        if (nativeEnginePtr != 0L) {
            NativeAudioDsp.resetEngine(nativeEnginePtr)
        }
        filters.forEach { perChannel -> perChannel.forEach { it.reset() } }
        bassBoostFilter.forEach { it.reset() }
        preAmp.reset()
        limiter.reset()
        reverb.reset()
        chorus.reset()
        clarity.reset()
    }

    override fun onReset() {
        if (nativeEnginePtr != 0L) {
            NativeAudioDsp.destroyEngine(nativeEnginePtr)
            nativeEnginePtr = 0L
            nativeSampleRateHz = 0
        }
        onFlush()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        syncFromController()

        val output = replaceOutputBuffer(remaining)
        if (bypass || channelCount <= 0 || channelCount > CHANNELS_MAX) {
            output.put(inputBuffer)
            output.flip()
            sampleSpectrumIfNeeded(output, encoding == C.ENCODING_PCM_FLOAT, remaining)
            return
        }

        // --- Fast Path: High-Performance Rust SIMD DSP Engine ---
        if (nativeEnginePtr != 0L && channelCount == 2 && output.isDirect) {
            val startPos = inputBuffer.position()
            try {
                output.put(inputBuffer)
                output.flip()
                val isFloat = encoding == C.ENCODING_PCM_FLOAT
                NativeAudioDsp.processDirectBuffer(
                    enginePtr = nativeEnginePtr,
                    buffer = output,
                    byteOffset = 0,
                    byteLength = remaining,
                    isFloat = isFloat
                )
                sampleSpectrumIfNeeded(output, isFloat, remaining)
                return
            } catch (_: Throwable) {
                // Graceful fallback to Kotlin engine on unexpected JNI boundary error
                inputBuffer.position(startPos)
                output.clear()
            }
        }

        val startPos = inputBuffer.position()
        try {
            val inputSamples = remaining / bytesPerSample()
            val frames = inputSamples / channelCount
            if (channelCount == 2) {
                for (f in 0 until frames) {
                    var left = readSample(inputBuffer, f * 2)
                    var right = readSample(inputBuffer, f * 2 + 1)

                    left = preAmp.process(left)
                    right = preAmp.process(right)

                    if (appliedBassBoost > 0) {
                        left = bassBoostFilter[0].process(left)
                        right = bassBoostFilter[1].process(right)
                    }

                    val bandFilters = filters
                    for (band in bandFilters.indices) {
                        left = bandFilters[band][0].process(left)
                        right = bandFilters[band][1].process(right)
                    }

                    if (appliedVirtualizer > 0) {
                        val widthFactor = 1.0f + (appliedVirtualizer / 1000f) * 1.5f
                        val mid = (left + right) * 0.5f
                        val side = (left - right) * 0.5f * widthFactor
                        left = mid + side
                        right = mid - side
                    }

                    if (clarity.strength > 0) {
                        left = clarity.process(left, 0)
                        right = clarity.process(right, 1)
                    }

                    if (chorus.strength > 0) {
                        chorus.process(left, right)
                        left = chorus.outLeft
                        right = chorus.outRight
                    }

                    if (reverb.strength > 0) {
                        reverb.process(left, right)
                        left = reverb.outLeft
                        right = reverb.outRight
                    }

                    left = limiter.process(left, 0)
                    right = limiter.process(right, 1)

                    writeSample(output, left)
                    writeSample(output, right)
                }
            } else {
                repeat(inputSamples) {
                    val channel = it % channelCount
                    var sample = readSample(inputBuffer, it)

                    sample = preAmp.process(sample)
                    if (appliedBassBoost > 0) {
                        sample = bassBoostFilter[channel].process(sample)
                    }
                    val bandFilters = filters
                    for (band in bandFilters.indices) {
                        sample = bandFilters[band][channel].process(sample)
                    }
                    if (clarity.strength > 0) {
                        sample = clarity.process(sample, channel)
                    }
                    sample = limiter.process(sample, channel)
                    writeSample(output, sample)
                }
            }
            output.flip()
        } catch (_: Throwable) {
            inputBuffer.position(startPos)
            output.clear()
            output.put(inputBuffer)
            output.flip()
        }
    }

    private fun rebuildFilters() {
        val q = 1.4f
        for (channel in 0 until channelCount) {
            bassBoostFilter[channel].configurePeaking(
                sampleRateHz = sampleRateHz,
                centreHz = 75f,
                gainDb = 0f,
                q = 0.9f
            )
        }
        for (band in BAND_FREQUENCIES_HZ.indices) {
            for (channel in 0 until channelCount) {
                filters[band][channel].configurePeaking(
                    sampleRateHz = sampleRateHz,
                    centreHz = BAND_FREQUENCIES_HZ[band],
                    gainDb = 0f,
                    q = q
                )
            }
        }
    }

    /** Copies the live UI state into coefficients, only when something actually changed. */
    private fun syncFromController() {
        val state = controller.state.value
        if (state == lastSyncedState) return
        lastSyncedState = state

        if (state.preAmpDb != appliedPreAmpDb) {
            appliedPreAmpDb = state.preAmpDb
            preAmp.configureGain(state.preAmpDb)
        }

        if (state.bassBoostStrength != appliedBassBoost) {
            appliedBassBoost = state.bassBoostStrength
            val boostDb = if (state.isEnabled && state.bassBoostStrength > 0) {
                (state.bassBoostStrength / 1000f) * 9.0f
            } else 0f
            for (channel in 0 until channelCount) {
                bassBoostFilter[channel].configurePeaking(
                    sampleRateHz = sampleRateHz,
                    centreHz = 75f,
                    gainDb = boostDb,
                    q = 0.9f
                )
            }
        }
        appliedVirtualizer = if (state.isEnabled) state.virtualizerStrength else 0

        val levels = state.bands
        for (band in BAND_FREQUENCIES_HZ.indices) {
            val gain = if (state.isEnabled) levels.getOrNull(band)?.levelDb ?: 0f else 0f
            if (gain == appliedGains[band]) continue
            appliedGains[band] = gain
            for (channel in 0 until channelCount) {
                filters[band][channel].configurePeaking(
                    sampleRateHz = sampleRateHz,
                    centreHz = BAND_FREQUENCIES_HZ[band],
                    gainDb = gain,
                    q = 1.4f
                )
            }
        }

        reverb.strength = if (state.isEnabled) state.reverbStrength else 0
        reverb.preset = state.reverbPreset
        chorus.strength = if (state.isEnabled) state.chorusStrength else 0
        clarity.strength = if (state.isEnabled) state.clarityStrength else 0

        // Limiting is needed when something in the chain adds gain or effects.
        val boosting = state.preAmpDb > 0.05f ||
            levels.any { it.levelDb > 0.05f } ||
            (state.isEnabled && state.bassBoostStrength > 0) ||
            (state.isEnabled && state.virtualizerStrength > 0) ||
            reverb.strength > 0 ||
            chorus.strength > 0 ||
            clarity.strength > 0

        limiter.enabled = state.isEnabled && boosting
        bypass = !state.isEnabled || (!boosting && levels.none { it.levelDb < -0.05f })

        if (nativeEnginePtr != 0L) {
            val presetIdx = when (state.reverbPreset.lowercase()) {
                "room" -> 1
                "hall" -> 2
                "plate" -> 3
                else -> 0
            }
            val gains = FloatArray(BAND_FREQUENCIES_HZ.size) { i ->
                var g = if (state.isEnabled) levels.getOrNull(i)?.levelDb ?: 0f else 0f
                if (state.isEnabled && state.bassBoostStrength > 0) {
                    val extraBass = (state.bassBoostStrength / 1000f) * 6.5f
                    if (i == 0) g += extraBass * 0.85f // 31Hz
                    if (i == 1) g += extraBass         // 62Hz
                    if (i == 2) g += extraBass * 0.45f // 125Hz
                }
                g
            }
            NativeAudioDsp.setParameters(
                enginePtr = nativeEnginePtr,
                preAmpDb = state.preAmpDb,
                bandGainsDb = gains,
                virtualizerStrength = if (state.isEnabled) state.virtualizerStrength else 0,
                reverbStrength = if (state.isEnabled) state.reverbStrength else 0,
                reverbPreset = presetIdx,
                chorusStrength = if (state.isEnabled) state.chorusStrength else 0,
                clarityStrength = if (state.isEnabled) state.clarityStrength else 0,
                isEnabled = state.isEnabled
            )
        }
    }

    private fun bytesPerSample(): Int =
        if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2

    private fun readSample(buffer: ByteBuffer, index: Int): Float =
        if (encoding == C.ENCODING_PCM_FLOAT) {
            Math.max(-1f, Math.min(1f, buffer.float))
        } else {
            buffer.short.toFloat() / 32_768f
        }

    private fun writeSample(buffer: ByteBuffer, value: Float) {
        val clamped = value.coerceIn(-1f, 1f)
        if (encoding == C.ENCODING_PCM_FLOAT) {
            buffer.putFloat(clamped)
        } else {
            buffer.putShort((clamped * 32_767f).toInt().toShort())
        }
    }

    /**
     * Feed forward peak limiter. Two milliseconds of attack keeps transients from slipping
     * through, and a hundred milliseconds of release avoids audible pumping. The final tanh
     * is a safety net for inter sample peaks, not the primary mechanism.
     */
    private class PeakLimiter {
        var enabled = true

        private var attackCoefficient = 0f
        private var releaseCoefficient = 0f
        private var sampleRate = 0
        private var gain = 1f
        private val channelGain = FloatArray(CHANNELS_MAX) { 1f }

        fun configure(sampleRateHz: Int) {
            if (sampleRateHz == sampleRate) return
            sampleRate = sampleRateHz
            attackCoefficient = exp(-1.0 / (0.002 * sampleRate)).toFloat()
            releaseCoefficient = exp(-1.0 / (0.100 * sampleRate)).toFloat()
        }

        fun process(sample: Float, channel: Int): Float {
            if (!enabled) {
                channelGain[channel] = 1f
                return sample
            }

            val magnitude = abs(sample)
            val desired = if (magnitude > THRESHOLD) THRESHOLD / magnitude else 1f

            val current = channelGain[channel]
            val smoothed = if (desired < current) {
                attackCoefficient * (current - desired) + desired
            } else {
                releaseCoefficient * (current - desired) + desired
            }
            channelGain[channel] = smoothed
            return sample * smoothed
        }

        fun reset() {
            gain = 1f
            channelGain.fill(1f)
        }

        private companion object {
            const val THRESHOLD = 0.985f
        }
    }

    private fun sampleSpectrumIfNeeded(buffer: ByteBuffer, isFloat: Boolean, byteLength: Int) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastSpectrumTimeMs < 50L) return
        lastSpectrumTimeMs = now

        try {
            // Direct zero-copy Rust FFT spectrum calculation directly from native buffer memory
            if (buffer.isDirect) {
                val directBands = NativeAudioDsp.analyzeSpectrumDirectBuffer(
                    buffer = buffer,
                    byteOffset = buffer.position(),
                    byteLength = byteLength,
                    isFloat = isFloat,
                    bandCount = 16
                )
                if (directBands != null) {
                    VisualizerBridge.updateBands(directBands)
                }
                if (nativeEnginePtr != 0L) {
                    if (NativeAudioDsp.readPeakLevelsDirect(nativeEnginePtr, scratchPeakBuffer)) {
                        VisualizerBridge.updatePeakLevelsDirect(scratchPeakBuffer)
                    }
                }
                if (directBands != null) {
                    return
                }
            }

            val sampleCount = if (isFloat) (byteLength / 4).coerceAtMost(512) else (byteLength / 2).coerceAtMost(512)
            if (sampleCount < 64) return

            val origPos = buffer.position()
            if (isFloat) {
                for (i in 0 until sampleCount) {
                    spectrumSampleBuffer[i] = buffer.getFloat(origPos + i * 4)
                }
            } else {
                for (i in 0 until sampleCount) {
                    spectrumSampleBuffer[i] = buffer.getShort(origPos + i * 2) / 32768.0f
                }
            }
            val bands = NativeAudioDsp.analyzeSpectrum(spectrumSampleBuffer, 16)
            if (bands != null) {
                VisualizerBridge.updateBands(bands)
            }
        } catch (_: Throwable) {}
    }

    companion object {
        /** Octave spaced, matching the ten sliders in the sheet. */
        val BAND_FREQUENCIES_HZ = floatArrayOf(
            31f, 62f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f
        )
        private const val CHANNELS_MAX = 2
    }
}
