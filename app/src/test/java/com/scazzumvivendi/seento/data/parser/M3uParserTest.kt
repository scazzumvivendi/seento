package com.scazzumvivendi.seento.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {

    private val parser = M3uParser()

    @Test
    fun `parses extended m3u content`() {
        val content = """
            #EXTM3U
            #EXTINF:210,First song
            /music/first.mp3
            #EXTINF:180,Second song
            /music/second.mp3
        """.trimIndent()

        val result = parser.parse(
            content = content,
            playlistId = 1L,
            playlistName = "My playlist"
        )

        assertEquals(1L, result.id)
        assertEquals("My playlist", result.name)
        assertEquals(2, result.tracks.size)

        assertEquals(
            "First song",
            result.tracks[0].title
        )

        assertEquals(
            210,
            result.tracks[0].durationSeconds
        )

        assertEquals(
            "/music/second.mp3",
            result.tracks[1].path
        )
    }

    @Test
    fun `ignores comments and empty lines`() {
        val content = """
            #EXTM3U

            # This is a comment
            /music/song.mp3

        """.trimIndent()

        val result = parser.parse(
            content = content,
            playlistId = 1L,
            playlistName = "Test"
        )

        assertEquals(1, result.tracks.size)
        assertEquals(
            "/music/song.mp3",
            result.tracks[0].path
        )
    }

    @Test
    fun `removes utf8 bom`() {
        val content = "\uFEFF#EXTM3U\n/music/song.mp3"

        val result = parser.parse(
            content = content,
            playlistId = 1L,
            playlistName = "Test"
        )

        assertEquals(1, result.tracks.size)
        assertEquals(
            "/music/song.mp3",
            result.tracks[0].path
        )
    }

    @Test
    fun `preserves duplicate paths`() {
        val content = """
            #EXTM3U
            /music/song.mp3
            /music/song.mp3
        """.trimIndent()

        val result = parser.parse(
            content = content,
            playlistId = 1L,
            playlistName = "Test"
        )

        assertEquals(2, result.tracks.size)
    }
}