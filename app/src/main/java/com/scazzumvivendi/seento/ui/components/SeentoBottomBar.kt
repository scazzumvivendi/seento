package com.scazzumvivendi.seento.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scazzumvivendi.seento.R
import com.scazzumvivendi.seento.ui.theme.SeentoGreen
import com.scazzumvivendi.seento.ui.theme.SeentoRed
import com.scazzumvivendi.seento.ui.theme.SeentoRedSoft

@Composable
fun SeentoBottomBar(
    selected: Int,
    onPlaylistClick: () -> Unit,
    onDeviceClick: () -> Unit,
    isDeviceConnected: Boolean,
    onSettingsClick: () -> Unit,
    settingsSelected: Boolean = false
) {
    NavigationBar(containerColor = SeentoRedSoft) {
        NavigationBarItem(
            selected = selected == 0,
            onClick = onPlaylistClick,
            icon = { Icon(Icons.AutoMirrored.Outlined.PlaylistPlay, contentDescription = null) },
            label = { androidx.compose.material3.Text(stringResource(R.string.playlists)) },
            colors = navigationItemColors()
        )
        NavigationBarItem(
            selected = selected == 1,
            onClick = onDeviceClick,
            icon = {
                Box {
                    Icon(Icons.Outlined.Watch, contentDescription = null)
                    if (isDeviceConnected) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .background(SeentoGreen, CircleShape)
                        )
                    }
                }
            },
            label = { androidx.compose.material3.Text(stringResource(R.string.device)) },
            colors = navigationItemColors()
        )
        NavigationBarItem(
            selected = settingsSelected,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { androidx.compose.material3.Text(stringResource(R.string.settings)) },
            colors = navigationItemColors()
        )
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    indicatorColor = SeentoRed,
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = SeentoRed
)
