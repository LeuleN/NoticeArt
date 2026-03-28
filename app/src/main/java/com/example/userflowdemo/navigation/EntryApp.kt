package com.example.userflowdemo.navigation

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
import com.example.userflowdemo.ui.NewEntryScreen

@Composable
fun EntryApp(
    viewModel: EntryViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }
    var editingEntry by remember { mutableStateOf<Entry?>(null) }
    var originalEntry by remember { mutableStateOf<Entry?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var recentlyDeletedEntry by remember { mutableStateOf<Entry?>(null) }

    val entries by viewModel.entries.collectAsState()
    val draft by viewModel.draft.collectAsState()

    LaunchedEffect(recentlyDeletedEntry) {
        recentlyDeletedEntry?.let { entry ->
            val result = snackbarHostState.showSnackbar(
                message = "Entry deleted",
                actionLabel = "UNDO"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.insertEntry(entry)
            }
            recentlyDeletedEntry = null
        }
    }

    if (currentScreen == "home") {
        HomeScreen(
            entries = entries,
            draft = draft,
            snackbarHostState = snackbarHostState,
            onAddClick = {
                editingEntry = null
                originalEntry = null
                selectedEntry = null
                viewModel.deleteDraft()
                viewModel.createDraft()
                currentScreen = "newEntry"
            },
            onDraftClick = {
                editingEntry = null
                originalEntry = null
                selectedEntry = null
                currentScreen = "newEntry"
            },
            onEntryClick = { entry ->
                selectedEntry = entry
                editingEntry = null
                originalEntry = null
                currentScreen = "detail"
            }
        )
    } else if (currentScreen == "newEntry") {
        NewEntryScreen(
            draft = draft,
            editingEntry = editingEntry,
            originalEntry = originalEntry,
            snackbarHostState = snackbarHostState,
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
                    originalEntry = null
                } else {
                    viewModel.publishDraft()
                }
                currentScreen = "home"
            },
            onBack = {
                if (editingEntry != null) {
                    editingEntry = null
                    originalEntry = null
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
            },
            onAutoSave = { entry ->
                viewModel.updateEntry(entry)
            }
        )
    } else if (currentScreen == "detail") {
        selectedEntry?.let {
            EntryDetailScreen(
                entry = it,
                onBack = { currentScreen = "home" },
                onDelete = {
                    recentlyDeletedEntry = it
                    viewModel.deleteEntry(it)
                    selectedEntry = null
                    editingEntry = null
                    originalEntry = null
                    currentScreen = "home"
                },
                onEdit = {
                    val entryToEdit = it.copy()
                    editingEntry = entryToEdit
                    originalEntry = it.copy()
                    currentScreen = "newEntry"
                }
            )
        }
    }
}
