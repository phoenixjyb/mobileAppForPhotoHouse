package dev.photohouse.tv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.photohouse.home.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class TvUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @After fun finish() { scope.cancel() }
    private fun fixture(name: String) = InstrumentationRegistry.getInstrumentation().context.assets.open(name).use { it.readBytes() }
    private inner class SyntheticApi : HomeApi {
        val base = HomeWire.feed(fixture("feed.json"), 1)
        val photos = (1..6).map { id -> base.items.single().copy(id = id,
            caption = "A quiet afternoon · 宁静的午后 <b>literal</b>",
            grid = base.items.single().grid.copy(url = HomeWire.path(id, Variant.GRID, 1)),
            display = base.items.single().display.copy(url = HomeWire.path(id, Variant.DISPLAY, 1))) }
        var displayReads = 0
        var feedReads = 0
        var denied = false
        var empty = false
        override suspend fun feed(page: Int): HomeFeed {
            feedReads++
            if (denied) throw HomeFailure(HomeError.DENIED)
            return base.copy(items = if (empty) emptyList() else photos, total = if (empty) 0 else 6)
        }
        override suspend fun preview(asset: HomeAsset, variant: Variant, revision: Int): ByteArray {
            if (variant == Variant.DISPLAY) displayReads++
            return fixture(if (variant == Variant.GRID) "home-8x8.jpg" else "home-3840x2160.jpg")
        }
    }
    private fun install(api: SyntheticApi = SyntheticApi(), connect: Boolean = true): HomeStore {
        val store = HomeStore(api, scope)
        rule.runOnUiThread {
            rule.activity.setContent { TvApp(store) }
            if (connect) store.foreground() else store.disconnect()
        }
        rule.waitForIdle()
        return store
    }
    private fun key(code: Int) {
        rule.waitUntil(10000) { rule.activity.hasWindowFocus() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        rule.waitForIdle()
    }
    private fun click(text: String) {
        val node = rule.onNode(hasText(text) and hasClickAction())
        runCatching { node.performScrollTo() } // Dialog controls have no scroll ancestor.
        node.performClick(); rule.waitForIdle()
    }
    private fun gallery() { rule.onNodeWithTag("grid").assertExists() }
    private fun capture(name: String) {
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(rule.activity.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Test fun setupIsClosedAndWindowRemainsSecure() {
        assertEquals("", BuildConfig.PHOTOHOUSE_ORIGIN)
        assertEquals("", BuildConfig.PHOTOHOUSE_LAN_ADDRESS)
        rule.onNodeWithTag("setup").assertIsDisplayed()
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertTrue(rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        capture("setup")
    }
    @Test fun remoteFocusOpensPhotoAndBackRestoresSelectedTile() {
        val store = install()
        rule.onNodeWithTag("asset-1").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("asset-2").assertIsFocused()
        capture("grid-en")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitUntil(5000) { store.state.value.selected == 2 }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("tv-image").fetchSemanticsNodes().size == 1 }
        capture("detail-en")
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("asset-2").assertIsFocused()
        rule.onNodeWithTag("language").performClick(); capture("grid-zh")
    }
    @Test fun fullScreenRemoteNavigationAndPrivacyClearImages() {
        val api = SyntheticApi(); val store = install(api); gallery()
        rule.onNodeWithTag("asset-1").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("quality").assertDoesNotExist()
        rule.onNodeWithTag("fullscreen").performScrollTo().performClick(); rule.waitForIdle()
        rule.onNodeWithTag("immersive").assertIsFocused()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("tv-image").fetchSemanticsNodes().size == 1 }
        capture("fullscreen")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.waitUntil(5000) { store.state.value.selected == 2 }
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        rule.waitUntil(5000) { store.state.value.selected == 1 }
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("viewer").assertExists()
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        rule.onNodeWithTag("covered").assertIsDisplayed()
        rule.onAllNodesWithTag("tv-image").assertCountEquals(0)
        capture("covered")
    }
    @Test fun prepared4kLoadsAutomaticallyAndDisconnectClearsIt() {
        val api = SyntheticApi(); val store = install(api); gallery()
        rule.onNodeWithTag("asset-1").performClick(); rule.waitForIdle()
        rule.waitUntil(10000) { store.state.value.display != null }
        assertEquals(1, api.displayReads)
        rule.onNodeWithTag("quality").assertDoesNotExist()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("tv-image").fetchSemanticsNodes().size == 1 }
        rule.onNodeWithTag("tv-image").assertIsDisplayed()
        rule.onNodeWithTag("disconnect").assertIsDisplayed()
        val imageBounds = rule.onNodeWithTag("tv-image").fetchSemanticsNode().boundsInRoot
        val viewerBounds = rule.onNodeWithTag("viewer").fetchSemanticsNode().boundsInRoot
        assertTrue(imageBounds.top >= viewerBounds.top && imageBounds.bottom <= viewerBounds.bottom)
        capture("display-caption")
        click("Captions"); rule.onNodeWithText("A quiet afternoon", substring = true).assertExists()
        click("Close")
        rule.onNodeWithTag("disconnect").performClick(); rule.waitForIdle()
        assertNull(store.state.value.feed); assertNull(store.state.value.display)
        assertTrue(store.state.value.grids.isEmpty())
        rule.onNodeWithTag("viewer").assertDoesNotExist()
    }
    @Test fun homeDisplayNeverShowsPersonalSignInFields() {
        install(SyntheticApi(), connect = false)
        rule.onNodeWithTag("home-access-needed").assertIsDisplayed()
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        rule.onAllNodesWithText("Sign in").assertCountEquals(0)
        capture("connection-needed")
    }
    @Test fun slideshowAdvancesAndStopsWhenCovered() {
        val api = SyntheticApi(); val store = install(api); gallery()
        rule.onNodeWithTag("asset-1").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("slideshow").performClick()
        rule.mainClock.advanceTimeBy(8200); rule.waitForIdle()
        rule.waitUntil(10000) { store.state.value.selected == 2 }
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        val reads = api.displayReads
        rule.mainClock.advanceTimeBy(16000); rule.waitForIdle()
        assertEquals(reads, api.displayReads)
        rule.onAllNodesWithTag("tv-image").assertCountEquals(0)
    }
    @Test fun decoderPreservesExact4kAndRejectsMalformedOrOversizedInputs() {
        val full = requireNotNull(decodeTvPhoto(fixture("home-3840x2160.jpg")))
        assertEquals(3840, full.bitmap.width); assertEquals(2160, full.bitmap.height); assertFalse(full.downsampled)
        full.bitmap.recycle()
        assertNull(decodeTvPhoto(byteArrayOf(1, 2, 3)))
        assertNull(decodeTvPhoto(ByteArray(HomeLimits.DISPLAY_BYTES + 1)))
        assertNull(decodeTvPhoto(fixture("home-3840x2160.jpg") + byteArrayOf(1)))
        val grid = requireNotNull(decodeTvPhoto(fixture("home-8x8.jpg"), 262144))
        assertEquals(8, grid.bitmap.width); grid.bitmap.recycle()
    }
    @Test fun denialAndEmptyFeedStayClearWithoutSignIn() {
        val api = SyntheticApi().apply { denied = true }; val store = install(api)
        rule.onNodeWithText("Home feed is disabled", substring = true).assertExists()
        rule.onAllNodesWithTag("tv-image").assertCountEquals(0)
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        capture("denied")
        api.denied = false; api.empty = true
        rule.runOnUiThread { store.retry() }; rule.waitForIdle()
        rule.onNodeWithText("No photos here yet.").assertExists()
        capture("empty")
    }
}
