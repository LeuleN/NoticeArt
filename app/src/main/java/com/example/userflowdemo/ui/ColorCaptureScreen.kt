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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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

    LaunchedEffect(aiState) {
        if (aiState is AiState.Success) {
            val suggested = (aiState as AiState.Success).colors
            capturedColors = (capturedColors + suggested).distinct()
            viewModel.resetAiState()
        }
    }

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
            }

            if (aiState is AiState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color List Preview
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Button(
                        onClick = { viewModel.suggestColorsForImage(context, imageUri) },
                        enabled = aiState !is AiState.Loading,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Suggest")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
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
                    Spacer(modifier = Modifier.width(16.dp))
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
                            
                            // Delete button (overlay X)
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(20.dp)
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
                                    modifier = Modifier.size(14.dp),
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
