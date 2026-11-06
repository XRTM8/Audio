package com.otium.audio.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parses timestamps and sorts them`() {
        val lrc = """
            [00:12.50]second line
            [00:01.00]first line
            [ar:Someone]
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)

        assertTrue(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals(1_000L, parsed.lines[0].timeMs)
        assertEquals(12_500L, parsed.lines[1].timeMs)
        assertEquals("first line", parsed.lines[0].text)
    }

    @Test
    fun `two digit fractions are hundredths`() {
        val parsed = LrcParser.parse("[00:03.05]x")

        assertEquals(3_050L, parsed.lines.single().timeMs)
    }

    @Test
    fun `plain text is not treated as synced lyrics`() {
        val parsed = LrcParser.parse("just lyrics\nno timestamps here")

        assertFalse(parsed.isSynced)
        assertTrue(parsed.lines.isEmpty())
    }

    @Test
    fun `active index follows the playback position`() {
        val parsed = LrcParser.parse("[00:01.00]a\n[00:05.00]b\n[00:09.00]c")

        assertEquals(0, parsed.findActiveIndex(0))
        assertEquals(0, parsed.findActiveIndex(2_000))
        assertEquals(1, parsed.findActiveIndex(6_000))
        assertEquals(2, parsed.findActiveIndex(60_000))
    }
}
