use crate::biquad::Biquad;
use crate::chorus::StereoChorus;
use crate::clarity::ClarityEnhancer;
use crate::limiter::PeakLimiter;
use crate::reverb::AudiophileReverb;

pub const BAND_FREQUENCIES_HZ: [f32; 10] = [
    31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
];

pub struct OtiumDspEngine {
    sample_rate_hz: f32,
    pre_amp: Biquad,
    filters: [[Biquad; 2]; 10],
    clarity: ClarityEnhancer,
    chorus: StereoChorus,
    reverb: AudiophileReverb,
    limiter: PeakLimiter,

    pub is_enabled: bool,
    pub is_bypass: bool,
    applied_pre_amp_db: f32,
    applied_gains: [f32; 10],
}

impl OtiumDspEngine {
    pub fn new(sample_rate_hz: f32) -> Self {
        let mut filters = [[Biquad::new(); 2]; 10];
        for (i, &centre_hz) in BAND_FREQUENCIES_HZ.iter().enumerate() {
            filters[i][0].configure_peaking(sample_rate_hz, centre_hz, 0.0, 1.4);
            filters[i][1].configure_peaking(sample_rate_hz, centre_hz, 0.0, 1.4);
        }

        Self {
            sample_rate_hz,
            pre_amp: Biquad::new(),
            filters,
            clarity: ClarityEnhancer::new(sample_rate_hz),
            chorus: StereoChorus::new(sample_rate_hz),
            reverb: AudiophileReverb::new(),
            limiter: PeakLimiter::new(sample_rate_hz),
            is_enabled: true,
            is_bypass: false,
            applied_pre_amp_db: 0.0,
            applied_gains: [0.0; 10],
        }
    }

    pub fn set_parameters(
        &mut self,
        pre_amp_db: f32,
        band_gains_db: &[f32],
        reverb_strength: i32,
        reverb_preset: i32,
        chorus_strength: i32,
        clarity_strength: i32,
        is_enabled: bool,
    ) {
        self.is_enabled = is_enabled;

        if pre_amp_db != self.applied_pre_amp_db {
            self.applied_pre_amp_db = pre_amp_db;
            self.pre_amp.configure_gain(pre_amp_db);
        }

        let mut has_boost = pre_amp_db > 0.05;
        let mut has_cut = pre_amp_db < -0.05;

        for (i, &gain) in band_gains_db.iter().enumerate().take(10) {
            let actual_gain = if is_enabled { gain } else { 0.0 };
            if actual_gain > 0.05 { has_boost = true; }
            if actual_gain < -0.05 { has_cut = true; }

            if actual_gain != self.applied_gains[i] {
                self.applied_gains[i] = actual_gain;
                let centre_hz = BAND_FREQUENCIES_HZ[i];
                self.filters[i][0].configure_peaking(self.sample_rate_hz, centre_hz, actual_gain, 1.4);
                self.filters[i][1].configure_peaking(self.sample_rate_hz, centre_hz, actual_gain, 1.4);
            }
        }

        self.reverb.strength = if is_enabled { reverb_strength } else { 0 };
        self.reverb.preset = reverb_preset;
        self.reverb.update_preset();

        self.chorus.strength = if is_enabled { chorus_strength } else { 0 };
        self.clarity.strength = if is_enabled { clarity_strength } else { 0 };

        let has_fx = reverb_strength > 0 || chorus_strength > 0 || clarity_strength > 0;
        self.limiter.enabled = is_enabled && (has_boost || has_fx);
        self.is_bypass = !is_enabled || (!has_boost && !has_cut && !has_fx);
    }

    pub fn reset(&mut self) {
        self.pre_amp.reset();
        for band in &mut self.filters {
            band[0].reset();
            band[1].reset();
        }
        self.clarity.reset();
        self.chorus.reset();
        self.reverb.reset();
        self.limiter.reset();
    }

    pub fn process_interleaved_float(&mut self, samples: &mut [f32]) {
        if self.is_bypass { return; }

        let frames = samples.len() / 2;
        for f in 0..frames {
            let mut left = samples[f * 2];
            let mut right = samples[f * 2 + 1];

            // 1. PreAmp
            left = self.pre_amp.process(left);
            right = self.pre_amp.process(right);

            // 2. 10-Band Peaking EQ
            for band in 0..10 {
                left = self.filters[band][0].process(left);
                right = self.filters[band][1].process(right);
            }

            // 3. Clarity & De-Harsh
            if self.clarity.strength > 0 {
                left = self.clarity.process(left, 0);
                right = self.clarity.process(right, 1);
            }

            // 4. Stereo Chorus
            if self.chorus.strength > 0 {
                self.chorus.process(left, right);
                left = self.chorus.out_l;
                right = self.chorus.out_r;
            }

            // 5. Audiophile Reverb
            if self.reverb.strength > 0 {
                self.reverb.process(left, right);
                left = self.reverb.out_l;
                right = self.reverb.out_r;
            }

            // 6. Peak Limiter
            left = self.limiter.process(left, 0);
            right = self.limiter.process(right, 1);

            samples[f * 2] = left;
            samples[f * 2 + 1] = right;
        }
    }

    pub fn process_interleaved_pcm16(&mut self, samples: &mut [i16]) {
        if self.is_bypass { return; }

        let frames = samples.len() / 2;
        for f in 0..frames {
            let mut left = samples[f * 2] as f32 / 32768.0;
            let mut right = samples[f * 2 + 1] as f32 / 32768.0;

            left = self.pre_amp.process(left);
            right = self.pre_amp.process(right);

            for band in 0..10 {
                left = self.filters[band][0].process(left);
                right = self.filters[band][1].process(right);
            }

            if self.clarity.strength > 0 {
                left = self.clarity.process(left, 0);
                right = self.clarity.process(right, 1);
            }

            if self.chorus.strength > 0 {
                self.chorus.process(left, right);
                left = self.chorus.out_l;
                right = self.chorus.out_r;
            }

            if self.reverb.strength > 0 {
                self.reverb.process(left, right);
                left = self.reverb.out_l;
                right = self.reverb.out_r;
            }

            left = self.limiter.process(left, 0);
            right = self.limiter.process(right, 1);

            samples[f * 2] = (left.clamp(-1.0, 1.0) * 32767.0) as i16;
            samples[f * 2 + 1] = (right.clamp(-1.0, 1.0) * 32767.0) as i16;
        }
    }
}
