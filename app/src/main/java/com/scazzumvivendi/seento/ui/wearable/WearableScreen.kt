package com.scazzumvivendi.seento.ui.wearable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.scazzumvivendi.seento.R
import com.scazzumvivendi.seento.data.ble.MdsConnectionStatus
import com.scazzumvivendi.seento.data.ble.model.RemotePlaylist
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.ui.theme.SeentoRed
import com.scazzumvivendi.seento.ui.theme.SeentoRedSoft
import com.scazzumvivendi.seento.ui.theme.SeentoGreen
import com.scazzumvivendi.seento.ui.components.SeentoBottomBar
import com.scazzumvivendi.seento.ui.components.SeentoHeader

@Composable
fun WearableScreen(
    state: WearableUiState,
    onScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onForceDisconnect: () -> Unit,
    onLoadMusic: () -> Unit,
    onTracksClick: () -> Unit,
    onHomeClick: () -> Unit,
    localPlaylists: List<Playlist> = emptyList(),
    onImportPlaylist: (RemotePlaylist, Long?) -> Unit = { _, _ -> },
    onCopyPlaylist: (RemotePlaylist) -> Unit = {},
    onRemotePlaylistClick: (RemotePlaylist) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<RemotePlaylist?>(null) }
    val candidates = state.devices.filter { it.isLikelyCandidate }
    val otherDevices = state.devices.filterNot { it.isLikelyCandidate }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SeentoHeader() },
        bottomBar = {
            SeentoBottomBar(
                selected = 1,
                onPlaylistClick = onHomeClick,
                onDeviceClick = {},
                isDeviceConnected = state.connectionStatus == MdsConnectionStatus.CONNECTED,
                onSettingsClick = onSettingsClick
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = if (state.connectionStatus == MdsConnectionStatus.CONNECTED) {
                    state.lastDeviceName ?: stringResource(R.string.device)
                } else {
                    stringResource(R.string.device)
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more_device_actions)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = SeentoRedSoft
                ) {
                    if (state.connectionStatus == MdsConnectionStatus.DISCONNECTED ||
                        state.connectionStatus == MdsConnectionStatus.FAILED
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.search_device)) },
                            onClick = {
                                menuExpanded = false
                                onScanClick()
                            }
                        )
                    }
                    if (state.isScanning) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stop_search)) },
                            onClick = {
                                menuExpanded = false
                                onStopScanClick()
                            }
                        )
                    }
                    if (state.connectionStatus == MdsConnectionStatus.CONNECTED) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.refresh_catalog)) },
                            enabled = !state.isLoadingMusic,
                            onClick = {
                                menuExpanded = false
                                onLoadMusic()
                            }
                        )
                    }
                    if (state.connectionStatus == MdsConnectionStatus.CONNECTED ||
                        state.connectionStatus == MdsConnectionStatus.CONNECTING
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.disconnect)) },
                            onClick = {
                                menuExpanded = false
                                onDisconnect()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.force_disconnect)) },
                        onClick = {
                            menuExpanded = false
                            onForceDisconnect()
                        }
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if ((state.connectionStatus == MdsConnectionStatus.DISCONNECTED ||
                    state.connectionStatus == MdsConnectionStatus.FAILED) && candidates.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.compatible_devices),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(candidates, key = { it.address }) { device ->
                    DeviceRow(device, state, onConnect)
                }
            }

            if ((state.connectionStatus == MdsConnectionStatus.DISCONNECTED ||
                    state.connectionStatus == MdsConnectionStatus.FAILED) && otherDevices.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.other_devices),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(otherDevices, key = { it.address }) { device ->
                    DeviceRow(device, state, onConnect)
                }
            }

            if ((state.connectionStatus == MdsConnectionStatus.DISCONNECTED ||
                    state.connectionStatus == MdsConnectionStatus.FAILED) &&
                state.devices.isEmpty() && !state.isScanning &&
                state.connectionStatus == MdsConnectionStatus.DISCONNECTED
            ) {
                item { Text(stringResource(R.string.start_search_hint)) }
            }

            if (state.connectionStatus == MdsConnectionStatus.CONNECTED && state.isDeviceDataLoaded) {
                item {
                    Text(
                        stringResource(R.string.tracks),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SeentoRedSoft),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(SeentoRed, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(stringResource(R.string.music_catalog), style = MaterialTheme.typography.titleLarge)
                                Text(
                                    pluralStringResource(R.plurals.track_count, state.tracks.size, state.tracks.size),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledIconButton(onClick = onTracksClick) {
                                Icon(
                                    imageVector = Icons.Outlined.MusicNote,
                                    contentDescription = stringResource(R.string.music_catalog)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.playlists),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (state.remotePlaylists.isEmpty()) {
                    item { Text(stringResource(R.string.no_device_playlists)) }
                } else {
                    items(state.remotePlaylists, key = { "remote-playlist-${it.id}" }) { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRemotePlaylistClick(playlist) },
                            colors = CardDefaults.cardColors(containerColor = SeentoRedSoft),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(SeentoRed, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.LibraryMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(playlist.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "${playlist.songKeys.size} brani",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledIconButton(
                                    onClick = {
                                        val existing = localPlaylists.firstOrNull {
                                            it.name.trim().equals(playlist.name.trim(), ignoreCase = true)
                                        }
                                        if (existing == null) onImportPlaylist(playlist, null)
                                        else pendingImport = playlist
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.FileDownload,
                                        contentDescription = stringResource(R.string.import_playlist)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }

        }
    }

    pendingImport?.let { playlist ->
        val existing = localPlaylists.firstOrNull {
            it.name.trim().equals(playlist.name.trim(), ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.playlist_already_exists)) },
            text = { Text(stringResource(R.string.overwrite_or_copy)) },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        onCopyPlaylist(playlist)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text(stringResource(R.string.make_copy)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        if (existing != null) onImportPlaylist(playlist, existing.id)
                    }
                ) { Text(stringResource(R.string.overwrite)) }
            }
        )
    }

}

@Composable
private fun DeviceRow(
    device: com.scazzumvivendi.seento.data.ble.BleDevice,
    state: WearableUiState,
    onConnect: (String) -> Unit
) {
    val isConnectedDevice = state.connectedSerial != null &&
        device.name.contains(state.connectedSerial, ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SeentoRedSoft),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(SeentoRed, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(device.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = { onConnect(device.address) },
                enabled = !isConnectedDevice &&
                    state.connectionStatus != MdsConnectionStatus.CONNECTING
            ) {
                Icon(
                    imageVector = Icons.Outlined.BluetoothSearching,
                    contentDescription = stringResource(R.string.connect_to_device, device.name)
                )
            }
        }
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    indicatorColor = SeentoRed,
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = SeentoRed
)
