package com.example.userflowdemo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.Entry
import com.example.userflowdemo.components.DraggableScrollbar

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

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete entry?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        // 1. Scrollable Content Area with Draggable Scrollbar
        val mainScrollState = rememberScrollState()
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(mainScrollState)
                    .padding(end = 12.dp) // Space for draggable thumb
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

                if (!entry.observation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Observations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 2. Internally Scrollable Observation Box with Draggable Scrollbar
                    val obsScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(obsScrollState)
                                .padding(end = 12.dp) // Space for draggable thumb
                        ) {
                            Text(
                                text = entry.observation,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        DraggableScrollbar(
                            scrollState = obsScrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 2.dp, top = 4.dp, bottom = 4.dp),
                            thumbHeight = 40.dp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            DraggableScrollbar(
                scrollState = mainScrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
                thumbHeight = 80.dp
            )
        }

        // 3. Fixed Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier.clickable { showDeleteDialog = true }
                )
            }
        }
    }
}