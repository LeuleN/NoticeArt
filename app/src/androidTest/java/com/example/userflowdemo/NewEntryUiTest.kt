package com.example.userflowdemo

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.userflowdemo.ui.NewEntryScreen
import com.example.userflowdemo.ui.theme.UserFlowDemoTheme
import org.junit.Rule
import org.junit.Test

class NewEntryUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun newEntry_emptyTitleShowsErrorOnPublish() {
        composeTestRule.setContent {
            UserFlowDemoTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NewEntryScreen(
                    draft = Entry(title = ""),
                    originalEntry = null,
                    isEditing = false,
                    snackbarHostState = snackbarHostState,
                    onTitleChange = {},
                    onObservationChange = {},
                    onPublish = {},
                    onSaveAndViewDetail = {},
                    onBackToHome = {},
                    onBackToDetail = {},
                    onAutoSave = {},
                    onNavigateToImageMedia = {}
                )
            }
        }

        // Click publish with empty title
        composeTestRule.onNodeWithContentDescription("Publish").performClick()

        // Should show error message
        composeTestRule.onNodeWithText("Title required").assertIsDisplayed()
    }

    @Test
    fun newEntry_backWithChangesShowsDiscardDialog() {
        var backToHomeCalled = false
        composeTestRule.setContent {
            UserFlowDemoTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NewEntryScreen(
                    draft = Entry(title = "Some Title"), // draft has content
                    originalEntry = null,
                    isEditing = false,
                    snackbarHostState = snackbarHostState,
                    onTitleChange = {},
                    onObservationChange = {},
                    onPublish = {},
                    onSaveAndViewDetail = {},
                    onBackToHome = { backToHomeCalled = true },
                    onBackToDetail = {},
                    onAutoSave = {},
                    onNavigateToImageMedia = {}
                )
            }
        }

        // Click back
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Should show discard dialog
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have unsaved changes. Are you sure you want to leave?").assertIsDisplayed()
        
        // Clicking "Discard" should trigger navigation
        composeTestRule.onNodeWithText("Discard").performClick()
        assert(backToHomeCalled)
    }

    @Test
    fun editEntry_noChangesBackDoesNotShowDialog() {
        var backToDetailCalled = false
        val entry = Entry(id = 1, title = "Original")
        composeTestRule.setContent {
            UserFlowDemoTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NewEntryScreen(
                    draft = entry.copy(isDraft = true),
                    originalEntry = entry,
                    isEditing = true,
                    snackbarHostState = snackbarHostState,
                    onTitleChange = {},
                    onObservationChange = {},
                    onPublish = {},
                    onSaveAndViewDetail = {},
                    onBackToHome = {},
                    onBackToDetail = { backToDetailCalled = true },
                    onAutoSave = {},
                    onNavigateToImageMedia = {}
                )
            }
        }

        // Click back
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Should NOT show dialog and call navigation immediately
        composeTestRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assert(backToDetailCalled)
    }
}
