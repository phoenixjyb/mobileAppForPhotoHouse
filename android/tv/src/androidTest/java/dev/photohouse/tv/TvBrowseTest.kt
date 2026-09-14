package dev.photohouse.tv

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.photohouse.home.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

class TvBrowseTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    @After fun finish() { scope.cancel() }
    @Test fun selectorsRenderCountsFilterGloballyAndKeepAnEmptyResultRecoverable() {
        val preview=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("home-8x8.jpg").use { it.readBytes() }
        val all=listOf(HomeAsset(300,"Waiting for preparation / 准备中",null,null,AssetKind.VIDEO),
            HomeAsset(102,"A quiet afternoon / 午后时光",Preview(8,8,preview.size,"","grid"),Preview(8,8,preview.size,"","display")))
        val api=object:HomeApi {
            override val catalogVersion=3; override val browseEnabled=true
            override suspend fun feed(page:Int):HomeFeed=error("Selection required")
            override suspend fun feed(page:Int,revision:Int?,selection:BrowseSelection):HomeFeed {
                val matching=all.filter { selection.media==BrowseMedia.ALL || it.kind==if(selection.media==BrowseMedia.PHOTOS) AssetKind.PHOTO else AssetKind.VIDEO }
                val ready=matching.count { it.deliveryReady() }
                val items=(if(selection.availability==Availability.READY) matching.filter { it.deliveryReady() } else matching).let {
                    if(selection.order==BrowseOrder.READY_FIRST) it.sortedByDescending { a->a.deliveryReady() } else it
                }
                return HomeFeed(9,"synthetic","Our home / 我们的家",1,50,items.size,false,items,3,BrowseCounts(ready,matching.size))
            }
            override suspend fun preview(asset:HomeAsset,variant:Variant,revision:Int)=preview
        }
        val store=HomeStore(api,scope)
        rule.runOnUiThread { rule.activity.setContent { TvApp(store) };store.foreground() }
        rule.onNodeWithTag("ready-first").performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        rule.onNodeWithTag("ready-first").assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("ready-first").assertIsSelected()
        rule.waitUntil { store.state.value.feed?.items?.firstOrNull()?.id==102 }
        rule.onNodeWithTag("ready-only").performScrollTo().performClick()
        rule.onNodeWithTag("ready-only").assertIsSelected()
        rule.onNodeWithTag("asset-300").assertDoesNotExist()
        rule.onNodeWithTag("asset-102").assertExists()
        snapshot("ready-browse-en.png")
        rule.onNodeWithTag("language").performClick()
        rule.onNodeWithTag("ready-only").assertTextContains("仅显示已就绪",substring=true)
        snapshot("ready-browse-zh.png")
        rule.onNodeWithTag("browse-video").performScrollTo().performClick()
        rule.waitUntil { store.state.value.feed?.total==0 }
        rule.onNodeWithText("暂无已就绪内容。可显示全部，或在发布更多媒体后刷新。").assertExists()
        rule.onNodeWithTag("ready-only").performScrollTo().performClick()
        rule.onNodeWithTag("asset-300").assertExists()
        rule.runOnUiThread { store.background() }
        rule.onNodeWithTag("covered").assertExists()
    }
    private fun snapshot(name:String) {
        rule.waitForIdle()
        rule.runOnUiThread {
            val view=rule.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap));File(rule.activity.filesDir,name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        }
    }
}
