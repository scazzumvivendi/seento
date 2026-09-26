package com.scazzumvivendi.seento.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scazzumvivendi.seento.data.local.entity.PlaylistEntity
import com.scazzumvivendi.seento.data.local.entity.PlaylistTrackEntity
import com.scazzumvivendi.seento.data.local.entity.TrackEntity

@Database(
    entities = [
        PlaylistEntity::class,
        TrackEntity::class,
        PlaylistTrackEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SeentoDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
}
