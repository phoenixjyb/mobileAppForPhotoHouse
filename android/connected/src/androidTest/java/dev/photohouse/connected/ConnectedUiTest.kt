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
        override var discoveryEnabled = false
        var discoveryError: ApiFailure? = null
        var discoverySearches = 0
        var discoveredFilters: PhoneFilters? = null
        private val pinned = PhoneChoice("7", "Sample family", 1, listOf("示例家人"))
        override suspend fun facets(token: Bearer, library: String, facet: PhoneFacet, page: Int, binding: String?): PhoneFacetPage {
            val snap = PhoneSnapshot(library, "b".repeat(64), "1", PhoneDiscoveryWire.fields, 51, 51,
                PhoneDiscoveryWire.fields.associateWith { PhoneCoverage(51, 0) }, "2026-01-01", "2026-12-31", listOf(pinned))
            val choices = if (page == 1) (1..50).map { if (it == 7) pinned else PhoneChoice(it.toString(), "Sample $it · 示例", 1) }
                else listOf(PhoneChoice("51", "Later choice · 后页选项", 1))
            return PhoneFacetPage(snap, facet, page, 50, 51, page == 1, choices)
        }
        override suspend fun search(token: Bearer, library: String, binding: String, filters: PhoneFilters, page: Int, fingerprint: String?): PhoneSearchPage {
            discoverySearches++; discoveredFilters = filters; discoveryError?.let { throw it }
            return PhoneSearchPage(Gallery(library, page, 50, 51, originalsAllowed, photos), binding, "f".repeat(64), page == 1)
        }
        var previewBytes: ByteArray? = null
        override var preparedVideoEnabled = false
        var preparedHeads = 0
        var originalVideoReads = 0
        var preparedError: ApiFailure? = null
        override suspend fun preparedVideoInfo(token: Bearer, library: String, assetId: String): PreparedVideoInfo {
            preparedHeads++; preparedError?.let { throw it }
            return PreparedVideoInfo(videoBytes.size.toLong(), "\"" + "a".repeat(64) + "\"")
        }
        override suspend fun preparedVideoRange(token: Bearer, library: String, assetId: String, info: PreparedVideoInfo, start: Long, length: Int): VideoChunk {
            preparedError?.let { throw it }; return readVideo(start, length)
        }
        var videoBytes = byteArrayOf()
        val videoReads = java.util.concurrent.CopyOnWriteArrayList<Pair<Long, Int>>()
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
        var transientDetailFailures = 0
        override suspend fun detail(token: Bearer, library: String, assetId: String): Detail {
            if (transientDetailFailures > 0) { transientDetailFailures--; throw ApiFailure(FailureKind.HTTP, 503) }
            return Detail(library, originalsAllowed, photos.first { it.id == assetId })
        }
        override suspend fun captions(token: Bearer, library: String, assetId: String) = Captions(library, assetId, false, listOf(Caption("1", "<b>Literal 原文</b>", false, false, null, null)))
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? = previewBytes
        override suspend fun videoRange(token: Bearer, library: String, assetId: String, start: Long, length: Int): VideoChunk {
            originalVideoReads++; return readVideo(start, length)
        }
        private fun readVideo(start: Long, length: Int): VideoChunk {
            videoReads += start to length
            return VideoChunk(start, videoBytes.size.toLong(), videoBytes.copyOfRange(start.toInt(), minOf(videoBytes.size, start.toInt() + length)))
        }
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
    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        if (text in listOf("简体中文", "English", "System", "系统", "Sign out", "退出登录")) {
            reveal(hasTestTag("app-settings")); rule.onNodeWithTag("app-settings").performClick()
        } else reveal(matcher)
        rule.onNode(matcher).performClick(); rule.waitForIdle()
    }
    private fun details(id: String) {
        reveal(hasTestTag("details-$id")); rule.onNodeWithTag("details-$id").performClick(); rule.waitForIdle()
    }
    private fun positionSeconds(): Int {
        val clock = rule.onNodeWithTag("video-position").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text.substringBefore(" / ")
        return clock.split(':').fold(0) { total, part -> total * 60 + part.toInt() }
    }
    private fun input(label: String, text: String) { val matcher = hasText(label) and hasSetTextAction(); reveal(matcher); rule.onNode(matcher).performTextInput(text) }
    private fun screenAwake(): Boolean {
        fun awake(view: android.view.View): Boolean = view.keepScreenOn || view is android.view.ViewGroup &&
            (0 until view.childCount).any { awake(view.getChildAt(it)) }
        return rule.runOnIdle { awake(rule.activity.window.decorView) }
    }
    private fun capture(name: String) {
        rule.waitForIdle()
        val videoBounds = if (name.startsWith("video-")) rule.onNodeWithTag("video-surface").fetchSemanticsNode().boundsInWindow else null
        rule.runOnUiThread {
            // Dialogs own a separate window; draw that owned window without
            // disabling FLAG_SECURE or capturing another application's surface.
            val view = if (name.startsWith("settings-")) {
                check(android.os.Build.VERSION.SDK_INT >= 29) { "Dialog render evidence requires API 29+" }
                android.view.inspector.WindowInspector.getGlobalWindowViews()
                    .last { it.isShown && it !== rule.activity.window.decorView }
            } else rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            // Software View.draw may omit the hardware video layer. Keep a separate
            // test-owned decoded frame; do not composite or disable FLAG_SECURE.
            if (name.startsWith("video-")) {
                fun textures(node: android.view.View): List<android.view.TextureView> = when (node) {
                    is android.view.TextureView -> listOf(node)
                    is android.view.ViewGroup -> (0 until node.childCount).flatMap { textures(node.getChildAt(it)) }
                    else -> emptyList()
                }
                val texture = textures(view).single()
                assertEquals("Video keeps its decoded aspect ratio", 16f / 9f, texture.width.toFloat() / texture.height, 0.02f)
                val frame = requireNotNull(texture.bitmap)
                val colors = mutableSetOf<Int>()
                for (y in 0 until frame.height step 8) for (x in 0 until frame.width step 8) colors += frame.getPixel(x, y)
                assertTrue("Decoded synthetic video must contain colored pixels", colors.size > 8)
                val bounds = requireNotNull(videoBounds)
                assertEquals(16f / 9f, bounds.width / bounds.height, 0.02f)
                File(rule.activity.filesDir, "$name-frame.png").outputStream().use { frame.compress(Bitmap.CompressFormat.PNG, 100, it) }
                frame.recycle()
            }
            File(rule.activity.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    private fun clickTag(tag: String) { reveal(hasTestTag(tag)); rule.onNodeWithTag(tag).performClick(); rule.waitForIdle() }
    @Test fun discoverySelectionPagingMediaReturnAndBilingualLayout() {
        val api = SyntheticApi().apply { discoveryEnabled = true }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); clickTag("open-discovery")
        clickTag("pin-7")
        capture("discovery-en")
        clickTag("facet-next"); clickTag("choice-people-51"); clickTag("facet-previous")
        assertEquals(listOf("7", "51"), store.state.value.discovery!!.filters.people.map { it.id })
        clickTag("facet-tags")
        clickTag("choice-tags-1")
        clickTag("facet-locations")
        clickTag("choice-locations-2")
        reveal(hasTestTag("discovery-caption")); rule.onNodeWithTag("discovery-caption").performTextInput("生日 birthday")
        reveal(hasTestTag("discovery-from")); rule.onNodeWithTag("discovery-from").performTextInput("2026-01-01")
        clickTag("discovery-media-video")
        reveal(hasTestTag("discovery-filter-summary")); rule.onNodeWithTag("discovery-filter-summary").assertTextContains("2026-01-01")
        assertEquals(0, api.discoverySearches)
        clickTag("discovery-quick-apply")
        assertEquals(1, api.discoverySearches); assertEquals("生日 birthday", api.discoveredFilters!!.caption)
        assertEquals("2026-01-01", api.discoveredFilters!!.from)
        assertEquals(listOf("2"), api.discoveredFilters!!.places.map { it.id })
        click("Next")
        reveal(hasTestTag("media-1")); rule.onNodeWithTag("media-1").performClick(); rule.waitForIdle()
        assertNull(store.state.value.video) // Fresh detail denies originals; discovery does not grant them.
        click("Back to results")
        assertEquals(2, store.state.value.gallery!!.page)
        clickTag("open-discovery")
        click("简体中文")
        reveal(hasText("寻找回忆")); capture("discovery-zh")
        clickTag("discovery-apply")
        rule.runOnUiThread { store.background() }
        rule.waitForIdle(); assertNull(store.state.value.discovery); assertTrue(store.state.value.previews.isEmpty())
    }
    @Test fun discoveryInvalidDatesAndStaleBindingRequireExplicitReapply() {
        val api = SyntheticApi().apply { discoveryEnabled = true }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); clickTag("open-discovery")
        reveal(hasTestTag("discovery-from")); rule.onNodeWithTag("discovery-from").performTextInput("2026-02-30")
        clickTag("discovery-apply"); assertEquals(0, api.discoverySearches)
        reveal(hasTestTag("discovery-input-error")); rule.onNodeWithTag("discovery-input-error").assertIsDisplayed()
        clickTag("discovery-clear")
        reveal(hasTestTag("discovery-filter-summary")); rule.onNodeWithTag("discovery-filter-summary").assertTextContains("No filters selected")
        api.discoveryError = ApiFailure(FailureKind.HTTP, 409)
        clickTag("discovery-apply")
        assertTrue(store.state.value.discovery!!.changed); assertEquals(1, api.discoverySearches)
        api.discoveryError = null; clickTag("discovery-reload")
        assertEquals(1, api.discoverySearches); assertEquals("", store.state.value.discovery!!.filters.from)
        clickTag("discovery-apply"); assertEquals(2, api.discoverySearches)
        rule.runOnUiThread { store.logout() }; rule.waitForIdle()
        assertNull(store.state.value.discovery); assertNull(store.state.value.gallery)
    }
    @Test fun galleryTapOpensPhotoAndDetailsActionKeepsMetadataReachable() {
        val api = SyntheticApi().apply { photos = listOf(photo.copy(kind = "image")); originalsAllowed = true; total = 100 }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); click("Next")
        reveal(hasTestTag("media-1")); rule.onNodeWithTag("media-1").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        if (rule.onAllNodesWithTag("photo-exit-fullscreen").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("photo-exit-fullscreen").performClick()
        assertEquals(2, store.state.value.photoNavigation?.page)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertNull(store.state.value.originalPhoto)
        click("Back to Photos"); details("1")
        assertFalse(store.state.value.viewingOriginal)
        reveal(hasText("<b>Literal 原文</b>")); rule.onNodeWithText("<b>Literal 原文</b>").assertIsDisplayed()
    }
    @Test fun longVideoStartsWithoutFullDownloadAndSeeksPastOneMinute() = longVideoJourney(false)
    @Test fun preparedViewerStreamsAndSeeksWithoutOriginalPermission() = longVideoJourney(true)
    private fun longVideoJourney(prepared: Boolean) {
        val api = SyntheticApi().apply {
            preparedVideoEnabled = prepared
            originalsAllowed = !prepared
            videoBytes = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("synthetic-long-video.mp4").use { it.readBytes() }
        }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library")
        reveal(hasTestTag("media-1")); rule.onNodeWithTag("media-1").performClick()
        fun ready() { rule.waitUntil(30000) { rule.onAllNodes(hasText("Play") and isEnabled()).fetchSemanticsNodes().isNotEmpty() } }
        ready()
        rule.onNodeWithTag("video-position").assertTextEquals("0:00 / 2:05")
        val reader = store.state.value.video!!
        // The decoder can request headers and a small read-ahead. It must not
        // require all bytes before presenting Play for this fast-start fixture.
        val ranges = api.videoReads.map { it.first until minOf(api.videoBytes.size.toLong(), it.first + it.second) }
        assertTrue(ranges.sumOf { it.last - it.first + 1 } < api.videoBytes.size)
        for (target in listOf(90000f, 123000f, 2000f)) {
            rule.onNodeWithTag("video-seek").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(target) }
            ready(); rule.waitUntil(10000) { positionSeconds() in (target.toInt() / 1000 - 1)..(target.toInt() / 1000 + 1) }
        }
        rule.onNodeWithText("Play").performScrollTo().performClick()
        rule.waitUntil(10000) { positionSeconds() >= 3 }
        rule.onNodeWithText("Pause").performScrollTo().performClick(); ready()
        capture(if (prepared) "video-prepared-en" else "video-long-en")
        if (prepared) { assertEquals(1, api.preparedHeads); assertEquals(0, api.originalVideoReads) }
        assertTrue(api.videoReads.all { it.second in 1..262144 })
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        assertTrue(reader.isClosed); assertNull(store.state.value.video)
    }
    @Test fun preparedReadinessShowsErrorsAndRequiresExplicitRetry() {
        val api = SyntheticApi().apply {
            preparedVideoEnabled = true
            preparedError = ApiFailure(FailureKind.HTTP, 404)
            videoBytes = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("synthetic-video.mp4").use { it.readBytes() }
        }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); clickTag("media-1")
        val unavailable = "This video is not ready for playback yet. You can try again later."
        reveal(hasText(unavailable)); rule.onNodeWithText(unavailable).assertIsDisplayed()
        capture("prepared-not-ready-en")
        rule.onAllNodesWithTag("open-original-video").assertCountEquals(0)
        assertEquals(1, api.preparedHeads); assertTrue(api.videoReads.isEmpty())
        api.preparedError = ApiFailure(FailureKind.HTTP, 429, 4000)
        clickTag("open-video")
        reveal(hasTestTag("open-video")); rule.onNodeWithTag("open-video").assertIsNotEnabled()
        api.preparedError = null
        rule.waitUntil(8000) { rule.onAllNodes(hasTestTag("open-video") and isEnabled()).fetchSemanticsNodes().size == 1 }
        assertEquals(2, api.preparedHeads)
        clickTag("open-video")
        rule.waitUntil(30000) { rule.onAllNodes(hasText("Play") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(3, api.preparedHeads); assertEquals(0, api.originalVideoReads)
        val reader = store.state.value.video!!
        rule.runOnUiThread { store.logout() }; rule.waitForIdle()
        assertTrue(reader.isClosed); assertNull(store.state.value.video)
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
        details("1")
        reveal(hasText("<b>Literal 原文</b>")); rule.onNodeWithText("<b>Literal 原文</b>").assertIsDisplayed(); capture("literal-caption-en")
        click("简体中文"); reveal(hasText("<b>Literal 原文</b>")); rule.onNodeWithText("<b>Literal 原文</b>").assertIsDisplayed(); capture("literal-caption-zh")
        click("退出登录"); assertFalse(store.hasSession)
        rule.onAllNodes(hasText("<b>Literal 原文</b>")).assertCountEquals(0)
    }
    @Test fun photoLedGalleryDetailAndSettingsRemainReachableInBothLanguages() {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(android.graphics.Color.rgb(196, 219, 225))
        paint.color = android.graphics.Color.rgb(246, 220, 165); canvas.drawCircle(460f, 100f, 48f, paint)
        paint.color = android.graphics.Color.rgb(138, 169, 151); canvas.drawOval(-120f, 220f, 680f, 700f, paint)
        paint.color = android.graphics.Color.rgb(68, 107, 88); canvas.drawOval(180f, 270f, 880f, 790f, paint)
        val preview = java.io.ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); bitmap.recycle(); stream.toByteArray()
        }
        val api = SyntheticApi().apply {
            previewBytes = preview; originalsAllowed = true
            photos = listOf(photo.copy(kind = "image"), photo.copy(id = "2", taken_at = "2026-01-02"),
                photo.copy(id = "3", kind = "image", taken_at = null))
            total = photos.size.toLong()
        }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        reveal(hasText("Your libraries")); capture("libraries-en")
        click("Open library")
        rule.waitUntil(5000) { store.state.value.previews.size == 3 }
        reveal(hasText("Photos")); capture("gallery-en")
        details("1")
        reveal(hasContentDescription("Photo 1")); rule.onNodeWithContentDescription("Photo 1").assertIsDisplayed(); capture("detail-en")
        click("简体中文")
        reveal(hasContentDescription("照片 1")); rule.onNodeWithContentDescription("照片 1").assertIsDisplayed(); capture("detail-zh")
        click("返回照片"); reveal(hasText("照片", substring = false)); capture("gallery-zh")
        click("资料库"); reveal(hasText("你的资料库")); capture("libraries-zh")
        reveal(hasTestTag("app-settings")); rule.onNodeWithTag("app-settings").performClick()
        rule.onNodeWithText("界面语言").assertIsDisplayed(); rule.onNodeWithText("退出登录").assertIsDisplayed(); capture("settings-zh")
        rule.onNodeWithText("退出登录").performClick(); rule.waitForIdle()
        assertFalse(store.hasSession); assertTrue(store.state.value.previews.isEmpty())
        rule.onNodeWithText("你的资料库").assertDoesNotExist()
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
    @Test fun nativeVideoPlaysPausesSeeksAndClosesInBothLanguages() {
        val api = SyntheticApi().apply {
            originalsAllowed = true
            videoBytes = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("synthetic-video.mp4").use { it.readBytes() }
        }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread {
            rule.activity.setContent { ConnectedApp(store) }
            store.authenticate("+12025550123", "synthetic-password-only")
        }
        click("Open library"); details("1"); click("Open video")
        val reader = store.state.value.video!!
        fun ready(label: String) { rule.waitUntil(30000) {
            rule.onAllNodes(hasText(label) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        } }
        ready("Play")
        rule.onNodeWithText("Play").performScrollTo().performClick()
        rule.waitUntil(15000) {
            positionSeconds() >= 1
        }
        // A competing transient audio focus request must pause, without auto-resume.
        val audio = rule.activity.getSystemService(android.media.AudioManager::class.java)
        val focus = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setOnAudioFocusChangeListener { }.build()
        try {
            assertEquals(android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED, audio.requestAudioFocus(focus))
            ready("Play")
        } finally { audio.abandonAudioFocusRequest(focus) }
        rule.onNodeWithText("Play").assertExists()
        rule.onNodeWithText("Play").performScrollTo().performClick(); ready("Pause")
        rule.onNodeWithText("Pause").performScrollTo().performClick(); ready("Play")
        rule.onNodeWithText("Forward 10s").performScrollTo().performClick(); ready("Play")
        rule.waitUntil(10000) { positionSeconds() >= 10 }
        capture("video-en")
        rule.onNodeWithTag("video-seek").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(3000f) }
        ready("Play")
        rule.waitUntil(10000) { positionSeconds() in 2..4 }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(reader.isClosed); assertNull(store.state.value.video)
        click("简体中文"); click("打开视频"); ready("播放")
        rule.onNodeWithText("播放").performScrollTo().performClick()
        rule.waitUntil(15000) { positionSeconds() >= 1 }
        rule.onNodeWithText("暂停").performScrollTo().performClick(); ready("播放"); capture("video-zh")
        val second = store.state.value.video!!
        rule.onNodeWithText("关闭视频").performScrollTo().performClick(); assertTrue(second.isClosed)
        click("打开视频"); ready("播放")
        val third = store.state.value.video!!
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        assertTrue(third.isClosed); assertNull(store.state.value.video)
        rule.onNodeWithTag("video-player").assertDoesNotExist()
        assertTrue(api.videoReads.size > 3)
        assertTrue(api.videoReads.all { it.second in 1..262144 })
    }
    @Test fun corruptVideoExitsPlayerWithoutRetryOrPrivateResidue() {
        val api = SyntheticApi().apply { originalsAllowed = true; videoBytes = ByteArray(128) { 7 } }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); details("1"); click("Open video")
        rule.waitUntil(30000) { store.state.value.video == null && store.state.value.problem != null }
        assertEquals(Message.MEDIA_UNAVAILABLE, store.state.value.problem?.message)
        assertFalse(store.canRetry()); rule.onNodeWithTag("video-player").assertDoesNotExist()
        val diagnosis = requireNotNull(store.state.value.problem?.playbackFailure)
        assertTrue(diagnosis in listOf(VideoPlaybackFailure.UNSUPPORTED, VideoPlaybackFailure.INVALID_MEDIA))
        reveal(hasText(diagnosis.message(false)))
        rule.onNodeWithText(diagnosis.message(false)).assertIsDisplayed()
        capture("playback-error-en")
        click("简体中文")
        reveal(hasText(diagnosis.message(true)))
        rule.onNodeWithText(diagnosis.message(true)).assertIsDisplayed()
        capture("playback-error-zh")
    }
    @Test fun failedPhotoSwitchRetriesBackIntoFullscreenWithoutRegistrationMessage() {
        val api = SyntheticApi().apply {
            photos = listOf(photo.copy(id = "1", kind = "image"), photo.copy(id = "2", kind = "image"))
            originalsAllowed = true
        }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); details("1"); click("Open original photo")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        rule.runOnIdle { api.transientDetailFailures = 2 }
        rule.onNodeWithTag("photo-fullscreen-next").performClick()
        rule.waitUntil(10000) { store.state.value.problem != null }
        rule.onNodeWithText("Could not load this item. Check the connection and retry.").assertExists()
        assertNotNull(store.state.value.session)
        click("Retry")
        rule.waitUntil(10000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        assertEquals("2", store.state.value.detail?.asset?.id)
        rule.onNodeWithTag("photo-controls").assertDoesNotExist()
        rule.onNodeWithTag("photo-fullscreen-previous").assertIsEnabled()
        rule.runOnIdle { store.background() }
    }
    @Test fun originalViewerZoomCloseAndPrivacyUseSyntheticImageBytes() {
        val api = SyntheticApi().apply { photos = listOf(photo.copy(kind = "image")); originalsAllowed = true }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread {
            rule.activity.setContent { ConnectedApp(store) }
            store.authenticate("+12025550123", "synthetic-password-only")
        }
        click("Open library"); details("1"); click("Open original photo")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        if (rule.onAllNodesWithTag("photo-exit-fullscreen").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("photo-exit-fullscreen").performClick()
        rule.onNodeWithTag("photo-zoom").assertTextEquals("100%")
        rule.onNode(hasText("Zoom in") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithTag("photo-zoom").assertTextEquals("150%")
        rule.onNode(hasText("Fit photo") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithTag("original-image").performTouchInput {
            val delta = androidx.compose.ui.geometry.Offset(35f, 0f)
            pinch(center - delta, center + delta, center - delta * 2f, center + delta * 2f, 300)
        }
        rule.onNodeWithTag("photo-zoom").assertTextContains("%", substring = true)
        rule.onNodeWithTag("photo-zoom").assert(hasText("100%").not())
        rule.onNode(hasText("Fit photo") and hasClickAction()).performScrollTo().performClick()
        rule.onNodeWithTag("original-image").performTouchInput { doubleClick(center) }
        rule.onNodeWithTag("photo-zoom").assertTextEquals("200%")
        rule.onNodeWithTag("original-image").performTouchInput { swipe(center, center + androidx.compose.ui.geometry.Offset(40f, 20f), 200) }
        capture("original-photo-en")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle(); assertNull(store.state.value.originalPhoto)
        rule.onAllNodesWithTag("original-viewer").assertCountEquals(0)
        click("简体中文"); click("打开原始照片")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        if (rule.onAllNodesWithTag("photo-exit-fullscreen").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("photo-exit-fullscreen").performClick()
        rule.onNodeWithTag("photo-zoom").assertTextEquals("100%")
        capture("original-photo-zh")
        rule.onNode(hasText("关闭照片") and hasClickAction()).performScrollTo().performClick()
        rule.waitForIdle(); assertNull(store.state.value.originalPhoto)
        click("打开原始照片")
        rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 }
        if (rule.onAllNodesWithTag("photo-exit-fullscreen").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("photo-exit-fullscreen").performClick()
        rule.runOnUiThread { store.background() }
        rule.waitForIdle(); rule.onAllNodesWithTag("original-viewer").assertCountEquals(0)
        assertNull(store.state.value.originalPhoto); assertTrue(store.state.value.covered)
    }
    @Test fun photoFitFillFullscreenAndSlideshowPreservePageAndClearOnBackground() {
        val api = SyntheticApi().apply {
            photos = (1..3).map { photo.copy(id = it.toString(), kind = "image", taken_at = "2026-01-0$it") }
            originalsAllowed = true; total = 100
        }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); click("Next"); details("1"); click("Open original photo")
        fun ready() { rule.waitUntil(5000) { rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1 } }
        fun mediaClick(label: String) { rule.onNode(hasText(label) and hasClickAction()).performScrollTo().performClick() }
        ready(); rule.onNodeWithTag("photo-exit-fullscreen").performClick(); val original = store.state.value.originalPhoto
        mediaClick("Fill screen"); rule.onNodeWithTag("photo-fit-mode").assertTextEquals("Fill · edges cropped")
        rule.onNodeWithTag("original-image").performTouchInput {
            swipe(center, center + androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.3f), 300)
        }
        val viewport = rule.onNodeWithTag("photo-viewport").fetchSemanticsNode().boundsInWindow
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            for (x in listOf(viewport.left.toInt() + 8, viewport.right.toInt() - 8))
                for (y in listOf(viewport.top.toInt() + 8, viewport.bottom.toInt() - 8))
                    assertNotEquals("Fill pan must reveal image edges, not black gaps", android.graphics.Color.BLACK, image.getPixel(x, y))
            image.recycle()
        }
        mediaClick("Full screen"); rule.onNodeWithTag("photo-exit-fullscreen").assertIsDisplayed()
        rule.onNodeWithTag("photo-controls").assertDoesNotExist(); capture("parity-photo-fullscreen")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertSame(original, store.state.value.originalPhoto)
        rule.onNodeWithTag("photo-controls").assertExists(); mediaClick("Fit photo")
        mediaClick("Start slideshow"); assertTrue(store.state.value.photoSlideshow)
        assertTrue(screenAwake())
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(8200)
        rule.waitUntil(5000) { store.state.value.detail?.asset?.id == "2" }
        // Decoder work uses a real worker thread. Pump frames while yielding wall
        // time instead of letting the spinner advance virtual slideshow time.
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("original-image").fetchSemanticsNodes().size == 1
        }
        assertTrue("Slideshow must still be active on the second photo", store.state.value.photoSlideshow)
        rule.mainClock.autoAdvance = true
        mediaClick("Pause slideshow"); assertFalse(store.state.value.photoSlideshow)
        capture("parity-photo-slideshow")
        mediaClick("Previous photo"); ready(); assertEquals("1", store.state.value.detail?.asset?.id)
        mediaClick("Full screen")
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        rule.onNodeWithTag("original-viewer").assertDoesNotExist()
        assertFalse(store.state.value.photoSlideshow); assertNull(store.state.value.originalPhoto)
        assertFalse(screenAwake())
    }
    @Test fun nativeVideoFitFillAndFullscreenKeepTheSameReaderAndRestoreControls() {
        val api = SyntheticApi().apply {
            originalsAllowed = true
            videoBytes = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("synthetic-video.mp4").use { it.readBytes() }
        }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); details("1"); click("Open video")
        rule.waitUntil(30000) { rule.onAllNodes(hasText("Play") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        val reader = store.state.value.video!!
        fun mediaClick(label: String) { rule.onNode(hasText(label) and hasClickAction()).performScrollTo().performClick() }
        mediaClick("Fill screen"); rule.onNodeWithTag("video-fit-mode").assertTextEquals("Fill · edges cropped")
        mediaClick("Fit video"); mediaClick("Play")
        rule.waitUntil(15000) { positionSeconds() >= 1 }
        mediaClick("Full screen"); rule.onNodeWithTag("video-exit-fullscreen").assertIsDisplayed()
        assertTrue(screenAwake())
        rule.onNodeWithTag("video-controls").assertDoesNotExist()
        assertSame(reader, store.state.value.video); assertFalse(reader.isClosed)
        capture("video-parity-fullscreen")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        assertSame(reader, store.state.value.video); assertFalse(reader.isClosed)
        rule.onNodeWithTag("video-controls").assertExists(); mediaClick("Pause")
        rule.waitUntil(5000) { rule.onAllNodes(hasText("Play") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        assertFalse(screenAwake())
        mediaClick("Full screen"); rule.onNodeWithTag("video-exit-fullscreen").performClick()
        mediaClick("Close video"); assertTrue(reader.isClosed)
        assertTrue(rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
    }
    @Test fun undecodablePhotoStopsSlideshowWithoutAdvancingOrKeepingScreenAwake() {
        var stopped = 0; var advanced = 0
        rule.runOnUiThread { rule.activity.setContent {
            OriginalPhotoViewer(byteArrayOf(1, 2, 3), false, false, {}, PhotoNavigation(1, listOf("1", "2"), 0),
                true, onStopSlideshow = { stopped++ }, onAdvanceSlideshow = { advanced++ })
        } }
        rule.waitUntil(5000) { stopped > 0 }
        rule.onNodeWithText("This image format or size cannot be displayed here.").assertExists()
        assertEquals(0, advanced); assertFalse(screenAwake())
    }
    @Test fun pageJumpRejectsInvalidInputAndClearsOnPrivateLifecycleBoundary() {
        val api = SyntheticApi().apply { total = 1000 }
        val store = ConnectedStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { ConnectedApp(store) }; store.authenticate("+12025550123", "synthetic-password-only") }
        click("Open library"); click("Go to page")
        rule.onNodeWithTag("phone-page-input").performTextReplacement("0")
        rule.onNodeWithTag("phone-page-go").assertIsNotEnabled()
        rule.onNodeWithTag("phone-page-input").performTextReplacement("21")
        rule.onNodeWithTag("phone-page-go").assertIsNotEnabled()
        rule.onNodeWithTag("phone-page-input").performTextReplacement("12")
        rule.onNodeWithTag("phone-page-go").performClick(); rule.waitForIdle()
        assertEquals(12, store.state.value.gallery?.page)
        click("简体中文"); click("跳转页面")
        rule.onNodeWithText("选择第 1 至 20 页").assertIsDisplayed()
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        rule.onNodeWithTag("phone-page-input").assertDoesNotExist()
        assertTrue(store.state.value.covered)
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
        click("Open library"); click("Next"); details("1")
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
        details("1")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        reveal(hasText("第 2 页 · 100 张照片")); rule.onNodeWithText("第 2 页 · 100 张照片").assertIsDisplayed()
        click("退出登录"); assertNull(store.state.value.photoNavigation)
        rule.onAllNodes(hasText("第 2 页 · 第 1/2 张")).assertCountEquals(0)
    }
}
