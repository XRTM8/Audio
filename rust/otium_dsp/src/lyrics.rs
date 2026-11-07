pub struct LrcEntry {
    pub timestamp_ms: i64,
    pub text: String,
}

/// Parses raw LRC lyrics string into sorted parallel arrays: (timestamps_ms, lyric_lines)
pub fn parse_lrc(lrc_text: &str) -> (Vec<i64>, Vec<String>) {
    let mut entries: Vec<LrcEntry> = Vec::new();

    for line in lrc_text.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() {
            continue;
        }

        // Extract all timestamp tags e.g. [01:23.45]
        let mut time_tags: Vec<i64> = Vec::new();
        let mut text_start = 0;

        let bytes = trimmed.as_bytes();
        let mut i = 0;
        while i < bytes.len() {
            if bytes[i] == b'[' {
                if let Some(close_idx) = trimmed[i..].find(']') {
                    let tag_content = &trimmed[i + 1..i + close_idx];
                    if let Some(ms) = parse_timestamp(tag_content) {
                        time_tags.push(ms);
                        text_start = i + close_idx + 1;
                        i += close_idx + 1;
                        continue;
                    }
                }
            }
            i += 1;
        }

        let lyric_text = trimmed[text_start..].trim().to_string();
        for ts in time_tags {
            entries.push(LrcEntry {
                timestamp_ms: ts,
                text: lyric_text.clone(),
            });
        }
    }

    entries.sort_by_key(|e| e.timestamp_ms);

    let mut timestamps = Vec::with_capacity(entries.len());
    let mut lines = Vec::with_capacity(entries.len());

    for entry in entries {
        timestamps.push(entry.timestamp_ms);
        lines.push(entry.text);
    }

    (timestamps, lines)
}

fn parse_timestamp(tag: &str) -> Option<i64> {
    // Format mm:ss.xx or mm:ss:xx or mm:ss
    let parts: Vec<&str> = tag.split(':').collect();
    if parts.len() < 2 {
        return None;
    }

    let minutes: i64 = parts[0].parse().ok()?;
    let seconds_parts: Vec<&str> = parts[1].split('.').collect();
    let seconds: i64 = seconds_parts[0].parse().ok()?;

    let millis: i64 = if seconds_parts.len() > 1 {
        let fraction_str = seconds_parts[1];
        if fraction_str.len() == 2 {
            fraction_str.parse::<i64>().ok()? * 10
        } else if fraction_str.len() == 3 {
            fraction_str.parse::<i64>().ok()?
        } else {
            fraction_str.chars().take(3).collect::<String>().parse::<i64>().ok()?
        }
    } else {
        0
    };

    Some(minutes * 60_000 + seconds * 1_000 + millis)
}

/// Binary search active line index in nanoseconds with zero memory allocation.
pub fn find_active_line(timestamps: &[i64], current_position_ms: i64) -> i32 {
    if timestamps.is_empty() {
        return -1;
    }
    if current_position_ms < timestamps[0] {
        return 0;
    }

    match timestamps.binary_search(&current_position_ms) {
        Ok(exact) => exact as i32,
        Err(insert) => (insert.saturating_sub(1)) as i32,
    }
}
