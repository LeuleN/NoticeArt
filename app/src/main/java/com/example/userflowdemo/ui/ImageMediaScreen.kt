package com.example.userflowdemo.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
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
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageMediaScreen(
    initialImageUri: String?,
    initialColor: Int?,
    onConfirm: (String, Int?) -> Unit,
    onBack: () -> Unit
) {
    var imageUri by rememberSaveable { mutableStateOf(initialImageUri) }
    var selectedColor by rememberSaveable { mutableStateOf(initialColor) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            imageUri = it.toString()
            selectedColor = null 
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Media") },
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
                if (imageUri != null) {
                    val uri = imageUri!!
                    val bitmap = remember(uri) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
                                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                    decoder.isMutableRequired = true
                                }
                            } else {
                                context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(uri) {
                                detectTapGestures { offset ->
                                    bitmap?.let { b ->
                                        try {
                                            val viewWidth = size.width.toFloat()
                                            val viewHeight = size.height.toFloat()
                                            val bmpWidth = b.width.toFloat()
                                            val bmpHeight = b.height.toFloat()

                                            val scale = max(viewWidth / bmpWidth, viewHeight / bmpHeight)

                                            val scaledWidth = bmpWidth * scale
                                            val scaledHeight = bmpHeight * scale
                                            val offsetX = (scaledWidth - viewWidth) / 2f
                                            val offsetY = (scaledHeight - viewHeight) / 2f

                                            val bitmapX = ((offset.x + offsetX) / scale).toInt().coerceIn(0, b.width - 1)
                                            val bitmapY = ((offset.y + offsetY) / scale).toInt().coerceIn(0, b.height - 1)

                                            val pixel = b.getPixel(bitmapX, bitmapY)
                                            selectedColor = pixel
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                    )
                } else {
                    Text("No image selected", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = "Gallery", modifier = Modifier.size(32.dp))
                }

                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { /* visual only */ },
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(Icons.Outlined.Colorize, contentDescription = "Color Picker", modifier = Modifier.size(32.dp))
                    }
                    if (selectedColor != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color(selectedColor!!))
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        imageUri?.let { onConfirm(it, selectedColor) }
                    },
                    enabled = imageUri != null,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (imageUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = "Confirm", 
                        modifier = Modifier.size(32.dp),
                        tint = if (imageUri != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
