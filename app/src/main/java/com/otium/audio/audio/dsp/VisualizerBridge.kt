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

    fun updateBands(bands: FloatArray) {
        _spectrumBands.value = bands
        var sum = 0f
        for (i in bands.indices) {
            sum += bands[i]
        }
        _averageEnergy.value = (sum / bands.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    fun reset() {
        _spectrumBands.value = emptyBands
        _averageEnergy.value = 0f
    }
}
