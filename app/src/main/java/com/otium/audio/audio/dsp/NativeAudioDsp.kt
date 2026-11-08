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
        virtualizerStrength: Int,
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
    external fun getEnginePeakLevels(enginePtr: Long): FloatArray?

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
    external fun searchIndexQuery(query: String, limit: Int): LongArray?
    external fun searchIndexAddOrUpdate(id: Long, title: String, artist: String, album: String)
    external fun searchIndexRemove(id: Long)

    // --- 4. Waveform & FFT Spectrum Analyzer ---
    external fun computeSpectrum(samples: FloatArray, bandCount: Int): FloatArray?
    external fun computeSpectrumDirectBuffer(
        buffer: ByteBuffer,
        byteOffset: Int,
        byteLength: Int,
        isFloat: Boolean,
        bandCount: Int
    ): FloatArray?
    external fun computeWaveformPeaks(pcmData: ByteArray, isFloat: Boolean, points: Int): FloatArray?

    // --- 5. High-Speed LRC Lyrics Parser & Synchronizer ---
    external fun parseLrc(lrcText: String): String?
    external fun findActiveLrcLine(timestamps: LongArray, positionMs: Long): Int

    // --- 6. Audiophile Native Helpers ---
    data class AudioFileDetails(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val bitrate: Int,
        val sampleRate: Int,
        val format: String,
        val hasArtwork: Boolean
    )

    fun readAudioMetadata(path: String): AudioFileDetails? {
        if (!isAvailable || path.isBlank()) return null
        return try {
            val json = scanAudioFile(path) ?: return null
            val obj = org.json.JSONObject(json)
            AudioFileDetails(
                title = obj.optString("title", ""),
                artist = obj.optString("artist", ""),
                album = obj.optString("album", ""),
                durationMs = obj.optLong("durationMs", 0L),
                bitrate = obj.optInt("bitrate", 0),
                sampleRate = obj.optInt("sampleRate", 44100),
                format = obj.optString("format", "AUDIO"),
                hasArtwork = obj.optBoolean("hasArtwork", false)
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun extractEmbeddedArtworkToFile(context: android.content.Context, relativePath: String, songId: Long): android.net.Uri? {
        if (!isAvailable || relativePath.isBlank()) return null
        return try {
            val bytes = extractArtwork(relativePath) ?: return null
            val cacheDir = java.io.File(context.cacheDir, "embedded_art").apply { mkdirs() }
            val target = java.io.File(cacheDir, "$songId.jpg")
            if (!target.exists() || target.length() == 0L) {
                target.writeBytes(bytes)
            }
            android.net.Uri.fromFile(target)
        } catch (_: Throwable) {
            null
        }
    }

    fun extractWaveformPeaks(pcmBytes: ByteArray, isFloat: Boolean = false, points: Int = 100): FloatArray? {
        if (!isAvailable || pcmBytes.isEmpty()) return null
        return try {
            computeWaveformPeaks(pcmBytes, isFloat, points)
        } catch (_: Throwable) {
            null
        }
    }

    fun analyzeSpectrum(samples: FloatArray, bandCount: Int = 16): FloatArray? {
        if (!isAvailable || samples.isEmpty()) return null
        return try {
            computeSpectrum(samples, bandCount)
        } catch (_: Throwable) {
            null
        }
    }

    fun analyzeSpectrumDirectBuffer(
        buffer: ByteBuffer,
        byteOffset: Int = 0,
        byteLength: Int = buffer.remaining(),
        isFloat: Boolean = false,
        bandCount: Int = 16
    ): FloatArray? {
        if (!isAvailable || !buffer.isDirect || byteLength <= 0) return null
        return try {
            computeSpectrumDirectBuffer(buffer, byteOffset, byteLength, isFloat, bandCount)
        } catch (_: Throwable) {
            null
        }
    }

    data class PeakRmsLevels(
        val peakLeft: Float,
        val peakRight: Float,
        val rmsLeft: Float,
        val rmsRight: Float
    )

    fun readPeakLevels(enginePtr: Long): PeakRmsLevels? {
        if (!isAvailable || enginePtr == 0L) return null
        return try {
            val arr = getEnginePeakLevels(enginePtr) ?: return null
            if (arr.size >= 4) {
                PeakRmsLevels(arr[0], arr[1], arr[2], arr[3])
            } else null
        } catch (_: Throwable) {
            null
        }
    }
}
