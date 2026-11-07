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
    if query.chars().count() < 3 {
        return None;
    }
    let max_dist = if query.chars().count() >= 5 { 2 } else { 1 };

    for tok in tokens {
        let dist = levenshtein(query, tok);
        if dist <= max_dist {
            return Some(120 - (dist as i32 * 40));
        }
    }
    None
}

fn levenshtein(a: &str, b: &str) -> usize {
    let a_chars: Vec<char> = a.chars().collect();
    let b_chars: Vec<char> = b.chars().collect();

    let m = a_chars.len();
    let n = b_chars.len();

    if m == 0 { return n; }
    if n == 0 { return m; }

    let mut dp = vec![vec![0usize; n + 1]; m + 1];

    for i in 0..=m { dp[i][0] = i; }
    for j in 0..=n { dp[0][j] = j; }

    for i in 1..=m {
        for j in 1..=n {
            let cost = if a_chars[i - 1] == b_chars[j - 1] { 0 } else { 1 };
            dp[i][j] = (dp[i - 1][j] + 1)
                .min(dp[i][j - 1] + 1)
                .min(dp[i - 1][j - 1] + cost);
        }
    }

    dp[m][n]
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
