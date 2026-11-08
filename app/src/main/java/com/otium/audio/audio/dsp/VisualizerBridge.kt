package com.otium.audio.audio.dsp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-performance, zero-overhead visualizer bridge.
 * Dispatches real-time FFT spectrum frequency magnitudes computed in Rust
 * to UI visualizer widgets at an energy-efficient rate (~20 FPS) without GC churn.
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
