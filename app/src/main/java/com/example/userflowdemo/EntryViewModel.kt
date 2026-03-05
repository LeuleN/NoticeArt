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

    fun createDraft(initialTitle: String = "") {
        viewModelScope.launch {
            val draftEntry = Entry(
                title = initialTitle,
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

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}