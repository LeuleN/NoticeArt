package com.example.userflowdemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EntryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = EntryDatabase.getDatabase(application)
    private val repository = EntryRepository(database.entryDao())

    val entries: StateFlow<List<Entry>> =
        repository.allEntries
            .map { list -> list.filter { !it.isDraft } }
            .stateIn(
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

    private fun loadDraft() {
        viewModelScope.launch {
            _draft.value = repository.getDraft()
        }
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
                    updatedMedia.add(MediaItem(imageUri = uri, colors = colors, textures = textures))
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
