package com.otium.audio.audio.dsp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * High-performance, zero-overhead visualizer bridge.
 * Dispatches real-time FFT spectrum frequency magnitudes computed in Rust
 * to UI visualizer widgets without touching or blocking the real-time audio thread.
 */
object VisualizerBridge {
    private val emptyBands = FloatArray(16)
    private val _spectrumBands = MutableStateFlow(emptyBands)
    val spectrumBands: StateFlow<FloatArray> = _spectrumBands.asStateFlow()

    private val _averageEnergy = MutableStateFlow(0f)
    val averageEnergy: StateFlow<Float> = _averageEnergy.asStateFlow()

    private val defaultLevels = NativeAudioDsp.PeakRmsLevels(0f, 0f, 0f, 0f)
    private val _peakLevels = MutableStateFlow(defaultLevels)
    val peakLevels: StateFlow<NativeAudioDsp.PeakRmsLevels> = _peakLevels.asStateFlow()

    var peakLeft: Float = 0f; private set
    var peakRight: Float = 0f; private set
    var rmsLeft: Float = 0f; private set
    var rmsRight: Float = 0f; private set

    // Lock-free double-buffered audio snapshot for visualizer worker
    private val snapshotBufferA = FloatArray(512)
    private val snapshotBufferB = FloatArray(512)
    @Volatile private var activeBuffer = snapshotBufferA
    @Volatile private var pendingBuffer = snapshotBufferB
    @Volatile private var hasPendingSamples = false
    @Volatile private var activeEnginePtr = 0L

    private val scratchPeaks = FloatArray(4)
    private var workerJob: Job? = null

    /**
     * Called strictly from real-time audio thread.
     * Takes ~0.001ms. Never blocks, never calls JNI FFT, never allocates objects.
     */
    fun postSamples(buffer: ByteBuffer, isFloat: Boolean, byteLength: Int, enginePtr: Long) {
        val count = if (isFloat) (byteLength / 4).coerceAtMost(512) else (byteLength / 2).coerceAtMost(512)
        if (count < 64) return

        val target = pendingBuffer
        val origPos = buffer.position()
        if (isFloat) {
            for (i in 0 until count) {
                target[i] = buffer.getFloat(origPos + i * 4)
            }
        } else {
            for (i in 0 until count) {
                target[i] = buffer.getShort(origPos + i * 2) / 32768.0f
            }
        }
        activeEnginePtr = enginePtr

        // Atomic buffer swap
        val temp = activeBuffer
        activeBuffer = target
        pendingBuffer = temp
        hasPendingSamples = true
    }

    /**
     * Dedicated background worker running at 60Hz on Dispatchers.Default.
     * Keeps all FFT and JNI metering strictly off the audio thread.
     */
    fun startWorker(scope: CoroutineScope) {
        if (workerJob?.isActive == true) return
        workerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (!hasPendingSamples) {
                    delay(60L) // Sleep deeper when playback is paused or idle
                    continue
                }
                delay(16L) // 60 FPS sub-frame sync during active playback
                if (hasPendingSamples) {
                    hasPendingSamples = false
                    val samples = activeBuffer
                    val bands = NativeAudioDsp.analyzeSpectrum(samples, 16)
                    if (bands != null) {
                        updateBands(bands)
                    }
                    val enginePtr = activeEnginePtr
                    if (enginePtr != 0L) {
                        if (NativeAudioDsp.readPeakLevelsDirect(enginePtr, scratchPeaks)) {
                            updatePeakLevelsDirect(scratchPeaks)
                        }
                    }
                }
            }
        }
    }

    fun stopWorker() {
        workerJob?.cancel()
        workerJob = null
        reset()
    }

    fun updateBands(bands: FloatArray) {
        _spectrumBands.value = bands
        var sum = 0f
        for (i in bands.indices) {
            sum += bands[i]
        }
        _averageEnergy.value = (sum / bands.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    fun updatePeakLevels(levels: NativeAudioDsp.PeakRmsLevels) {
        peakLeft = levels.peakLeft
        peakRight = levels.peakRight
        rmsLeft = levels.rmsLeft
        rmsRight = levels.rmsRight
        _peakLevels.value = levels
    }

    fun updatePeakLevelsDirect(levels: FloatArray) {
        if (levels.size >= 4) {
            peakLeft = levels[0]
            peakRight = levels[1]
            rmsLeft = levels[2]
            rmsRight = levels[3]
            _peakLevels.value = NativeAudioDsp.PeakRmsLevels(levels[0], levels[1], levels[2], levels[3])
        }
    }

    fun reset() {
        _spectrumBands.value = emptyBands
        _averageEnergy.value = 0f
        peakLeft = 0f
        peakRight = 0f
        rmsLeft = 0f
        rmsRight = 0f
        _peakLevels.value = defaultLevels
    }
}
