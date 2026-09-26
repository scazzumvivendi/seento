package com.scazzumvivendi.seento.ui.playlist

import com.scazzumvivendi.seento.domain.model.Playlist

data class PlaylistListUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val isSaving: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val errorMessage: String? = null
)
