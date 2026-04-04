package com.example.userflowdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.userflowdemo.Entry
import com.example.userflowdemo.components.AddCard
import com.example.userflowdemo.components.DraftCard
import com.example.userflowdemo.components.EntryCard
import com.example.userflowdemo.components.GridScrollbar
import androidx.compose.foundation.clickable

/**
 * Modern card-based grid layout for the Home Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    entries: List<Entry>,
    draft: Entry?,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onDraftClick: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onExportPdf: (Entry) -> Unit,
    onShareEntry: (Entry) -> Unit,
    onDeleteEntry: (Entry) -> Unit
) {
    var showDraftDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    
    var selectedEntryForMenu by remember { mutableStateOf<Entry?>(null) }
    val sheetState = rememberModalBottomSheetState()

    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("Unfinished draft") },
            text = { Text("You have unsaved changes. What would you like to do?") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showDraftDialog = false
                        onDraftClick()
                    }) {
                        Text("Continue Draft")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        showDraftDialog = false
                        onAddClick()
                    }) {
                        Text("Start New Entry", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDraftDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HomeHeader(userName = userName)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. "+" Create Card (First Item)
                    item {
                        AddCard(onAddClick = {
                            if (draft == null) {
                                onAddClick()
                            } else {
                                showDraftDialog = true
                            }
                        })
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
                            onClick = { onEntryClick(entry) },
                            onLongClick = { selectedEntryForMenu = entry }
                        )
                    }
                }

                GridScrollbar(
                    gridState = gridState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 8.dp, bottom = 8.dp),
                    thumbHeight = 60.dp
                )
            }
        }
    }

    if (selectedEntryForMenu != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedEntryForMenu = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = selectedEntryForMenu?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                
                ListItem(
                    headlineContent = { Text("Export as PDF") },
                    leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedEntryForMenu?.let { onExportPdf(it) }
                        selectedEntryForMenu = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Share Text") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        selectedEntryForMenu?.let { onShareEntry(it) }
                        selectedEntryForMenu = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        selectedEntryForMenu?.let { onDeleteEntry(it) }
                        selectedEntryForMenu = null
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
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
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "What will you notice today?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
