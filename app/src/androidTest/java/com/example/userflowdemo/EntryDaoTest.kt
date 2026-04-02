package com.example.userflowdemo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class EntryDaoTest {

    private lateinit var entryDao: EntryDao
    private lateinit var db: EntryDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EntryDatabase::class.java).build()
        entryDao = db.entryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadEntry() = runBlocking {
        val entry = Entry(title = "Test Entry", observation = "Observation")
        entryDao.insert(entry)
        val allEntries = entryDao.getAllEntries().first()
        assertEquals(allEntries[0].title, "Test Entry")
    }

    @Test
    fun draftSystem_getDraftReturnsDraftOnly() = runBlocking {
        val draft = Entry(title = "Draft", isDraft = true)
        val published = Entry(title = "Published", isDraft = false)
        entryDao.insert(draft)
        entryDao.insert(published)

        val retrievedDraft = entryDao.getDraft()
        assertNotNull(retrievedDraft)
        assertEquals("Draft", retrievedDraft?.title)
        assertTrue(retrievedDraft?.isDraft == true)
    }

    @Test
    fun deleteById_removesCorrectEntry() = runBlocking {
        val entry1 = Entry(id = 1, title = "Entry 1")
        val entry2 = Entry(id = 2, title = "Entry 2")
        entryDao.insert(entry1)
        entryDao.insert(entry2)

        entryDao.deleteById(1)
        val allEntries = entryDao.getAllEntries().first()
        assertEquals(1, allEntries.size)
        assertEquals(2, allEntries[0].id)
    }

    @Test
    fun updateEntry_persistsChanges() = runBlocking {
        val entry = Entry(id = 10, title = "Old Title")
        entryDao.insert(entry)
        val updated = entry.copy(title = "New Title")
        entryDao.update(updated)

        val allEntries = entryDao.getAllEntries().first()
        assertEquals("New Title", allEntries[0].title)
    }
}
