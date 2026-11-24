package com.otium.audio.data.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test

class SongBadgeTest {

    @Test
    fun `sample rate greater than or equal to 88200 is marked HI-RES`() {
        val badge = Song.determineAudioBadgeFast(
            path = "/storage/emulated/0/Music/track.flac",
            sampleRate = 96_000
        )
        assertEquals("HI-RES", badge)
    }

    @Test
    fun `standard flac without high sample rate is marked FLAC`() {
        val badge = Song.determineAudioBadgeFast(
            path = "/storage/emulated/0/Music/track.flac",
            sampleRate = 44_100
        )
        assertEquals("FLAC", badge)
    }

    @Test
    fun `high bitrate mp3 is marked 320K`() {
        val badge = Song.determineAudioBadgeFast(
            path = "/storage/emulated/0/Music/track.mp3",
            bitrate = 320_000
        )
        assertEquals("320K", badge)
    }

    @Test
    fun `standard mp3 is marked MP3`() {
        val badge = Song.determineAudioBadgeFast(
            path = "/storage/emulated/0/Music/track.mp3",
            bitrate = 192_000
        )
        assertEquals("MP3", badge)
    }

    @Test
    fun `unknown extension returns AUDIO not HI-RES`() {
        val badge = Song.determineAudioBadgeFast(
            path = "/storage/emulated/0/Music/unknown_file",
            sampleRate = 44_100
        )
        assertEquals("AUDIO", badge)
    }

    @Test
    fun `dsd and alac extensions are recognized`() {
        assertEquals("DSD", Song.determineAudioBadgeFast("/Music/track.dsf"))
        assertEquals("DSD", Song.determineAudioBadgeFast("/Music/track.dff"))
        assertEquals("ALAC", Song.determineAudioBadgeFast("/Music/track.alac"))
        assertEquals("AIFF", Song.determineAudioBadgeFast("/Music/track.aiff"))
    }

    @Test
    fun `extractFoldersFromSongs groups songs by directory`() {
        val dummyUri = Uri.parse("content://dummy")
        val songs = listOf(
            Song(1, "S1", "A", "Alb", 1000, dummyUri, dummyUri, 100, "Music/Rock/s1.mp3", 0),
            Song(2, "S2", "A", "Alb", 1000, dummyUri, dummyUri, 100, "Music/Rock/s2.mp3", 0),
            Song(3, "S3", "A", "Alb", 1000, dummyUri, dummyUri, 100, "Music/Jazz/s3.mp3", 0)
        )

        val folders = AudioFolder.extractFoldersFromSongs(songs)
        assertEquals(2, folders.size)
        assertEquals("Jazz", folders[0].name)
        assertEquals(1, folders[0].songCount)
        assertEquals("Rock", folders[1].name)
        assertEquals(2, folders[1].songCount)
    }
}
