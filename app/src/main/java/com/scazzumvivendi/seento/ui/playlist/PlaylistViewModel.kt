package com.scazzumvivendi.seento.ui.playlist

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scazzumvivendi.seento.data.importer.ImportPlaylistUseCase
import com.scazzumvivendi.seento.domain.repository.PlaylistRepository
import com.scazzumvivendi.seento.data.ble.model.RemoteMusicTrack
import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist
import com.scazzumvivendi.seento.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.scazzumvivendi.seento.R

class PlaylistViewModel(
    private val repository: PlaylistRepository,
    private val importPlaylistUseCase: ImportPlaylistUseCase,
    private val context: Context? = null
) : ViewModel() {

    private fun text(id: Int, vararg args: Any): String =
        context?.let { ContextCompat.getContextForLanguage(it).getString(id, *args) } ?: when (id) {
            R.string.cannot_load_playlists -> "Impossibile caricare le playlist"
            R.string.cannot_import_playlist -> "Impossibile importare la playlist"
            R.string.playlist_name_required -> "Inserisci un nome per la playlist"
            R.string.cannot_merge_device_playlists -> "Impossibile unire le playlist del dispositivo"
            R.string.device_playlist_missing_tracks -> "Il catalogo non contiene tutti i brani della playlist dell’orologio"
            R.string.cannot_replace_local_playlist -> "Impossibile sostituire la playlist locale"
            R.string.cannot_import_watch_playlist -> "Impossibile importare la playlist dell’orologio"
            R.string.cannot_copy_playlist -> "Impossibile creare una copia della playlist"
            R.string.tracks_already_in_playlist -> "I brani selezionati sono già nella playlist"
            R.string.cannot_add_device_tracks -> "Impossibile aggiungere i brani del dispositivo"
            R.string.cannot_save_track_order -> "Impossibile salvare il nuovo ordine"
            R.string.cannot_save_changes -> "Impossibile salvare le modifiche"
            R.string.track_number -> "Brano ${args.firstOrNull()}"
            R.string.copy_suffix -> "copia"
            R.string.copy_suffix_number -> "copia ${args.firstOrNull()}"
            else -> ""
        }

    private val _uiState = MutableStateFlow(
        PlaylistListUiState(isLoading = true)
    )

    val uiState: StateFlow<PlaylistListUiState> =
        _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    fun reload() {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.value = PlaylistListUiState(
                isLoading = true
            )

            try {
                val playlists = repository.getAllPlaylists()

                _uiState.value = PlaylistListUiState(
                    playlists = playlists
                )
            } catch (exception: Exception) {
                _uiState.value = PlaylistListUiState(
                    errorMessage = exception.message
                        ?: text(R.string.cannot_load_playlists)
                )
            }
        }
    }

    fun importPlaylist(
        content: String,
        playlistName: String
    ) {
        if (_uiState.value.isImporting) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                errorMessage = null
            )

            try {
                importPlaylistUseCase(
                    content = content,
                    playlistName = playlistName
                )

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    playlists = repository.getAllPlaylists()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = exception.message
                        ?: text(R.string.cannot_import_playlist)
                )
            }
        }
    }

    fun reportImportError(message: String) {
        _uiState.value = _uiState.value.copy(
            isImporting = false,
            errorMessage = message
        )
    }

    fun createPlaylist(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            reportImportError(text(R.string.playlist_name_required))
            return
        }

        mutatePlaylists {
            repository.createPlaylist(normalizedName, emptyList())
        }
    }

    fun mergeWatchPlaylists(
        remotePlaylists: List<RemotePlaylist>,
        remoteTracks: List<RemoteMusicTrack>
    ) {
        if (_uiState.value.isSaving || _uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                var local = repository.getAllPlaylists()
                val tracksByKey = remoteTracks.associateBy { it.key }
                remotePlaylists.forEach { remote ->
                    val incomingTracks = remote.songKeys.mapNotNull { key ->
                        val source = tracksByKey[key] ?: return@mapNotNull null
                        Track(
                            id = 0L,
                            title = source.title ?: source.path.substringAfterLast('/').takeIf(String::isNotBlank),
                            path = source.path,
                            durationSeconds = source.durationMillis?.div(1_000L)?.toInt(),
                            deviceKey = key.toString()
                        )
                    }
                    val existing = local.firstOrNull { it.remotePlaylistId == remote.id }
                        ?: local.firstOrNull {
                            it.remotePlaylistId == null &&
                                it.name.trim().equals(remote.name.trim(), ignoreCase = true)
                        }
                    if (existing == null) {
                        repository.createPlaylist(
                            name = remote.name,
                            tracks = incomingTracks,
                            remotePlaylistId = remote.id
                        )
                    } else {
                        if (existing.remotePlaylistId == null) {
                            repository.setRemotePlaylistId(existing.id, remote.id)
                        }
                        val existingKeys = existing.tracks.mapNotNull { it.deviceKey }.toSet()
                        val additions = incomingTracks.filterNot { it.deviceKey in existingKeys }
                        if (additions.isNotEmpty()) repository.appendTracks(existing.id, additions)
                    }
                    local = repository.getAllPlaylists()
                }
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    playlists = local
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = exception.message ?: text(R.string.cannot_merge_device_playlists)
                )
            }
        }
    }

    fun keepWatchPlaylist(localPlaylistId: Long, remote: RemotePlaylist, remoteTracks: List<RemoteMusicTrack>) {
        if (_uiState.value.isSaving || _uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val tracksByKey = remoteTracks.associateBy { it.key }
                val tracks = remote.songKeys.mapNotNull { key ->
                    val source = tracksByKey[key] ?: return@mapNotNull null
                    Track(
                        id = 0L,
                        title = source.title ?: source.path.substringAfterLast('/').takeIf(String::isNotBlank),
                        path = source.path,
                        durationSeconds = source.durationMillis?.div(1_000L)?.toInt(),
                        deviceKey = key.toString()
                    )
                }
                require(tracks.size == remote.songKeys.size) {
                    text(R.string.device_playlist_missing_tracks)
                }
                repository.replacePlaylistTracks(localPlaylistId, tracks, remote.id)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    playlists = repository.getAllPlaylists()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = exception.message ?: text(R.string.cannot_replace_local_playlist)
                )
            }
        }
    }

    fun importWatchPlaylist(remote: RemotePlaylist, remoteTracks: List<RemoteMusicTrack>) {
        if (_uiState.value.isSaving || _uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val tracks = tracksFromRemote(remote, remoteTracks)
                repository.createPlaylist(
                    name = remote.name,
                    tracks = tracks,
                    remotePlaylistId = remote.id
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    playlists = repository.getAllPlaylists()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = exception.message ?: text(R.string.cannot_import_watch_playlist)
                )
            }
        }
    }

    fun copyWatchPlaylist(remote: RemotePlaylist, remoteTracks: List<RemoteMusicTrack>) {
        if (_uiState.value.isSaving || _uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val tracks = tracksFromRemote(remote, remoteTracks)
                val existingNames = repository.getAllPlaylists()
                    .map { it.name.trim() }
                    .toSet()
                val baseName = "${remote.name} (${text(R.string.copy_suffix)})"
                var copyName = baseName
                var suffix = 2
                while (copyName in existingNames) {
                    copyName = "${remote.name} (${text(R.string.copy_suffix_number, suffix)})"
                    suffix++
                }
                repository.createPlaylist(name = copyName, tracks = tracks)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    playlists = repository.getAllPlaylists()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = exception.message ?: text(R.string.cannot_copy_playlist)
                )
            }
        }
    }

    private fun tracksFromRemote(
        remote: RemotePlaylist,
        remoteTracks: List<RemoteMusicTrack>
    ): List<Track> {
        val tracksByKey = remoteTracks.associateBy { it.key }
        val tracks = remote.songKeys.mapNotNull { key ->
            val source = tracksByKey[key] ?: return@mapNotNull null
            Track(
                id = 0L,
                title = source.title ?: source.path.substringAfterLast('/').takeIf(String::isNotBlank),
                path = source.path,
                durationSeconds = source.durationMillis?.div(1_000L)?.toInt(),
                deviceKey = key.toString()
            )
        }
        require(tracks.size == remote.songKeys.size) {
            text(R.string.device_playlist_missing_tracks)
        }
        return tracks
    }

    fun addDeviceTracks(playlistId: Long, deviceTracks: List<RemoteMusicTrack>) {
        if (_uiState.value.isSaving || _uiState.value.isImporting || deviceTracks.isEmpty()) return

        val playlist = _uiState.value.playlists.firstOrNull { it.id == playlistId } ?: return
        val existingDeviceKeys = playlist.tracks.mapNotNull { it.deviceKey }.toSet()
        val additions = deviceTracks
            .distinctBy { it.deviceKey }
            .filterNot { it.deviceKey in existingDeviceKeys }
            .map { source ->
                Track(
                    id = 0L,
                    title = source.title
                        ?: source.path.substringAfterLast('/').takeIf(String::isNotBlank)
                        ?: text(R.string.track_number, source.key),
                    path = source.path,
                    durationSeconds = source.durationMillis?.div(1_000L)?.toInt(),
                    deviceKey = source.deviceKey
                )
            }

        if (additions.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = text(R.string.tracks_already_in_playlist)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                repository.appendTracks(playlistId, additions)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    playlists = repository.getAllPlaylists()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = exception.message ?: text(R.string.cannot_add_device_tracks)
                )
            }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        val normalizedName = newName.trim()
        if (normalizedName.isEmpty()) {
            reportImportError(text(R.string.playlist_name_required))
            return
        }

        mutatePlaylists {
            repository.renamePlaylist(playlistId, normalizedName)
        }
    }

    fun setRemotePlaylistId(playlistId: Long, remotePlaylistId: Long) {
        mutatePlaylists {
            repository.setRemotePlaylistId(playlistId, remotePlaylistId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        mutatePlaylists {
            repository.deletePlaylist(playlistId)
        }
    }

    fun removeTrackAt(playlistId: Long, position: Int) {
        if (_uiState.value.playlists.none { it.id == playlistId }) {
            return
        }

        mutatePlaylists {
            repository.removeTrackAt(playlistId, position)
        }
    }

    fun moveTrack(
        playlistId: Long,
        fromIndex: Int,
        toIndex: Int
    ) {
        val currentPlaylist = _uiState.value.playlists
            .firstOrNull { it.id == playlistId }
            ?: return

        val updatedPlaylist = currentPlaylist.moveTrack(
            fromIndex = fromIndex,
            toIndex = toIndex
        )

        _uiState.value = _uiState.value.copy(
            playlists = _uiState.value.playlists.map { playlist ->
                if (playlist.id == playlistId) {
                    updatedPlaylist
                } else {
                    playlist
                }
            }
        )

        viewModelScope.launch {
            try {
                repository.reorderTracks(
                    playlistId = playlistId,
                    orderedTrackIds = updatedPlaylist.tracks.map { it.id }
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message
                        ?: text(R.string.cannot_save_track_order)
                )
            }
        }
    }

    private fun mutatePlaylists(action: suspend () -> Unit) {
        if (_uiState.value.isSaving || _uiState.value.isImporting) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null
            )

            try {
                action()
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    playlists = repository.getAllPlaylists()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = exception.message
                        ?: text(R.string.cannot_save_changes)
                )
            }
        }
    }
}
