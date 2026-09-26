package com.scazzumvivendi.seento.ui.playlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.scazzumvivendi.seento.data.importer.ImportPlaylistUseCase
import com.scazzumvivendi.seento.domain.repository.PlaylistRepository

class PlaylistViewModelFactory(
    private val repository: PlaylistRepository,
    private val importPlaylistUseCase: ImportPlaylistUseCase,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            return PlaylistViewModel(
                repository = repository,
                importPlaylistUseCase = importPlaylistUseCase,
                context = context.applicationContext
            ) as T
        }

        throw IllegalArgumentException(
            "ViewModel non supportato: ${modelClass.name}"
        )
    }
}
