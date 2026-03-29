package com.example.userflowdemo.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    var isScrolling by remember { mutableStateOf(false) }
    
    // Detect scrolling activity
    LaunchedEffect(scrollState.value) {
        if (scrollState.maxValue > 0) {
            isScrolling = true
            delay(1000) // Stay visible for 1 second after scroll stops
            isScrolling = false
        }
    }

    // Smooth fade animation
    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "scrollbar_alpha"
    )

    if (scrollState.maxValue > 0) {
        val scrollbarHeightFraction = scrollState.viewportSize.toFloat() / (scrollState.maxValue + scrollState.viewportSize)
        val scrollbarOffsetFraction = scrollState.value.toFloat() / (scrollState.maxValue + scrollState.viewportSize)

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(3.dp)
                .alpha(alpha)
                .padding(vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(scrollbarHeightFraction)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = (scrollbarOffsetFraction * scrollState.viewportSize).dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/**
 * A persistent, classic-style scrollbar with a fixed height.
 */
@Composable
fun PersistentScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    if (scrollState.maxValue > 0) {
        val scrollbarHeight = 40.dp
        
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(4.dp)
                .padding(vertical = 4.dp)
        ) {
            val scrollFraction = scrollState.value.toFloat() / scrollState.maxValue
            
            Column(modifier = Modifier.fillMaxSize()) {
                if (scrollFraction > 0f) {
                    Spacer(modifier = Modifier.weight(scrollFraction))
                }
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = scrollbarHeight)
                        .background(
                            color = Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                if (scrollFraction < 1f) {
                    Spacer(modifier = Modifier.weight(1f - scrollFraction))
                }
            }
        }
    }
}

/**
 * A classic scrollbar with a fixed thumb size that moves within the viewport.
 */
@Composable
fun FixedThumbScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 60.dp
) {
    if (scrollState.maxValue > 0) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxHeight()
                .width(4.dp)
        ) {
            val visibleHeight = this.maxHeight
            val scrollPercent = scrollState.value.toFloat() / scrollState.maxValue
            
            // The range the thumb can move is the visible height minus the thumb's own height
            val moveRange = visibleHeight - thumbHeight
            val thumbOffset = moveRange * scrollPercent

            Box(
                modifier = Modifier
                    .offset(y = thumbOffset)
                    .size(width = 4.dp, height = thumbHeight)
                    .background(
                        color = Color.Gray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/**
 * A draggable scrollbar that allows user to scroll by grabbing the thumb.
 */
@Composable
fun DraggableScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 80.dp
) {
    if (scrollState.maxValue > 0) {
        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current

        BoxWithConstraints(
            modifier = modifier
                .fillMaxHeight()
                .width(32.dp) // Larger touch area for dragging
        ) {
            val visibleHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
            val thumbHeightPx = with(density) { thumbHeight.toPx() }
            val moveRangePx = visibleHeightPx - thumbHeightPx
            
            val scrollPercent = scrollState.value.toFloat() / scrollState.maxValue
            val currentThumbOffsetPx = scrollPercent * moveRangePx

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, currentThumbOffsetPx.roundToInt()) }
                    .size(width = 4.dp, height = thumbHeight)
                    .background(
                        color = Color.Gray.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .draggable(
                        state = rememberDraggableState { delta ->
                            val newOffsetPx = (currentThumbOffsetPx + delta).coerceIn(0f, moveRangePx)
                            val newScrollPercent = newOffsetPx / moveRangePx
                            coroutineScope.launch {
                                scrollState.scrollTo((newScrollPercent * scrollState.maxValue).roundToInt())
                            }
                        },
                        orientation = Orientation.Vertical
                    )
            )
        }
    }
}

/**
 * A scrollbar for LazyVerticalGrid that tracks scroll position based on visible items.
 */
@Composable
fun GridScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 60.dp
) {
    val totalItemsCount = gridState.layoutInfo.totalItemsCount
    val visibleItemsCount = gridState.layoutInfo.visibleItemsInfo.size
    
    if (totalItemsCount > visibleItemsCount) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxHeight()
                .width(4.dp)
        ) {
            val visibleHeight = this.maxHeight
            
            // Calculate scroll percentage
            val firstVisibleItem = gridState.firstVisibleItemIndex
            val firstVisibleOffset = gridState.firstVisibleItemScrollOffset.toFloat()
            
            // Average item height for offset calculation (rough estimate)
            val averageItemHeight = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?.toFloat() ?: 1f
            
            // Estimate total scrollable range
            // We use item indices as a proxy for scroll position
            val totalScrollPercent = if (totalItemsCount > 0) {
                (firstVisibleItem + (firstVisibleOffset / averageItemHeight)) / totalItemsCount.toFloat()
            } else 0f

            val moveRange = visibleHeight - thumbHeight
            val thumbOffset = moveRange * totalScrollPercent.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .offset(y = thumbOffset)
                    .size(width = 4.dp, height = thumbHeight)
                    .background(
                        color = Color.Gray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}