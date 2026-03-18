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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.userflowdemo.ui.theme.UserFlowDemoTheme
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect

// Import for Room
import androidx.room.Entity
import androidx.room.PrimaryKey
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

@Entity
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDraft: Boolean = false
)

@Composable
fun EntryApp(
    viewModel: EntryViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    var selectedEntry by rememberSaveable { mutableStateOf<Entry?>(null) }

    val entries by viewModel.entries.collectAsState()
    val draft by viewModel.draft.collectAsState()

    if (currentScreen == "home") {
        HomeScreen(
            entries = entries,
            draft = draft,
            onAddClick = {
                if (draft == null) {
                    viewModel.createDraft()
                }
                currentScreen = "newEntry"
            },
            onDraftClick = {
                currentScreen = "newEntry"
            },
            onEntryClick = { entry ->
                selectedEntry = entry
                currentScreen = "detail"
            }
        )
    } else if (currentScreen == "newEntry") {
        NewEntryScreen(
            draft = draft,
            onTitleChange = { viewModel.updateDraft(it) },
            onPublish = {
                viewModel.publishDraft()
                currentScreen = "home"
            },
            onBack = { currentScreen = "home" },
            onDeleteDraft = {
                viewModel.deleteDraft()
                currentScreen = "home"
            }
        )
    } else if (currentScreen == "detail") {
        selectedEntry?.let {
            EntryDetailScreen(
                entry = it,
                onBack = { currentScreen = "home" },
                onDelete = {
                    viewModel.deleteEntry(it)
                    currentScreen = "home"
                },
                onEdit = {
                    viewModel.loadEntryAsDraft(it)
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

                    Text(
                        text = "• ${entry.title}",
                        modifier = Modifier.clickable {
                            onEntryClick(entry)
                        }
                    )
                    Text(formattedTime)
                }
        }
    }
}

@Composable
fun NewEntryScreen(
    draft: Entry?,
    onTitleChange: (String) -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    onDeleteDraft: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(draft?.title ?: "") }
    var showError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(draft) {
        title = draft?.title ?: ""
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
                label = { Text("Title") }
            )

            if (showError) {
                Text(
                    text = "Title required",
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Back",
                modifier = Modifier.clickable {
                    onBack()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Discard Draft",
                color = Color.Red,
                modifier = Modifier.clickable {
                    onDeleteDraft()
                }
            )
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
        modifier = Modifier.padding(16.dp)
    ) {

        Text("Entry Detail")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Title: ${entry.title}")
        Text("Date: $formattedTime")

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
