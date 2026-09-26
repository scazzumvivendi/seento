package com.scazzumvivendi.seento.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.scazzumvivendi.seento.R
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.MoreVert
import com.scazzumvivendi.seento.ui.playlist.components.PlaylistNameDialog
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.ui.theme.SeentoGreen
import com.scazzumvivendi.seento.ui.theme.SeentoRed
import com.scazzumvivendi.seento.ui.theme.SeentoRedSoft
import com.scazzumvivendi.seento.ui.theme.SeentoRedWash
import com.scazzumvivendi.seento.ui.theme.SeentoInk
import com.scazzumvivendi.seento.ui.components.SeentoBottomBar
import com.scazzumvivendi.seento.ui.components.SeentoHeader

@Composable
fun PlaylistLibraryScreen(
    state: PlaylistListUiState,
    onImportClick: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRetry: () -> Unit,
    onWearableClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    lastDeviceName: String? = null,
    isDeviceConnected: Boolean = false,
    onSendPlaylist: (Playlist) -> Unit = {},
    deviceFeedback: String? = null,
    deviceFeedbackIsError: Boolean = false,
    onTracksClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    when {
        state.isLoading -> {
            AppShell(selected = 0, onLibraryClick = {}, onDeviceClick = onWearableClick, onTracksClick = onTracksClick, onSettingsClick = onSettingsClick, tracksEnabled = isDeviceConnected) {
                LoadingContent(Modifier.fillMaxSize().safeDrawingPadding())
            }
        }

        state.errorMessage != null -> {
            AppShell(selected = 0, onLibraryClick = {}, onDeviceClick = onWearableClick, onTracksClick = onTracksClick, onSettingsClick = onSettingsClick, tracksEnabled = isDeviceConnected) {
                ErrorContent(message = state.errorMessage, onRetry = onRetry, modifier = Modifier.fillMaxSize().safeDrawingPadding())
            }
        }

        state.playlists.isEmpty() -> {
            AppShell(selected = 0, onLibraryClick = {}, onDeviceClick = onWearableClick, onTracksClick = onTracksClick, onSettingsClick = onSettingsClick, tracksEnabled = isDeviceConnected) {
                EmptyContent(
                    onImportClick = onImportClick,
                    onCreateClick = { showCreateDialog = true },
                    actionsEnabled = !state.isImporting && !state.isSaving,
                    modifier = Modifier.fillMaxSize().safeDrawingPadding()
                )
            }
        }

       else -> {
           AppShell(selected = 0, onLibraryClick = {}, onDeviceClick = onWearableClick, onTracksClick = onTracksClick, onSettingsClick = onSettingsClick, tracksEnabled = isDeviceConnected) {
                PlaylistList(
                    state,
                    onImportClick,
                    { showCreateDialog = true },
                    onPlaylistClick,
                    lastDeviceName,
                    isDeviceConnected,
                    onSendPlaylist,
                    deviceFeedback,
                    deviceFeedbackIsError,
                    Modifier
                )
           }
       }
    }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onCreatePlaylist(name)
            }
        )
    }
}

@Composable
private fun AppShell(selected: Int, onLibraryClick: () -> Unit, onDeviceClick: () -> Unit, onTracksClick: () -> Unit, onSettingsClick: () -> Unit, tracksEnabled: Boolean, content: @Composable () -> Unit) {
    Scaffold(
        topBar = { SeentoHeader() },
        content = { padding -> Column(Modifier.padding(padding)) { content() } },
        bottomBar = {
            SeentoBottomBar(
                selected = selected,
                onPlaylistClick = onLibraryClick,
                onDeviceClick = onDeviceClick,
                isDeviceConnected = tracksEnabled,
                onSettingsClick = onSettingsClick
            )
        }
    )
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    indicatorColor = SeentoRed,
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = SeentoRed
)

@Composable
private fun LoadingContent(
    modifier: Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.loading_playlists),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.generic_error),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp)
        )

        TextButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EmptyContent(
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    actionsEnabled: Boolean,
    modifier: Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.your_playlists),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            PlaylistActionsMenu(
                expanded = menuExpanded,
                onExpandChange = { menuExpanded = it },
                onCreateClick = onCreateClick,
                onImportClick = onImportClick,
                enabled = actionsEnabled
            )
        }

        Text(
            text = stringResource(R.string.no_playlists),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 48.dp)
        )

        Text(
            text = stringResource(R.string.first_playlist_hint),
                modifier = Modifier.padding(top = 8.dp)
        )

    }
}

@Composable
private fun PlaylistActionsMenu(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit,
    enabled: Boolean = true
) {
    Box {
        IconButton(
            onClick = { onExpandChange(true) },
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_playlist_actions)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            containerColor = SeentoRedSoft
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_playlist)) },
                onClick = {
                    onExpandChange(false)
                    onCreateClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.import_playlist)) },
                onClick = {
                    onExpandChange(false)
                    onImportClick()
                }
            )
        }
    }
}

@Composable
private fun PlaylistList(
    state: PlaylistListUiState,
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    lastDeviceName: String?,
    isDeviceConnected: Boolean,
    onSendPlaylist: (Playlist) -> Unit,
    deviceFeedback: String?,
    deviceFeedbackIsError: Boolean,
    modifier: Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.your_playlists),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                PlaylistActionsMenu(
                    expanded = menuExpanded,
                    onExpandChange = { menuExpanded = it },
                    onCreateClick = onCreateClick,
                    onImportClick = onImportClick,
                    enabled = !state.isSaving && !state.isImporting
                )
            }
            if (state.isImporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }

        items(
            items = state.playlists,
            key = { playlist -> playlist.id }
        ) { playlist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist.id) },
                colors = CardDefaults.cardColors(containerColor = SeentoRedSoft),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(SeentoRed, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(playlist.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            pluralStringResource(R.plurals.track_count, playlist.tracks.size, playlist.tracks.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledIconButton(
                        onClick = { onSendPlaylist(playlist) },
                        enabled = isDeviceConnected && !state.isSaving
                    ) {
                        Icon(Icons.Outlined.Send, contentDescription = stringResource(R.string.send_playlist))
                    }
                }
            }
        }

        item {
            deviceFeedback?.let { feedback ->
                Text(
                    text = feedback,
                    color = if (deviceFeedbackIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
