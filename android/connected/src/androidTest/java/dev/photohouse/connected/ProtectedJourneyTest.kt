package dev.photohouse.connected

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.photohouse.connected.core.*
import dev.photohouse.protocol.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Synthetic protected-native journey. No configured origin, credentials or live data. */
class ProtectedJourneyTest {
    @get:Rule val rule = createComposeRule()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After fun stop() { scope.cancel() }

    private class SyntheticApi : PhotoHouseApi {
        override val protectedNativeV2Enabled = true
        override val photoDeliveryEnabled = true
        var loginPhone: String? = null
        var loginPassword: String? = null
        var displayCalls = 0
        var originalCalls = 0
        var failNextStories = false
        var storyCalls = 0
        var storyText = "Literal family text / 家人亲笔文字"

        private val asset = Asset("1", "image", 1, 1, null, "2026-01-01", "/assets/1/thumbnail?library=synthetic-library")
        private val display = png()

        override suspend fun login(phone: String, password: String): SessionToken {
            loginPhone = phone; loginPassword = password
            return SessionToken(86400, "T".repeat(43), "Bearer")
        }
        override suspend fun register(phone: String, password: String, code: String) = login(phone, password)
        override suspend fun session(token: Bearer) = Session(
            "synthetic-account", "+8612345678",
            listOf(Membership("synthetic-library", "approved", "viewer", 1, null, 0, true)))
        override suspend fun logout(token: Bearer) = Unit
        override suspend fun acceptInvitation(token: Bearer, code: String) = Unit
        override suspend fun gallery(token: Bearer, library: String, page: Int) =
            Gallery(library, page, 50, 1, false, listOf(asset))
        override suspend fun detail(token: Bearer, library: String, assetId: String) =
            Detail(library, false, asset)
        override suspend fun captions(token: Bearer, library: String, assetId: String) =
            Captions(library, assetId, false, emptyList())
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? = null
        override suspend fun displayPhoto(token: Bearer, library: String, assetId: String): ByteArray {
            displayCalls++; return display
        }
        override suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray {
            originalCalls++; return display
        }
        override suspend fun videoRange(token: Bearer, library: String, assetId: String, start: Long, length: Int) =
            VideoChunk(start, 1, byteArrayOf(0))
        override suspend fun stories(token: Bearer, library: String, assetId: String, page: Int): ProtectedStoryPage {
            storyCalls++
            if (failNextStories) { failNextStories = false; throw ApiFailure(FailureKind.OFFLINE) }
            val items = if (page == 1) listOf(ProtectedStory(
                "11111111-1111-1111-1111-111111111111", assetId,
                "Lake day / 湖边的一天", storyText, "mixed",
                "Synthetic writer / 示例作者", "22222222-2222-2222-2222-222222222222",
                1, 100, 100, false, false)) else emptyList()
            return ProtectedStoryPage(library, assetId, page, false, page == 1, items)
        }

        private fun png(): ByteArray {
            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            return ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                bitmap.recycle(); out.toByteArray()
            }
        }
    }

    private fun start(api: SyntheticApi): ConnectedStore {
        val store = ConnectedStore(api, scope)
        rule.setContent { ConnectedApp(store) }
        return store
    }
    private fun scroll(tag: String) { rule.onNodeWithTag("connected-screen").performScrollToNode(hasTestTag(tag)) }
    private fun clickTag(tag: String) { scroll(tag); rule.onNodeWithTag(tag).performScrollTo().performClick(); rule.waitForIdle() }
    private fun clickText(text: String) {
        val matcher = hasText(text) and hasClickAction()
        rule.onNodeWithTag("connected-screen").performScrollToNode(matcher)
        rule.onNode(matcher).performClick(); rule.waitForIdle()
    }
    private fun input(label: String, text: String) {
        val matcher = hasText(label) and hasSetTextAction()
        rule.onNodeWithTag("connected-screen").performScrollToNode(matcher)
        rule.onNode(matcher).performTextInput(text)
    }

    @Test fun protectedLoginStoriesMediaPagingRetryRefreshAndLogout() {
        val api = SyntheticApi()
        val store = start(api)

        rule.onNode(hasText("Phone with country code") and hasSetTextAction()).assertTextContains("+86")
        rule.onNode(hasText("Phone with country code") and hasSetTextAction()).performTextReplacement("+8612345678")
        input("Password", "eight888")
        clickText("Sign in")
        rule.waitUntil(5000) { rule.onAllNodesWithText("Your libraries").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("+8612345678", api.loginPhone)
        assertEquals("eight888", api.loginPassword)

        clickText("Open library")
        rule.waitUntil(5000) { rule.onAllNodesWithText("Photos").fetchSemanticsNodes().isNotEmpty() }
        clickTag("details-1")
        clickText("View photo")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, api.displayCalls)
        assertEquals("Display route must be used; originals remain untouched", 0, api.originalCalls)
        rule.onNodeWithText("Close photo").performClick(); rule.waitForIdle()

        clickTag("stories-open")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("story-11111111-1111-1111-1111-111111111111").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("connected-screen").performScrollToNode(hasText("Literal family text / 家人亲笔文字"))
        rule.onNodeWithText("Literal family text / 家人亲笔文字").assertIsDisplayed()
        scroll("story-11111111-1111-1111-1111-111111111111")
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "protected-stories-en.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        clickTag("stories-next")
        rule.onNodeWithTag("connected-screen").performScrollToNode(hasText("No family stories on this page yet."))
        rule.onNodeWithText("No family stories on this page yet.").assertIsDisplayed()
        clickTag("stories-previous")
        rule.onNodeWithTag("connected-screen").performScrollToNode(hasText("Lake day / 湖边的一天"))
        rule.onNodeWithText("Lake day / 湖边的一天").assertIsDisplayed()
        clickTag("stories-refresh")
        assertTrue(api.storyCalls >= 4)

        api.failNextStories = true
        clickTag("stories-refresh")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("stories-retry").fetchSemanticsNodes().isNotEmpty() }
        clickTag("stories-retry")
        rule.waitUntil(5000) { rule.onAllNodesWithText("Lake day / 湖边的一天").fetchSemanticsNodes().isNotEmpty() }

        clickTag("app-settings")
        rule.onNodeWithText("Sign out").performClick(); rule.waitForIdle()
        assertTrue(!store.hasSession)
        rule.onNodeWithTag("protected-stories").assertDoesNotExist()
    }
    @Test fun longStoryReaderScrollsToEndAndClearsOnBackground() {
        val api = SyntheticApi().apply { storyText = "我们在湖边散步，听风吹过树梢。\n".repeat(1000) + "最后一段 / End of memory" }
        val store = start(api)
        rule.runOnIdle { store.authenticate("+8612345678", "eight888") }
        rule.waitForIdle(); clickText("Open library"); clickTag("details-1");clickTag("stories-open")
        clickTag("story-read-full")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("story-reader").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("story-reader").performScrollToNode(hasText("最后一段 / End of memory", substring = true))
        rule.onNode(hasText("最后一段 / End of memory", substring = true)).assertIsDisplayed()
        val bitmap = rule.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "protected-story-long.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rule.runOnIdle { store.background() };rule.waitForIdle()
        rule.onNodeWithTag("story-reader").assertDoesNotExist()
        assertTrue(store.state.value.stories == null)
    }

}
