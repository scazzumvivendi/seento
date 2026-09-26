package com.scazzumvivendi.seento.ui.playlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.scazzumvivendi.seento.R
import com.scazzumvivendi.seento.domain.model.Track

@Composable
fun TrackRow(
    position: Int,
    track: Track,
    modifier: Modifier = Modifier,
    onRemoveClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$position. ${track.title ?: track.path}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = track.path,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (onRemoveClick != null) {
            IconButton(onClick = onRemoveClick) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.remove_track))
            }
        }
    }
}
