package com.example.userflowdemo.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.userflowdemo.Entry
import com.example.userflowdemo.EntryViewModel
import com.example.userflowdemo.ui.ColorCaptureScreen
import com.example.userflowdemo.ui.EntryDetailScreen
import com.example.userflowdemo.ui.HomeScreen
import com.example.userflowdemo.ui.ImageMediaScreen
import com.example.userflowdemo.ui.NewEntryScreen
import com.example.userflowdemo.ui.WelcomeScreen
import com.example.userflowdemo.utils.PreferenceManager
import com.example.userflowdemo.utils.isDraftEmpty
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun EntryApp(
    viewModel: EntryViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    
    var currentScreen by rememberSaveable { 
        mutableStateOf(if (preferenceManager.hasOnboarded()) "home" else "welcome") 
    }
    
    var userName by remember { mutableStateOf(preferenceManager.getUserName()) }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editingMediaIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var colorCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var recentlyDeletedEntry by remember { mutableStateOf<Entry?>(null) }
    var recentlyDiscardedDraft by remember { mutableStateOf<Entry?>(null) }

    val entries by viewModel.entries.collectAsState()
    val draft by viewModel.draft.collectAsState()

    // Handle Undo for deleted entries
    LaunchedEffect(recentlyDeletedEntry) {
        recentlyDeletedEntry?.let { entry ->
            val result = snackbarHostState.showSnackbar(
                message = "Entry deleted",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.insertEntry(entry)
            }
            recentlyDeletedEntry = null
        }
    }

    // Handle Undo for discarded drafts
    LaunchedEffect(recentlyDiscardedDraft) {
        recentlyDiscardedDraft?.let { entry ->
            val result = snackbarHostState.showSnackbar(
                message = "Draft discarded",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreDraft(entry)
                currentScreen = "newEntry"
            }
            recentlyDiscardedDraft = null
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addAudioUri(it.toString())
        }
    }

    when (currentScreen) {
        "welcome" -> {
            WelcomeScreen(
                onNameSubmitted = { name ->
                    preferenceManager.saveUserName(name)
                    preferenceManager.setOnboardingCompleted()
                    userName = name
                    currentScreen = "home"
                }
            )
        }
        "home" -> {
            HomeScreen(
                userName = userName,
                entries = entries,
                draft = draft,
                snackbarHostState = snackbarHostState,
                onAddClick = {
                    isEditing = false
                    selectedEntry = null
                    viewModel.createDraft()
                    currentScreen = "newEntry"
                },
                onDraftClick = {
                    isEditing = false
                    selectedEntry = null
                    currentScreen = "newEntry"
                },
                onEntryClick = { entry ->
                    selectedEntry = entry
                    currentScreen = "detail"
                }
            )
        }
        "newEntry" -> {
            NewEntryScreen(
                draft = draft,
                originalEntry = selectedEntry,
                isEditing = isEditing,
                snackbarHostState = snackbarHostState,
                onTitleChange = { newTitle ->
                    viewModel.updateDraft(newTitle)
                },
                onObservationChange = { newObservation ->
                    viewModel.updateObservation(newObservation)
                },
                onPublish = {
                    viewModel.publishDraft()
                    currentScreen = "home"
                },
                onSaveAndViewDetail = {
                    viewModel.publishDraft()
                    currentScreen = "detail"
                },
                onBackToHome = {
                    val draftToRestore = draft
                    viewModel.discardDraft()
                    if (draftToRestore != null && !isEditing && !isDraftEmpty(draftToRestore)) {
                        recentlyDiscardedDraft = draftToRestore
                    }
                    currentScreen = "home"
                },
                onBackToDetail = {
                    viewModel.discardDraft()
                    currentScreen = "detail"
                },
                onAutoSave = {
                    viewModel.autoSave()
                },
                onNavigateToImageMedia = { index ->
                    editingMediaIndex = index
                    currentScreen = "imageMedia"
                },
                onAddAudio = {
                    audioPicker.launch(arrayOf("audio/*"))
                },
                onRemoveAudio = { uri ->
                    viewModel.removeAudioUri(uri)
                }
            )
        }
        "imageMedia" -> {
            val mediaItem = editingMediaIndex?.let { index ->
                if (index >= 0) draft?.media?.getOrNull(index) else null
            }
            ImageMediaScreen(
                initialImageUri = mediaItem?.imageUri,
                onConfirm = { uri ->
                    viewModel.addOrUpdateMediaItem(uri, mediaItem?.colors ?: emptyList(), editingMediaIndex?.takeIf { it >= 0 })
                    editingMediaIndex = null
                    currentScreen = "newEntry"
                },
                onColorCapture = { uri ->
                    colorCaptureUri = uri
                    currentScreen = "colorCapture"
                },
                onBack = {
                    editingMediaIndex = null
                    currentScreen = "newEntry"
                }
            )
        }
        "colorCapture" -> {
            val mediaItem = editingMediaIndex?.let { index ->
                if (index >= 0) draft?.media?.getOrNull(index) else null
            }
            colorCaptureUri?.let { uri ->
                ColorCaptureScreen(
                    imageUri = uri,
                    initialColors = mediaItem?.colors ?: emptyList(),
                    onConfirm = { colors ->
                        viewModel.addOrUpdateMediaItem(uri, colors, editingMediaIndex?.takeIf { it >= 0 })
                        colorCaptureUri = null
                        editingMediaIndex = null
                        currentScreen = "newEntry"
                    },
                    onBack = {
                        colorCaptureUri = null
                        currentScreen = "imageMedia"
                    }
                )
            }
        }
        "detail" -> {
            selectedEntry?.let { entry ->
                val latestEntry = entries.find { it.id == entry.id } ?: entry
                EntryDetailScreen(
                    entry = latestEntry,
                    onBack = { currentScreen = "home" },
                    onDelete = {
                        recentlyDeletedEntry = latestEntry
                        viewModel.deleteEntry(latestEntry)
                        selectedEntry = null
                        currentScreen = "home"
                    },
                    onEdit = {
                        isEditing = true
                        viewModel.startEditing(latestEntry)
                        currentScreen = "newEntry"
                    }
                )
            }
        }
    }
}
