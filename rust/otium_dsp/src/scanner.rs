use std::path::Path;
use lofty::probe::Probe;
use lofty::tag::{Accessor, TagExt};
use lofty::file::{AudioFile, TaggedFileExt};
use lofty::config::WriteOptions;

pub struct ScannedAudio {
    pub title: String,
    pub artist: String,
    pub album: String,
    pub duration_ms: i64,
    pub bitrate: u32,
    pub sample_rate: u32,
    pub format: String,
    pub has_artwork: bool,
}

pub fn scan_file(file_path: &str) -> Option<ScannedAudio> {
    let path = Path::new(file_path);
    if !path.exists() {
        return None;
    }

    let tagged_file = Probe::open(path).ok()?.read().ok()?;
    let properties = tagged_file.properties();

    let tag = tagged_file.primary_tag()
        .or_else(|| tagged_file.first_tag());

    let file_stem = path.file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Unknown Title")
        .to_string();

    let title = tag.and_then(|t| t.title().map(|s| s.to_string()))
        .unwrap_or(file_stem);

    let artist = tag.and_then(|t| t.artist().map(|s| s.to_string()))
        .unwrap_or_else(|| "Unknown Artist".to_string());

    let album = tag.and_then(|t| t.album().map(|s| s.to_string()))
        .unwrap_or_else(|| "Unknown Album".to_string());

    let has_artwork = tag.map(|t| !t.pictures().is_empty()).unwrap_or(false);

    let format_str = tagged_file.file_type();
    let format = format!("{:?}", format_str).to_uppercase();

    Some(ScannedAudio {
        title,
        artist,
        album,
        duration_ms: properties.duration().as_millis() as i64,
        bitrate: properties.audio_bitrate().unwrap_or(0),
        sample_rate: properties.sample_rate().unwrap_or(44100),
        format,
        has_artwork,
    })
}

pub fn extract_artwork(file_path: &str) -> Option<Vec<u8>> {
    let path = Path::new(file_path);
    let tagged_file = Probe::open(path).ok()?.read().ok()?;
    let tag = tagged_file.primary_tag().or_else(|| tagged_file.first_tag())?;
    let pic = tag.pictures().first()?;
    Some(pic.data().to_vec())
}

pub fn extract_lyrics(file_path: &str) -> Option<String> {
    let path = Path::new(file_path);
    let tagged_file = Probe::open(path).ok()?.read().ok()?;
    let tag = tagged_file.primary_tag().or_else(|| tagged_file.first_tag())?;
    tag.get_string(&lofty::tag::ItemKey::Lyrics).map(|s| s.to_string())
}

pub fn write_tags(file_path: &str, title: &str, artist: &str, album: &str) -> bool {
    let path = Path::new(file_path);
    if let Ok(mut tagged_file) = Probe::open(path).and_then(|p| p.read()) {
        let tag = match tagged_file.primary_tag_mut() {
            Some(primary_tag) => primary_tag,
            None => {
                if let Some(first_tag) = tagged_file.first_tag_mut() {
                    first_tag
                } else {
                    let tag_type = tagged_file.primary_tag_type();
                    tagged_file.insert_tag(lofty::tag::Tag::new(tag_type));
                    tagged_file.primary_tag_mut().unwrap()
                }
            }
        };

        if !title.is_empty() {
            tag.set_title(title.to_string());
        }
        if !artist.is_empty() {
            tag.set_artist(artist.to_string());
        }
        if !album.is_empty() {
            tag.set_album(album.to_string());
        }

        tag.save_to_path(path, WriteOptions::default()).is_ok()
    } else {
        false
    }
}
