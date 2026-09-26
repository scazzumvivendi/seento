package com.scazzumvivendi.seento.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String?,
    val path: String,
    val durationSeconds: Int?,
    val deviceKey: String?
)
