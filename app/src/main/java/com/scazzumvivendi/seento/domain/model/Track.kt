package com.scazzumvivendi.seento.domain.model

data class Track(
    val id: Long,
    val title: String?,
    val path: String,
    val durationSeconds: Int? = null,
    val deviceKey: String? = null
)