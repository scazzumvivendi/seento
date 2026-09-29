package com.scazzumvivendi.seento.ui.playlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scazzumvivendi.seento.R
import com.scazzumvivendi.seento.domain.model.Track

@Composable
fun TrackRow(
    position: Int,
    track: Track,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier? = null,
    canMoveUp: Boolean = true,
    canMoveDown: Boolean = true,
    actionsEnabled: Boolean = true,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemoveClick: (() -> Unit)? = null
) {
    var menuExpanded by remember(track.id) { mutableStateOf(false) }
    val accessibleTrackName = track.title
        ?: stringResource(R.string.track_number, position)

    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dragHandleModifier != null) {
            IconButton(
                onClick = {},
                modifier = dragHandleModifier.clearAndSetSemantics {}
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$position. ${track.title ?: track.path}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (track.title != null && track.path.isNotBlank()) {
                Text(
                    text = track.path,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onMoveUp != null || onMoveDown != null || onRemoveClick != null) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = actionsEnabled
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(
                            R.string.track_actions,
                            accessibleTrackName
                        )
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (onMoveUp != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_track_up)) },
                            onClick = {
                                menuExpanded = false
                                onMoveUp()
                            },
                            enabled = actionsEnabled && canMoveUp
                        )
                    }
                    if (onMoveDown != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_track_down)) },
                            onClick = {
                                menuExpanded = false
                                onMoveDown()
                            },
                            enabled = actionsEnabled && canMoveDown
                        )
                    }
                    if (onRemoveClick != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_track)) },
                            onClick = {
                                menuExpanded = false
                                onRemoveClick()
                            },
                            enabled = actionsEnabled
                        )
                    }
                }
            }
        }
    }
}
