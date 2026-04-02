package com.example.userflowdemo.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import android.content.Context
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.PhotoCamera
import java.io.File

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageMediaScreen(
    initialImageUri: String?,
    onConfirm: (String) -> Unit,
    onColorCapture: (String) -> Unit,
    onBack: () -> Unit
) {
    var imageUri by rememberSaveable { mutableStateOf(initialImageUri) }
    val context = LocalContext.current
    var showImageSourceDialog by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            imageUri = it.toString()
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
        onBack()
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
                        imagePicker.launch(arrayOf("image/*"))
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
                    IconButton(onClick = handleBack) {
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
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
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
                    onClick = { showImageSourceDialog = true },
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Add or change image",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { imageUri?.let { onColorCapture(it) } },
                    enabled = imageUri != null,
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.dp, if (imageUri != null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.Colorize, 
                        contentDescription = "Color Picker", 
                        modifier = Modifier.size(32.dp),
                        tint = if (imageUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = {
                        imageUri?.let { onConfirm(it) }
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
