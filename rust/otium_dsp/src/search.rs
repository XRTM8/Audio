use std::sync::RwLock;

pub fn normalize_arabic(input: &str) -> String {
    let mut out = String::with_capacity(input.len());
    for c in input.chars() {
        match c {
            // Remove Tashkeel / Harakat and Tatweel
            '\u{064B}'..='\u{065F}' | '\u{0670}' | '\u{0640}' => continue,
            // Normalize Alif forms (آ, أ, إ, ٱ -> ا)
            '\u{0622}' | '\u{0623}' | '\u{0625}' | '\u{0671}' => out.push('ا'),
            // Normalize Ta Marbuta (ة -> ه)
            'ة' => out.push('ه'),
            // Normalize Alif Maqsura (ى -> ي)
            'ى' => out.push('ي'),
            // Normalize Persian/Urdu forms (ک -> ك, ی -> ي)
            '\u{06A9}' => out.push('ك'),
            '\u{06CC}' => out.push('ي'),
            // Punctuation and delimiters become spaces
            '-' | '_' | '|' | '.' | '/' | '\\' | ',' | ':' | ';' | '(' | ')' | '[' | ']' | '{' | '}' => out.push(' '),
            _ => {
                for lower in c.to_lowercase() {
                    out.push(lower);
                }
            }
        }
    }
    // Collapse multi-spaces
    out.split_whitespace().collect::<Vec<&str>>().join(" ")
}

pub struct SearchItem {
    pub id: i64,
    pub norm_title: String,
    pub norm_artist: String,
    pub norm_album: String,
    pub title_tokens: Vec<String>,
    pub artist_tokens: Vec<String>,
}

impl SearchItem {
    pub fn new(id: i64, title: &str, artist: &str, album: &str) -> Self {
        let norm_title = normalize_arabic(title);
        let norm_artist = normalize_arabic(artist);
        let norm_album = normalize_arabic(album);

        let title_tokens = norm_title.split_whitespace().map(|s| s.to_string()).collect();
        let artist_tokens = norm_artist.split_whitespace().map(|s| s.to_string()).collect();

        Self {
            id,
            norm_title,
            norm_artist,
            norm_album,
            title_tokens,
            artist_tokens,
        }
    }
}

pub struct SearchEngine {
    items: Vec<SearchItem>,
}

impl SearchEngine {
    pub fn new() -> Self {
        Self { items: Vec::new() }
    }

    pub fn set_items(&mut self, new_items: Vec<SearchItem>) {
        self.items = new_items;
    }

    pub fn add_or_update(&mut self, item: SearchItem) {
        if let Some(pos) = self.items.iter().position(|it| it.id == item.id) {
            self.items[pos] = item;
        } else {
            self.items.push(item);
        }
    }

    pub fn remove(&mut self, id: i64) {
        self.items.retain(|it| it.id != id);
    }

    pub fn query(&self, query_str: &str, limit: usize) -> Vec<i64> {
        let norm_q = normalize_arabic(query_str);
        if norm_q.is_empty() {
            return self.items.iter().take(limit).map(|it| it.id).collect();
        }

        let q_tokens: Vec<&str> = norm_q.split_whitespace().collect();
        let mut scored: Vec<(i64, i32)> = Vec::new();

        for item in &self.items {
            let mut total_score = 0;
            let mut matched_all = true;

            // Direct full phrase matches
            if item.norm_title.contains(&norm_q) {
                total_score += 1500;
            }
            if item.norm_artist.contains(&norm_q) {
                total_score += 800;
            }

            // Token-based matching
            for &q_tok in &q_tokens {
                let mut token_score = 0;

                // Title check
                if item.norm_title.contains(q_tok) {
                    token_score += 400;
                } else if let Some(fuzzy_score) = fuzzy_match_tokens(q_tok, &item.title_tokens) {
                    token_score += fuzzy_score;
                }

                // Artist check
                if item.norm_artist.contains(q_tok) {
                    token_score += 250;
                } else if let Some(fuzzy_score) = fuzzy_match_tokens(q_tok, &item.artist_tokens) {
                    token_score += fuzzy_score / 2;
                }

                // Album check
                if item.norm_album.contains(q_tok) {
                    token_score += 100;
                }

                if token_score == 0 {
                    matched_all = false;
                    break;
                }
                total_score += token_score;
            }

            if matched_all && total_score > 0 {
                scored.push((item.id, total_score));
            }
        }

        scored.sort_by(|a, b| b.1.cmp(&a.1));
        scored.into_iter().take(limit).map(|(id, _)| id).collect()
    }
}

fn fuzzy_match_tokens(query: &str, tokens: &[String]) -> Option<i32> {
    let q_chars: Vec<char> = query.chars().collect();
    let q_len = q_chars.len();
    if q_len < 3 || q_len > 60 {
        return None;
    }
    let max_dist = if q_len >= 5 { 2 } else { 1 };

    for tok in tokens {
        let t_chars: Vec<char> = tok.chars().collect();
        let t_len = t_chars.len();
        if t_len > 60 || t_len.abs_diff(q_len) > max_dist {
            continue;
        }

        let dist = levenshtein_bounded(&q_chars, &t_chars, max_dist);
        if dist <= max_dist {
            return Some(120 - (dist as i32 * 40));
        }
    }
    None
}

/// Zero-heap-allocation, stack-based Levenshtein distance with early-exit termination.
fn levenshtein_bounded(a: &[char], b: &[char], max_dist: usize) -> usize {
    let m = a.len();
    let n = b.len();

    if m == 0 { return n; }
    if n == 0 { return m; }
    if m.abs_diff(n) > max_dist { return max_dist + 1; }

    let mut prev = [0usize; 64];
    let mut curr = [0usize; 64];

    for j in 0..=n {
        prev[j] = j;
    }

    for i in 1..=m {
        curr[0] = i;
        let mut min_in_row = curr[0];

        for j in 1..=n {
            let cost = if a[i - 1] == b[j - 1] { 0 } else { 1 };
            curr[j] = (prev[j] + 1).min(curr[j - 1] + 1).min(prev[j - 1] + cost);
            if curr[j] < min_in_row {
                min_in_row = curr[j];
            }
        }

        // Early termination: if minimum distance in current row already exceeds max_dist, abort immediately
        if min_in_row > max_dist {
            return max_dist + 1;
        }

        prev[..=n].copy_from_slice(&curr[..=n]);
    }

    prev[n]
}

lazy_static_search_engine!();

macro_rules! lazy_static_search_engine {
    () => {
        static SEARCH_ENGINE: RwLock<Option<SearchEngine>> = RwLock::new(None);

        pub fn global_search_init(items: Vec<SearchItem>) {
            let mut engine = SEARCH_ENGINE.write().unwrap();
            let mut inst = SearchEngine::new();
            inst.set_items(items);
            *engine = Some(inst);
        }

        pub fn global_search_query(q: &str, limit: usize) -> Vec<i64> {
            let engine = SEARCH_ENGINE.read().unwrap();
            if let Some(inst) = engine.as_ref() {
                inst.query(q, limit)
            } else {
                Vec::new()
            }
        }

        pub fn global_search_add_or_update(item: SearchItem) {
            let mut engine = SEARCH_ENGINE.write().unwrap();
            if let Some(inst) = engine.as_mut() {
                inst.add_or_update(item);
            }
        }

        pub fn global_search_remove(id: i64) {
            let mut engine = SEARCH_ENGINE.write().unwrap();
            if let Some(inst) = engine.as_mut() {
                inst.remove(id);
            }
        }
    };
}
use lazy_static_search_engine;
