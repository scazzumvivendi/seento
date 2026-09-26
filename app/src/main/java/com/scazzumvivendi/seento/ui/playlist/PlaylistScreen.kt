package com.scazzumvivendi.seento.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.scazzumvivendi.seento.R
import com.scazzumvivendi.seento.domain.model.Track
import com.scazzumvivendi.seento.domain.model.Playlist
import com.scazzumvivendi.seento.ui.playlist.components.TrackRow

@Composable
fun PlaylistScreen(
    playlist: Playlist,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = pluralStringResource(R.plurals.track_count, playlist.tracks.size, playlist.tracks.size),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(playlist.tracks) { index, track ->
                TrackRow(
                    position = index + 1,
                    track = track
                )
            }
        }
    }
}
