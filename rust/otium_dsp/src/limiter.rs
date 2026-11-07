pub struct PeakLimiter {
    pub enabled: bool,
    attack_coeff: f32,
    release_coeff: f32,
    channel_gain: [f32; 2],
}

impl PeakLimiter {
    pub fn new(sample_rate_hz: f32) -> Self {
        let mut pl = Self {
            enabled: true,
            attack_coeff: 0.0,
            release_coeff: 0.0,
            channel_gain: [1.0, 1.0],
        };
        pl.configure(sample_rate_hz);
        pl
    }

    pub fn configure(&mut self, sample_rate_hz: f32) {
        if sample_rate_hz > 0.0 {
            self.attack_coeff = (-1.0 / (0.002 * sample_rate_hz)).exp();
            self.release_coeff = (-1.0 / (0.100 * sample_rate_hz)).exp();
        }
    }

    pub fn reset(&mut self) {
        self.channel_gain = [1.0, 1.0];
    }

    #[inline(always)]
    pub fn process(&mut self, sample: f32, channel: usize) -> f32 {
        if !self.enabled {
            self.channel_gain[channel] = 1.0;
            return sample;
        }

        let abs_sample = sample.abs();
        let target_gain = if abs_sample > 0.98 { 0.98 / abs_sample } else { 1.0 };

        let current_gain = self.channel_gain[channel];
        let coeff = if target_gain < current_gain { self.attack_coeff } else { self.release_coeff };
        let smoothed_gain = target_gain + coeff * (current_gain - target_gain);
        self.channel_gain[channel] = smoothed_gain;

        (sample * smoothed_gain).clamp(-1.0, 1.0)
    }
}
