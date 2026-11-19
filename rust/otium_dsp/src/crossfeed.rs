/// Natural Acoustic Crossfeed (Bauer / Chu Moy / Meier headphone staging).
/// Eliminates extreme stereo separation fatigue on headphones by simulating
/// natural acoustic head-shadow and interaural time difference (ITD).
#[derive(Clone, Copy)]
pub struct Crossfeed {
    sample_rate: f32,
    enabled: bool,
    // Delay lines (128 samples stack buffer, plenty for up to 192 kHz at 500 µs)
    delay_l: [f32; 128],
    delay_r: [f32; 128],
    delay_idx: usize,
    delay_samples: usize,
    // Single-pole IIR low-pass filter state for crossfeed signal (~700 Hz cutoff)
    lpf_l: f32,
    lpf_r: f32,
    lpf_coeff: f32,
    // Mixing gains
    direct_gain: f32,
    cross_gain: f32,
}

impl Crossfeed {
    pub fn new(sample_rate: f32) -> Self {
        let mut cf = Self {
            sample_rate,
            enabled: false,
            delay_l: [0.0; 128],
            delay_r: [0.0; 128],
            delay_idx: 0,
            delay_samples: 14,
            lpf_l: 0.0,
            lpf_r: 0.0,
            lpf_coeff: 0.1,
            direct_gain: 0.76,
            cross_gain: 0.24,
        };
        cf.configure(sample_rate);
        cf
    }

    pub fn configure(&mut self, sample_rate: f32) {
        self.sample_rate = sample_rate.max(8000.0);
        // Interaural delay is ~280 microseconds (approx 12-14 samples @ 44.1/48kHz)
        let delay_sec = 0.000280f32;
        self.delay_samples = ((delay_sec * self.sample_rate).round() as usize).clamp(1, 120);

        // Low-pass filter at ~700 Hz for head-shadow simulation
        // Single-pole IIR filter: y[n] = (1 - alpha) * y[n-1] + alpha * x[n]
        // alpha = 1 - exp(-2 * pi * fc / fs)
        let fc = 700.0f32;
        let alpha = 1.0 - (-2.0 * std::f32::consts::PI * fc / self.sample_rate).exp();
        self.lpf_coeff = alpha.clamp(0.01, 0.99);

        // Chu Moy standard crossfeed mix: ~6dB attenuation on cross-feed
        self.direct_gain = 0.76;
        self.cross_gain = 0.24;
    }

    pub fn set_enabled(&mut self, enabled: bool) {
        self.enabled = enabled;
    }

    pub fn is_enabled(&self) -> bool {
        self.enabled
    }

    pub fn reset(&mut self) {
        self.delay_l.fill(0.0);
        self.delay_r.fill(0.0);
        self.delay_idx = 0;
        self.lpf_l = 0.0;
        self.lpf_r = 0.0;
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if !self.enabled {
            return (in_l, in_r);
        }

        // 1. Low-pass filter for head shadow
        self.lpf_l += self.lpf_coeff * (in_l - self.lpf_l);
        self.lpf_r += self.lpf_coeff * (in_r - self.lpf_r);

        // 2. Write filtered signal into delay line
        self.delay_l[self.delay_idx] = self.lpf_l;
        self.delay_r[self.delay_idx] = self.lpf_r;

        // 3. Read delayed cross-channel signals
        let read_idx = if self.delay_idx >= self.delay_samples {
            self.delay_idx - self.delay_samples
        } else {
            128 + self.delay_idx - self.delay_samples
        };

        let delayed_cross_l = self.delay_l[read_idx];
        let delayed_cross_r = self.delay_r[read_idx];

        // Advance delay line pointer
        self.delay_idx = (self.delay_idx + 1) & 127;

        // 4. Natural acoustic blend:
        // Left ear hears direct Left + delayed & shadow-filtered Right
        // Right ear hears direct Right + delayed & shadow-filtered Left
        let out_l = in_l * self.direct_gain + delayed_cross_r * self.cross_gain;
        let out_r = in_r * self.direct_gain + delayed_cross_l * self.cross_gain;

        (out_l, out_r)
    }
}
