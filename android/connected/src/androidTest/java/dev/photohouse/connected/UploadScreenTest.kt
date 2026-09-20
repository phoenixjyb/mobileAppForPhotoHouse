package dev.photohouse.connected

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import dev.photohouse.connected.core.UploadReceipt
import dev.photohouse.connected.core.UploadState

class UploadScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun receivedStateExplainsQueueAndPromotionBoundary() {
        compose.setContent {
            UploadPanelState(
                UploadState.Succeeded(UploadReceipt("7", null, "member", "0123456789abcdef0123456789abcdef", "image", 1, 1, "a".repeat(64), 12, 5)),
                zh = false, onPick = {}, onClose = {}, onApproveNetwork = {}, onRetry = {}, onCancel = {},
            )
        }
        compose.onNodeWithTag("upload-received").assertIsDisplayed()
        compose.onNodeWithTag("upload-done").assertIsDisplayed()
    }
}
