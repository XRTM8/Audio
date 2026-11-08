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

    fun updateBands(bands: FloatArray) {
        _spectrumBands.value = bands
    }

    fun reset() {
        _spectrumBands.value = emptyBands
    }
}
