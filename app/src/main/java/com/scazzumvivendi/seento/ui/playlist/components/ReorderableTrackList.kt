package com.scazzumvivendi.seento.ui.playlist.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.scazzumvivendi.seento.domain.model.Track
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ReorderableTrackList(
    tracks: List<Track>,
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit,
    onSaveTrackOrder: () -> Unit,
    onRemoveTrack: ((index: Int) -> Unit)? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    var hasPendingDragChanges by remember { mutableStateOf(false) }
    var displayedTracks by remember(tracks) { mutableStateOf(tracks) }

    fun moveDisplayedTrack(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in displayedTracks.indices || toIndex !in displayedTracks.indices ||
            fromIndex == toIndex
        ) return

        displayedTracks = displayedTracks.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        onMoveTrack(fromIndex, toIndex)
    }

    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        if (!readOnly && enabled && from.index != to.index) {
            moveDisplayedTrack(from.index, to.index)
            hasPendingDragChanges = true
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        itemsIndexed(
            items = displayedTracks,
            key = ::trackKey
        ) { index, track ->
            val itemKey = trackKey(index, track)
            ReorderableItem(
                state = reorderableState,
                key = itemKey,
            ) { isDragging ->
                val dragHandleModifier = if (readOnly || !enabled) {
                    null
                } else {
                    Modifier.draggableHandle(
                        onDragStarted = {
                            hasPendingDragChanges = false
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.GestureThresholdActivate
                            )
                        },
                        onDragStopped = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            if (hasPendingDragChanges) {
                                hasPendingDragChanges = false
                                onSaveTrackOrder()
                            }
                        }
                    )
                }

                TrackRow(
                    position = index + 1,
                    track = track,
                    dragHandleModifier = dragHandleModifier,
                    canMoveUp = enabled && index > 0,
                    canMoveDown = enabled && index < displayedTracks.lastIndex,
                    actionsEnabled = enabled,
                    onMoveUp = if (readOnly) null else {
                        {
                            if (enabled && index > 0) {
                                moveDisplayedTrack(index, index - 1)
                                onSaveTrackOrder()
                            }
                        }
                    },
                    onMoveDown = if (readOnly) null else {
                        {
                            if (enabled && index < displayedTracks.lastIndex) {
                                moveDisplayedTrack(index, index + 1)
                                onSaveTrackOrder()
                            }
                        }
                    },
                    onRemoveClick = if (readOnly) null else onRemoveTrack?.let { remove ->
                        { remove(index) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                        }
                )
            }
        }
    }
}

private fun trackKey(index: Int, track: Track): String =
    track.id.takeIf { it > 0L }
        ?.let { "track-$it" }
        ?: "device-${track.deviceKey ?: track.path}-$index"
