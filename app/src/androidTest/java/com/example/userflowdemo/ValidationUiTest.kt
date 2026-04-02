package com.example.userflowdemo

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.userflowdemo.ui.WelcomeScreen
import com.example.userflowdemo.ui.theme.UserFlowDemoTheme
import org.junit.Rule
import org.junit.Test

class ValidationUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_nameValidation() {
        composeTestRule.setContent {
            UserFlowDemoTheme {
                WelcomeScreen(onNameSubmitted = {})
            }
        }

        // Initially empty -> "NEXT" button should be disabled
        composeTestRule.onNodeWithText("NEXT").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Please enter your name").assertExists()

        // Non-empty -> "NEXT" button should be enabled
        composeTestRule.onNode(hasSetTextAction()).performTextInput("John")
        composeTestRule.onNodeWithText("NEXT").assertIsEnabled()
        composeTestRule.onNodeWithText("4 / 20").assertExists()

        // Exceeds max length (20) -> should be blocked at 20
        val longName = "ThisIsAVeryLongNameThatExceedsTwentyCharacters"
        composeTestRule.onNode(hasSetTextAction()).performTextInput(longName)
        composeTestRule.onNodeWithText("20 / 20").assertExists()
    }
}
