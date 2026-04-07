package com.example.userflowdemo.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onSizeChanged

/**
 * A draggable scrollbar for standard ScrollState.
 * Optimized to prevent NaN crashes and frequent recompositions.
 */
@Composable
fun DraggableScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 80.dp
) {
    if (scrollState.maxValue <= 0) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var containerHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp) // Touch area
            .onSizeChanged { size ->
                containerHeightPx = size.height.toFloat()
            }
    ) {
        if (containerHeightPx <= 0f) return@Box

        val thumbHeightPx = with(density) { thumbHeight.toPx() }
        val moveRangePx = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val maxValue = scrollState.maxValue
                    val scrollPercent =
                        if (maxValue > 0) scrollState.value.toFloat() / maxValue else 0f

                    IntOffset(
                        0,
                        (scrollPercent * moveRangePx).roundToInt()
                    )
                }
                .size(width = 4.dp, height = thumbHeight)
                .background(
                    color = Color.Gray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(2.dp)
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        val maxValue = scrollState.maxValue
                        if (maxValue > 0 && moveRangePx > 0f) {
                            val currentPercent = scrollState.value.toFloat() / maxValue
                            val currentOffsetPx = currentPercent * moveRangePx
                            val newOffsetPx =
                                (currentOffsetPx + delta).coerceIn(0f, moveRangePx)
                            val newPercent = newOffsetPx / moveRangePx

                            coroutineScope.launch {
                                scrollState.scrollTo((newPercent * maxValue).roundToInt())
                            }
                        }
                    },
                    orientation = Orientation.Vertical
                )
        )
    }
}

/**
 * A non-interactive scrollbar for LazyVerticalGrid.
 * Optimized using derivedStateOf and placement lambdas to avoid performance issues.
 */
@Composable
fun GridScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 60.dp
) {
    val showScrollbar by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            layoutInfo.totalItemsCount > layoutInfo.visibleItemsInfo.size
        }
    }

    if (!showScrollbar) return

    val density = LocalDensity.current
    var containerHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(4.dp)
            .onSizeChanged { size ->
                containerHeightPx = size.height.toFloat()
            }
    ) {
        if (containerHeightPx <= 0f) return@Box

        val moveRangePx = containerHeightPx - with(density) { thumbHeight.toPx() }

        Box(
            modifier = Modifier
                .offset {
                    val layoutInfo = gridState.layoutInfo
                    val totalItemsCount = layoutInfo.totalItemsCount
                    if (totalItemsCount == 0) return@offset IntOffset(0, 0)

                    val firstVisibleItem = gridState.firstVisibleItemIndex
                    val firstVisibleOffset = gridState.firstVisibleItemScrollOffset.toFloat()
                    val averageItemHeight =
                        layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?.toFloat() ?: 1f

                    val scrollPercent =
                        (firstVisibleItem + (firstVisibleOffset / averageItemHeight)) / totalItemsCount.toFloat()

                    IntOffset(
                        0,
                        (scrollPercent.coerceIn(0f, 1f) * moveRangePx).roundToInt()
                    )
                }
                .size(width = 4.dp, height = thumbHeight)
                .background(
                    color = Color.Gray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}