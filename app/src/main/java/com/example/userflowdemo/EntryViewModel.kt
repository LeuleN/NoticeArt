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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val _textureDetectionCount = MutableStateFlow(6)
    val textureDetectionCount: StateFlow<Int> = _textureDetectionCount

    // User-controlled color detection count
    private val _colorDetectionCount = MutableStateFlow(6)
    val colorDetectionCount: StateFlow<Int> = _colorDetectionCount

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
    private val draftMutex = Mutex()

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

    fun setTextureDetectionCount(count: Int) {
        _textureDetectionCount.value = count.coerceIn(4, 15)
    }

    fun setColorDetectionCount(count: Int) {
        _colorDetectionCount.value = count.coerceIn(4, 10)
    }

    fun suggestColorsForImage(context: Context, uriString: String) {
        viewModelScope.launch {
            val count = _colorDetectionCount.value
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
            val count = _textureDetectionCount.value
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
            // DO NOT insert into repository here. Only keep in memory.
            _draft.value = draftEntry
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
        _draft.update { it?.copy(title = title) }
        viewModelScope.launch {
            _draft.value?.let { persistDraftInternal(it) }
        }
    }

    fun updateObservation(observation: String) {
        _draft.update { it?.copy(observation = observation) }
        viewModelScope.launch {
            _draft.value?.let { persistDraftInternal(it) }
        }
    }

    private fun persistDraft() {
        viewModelScope.launch {
            _draft.value?.let { persistDraftInternal(it) }
        }
    }

    fun updateColors(uri: String, colors: List<Int>, index: Int? = null, mediaId: String? = null) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.toMutableList()
                if (index != null && index in updatedMedia.indices) {
                    val existingItem = updatedMedia[index]
                    updatedMedia[index] = existingItem.copy(colors = colors)
                } else if (mediaId != null) {
                    val existingIndex = updatedMedia.indexOfFirst { it.id == mediaId }
                    if (existingIndex != -1) {
                        updatedMedia[existingIndex] = updatedMedia[existingIndex].copy(colors = colors)
                    } else {
                        updatedMedia.add(MediaItem(id = mediaId, imageUri = uri, colors = colors))
                    }
                } else {
                    updatedMedia.add(MediaItem(imageUri = uri, colors = colors))
                }

                val updated = draft.copy(media = updatedMedia)
                _draft.value = updated
                persistDraftInternal(updated)
            }
        }
    }

    fun addOrUpdateMediaItem(uri: String, colors: List<Int>, textures: List<Texture> = emptyList(), index: Int? = null, mediaId: String? = null) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.toMutableList()
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
                persistDraftInternal(updated)
            }
        }
    }

    private suspend fun persistDraftInternal(entry: Entry) {
        draftMutex.withLock {
            if (isEditing || !com.example.userflowdemo.utils.isDraftEmpty(entry)) {
                val existingDraft = repository.getDraft()
                if (existingDraft == null && !isEditing) {
                    val newId = repository.insert(entry)
                    _draft.update { if (it != null && it.id == 0L) it.copy(id = newId) else it }
                } else {
                    val idToUse = if (entry.id == 0L && existingDraft != null) existingDraft.id else entry.id
                    val toUpdate = entry.copy(id = idToUse)
                    repository.update(toUpdate)
                    if (entry.id == 0L && existingDraft != null) {
                        _draft.update { if (it != null && it.id == 0L) it.copy(id = idToUse) else it }
                    }
                }
            }
        }
    }

    fun addTextureToImage(mediaId: String, texture: Texture) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.map { item ->
                    if (item.id == mediaId) {
                        // Check for duplicate imageUri (ignore nulls which are placeholders)
                        if (texture.imageUri != null && item.textures.any { it.imageUri == texture.imageUri }) {
                            return@map item
                        }

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
                persistDraftInternal(updated)
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

                    // Ensure unique textures and filter out already added ones
                    val uniqueSuggestions = currentState.suggestedTextures
                        .map { it.toString() }
                        .distinct()
                        .filter { it !in currentUris }

                    if (uniqueSuggestions.isEmpty()) return@launch

                    // Avoid duplicate names by finding the next available Auto Texture index
                    val existingAutoIndices = mediaItem.textures
                        .mapNotNull { it.name.removePrefix("Auto Texture ").toIntOrNull() }
                    var nextIndex = (existingAutoIndices.maxOrNull() ?: 0) + 1

                    val newTextures = uniqueSuggestions.map { uriString ->
                        Texture(
                            imageUri = uriString,
                            name = "Auto Texture ${nextIndex++}"
                        )
                    }

                    val updatedMedia = draft.media.map { item ->
                        if (item.id == mediaId) {
                            item.copy(textures = item.textures + newTextures)
                        } else item
                    }
                    val updated = draft.copy(media = updatedMedia)
                    _draft.value = updated
                    persistDraftInternal(updated)
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
                persistDraftInternal(updated)
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
                persistDraftInternal(updated)
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
                    persistDraftInternal(updated)
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
                    persistDraftInternal(updated)
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
                persistDraftInternal(updated)
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
                    persistDraftInternal(updated)
                }
            }
        }
    }

    fun removeAudioUri(uri: String) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updated = draft.copy(audioUris = draft.audioUris.filter { it != uri })
                _draft.value = updated
                persistDraftInternal(updated)
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
                    // Only auto-save if the draft is not empty
                    if (!com.example.userflowdemo.utils.isDraftEmpty(currentDraft)) {
                        val existingDraft = repository.getDraft()
                        if (existingDraft == null) {
                            repository.insert(currentDraft)
                        } else {
                            repository.update(currentDraft)
                        }
                    } else {
                        // If it's empty, ensure it's NOT in the database
                        deleteDraftInternal()
                        // But keep the in-memory draft so the UI doesn't clear
                        _draft.value = currentDraft
                    }
                }
            }
        }
    }

    fun publishDraft() {
        viewModelScope.launch {
            _draft.value?.let { currentDraft ->
                if (com.example.userflowdemo.utils.isDraftEmpty(currentDraft) && !isEditing) return@launch

                val published = currentDraft.copy(
                    isDraft = false,
                    timestamp = System.currentTimeMillis()
                )

                if (isEditing) {
                    repository.update(published)
                } else {
                    val existingDraft = repository.getDraft()
                    if (existingDraft == null) {
                        repository.insert(published) // INSERT if new
                    } else {
                        repository.update(published) // UPDATE if existing draft
                    }
                }

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