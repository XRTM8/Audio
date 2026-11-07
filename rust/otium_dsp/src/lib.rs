mod biquad;
mod chorus;
mod clarity;
mod engine;
mod limiter;
mod reverb;
pub mod scanner;
pub mod search;
pub mod waveform;
pub mod lyrics;

use engine::OtiumDspEngine;
use jni::objects::{JByteArray, JByteBuffer, JClass, JFloatArray, JLongArray, JObjectArray, JString};
use jni::sys::{jboolean, jfloat, jint, jlong};
use jni::JNIEnv;

// ============================================================================
// 1. Real-time Audio DSP Engine (Equalizer, Reverb, Chorus, Clarity, Limiter)
// ============================================================================

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_createEngine(
    _env: JNIEnv,
    _class: JClass,
    sample_rate: jint,
) -> jlong {
    let engine = Box::new(OtiumDspEngine::new(sample_rate as f32));
    Box::into_raw(engine) as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_destroyEngine(
    _env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) {
    if engine_ptr != 0 {
        unsafe {
            let _ = Box::from_raw(engine_ptr as *mut OtiumDspEngine);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_resetEngine(
    _env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) {
    if engine_ptr != 0 {
        let engine = unsafe { &mut *(engine_ptr as *mut OtiumDspEngine) };
        engine.reset();
    }
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_setParameters(
    env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    pre_amp_db: jfloat,
    band_gains_db: JFloatArray,
    reverb_strength: jint,
    reverb_preset: jint,
    chorus_strength: jint,
    clarity_strength: jint,
    is_enabled: jboolean,
) {
    if engine_ptr == 0 { return; }
    let engine = unsafe { &mut *(engine_ptr as *mut OtiumDspEngine) };

    let mut gains = [0.0_f32; 10];
    if !band_gains_db.is_null() {
        let _ = env.get_float_array_region(&band_gains_db, 0, &mut gains);
    }

    engine.set_parameters(
        pre_amp_db,
        &gains,
        reverb_strength,
        reverb_preset,
        chorus_strength,
        clarity_strength,
        is_enabled != 0,
    );
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_processDirectBuffer(
    env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    buffer_obj: JByteBuffer,
    byte_offset: jint,
    byte_length: jint,
    is_float: jboolean,
) {
    if engine_ptr == 0 || buffer_obj.is_null() || byte_length <= 0 { return; }
    let engine = unsafe { &mut *(engine_ptr as *mut OtiumDspEngine) };

    let buffer_address = match env.get_direct_buffer_address(&buffer_obj) {
        Ok(addr) => addr,
        Err(_) => return,
    };

    if buffer_address.is_null() { return; }

    unsafe {
        let ptr = buffer_address.add(byte_offset.max(0) as usize);
        if is_float != 0 {
            let float_samples = (byte_length as usize) / 4;
            let slice = std::slice::from_raw_parts_mut(ptr as *mut f32, float_samples);
            engine.process_interleaved_float(slice);
        } else {
            let pcm16_samples = (byte_length as usize) / 2;
            let slice = std::slice::from_raw_parts_mut(ptr as *mut i16, pcm16_samples);
            engine.process_interleaved_pcm16(slice);
        }
    }
}

// ============================================================================
// 2. High-Speed Audio Metadata & Tag Scanner
// ============================================================================

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_scanAudioFile(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jni::sys::jstring {
    let path_str: String = match env.get_string(&path) {
        Ok(js) => js.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    if let Some(meta) = scanner::scan_file(&path_str) {
        let json = format!(
            "{{\"title\":{},\"artist\":{},\"album\":{},\"durationMs\":{},\"bitrate\":{},\"sampleRate\":{},\"format\":{},\"hasArtwork\":{}}}",
            escape_json(&meta.title),
            escape_json(&meta.artist),
            escape_json(&meta.album),
            meta.duration_ms,
            meta.bitrate,
            meta.sample_rate,
            escape_json(&meta.format),
            meta.has_artwork
        );
        match env.new_string(json) {
            Ok(js) => js.into_raw(),
            Err(_) => std::ptr::null_mut(),
        }
    } else {
        std::ptr::null_mut()
    }
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_extractArtwork(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jni::sys::jbyteArray {
    let path_str: String = match env.get_string(&path) {
        Ok(js) => js.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    if let Some(art_bytes) = scanner::extract_artwork(&path_str) {
        match env.new_byte_array(art_bytes.len() as i32) {
            Ok(byte_arr) => {
                let slice: &[i8] = unsafe {
                    std::slice::from_raw_parts(art_bytes.as_ptr() as *const i8, art_bytes.len())
                };
                let _ = env.set_byte_array_region(&byte_arr, 0, slice);
                byte_arr.into_raw()
            }
            Err(_) => std::ptr::null_mut(),
        }
    } else {
        std::ptr::null_mut()
    }
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_writeAudioTags(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    title: JString,
    artist: JString,
    album: JString,
) -> jboolean {
    let path_str: String = match env.get_string(&path) { Ok(s) => s.into(), Err(_) => return 0 };
    let title_str: String = match env.get_string(&title) { Ok(s) => s.into(), Err(_) => String::new() };
    let artist_str: String = match env.get_string(&artist) { Ok(s) => s.into(), Err(_) => String::new() };
    let album_str: String = match env.get_string(&album) { Ok(s) => s.into(), Err(_) => String::new() };

    if scanner::write_tags(&path_str, &title_str, &artist_str, &album_str) { 1 } else { 0 }
}

fn escape_json(s: &str) -> String {
    let mut out = String::with_capacity(s.len() + 2);
    out.push('"');
    for c in s.chars() {
        match c {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            _ => out.push(c),
        }
    }
    out.push('"');
    out
}

// ============================================================================
// 3. Intelligent Arabic In-Memory Fuzzy Search Engine
// ============================================================================

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_searchIndexInit(
    mut env: JNIEnv,
    _class: JClass,
    ids: JLongArray,
    titles: JObjectArray,
    artists: JObjectArray,
    albums: JObjectArray,
) {
    let count = match env.get_array_length(&ids) {
        Ok(c) => c as usize,
        Err(_) => return,
    };

    let mut id_buf = vec![0i64; count];
    if env.get_long_array_region(&ids, 0, &mut id_buf).is_err() {
        return;
    }

    let mut items = Vec::with_capacity(count);

    for i in 0..count {
        let id = id_buf[i];

        let title = match env.get_object_array_element(&titles, i as i32) {
            Ok(o) if !o.is_null() => {
                let js = JString::from(o);
                env.get_string(&js).map(|s| s.into()).unwrap_or_default()
            }
            _ => String::new(),
        };

        let artist = match env.get_object_array_element(&artists, i as i32) {
            Ok(o) if !o.is_null() => {
                let js = JString::from(o);
                env.get_string(&js).map(|s| s.into()).unwrap_or_default()
            }
            _ => String::new(),
        };

        let album = match env.get_object_array_element(&albums, i as i32) {
            Ok(o) if !o.is_null() => {
                let js = JString::from(o);
                env.get_string(&js).map(|s| s.into()).unwrap_or_default()
            }
            _ => String::new(),
        };

        items.push(search::SearchItem::new(id, &title, &artist, &album));
    }

    search::global_search_init(items);
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_searchIndexQuery(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
    limit: jint,
) -> jni::sys::jlongArray {
    let empty_array = |env: &mut JNIEnv| {
        env.new_long_array(0).map(|a| a.into_raw()).unwrap_or(std::ptr::null_mut())
    };

    let q_str: String = match env.get_string(&query) {
        Ok(s) => s.into(),
        Err(_) => return empty_array(&mut env),
    };

    let results = search::global_search_query(&q_str, limit.max(1) as usize);

    match env.new_long_array(results.len() as i32) {
        Ok(arr) => {
            let _ = env.set_long_array_region(&arr, 0, &results);
            arr.into_raw()
        }
        Err(_) => empty_array(&mut env),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_searchIndexAddOrUpdate(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    title: JString,
    artist: JString,
    album: JString,
) {
    let title_str: String = match env.get_string(&title) { Ok(s) => s.into(), Err(_) => String::new() };
    let artist_str: String = match env.get_string(&artist) { Ok(s) => s.into(), Err(_) => String::new() };
    let album_str: String = match env.get_string(&album) { Ok(s) => s.into(), Err(_) => String::new() };

    let item = search::SearchItem::new(id, &title_str, &artist_str, &album_str);
    search::global_search_add_or_update(item);
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_searchIndexRemove(
    _env: JNIEnv,
    _class: JClass,
    id: jlong,
) {
    search::global_search_remove(id);
}

// ============================================================================
// 4. Waveform & FFT Spectrum Analyzer
// ============================================================================

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_computeSpectrum(
    env: JNIEnv,
    _class: JClass,
    samples: JFloatArray,
    band_count: jint,
) -> jni::sys::jfloatArray {
    let len = match env.get_array_length(&samples) {
        Ok(l) => l as usize,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut buf = vec![0.0f32; len];
    if env.get_float_array_region(&samples, 0, &mut buf).is_err() {
        return std::ptr::null_mut();
    }

    let bands = waveform::compute_spectrum(&buf, band_count.max(1) as usize);

    match env.new_float_array(bands.len() as i32) {
        Ok(arr) => {
            let _ = env.set_float_array_region(&arr, 0, &bands);
            arr.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_computeWaveformPeaks(
    env: JNIEnv,
    _class: JClass,
    pcm_data: JByteArray,
    is_float: jboolean,
    points: jint,
) -> jni::sys::jfloatArray {
    let len = match env.get_array_length(&pcm_data) {
        Ok(l) => l as usize,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut byte_buf = vec![0i8; len];
    if env.get_byte_array_region(&pcm_data, 0, &mut byte_buf).is_err() {
        return std::ptr::null_mut();
    }

    let u8_slice: &[u8] = unsafe {
        std::slice::from_raw_parts(byte_buf.as_ptr() as *const u8, len)
    };

    let peaks = waveform::compute_waveform_peaks(u8_slice, is_float != 0, points.max(10) as usize);

    match env.new_float_array(peaks.len() as i32) {
        Ok(arr) => {
            let _ = env.set_float_array_region(&arr, 0, &peaks);
            arr.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}

// ============================================================================
// 5. High-Speed LRC Parser & Synchronizer
// ============================================================================

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_parseLrc(
    mut env: JNIEnv,
    _class: JClass,
    lrc_text: JString,
) -> jni::sys::jstring {
    let text_str: String = match env.get_string(&lrc_text) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    let (timestamps, lines) = lyrics::parse_lrc(&text_str);

    let mut out = String::with_capacity(text_str.len() + 100);
    for i in 0..timestamps.len() {
        out.push_str(&timestamps[i].to_string());
        out.push('\t');
        out.push_str(&lines[i]);
        out.push('\n');
    }

    match env.new_string(out) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_otium_audio_audio_dsp_NativeAudioDsp_findActiveLrcLine(
    env: JNIEnv,
    _class: JClass,
    timestamps: JLongArray,
    position_ms: jlong,
) -> jint {
    let len = match env.get_array_length(&timestamps) {
        Ok(l) => l as usize,
        Err(_) => return -1,
    };

    let mut ts_buf = vec![0i64; len];
    if env.get_long_array_region(&timestamps, 0, &mut ts_buf).is_err() {
        return -1;
    }

    lyrics::find_active_line(&ts_buf, position_ms)
}
