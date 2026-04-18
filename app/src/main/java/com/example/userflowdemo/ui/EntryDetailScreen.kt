package com.example.userflowdemo.ui

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.userflowdemo.Entry
import com.example.userflowdemo.MediaItem
import com.example.userflowdemo.components.DraggableScrollbar
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entry: Entry,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onExportPdf: (Entry) -> Unit
) {
    val formattedTime = java.text.SimpleDateFormat(
        "MMM dd, yyyy hh:mm a",
        Locale.getDefault()
    ).format(Date(entry.timestamp))

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
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

    selectedMediaItem?.let { mediaItem ->
        ModalBottomSheet(
            onDismissRequest = { selectedMediaItem = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
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

                if (mediaItem.textures.isNotEmpty()) {
                    Text(
                        text = "Captured Textures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val prioritizedTextures = remember(mediaItem.textures) {
                        mediaItem.textures.sortedBy { it.name.startsWith("Auto Texture") }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(prioritizedTextures) { texture ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(texture.imageUri),
                                        contentDescription = texture.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = texture.name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                if (mediaItem.colors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Palette Colors",
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
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Entry Detail",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (entry.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = if (entry.isFavorite) {
                                "Remove favorite"
                            } else {
                                "Add favorite"
                            },
                            tint = if (entry.isFavorite) {
                                Color.Red
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            val mainScrollState = rememberScrollState()
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(mainScrollState)
                        .padding(end = 12.dp)
                ) {
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

                                if (mediaItem.textures.isNotEmpty()) {
                                    val prioritizedTextures = remember(mediaItem.textures) {
                                        mediaItem.textures.sortedBy { it.name.startsWith("Auto Texture") }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.25f)
                                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                    ) {
                                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp, color = Color.Black)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        ) {
                                            val texturesToShow = prioritizedTextures.take(3)
                                            texturesToShow.forEachIndexed { index, texture ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                ) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(texture.imageUri),
                                                        contentDescription = texture.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    if (index != texturesToShow.lastIndex) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.CenterEnd)
                                                                .width(2.dp)
                                                                .fillMaxHeight()
                                                                .background(Color.Black)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (mediaItem.colors.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.25f)
                                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                    ) {
                                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp, color = Color.Black)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        ) {
                                            val colorsToShow = mediaItem.colors.take(3)
                                            colorsToShow.forEachIndexed { index, colorInt ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(Color(colorInt))
                                                ) {
                                                    if (index != colorsToShow.lastIndex) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.CenterEnd)
                                                                .width(2.dp)
                                                                .fillMaxHeight()
                                                                .background(Color.Black)
                                                        )
                                                    }
                                                }
                                            }
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
                                            contentDescription = if (playingAudioIndex == index) {
                                                "Pause audio"
                                            } else {
                                                "Play audio"
                                            }
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


                        val obsScrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(obsScrollState)
                                    .padding(end = 12.dp)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onExportPdf(entry) }) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Export PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}
