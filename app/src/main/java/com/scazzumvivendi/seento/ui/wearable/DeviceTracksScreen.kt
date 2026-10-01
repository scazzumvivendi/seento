package com.scazzumvivendi.seento.ui.wearable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.scazzumvivendi.seento.R
import com.scazzumvivendi.seento.data.ble.MdsConnectionStatus
import com.scazzumvivendi.seento.ui.theme.SeentoRed
import com.scazzumvivendi.seento.ui.theme.SeentoRedSoft
import com.scazzumvivendi.seento.ui.components.SeentoBottomBar
import com.scazzumvivendi.seento.ui.components.SeentoHeader
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DeviceTracksScreen(state: WearableUiState, onHomeClick: () -> Unit, onDeviceClick: () -> Unit,
                       onSettingsClick: () -> Unit,
                       modifier: Modifier = Modifier
                       ) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SeentoHeader() },
        bottomBar = {
            SeentoBottomBar(
                selected = 1,
                onPlaylistClick = onHomeClick,
                onDeviceClick = onDeviceClick,
                isDeviceConnected = state.connectionStatus == MdsConnectionStatus.CONNECTED,
                onSettingsClick = onSettingsClick
            )
        }
    ) { padding ->
    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(stringResource(R.string.tracks_list), style = MaterialTheme.typography.headlineMedium)
        if (state.connectionStatus != MdsConnectionStatus.CONNECTED) {
            Text(stringResource(R.string.connect_device_to_read_tracks), modifier = Modifier.padding(top = 16.dp))
        } else {
            if (!state.isLoadingMusic) LazyColumn {
                item { Text(pluralStringResource(R.plurals.track_count, state.tracks.size, state.tracks.size), style = MaterialTheme.typography.titleMedium) }
                items(state.tracks, key = { "track-${it.key}" }) { track ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(track.title ?: stringResource(R.string.track_number, track.key))
                        if (track.path.isNotBlank() && !track.path.startsWith("device-key:")) Text(track.path, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

    }

    }
}
