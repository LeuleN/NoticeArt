package com.example.userflowdemo.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.Texture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

private fun createImageUri(context: Context): Uri {
    val picturesDir = context.getExternalFilesDir("Pictures")
        ?: throw IllegalStateException("Pictures directory is unavailable")

    val imageFile = File(
        picturesDir,
        "captured_${System.currentTimeMillis()}.jpg"
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}

private suspend fun copyPickedImageToAppStorage(
    context: Context,
    sourceUri: Uri
): String? = withContext(Dispatchers.IO) {
    try {
        val picturesDir = context.getExternalFilesDir("Pictures")
            ?: return@withContext null

        val destinationFile = File(
            picturesDir,
            "picked_${System.currentTimeMillis()}.jpg"
        )

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null

        destinationFile.toURI()
        Uri.fromFile(destinationFile).toString()
    } catch (e: Exception) {
        null
    }
}

private fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
    // If it's already an internal file URI, just return it
    if (uri.scheme == "file" || (uri.scheme == "content" && uri.authority == "${context.packageName}.provider")) {
        return uri.toString()
    }

    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(mediaDir, "image_${UUID.randomUUID()}.jpg")

        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageMediaScreen(
    initialImageUri: String?,
    initialColors: List<Int>,
    initialTextures: List<Texture>,
    onConfirm: (String, List<Int>, List<Texture>) -> Unit,
    onColorCapture: (String) -> Unit,
    onTextureCapture: (String) -> Unit,
    onBack: () -> Unit
) {
    var imageUri by rememberSaveable { mutableStateOf(initialImageUri) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showImageSourceDialog by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            scope.launch {
                isProcessingImage = true
                val localUri = copyPickedImageToAppStorage(context, pickedUri)
                if (localUri != null) {
                    imageUri = localUri
                }
                isProcessingImage = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            imageUri = pendingCameraUri?.toString()
        }
    }

    val handleBack = {
        if (!isProcessingImage) {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Add Image") },
            text = { Text("Choose an image source") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        val uri = createImageUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                ) {
                    Text("Take a photo from Camera")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Pick a photo from gallery")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Media") },
                navigationIcon = {
                    IconButton(
                        onClick = handleBack,
                        enabled = !isProcessingImage
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                when {
                    isProcessingImage -> {
                        CircularProgressIndicator()
                    }

                    imageUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        Text(
                            "No image selected",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                val isExistingImage = initialImageUri != null

                IconButton(
                    onClick = { showImageSourceDialog = true },
                    enabled = !isProcessingImage,
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            1.dp,
                            if (!isExistingImage) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = if (isExistingImage) "Image cannot be changed" else "Add image",
                        modifier = Modifier.size(32.dp),
                        tint = if (!isExistingImage) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = { imageUri?.let { onColorCapture(it) } },
                    enabled = imageUri != null && !isProcessingImage,
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            1.dp,
                            if (imageUri != null && !isProcessingImage) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Outlined.Colorize,
                        contentDescription = "Color Picker",
                        modifier = Modifier.size(32.dp),
                        tint = if (imageUri != null && !isProcessingImage) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }

                IconButton(
                    onClick = { imageUri?.let { onTextureCapture(it) } },
                    enabled = imageUri != null,
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.dp, if (imageUri != null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.Crop,
                        contentDescription = "Texture Capture",
                        modifier = Modifier.size(32.dp),
                        tint = if (imageUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = {
                        imageUri?.let { onConfirm(it, initialColors, initialTextures) }
                    },
                    enabled = imageUri != null && !isProcessingImage,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (imageUri != null && !isProcessingImage) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(32.dp),
                        tint = if (imageUri != null && !isProcessingImage) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}