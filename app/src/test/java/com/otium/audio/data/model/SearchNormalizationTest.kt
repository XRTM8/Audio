package com.otium.audio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNormalizationTest {

    @Test
    fun `normalizes Arabic tashkeel harakat correctly`() {
        val input = "مُحَمَّدٌ"
        val normalized = Song.normalizeForSearchFast(input)
        assertEquals("محمد", normalized)
    }

    @Test
    fun `normalizes Alif variants to bare alif`() {
        val input = "إبراهيم وأحمد وآدم"
        val normalized = Song.normalizeForSearchFast(input)
        assertEquals("ابراهيم واحمد وادم", normalized)
    }

    @Test
    fun `normalizes Ta Marbuta to Ha and Alif Maqsura to Ya`() {
        val input = "فاطمة على موسى"
        val normalized = Song.normalizeForSearchFast(input)
        assertEquals("فاطمه علي موسي", normalized)
    }

    @Test
    fun `extractIndexLetterFast handles brackets and arabic letters`() {
        assertEquals("A", Song.extractIndexLetterFast("(Artist) Song"))
        assertEquals("ا", Song.extractIndexLetterFast("أحمد سعد"))
        assertEquals("م", Song.extractIndexLetterFast("محمد عبده"))
        assertEquals("#", Song.extractIndexLetterFast("123 Song"))
    }
}
