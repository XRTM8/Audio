package com.otium.audio.audio.dsp

import java.nio.ByteBuffer

/**
 * High-performance JNI bridge to the Rust Native DSP Audio Engine (`libotium_dsp.so`).
 * Provides direct zero-copy buffer processing with SIMD acceleration and 0 heap allocations.
 * Includes graceful fallback to pure Kotlin DSP if the native library is unavailable.
 */
object NativeAudioDsp {

    val isAvailable: Boolean

    init {
        var loaded = false
        try {
            System.loadLibrary("otium_dsp")
            loaded = true
        } catch (_: Throwable) {
            loaded = false
        }
        isAvailable = loaded
    }

    external fun createEngine(sampleRate: Int): Long
    external fun destroyEngine(enginePtr: Long)
    external fun resetEngine(enginePtr: Long)
    external fun setParameters(
        enginePtr: Long,
        preAmpDb: Float,
        bandGainsDb: FloatArray,
        reverbStrength: Int,
        reverbPreset: Int,
        chorusStrength: Int,
        clarityStrength: Int,
        isEnabled: Boolean
    )
    external fun processDirectBuffer(
        enginePtr: Long,
        buffer: ByteBuffer,
        byteOffset: Int,
        byteLength: Int,
        isFloat: Boolean
    )
}
