package com.scazzumvivendi.seento.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.scazzumvivendi.seento.data.local.entity.PlaylistEntity
import com.scazzumvivendi.seento.data.local.entity.PlaylistTrackEntity
import com.scazzumvivendi.seento.data.local.entity.TrackEntity

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insertPlaylist(
        playlist: PlaylistEntity
    ): Long

    @Insert
    suspend fun insertTrack(
        track: TrackEntity
    ): Long

    @Insert
    suspend fun insertPlaylistTrack(
        relation: PlaylistTrackEntity
    ): Long

    @Query("SELECT COUNT(*) FROM playlists WHERE id = :playlistId")
    suspend fun playlistExists(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun countTracks(playlistId: Long): Int

    @Query("SELECT * FROM playlists ORDER BY name")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query(
        """
        SELECT tracks.*
        FROM tracks
        INNER JOIN playlist_tracks
            ON tracks.id = playlist_tracks.trackId
        WHERE playlist_tracks.playlistId = :playlistId
        ORDER BY playlist_tracks.position
        """
    )
    suspend fun getTracksForPlaylist(
        playlistId: Long
    ): List<TrackEntity>

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE id = :relationId")
    suspend fun deletePlaylistTrack(relationId: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(
        playlistId: Long,
        name: String
    )

    @Query("UPDATE playlists SET remotePlaylistId = :remotePlaylistId WHERE id = :playlistId")
    suspend fun setRemotePlaylistId(playlistId: Long, remotePlaylistId: Long)

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(
        playlistId: Long
    ): PlaylistEntity?

    @Query(
        """
    SELECT *
    FROM playlist_tracks
    WHERE playlistId = :playlistId
    ORDER BY position
    """
    )
    suspend fun getPlaylistTrackRelations(
        playlistId: Long
    ): List<PlaylistTrackEntity>

    @Query(
        """
    UPDATE playlist_tracks
    SET position = :position
    WHERE id = :relationId
    """
    )
    suspend fun updatePlaylistTrackPosition(
        relationId: Long,
        position: Int
    )
}
