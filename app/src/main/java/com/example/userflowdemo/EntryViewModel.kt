package com.example.userflowdemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EntryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = EntryDatabase.getDatabase(application)
    private val repository = EntryRepository(database.entryDao())

    // 5. ViewModel RULE: Separate state. Home screen filters out drafts.
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

    // Track original entry being edited for isolated saving
    private var editingOriginalId: Long? = null

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
            deleteDraftInternal() // Ensure clean start
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
        }
    }

    fun startEditing(entry: Entry) {
        viewModelScope.launch {
            deleteDraftInternal()
            // 1. NEVER mutate originalEntry. Use an ISOLATED COPY.
            // Create a temporary draft entry in DB with a NEW ID.
            val editingDraft = entry.copy(id = 0, isDraft = true)
            repository.insert(editingDraft)
            loadDraft()
            editingOriginalId = entry.id
        }
    }

    // Isolated updates to the current draft/editing state
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

    // 4. On Save: Update real entry from draft or publish new
    fun publishDraft() {
        viewModelScope.launch {
            _draft.value?.let { currentDraft ->
                if (editingOriginalId != null) {
                    // Update original entry with draft data
                    val updatedOriginal = currentDraft.copy(
                        id = editingOriginalId!!,
                        isDraft = false,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.update(updatedOriginal)
                    repository.delete(currentDraft)
                } else {
                    // New entry: make it permanent
                    val published = currentDraft.copy(
                        isDraft = false,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.update(published)
                }
                _draft.value = null
                editingOriginalId = null
            }
        }
    }

    // 3. On Discard: Delete editingEntry / draft
    fun discardDraft() {
        viewModelScope.launch {
            _draft.value?.let {
                repository.delete(it)
                _draft.value = null
                editingOriginalId = null
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

    // Note: updateEntry is still used for direct updates if needed, 
    // but NewEntryScreen now uses the draft flow.
    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            repository.update(entry)
        }
    }
}
