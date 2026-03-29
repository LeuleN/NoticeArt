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

    // 3. ViewModel MUST distinguish modes
    private var editingOriginalId: Long? = null
    private var originalEntrySnapshot: Entry? = null // For isolated revert on discard

    val isEditing: Boolean get() = editingOriginalId != null

    init {
        loadDraft()
    }

    private fun loadDraft() {
        viewModelScope.launch {
            _draft.value = repository.getDraft()
        }
    }

    fun createDraft(title: String = "", observation: String? = null, imageUri: String? = null, color: Int? = null) {
        viewModelScope.launch {
            deleteDraftInternal()
            val draftEntry = Entry(
                title = title,
                observation = observation,
                imageUri = imageUri,
                color = color,
                isDraft = true
            )
            repository.insert(draftEntry)
            loadDraft()
            editingOriginalId = null
            originalEntrySnapshot = null
        }
    }

    // 1. Editing MUST preserve original ID
    fun startEditing(entry: Entry) {
        viewModelScope.launch {
            // 6. CLEANUP: Ensure no other draft exists
            deleteDraftInternal()
            
            // Mark the ORIGINAL entry as a draft to isolate it from the main list
            val editingEntry = entry.copy(isDraft = true)
            repository.update(editingEntry)
            
            _draft.value = editingEntry
            editingOriginalId = entry.id
            // Take a snapshot of the entry as it was before editing for discard/revert
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

    fun attachImage(uri: String) {
        viewModelScope.launch {
            _draft.value?.let {
                val updated = it.copy(imageUri = uri, color = null)
                repository.update(updated)
                _draft.value = updated
            }
        }
    }

    fun updateColor(color: Int) {
        viewModelScope.launch {
            _draft.value?.let {
                val updated = it.copy(color = color)
                repository.update(updated)
                _draft.value = updated
            }
        }
    }

    // ✅ FIXED: Update timestamp during auto-save for edits
    fun autoSave() {
        viewModelScope.launch {
            _draft.value?.let { currentDraft ->
                if (isEditing) {
                    // CASE A — Editing Existing Entry: Commit changes immediately on app close
                    // Updated to include timestamp refresh so it reflects "last modified"
                    val updated = currentDraft.copy(
                        isDraft = false,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.update(updated)
                } else {
                    // CASE B — Creating New Entry (Draft): Keep it as a draft
                    repository.update(currentDraft)
                }
            }
        }
    }

    // ✅ Manual Save: Updates timestamp and commits the draft
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
                    // Revert to original state using snapshot
                    repository.update(originalEntrySnapshot!!)
                } else {
                    // New entry: delete the draft row
                    repository.delete(currentDraft)
                }
                _draft.value = null
                editingOriginalId = null
                originalEntrySnapshot = null
            }
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
