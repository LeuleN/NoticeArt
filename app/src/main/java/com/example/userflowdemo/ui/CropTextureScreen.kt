package com.example.userflowdemo.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.userflowdemo.CropRect
import com.example.userflowdemo.EntryViewModel
import com.example.userflowdemo.Texture
import java.io.File
import java.io.FileOutputStream
import java.util.*

private enum class CropMode { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropTextureScreen(
    viewModel: EntryViewModel,
    mediaId: String,
    imageUri: String,
    existingTexture: Texture?,
    textureCount: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    var typedName by remember { mutableStateOf(if (existingTexture != null && existingTexture.isCustomName) existingTexture.name else "") }
    val placeholderName = existingTexture?.name ?: "Texture ${textureCount + 1}"

    val bitmap = remember(imageUri) {
        try {
            val uri = Uri.parse(imageUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) { null }
    }

    // State in pixels.
    var cropRect by remember {
        mutableStateOf(existingTexture?.cropRect ?: CropRect(0.2f, 0.2f, 0.8f, 0.8f))
    }

    var displaySize by remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }

    // Convert normalized to pixels once imageSize is known
    LaunchedEffect(imageSize) {
        if (imageSize.width > 0 && cropRect.left <= 1f && cropRect.right <= 1f) {
            val isDefault = cropRect.left == 0.2f && cropRect.top == 0.2f && cropRect.right == 0.8f && cropRect.bottom == 0.8f
            if (isDefault) {
                val size = Math.min(imageSize.width, imageSize.height) * 0.6f
                val left = (imageSize.width - size) / 2f
                val top = (imageSize.height - size) / 2f
                cropRect = CropRect(left, top, left + size, top + size)
            } else {
                cropRect = CropRect(
                    left = cropRect.left * imageSize.width,
                    top = cropRect.top * imageSize.height,
                    right = cropRect.right * imageSize.width,
                    bottom = cropRect.bottom * imageSize.height
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crop Texture", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val b = bitmap ?: return@IconButton
                            if (imageSize.width <= 0) return@IconButton

                            try {
                                val left = cropRect.left.coerceIn(0f, imageSize.width)
                                val top = cropRect.top.coerceIn(0f, imageSize.height)
                                val right = cropRect.right.coerceIn(0f, imageSize.width)
                                val bottom = cropRect.bottom.coerceIn(0f, imageSize.height)

                                val width = right - left
                                val height = bottom - top

                                if (width <= 0 || height <= 0) return@IconButton

                                val scaleX = b.width.toFloat() / imageSize.width
                                val scaleY = b.height.toFloat() / imageSize.height

                                val bitmapLeft = (left * scaleX).toInt().coerceIn(0, b.width - 1)
                                val bitmapTop = (top * scaleY).toInt().coerceIn(0, b.height - 1)
                                val bitmapWidth = (width * scaleX).toInt().coerceIn(1, b.width - bitmapLeft)
                                val bitmapHeight = (height * scaleY).toInt().coerceIn(1, b.height - bitmapTop)

                                val cropped = Bitmap.createBitmap(b, bitmapLeft, bitmapTop, bitmapWidth, bitmapHeight)
                                
                                val file = File(context.cacheDir, "texture_${UUID.randomUUID()}.png")
                                FileOutputStream(file).use { out ->
                                    cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                
                                val texture = Texture(
                                    id = existingTexture?.id ?: UUID.randomUUID().toString(),
                                    imageUri = Uri.fromFile(file).toString(),
                                    name = typedName.ifBlank { placeholderName },
                                    isCustomName = typedName.isNotBlank(),
                                    cropRect = cropRect
                                )
                                
                                viewModel.addTextureToImage(mediaId, texture)
                                onBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Crop failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .onGloballyPositioned { displaySize = it.size.toSize() }
            ) {
                if (bitmap != null) {
                    val bmpRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    
                    if (displaySize != Size.Zero) {
                        val displayRatio = displaySize.width / displaySize.height
                        if (bmpRatio > displayRatio) {
                            imageSize = Size(displaySize.width, displaySize.width / bmpRatio)
                        } else {
                            imageSize = Size(displaySize.height * bmpRatio, displaySize.height)
                        }
                        imageOffset = Offset(
                            (displaySize.width - imageSize.width) / 2f,
                            (displaySize.height - imageSize.height) / 2f
                        )
                    }

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (imageSize != Size.Zero) {
                        CropOverlay(
                            imageSize = imageSize,
                            imageOffset = imageOffset,
                            cropRect = cropRect,
                            onCropChanged = { cropRect = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                var isFocused by remember { mutableStateOf(false) }
                BasicTextField(
                    value = typedName,
                    onValueChange = { if (it.length <= 15) typedName = it },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (typedName.isEmpty() && !isFocused) {
                            Text(
                                text = placeholderName,
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    textDecoration = TextDecoration.Underline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun CropOverlay(
    imageSize: Size,
    imageOffset: Offset,
    cropRect: CropRect,
    onCropChanged: (CropRect) -> Unit
) {
    val density = LocalDensity.current
    val cornerRadius = with(density) { 30.dp.toPx() }
    val minSize = 40f

    var currentMode by remember { mutableStateOf(CropMode.NONE) }

    val updatedCropRect = rememberUpdatedState(cropRect)
    val updatedOnCropChanged = rememberUpdatedState(onCropChanged)

    val leftPx = imageOffset.x + cropRect.left
    val topPx = imageOffset.y + cropRect.top
    val rightPx = imageOffset.x + cropRect.right
    val bottomPx = imageOffset.y + cropRect.bottom

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageSize, imageOffset) {
                detectDragGestures(
                    onDragStart = { touch ->
                        val currentRect = updatedCropRect.value
                        val l = imageOffset.x + currentRect.left
                        val t = imageOffset.y + currentRect.top
                        val r = imageOffset.x + currentRect.right
                        val b = imageOffset.y + currentRect.bottom

                        val distTL = (touch - Offset(l, t)).getDistance()
                        val distTR = (touch - Offset(r, t)).getDistance()
                        val distBL = (touch - Offset(l, b)).getDistance()
                        val distBR = (touch - Offset(r, b)).getDistance()

                        currentMode = when {
                            distTL < cornerRadius -> CropMode.TOP_LEFT
                            distTR < cornerRadius -> CropMode.TOP_RIGHT
                            distBL < cornerRadius -> CropMode.BOTTOM_LEFT
                            distBR < cornerRadius -> CropMode.BOTTOM_RIGHT
                            touch.x in l..r && touch.y in t..b -> CropMode.MOVE
                            else -> CropMode.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (currentMode == CropMode.NONE) return@detectDragGestures
                        change.consume()

                        val currentRect = updatedCropRect.value
                        var newLeft = currentRect.left
                        var newTop = currentRect.top
                        var newRight = currentRect.right
                        var newBottom = currentRect.bottom

                        when (currentMode) {
                            CropMode.MOVE -> {
                                val width = currentRect.right - currentRect.left
                                val height = currentRect.bottom - currentRect.top
                                newLeft = (currentRect.left + dragAmount.x).coerceIn(0f, imageSize.width - width)
                                newTop = (currentRect.top + dragAmount.y).coerceIn(0f, imageSize.height - height)
                                newRight = newLeft + width
                                newBottom = newTop + height
                            }
                            CropMode.TOP_LEFT -> {
                                val drag = if (Math.abs(dragAmount.x) > Math.abs(dragAmount.y)) dragAmount.x else dragAmount.y
                                val maxPossibleSize = Math.min(currentRect.right, currentRect.bottom)
                                val newSize = (currentRect.right - currentRect.left - drag).coerceIn(minSize, maxPossibleSize)
                                newLeft = currentRect.right - newSize
                                newTop = currentRect.bottom - newSize
                                newRight = currentRect.right
                                newBottom = currentRect.bottom
                            }
                            CropMode.TOP_RIGHT -> {
                                val drag = if (Math.abs(dragAmount.x) > Math.abs(dragAmount.y)) dragAmount.x else -dragAmount.y
                                val maxPossibleSize = Math.min(imageSize.width - currentRect.left, currentRect.bottom)
                                val newSize = (currentRect.right - currentRect.left + drag).coerceIn(minSize, maxPossibleSize)
                                newLeft = currentRect.left
                                newTop = currentRect.bottom - newSize
                                newRight = currentRect.left + newSize
                                newBottom = currentRect.bottom
                            }
                            CropMode.BOTTOM_LEFT -> {
                                val drag = if (Math.abs(dragAmount.x) > Math.abs(dragAmount.y)) -dragAmount.x else dragAmount.y
                                val maxPossibleSize = Math.min(currentRect.right, imageSize.height - currentRect.top)
                                val newSize = (currentRect.right - currentRect.left + drag).coerceIn(minSize, maxPossibleSize)
                                newLeft = currentRect.right - newSize
                                newTop = currentRect.top
                                newRight = currentRect.right
                                newBottom = currentRect.top + newSize
                            }
                            CropMode.BOTTOM_RIGHT -> {
                                val drag = if (Math.abs(dragAmount.x) > Math.abs(dragAmount.y)) dragAmount.x else dragAmount.y
                                val maxPossibleSize = Math.min(imageSize.width - currentRect.left, imageSize.height - currentRect.top)
                                val newSize = (currentRect.right - currentRect.left + drag).coerceIn(minSize, maxPossibleSize)
                                newLeft = currentRect.left
                                newTop = currentRect.top
                                newRight = currentRect.left + newSize
                                newBottom = currentRect.top + newSize
                            }
                            else -> {}
                        }

                        updatedOnCropChanged.value(CropRect(newLeft, newTop, newRight, newBottom))
                    },
                    onDragEnd = { currentMode = CropMode.NONE },
                    onDragCancel = { currentMode = CropMode.NONE }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cropPath = Path().apply {
                addRect(Rect(Offset(leftPx, topPx), Size(rightPx - leftPx, bottomPx - topPx)))
            }
            clipPath(cropPath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.5f))
            }

            drawRect(
                color = Color.White,
                topLeft = Offset(leftPx, topPx),
                size = Size(rightPx - leftPx, bottomPx - topPx),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                )
            )

            val cornerLen = 24.dp.toPx()
            val cornerThickness = 4.dp.toPx()
            val corners = listOf(
                Offset(leftPx, topPx) to Offset(leftPx + cornerLen, topPx),
                Offset(leftPx, topPx) to Offset(leftPx, topPx + cornerLen),
                Offset(rightPx, topPx) to Offset(rightPx - cornerLen, topPx),
                Offset(rightPx, topPx) to Offset(rightPx, topPx + cornerLen),
                Offset(leftPx, bottomPx) to Offset(leftPx + cornerLen, bottomPx),
                Offset(leftPx, bottomPx) to Offset(leftPx, bottomPx - cornerLen),
                Offset(rightPx, bottomPx) to Offset(rightPx - cornerLen, bottomPx),
                Offset(rightPx, bottomPx) to Offset(rightPx, bottomPx - cornerLen)
            )

            corners.forEach { (start, end) ->
                drawLine(Color.White, start, end, cornerThickness, cap = StrokeCap.Round)
            }
        }
    }
}
