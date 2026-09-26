package com.scazzumvivendi.seento.domain.repository

import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.model.Track

interface PlaylistRepository {

    suspend fun createPlaylist(
        name: String,
        tracks: List<Track>,
        remotePlaylistId: Long? = null
    ): Playlist

    suspend fun appendTracks(
        playlistId: Long,
        tracks: List<Track>
    )

    suspend fun replacePlaylistTracks(
        playlistId: Long,
        tracks: List<Track>,
        remotePlaylistId: Long
    )

    suspend fun getAllPlaylists(): List<Playlist>

    suspend fun getPlaylist(
        playlistId: Long
    ): Playlist?

    suspend fun renamePlaylist(
        playlistId: Long,
        newName: String
    )

    suspend fun setRemotePlaylistId(playlistId: Long, remotePlaylistId: Long)

    suspend fun deletePlaylist(
        playlistId: Long
    )

    suspend fun removeTrackAt(
        playlistId: Long,
        position: Int
    )

    suspend fun reorderTracks(
        playlistId: Long,
        orderedTrackIds: List<Long>
    )
}
