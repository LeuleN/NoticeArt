package com.example.userflowdemo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: EntryViewModel
    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        val db = EntryDatabase.getDatabase(app)
        db.clearAllTables()
        viewModel = EntryViewModel(app)
    }

    @Test
    fun draftSystem_creatingNewEntryCreatesDraft() = runTest {
        viewModel.createDraft(title = "New Draft")
        // No need for advanceUntilIdle with UnconfinedTestDispatcher in MainDispatcherRule
        
        val draft = viewModel.draft.value
        assertNotNull(draft)
        assertEquals("New Draft", draft?.title)
        assertTrue(draft?.isDraft == true)
    }

    @Test
    fun editSystem_startEditingUsesExistingId() = runTest {
        val original = Entry(id = 1, title = "Original", isDraft = false)
        viewModel.insertEntry(original)

        viewModel.startEditing(original)

        val draft = viewModel.draft.value
        assertNotNull(draft)
        assertEquals(1L, draft?.id)
        assertTrue(draft?.isDraft == true)
        assertTrue(viewModel.isEditing)
    }

    @Test
    fun editSystem_discardRestoresOriginalState() = runTest {
        val original = Entry(id = 2, title = "Original", observation = "Old")
        viewModel.insertEntry(original)

        viewModel.startEditing(original)
        
        viewModel.updateDraft("Changed Title")
        viewModel.updateObservation("New Observation")

        viewModel.discardDraft()

        assertNull(viewModel.draft.value)
        val entries = viewModel.entries.first()
        val entry = entries.find { it.id == 2L }
        assertEquals("Original", entry?.title)
        assertEquals("Old", entry?.observation)
    }

    @Test
    fun autoSave_updatesTimestampAndPersistsChanges() = runTest {
        viewModel.createDraft(title = "AutoSave Test")
        val initialDraft = viewModel.draft.value
        val initialTimestamp = initialDraft?.timestamp ?: 0

        // Small delay to ensure timestamp can change
        Thread.sleep(50) 
        
        viewModel.startEditing(initialDraft!!.copy(isDraft = false))
        
        viewModel.updateDraft("Updated Title")
        viewModel.autoSave()

        val entries = viewModel.entries.first()
        val saved = entries.find { it.id == initialDraft.id }
        assertNotNull(saved)
        assertEquals("Updated Title", saved?.title)
        assertFalse(saved!!.isDraft)
        assertTrue("Timestamp should be updated", saved.timestamp > initialTimestamp)
    }

    @Test
    fun undoDelete_restoresEntry() = runTest {
        val entry = Entry(id = 100, title = "To be deleted")
        viewModel.insertEntry(entry)

        viewModel.deleteEntry(entry)
        assertTrue(viewModel.entries.first().isEmpty())

        // Simulate Undo
        viewModel.insertEntry(entry)
        val entries = viewModel.entries.first()
        assertEquals(1, entries.size)
        assertEquals("To be deleted", entries[0].title)
    }

    @Test
    fun mediaSystem_colorsDoNotLeakBetweenImages() = runTest {
        viewModel.createDraft("Media Test")

        // Add first image
        viewModel.addOrUpdateMediaItem("uri1", listOf(0xFFFF0000.toInt()))

        // Add second image
        viewModel.addOrUpdateMediaItem("uri2", listOf(0xFF0000FF.toInt()))

        val draft = viewModel.draft.value
        assertEquals(2, draft?.media?.size)
        assertEquals(listOf(0xFFFF0000.toInt()), draft?.media?.get(0)?.colors)
        assertEquals(listOf(0xFF0000FF.toInt()), draft?.media?.get(1)?.colors)
    }
}
