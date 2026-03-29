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
        repository.allEntries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _draft = MutableStateFlow<Entry?>(null)
    val draft: StateFlow<Entry?> = _draft

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
            val draftEntry = Entry(
                title = title,
                observation = observation,
                imageUri = imageUri,
                color = color,
                isDraft = true
            )
            repository.insert(draftEntry)
            loadDraft()
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

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            repository.update(entry)
        }
    }

    fun insertEntry(entry: Entry) {
        viewModelScope.launch {
            repository.insert(entry)
        }
    }

    fun publishDraft() {
        viewModelScope.launch {
            _draft.value?.let {
                val published = it.copy(
                    isDraft = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.update(published)
                _draft.value = null
            }
        }
    }

    fun deleteDraft() {
        viewModelScope.launch {
            _draft.value?.let {
                repository.delete(it)
                _draft.value = null
            }
        }
    }

    fun loadEntryAsDraft(entry: Entry) {
        viewModelScope.launch {
            val updated = entry.copy(isDraft = true)
            repository.update(updated)
            _draft.value = updated
        }
    }

    fun attachImage(uri: String) {
        viewModelScope.launch {
            _draft.value?.let {
                val updated = it.copy(
                    imageUri = uri,
                    color = null
                )
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

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}