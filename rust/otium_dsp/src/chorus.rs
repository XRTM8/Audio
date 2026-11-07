use std::f32::consts::PI;

const TABLE_SIZE: usize = 1024;
const TABLE_MASK: usize = TABLE_SIZE - 1;
const BUFFER_SIZE: usize = 4096;
const BUFFER_MASK: usize = BUFFER_SIZE - 1;

pub struct StereoChorus {
    pub strength: i32,
    pub out_l: f32,
    pub out_r: f32,

    sample_rate_hz: f32,
    buffer_l: [f32; BUFFER_SIZE],
    buffer_r: [f32; BUFFER_SIZE],
    write_index: usize,

    lfo_phase: f32,
    lfo_rate_hz: f32,
    base_delay_samples: f32,
    depth_samples: f32,

    sine_table: [f32; TABLE_SIZE],
}

impl StereoChorus {
    pub fn new(sample_rate_hz: f32) -> Self {
        let mut table = [0.0; TABLE_SIZE];
        for i in 0..TABLE_SIZE {
            table[i] = (i as f32 * 2.0 * PI / TABLE_SIZE as f32).sin();
        }

        let mut sc = Self {
            strength: 0,
            out_l: 0.0,
            out_r: 0.0,
            sample_rate_hz,
            buffer_l: [0.0; BUFFER_SIZE],
            buffer_r: [0.0; BUFFER_SIZE],
            write_index: 0,
            lfo_phase: 0.0,
            lfo_rate_hz: 1.15,
            base_delay_samples: 0.014 * sample_rate_hz,
            depth_samples: 0.0035 * sample_rate_hz,
            sine_table: table,
        };
        sc.configure(sample_rate_hz);
        sc
    }

    pub fn configure(&mut self, sample_rate_hz: f32) {
        if sample_rate_hz > 0.0 {
            self.sample_rate_hz = sample_rate_hz;
            self.base_delay_samples = 0.014 * sample_rate_hz;
            self.depth_samples = 0.0035 * sample_rate_hz;
        }
    }

    pub fn reset(&mut self) {
        self.buffer_l.fill(0.0);
        self.buffer_r.fill(0.0);
        self.write_index = 0;
        self.lfo_phase = 0.0;
        self.out_l = 0.0;
        self.out_r = 0.0;
    }

    #[inline(always)]
    pub fn process(&mut self, left: f32, right: f32) {
        if self.strength <= 0 {
            self.out_l = left;
            self.out_r = right;
            return;
        }

        let wet_mix = (self.strength as f32 / 100.0).clamp(0.0, 1.0);

        self.buffer_l[self.write_index] = left;
        self.buffer_r[self.write_index] = right;

        let phase_increment = (2.0 * PI * self.lfo_rate_hz) / self.sample_rate_hz;
        self.lfo_phase += phase_increment;
        if self.lfo_phase >= 2.0 * PI {
            self.lfo_phase -= 2.0 * PI;
        }

        let index_l = ((self.lfo_phase * (TABLE_SIZE as f32 / (2.0 * PI))) as usize) & TABLE_MASK;
        let index_r = (index_l + 256) & TABLE_MASK;

        let mod_l = self.sine_table[index_l];
        let mod_r = self.sine_table[index_r];

        let delay_l = self.base_delay_samples + self.depth_samples * mod_l;
        let delay_r = self.base_delay_samples + self.depth_samples * mod_r;

        let chorus_l = self.read_interpolated(&self.buffer_l, delay_l);
        let chorus_r = self.read_interpolated(&self.buffer_r, delay_r);

        self.write_index = (self.write_index + 1) & BUFFER_MASK;

        let dry_mix = 1.0 - (wet_mix * 0.25);
        let wet = wet_mix * 0.65;

        self.out_l = left * dry_mix + chorus_l * wet;
        self.out_r = right * dry_mix + chorus_r * wet;
    }

    #[inline(always)]
    fn read_interpolated(&self, buffer: &[f32; BUFFER_SIZE], delay: f32) -> f32 {
        let mut read_pos = self.write_index as f32 - delay;
        while read_pos < 0.0 { read_pos += BUFFER_SIZE as f32; }
        while read_pos >= BUFFER_SIZE as f32 { read_pos -= BUFFER_SIZE as f32; }

        let index0 = read_pos as usize;
        let frac = read_pos - index0 as f32;
        let index1 = (index0 + 1) & BUFFER_MASK;

        let s0 = buffer[index0];
        let s1 = buffer[index1];
        s0 + frac * (s1 - s0)
    }
}
