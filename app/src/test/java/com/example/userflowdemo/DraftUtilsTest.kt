package com.example.userflowdemo

import com.example.userflowdemo.utils.hasEntryChanged
import com.example.userflowdemo.utils.isDraftEmpty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftUtilsTest {

    @Test
    fun `isDraftEmpty returns true for null entry`() {
        assertTrue(isDraftEmpty(null))
    }

    @Test
    fun `isDraftEmpty returns true for entry with blank title, no media, and no observation`() {
        val entry = Entry(title = "  ", media = emptyList(), observation = "")
        assertTrue(isDraftEmpty(entry))
    }

    @Test
    fun `isDraftEmpty returns false if title is present`() {
        val entry = Entry(title = "Some Title")
        assertFalse(isDraftEmpty(entry))
    }

    @Test
    fun `isDraftEmpty returns false if media is present`() {
        val entry = Entry(title = "", media = listOf(MediaItem("uri")))
        assertFalse(isDraftEmpty(entry))
    }

    @Test
    fun `isDraftEmpty returns false if observation is present`() {
        val entry = Entry(title = "", observation = "Something noticed")
        assertFalse(isDraftEmpty(entry))
    }

    @Test
    fun `hasEntryChanged returns false if both are null`() {
        assertFalse(hasEntryChanged(null, null))
    }

    @Test
    fun `hasEntryChanged returns false if one is null`() {
        val entry = Entry(title = "Title")
        assertFalse(hasEntryChanged(entry, null))
        assertFalse(hasEntryChanged(null, entry))
    }

    @Test
    fun `hasEntryChanged returns true if title differs`() {
        val original = Entry(id = 1, title = "Original")
        val current = Entry(id = 1, title = "Changed")
        assertTrue(hasEntryChanged(original, current))
    }

    @Test
    fun `hasEntryChanged returns true if media differs`() {
        val original = Entry(id = 1, title = "T", media = listOf(MediaItem("1")))
        val current = Entry(id = 1, title = "T", media = listOf(MediaItem("2")))
        assertTrue(hasEntryChanged(original, current))
    }

    @Test
    fun `hasEntryChanged returns true if observation differs`() {
        val original = Entry(id = 1, title = "T", observation = "Old")
        val current = Entry(id = 1, title = "T", observation = "New")
        assertTrue(hasEntryChanged(original, current))
    }

    @Test
    fun `hasEntryChanged returns false if all relevant fields are same`() {
        val original = Entry(id = 1, title = "T", observation = "O", media = listOf(MediaItem("U")))
        val current = Entry(id = 1, title = "T", observation = "O", media = listOf(MediaItem("U")))
        assertFalse(hasEntryChanged(original, current))
    }
}
