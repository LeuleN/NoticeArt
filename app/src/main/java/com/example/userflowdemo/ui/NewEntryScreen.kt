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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.Entry
import com.example.userflowdemo.components.DraggableScrollbar
import com.example.userflowdemo.utils.hasEntryChanged
import com.example.userflowdemo.utils.isDraftEmpty
import kotlin.math.max

@Composable
fun NewEntryScreen(
    draft: Entry?,
    editingEntry: Entry?,
    originalEntry: Entry?,
    snackbarHostState: SnackbarHostState,
    onTitleChange: (String) -> Unit,
    onObservationChange: (String) -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    onDeleteDraft: () -> Unit,
    onImageSelected: (String) -> Unit,
    onColorSelected: (Int) -> Unit,
    onAutoSave: (Entry) -> Unit
) {
    val currentEntry = editingEntry ?: draft
    var title by rememberSaveable { mutableStateOf(currentEntry?.title ?: "") }
    var observation by rememberSaveable { mutableStateOf(currentEntry?.observation ?: "") }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showEditDiscardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use updated states for the lifecycle observer to capture latest values
    val currentEditingEntryState by rememberUpdatedState(editingEntry)
    val onAutoSaveState by rememberUpdatedState(onAutoSave)

    // Auto-save logic
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                currentEditingEntryState?.let { onAutoSaveState(it) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            currentEditingEntryState?.let { onAutoSaveState(it) }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentEntry) {
        if (currentEntry?.title != title && title.isEmpty()) {
            title = currentEntry?.title ?: ""
        }
        if (currentEntry?.observation != observation && observation.isEmpty()) {
            observation = currentEntry?.observation ?: ""
        }
    }

    val handleBack = {
        if (editingEntry != null) {
            val updatedEditingEntry = editingEntry.copy(title = title, observation = observation)
            if (hasEntryChanged(originalEntry, updatedEditingEntry)) {
                showEditDiscardDialog = true
            } else {
                onBack()
            }
        } else {
            // For drafts, check if it's empty
            val currentDraftState = draft?.copy(title = title, observation = observation)
            if (isDraftEmpty(currentDraftState)) {
                onDeleteDraft() // Automatically delete and go home
            } else {
                showDiscardDialog = true // Confirm discard for contentful drafts
            }
        }
    }

    // Handle system back button
    BackHandler(onBack = handleBack)

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDeleteDraft()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showEditDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(onClick = {
                    showEditDiscardDialog = false
                    onBack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onImageSelected(it.toString())
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (title.isBlank()) {
                        showError = true
                    } else {
                        onPublish()
                    }
                }
            ) {
                Text("✓")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 1. Scrollable Content Area with Scrollbar
            val mainScrollState = rememberScrollState()
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(mainScrollState)
                        .padding(end = 12.dp) // Space for draggable thumb
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            showError = false
                            onTitleChange(it)
                        },
                        label = {
                            Text(if (editingEntry != null) "Edit Title" else "Title")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showError) {
                        Text(
                            text = "Title required",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = { imagePicker.launch(arrayOf("image/*")) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Add Image",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        currentEntry?.color?.let {
                            Spacer(modifier = Modifier.size(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(it))
                            )
                        }
                    }

                    currentEntry?.imageUri?.let { uri ->
                        Spacer(modifier = Modifier.height(16.dp))

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

                        Box(modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
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
                                                    // 1. Get View and Bitmap dimensions
                                                    val viewWidth = size.width.toFloat()
                                                    val viewHeight = size.height.toFloat()
                                                    val bmpWidth = b.width.toFloat()
                                                    val bmpHeight = b.height.toFloat()

                                                    // 2. Calculate Scale (ContentScale.Crop logic)
                                                    val scale = max(viewWidth / bmpWidth, viewHeight / bmpHeight)

                                                    // 3. Calculate cropping offsets
                                                    val scaledWidth = bmpWidth * scale
                                                    val scaledHeight = bmpHeight * scale
                                                    val offsetX = (scaledWidth - viewWidth) / 2f
                                                    val offsetY = (scaledHeight - viewHeight) / 2f

                                                    // 4. Map tap coordinate to original bitmap
                                                    val bitmapX = ((offset.x + offsetX) / scale).toInt().coerceIn(0, b.width - 1)
                                                    val bitmapY = ((offset.y + offsetY) / scale).toInt().coerceIn(0, b.height - 1)

                                                    val pixel = b.getPixel(bitmapX, bitmapY)
                                                    onColorSelected(pixel)
                                                } catch (e: Exception) {
                                                }
                                            }
                                        }
                                    }
                            )
                            
                            if (bitmap != null) {
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Tap image to pick color",
                                        modifier = Modifier.padding(6.dp),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Observations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 2. Internally Scrollable Observation Box with Draggable Scrollbar
                    val obsScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        TextField(
                            value = observation,
                            onValueChange = {
                                observation = it
                                onObservationChange(it)
                            },
                            placeholder = { Text("I notice...") },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(obsScrollState),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            )
                        )
                        
                        DraggableScrollbar(
                            scrollState = obsScrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 2.dp, top = 4.dp, bottom = 4.dp),
                            thumbHeight = 40.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                DraggableScrollbar(
                    scrollState = mainScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    thumbHeight = 80.dp
                )
            }

            // 3. Fixed Bottom Action Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Back",
                    modifier = Modifier.clickable { handleBack() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}