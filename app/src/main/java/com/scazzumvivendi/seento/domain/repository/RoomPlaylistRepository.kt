package com.scazzumvivendi.seento.data.repository

import androidx.room.withTransaction
import com.scazzumvivendi.seento.data.local.SeentoDatabase
import com.scazzumvivendi.seento.data.local.entity.PlaylistEntity
import com.scazzumvivendi.seento.data.local.entity.PlaylistTrackEntity
import com.scazzumvivendi.seento.data.local.entity.TrackEntity
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.domain.model.Track
import com.scazzumvivendi.seento.domain.repository.PlaylistRepository

class RoomPlaylistRepository(
    private val database: SeentoDatabase
) : PlaylistRepository {

    private val dao
        get() = database.playlistDao()

    override suspend fun createPlaylist(
        name: String,
        tracks: List<Track>,
        remotePlaylistId: Long?
    ): Playlist {
        return database.withTransaction {
            val playlistId = dao.insertPlaylist(
                PlaylistEntity(name = name, remotePlaylistId = remotePlaylistId)
            )

            val savedTracks = tracks.mapIndexed { position, track ->
                val trackId = dao.insertTrack(
                    TrackEntity(
                        title = track.title,
                        path = track.path,
                        durationSeconds = track.durationSeconds,
                        deviceKey = track.deviceKey
                    )
                )

                dao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = playlistId,
                        trackId = trackId,
                        position = position
                    )
                )

                track.copy(id = trackId)
            }

            Playlist(
                id = playlistId,
                name = name,
                tracks = savedTracks,
                remotePlaylistId = remotePlaylistId
            )
        }
    }

    override suspend fun appendTracks(playlistId: Long, tracks: List<Track>) {
        database.withTransaction {
            require(dao.getPlaylistById(playlistId) != null) {
                "Playlist non trovata: $playlistId"
            }
            var position = dao.countTracks(playlistId)
            tracks.forEach { track ->
                val trackId = dao.insertTrack(
                    TrackEntity(
                        title = track.title,
                        path = track.path,
                        durationSeconds = track.durationSeconds,
                        deviceKey = track.deviceKey
                    )
                )
                dao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = playlistId,
                        trackId = trackId,
                        position = position++
                    )
                )
            }
        }
    }

    override suspend fun replacePlaylistTracks(
        playlistId: Long,
        tracks: List<Track>,
        remotePlaylistId: Long
    ) {
        database.withTransaction {
            require(dao.playlistExists(playlistId) > 0) {
                "Playlist non trovata: $playlistId"
            }
            dao.deletePlaylistTracks(playlistId)
            dao.setRemotePlaylistId(playlistId, remotePlaylistId)
            tracks.forEachIndexed { position, track ->
                val trackId = dao.insertTrack(
                    TrackEntity(
                        title = track.title,
                        path = track.path,
                        durationSeconds = track.durationSeconds,
                        deviceKey = track.deviceKey
                    )
                )
                dao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = playlistId,
                        trackId = trackId,
                        position = position
                    )
                )
            }
        }
    }

    override suspend fun getAllPlaylists(): List<Playlist> {
        return dao.getAllPlaylists().map { playlistEntity ->
            Playlist(
                id = playlistEntity.id,
                name = playlistEntity.name,
                tracks = dao
                    .getTracksForPlaylist(playlistEntity.id)
                    .map(::toDomain),
                remotePlaylistId = playlistEntity.remotePlaylistId
            )
        }
    }

    override suspend fun getPlaylist(
        playlistId: Long
    ): Playlist? {
        val playlistEntity = dao.getPlaylistById(playlistId)
            ?: return null

        return Playlist(
            id = playlistEntity.id,
            name = playlistEntity.name,
            tracks = dao
                .getTracksForPlaylist(playlistEntity.id)
                .map(::toDomain),
            remotePlaylistId = playlistEntity.remotePlaylistId
        )
    }

    override suspend fun renamePlaylist(
        playlistId: Long,
        newName: String
    ) {
        require(newName.isNotBlank()) {
            "Il nome della playlist non può essere vuoto"
        }

        dao.renamePlaylist(
            playlistId = playlistId,
            name = newName.trim()
        )
    }

    override suspend fun setRemotePlaylistId(playlistId: Long, remotePlaylistId: Long) {
        dao.setRemotePlaylistId(playlistId, remotePlaylistId)
    }

    override suspend fun deletePlaylist(
        playlistId: Long
    ) {
        database.withTransaction {
            dao.deletePlaylistTracks(playlistId)
            dao.deletePlaylist(playlistId)
        }
    }

    override suspend fun removeTrackAt(
        playlistId: Long,
        position: Int
    ) {
        database.withTransaction {
            val relations = dao.getPlaylistTrackRelations(playlistId)
            val relation = relations.getOrNull(position)
                ?: return@withTransaction

            dao.deletePlaylistTrack(relation.id)

            dao.getPlaylistTrackRelations(playlistId)
                .forEachIndexed { newPosition, remainingRelation ->
                    dao.updatePlaylistTrackPosition(
                        relationId = remainingRelation.id,
                        position = newPosition
                    )
                }
        }
    }

    private fun toDomain(
        entity: TrackEntity
    ): Track {
        return Track(
            id = entity.id,
            title = entity.title,
            path = entity.path,
            durationSeconds = entity.durationSeconds,
            deviceKey = entity.deviceKey
        )
    }

    override suspend fun reorderTracks(
        playlistId: Long,
        orderedTrackIds: List<Long>
    ) {
        database.withTransaction {
            val remainingRelations =
                dao.getPlaylistTrackRelations(playlistId).toMutableList()

            require(
                remainingRelations.size == orderedTrackIds.size
            ) {
                "Il numero di tracce non corrisponde"
            }

            orderedTrackIds.forEachIndexed { newPosition, trackId ->
                val relationIndex = remainingRelations
                    .indexOfFirst { relation ->
                        relation.trackId == trackId
                    }

                require(relationIndex >= 0) {
                    "Traccia non presente nella playlist: $trackId"
                }

                val relation = remainingRelations.removeAt(
                    relationIndex
                )

                dao.updatePlaylistTrackPosition(
                    relationId = relation.id,
                    position = newPosition
                )
            }
        }
    }
}
