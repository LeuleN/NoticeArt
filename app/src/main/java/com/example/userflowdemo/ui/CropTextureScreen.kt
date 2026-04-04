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
import coil.compose.AsyncImage
import com.example.userflowdemo.CropRect
import com.example.userflowdemo.Texture
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.roundToInt

private enum class CropMode { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropTextureScreen(
    imageUri: String,
    existingTexture: Texture?,
    textureCount: Int,
    onConfirm: (Texture) -> Unit,
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

    var cropRect by remember {
        mutableStateOf(existingTexture?.cropRect ?: CropRect(0.2f, 0.2f, 0.8f, 0.8f))
    }

    var displaySize by remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crop Texture", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            IconButton(
                onClick = {
                    bitmap?.let { b ->
                        try {
                            val bitmapLeft = (cropRect.left * b.width).roundToInt().coerceIn(0, b.width - 1)
                            val bitmapTop = (cropRect.top * b.height).roundToInt().coerceIn(0, b.height - 1)
                            val bitmapRight = (cropRect.right * b.width).roundToInt().coerceIn(bitmapLeft + 1, b.width)
                            val bitmapBottom = (cropRect.bottom * b.height).roundToInt().coerceIn(bitmapTop + 1, b.height)
                            
                            val width = (bitmapRight - bitmapLeft).coerceAtLeast(1)
                            val height = (bitmapBottom - bitmapTop).coerceAtLeast(1)
                            
                            val cropped = Bitmap.createBitmap(b, bitmapLeft, bitmapTop, width, height)
                            
                            val file = File(context.cacheDir, "texture_${UUID.randomUUID()}.png")
                            FileOutputStream(file).use { out ->
                                cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            
                            onConfirm(Texture(
                                id = existingTexture?.id ?: UUID.randomUUID().toString(),
                                imageUri = Uri.fromFile(file).toString(),
                                name = typedName.ifBlank { placeholderName },
                                isCustomName = typedName.isNotBlank(),
                                cropRect = cropRect
                            ))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Crop failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .border(3.dp, Color.Black, CircleShape)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Confirm",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Black
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
    val minSize = 0.1f

    var currentMode by remember { mutableStateOf(CropMode.NONE) }

    val leftPx = imageOffset.x + cropRect.left * imageSize.width
    val topPx = imageOffset.y + cropRect.top * imageSize.height
    val rightPx = imageOffset.x + cropRect.right * imageSize.width
    val bottomPx = imageOffset.y + cropRect.bottom * imageSize.height

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageSize, imageOffset, cropRect) {
                detectDragGestures(
                    onDragStart = { touch ->
                        val distTL = (touch - Offset(leftPx, topPx)).getDistance()
                        val distTR = (touch - Offset(rightPx, topPx)).getDistance()
                        val distBL = (touch - Offset(leftPx, bottomPx)).getDistance()
                        val distBR = (touch - Offset(rightPx, bottomPx)).getDistance()

                        currentMode = when {
                            distTL < cornerRadius -> CropMode.TOP_LEFT
                            distTR < cornerRadius -> CropMode.TOP_RIGHT
                            distBL < cornerRadius -> CropMode.BOTTOM_LEFT
                            distBR < cornerRadius -> CropMode.BOTTOM_RIGHT
                            touch.x in leftPx..rightPx && touch.y in topPx..bottomPx -> CropMode.MOVE
                            else -> CropMode.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (currentMode == CropMode.NONE) return@detectDragGestures

                        var newLeft = cropRect.left
                        var newTop = cropRect.top
                        var newRight = cropRect.right
                        var newBottom = cropRect.bottom

                        val dx = dragAmount.x / imageSize.width
                        val dy = dragAmount.y / imageSize.height

                        when (currentMode) {
                            CropMode.MOVE -> {
                                val width = cropRect.right - cropRect.left
                                val height = cropRect.bottom - cropRect.top
                                newLeft = (cropRect.left + dx).coerceIn(0f, 1f - width)
                                newTop = (cropRect.top + dy).coerceIn(0f, 1f - height)
                                newRight = newLeft + width
                                newBottom = newTop + height
                            }
                            CropMode.TOP_LEFT -> {
                                newLeft = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                                newTop = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
                            }
                            CropMode.TOP_RIGHT -> {
                                newRight = (cropRect.right + dx).coerceIn(cropRect.left + minSize, 1f)
                                newTop = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
                            }
                            CropMode.BOTTOM_LEFT -> {
                                newLeft = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                                newBottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, 1f)
                            }
                            CropMode.BOTTOM_RIGHT -> {
                                newRight = (cropRect.right + dx).coerceIn(cropRect.left + minSize, 1f)
                                newBottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, 1f)
                            }
                            else -> {}
                        }

                        onCropChanged(CropRect(newLeft, newTop, newRight, newBottom))
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
