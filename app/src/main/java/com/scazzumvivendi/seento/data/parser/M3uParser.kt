package com.scazzumvivendi.seento.data.parser

import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.model.Track

class M3uParser {

    fun parse(
        content: String,
        playlistId: Long,
        playlistName: String
    ): Playlist {
        val tracks = mutableListOf<Track>()

        var pendingTitle: String? = null
        var pendingDuration: Int? = null

        val lines = content
            .removePrefix("\uFEFF")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (line in lines) {
            when {
                line.equals("#EXTM3U", ignoreCase = true) -> Unit

                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val metadata = line.substringAfter(":", "")
                    val commaIndex = metadata.indexOf(',')

                    if (commaIndex >= 0) {
                        pendingDuration = metadata
                            .substring(0, commaIndex)
                            .toIntOrNull()
                            ?.takeIf { it >= 0 }

                        pendingTitle = metadata
                            .substring(commaIndex + 1)
                            .trim()
                            .ifEmpty { null }
                    }
                }

                line.startsWith("#") -> Unit

                else -> {
                    tracks += Track(
                        id = 0L,
                        title = pendingTitle,
                        path = line,
                        durationSeconds = pendingDuration
                    )

                    pendingTitle = null
                    pendingDuration = null
                }
            }
        }

        return Playlist(
            id = playlistId,
            name = playlistName,
            tracks = tracks
        )
    }
}