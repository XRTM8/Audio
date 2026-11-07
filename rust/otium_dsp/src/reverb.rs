const COMB_DELAYS_L: [usize; 4] = [1116, 1188, 1277, 1356];
const COMB_DELAYS_R: [usize; 4] = [1138, 1211, 1297, 1378];

pub struct AudiophileReverb {
    pub strength: i32,
    pub preset: i32, // 0: Studio, 1: Room, 2: Hall, 3: Plate

    pub out_l: f32,
    pub out_r: f32,

    comb_buffers_l: [[f32; 2400]; 4],
    comb_buffers_r: [[f32; 2400]; 4],
    comb_indices_l: [usize; 4],
    comb_indices_r: [usize; 4],
    comb_filter_l: [f32; 4],
    comb_filter_r: [f32; 4],

    allpass_l1: [f32; 556],
    allpass_l2: [f32; 441],
    allpass_r1: [f32; 567],
    allpass_r2: [f32; 452],
    allpass_idx_l1: usize,
    allpass_idx_l2: usize,
    allpass_idx_r1: usize,
    allpass_idx_r2: usize,

    feedback: f32,
    damping: f32,
}

impl AudiophileReverb {
    pub fn new() -> Self {
        let mut r = Self {
            strength: 0,
            preset: 0,
            out_l: 0.0,
            out_r: 0.0,
            comb_buffers_l: [[0.0; 2400]; 4],
            comb_buffers_r: [[0.0; 2400]; 4],
            comb_indices_l: [0; 4],
            comb_indices_r: [0; 4],
            comb_filter_l: [0.0; 4],
            comb_filter_r: [0.0; 4],
            allpass_l1: [0.0; 556],
            allpass_l2: [0.0; 441],
            allpass_r1: [0.0; 567],
            allpass_r2: [0.0; 452],
            allpass_idx_l1: 0,
            allpass_idx_l2: 0,
            allpass_idx_r1: 0,
            allpass_idx_r2: 0,
            feedback: 0.52,
            damping: 0.42,
        };
        r.update_preset();
        r
    }

    pub fn update_preset(&mut self) {
        match self.preset {
            0 => { // Studio
                self.feedback = 0.52;
                self.damping = 0.42;
            }
            1 => { // Room
                self.feedback = 0.68;
                self.damping = 0.28;
            }
            2 => { // Hall
                self.feedback = 0.82;
                self.damping = 0.20;
            }
            3 => { // Plate
                self.feedback = 0.78;
                self.damping = 0.12;
            }
            _ => {
                self.feedback = 0.60;
                self.damping = 0.32;
            }
        }
    }

    pub fn reset(&mut self) {
        for b in &mut self.comb_buffers_l { b.fill(0.0); }
        for b in &mut self.comb_buffers_r { b.fill(0.0); }
        self.comb_indices_l.fill(0);
        self.comb_indices_r.fill(0);
        self.comb_filter_l.fill(0.0);
        self.comb_filter_r.fill(0.0);
        self.allpass_l1.fill(0.0);
        self.allpass_l2.fill(0.0);
        self.allpass_r1.fill(0.0);
        self.allpass_r2.fill(0.0);
        self.allpass_idx_l1 = 0;
        self.allpass_idx_l2 = 0;
        self.allpass_idx_r1 = 0;
        self.allpass_idx_r2 = 0;
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
        let mono_in = (left + right) * 0.5;

        // 1. Parallel Comb Filters (Left)
        let mut sum_l = 0.0_f32;
        for i in 0..4 {
            let max_len = COMB_DELAYS_L[i];
            let idx = self.comb_indices_l[i];
            let out = self.comb_buffers_l[i][idx];
            self.comb_filter_l[i] = out * (1.0 - self.damping) + self.comb_filter_l[i] * self.damping;
            self.comb_buffers_l[i][idx] = mono_in + self.comb_filter_l[i] * self.feedback;
            self.comb_indices_l[i] = if idx + 1 >= max_len { 0 } else { idx + 1 };
            sum_l += out;
        }

        // 2. Parallel Comb Filters (Right)
        let mut sum_r = 0.0_f32;
        for i in 0..4 {
            let max_len = COMB_DELAYS_R[i];
            let idx = self.comb_indices_r[i];
            let out = self.comb_buffers_r[i][idx];
            self.comb_filter_r[i] = out * (1.0 - self.damping) + self.comb_filter_r[i] * self.damping;
            self.comb_buffers_r[i][idx] = mono_in + self.comb_filter_r[i] * self.feedback;
            self.comb_indices_r[i] = if idx + 1 >= max_len { 0 } else { idx + 1 };
            sum_r += out;
        }

        sum_l *= 0.25;
        sum_r *= 0.25;

        // 3. Cascaded All-Pass Diffusers (Left)
        let diff_l1 = Self::process_allpass(sum_l, &mut self.allpass_l1, &mut self.allpass_idx_l1);
        let diff_l2 = Self::process_allpass(diff_l1, &mut self.allpass_l2, &mut self.allpass_idx_l2);

        // 4. Cascaded All-Pass Diffusers (Right)
        let diff_r1 = Self::process_allpass(sum_r, &mut self.allpass_r1, &mut self.allpass_idx_r1);
        let diff_r2 = Self::process_allpass(diff_r1, &mut self.allpass_r2, &mut self.allpass_idx_r2);

        let dry_amount = 1.0 - (wet_mix * 0.22);
        let wet_amount = wet_mix * 0.38;

        self.out_l = left * dry_amount + diff_l2 * wet_amount;
        self.out_r = right * dry_amount + diff_r2 * wet_amount;
    }

    #[inline(always)]
    fn process_allpass(input: f32, buffer: &mut [f32], index: &mut usize) -> f32 {
        let buf_out = buffer[*index];
        let output = -input + buf_out;
        buffer[*index] = input + buf_out * 0.5;
        *index = if *index + 1 >= buffer.len() { 0 } else { *index + 1 };
        output
    }
}
