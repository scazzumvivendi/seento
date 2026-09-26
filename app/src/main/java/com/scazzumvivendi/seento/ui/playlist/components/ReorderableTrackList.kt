package com.scazzumvivendi.seento.ui.playlist.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.scazzumvivendi.seento.domain.model.Track

@Composable
fun ReorderableTrackList(
    tracks: List<Track>,
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveTrack: ((index: Int) -> Unit)? = null,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    var draggedIndex by remember {
        mutableStateOf<Int?>(null)
    }

    var draggedOffset by remember {
        mutableFloatStateOf(0f)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        itemsIndexed(
            items = tracks,
            key = { index, track ->
                "${track.id}-$index"
            }
        ) { index, track ->

            val isDragged = draggedIndex == index

            TrackRow(
                position = index + 1,
                track = track,
                onRemoveClick = onRemoveTrack?.let { remove ->
                    { remove(index) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(
                        if (isDragged) 1f else 0f
                    )
                    .graphicsLayer {
                        translationY = if (isDragged) {
                            draggedOffset
                        } else {
                            0f
                        }
                        shadowElevation = if (isDragged) 16f else 0f
                        scaleX = if (isDragged) 1.02f else 1f
                        scaleY = if (isDragged) 1.02f else 1f
                    }
                    .pointerInput(index, tracks.size, readOnly) {
                        if (readOnly) return@pointerInput
                        detectDragGesturesAfterLongPress(
                            onDragStart = { _ ->
                                draggedIndex = index
                                draggedOffset = 0f
                            },
                            onDragCancel = {
                                draggedIndex = null
                                draggedOffset = 0f
                            },
                            onDragEnd = {
                                draggedIndex = null
                                draggedOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                val currentIndex =
                                    draggedIndex
                                        ?: return@detectDragGesturesAfterLongPress

                                draggedOffset += dragAmount.y

                                val currentItem =
                                    listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull {
                                            it.index == currentIndex
                                        }
                                        ?: return@detectDragGesturesAfterLongPress

                                val draggedCenter =
                                    currentItem.offset +
                                            draggedOffset +
                                            currentItem.size / 2

                                val targetItem =
                                    listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { item ->
                                            item.index != currentIndex &&
                                                    draggedCenter >= item.offset &&
                                                    draggedCenter <=
                                                    item.offset + item.size
                                        }

                                if (targetItem != null) {
                                    val fromIndex = currentIndex
                                    val toIndex = targetItem.index

                                    onMoveTrack(
                                        fromIndex,
                                        toIndex
                                    )

                                    draggedIndex = toIndex
                                    draggedOffset = 0f
                                }
                            }
                        )
                    }
            )
        }
    }
}
