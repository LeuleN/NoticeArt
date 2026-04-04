package com.example.userflowdemo.navigation

import com.example.userflowdemo.ui.RecordAudioScreen
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
import com.example.userflowdemo.Texture
import com.example.userflowdemo.ui.TextureCaptureScreen
import com.example.userflowdemo.ui.CropTextureScreen
import java.util.UUID

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
    var currentMediaId by rememberSaveable { mutableStateOf<String?>(null) }
    var colorCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    var textureCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    
    // For CropTextureScreen
    var textureToEdit by remember { mutableStateOf<Texture?>(null) }

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
                onNavigateToImageMedia = { index: Int? ->
                    editingMediaIndex = index
                    currentMediaId = if (index != null && index >= 0) {
                        draft?.media?.getOrNull(index)?.id
                    } else null
                    currentScreen = "imageMedia"
                },
                onRemoveMedia = { index: Int ->
                    viewModel.removeMediaItem(index)
                },
                onAddAudioFromFiles = {
                    audioPicker.launch(arrayOf("audio/*"))
                },
                onRecordAudioNow = {
                    currentScreen = "recordAudio"
                },
                onRemoveAudio = { uri ->
                    viewModel.removeAudioUri(uri)
                }
            )
        }
        "recordAudio" -> {
            RecordAudioScreen(
                onConfirm = { uri ->
                    viewModel.addAudioUri(uri)
                    currentScreen = "newEntry"
                },
                onBack = {
                    currentScreen = "newEntry"
                }
            )
        }
        "imageMedia" -> {
            val mediaItem = if (editingMediaIndex != null && editingMediaIndex!! >= 0) {
                draft?.media?.getOrNull(editingMediaIndex!!)
            } else {
                draft?.media?.find { it.id == currentMediaId }
            }
            ImageMediaScreen(
                initialImageUri = mediaItem?.imageUri,
                initialColors = mediaItem?.colors ?: emptyList(),
                initialTextures = mediaItem?.textures ?: emptyList(),
                onConfirm = { uri, colors, textures ->
                    viewModel.addOrUpdateMediaItem(uri, colors, textures, editingMediaIndex?.takeIf { it >= 0 }, currentMediaId)
                    editingMediaIndex = null
                    currentMediaId = null
                    currentScreen = "newEntry"
                },
                onColorCapture = { uri ->
                    colorCaptureUri = uri
                    if (editingMediaIndex == -1 && currentMediaId == null) {
                        val newId = UUID.randomUUID().toString()
                        currentMediaId = newId
                        viewModel.addOrUpdateMediaItem(uri, emptyList(), emptyList(), mediaId = newId)
                    }
                    currentScreen = "colorCapture"
                },
                onTextureCapture = { uri ->
                    textureCaptureUri = uri
                    if (editingMediaIndex == -1 && currentMediaId == null) {
                        val newId = UUID.randomUUID().toString()
                        currentMediaId = newId
                        viewModel.addOrUpdateMediaItem(uri, emptyList(), emptyList(), mediaId = newId)
                    }
                    currentScreen = "textureCapture"
                },
                onBack = {
                    editingMediaIndex = null
                    currentMediaId = null
                    currentScreen = "newEntry"
                }
            )
        }
        "colorCapture" -> {
            val mediaItem = if (editingMediaIndex != null && editingMediaIndex!! >= 0) {
                draft?.media?.getOrNull(editingMediaIndex!!)
            } else {
                draft?.media?.find { it.id == currentMediaId }
            }
            colorCaptureUri?.let { uri ->
                ColorCaptureScreen(
                    imageUri = uri,
                    initialColors = mediaItem?.colors ?: emptyList(),
                    onConfirm = { colors ->
                        viewModel.addOrUpdateMediaItem(uri, colors, mediaItem?.textures ?: emptyList(), editingMediaIndex?.takeIf { it >= 0 }, currentMediaId)
                        colorCaptureUri = null
                        editingMediaIndex = null
                        currentMediaId = null
                        currentScreen = "newEntry"
                    },
                    onBack = {
                        colorCaptureUri = null
                        currentScreen = "imageMedia"
                    }
                )
            }
        }
        "textureCapture" -> {
            val mediaId = if (editingMediaIndex != null && editingMediaIndex!! >= 0) {
                draft?.media?.getOrNull(editingMediaIndex!!)?.id
            } else {
                currentMediaId
            }
            
            if (mediaId != null && textureCaptureUri != null) {
                TextureCaptureScreen(
                    viewModel = viewModel,
                    mediaId = mediaId,
                    imageUri = textureCaptureUri!!,
                    onAddTexture = {
                        textureToEdit = null
                        currentScreen = "cropTexture"
                    },
                    onEditTexture = { texture ->
                        textureToEdit = texture
                        currentScreen = "cropTexture"
                    },
                    onConfirm = {
                        textureCaptureUri = null
                        currentScreen = "imageMedia"
                    },
                    onBack = {
                        textureCaptureUri = null
                        currentScreen = "imageMedia"
                    }
                )
            }
        }
        "cropTexture" -> {
            val mediaId = if (editingMediaIndex != null && editingMediaIndex!! >= 0) {
                draft?.media?.getOrNull(editingMediaIndex!!)?.id
            } else {
                currentMediaId
            }
            
            if (mediaId != null && textureCaptureUri != null) {
                CropTextureScreen(
                    viewModel = viewModel,
                    mediaId = mediaId,
                    imageUri = textureCaptureUri!!,
                    existingTexture = textureToEdit,
                    textureCount = draft?.media?.find { it.id == mediaId }?.textures?.size ?: 0,
                    onBack = {
                        textureToEdit = null
                        currentScreen = "textureCapture"
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
