package dev.photohouse.connected

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.photohouse.connected.core.*
import dev.photohouse.protocol.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Synthetic component tests. No configured origin, network or test bypass in the APK. */
@RunWith(AndroidJUnit4::class)
class ConnectedUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @After fun stop() { scope.cancel() }
    private class SyntheticApi : PhotoHouseApi {
        var registrationCode: String? = null
        val photo = Asset("1", "video", null, null, null, "2026-01-01", "/assets/1/thumbnail?library=synthetic-library")
        var photos = listOf(photo)
        var total = 1L
        var originalsAllowed = false
        override suspend fun login(phone: String, password: String) = SessionToken(86400, "T".repeat(43), "Bearer")
        override suspend fun register(phone: String, password: String, code: String): SessionToken { registrationCode = code; return login(phone, password) }
        override suspend fun session(token: Bearer) = Session("synthetic-account", "+12025550123", listOf(Membership("synthetic-library", "approved", "viewer", 1, null, 0, true)))
        override suspend fun logout(token: Bearer) { }
        override suspend fun acceptInvitation(token: Bearer, code: String) { }
        override suspend fun gallery(token: Bearer, library: String, page: Int) = Gallery(library, page, 50, total, false, photos)
        override suspend fun detail(token: Bearer, library: String, assetId: String) = Detail(library, originalsAllowed, photos.first { it.id == assetId })
        override suspend fun captions(token: Bearer, library: String, assetId: String) = Captions(library, assetId, false, listOf(Caption("1", "<b>Literal 原文</b>", false, false, null, null)))
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? = null
        override suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray {
            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.rgb(60, 120, 160))
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.YELLOW }
            canvas.drawRect(80f, 80f, 400f, 360f, paint)
            return java.io.ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); bitmap.recycle(); stream.toByteArray()
            }
        }
    }
    private fun reveal(matcher: SemanticsMatcher) { rule.onNodeWithTag("connected-screen").performScrollToNode(matcher) }
    private fun click(text: String) { val matcher = hasText(text) and hasClickAction(); reveal(matcher); rule.onNode(matcher).performClick(); rule.waitForIdle() }
    private fun input(label: String, text: String) { val matcher = hasText(label) and hasSetTextAction(); reveal(matcher); rule.onNode(matcher).performTextInput(text) }
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
    @Test fun unconfiguredAppDisablesAdmissionAndKeepsSecureWindow() {
        assertEquals("", BuildConfig.PHOTOHOUSE_ORIGIN)
        rule.onNodeWithTag("server-not-configured").assertIsDisplayed()
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertTrue(rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        capture("unconfigured-en")
        click("简体中文"); rule.onNodeWithText("需要配置服务器").assertIsDisplayed(); capture("unconfigured-zh")
    }
    @Test fun syntheticLoginBrowsingAndLiteralCaptionComponents() {
        val store = ConnectedStore(SyntheticApi(), scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) } }
        capture("admission-en")
        input("Phone with country code", "+12025550123"); input("Password (15–128 characters)", "synthetic-password-only")
        click("Sign in"); click("Open library")
        rule.onNodeWithText("Preview unavailable").assertExists()
        click("2026-01-01")
        reveal(hasText("<b>Literal 原文</b>")); rule.onNodeWithText("<b>Literal 原文</b>").assertIsDisplayed(); capture("literal-caption-en")
        click("简体中文"); reveal(hasText("<b>Literal 原文</b>")); rule.onNodeWithText("<b>Literal 原文</b>").assertIsDisplayed(); capture("literal-caption-zh")
        click("退出登录"); assertFalse(store.hasSession)
        rule.onAllNodes(hasText("<b>Literal 原文</b>")).assertCountEquals(0)
    }
    @Test fun syntheticInvitedRegistrationRequiresAllInputs() {
        val api = SyntheticApi(); val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) } }
        click("Have an invitation? Register")
        reveal(hasText("Register with invitation")); rule.onNode(hasText("Register with invitation") and hasClickAction()).assertIsNotEnabled()
        input("Phone with country code", "+12025550123"); input("Password (15–128 characters)", "synthetic-password-only"); input("Invitation code", "synthetic-invitation")
        click("Register with invitation")
        assertEquals("synthetic-invitation", api.registrationCode)
        assertNotNull(store.state.value.session)
        rule.onAllNodes(hasText("synthetic-invitation")).assertCountEquals(0)
    }
    @Test fun originalViewerZoomCloseAndPrivacyUseSyntheticImageBytes() {
        val api = SyntheticApi().apply { photos = listOf(photo.copy(kind = "image")); originalsAllowed = true }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread {
            rule.activity.setContent { ConnectedApp(store) }
            store.authenticate("+12025550123", "synthetic-password-only")
        }
        click("Open library"); click("2026-01-01"); click("Open original photo")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        rule.onNodeWithTag("photo-zoom").assertTextEquals("100%")
        rule.onNode(hasText("Zoom in") and hasClickAction()).performClick()
        rule.onNodeWithTag("photo-zoom").assertTextEquals("150%")
        rule.onNode(hasText("Fit photo") and hasClickAction()).performClick()
        rule.onNodeWithTag("original-image").performTouchInput {
            val delta = androidx.compose.ui.geometry.Offset(35f, 0f)
            pinch(center - delta, center + delta, center - delta * 2f, center + delta * 2f, 300)
        }
        rule.onNodeWithTag("photo-zoom").assertTextContains("%", substring = true)
        rule.onNodeWithTag("photo-zoom").assert(hasText("100%").not())
        rule.onNode(hasText("Fit photo") and hasClickAction()).performClick()
        rule.onNodeWithTag("original-image").performTouchInput { doubleClick(center) }
        rule.onNodeWithTag("photo-zoom").assertTextEquals("200%")
        rule.onNodeWithTag("original-image").performTouchInput { swipe(center, center + androidx.compose.ui.geometry.Offset(40f, 20f), 200) }
        capture("original-photo-en")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle(); assertNull(store.state.value.originalPhoto)
        rule.onAllNodesWithTag("original-viewer").assertCountEquals(0)
        click("简体中文"); click("打开原始照片")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        rule.onNodeWithTag("photo-zoom").assertTextEquals("100%")
        capture("original-photo-zh")
        rule.onNode(hasText("关闭照片") and hasClickAction()).performClick()
        rule.waitForIdle(); assertNull(store.state.value.originalPhoto)
        click("打开原始照片")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        rule.runOnUiThread { store.background() }
        rule.waitForIdle(); rule.onAllNodesWithTag("original-viewer").assertCountEquals(0)
        assertNull(store.state.value.originalPhoto); assertTrue(store.state.value.covered)
    }
    @Test fun originalDecoderRejectsCorruptionBoundsPixelsAndHandlesAllExifOrientations() {
        assertNull(decodeOriginalPhoto(byteArrayOf(1, 2, 3)))
        val large = Bitmap.createBitmap(3200, 2000, Bitmap.Config.ARGB_8888)
        large.eraseColor(android.graphics.Color.BLUE)
        val bytes = java.io.ByteArrayOutputStream().use { stream ->
            large.compress(Bitmap.CompressFormat.PNG, 100, stream); large.recycle(); stream.toByteArray()
        }
        val reduced = requireNotNull(decodeOriginalPhoto(bytes))
        assertTrue(reduced.downsampled); assertTrue(reduced.bitmap.width.toLong() * reduced.bitmap.height <= 4_000_000)
        reduced.bitmap.recycle()
        val landscape = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val jpeg = java.io.ByteArrayOutputStream().use { stream ->
            landscape.compress(Bitmap.CompressFormat.JPEG, 90, stream); landscape.recycle(); stream.toByteArray()
        }
        // Generated EXIF APP1 segment: little-endian TIFF with orientation 6.
        val tiff = java.nio.ByteBuffer.allocate(26).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put(0x49.toByte()).put(0x49.toByte()).putShort(42.toShort()).putInt(8).putShort(1.toShort())
            .putShort(0x112.toShort()).putShort(3.toShort()).putInt(1).putShort(6.toShort()).putShort(0.toShort()).putInt(0).array()
        val exif = byteArrayOf(0xff.toByte(), 0xe1.toByte(), 0, 34) + byteArrayOf(69, 120, 105, 102, 0, 0) + tiff
        val rotated = requireNotNull(decodeOriginalPhoto(jpeg.copyOfRange(0, 2) + exif + jpeg.copyOfRange(2, jpeg.size)))
        assertEquals(100, rotated.bitmap.width); assertEquals(200, rotated.bitmap.height); rotated.bitmap.recycle()
        val colors = intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt(), 0xffffffff.toInt(), 0xffffff00.toInt(), 0xff00ffff.toInt())
        val orders = listOf(listOf(0,1,2,3,4,5), listOf(2,1,0,5,4,3), listOf(5,4,3,2,1,0),
            listOf(3,4,5,0,1,2), listOf(0,3,1,4,2,5), listOf(3,0,4,1,5,2),
            listOf(5,2,4,1,3,0), listOf(2,5,1,4,0,3))
        for (orientation in 1..8) {
            val input = Bitmap.createBitmap(colors, 3, 2, Bitmap.Config.ARGB_8888)
            val output = orientPhoto(input, orientation)
            assertEquals(if (orientation < 5) 3 else 2, output.width)
            assertEquals(if (orientation < 5) 2 else 3, output.height)
            val actual = IntArray(6); output.getPixels(actual, 0, output.width, 0, 0, output.width, output.height)
            assertArrayEquals(orders[orientation - 1].map { colors[it] }.toIntArray(), actual)
            output.recycle()
        }
    }
    @Test fun photoNavigationReturnsToSelectedPageAndSupportsBothLanguages() {
        val api = SyntheticApi().apply { photos = listOf(photo, photo.copy(id = "2", taken_at = "2026-01-02")); total = 100 }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread {
            rule.activity.setContent { ConnectedApp(store) }
            store.authenticate("+12025550123", "synthetic-password-only")
        }
        click("Open library"); click("Next"); click("2026-01-01")
        reveal(hasText("Photo 1 of 2 · Page 2"))
        rule.onNodeWithText("Photo 1 of 2 · Page 2").assertIsDisplayed()
        rule.onNode(hasText("Previous photo") and hasClickAction()).assertIsNotEnabled()
        click("Next photo")
        reveal(hasText("Photo 2 of 2 · Page 2"))
        rule.onNodeWithText("Photo 2 of 2 · Page 2").assertIsDisplayed()
        rule.onNode(hasText("Next photo") and hasClickAction()).assertIsNotEnabled()
        capture("photo-navigation-en")
        click("简体中文"); reveal(hasText("第 2 页 · 第 2/2 张"))
        rule.onNodeWithText("第 2 页 · 第 2/2 张").assertIsDisplayed()
        capture("photo-navigation-zh")
        click("上一张"); click("返回照片")
        reveal(hasText("第 2 页 · 100 张照片")); rule.onNodeWithText("第 2 页 · 100 张照片").assertIsDisplayed()
        click("2026-01-01")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        reveal(hasText("第 2 页 · 100 张照片")); rule.onNodeWithText("第 2 页 · 100 张照片").assertIsDisplayed()
        click("退出登录"); assertNull(store.state.value.photoNavigation)
        rule.onAllNodes(hasText("第 2 页 · 第 1/2 张")).assertCountEquals(0)
    }
}
