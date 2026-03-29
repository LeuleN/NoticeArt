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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.userflowdemo.Entry
import com.example.userflowdemo.EntryViewModel
import com.example.userflowdemo.ui.EntryDetailScreen
import com.example.userflowdemo.ui.HomeScreen
import com.example.userflowdemo.ui.ImageMediaScreen
import com.example.userflowdemo.ui.NewEntryScreen
import com.example.userflowdemo.utils.isDraftEmpty

@Composable
fun EntryApp(
    viewModel: EntryViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

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

    when (currentScreen) {
        "home" -> {
            HomeScreen(
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
                    // Capture draft for UNDO before discarding
                    val draftToRestore = draft
                    viewModel.discardDraft()
                    // ✅ FIX: Only show UNDO snackbar if the draft was NOT empty
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
                onNavigateToImageMedia = {
                    currentScreen = "imageMedia"
                }
            )
        }
        "imageMedia" -> {
            ImageMediaScreen(
                initialImageUri = draft?.imageUris?.firstOrNull(),
                initialColor = draft?.color,
                onConfirm = { uri, color ->
                    viewModel.attachImage(uri)
                    color?.let { viewModel.updateColor(it) }
                    currentScreen = "newEntry"
                },
                onBack = {
                    currentScreen = "newEntry"
                }
            )
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
