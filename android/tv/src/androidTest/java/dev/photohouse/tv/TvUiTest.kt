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
import dev.photohouse.connected.core.*
import dev.photohouse.protocol.*
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
    private fun image(width: Int = 1024, height: Int = 576): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(187, 214, 216))
        val paint = Paint().apply { color = android.graphics.Color.rgb(239, 206, 139) }
        canvas.drawCircle(width * .76f, height * .24f, height * .13f, paint)
        paint.color = android.graphics.Color.rgb(86, 127, 97)
        canvas.drawOval(-width * .1f, height * .55f, width * 1.1f, height * 1.5f, paint)
        paint.color = android.graphics.Color.rgb(36, 76, 65)
        canvas.drawOval(width * .35f, height * .7f, width * 1.3f, height * 1.5f, paint)
        return ByteArrayOutputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); bitmap.recycle(); stream.toByteArray() }
    }
    private inner class SyntheticApi : PhotoHouseApi {
        val photos = (1..6).map { Asset("$it", "image", 1024, 576, null, "2026-09-0$it", "/assets/$it/thumbnail?library=synthetic") }
        val preview = image()
        var originalAllowed = false
        var originalsRead = 0
        var registered: String? = null
        var detailReads = 0
        override suspend fun login(phone: String, password: String) = SessionToken(86400, "T".repeat(43), "Bearer")
        override suspend fun register(phone: String, password: String, code: String): SessionToken { registered = code; return login(phone, password) }
        override suspend fun session(token: Bearer) = Session("synthetic-account", "+12025550123", listOf(Membership("synthetic", "approved", "viewer", 1, null, if (originalAllowed) 1 else 0, true)))
        override suspend fun acceptInvitation(token: Bearer, code: String) { }
        override suspend fun logout(token: Bearer) { }
        override suspend fun gallery(token: Bearer, library: String, page: Int) = Gallery(library, page, 50, 6, originalAllowed, photos)
        override suspend fun detail(token: Bearer, library: String, assetId: String): Detail { detailReads++; return Detail(library, originalAllowed, photos.first { it.id == assetId }) }
        override suspend fun captions(token: Bearer, library: String, assetId: String) = Captions(library, assetId, false, listOf(Caption("1", "A quiet afternoon · 宁静的午后 <b>literal</b>", false, false, null, null)))
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset) = preview
        override suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray { originalsRead++; return image(3840, 2160) }
        override suspend fun videoRange(token: Bearer, library: String, assetId: String, start: Long, length: Int): VideoChunk = error("No video reads in TV slice")
    }
    private fun install(api: SyntheticApi = SyntheticApi(), login: Boolean = true): ConnectedStore {
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread {
            rule.activity.setContent { TvApp(store) }
            if (login) store.authenticate("+12025550123", "synthetic-password-only")
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
    private fun gallery() { rule.onNodeWithTag("library-0").performClick(); rule.waitForIdle() }
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
        rule.onNodeWithTag("setup").assertIsDisplayed()
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertTrue(rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        capture("setup")
    }
    @Test fun remoteFocusOpensPhotoAndBackRestoresSelectedTile() {
        val store = install()
        rule.onNodeWithTag("library-0").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("asset-1").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("asset-2").assertIsFocused()
        capture("grid-en")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitUntil(5000) { store.state.value.detail?.asset?.id == "2" }
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
        capture("fullscreen")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.waitUntil(5000) { store.state.value.detail?.asset?.id == "2" }
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        rule.waitUntil(5000) { store.state.value.detail?.asset?.id == "1" }
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("viewer").assertExists()
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        rule.onNodeWithTag("covered").assertIsDisplayed()
        rule.onAllNodesWithTag("tv-image").assertCountEquals(0)
        assertEquals(0, api.originalsRead)
        capture("covered")
    }
    @Test fun originalModeRequiresExplicitActionAndLogoutClearsIt() {
        val api = SyntheticApi().apply { originalAllowed = true }; val store = install(api); gallery()
        rule.onNodeWithTag("asset-1").performClick(); rule.waitForIdle()
        assertEquals(0, api.originalsRead)
        rule.onNodeWithTag("quality").performScrollTo().performClick()
        rule.waitUntil(10000) { store.state.value.originalPhoto != null }
        assertEquals(1, api.originalsRead)
        rule.waitForIdle()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("tv-image").fetchSemanticsNodes().size == 1 }
        rule.onNodeWithTag("tv-image").assertIsDisplayed()
        capture("original-caption")
        click("Captions"); rule.onNodeWithText("A quiet afternoon", substring = true).assertExists()
        click("Close")
        rule.onNodeWithTag("logout").performClick(); rule.waitForIdle()
        assertFalse(store.hasSession); assertNull(store.state.value.originalPhoto)
        assertTrue(store.state.value.previews.isEmpty())
        rule.onNodeWithTag("viewer").assertDoesNotExist()
    }
    @Test fun homeDisplayNeverShowsPersonalSignInFields() {
        install(SyntheticApi(), login = false)
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
        rule.waitUntil(10000) { store.state.value.detail?.asset?.id == "2" }
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        val reads = api.detailReads
        rule.mainClock.advanceTimeBy(16000); rule.waitForIdle()
        assertEquals(reads, api.detailReads)
        rule.onAllNodesWithTag("tv-image").assertCountEquals(0)
    }
    @Test fun decoderPreserves4kAndBoundsLargerOrMalformedInputs() {
        val full = requireNotNull(decodeTvPhoto(image(3840, 2160)))
        assertEquals(3840, full.bitmap.width); assertEquals(2160, full.bitmap.height); assertFalse(full.downsampled)
        full.bitmap.recycle()
        val large = requireNotNull(decodeTvPhoto(image(4000, 2400)))
        assertTrue(large.downsampled); assertTrue(large.bitmap.width.toLong() * large.bitmap.height <= 8_847_360)
        large.bitmap.recycle()
        assertNull(decodeTvPhoto(byteArrayOf(1, 2, 3)))
        assertNull(decodeTvPhoto(ByteArray(HttpsPhotoHouseApi.ORIGINAL_LIMIT + 1)))
        val grid = requireNotNull(decodeTvPhoto(image(), 262144))
        assertTrue(grid.bitmap.width.toLong() * grid.bitmap.height <= 262144); grid.bitmap.recycle()
    }
}
