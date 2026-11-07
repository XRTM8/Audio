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

    // --- 1. Audio Processing & DSP Engine ---
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

    // --- 2. Fast Audio Metadata & Tag Scanner (Lofty) ---
    external fun scanAudioFile(path: String): String?
    external fun extractArtwork(path: String): ByteArray?
    external fun writeAudioTags(path: String, title: String, artist: String, album: String): Boolean

    // --- 3. In-Memory Intelligent Arabic Fuzzy Search Engine ---
    external fun searchIndexInit(
        ids: LongArray,
        titles: Array<String>,
        artists: Array<String>,
        albums: Array<String>
    )
    external fun searchIndexQuery(query: String, limit: Int): LongArray
    external fun searchIndexAddOrUpdate(id: Long, title: String, artist: String, album: String)
    external fun searchIndexRemove(id: Long)

    // --- 4. Waveform & FFT Spectrum Analyzer ---
    external fun computeSpectrum(samples: FloatArray, bandCount: Int): FloatArray?
    external fun computeWaveformPeaks(pcmData: ByteArray, isFloat: Boolean, points: Int): FloatArray?

    // --- 5. High-Speed LRC Lyrics Parser & Synchronizer ---
    external fun parseLrc(lrcText: String): String?
    external fun findActiveLrcLine(timestamps: LongArray, positionMs: Long): Int
}
