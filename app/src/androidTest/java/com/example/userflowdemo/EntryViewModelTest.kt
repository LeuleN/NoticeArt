package com.example.userflowdemo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
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

    private suspend fun awaitDraftNotNull(): Entry {
        return viewModel.draft.filter { it != null }.first()!!
    }

    private suspend fun awaitEntriesNotEmpty(): List<Entry> {
        return viewModel.entries.filter { it.isNotEmpty() }.first()
    }

    @Test
    fun draftSystem_creatingNewEntryCreatesDraft() = runTest {
        viewModel.createDraft(title = "New Draft")
        
        val draft = awaitDraftNotNull()
        assertEquals("New Draft", draft.title)
        assertTrue(draft.isDraft)
    }

    @Test
    fun editSystem_startEditingUsesExistingId() = runTest {
        viewModel.insertEntry(Entry(title = "Original", isDraft = false))
        val original = awaitEntriesNotEmpty().first()
        val originalId = original.id

        viewModel.startEditing(original)

        val draft = awaitDraftNotNull()
        assertEquals(originalId, draft.id)
        assertTrue(draft.isDraft)
        assertTrue(viewModel.isEditing)
    }

    @Test
    fun editSystem_discardRestoresOriginalState() = runTest {
        viewModel.insertEntry(Entry(title = "Original", observation = "Old"))
        val original = awaitEntriesNotEmpty().first()
        val originalId = original.id

        viewModel.startEditing(original)
        awaitDraftNotNull()
        
        viewModel.updateDraft("Changed Title")
        viewModel.updateObservation("New Observation")

        viewModel.discardDraft()

        viewModel.draft.filter { it == null }.first()

        val entries = awaitEntriesNotEmpty()
        val restored = entries.find { it.id == originalId }
        assertNotNull("Entry should still exist", restored)
        assertEquals("Original", restored?.title)
        assertEquals("Old", restored?.observation)
    }

    @Test
    fun undoDelete_restoresEntry() = runTest {
        viewModel.insertEntry(Entry(title = "To be deleted"))
        val entry = awaitEntriesNotEmpty().first()

        viewModel.deleteEntry(entry)
        
        viewModel.entries.filter { it.isEmpty() }.first()

        viewModel.insertEntry(entry)
        
        val entries = awaitEntriesNotEmpty()
        assertEquals(1, entries.size)
        assertEquals("To be deleted", entries[0].title)
    }

    @Test
    fun mediaSystem_colorsDoNotLeakBetweenImages() = runTest {
        viewModel.createDraft("Media Test")
        awaitDraftNotNull()

        viewModel.addOrUpdateMediaItem("uri1", listOf(0xFFFF0000.toInt()))
        viewModel.draft.filter { it?.media?.size == 1 }.first()

        viewModel.addOrUpdateMediaItem("uri2", listOf(0xFF0000FF.toInt()))
        val finalDraft = viewModel.draft.filter { it?.media?.size == 2 }.first()!!

        assertEquals(2, finalDraft.media.size)
        assertEquals(listOf(0xFFFF0000.toInt()), finalDraft.media[0].colors)
        assertEquals(listOf(0xFF0000FF.toInt()), finalDraft.media[1].colors)
    }
}
