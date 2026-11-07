use crate::biquad::Biquad;

pub struct ClarityEnhancer {
    pub strength: i32,
    sample_rate_hz: f32,
    de_harsh_filters: [Biquad; 2],
    air_filters: [Biquad; 2],
    applied_strength: i32,
}

impl ClarityEnhancer {
    pub fn new(sample_rate_hz: f32) -> Self {
        let mut ce = Self {
            strength: 0,
            sample_rate_hz,
            de_harsh_filters: [Biquad::new(), Biquad::new()],
            air_filters: [Biquad::new(), Biquad::new()],
            applied_strength: -1,
        };
        ce.recompute_filters();
        ce
    }

    #[allow(dead_code)]
    pub fn configure(&mut self, sample_rate_hz: f32) {
        if sample_rate_hz > 0.0 {
            self.sample_rate_hz = sample_rate_hz;
            self.applied_strength = -1;
            self.recompute_filters();
        }
    }

    pub fn reset(&mut self) {
        for f in &mut self.de_harsh_filters { f.reset(); }
        for f in &mut self.air_filters { f.reset(); }
    }

    fn recompute_filters(&mut self) {
        if self.strength == self.applied_strength { return; }
        self.applied_strength = self.strength;

        let fraction = (self.strength as f32 / 100.0).clamp(0.0, 1.0);

        if self.strength <= 0 {
            for ch in 0..2 {
                self.de_harsh_filters[ch].configure_gain(0.0);
                self.air_filters[ch].configure_gain(0.0);
            }
            return;
        }

        let de_harsh_gain_db = -3.2 * fraction;
        let air_gain_db = 2.4 * fraction;

        for ch in 0..2 {
            self.de_harsh_filters[ch].configure_peaking(
                self.sample_rate_hz,
                6200.0,
                de_harsh_gain_db,
                1.8,
            );
            self.air_filters[ch].configure_peaking(
                self.sample_rate_hz,
                12000.0,
                air_gain_db,
                1.2,
            );
        }
    }

    #[inline(always)]
    pub fn process(&mut self, sample: f32, channel: usize) -> f32 {
        if self.strength <= 0 { return sample; }
        if self.strength != self.applied_strength {
            self.recompute_filters();
        }

        let ch = if channel <= 1 { channel } else { 0 };

        // 1. Tame piercing sibilance
        let mut out = self.de_harsh_filters[ch].process(sample);

        // 2. Air presence micro-detail
        out = self.air_filters[ch].process(out);

        // 3. Gentle analog saturation
        let abs_val = out.abs();
        if abs_val > 0.82 {
            let delta = abs_val - 0.82;
            let compressed = 0.82 + delta / (1.0 + delta * 1.5);
            out = if out > 0.0 { compressed } else { -compressed };
        }

        out
    }
}
