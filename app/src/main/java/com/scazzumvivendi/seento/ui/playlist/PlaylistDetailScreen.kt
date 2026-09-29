package com.scazzumvivendi.seento.ui.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.scazzumvivendi.seento.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.scazzumvivendi.seento.data.ble.model.RemoteMusicTrack
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.ui.playlist.components.PlaylistNameDialog
import com.scazzumvivendi.seento.ui.playlist.components.ReorderableTrackList
import com.scazzumvivendi.seento.ui.theme.SeentoRed
import com.scazzumvivendi.seento.ui.theme.SeentoRedSoft
import com.scazzumvivendi.seento.ui.components.SeentoBottomBar
import com.scazzumvivendi.seento.ui.components.SeentoHeader

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit,
    onSaveTrackOrder: () -> Unit = {},
    onRemoveTrack: (index: Int) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    deviceTracks: List<RemoteMusicTrack> = emptyList(),
    onAddDeviceTracks: (List<RemoteMusicTrack>) -> Unit = {},
    onOpenDevice: () -> Unit = {},
    onSendPlaylist: (Playlist) -> Unit = {},
    isDeviceConnected: Boolean = false,
    isSendingPlaylist: Boolean = false,
    sendFeedback: String? = null,
    sendFeedbackIsError: Boolean = false,
    onHomeClick: () -> Unit = {},
    onDeviceClick: () -> Unit = {},
    onTracksClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isSaving: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    showRemoveAction: Boolean = false,
    selectedBottomTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeviceTrackPicker by remember { mutableStateOf(false) }
    var selectedDeviceKeys by remember { mutableStateOf(emptySet<String>()) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SeentoHeader() },
        bottomBar = {
            SeentoBottomBar(
                selected = selectedBottomTab,
                onPlaylistClick = onHomeClick,
                onDeviceClick = onDeviceClick,
                isDeviceConnected = isDeviceConnected,
                onSettingsClick = onSettingsClick
            )
        }
    ) { contentPadding ->
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            if (!readOnly || showRemoveAction) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_playlist_actions))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = SeentoRedSoft
                    ) {
                        if (!readOnly) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename)) },
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_tracks)) },
                                onClick = {
                                    menuExpanded = false
                                    if (deviceTracks.isEmpty()) onOpenDevice()
                                    else {
                                        selectedDeviceKeys = emptySet()
                                        showDeviceTrackPicker = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.send)) },
                                enabled = !isSaving && !isSendingPlaylist && isDeviceConnected,
                                onClick = {
                                    menuExpanded = false
                                    onSendPlaylist(playlist)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                enabled = !isSaving,
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                        if (showRemoveAction) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove)) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isSaving) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        if (isSendingPlaylist) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text(stringResource(R.string.sending_playlist), modifier = Modifier.padding(top = 8.dp))
        }

        sendFeedback?.let { feedback ->
            Text(
                text = feedback,
                color = if (sendFeedbackIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Text(
            text = pluralStringResource(R.plurals.track_count, playlist.tracks.size, playlist.tracks.size),
            modifier = Modifier.padding(top = 8.dp)
        )

        ReorderableTrackList(
            tracks = playlist.tracks,
            onMoveTrack = onMoveTrack,
            onSaveTrackOrder = onSaveTrackOrder,
            onRemoveTrack = if (readOnly) null else onRemoveTrack,
            readOnly = readOnly,
            enabled = !isSaving,
            modifier = Modifier
                .weight(1f)
                .padding(top = 24.dp)
        )

    }
    }

    if (showRenameDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.rename_playlist),
            initialName = playlist.name,
            confirmLabel = stringResource(R.string.save),
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                showRenameDialog = false
                onRename(name)
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_playlist_question)) },
            text = { Text(stringResource(R.string.playlist_removed_from_device)) },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        )
    }

    if (showDeviceTrackPicker) {
        val selectedTracks = deviceTracks.filter { it.deviceKey in selectedDeviceKeys }
        AlertDialog(
            onDismissRequest = { showDeviceTrackPicker = false },
            title = { Text(stringResource(R.string.add_tracks)) },
            text = {
                Column {
                    Text(stringResource(R.string.selected_tracks_saved_as_references))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .padding(top = 8.dp)
                    ) {
                        items(deviceTracks, key = { it.deviceKey }) { track ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = track.deviceKey in selectedDeviceKeys,
                                    onCheckedChange = { checked ->
                                        selectedDeviceKeys = if (checked) {
                                            selectedDeviceKeys + track.deviceKey
                                        } else {
                                            selectedDeviceKeys - track.deviceKey
                                        }
                                    }
                                )
                                Column {
                                    Text(track.title ?: stringResource(R.string.track_number, track.key))
                                    if (track.artist != null) {
                                        Text(track.artist, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceTrackPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddDeviceTracks(selectedTracks)
                        showDeviceTrackPicker = false
                    },
                    enabled = selectedTracks.isNotEmpty() && !isSaving
                ) { Text(stringResource(R.string.add_selected_tracks, selectedTracks.size)) }
            }
        )
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    indicatorColor = SeentoRed,
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = SeentoRed
)
