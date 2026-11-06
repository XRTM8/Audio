package com.otium.audio.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * The kinds of output worth a separate curve. Detection is a priority list rather than a
 * query for "the active route", which Android only exposes for communication devices: the
 * highest priority device that is currently present wins.
 */
enum class OutputDevice(val key: String, val label: String) {
    USB("usb", "DAC عبر USB"),
    BLUETOOTH("bluetooth", "بلوتوث"),
    WIRED("wired", "سماعة سلكية"),
    SPEAKER("speaker", "مكبّر الهاتف");

    companion object {
        fun detect(context: Context): OutputDevice {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return SPEAKER
            val outputs = runCatching {
                manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            }.getOrDefault(emptyList())

            fun any(vararg types: Int) = outputs.any { it.type in types }

            return when {
                any(AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE) -> USB
                any(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO) -> BLUETOOTH
                any(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES) -> WIRED
                else -> SPEAKER
            }
        }
    }
}
