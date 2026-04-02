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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.Entry
import com.example.userflowdemo.MediaItem
import com.example.userflowdemo.components.DraggableScrollbar
import android.media.MediaPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    // skipPartiallyExpanded ensures the sheet opens to its full content height immediately
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingAudioIndex by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

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

    // Use null-safe check to prevent crashes during dismissal
    selectedMediaItem?.let { mediaItem ->
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItem = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp), // Tightened bottom space
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(mediaItem.imageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Extracted Colors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mediaItem.colors) { colorInt ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "#%06X".format(0xFFFFFF and colorInt),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    entry.media.forEach { mediaItem ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clickable { selectedMediaItem = mediaItem }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(mediaItem.imageUri),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (mediaItem.colors.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .align(Alignment.BottomCenter)
                                    ) {
                                        mediaItem.colors.take(3).forEach { colorInt ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(Color(colorInt))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (entry.audioUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Audio Clips",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        entry.audioUris.forEachIndexed { index, audioUri ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Audio ${index + 1}")

                                    IconButton(
                                        onClick = {
                                            if (playingAudioIndex == index) {
                                                mediaPlayer?.pause()
                                                playingAudioIndex = null
                                            } else {
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                mediaPlayer = null

                                                val player = MediaPlayer().apply {
                                                    setDataSource(context, android.net.Uri.parse(audioUri))
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener {
                                                        playingAudioIndex = null
                                                        mediaPlayer?.release()
                                                        mediaPlayer = null
                                                    }
                                                }

                                                mediaPlayer = player
                                                playingAudioIndex = index
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (playingAudioIndex == index) {
                                                Icons.Default.Pause
                                            } else {
                                                Icons.Default.PlayArrow
                                            },
                                            contentDescription = if (playingAudioIndex == index) "Pause audio" else "Play audio"
                                        )
                                    }
                                }
                            }
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
}
