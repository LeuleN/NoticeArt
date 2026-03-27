package com.example.userflowdemo

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.ui.theme.UserFlowDemoTheme
import androidx.compose.material3.FloatingActionButton
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UserFlowDemoTheme {
                EntryApp()
            }
        }
    }
}

@Composable
fun EntryApp(
    viewModel: EntryViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var editingEntry by remember { mutableStateOf<Entry?>(null) }

    val entries by viewModel.entries.collectAsState()
    val draft by viewModel.draft.collectAsState()

    if (currentScreen == "home") {
        HomeScreen(
            entries = entries,
            draft = draft,
            onAddClick = {
                editingEntry = null
                selectedEntry = null
                if (draft == null) {
                    viewModel.createDraft()
                }
                currentScreen = "newEntry"
            },
            onDraftClick = {
                editingEntry = null
                selectedEntry = null
                currentScreen = "newEntry"
            },
            onEntryClick = { entry ->
                selectedEntry = entry
                editingEntry = null
                currentScreen = "detail"
            }
        )
    } else if (currentScreen == "newEntry") {
        NewEntryScreen(
            draft = draft,
            editingEntry = editingEntry,
            onTitleChange = { newTitle ->
                if (editingEntry != null) {
                    editingEntry = editingEntry!!.copy(title = newTitle)
                } else {
                    viewModel.updateDraft(newTitle)
                }
            },
            onPublish = {
                if (editingEntry != null) {
                    viewModel.updateEntry(
                        editingEntry!!.copy(timestamp = System.currentTimeMillis())
                    )
                    selectedEntry = editingEntry
                    editingEntry = null
                } else {
                    viewModel.publishDraft()
                }
                currentScreen = "home"
            },
            onBack = {
                if (editingEntry != null) {
                    editingEntry = null
                    currentScreen = "detail"
                } else {
                    currentScreen = "home"
                }
            },
            onDeleteDraft = {
                if (editingEntry == null) {
                    viewModel.deleteDraft()
                    currentScreen = "home"
                }
            },
            onImageSelected = { uri ->
                if (editingEntry != null) {
                    editingEntry = editingEntry!!.copy(
                        imageUri = uri,
                        color = null
                    )
                } else {
                    viewModel.attachImage(uri)
                }
            },
            onColorSelected = { color ->
                if (editingEntry != null) {
                    editingEntry = editingEntry!!.copy(color = color)
                } else {
                    viewModel.updateColor(color)
                }
            }
        )
    } else if (currentScreen == "detail") {
        selectedEntry?.let {
            EntryDetailScreen(
                entry = it,
                onBack = { currentScreen = "home" },
                onDelete = {
                    viewModel.deleteEntry(it)
                    selectedEntry = null
                    editingEntry = null
                    currentScreen = "home"
                },
                onEdit = {
                    editingEntry = it.copy()
                    currentScreen = "newEntry"
                }
            )
        }
    }
}

/**
 * Modern card-based grid layout for the Home Screen.
 */
@Composable
fun HomeScreen(
    entries: List<Entry>,
    draft: Entry?,
    onAddClick: () -> Unit,
    onDraftClick: () -> Unit,
    onEntryClick: (Entry) -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HomeHeader(userName = "Georgia")

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. "+" Create Card (First Item)
                item {
                    AddCard(onAddClick = onAddClick)
                }

                // 2. Draft Card (inside grid if it exists)
                if (draft != null) {
                    item {
                        DraftCard(draft = draft, onDraftClick = onDraftClick)
                    }
                }

                // 3. Entry Cards
                items(entries.filter { !it.isDraft }) { entry ->
                    EntryCard(
                        entry = entry,
                        onClick = { onEntryClick(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeHeader(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Hello, $userName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "What will you notice today?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AddCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        onClick = onAddClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "+",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun DraftCard(draft: Entry, onDraftClick: () -> Unit) {
    EntryCard(
        entry = draft,
        onClick = onDraftClick,
        isDraft = true
    )
}

@Composable
fun EntryCard(
    entry: Entry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDraft: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDraft) 0.8f else 1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Box {
            Column {
                // Top area: Image preview, Color block, or Placeholder
                val backgroundColor = when {
                    entry.imageUri == null && entry.color != null -> Color(entry.color)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(backgroundColor)
                ) {
                    if (entry.imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(entry.imageUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Bottom area: Title and Date
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entry.imageUri != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📷", fontSize = 14.sp)
                        }
                    }

                    val formattedDate = remember(entry.timestamp) {
                        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(entry.timestamp))
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // DRAFT Badge Overlay
            if (isDraft) {
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "DRAFT",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Helper to determine if a draft is effectively empty and should be discarded.
 */
fun isDraftEmpty(entry: Entry?): Boolean {
    return entry == null || (entry.title.isBlank() && entry.imageUri == null && entry.color == null)
}

@Composable
fun NewEntryScreen(
    draft: Entry?,
    editingEntry: Entry?,
    onTitleChange: (String) -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    onDeleteDraft: () -> Unit,
    onImageSelected: (String) -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val currentEntry = editingEntry ?: draft
    var title by rememberSaveable { mutableStateOf(currentEntry?.title ?: "") }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(currentEntry) {
        // Only update local title state if currentEntry actually changes (e.g. switching between items)
        // Keystrokes update currentEntry via onTitleChange, but we keep the local state for responsiveness.
        if (currentEntry?.title != title && title.isEmpty()) {
            title = currentEntry?.title ?: ""
        }
    }

    val handleBack = {
        if (editingEntry != null) {
            // If editing an existing entry, just go back to detail
            onBack()
        } else {
            // For drafts, check if it's empty
            val currentDraftState = draft?.copy(title = title)
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
            title = { Text("Discard draft?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard this draft?") },
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
                .padding(padding)
                .padding(16.dp)
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
                                            val x = (offset.x / size.width * b.width).toInt().coerceIn(0, b.width - 1)
                                            val y = (offset.y / size.height * b.height).toInt().coerceIn(0, b.height - 1)
                                            val pixel = b.getPixel(x, y)
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

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Back",
                    modifier = Modifier.clickable { handleBack() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (editingEntry == null) {
                    Text(
                        text = "Discard Draft",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { 
                            val currentDraftState = draft?.copy(title = title)
                            if (isDraftEmpty(currentDraftState)) {
                                onDeleteDraft()
                            } else {
                                showDiscardDialog = true 
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EntryDetailScreen(
    entry: Entry,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val formattedTime = java.text.SimpleDateFormat(
        "MMM dd, yyyy hh:mm a",
        java.util.Locale.getDefault()
    ).format(java.util.Date(entry.timestamp))

    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Entry Detail",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            entry.color?.let {
                Spacer(modifier = Modifier.size(16.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(it))
                )
            }
        }
        
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        entry.imageUri?.let { uri ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(300.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Back",
                modifier = Modifier.clickable { onBack() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                Text(
                    text = "Edit",
                    modifier = Modifier.clickable { onEdit() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "Delete",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}
