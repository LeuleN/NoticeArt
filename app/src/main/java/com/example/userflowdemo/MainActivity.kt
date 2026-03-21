package com.example.userflowdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.userflowdemo.ui.theme.UserFlowDemoTheme
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.foundation.layout.safeDrawingPadding

// Import for Room
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState


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

    // Changes start here
    var editingEntry by remember { mutableStateOf<Entry?>(null) }
    // Changes end here

    val entries by viewModel.entries.collectAsState()
    val draft by viewModel.draft.collectAsState()

    if (currentScreen == "home") {
        HomeScreen(
            entries = entries,
            draft = draft,
            onAddClick = {
                // Changes start here
                editingEntry = null
                selectedEntry = null
                // Changes end here

                if (draft == null) {
                    viewModel.createDraft()
                }
                currentScreen = "newEntry"
            },
            onDraftClick = {
                // Changes start here
                editingEntry = null
                selectedEntry = null
                // Changes end here
                currentScreen = "newEntry"
            },
            onEntryClick = { entry ->
                selectedEntry = entry
                // Changes start here
                editingEntry = null
                // Changes end here
                currentScreen = "detail"
            }
        )
    } else if (currentScreen == "newEntry") {
        NewEntryScreen(
            draft = draft,
            // Changes start here
            editingEntry = editingEntry,
            // Changes end here
            onTitleChange = { newTitle ->
                // Changes start here
                if (editingEntry != null) {
                    editingEntry = editingEntry!!.copy(title = newTitle)
                } else {
                    viewModel.updateDraft(newTitle)
                }
                // Changes end here
            },
            onPublish = {
                // Changes start here
                if (editingEntry != null) {
                    viewModel.updateEntry(
                        editingEntry!!.copy(timestamp = System.currentTimeMillis())
                    )
                    selectedEntry = editingEntry
                    editingEntry = null
                } else {
                    viewModel.publishDraft()
                }
                // Changes end here
                currentScreen = "home"
            },
            onBack = {
                // Changes start here
                if (editingEntry != null) {
                    // Cancel edit: discard unsaved changes
                    editingEntry = null
                    currentScreen = "detail"
                } else {
                    currentScreen = "home"
                }
                // Changes end here
            },
            onDeleteDraft = {
                // Changes start here
                if (editingEntry == null) {
                    viewModel.deleteDraft()
                    currentScreen = "home"
                }
                // Changes end here
            },
            onImageSelected = { uri ->
                // Changes start here
                if (editingEntry != null) {
                    editingEntry = editingEntry!!.copy(
                        imageUri = uri,
                        color = null
                    )
                } else {
                    viewModel.attachImage(uri)
                }
                // Changes end here
            },
            onColorSelected = { color ->
                // Changes start here
                if (editingEntry != null) {
                    editingEntry = editingEntry!!.copy(color = color)
                } else {
                    viewModel.updateColor(color)
                }
                // Changes end here
            }
        )
    } else if (currentScreen == "detail") {
        selectedEntry?.let {
            EntryDetailScreen(
                entry = it,
                onBack = { currentScreen = "home" },
                onDelete = {
                    viewModel.deleteEntry(it)
                    // Changes start here
                    selectedEntry = null
                    editingEntry = null
                    // Changes end here
                    currentScreen = "home"
                },
                onEdit = {
                    // Changes start here
                    editingEntry = it.copy()
                    // Changes end here
                    currentScreen = "newEntry"
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    entries: List<Entry>,
    draft: Entry?,
    onAddClick: () -> Unit,
    onDraftClick: () -> Unit,
    onEntryClick: (Entry) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text("+")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            Text("Home Screen")

            draft?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "[Draft • Unsaved] ${it.title}",
                    color = Color.Red,
                    modifier = Modifier.clickable { onDraftClick() }
                )
            }

            entries
                .filter { !it.isDraft }
                .forEach { entry ->

                    val formattedTime = java.text.SimpleDateFormat(
                        "MMM dd, yyyy hh:mm a",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(entry.timestamp))

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        entry.color?.let {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(it))
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            text = "• ${entry.title} ${if (entry.imageUri != null) "📷" else ""}",
                            modifier = Modifier.clickable {
                                onEntryClick(entry)
                            }
                        )
                    }
                    Text(formattedTime)
                }
        }
    }
}

@Composable
fun NewEntryScreen(
    draft: Entry?,
    // Changes start here
    editingEntry: Entry?,
    // Changes end here
    onTitleChange: (String) -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    onDeleteDraft: () -> Unit,
    onImageSelected: (String) -> Unit,
    onColorSelected: (Int) -> Unit
) {
    // Changes start here
    val currentEntry = editingEntry ?: draft
    // Changes end here

    var title by rememberSaveable { mutableStateOf(currentEntry?.title ?: "") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Changes start here
    LaunchedEffect(currentEntry) {
        title = currentEntry?.title ?: ""
    }
    // Changes end here

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
                    // Changes start here
                    Text(if (editingEntry != null) "Edit Title" else "Title")
                    // Changes end here
                }
            )

            if (showError) {
                Text(
                    text = "Title required",
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Add Image",
                    modifier = Modifier.clickable {
                        imagePicker.launch(arrayOf("image/*"))
                    }
                )

                // Changes start here
                currentEntry?.color?.let {
                    // Changes end here
                    Spacer(modifier = Modifier.size(16.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(it))
                    )
                    Text(" (Selected Color)", modifier = Modifier.padding(start = 8.dp))
                }
            }

            // Changes start here
            currentEntry?.imageUri?.let { uri ->
                // Changes end here
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

                if (bitmap != null) {
                    Text(
                        "Tap image to pick color",
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(4.dp),
                        color = Color.White
                    )
                }

                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(200.dp)
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Back",
                modifier = Modifier.clickable {
                    onBack()
                }
            )

            // Changes start here
            if (editingEntry == null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Discard Draft",
                    color = Color.Red,
                    modifier = Modifier.clickable {
                        onDeleteDraft()
                    }
                )
            }
            // Changes end here
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

    // changed column so that there is padding in between detail screen text
    // and notifications bar above. also imported safeDrawingPadding
    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(16.dp)
    ) {

        Text("Entry Detail")

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Title: ${entry.title}")
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
        Text("Date: $formattedTime")

        entry.imageUri?.let { uri ->
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                modifier = Modifier.height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Back",
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Edit",
            modifier = Modifier.clickable {
                onEdit()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Delete Entry",
            color = Color.Red,
            modifier = Modifier.clickable { onDelete() }
        )
    }
}
