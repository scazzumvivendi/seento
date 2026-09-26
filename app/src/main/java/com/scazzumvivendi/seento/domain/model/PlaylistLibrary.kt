package com.scazzumvivendi.seento.domain.model

data class PlaylistLibrary(
    val playlists: List<Playlist> = emptyList()
) {
    fun addPlaylist(playlist: Playlist): PlaylistLibrary {
        return copy(
            playlists = playlists + playlist
        )
    }

    fun removePlaylist(playlistId: Long): PlaylistLibrary {
        return copy(
            playlists = playlists.filterNot { it.id == playlistId }
        )
    }

    fun renamePlaylist(
        playlistId: Long,
        newName: String
    ): PlaylistLibrary {
        return copy(
            playlists = playlists.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(name = newName)
                } else {
                    playlist
                }
            }
        )
    }

    fun findPlaylist(playlistId: Long): Playlist? {
        return playlists.firstOrNull { it.id == playlistId }
    }
}