package com.example.userflowdemo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.Entry
import com.example.userflowdemo.components.DraggableScrollbar
import com.example.userflowdemo.utils.hasEntryChanged
import com.example.userflowdemo.utils.isDraftEmpty

@OptIn(ExperimentalMaterial3Api::class)
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
    onAutoSave: (Entry) -> Unit,
    onNavigateToImageMedia: () -> Unit
) {
    val currentEntry = editingEntry ?: draft
    var title by rememberSaveable { mutableStateOf(currentEntry?.title ?: "") }
    var observation by rememberSaveable { mutableStateOf(currentEntry?.observation ?: "") }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showEditDiscardDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentEditingEntryState by rememberUpdatedState(editingEntry)
    val onAutoSaveState by rememberUpdatedState(onAutoSave)

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
            val currentDraftState = draft?.copy(title = title, observation = observation)
            if (isDraftEmpty(currentDraftState)) {
                onDeleteDraft()
            } else {
                showDiscardDialog = true
            }
        }
    }

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

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Capture Media",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add Image", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                showBottomSheet = false
                                onNavigateToImageMedia()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Add Image",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Entry", fontWeight = FontWeight.Bold) }
            )
        },
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
                Icon(Icons.Default.Check, contentDescription = "Publish")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            val mainScrollState = rememberScrollState()
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(mainScrollState)
                        .padding(end = 12.dp)
                ) {
                    TextField(
                        value = title,
                        onValueChange = {
                            title = it
                            showError = false
                            onTitleChange(it)
                        },
                        placeholder = {
                            Text("Title", fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    if (showError) {
                        Text(
                            text = "Title required",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Add Media", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentEntry?.imageUri == null) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .clickable { showBottomSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Media",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(currentEntry.imageUri),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            currentEntry.color?.let {
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(it))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Observations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val obsScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Back",
                    modifier = Modifier.clickable { handleBack() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
