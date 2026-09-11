package dev.photohouse.tv

import android.view.KeyEvent as AndroidKey
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.photohouse.connected.core.*
import kotlinx.coroutines.delay

private val Ink = Color(0xFF10231F)
private val Cream = Color(0xFFF6ECD9)
private val Gold = Color(0xFFEBC384)

@Composable internal fun TvButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Button(onClick, modifier.onFocusChanged { focused = it.isFocused }, enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (focused) 3.dp else 1.dp, if (focused) Gold else Color(0xFF496258)),
        colors = ButtonDefaults.buttonColors(containerColor = if (focused) Gold else Color(0xFF233C32),
            contentColor = if (focused) Ink else Cream)) { Text(label) }
}

@Composable fun TvApp(store: ConnectedStore?) {
    var language by remember { mutableStateOf("system") }
    val zh = language == "zh" || language == "system" && LocalConfiguration.current.locales[0].language == "zh"
    fun t(en: String, cn: String) = if (zh) cn else en
    val state = store?.state?.collectAsState()?.value ?: LiveState()
    val viewer = state.detail != null || state.photoNavigation != null
    var playing by remember(state.library) { mutableStateOf(false) }
    var originals by remember(state.library) { mutableStateOf(false) }
    var immersive by remember { mutableStateOf(false) }
    var captions by remember { mutableStateOf(false) }
    var lastAsset by remember(state.library) { mutableStateOf<String?>(null) }
    val first = remember { FocusRequester() }
    val route = when { state.covered -> "covered"; store == null -> "setup"; state.session == null -> "connection"; state.library == null -> "libraries"; viewer -> "viewer"; else -> "grid" }
    val grid = rememberLazyGridState()
    val view = LocalView.current
    DisposableEffect(playing, state.covered, view) {
        view.keepScreenOn = playing && !state.covered
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(route, state.gallery?.page) {
        // Re-request after a remote navigation transition, but not on every thumbnail update.
        if (route == "grid") {
            val index = state.gallery?.items?.indexOfFirst { it.id == lastAsset } ?: -1
            if (index >= 0) grid.scrollToItem(index)
        }
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    LaunchedEffect(state.covered, state.problem, viewer, state.session) {
        if (state.covered || state.problem != null || !viewer || state.session == null) {
            playing = false; originals = false; immersive = false
        }
    }
    LaunchedEffect(originals, state.detail?.asset?.id, state.busy, state.problem, state.covered) {
        val detail = state.detail
        if (originals && detail != null && !state.busy && state.problem == null && !state.covered &&
            !state.viewingOriginal && detail.originals_allowed && detail.asset.kind == "image") store?.openOriginalPhoto()
    }
    LaunchedEffect(playing, state.detail?.asset?.id, state.busy, state.problem, state.covered, state.viewingOriginal) {
        if (playing && !state.busy && state.detail != null && state.problem == null && !state.covered) {
            delay(8000)
            val nav = state.photoNavigation
            if (nav != null && nav.index < nav.assetIds.lastIndex) store?.adjacentPhoto(1) else playing = false
        }
    }
    fun back() {
        playing = false; originals = false
        if (viewer) store?.backToPhotos() else store?.libraries()
    }
    BackHandler(state.library != null && !state.covered) { if (immersive) immersive = false else back() }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Ink, onBackground = Cream, onSurface = Cream)) {
        if (captions && viewer && !state.covered && state.captions != null) {
            val closeFocus = remember { FocusRequester() }
            AlertDialog(onDismissRequest = { captions = false },
                title = { Text(t("Photo captions", "照片说明")) },
                text = { Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.captions!!.items.isEmpty()) Text(t("No captions yet.", "暂无说明。"))
                    state.captions!!.items.forEach { Text(it.text) }
                    if (state.captions!!.has_more) Text(t("More captions are available on the server.", "服务器上还有更多说明。"))
                } },
                confirmButton = {
                    TvButton(t("Close", "关闭"), Modifier.focusRequester(closeFocus)) { captions = false }
                    LaunchedEffect(Unit) { closeFocus.requestFocus() }
                })
        }
        Surface(Modifier.fillMaxSize(), color = Ink) {
            if (immersive && viewer && !state.covered && state.session != null) {
                val remote = remember { FocusRequester() }
                LaunchedEffect(Unit) { remote.requestFocus() }
                Box(Modifier.fillMaxSize().background(Color.Black).testTag("immersive")
                    .focusRequester(remote).onPreviewKeyEvent {
                        if (it.nativeKeyEvent.action != AndroidKey.ACTION_DOWN) false
                        else when (it.nativeKeyEvent.keyCode) {
                            AndroidKey.KEYCODE_DPAD_LEFT -> { playing = false; store?.adjacentPhoto(-1); true }
                            AndroidKey.KEYCODE_DPAD_RIGHT -> { playing = false; store?.adjacentPhoto(1); true }
                            AndroidKey.KEYCODE_DPAD_CENTER, AndroidKey.KEYCODE_ENTER, AndroidKey.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (!state.busy && state.problem == null) playing = !playing
                                true
                            }
                            else -> false
                        }
                    }.focusable()) {
                    val bytes = if (state.viewingOriginal) state.originalPhoto else state.detail?.asset?.id?.let { state.previews[it] }
                    TvImage(bytes, t("Photo", "照片") + " ${state.detail?.asset?.id.orEmpty()}", Modifier.fillMaxSize(), t("Preview unavailable", "预览不可用"))
                    Text(t("← → Photos   ·   OK Play/Pause   ·   Back Controls", "← → 切换照片   ·   确定 播放/暂停   ·   返回 控制栏"),
                        Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.65f)).padding(8.dp), style = MaterialTheme.typography.labelSmall)
                }
                return@Surface
            }
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp, vertical = 20.dp).testTag("tv-screen")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("PhotoHouse", "拾光相册"), fontFamily = FontFamily.Serif, style = MaterialTheme.typography.headlineMedium)
                        Text(t("A little closer to home · TV preview", "把回忆带回家 · 电视预览版"), style = MaterialTheme.typography.labelMedium, color = Gold)
                    }
                    TvButton(if (zh) "English" else "简体中文", Modifier.testTag("language")) { language = if (zh) "en" else "zh" }
                    if (state.session != null && !state.covered) TvButton(t("Disconnect", "断开连接"), Modifier.testTag("logout")) { playing = false; originals = false; store?.logout() }
                }
                Spacer(Modifier.height(12.dp))
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.problem?.let { problem ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(problemText(problem.message, zh), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        var now by remember(problem) { mutableLongStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(problem) { while (now < problem.retryAtMillis) { delay(500); now = System.currentTimeMillis() } }
                        if (store?.canRetry(now) == true) TvButton(t("Retry", "重试")) { store.retry() }
                    }
                }
                when {
                    store == null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        Text(t("Your family album, on the big screen.", "在大屏幕上，重温家里的故事。"), style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(t("Server setup needed. Ask for a TV build configured for your protected home server.", "需要配置服务器。请获取已配置家庭安全服务器的电视版本。"), Modifier.testTag("setup"))
                        TvButton(if (zh) "English" else "简体中文", Modifier.focusRequester(first)) { language = if (zh) "en" else "zh" }
                    }
                    state.covered -> Column {
                        Text(t("Private content is covered.", "私人内容已隐藏。"), Modifier.testTag("covered"))
                        TvButton(t("Recheck session", "重新验证会话"), Modifier.focusRequester(first), !state.busy) { store.foreground() }
                        TvButton(t("Disconnect locally", "在本机断开")) { store.logout() }
                    }
                    state.session == null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        Text(t("This display is not connected yet.", "此屏幕尚未连接。"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("home-access-needed"))
                        Spacer(Modifier.height(16.dp))
                        Text(t("The household owner needs to enable home TV access. No personal sign-in is needed on this screen.", "请由家庭主人启用电视访问。此屏幕无需个人登录。"))
                        TvButton(if (zh) "English" else "简体中文", Modifier.focusRequester(first)) { language = if (zh) "en" else "zh" }
                    }
                    state.library == null -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { Text(t("Your libraries", "你的资料库"), style = MaterialTheme.typography.headlineSmall) }
                        val available = state.session!!.memberships.indexOfFirst { it.available }
                        items(state.session!!.memberships.size) { index ->
                            val member = state.session!!.memberships[index]
                            TvButton(member.library_id + if (member.available) "" else t(" · Unavailable", " · 不可访问"),
                                if (index == available) Modifier.focusRequester(first).testTag("library-$index") else Modifier.testTag("library-$index"), member.available) { store.selectLibrary(member.library_id) }
                        }
                        if (available < 0) item { TvButton(t("No available library — disconnect", "暂无可访问的资料库 — 断开"), Modifier.focusRequester(first)) { store.logout() } }
                    }
                    viewer -> {
                        val detail = state.detail
                        val nav = state.photoNavigation
                        val bytes = if (state.viewingOriginal) state.originalPhoto else detail?.asset?.id?.let { state.previews[it] }
                        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black).testTag("viewer")) {
                            TvImage(bytes, t("Photo", "照片") + " ${detail?.asset?.id.orEmpty()}", Modifier.fillMaxSize(), t("Preview unavailable", "预览不可用"))

                        }
                        // Toolbar arrows move focus; immersive mode maps arrows to photos.
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TvButton(t("Photos", "照片"), Modifier.focusRequester(first)) { back() }
                            TvButton(t("Previous", "上一张"), enabled = !state.busy && nav != null && nav.index > 0) { playing = false; store.adjacentPhoto(-1) }
                            TvButton(if (playing) t("Pause", "暂停") else t("Play page", "播放本页"), Modifier.testTag("slideshow").onPreviewKeyEvent {
                                if (it.nativeKeyEvent.action != AndroidKey.ACTION_DOWN) false
                                else when (it.nativeKeyEvent.keyCode) {
                                    AndroidKey.KEYCODE_MEDIA_PLAY_PAUSE -> { playing = !playing; true }
                                    else -> false
                                }
                            }, enabled = detail != null && !state.busy && state.problem == null && nav != null && (playing || nav.index < nav.assetIds.lastIndex)) { playing = !playing }
                            TvButton(t("Next", "下一张"), enabled = !state.busy && nav != null && nav.index < nav.assetIds.lastIndex) { playing = false; store.adjacentPhoto(1) }
                            TvButton(t("Full screen", "全屏"), Modifier.testTag("fullscreen")) { immersive = true }
                            TvButton(t("Captions", "说明")) { captions = !captions }
                            if (detail?.originals_allowed == true && detail.asset.kind == "image") {
                                TvButton(if (originals) t("Use previews", "使用预览") else t("Use originals", "使用原图"), Modifier.testTag("quality"), !state.busy) {
                                    originals = !originals
                                    if (!originals) store.closeOriginalPhoto()
                                }
                            }
                        }
                        Text((nav?.let { "${it.index + 1} / ${it.assetIds.size}  ·  " } ?: "") +
                            if (state.viewingOriginal) t("Original · up to 4K", "原图 · 最高 4K") else t("Cached preview · 8 seconds per slide", "缓存预览 · 每张 8 秒"), style = MaterialTheme.typography.labelSmall)
                    }
                    else -> {
                        val gallery = state.gallery
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            TvButton(t("Libraries", "资料库"), if (gallery?.items.isNullOrEmpty()) Modifier.focusRequester(first) else Modifier) { store.libraries() }
                            TvButton(t("Refresh", "刷新"), enabled = !state.busy) { store.loadPage(gallery?.page ?: 1) }
                            TvButton(t("Previous page", "上一页"), enabled = !state.busy && gallery != null && gallery.page > 1) { store.loadPage(gallery!!.page - 1) }
                            TvButton(t("Next page", "下一页"), enabled = !state.busy && gallery != null && gallery.page.toLong() * gallery.page_size < gallery.total && gallery.page < 100000) { store.loadPage(gallery!!.page + 1) }
                        }
                        if (gallery != null && gallery.items.isEmpty()) Text(t("No photos here yet.", "这里还没有照片。"))
                        LazyVerticalGrid(GridCells.Adaptive(180.dp), Modifier.weight(1f).testTag("grid"), state = grid,
                            contentPadding = PaddingValues(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(gallery?.items.orEmpty(), key = { it.id }) { asset ->
                                val target = gallery?.items?.firstOrNull { it.id == lastAsset }?.id ?: gallery?.items?.firstOrNull()?.id
                                var focused by remember { mutableStateOf(false) }
                                OutlinedButton({ lastAsset = asset.id; store.openAsset(asset) },
                                    Modifier.fillMaxWidth().height(158.dp).then(if (asset.id == target) Modifier.focusRequester(first) else Modifier)
                                        .onFocusChanged { focused = it.isFocused }.testTag("asset-${asset.id}"),
                                    shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(6.dp),
                                    border = BorderStroke(if (focused) 3.dp else 1.dp, if (focused) Gold else Color(0xFF355044))) {
                                    Column(Modifier.fillMaxSize()) {
                                        TvImage(state.previews[asset.id], t("Photo", "照片") + " ${asset.id}", Modifier.fillMaxWidth().weight(1f), t("Preview unavailable", "预览不可用"), maxPixels = 262144)
                                        Text(asset.taken_at ?: t("Date unknown", "日期未知"), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                        if (asset.kind != "image") Text(t("Preview only", "仅预览"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun problemText(message: Message, zh: Boolean): String {
    fun t(en: String, cn: String) = if (zh) cn else en
    return when (message) {
        Message.SIGNED_OUT_LOCAL -> t("Disconnected locally; server acknowledgement unavailable.", "已在本机断开；尚未获得服务器确认。")
        Message.SIGNED_OUT_CONFIRMED -> t("Disconnected; server session revoked.", "已断开；服务器会话已撤销。")
        Message.SESSION_ENDED -> t("Display access ended. The household owner needs to reconnect it.", "屏幕访问已结束，请由家庭主人重新连接。")
        Message.ACCESS_DENIED, Message.CLOSED -> t("Access unavailable. The household owner needs to check this display.", "无法访问，请由家庭主人检查此屏幕。")
        Message.INVALID_INPUT -> t("Display setup is invalid. Contact the household owner.", "屏幕配置无效，请联系家庭主人。")
        Message.RATE_LIMITED -> t("Too many requests. Wait before retrying.", "请求过多，请稍后重试。")
        Message.TLS_ERROR -> t("Cannot verify the secure server connection.", "无法验证服务器的安全连接。")
        Message.TOO_LARGE -> t("Image exceeds this viewer's memory or transfer limit.", "图片超出此查看器的内存或传输限制。")
        Message.INVALID_RESPONSE -> t("Server response could not be displayed safely.", "无法安全显示服务器响应。")
        Message.MEDIA_UNAVAILABLE -> t("Media unavailable.", "媒体不可用。")
        Message.UNAVAILABLE -> t("Server unavailable. Retry when the connection returns.", "服务器不可用，请在网络恢复后重试。")
    }
}
