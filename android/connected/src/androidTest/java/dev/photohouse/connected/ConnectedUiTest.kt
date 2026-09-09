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
        override suspend fun login(phone: String, password: String) = SessionToken(86400, "T".repeat(43), "Bearer")
        override suspend fun register(phone: String, password: String, code: String): SessionToken { registrationCode = code; return login(phone, password) }
        override suspend fun session(token: Bearer) = Session("synthetic-account", "+12025550123", listOf(Membership("synthetic-library", "approved", "viewer", 1, null, 0, true)))
        override suspend fun logout(token: Bearer) { }
        override suspend fun acceptInvitation(token: Bearer, code: String) { }
        override suspend fun gallery(token: Bearer, library: String, page: Int) = Gallery(library, page, 50, 1, false, listOf(photo))
        override suspend fun detail(token: Bearer, library: String, assetId: String) = Detail(library, false, photo)
        override suspend fun captions(token: Bearer, library: String, assetId: String) = Captions(library, assetId, false, listOf(Caption("1", "<b>Literal 原文</b>", false, false, null, null)))
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? = null
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
}
