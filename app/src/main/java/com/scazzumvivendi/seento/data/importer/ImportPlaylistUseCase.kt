package com.scazzumvivendi.seento.data.importer

import com.scazzumvivendi.seento.data.parser.M3uParser
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.repository.PlaylistRepository

class ImportPlaylistUseCase(
    private val parser: M3uParser,
    private val repository: PlaylistRepository
) {

    suspend operator fun invoke(
        content: String,
        playlistName: String
    ): Playlist {
        val parsedPlaylist = parser.parse(
            content = content,
            playlistId = 0L,
            playlistName = playlistName
        )

        return repository.createPlaylist(
            name = parsedPlaylist.name,
            tracks = parsedPlaylist.tracks
        )
    }
}
