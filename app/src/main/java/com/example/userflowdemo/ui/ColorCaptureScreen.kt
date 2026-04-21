package com.example.userflowdemo.ui

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.AiState
import com.example.userflowdemo.EntryViewModel
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorCaptureScreen(
    imageUri: String,
    initialColors: List<Int>,
    viewModel: EntryViewModel,
    mediaIndex: Int?,
    mediaId: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val aiState by viewModel.aiState.collectAsState()
    val colorDetectionCount by viewModel.colorDetectionCount.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val listState = rememberLazyListState()

    var showClearConfirm by remember { mutableStateOf(false) }
    var showSortConfirm by remember { mutableStateOf(false) }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragDisplacement by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val itemWidthPx = remember(density) { with(density) { 88.dp.toPx() } } // Swatch(56) + Padding(16) + SpacedBy(16)

    // Derived State: Single Source of Truth for captured colors
    // We lookup the current colors from the ViewModel's draft StateFlow
    val capturedColors = remember(draft, mediaIndex, mediaId, initialColors) {
        val mediaItem = if (mediaIndex != null && mediaIndex >= 0) {
            draft?.media?.getOrNull(mediaIndex)
        } else if (mediaId != null) {
            draft?.media?.find { it.id == mediaId }
        } else {
            draft?.media?.find { it.imageUri == imageUri }
        }
        mediaItem?.colors ?: initialColors
    }
    
    // CRITICAL FIX: pointerInput captures the 'capturedColors' list from its initial scope.
    // To ensure the gesture listener always uses the LATEST list (so it can append to it),
    // we use rememberUpdatedState. This allows the closure to access the current value.
    val latestCapturedColors by rememberUpdatedState(capturedColors)

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Colors?") },
            text = { Text("This will remove all detected and manually selected colors for this image.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllColors(mediaId ?: "")
                    showClearConfirm = false
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSortConfirm) {
        AlertDialog(
            onDismissRequest = { showSortConfirm = false },
            title = { Text("Organize Colors?") },
            text = { Text("Organize colors into a gradient?") },
            confirmButton = {
                TextButton(onClick = {
                    val sorted = capturedColors.sortedWith { c1, c2 ->
                        val hsl1 = FloatArray(3)
                        val hsl2 = FloatArray(3)

                        androidx.core.graphics.ColorUtils.colorToHSL(c1, hsl1)
                        androidx.core.graphics.ColorUtils.colorToHSL(c2, hsl2)

                        fun getCategory(hsl: FloatArray): Int {
                            val s = hsl[1]
                            val l = hsl[2]

                            val isNeutral = s < 0.15f
                            val isVeryDark = l < 0.22f
                            val isMuddy = s < 0.35f && l < 0.5f && !isNeutral

                            return when {
                                isNeutral -> 0
                                isMuddy || isVeryDark -> 2
                                else -> 1
                            }
                        }

                        val cat1 = getCategory(hsl1)
                        val cat2 = getCategory(hsl2)

                        if (cat1 != cat2) {
                            cat1.compareTo(cat2)
                        } else {
                            when (cat1) {
                                0 -> {
                                    // Neutrals: Light → Dark
                                    hsl2[2].compareTo(hsl1[2])
                                }
                                1 -> {
                                    // Chromatic: Hue → Lightness
                                    val hueComp = hsl1[0].compareTo(hsl2[0])
                                    if (hueComp != 0) hueComp
                                    else hsl2[2].compareTo(hsl1[2])
                                }
                                else -> {
                                    // Outliers: Light → Dark
                                    hsl2[2].compareTo(hsl1[2])
                                }
                            }
                        }
                    }
                    viewModel.updateColors(imageUri, sorted, mediaIndex, mediaId)
                    showSortConfirm = false
                }) {
                    Text("Organize")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSortConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Auto-scroll logic for drag-and-drop
    LaunchedEffect(draggingIndex) {
        if (draggingIndex != null) {
            while (true) {
                val currentIdx = draggingIndex ?: break
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val draggedColor = capturedColors.getOrNull(currentIdx) ?: break
                val itemInfo = visibleItems.find { it.key == draggedColor }
                
                if (itemInfo != null) {
                    val rowWidth = layoutInfo.viewportSize.width.toFloat()
                    // Use the item's current position in the row + its relative drag displacement
                    val itemCenter = itemInfo.offset.toFloat() + itemInfo.size.toFloat() / 2f + dragDisplacement
                    
                    val scrollThreshold = 150f
                    val maxScrollSpeed = 25f
                    
                    if (itemCenter < scrollThreshold) {
                        // Smoothly increase speed as we get closer to the edge
                        val intensity = (scrollThreshold - itemCenter) / scrollThreshold
                        listState.scrollBy(-(maxScrollSpeed * intensity))
                    } else if (itemCenter > rowWidth - scrollThreshold) {
                        val intensity = (itemCenter - (rowWidth - scrollThreshold)) / scrollThreshold
                        listState.scrollBy(maxScrollSpeed * intensity)
                    }
                }
                kotlinx.coroutines.delay(16)
            }
        }
    }

    var isEyedropperActive by rememberSaveable { mutableStateOf(false) }

    // Fix: Reset AI state when the image changes to prevent cross-image leaks
    LaunchedEffect(imageUri) {
        viewModel.resetAiState()
    }

    val bitmap = remember(imageUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri.toUri())
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(imageUri.toUri())?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Color Capture",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onConfirm) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var magnifierOffset by remember { mutableStateOf(Offset.Unspecified) }
            var magnifierColor by remember { mutableStateOf<Int?>(null) }
            var showMagnifier by remember { mutableStateOf(false) }
            var containerSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onGloballyPositioned { containerSize = it.size },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageUri, isEyedropperActive) {
                            if (!isEyedropperActive) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                showMagnifier = true
                                
                                fun updateAt(offset: Offset) {
                                    magnifierOffset = offset
                                    bitmap?.let { b ->
                                        val viewWidth = size.width.toFloat()
                                        val viewHeight = size.height.toFloat()
                                        val bmpWidth = b.width.toFloat()
                                        val bmpHeight = b.height.toFloat()
                                        val scale = Math.min(viewWidth / bmpWidth, viewHeight / bmpHeight)
                                        val left = (viewWidth - bmpWidth * scale) / 2f
                                        val top = (viewHeight - bmpHeight * scale) / 2f
                                        
                                        if (offset.x in left..(left + bmpWidth * scale) && 
                                            offset.y in top..(top + bmpHeight * scale)) {
                                            val bx = ((offset.x - left) / scale).toInt().coerceIn(0, b.width - 1)
                                            val by = ((offset.y - top) / scale).toInt().coerceIn(0, b.height - 1)
                                            magnifierColor = b.getPixel(bx, by)
                                        } else {
                                            magnifierColor = null
                                        }
                                    }
                                }
                                
                                updateAt(down.position)
                                
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            updateAt(change.position)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                                
                                // Released
                                magnifierColor?.let { color ->
                                    if (latestCapturedColors.contains(color)) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Color already captured")
                                        }
                                    } else {
                                        viewModel.updateColors(imageUri, latestCapturedColors + color, mediaIndex, mediaId)
                                    }
                                }
                                
                                showMagnifier = false
                                magnifierOffset = Offset.Unspecified
                                magnifierColor = null
                            }
                        }
                )

                if (showMagnifier) {
                    MagnifierOverlay(
                        offset = magnifierOffset,
                        containerSize = containerSize,
                        bitmap = bitmap,
                        currentColor = magnifierColor
                    )
                }

                if (aiState is AiState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Suggested Colors Section
            if (aiState is AiState.Success) {
                val suggestions = (aiState as AiState.Success).colors
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detected Colors",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    val newColors = suggestions.filter { it !in capturedColors }
                                    if (newColors.isNotEmpty()) {
                                        viewModel.updateColors(imageUri, capturedColors + newColors, mediaIndex, mediaId)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Add All", style = MaterialTheme.typography.labelLarge)
                            }
                            IconButton(onClick = { viewModel.resetAiState() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Suggestions", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(suggestions) { colorInt ->
                            val isAlreadyCaptured = capturedColors.contains(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = 2.dp,
                                        color = if (isAlreadyCaptured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable(!isAlreadyCaptured) {
                                        viewModel.updateColors(imageUri, capturedColors + colorInt, mediaIndex, mediaId)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isAlreadyCaptured) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (Color(colorInt).luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (Color(colorInt).luminance() > 0.5f) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controls and Selected Colors
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = { viewModel.suggestColorsForImage(context, imageUri) },
                            enabled = aiState !is AiState.Loading,
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("notice colors", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Color Count Stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setColorDetectionCount(colorDetectionCount - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                            }
                            
                            Text(
                                text = colorDetectionCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            
                            IconButton(
                                onClick = { viewModel.setColorDetectionCount(colorDetectionCount + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Clear All", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = { if (capturedColors.isNotEmpty()) showSortConfirm = true },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Sort", 
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        IconButton(
                            onClick = { isEyedropperActive = !isEyedropperActive },
                            modifier = Modifier
                                .size(56.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (isEyedropperActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .background(
                                    if (isEyedropperActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Outlined.Colorize,
                                contentDescription = "Activate Eyedropper",
                                tint = if (isEyedropperActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                itemsIndexed(capturedColors, key = { _, color -> color }) { index, colorInt ->
                    val isDragging = draggingIndex == index
                    val offset = if (isDragging) dragDisplacement else 0f
                    
                    // CRITICAL: Gesture detector must use updated state to avoid stale index captures.
                    // This ensures that after reordering, long-pressing an item correctly identifies its current index.
                    val currentItemIndex by rememberUpdatedState(index)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .graphicsLayer {
                                translationX = offset
                                scaleX = if (isDragging) 1.1f else 1f
                                scaleY = if (isDragging) 1.1f else 1f
                                alpha = if (isDragging) 0.9f else 1f
                                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .animateItem()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        dragDisplacement = 0f // Reset displacement on new drag
                                        draggingIndex = currentItemIndex 
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragDisplacement = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragDisplacement = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDisplacement += dragAmount.x

                                        val currentIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                        
                                        // Improved reordering logic: move multiple positions in one gesture
                                        val shift = (dragDisplacement / itemWidthPx).roundToInt()
                                        val targetIdx = (currentIdx + shift).coerceIn(latestCapturedColors.indices)

                                        if (targetIdx != currentIdx) {
                                            val newList = latestCapturedColors.toMutableList()
                                            val item = newList.removeAt(currentIdx)
                                            newList.add(targetIdx, item)

                                            viewModel.updateColors(imageUri, newList, mediaIndex, mediaId)
                                            draggingIndex = targetIdx
                                            dragDisplacement -= (targetIdx - currentIdx) * itemWidthPx
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.TopEnd,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable {
                                        viewModel.updateColors(imageUri, capturedColors.filter { it != colorInt }, mediaIndex, mediaId)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove Color",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Text(
                            text = "#%06X".format(0xFFFFFF and colorInt),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// Extension to calculate luminance for adaptive icon tinting
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

@Composable
fun MagnifierOverlay(
    offset: Offset,
    containerSize: IntSize,
    bitmap: android.graphics.Bitmap?,
    currentColor: Int?
) {
    if (bitmap == null || offset == Offset.Unspecified || currentColor == null) return

    val density = LocalDensity.current
    val magnifierSizeDp = 100.dp 
    val magnifierSizePx = with(density) { magnifierSizeDp.toPx() }
    
    // Determine position based on vertical zone (Top Half vs Bottom Half)
    // crossing the midpoint (containerSize.height / 2) triggers the flip
    val isTopHalf = offset.y < containerSize.height / 2f

    // Position magnifier above or below the finger with a consistent gap
    // Top Half -> Magnifier BELOW finger (to keep it on screen)
    // Bottom Half -> Magnifier ABOVE finger (default)
    val verticalGapPx = with(density) { 100.dp.toPx() }
    val targetYOffset = if (isTopHalf) verticalGapPx else -verticalGapPx
    
    // Animate the vertical offset for a smooth flip transition
    val animatedYOffset by animateFloatAsState(
        targetValue = targetYOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "MagnifierFlip"
    )
    
    // Final center position for the magnifier, clamped to stay within container bounds
    val centerX = offset.x.coerceIn(magnifierSizePx / 2, containerSize.width.toFloat() - magnifierSizePx / 2)
    val centerY = (offset.y + animatedYOffset).coerceIn(magnifierSizePx / 2, containerSize.height.toFloat() - magnifierSizePx / 2)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (centerX - magnifierSizePx / 2).roundToInt(),
                        (centerY - magnifierSizePx / 2).roundToInt()
                    )
                }
                .size(magnifierSizeDp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.Black)
            ) {
                val bmpWidth = bitmap.width.toFloat()
                val bmpHeight = bitmap.height.toFloat()
                val viewWidth = containerSize.width.toFloat()
                val viewHeight = containerSize.height.toFloat()

                val scale = Math.min(viewWidth / bmpWidth, viewHeight / bmpHeight)
                val left = (viewWidth - bmpWidth * scale) / 2f
                val top = (viewHeight - bmpHeight * scale) / 2f

                val bitmapX = (offset.x - left) / scale
                val bitmapY = (offset.y - top) / scale

                val zoom = 8f // Increased slightly to compensate for smaller size
                val sourceSizePx = magnifierSizePx / (scale * zoom)
                
                val srcRectLeft = (bitmapX - sourceSizePx / 2).toInt()
                val srcRectTop = (bitmapY - sourceSizePx / 2).toInt()
                val srcRectSize = sourceSizePx.toInt()

                // Clamp to bitmap bounds
                val finalSrcLeft = srcRectLeft.coerceIn(0, (bitmap.width - srcRectSize).coerceAtLeast(0))
                val finalSrcTop = srcRectTop.coerceIn(0, (bitmap.height - srcRectSize).coerceAtLeast(0))
                val finalSrcSize = srcRectSize.coerceIn(1, bitmap.width.coerceAtMost(bitmap.height))

                drawImage(
                    image = bitmap.asImageBitmap(),
                    srcOffset = IntOffset(finalSrcLeft, finalSrcTop),
                    srcSize = IntSize(finalSrcSize, finalSrcSize),
                    dstSize = IntSize(magnifierSizePx.toInt(), magnifierSizePx.toInt()),
                    filterQuality = FilterQuality.None
                )
                
                // Draw center crosshair
                val rectSize = 14f // Adjusted for smaller magnifier size
                drawRect(
                    color = Color.White,
                    topLeft = Offset(this.center.x - rectSize/2, this.center.y - rectSize/2),
                    size = Size(rectSize, rectSize),
                    style = Stroke(width = 2f)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(this.center.x - rectSize/2 + 1f, this.center.y - rectSize/2 + 1f),
                    size = Size(rectSize - 2f, rectSize - 2f),
                    style = Stroke(width = 1f)
                )
            }
            
            // Color tag at the bottom
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(currentColor))
                            .border(1.dp, Color.Black.copy(alpha = 0.2f))
                    )
                    Text(
                        text = "#%06X".format(0xFFFFFF and currentColor),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }
            }
        }
    }
}
