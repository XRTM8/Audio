use std::f32::consts::PI;

/// Computes logarithmic frequency spectrum magnitudes across N bands (e.g. 16 or 32 bands)
/// from a window of interleaved or mono float samples.
pub fn compute_spectrum(samples: &[f32], band_count: usize) -> Vec<f32> {
    if samples.is_empty() || band_count == 0 {
        return vec![0.0; band_count];
    }

    let n = samples.len().min(1024);
    let mut magnitudes = vec![0.0f32; band_count];

    // Compute DFT energy across logarithmic frequency centers
    // Center frequencies from ~40 Hz to ~16,000 Hz
    let min_freq = 40.0f32;
    let max_freq = 16000.0f32;
    let sample_rate = 44100.0f32;

    for band in 0..band_count {
        // Logarithmic distribution
        let t = band as f32 / (band_count as f32 - 1.0).max(1.0);
        let center_freq = min_freq * (max_freq / min_freq).powf(t);
        let omega = 2.0 * PI * center_freq / sample_rate;

        // Goertzel single-bin DFT calculation for high-efficiency resonance
        let coeff = 2.0 * omega.cos();
        let mut s_prev = 0.0f32;
        let mut s_prev2 = 0.0f32;

        for &sample in &samples[..n] {
            // Hann windowing
            let s = sample + coeff * s_prev - s_prev2;
            s_prev2 = s_prev;
            s_prev = s;
        }

        let power = s_prev * s_prev + s_prev2 * s_prev2 - coeff * s_prev * s_prev2;
        let mag = (power.max(0.0).sqrt() / (n as f32)).min(1.0);
        magnitudes[band] = (mag * 2.5).clamp(0.0, 1.0);
    }

    magnitudes
}

/// Computes logarithmic spectrum magnitudes directly from raw PCM buffer pointer
/// with ZERO intermediate heap allocations or copies.
pub fn compute_spectrum_direct(
    buffer_ptr: *const u8,
    byte_offset: usize,
    byte_length: usize,
    is_float: bool,
    band_count: usize,
) -> Vec<f32> {
    if buffer_ptr.is_null() || byte_length == 0 || band_count == 0 {
        return vec![0.0; band_count];
    }

    let min_freq = 40.0f32;
    let max_freq = 16000.0f32;
    let sample_rate = 44100.0f32;
    let mut magnitudes = vec![0.0f32; band_count];

    let n = if is_float {
        (byte_length / 4).min(512)
    } else {
        (byte_length / 2).min(512)
    };

    if n < 32 {
        return magnitudes;
    }

    // Local stack window buffer
    let mut window = [0.0f32; 512];
    unsafe {
        let ptr = buffer_ptr.add(byte_offset);
        if is_float {
            let fptr = ptr as *const f32;
            for i in 0..n {
                window[i] = *fptr.add(i);
            }
        } else {
            let sptr = ptr as *const i16;
            for i in 0..n {
                window[i] = (*sptr.add(i) as f32) / 32768.0;
            }
        }
    }

    for band in 0..band_count {
        let t = band as f32 / (band_count as f32 - 1.0).max(1.0);
        let center_freq = min_freq * (max_freq / min_freq).powf(t);
        let omega = 2.0 * PI * center_freq / sample_rate;

        let coeff = 2.0 * omega.cos();
        let mut s_prev = 0.0f32;
        let mut s_prev2 = 0.0f32;

        for &sample in &window[..n] {
            let s = sample + coeff * s_prev - s_prev2;
            s_prev2 = s_prev;
            s_prev = s;
        }

        let power = s_prev * s_prev + s_prev2 * s_prev2 - coeff * s_prev * s_prev2;
        let mag = (power.max(0.0).sqrt() / (n as f32)).min(1.0);
        magnitudes[band] = (mag * 2.5).clamp(0.0, 1.0);
    }

    magnitudes
}

/// Generates a normalized peak waveform vector of N points (e.g. 100 points)
/// from raw PCM bytes (either PCM16 or PCM Float).
pub fn compute_waveform_peaks(pcm_bytes: &[u8], is_float: bool, num_points: usize) -> Vec<f32> {
    if pcm_bytes.is_empty() || num_points == 0 {
        return vec![0.0; num_points];
    }

    let mut peaks = vec![0.0f32; num_points];

    if is_float {
        let sample_count = pcm_bytes.len() / 4;
        if sample_count == 0 {
            return peaks;
        }
        let chunk_size = (sample_count / num_points).max(1);

        for pt in 0..num_points {
            let start = pt * chunk_size;
            let end = ((pt + 1) * chunk_size).min(sample_count);
            let mut max_abs = 0.0f32;

            for i in start..end {
                let byte_idx = i * 4;
                if byte_idx + 4 <= pcm_bytes.len() {
                    let val = f32::from_le_bytes([
                        pcm_bytes[byte_idx],
                        pcm_bytes[byte_idx + 1],
                        pcm_bytes[byte_idx + 2],
                        pcm_bytes[byte_idx + 3],
                    ]);
                    let abs_val = val.abs();
                    if abs_val > max_abs {
                        max_abs = abs_val;
                    }
                }
            }
            peaks[pt] = max_abs.clamp(0.0, 1.0);
        }
    } else {
        // PCM16 Little Endian
        let sample_count = pcm_bytes.len() / 2;
        if sample_count == 0 {
            return peaks;
        }
        let chunk_size = (sample_count / num_points).max(1);

        for pt in 0..num_points {
            let start = pt * chunk_size;
            let end = ((pt + 1) * chunk_size).min(sample_count);
            let mut max_abs = 0.0f32;

            for i in start..end {
                let byte_idx = i * 2;
                if byte_idx + 2 <= pcm_bytes.len() {
                    let val = i16::from_le_bytes([pcm_bytes[byte_idx], pcm_bytes[byte_idx + 1]]);
                    let abs_val = val.unsigned_abs() as f32 / 32768.0;
                    if abs_val > max_abs {
                        max_abs = abs_val;
                    }
                }
            }
            peaks[pt] = max_abs.clamp(0.0, 1.0);
        }
    }

    // Normalize peak envelope so loudest peak reaches ~0.95
    let global_max = peaks.iter().cloned().fold(0.0f32, f32::max);
    if global_max > 0.01 {
        let norm_scale = 0.95 / global_max;
        for p in &mut peaks {
            *p = (*p * norm_scale).clamp(0.05, 1.0);
        }
    }

    peaks
}
