package com.example.userflowdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.userflowdemo.EntrySortOption
import com.example.userflowdemo.components.AddCard
import com.example.userflowdemo.components.DraftCard
import com.example.userflowdemo.components.EntryCard
import com.example.userflowdemo.components.GridScrollbar

@Composable
fun HomeScreen(
    userName: String,
    entries: List<Entry>,
    draft: Entry?,
    sortOption: EntrySortOption,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onDraftClick: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onSortOptionChange: (EntrySortOption) -> Unit
) {
    var showDraftDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

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
            HomeHeader(
                userName = userName,
                selectedSortOption = sortOption,
                onSortOptionChange = onSortOptionChange
            )

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
                    item {
                        AddCard(onAddClick = {
                            if (draft == null) {
                                onAddClick()
                            } else {
                                showDraftDialog = true
                            }
                        })
                    }

                    if (draft != null) {
                        item {
                            DraftCard(draft = draft, onDraftClick = onDraftClick)
                        }
                    }

                    items(entries) { entry ->
                        EntryCard(
                            entry = entry,
                            onClick = { onEntryClick(entry) }
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
}

@Composable
fun HomeHeader(
    userName: String,
    selectedSortOption: EntrySortOption,
    onSortOptionChange: (EntrySortOption) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
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
            Text(
                text = "Sorted by: ${selectedSortOption.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Sort entries"
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                EntrySortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSortOptionChange(option)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
    }
}