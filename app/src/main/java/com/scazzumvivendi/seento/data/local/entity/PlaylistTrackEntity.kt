package com.scazzumvivendi.seento.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val playlistId: Long,
    val trackId: Long,
    val position: Int
)