package com.example.userflowdemo

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.userflowdemo.utils.AiColorService
import com.example.userflowdemo.utils.ImageUtils
import com.example.userflowdemo.utils.TextureDetectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val colors: List<Int>) : AiState()
    data class Error(val message: String) : AiState()
}

sealed class TextureDetectionState {
    object Idle : TextureDetectionState()
    object Loading : TextureDetectionState()
    data class Success(val suggestedTextures: List<Uri>) : TextureDetectionState()
    data class Error(val message: String) : TextureDetectionState()
}



class EntryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = EntryDatabase.getDatabase(application)
    private val repository = EntryRepository(database.entryDao())
    private val aiService = AiColorService()
    private val textureService = TextureDetectionService()

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState

    private val _textureState = MutableStateFlow<TextureDetectionState>(TextureDetectionState.Idle)
    val textureState: StateFlow<TextureDetectionState> = _textureState

    // User-controlled texture detection count
    private val _textureCount = MutableStateFlow(6)
    val textureCount: StateFlow<Int> = _textureCount

    // User-controlled color detection count
    private val _colorCount = MutableStateFlow(6)
    val colorCount: StateFlow<Int> = _colorCount

    private val _sortOption = MutableStateFlow(EntrySortOption.LAST_MODIFIED)
    val sortOption: StateFlow<EntrySortOption> = _sortOption.asStateFlow()

    val entries: StateFlow<List<Entry>> =
        combine(repository.allEntries, _sortOption) { list, sortOption ->
            val nonDraftEntries = list.filter { !it.isDraft }

            when (sortOption) {
                EntrySortOption.LAST_MODIFIED ->
                    nonDraftEntries.sortedByDescending { it.timestamp }

                EntrySortOption.TITLE_ASC ->
                    nonDraftEntries.sortedBy { it.title.trim().lowercase() }

                EntrySortOption.TITLE_DESC ->
                    nonDraftEntries.sortedByDescending { it.title.trim().lowercase() }

                EntrySortOption.FAVORITES_FIRST ->
                    nonDraftEntries.sortedWith(
                        compareByDescending<Entry> { it.isFavorite }
                            .thenByDescending { it.timestamp }
                    )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _draft = MutableStateFlow<Entry?>(null)
    val draft: StateFlow<Entry?> = _draft

    val mediaItems: StateFlow<List<MediaItem>> = _draft
        .map { it?.media ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var editingOriginalId: Long? = null
    private var originalEntrySnapshot: Entry? = null

    val isEditing: Boolean get() = editingOriginalId != null

    init {
        loadDraft()
    }

    fun setSortOption(option: EntrySortOption) {
        _sortOption.value = option
    }

    private fun loadDraft() {
        viewModelScope.launch {
            _draft.value = repository.getDraft()
        }
    }

    fun setTextureCount(count: Int) {
        _textureCount.value = count.coerceIn(4, 15)
    }

    fun setColorCount(count: Int) {
        _colorCount.value = count.coerceIn(4, 10)
    }

    fun suggestColorsForImage(context: Context, uriString: String) {
        viewModelScope.launch {
            val count = _colorCount.value
            _aiState.value = AiState.Loading
            try {
                val uri = uriString.toUri()
                val bitmap = loadBitmapFromUri(context, uri)

                if (bitmap != null) {
                    val colors = aiService.suggestColors(bitmap, count)
                    _aiState.value = AiState.Success(colors)
                } else {
                    _aiState.value = AiState.Error("Could not load image")
                }
            } catch (e: Exception) {
                _aiState.value = AiState.Error(e.message ?: "AI Suggestion failed")
            }
        }
    }

    fun detectTextures(context: Context, uriString: String) {
        viewModelScope.launch {
            val count = _textureCount.value
            _textureState.value = TextureDetectionState.Loading
            try {
                val uri = uriString.toUri()
                val bitmap = loadBitmapFromUri(context, uri)

                if (bitmap != null) {
                    val rects = withContext(Dispatchers.Default) {
                        textureService.getProminentTextureAreas(
                            bitmap = bitmap,
                            textureCount = count
                        )
                    }
                    val croppedBitmaps = textureService.cropTextureBitmaps(bitmap, rects)
                    val uris = croppedBitmaps.mapNotNull {
                        ImageUtils.saveBitmapToInternalStorage(context, it)
                    }
                    _textureState.value = TextureDetectionState.Success(uris)
                } else {
                    _textureState.value = TextureDetectionState.Error("Could not load image")
                }
            } catch (e: Exception) {
                _textureState.value = TextureDetectionState.Error(e.message ?: "Texture detection failed")
            }
        }
    }

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun resetAiState() {
        _aiState.value = AiState.Idle
    }

    fun resetTextureState() {
        _textureState.value = TextureDetectionState.Idle
    }

    fun createDraft(
        title: String = "",
        observation: String? = null,
        media: List<MediaItem> = emptyList(),
        audioUris: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            deleteDraftInternal()
            val draftEntry = Entry(
                title = title,
                observation = observation,
                media = media,
                audioUris = audioUris,
                isDraft = true
            )
            repository.insert(draftEntry)
            loadDraft()
            editingOriginalId = null
            originalEntrySnapshot = null
        }
    }

    fun startEditing(entry: Entry) {
        viewModelScope.launch {
            deleteDraftInternal()
            val editingEntry = entry.copy(isDraft = true)
            repository.update(editingEntry)
            _draft.value = editingEntry
            editingOriginalId = entry.id
            originalEntrySnapshot = entry.copy(isDraft = false)
        }
    }

    fun updateDraft(title: String) {
        viewModelScope.launch {
            _draft.value?.let {
                val updated = it.copy(title = title)
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun updateObservation(observation: String) {
        viewModelScope.launch {
            _draft.value?.let {
                val updated = it.copy(observation = observation)
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun addOrUpdateMediaItem(uri: String, colors: List<Int>, textures: List<Texture> = emptyList(), index: Int? = null, mediaId: String? = null) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.toMutableList()
                val newItem = MediaItem(imageUri = uri, colors = colors)

                if (index != null && index in updatedMedia.indices) {
                    val existingItem = updatedMedia[index]
                    updatedMedia[index] = existingItem.copy(imageUri = uri, colors = colors, textures = textures)
                } else if (mediaId != null) {
                    val existingIndex = updatedMedia.indexOfFirst { it.id == mediaId }
                    if (existingIndex != -1) {
                        updatedMedia[existingIndex] = updatedMedia[existingIndex].copy(imageUri = uri, colors = colors, textures = textures)
                    } else {
                        updatedMedia.add(MediaItem(id = mediaId, imageUri = uri, colors = colors, textures = textures))
                    }
                } else {
                    updatedMedia.add(MediaItem(id = mediaId ?: java.util.UUID.randomUUID().toString(), imageUri = uri, colors = colors, textures = textures))
                }

                val updated = draft.copy(media = updatedMedia)
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun addTextureToImage(mediaId: String, texture: Texture) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.map { item ->
                    if (item.id == mediaId) {
                        // Check if texture with same imageUri already exists
                        val duplicate = item.textures.any { it.imageUri == texture.imageUri }
                        if (duplicate) return@map item

                        val exists = item.textures.any { it.id == texture.id }
                        val newTextures = if (exists) {
                            item.textures.map { if (it.id == texture.id) texture else it }
                        } else {
                            item.textures + texture
                        }
                        item.copy(textures = newTextures)
                    } else item
                }
                val updated = draft.copy(media = updatedMedia)
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun addAllDetectedTextures(mediaId: String) {
        val currentState = _textureState.value
        if (currentState is TextureDetectionState.Success) {
            viewModelScope.launch {
                _draft.value?.let { draft ->
                    val mediaItem = draft.media.find { it.id == mediaId } ?: return@launch
                    val currentUris = mediaItem.textures.mapNotNull { it.imageUri }.toSet()

                    val filteredSuggestions = currentState.suggestedTextures.filter { it.toString() !in currentUris }
                    if (filteredSuggestions.isEmpty()) return@launch

                    val newTextures = filteredSuggestions.mapIndexed { index, uri ->
                        Texture(
                            imageUri = uri.toString(),
                            name = "Auto Texture ${mediaItem.textures.size + index + 1}"
                        )
                    }

                    val updatedMedia = draft.media.map { item ->
                        if (item.id == mediaId) {
                            item.copy(textures = item.textures + newTextures)
                        } else item
                    }
                    val updated = draft.copy(media = updatedMedia)
                    _draft.value = updated
                    repository.update(updated)
                }
            }
        }
    }

    fun renameTexture(mediaId: String, textureId: String, newName: String) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.map { item ->
                    if (item.id == mediaId) {
                        val updatedTextures = item.textures.map { texture ->
                            if (texture.id == textureId) {
                                texture.copy(name = newName, isCustomName = true)
                            } else texture
                        }
                        item.copy(textures = updatedTextures)
                    } else item
                }
                val updated = draft.copy(media = updatedMedia)
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun removeTextureFromImage(mediaId: String, textureId: String) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.map { item ->
                    if (item.id == mediaId) {
                        item.copy(textures = item.textures.filter { it.id != textureId })
                    } else item
                }
                val updated = draft.copy(media = updatedMedia)
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun updateTextures(mediaIndex: Int, textures: List<Texture>) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.toMutableList()
                if (mediaIndex in updatedMedia.indices) {
                    val currentItem = updatedMedia[mediaIndex]
                    updatedMedia[mediaIndex] = currentItem.copy(textures = textures)
                    val updated = draft.copy(media = updatedMedia)
                    _draft.value = updated
                    repository.update(updated)
                }
            }
        }
    }

    fun removeMediaItem(index: Int) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.toMutableList()
                if (index in updatedMedia.indices) {
                    updatedMedia.removeAt(index)
                    val updated = draft.copy(media = updatedMedia)
                    _draft.value = updated
                    repository.update(updated)
                }
            }
        }
    }

    fun removeImage(mediaId: String) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.filter { it.id != mediaId }
                val updated = draft.copy(media = updatedMedia)
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun openColorCapture(mediaId: String) {
        // Navigation handled in EntryApp
    }

    fun openTextureCapture(mediaId: String) {
        // Navigation handled in EntryApp
    }

    fun addAudioUri(uri: String) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                if (uri !in draft.audioUris) {
                    val updated = draft.copy(audioUris = draft.audioUris + uri)
                    _draft.value = updated
                    repository.update(updated)
                }
            }
        }
    }

    fun removeAudioUri(uri: String) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updated = draft.copy(audioUris = draft.audioUris.filter { it != uri })
                _draft.value = updated
                repository.update(updated)
            }
        }
    }

    fun autoSave() {
        viewModelScope.launch {
            _draft.value?.let { currentDraft ->
                if (isEditing) {
                    val updated = currentDraft.copy(
                        isDraft = false,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.update(updated)
                } else {
                    repository.update(currentDraft)
                }
            }
        }
    }

    fun publishDraft() {
        viewModelScope.launch {
            _draft.value?.let { currentDraft ->
                val published = currentDraft.copy(
                    isDraft = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.update(published)
                _draft.value = null
                editingOriginalId = null
                originalEntrySnapshot = null
            }
        }
    }

    fun discardDraft() {
        viewModelScope.launch {
            _draft.value?.let { currentDraft ->
                if (isEditing && originalEntrySnapshot != null) {
                    repository.update(originalEntrySnapshot!!)
                } else {
                    repository.delete(currentDraft)
                }
                _draft.value = null
                editingOriginalId = null
                originalEntrySnapshot = null
            }
        }
    }

    fun toggleFavorite(entry: Entry) {
        viewModelScope.launch {
            repository.update(
                entry.copy(isFavorite = !entry.isFavorite)
            )
        }
    }

    @Suppress("UNUSED")
    fun restoreDraft(entry: Entry) {
        viewModelScope.launch {
            repository.insert(entry)
            loadDraft()
        }
    }

    fun deleteDraft() {
        viewModelScope.launch {
            deleteDraftInternal()
        }
    }

    private suspend fun deleteDraftInternal() {
        val existingDraft = repository.getDraft()
        if (existingDraft != null) {
            repository.delete(existingDraft)
        }
        _draft.value = null
        editingOriginalId = null
        originalEntrySnapshot = null
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }

    fun insertEntry(entry: Entry) {
        viewModelScope.launch {
            repository.insert(entry)
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            repository.update(entry)
        }
    }
}