mod biquad;
mod chorus;
mod clarity;
mod engine;
mod limiter;
mod reverb;

use engine::OtiumDspEngine;
use jni::objects::{JByteBuffer, JClass, JFloatArray};
use jni::sys::{jboolean, jfloat, jint, jlong};
use jni::JNIEnv;

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
        let raw_ptr = buffer_address.offset(byte_offset as isize);
        if is_float != 0 {
            let float_count = (byte_length as usize) / std::mem::size_of::<f32>();
            let slice = std::slice::from_raw_parts_mut(raw_ptr as *mut f32, float_count);
            engine.process_interleaved_float(slice);
        } else {
            let short_count = (byte_length as usize) / std::mem::size_of::<i16>();
            let slice = std::slice::from_raw_parts_mut(raw_ptr as *mut i16, short_count);
            engine.process_interleaved_pcm16(slice);
        }
    }
}
