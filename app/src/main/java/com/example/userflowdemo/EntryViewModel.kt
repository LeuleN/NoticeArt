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

    fun createDraft(title: String = "", observation: String? = null, media: List<MediaItem> = emptyList()) {
        viewModelScope.launch {
            deleteDraftInternal()
            val draftEntry = Entry(
                title = title,
                observation = observation,
                media = media,
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
                repository.update(updated)
                _draft.value = updated
            }
        }
    }

    fun updateObservation(observation: String) {
        viewModelScope.launch {
            _draft.value?.let {
                val updated = it.copy(observation = observation)
                repository.update(updated)
                _draft.value = updated
            }
        }
    }

    fun addOrUpdateMediaItem(uri: String, colors: List<Int>, index: Int? = null) {
        viewModelScope.launch {
            _draft.value?.let { draft ->
                val updatedMedia = draft.media.toMutableList()
                val newItem = MediaItem(imageUri = uri, colors = colors)
                
                if (index != null && index in updatedMedia.indices) {
                    updatedMedia[index] = newItem
                } else {
                    updatedMedia.add(newItem)
                }
                
                val updated = draft.copy(media = updatedMedia)
                repository.update(updated)
                _draft.value = updated
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
