package com.example.userflowdemo.ui

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.AiState
import com.example.userflowdemo.EntryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorCaptureScreen(
    imageUri: String,
    initialColors: List<Int>,
    viewModel: EntryViewModel,
    onConfirm: (List<Int>) -> Unit,
    onBack: () -> Unit
) {
    var capturedColors by rememberSaveable { mutableStateOf(initialColors) }
    var isEyedropperActive by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val aiState by viewModel.aiState.collectAsState()
    val colorCount by viewModel.colorCount.collectAsState()

    val bitmap = remember(imageUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(imageUri))
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageUri, isEyedropperActive) {
                            if (isEyedropperActive) {
                                detectTapGestures { offset ->
                                    bitmap?.let { b ->
                                        try {
                                            val viewWidth = size.width.toFloat()
                                            val viewHeight = size.height.toFloat()
                                            val bmpWidth = b.width.toFloat()
                                            val bmpHeight = b.height.toFloat()

                                            val scale = Math.min(viewWidth / bmpWidth, viewHeight / bmpHeight)
                                            val scaledWidth = bmpWidth * scale
                                            val scaledHeight = bmpHeight * scale

                                            val left = (viewWidth - scaledWidth) / 2f
                                            val top = (viewHeight - scaledHeight) / 2f

                                            if (offset.x in left..(left + scaledWidth) && offset.y in top..(top + scaledHeight)) {
                                                val bitmapX = ((offset.x - left) / scale).toInt().coerceIn(0, b.width - 1)
                                                val bitmapY = ((offset.y - top) / scale).toInt().coerceIn(0, b.height - 1)
                                                val pixel = b.getPixel(bitmapX, bitmapY)
                                                
                                                if (capturedColors.contains(pixel)) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Color already captured")
                                                    }
                                                } else {
                                                    capturedColors = capturedColors + pixel
                                                }
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                        }
                )

                if (aiState is AiState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suggested Colors Section (matches Texture screen style)
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
                        IconButton(onClick = { viewModel.resetAiState() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Suggestions", modifier = Modifier.size(16.dp))
                        }
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(suggestions) { colorInt ->
                            val isAlreadyCaptured = capturedColors.contains(colorInt)
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = 2.dp,
                                        color = if (isAlreadyCaptured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable(!isAlreadyCaptured) {
                                        capturedColors = capturedColors + colorInt
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isAlreadyCaptured) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (Color(colorInt).luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (Color(colorInt).luminance() > 0.5f) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls and Selected Colors
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
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
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Color Count Stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setColorCount(colorCount - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            
                            Text(
                                text = colorCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            IconButton(
                                onClick = { viewModel.setColorCount(colorCount + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }

                item {
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

                items(capturedColors) { colorInt ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
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
                                        capturedColors = capturedColors.filter { it != colorInt }
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

            Spacer(modifier = Modifier.height(24.dp))

            IconButton(
                onClick = { onConfirm(capturedColors) },
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Confirm",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// Extension to calculate luminance for adaptive icon tinting
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
