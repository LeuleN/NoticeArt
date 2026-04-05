package com.example.userflowdemo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.userflowdemo.EntryViewModel
import com.example.userflowdemo.Texture
import com.example.userflowdemo.TextureDetectionState

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
    val mediaItems by viewModel.mediaItems.collectAsState()
    val mediaItem = remember(mediaItems, mediaId) { mediaItems.find { it.id == mediaId } }
    val textures = mediaItem?.textures ?: emptyList()
    
    val textureState by viewModel.textureState.collectAsState()

    // Ordering logic: Custom-named textures FIRST, then default textures
    val sortedTextures = remember(textures) {
        val (custom, default) = textures.partition { it.isCustomName }
        custom.sortedBy { it.name } + default.sortedBy { it.name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Texture Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

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
                        IconButton(onClick = { viewModel.resetTextureState() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Suggestions", modifier = Modifier.size(16.dp))
                        }
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestions) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val newTexture = Texture(
                                            imageUri = uri.toString(),
                                            name = "Auto Texture ${textures.size + 1}"
                                        )
                                        viewModel.addTextureToImage(mediaId, newTexture)
                                    }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Suggested Texture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { onAddTexture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Texture")
                    }
                }

                items(sortedTextures, key = { it.id }) { texture ->
                    TextureItem(
                        texture = texture,
                        onDelete = {
                            viewModel.removeTextureFromImage(mediaId, texture.id)
                        },
                        onClick = { onEditTexture(texture) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            IconButton(
                onClick = onConfirm,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Confirm All",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun TextureItem(
    texture: Texture,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(modifier = Modifier.size(80.dp)) {
            AsyncImage(
                model = texture.imageUri,
                contentDescription = texture.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { onClick() }
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = texture.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
        )
    }
}
