package com.scazzumvivendi.seento.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val tracks: List<Track> = emptyList(),
    val remotePlaylistId: Long? = null
) {
    fun addTrack(track: Track): Playlist {
        return copy(
            tracks = tracks + track
        )
    }

    fun removeTrack(trackId: Long): Playlist {
        return copy(
            tracks = tracks.filterNot { it.id == trackId }
        )
    }

    fun removeTrackAt(index: Int): Playlist {
        if (index !in tracks.indices) {
            return this
        }

        return copy(
            tracks = tracks.toMutableList().apply {
                removeAt(index)
            }
        )
    }

    fun moveTrack(fromIndex: Int, toIndex: Int): Playlist {
        if (fromIndex !in tracks.indices || toIndex !in tracks.indices) {
            return this
        }

        val reorderedTracks = tracks.toMutableList()
        val movedTrack = reorderedTracks.removeAt(fromIndex)
        reorderedTracks.add(toIndex, movedTrack)

        return copy(
            tracks = reorderedTracks
        )
    }
}
