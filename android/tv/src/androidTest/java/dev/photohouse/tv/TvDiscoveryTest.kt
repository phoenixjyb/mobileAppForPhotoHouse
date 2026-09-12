package dev.photohouse.tv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.photohouse.home.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Synthetic editor/remote evidence; callbacks are not proof of a deployed search service. */
class TvDiscoveryTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Before fun remoteInput() { InstrumentationRegistry.getInstrumentation().setInTouchMode(false) }
    @After fun finish() { scope.cancel() }
    private val options = DiscoveryOptions(DiscoveryField.entries.toSet(),
        people = (1..7).map { DiscoveryChoice("person-$it", "Sample $it", listOf("示例 $it")) }, years = listOf(2025, 2024),
        themes = listOf(DiscoveryChoice("theme-1", "Celebrations", listOf("庆祝"))),
        topics = listOf(DiscoveryChoice("topic-1", "Outdoors", listOf("户外"))),
        tags = listOf(DiscoveryChoice("tag-1", "Picnic", listOf("野餐"))),
        places = listOf(DiscoveryChoice("place-1", "Sample garden", listOf("示例花园"))), peopleAll = true, tagsAll = true)
    private val applied = mutableListOf<DiscoveryDraft>()
    private var closed = false
    private fun install(info: DiscoveryOptions? = options, zh: Boolean = false, initial: DiscoveryDraft = DiscoveryDraft()) {
        rule.runOnUiThread { rule.activity.setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Ink, onBackground = Cream, onSurface = Cream)) {
                Surface(color = Ink) { Box(Modifier.fillMaxSize().padding(32.dp)) {
                    TvDiscovery(info, zh, initial, { closed = true }, { applied += it })
                } }
            }
        } }; rule.waitForIdle()
        rule.waitUntil(10000) { rule.activity.hasWindowFocus() && rule.onAllNodes(hasTestTag("discovery-back") and isFocused()).fetchSemanticsNodes().size == 1 }
    }
    private fun reveal(tag: String): SemanticsNodeInteraction {
        rule.onNodeWithTag("discovery").performScrollToNode(hasTestTag(tag))
        return rule.onNodeWithTag(tag)
    }
    private fun click(tag: String) { reveal(tag).performClick(); rule.waitForIdle() }
    private fun key(code: Int) {
        rule.waitUntil(10000) { rule.activity.hasWindowFocus() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code); rule.waitForIdle()
    }
    private fun capture(name: String) {
        rule.waitForIdle(); rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(rule.activity.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Test fun bilingualShortcutSubmitsStableIdentityWithNativeOk() {
        install()
        rule.onNodeWithText("示例 1", useUnmergedTree = true).assertExists()
        capture("discovery-home")
        rule.onNodeWithTag("quick-person-person-1").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitUntil(10000) { rule.onAllNodes(hasTestTag("quick-person-person-1") and isFocused()).fetchSemanticsNodes().size == 1 }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(listOf(DiscoveryDraft(people = setOf("person-1"))), applied)
        assertFalse(closed)
    }
    @Test fun advancedCombinesCriteriaAndBlocksInvalidDatesUntilExplicitApply() {
        install(); click("advanced-search")
        reveal("search-text").performTextInput("picnic")
        click("person-person-1"); click("person-person-2"); click("people-match-ALL")
        reveal("date-from").performTextInput("2025-02-30")
        reveal("apply-search").assertIsNotEnabled()
        assertTrue(applied.isEmpty())
        reveal("date-from").performTextClearance(); rule.onNodeWithTag("date-from").performTextInput("2025-02-01")
        reveal("date-through").performTextInput("2025-02-28")
        click("tag-tag-1"); click("place-place-1"); click("media-VIDEO")
        capture("discovery-advanced")
        reveal("apply-search").assertIsEnabled().performClick()
        assertEquals(DiscoveryDraft(text = "picnic", people = setOf("person-1", "person-2"), peopleMatch = MatchMode.ALL,
            from = "2025-02-01", through = "2025-02-28", tags = setOf("tag-1"), place = "place-1", media = AssetKind.VIDEO), applied.single())
    }
    @Test fun cancellationRestoresDraftAndFocusThenBackLeavesExplore() {
        val initial = DiscoveryDraft(people = setOf("person-1"))
        install(initial = initial); click("advanced-search"); click("person-person-2")
        reveal("cancel-search"); key(KeyEvent.KEYCODE_BACK)
        rule.waitUntil(10000) { rule.onAllNodes(hasTestTag("advanced-search") and isFocused()).fetchSemanticsNodes().size == 1 }
        rule.onNodeWithTag("advanced-search").assertIsFocused()
        click("advanced-search"); reveal("filter-summary").assertTextEquals("1 active filters · all categories must match")
        click("clear-search"); reveal("apply-search").performClick()
        assertEquals(DiscoveryDraft(), applied.single())
        click("cancel-search"); key(KeyEvent.KEYCODE_BACK); assertTrue(closed)
    }
    @Test fun chineseIndexCoverageAndYearPickerAreExplicit() {
        install(options.copy(partialIndex = true), zh = true)
        rule.onNodeWithTag("discovery-partial").assertExists(); capture("discovery-zh")
        click("explore-dates"); click("year-2024")
        reveal("date-from").assertTextContains("2024-01-01")
        reveal("date-through").assertTextContains("2024-12-31")
        capture("discovery-dates")
        click("apply-search"); assertEquals(DiscoveryDraft().year(2024), applied.single())
    }
    @Test fun unavailableProductionFeedDoesNotInventSearchOrRetainEditorInBackground() {
        var reads = 0
        val api = object : HomeApi {
            override suspend fun feed(page: Int): HomeFeed { reads++; return HomeFeed(1, "synthetic", "Sample library", 1, 50, 0, false, emptyList()) }
            override suspend fun preview(asset: HomeAsset, variant: Variant, revision: Int): ByteArray? = error("No media request")
        }
        val store = HomeStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { TvApp(store) }; store.foreground() }; rule.waitForIdle()
        rule.onNodeWithTag("explore").performClick()
        rule.onNodeWithTag("discovery-unavailable").assertExists(); capture("discovery-unavailable")
        click("advanced-search"); reveal("apply-search").assertIsNotEnabled()
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertEquals(1, reads)
        click("cancel-search"); key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("explore").assertIsFocused()
        rule.onNodeWithTag("explore").performClick(); click("advanced-search")
        rule.runOnUiThread { store.background() }; rule.waitForIdle()
        rule.onNodeWithTag("discovery").assertDoesNotExist()
        rule.onNodeWithTag("covered").assertExists()
    }
}
