package com.example.userflowdemo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.userflowdemo.EntryViewModel
import com.example.userflowdemo.Texture
import com.example.userflowdemo.TextureDetectionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextureCaptureScreen(
    viewModel: EntryViewModel,
    mediaId: String,
    imageUri: String,
    onAddTexture: () -> Unit,
    onEditTexture: (Texture) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val mediaItems by viewModel.mediaItems.collectAsState()
    val mediaItem = remember(mediaItems, mediaId) { mediaItems.find { it.id == mediaId } }
    val textures = mediaItem?.textures ?: emptyList()
    
    val textureState by viewModel.textureState.collectAsState()
    val textureDetectionCount by viewModel.textureDetectionCount.collectAsState()

    val listState = rememberLazyListState()
    var textureToRename by remember { mutableStateOf<Texture?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Textures?") },
            text = { Text("This will remove all detected and manual textures for this image.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllTextures(mediaId)
                    showClearConfirm = false
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fix: Reset detection state ONLY when the image URI actually changes
    LaunchedEffect(imageUri) {
        viewModel.resetTextureState()
    }

    if (textureToRename != null) {
        RenameDialog(
            initialName = textureToRename!!.name,
            onDismiss = { textureToRename = null },
            onSave = { newName ->
                viewModel.renameTexture(mediaId, textureToRename!!.id, newName)
                textureToRename = null
            }
        )
    }

    // Auto-scroll logic removed

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Texture Capture",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onConfirm) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                
                if (textureState is TextureDetectionState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detection Results / Suggestions
            if (textureState is TextureDetectionState.Success) {
                val suggestions = (textureState as TextureDetectionState.Success).suggestedTextures
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detected Textures",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { 
                                    val added = viewModel.addAllDetectedTextures(mediaId)
                                    scope.launch {
                                        if (added > 0) {
                                            snackbarHostState.showSnackbar("Added $added textures")
                                        } else {
                                            snackbarHostState.showSnackbar("All textures already added")
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Add All", style = MaterialTheme.typography.labelLarge)
                            }
                            IconButton(onClick = { viewModel.resetTextureState() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Suggestions", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestions) { uri ->
                            val isAlreadyAdded = remember(textures, uri) {
                                textures.any { it.imageUri == uri.toString() }
                            }
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isAlreadyAdded) 3.dp else 1.dp,
                                        color = if (isAlreadyAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(!isAlreadyAdded) {
                                        val (name, index) = viewModel.getNextTextureInfo(mediaId, isAuto = true)
                                        val newTexture = Texture(
                                            imageUri = uri.toString(),
                                            name = name,
                                            autoIndex = index
                                        )
                                        viewModel.addTextureToImage(mediaId, newTexture)
                                    }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Suggested Texture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().alpha(if (isAlreadyAdded) 0.6f else 1f)
                                )
                                if (isAlreadyAdded) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Added",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .padding(2.dp)
                                            .size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                            .padding(4.dp)
                                            .size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = { viewModel.detectTextures(context, imageUri) },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("notice textures", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Color Count Stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setTextureDetectionCount(textureDetectionCount - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                            }
                            
                            Text(
                                text = textureDetectionCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            
                            IconButton(
                                onClick = { viewModel.setTextureDetectionCount(textureDetectionCount + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Clear All", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { onAddTexture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Texture")
                    }
                }

                itemsIndexed(textures, key = { _, texture -> texture.id }) { index, texture ->
                    Box(
                        modifier = Modifier.animateItem()
                    ) {
                        TextureItem(
                            texture = texture,
                            onDelete = {
                                viewModel.removeTextureFromImage(mediaId, texture.id)
                            },
                            onRename = { textureToRename = texture },
                            onEditCrop = { onEditTexture(texture) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TextureItem(
    texture: Texture,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onEditCrop: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(modifier = Modifier.size(70.dp)) {
            AsyncImage(
                model = texture.imageUri,
                contentDescription = texture.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { onRename() }
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(16.dp))
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    // Disable Edit Crop for auto-generated textures (system-generated)
                    if (texture.autoIndex == null) {
                        DropdownMenuItem(
                            text = { Text("Edit Crop") },
                            onClick = {
                                showMenu = false
                                onEditCrop()
                            },
                            leadingIcon = { Icon(Icons.Default.Crop, contentDescription = null) }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
        Text(
            text = texture.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
        )
    }
}

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Texture") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
