package dev.photohouse.tv

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.photohouse.home.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.io.ByteArrayOutputStream
import java.io.File

class TvOnDemandTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @After fun finish() { scope.cancel() }
    @Test fun originalQualityPhotoOpensWithRemoteControlsAndClearsOnBackground() {
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff607b66.toInt()) }
        fun encode(format: Bitmap.CompressFormat) = ByteArrayOutputStream().use { out -> bitmap.compress(format, 90, out); out.toByteArray() }
        val original = encode(Bitmap.CompressFormat.PNG); val preview = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("home-8x8.jpg").use { it.readBytes() }; bitmap.recycle()
        val p = Preview(0, 0, Variant.DISPLAY.bytes, "", "/home/v3/assets/1/preview?variant=display&revision=1", true)
        val asset = HomeAsset(1, "A quiet afternoon / 午后时光", null, p, original = HomeOriginal("image/png", original.size, 1200, 800, "/home/v3/assets/1/original?revision=1"))
        val api = object : HomeApi {
            override val catalogVersion = 3
            override suspend fun feed(page: Int) = HomeFeed(1,"synthetic","Our home",1,50,1,false,listOf(asset),3)
            override suspend fun preview(asset: HomeAsset, variant: Variant, revision: Int) = preview
            override suspend fun original(asset: HomeAsset, revision: Int) = original
        }
        val store = HomeStore(api, scope)
        rule.runOnUiThread { rule.activity.setContent { TvApp(store) }; store.foreground() }
        rule.onNodeWithTag("asset-1").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("tv-image").fetchSemanticsNodes().size == 1 }
        rule.onNodeWithTag("photo-original").performScrollTo().performClick()
        rule.waitUntil(10000) { store.state.value.originalQuality && !store.state.value.busy }
        rule.onNodeWithTag("photo-original").assertIsNotEnabled()
        rule.onNodeWithTag("photo-zoom").performScrollTo().performClick()
        rule.onNodeWithTag("immersive").assertExists()
        rule.waitForIdle()
        rule.runOnUiThread {
            val view=rule.activity.window.decorView;val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image));File(rule.activity.filesDir,"on-demand-tv.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            store.background()
        }
        rule.onNodeWithTag("covered").assertExists(); assertNull(store.state.value.display)
    }
}
