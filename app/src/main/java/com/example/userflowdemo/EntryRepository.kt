package com.example.userflowdemo

import kotlinx.coroutines.flow.Flow

class EntryRepository(
    private val entryDao: EntryDao
) {

    val allEntries: Flow<List<Entry>> =
        entryDao.getAllEntries()

    suspend fun insert(entry: Entry) {
        entryDao.insert(entry)
    }

    suspend fun update(entry: Entry) {
        entryDao.update(entry)
    }

    suspend fun getDraft(): Entry? {
        return entryDao.getDraft()
    }

    suspend fun delete(entry: Entry) {
        entryDao.deleteById(entry.id)
    }
}