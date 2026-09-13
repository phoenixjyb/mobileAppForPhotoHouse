package dev.photohouse.tv

import android.view.KeyEvent as AndroidKey
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.photohouse.home.*
import kotlinx.coroutines.delay

internal val Ink = Color(0xFF10231F)
internal val Cream = Color(0xFFF6ECD9)
internal val Gold = Color(0xFFEBC384)
internal val Moss = Color(0xFF233C32)
internal val Muted = Color(0xFFC0C8BE)
internal val Edge = Color(0xFF496258)

@Composable internal fun TvButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    // TV focus must survive a touch/air-mouse interaction and return to the remote.
    Button(onClick, modifier.heightIn(min = 52.dp).focusProperties { canFocus = enabled }.onFocusChanged { focused = it.isFocused }, enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (focused) 3.dp else 1.dp, if (focused) Gold else Edge),
        colors = ButtonDefaults.buttonColors(containerColor = if (focused) Gold else Moss,
            contentColor = if (focused) Ink else Cream)) { Text(label) }
}

@Composable fun TvApp(browseStore: HomeStore?, discovery: DiscoveryController? = null) {
    val discoveryState = discovery?.state?.collectAsState()?.value ?: DiscoveryState()
    val store = discoveryState.results ?: browseStore
    var language by remember { mutableStateOf("system") }
    val zh = language == "zh" || language == "system" && LocalConfiguration.current.locales[0].language == "zh"
    fun t(en: String, cn: String) = if (zh) cn else en
    // A result store is replaced on each query. Do not briefly reuse its covered/error
    // state while Compose subscribes to the browse store after clearing the search.
    val state = key(store) { store?.state?.collectAsState()?.value ?: HomeState() }
    val viewer = state.asset != null
    var exploring by remember { mutableStateOf(false) }
    var restoreExplore by remember { mutableStateOf(false) }
    val exploreFocus = remember { FocusRequester() }
    var playing by remember(state.feed?.id) { mutableStateOf(false) }
    var immersive by remember { mutableStateOf(false) }
    var transform by remember(state.selected, state.feed?.revision, state.covered) { mutableStateOf(PhotoTransform()) }
    var playbackFailure by remember(store, state.selected, state.feed?.revision, state.covered) { mutableStateOf<TvPlaybackFailure?>(null) }
    var hintTick by remember { mutableStateOf(0) }
    var showHint by remember { mutableStateOf(true) }
    LaunchedEffect(immersive, hintTick) { showHint = true; if (immersive) { delay(4000); showHint = false } }
    var pages by remember(state.feed?.id, state.feed?.revision) { mutableStateOf(false) }
    var captions by remember { mutableStateOf(false) }
    var lastAsset by remember(state.feed?.id) { mutableStateOf<Int?>(null) }
    val first = remember { FocusRequester() }
    val route = when { state.covered -> "covered"; store == null -> "setup"; exploring -> "explore"; state.feed == null -> "connection"; state.video != null -> "video"; viewer -> "viewer"; else -> "grid" }
    val grid = rememberLazyGridState()
    val focusAsset = state.feed?.items?.firstOrNull { it.id == lastAsset && it.canOpen() }
        ?: state.feed?.items?.firstOrNull { it.canOpen() }
    val view = LocalView.current
    val focusedWindow = LocalWindowInfo.current.isWindowFocused
    DisposableEffect(playing, state.covered, view) {
        view.keepScreenOn = playing && !state.covered
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(route, state.feed?.page, store, focusedWindow) {
        if (!focusedWindow) return@LaunchedEffect
        // Re-request after a remote navigation transition, but not on every thumbnail update.
        if (route == "grid") {
            val index = state.feed?.items?.indexOfFirst { it.id == focusAsset?.id } ?: -1
            grid.scrollToItem(index.coerceAtLeast(0))
        }
        withFrameNanos { }
        if (route != "explore") runCatching { if (route == "grid" && restoreExplore) { exploreFocus.requestFocus(); restoreExplore = false } else first.requestFocus() }
    }
    LaunchedEffect(state.covered, state.problem, viewer, state.disconnected, state.asset?.kind) {
        if (state.covered || state.disconnected || state.problem == HomeError.DENIED) { exploring = false; restoreExplore = false }
        if (discoveryState.results != null) state.problem?.let { discovery?.invalidateResults(it) }
        if (state.covered || state.problem != null || !viewer || state.disconnected || state.asset?.kind != AssetKind.PHOTO) {
            playing = false; immersive = false; captions = false
        }
    }
    LaunchedEffect(playing, state.selected, state.busy, state.problem, state.covered, state.displayMissing) {
        if (playing && !state.busy && viewer && state.problem == null && !state.covered) {
            if (state.displayMissing) playing = false
            else {
                delay(8000)
                if (state.adjacentAsset(1) != null) store?.adjacentPhoto(1) else playing = false
            }
        }
    }
    fun back() { playing = false; store?.backToPhotos() }
    BackHandler(viewer && !state.covered) { if (transform.zoom > 1f) transform = PhotoTransform() else if (immersive) immersive = false else back() }
    BackHandler(!viewer && !exploring && !state.covered && discoveryState.query != null) {
        restoreExplore = true; discovery?.clearResults()
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Ink, onBackground = Cream, onSurface = Cream)) {
        if (pages && state.feed != null && !state.covered && !viewer) {
            PageJump(state.feed!!.page, state.feed!!.total, zh, { pages = false }, { pages = false; store?.loadPage(it) })
        }
        if (captions && viewer && !state.covered) {
            val closeFocus = remember { FocusRequester() }
            AlertDialog(onDismissRequest = { captions = false },
                title = { Text(if ((state.feed?.version ?: 1) >= 2) t("Asset details", "内容信息") else t("Photo captions", "照片说明")) },
                text = { Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    Text(state.asset?.caption?.ifEmpty { t("No captions yet.", "暂无说明。") }.orEmpty())
                } },
                confirmButton = {
                    TvButton(t("Close", "关闭"), Modifier.focusRequester(closeFocus)) { captions = false }
                    val focusedWindow = LocalWindowInfo.current.isWindowFocused
                    LaunchedEffect(focusedWindow) {
                        if (focusedWindow) { withFrameNanos { }; closeFocus.requestFocus() }
                    }
                })
        }
        Surface(Modifier.fillMaxSize(), color = Ink) {
            val video = state.video
            if (video != null && !state.covered && state.problem == null) {
                TvVideoPlayer(video, zh, { if (store?.state?.value?.video === video) store.closeVideo() },
                    { reason -> if (store?.state?.value?.video === video) { playbackFailure = reason; store.videoPlaybackFailed() } })
                return@Surface
            }
            if (immersive && viewer && !state.covered && state.feed != null) {
                val remote = remember { FocusRequester() }
                LaunchedEffect(Unit) { remote.requestFocus() }
                Box(Modifier.fillMaxSize().background(Color.Black).testTag("immersive")
                    .focusRequester(remote).onPreviewKeyEvent {
                        if (it.nativeKeyEvent.action != AndroidKey.ACTION_DOWN) false
                        else { hintTick++; when (it.nativeKeyEvent.keyCode) {
                            AndroidKey.KEYCODE_DPAD_LEFT -> { playing = false; if (transform.zoom > 1f) transform = transform.pan(0.2f, 0f) else store?.adjacentPhoto(-1); true }
                            AndroidKey.KEYCODE_DPAD_RIGHT -> { playing = false; if (transform.zoom > 1f) transform = transform.pan(-0.2f, 0f) else store?.adjacentPhoto(1); true }
                            AndroidKey.KEYCODE_DPAD_UP -> { transform = transform.pan(0f, 0.2f); true }
                            AndroidKey.KEYCODE_DPAD_DOWN -> { transform = transform.pan(0f, -0.2f); true }
                            AndroidKey.KEYCODE_DPAD_CENTER, AndroidKey.KEYCODE_ENTER -> {
                                playing = false; transform = transform.nextZoom(); true
                            }
                            AndroidKey.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                transform = PhotoTransform()
                                if (!state.busy && state.problem == null) playing = !playing
                                true
                            }
                            else -> false
                        }
                    } }.focusable()) {
                    val bytes = state.display
                    TvImage(bytes, t("Photo", "照片") + " ${state.selected ?: ""}", Modifier.fillMaxSize(), if (state.asset?.kind == AssetKind.VIDEO) t("Video", "视频") else unavailableText(state.asset?.displayUnavailable, zh), transform = transform, loading = state.busy, original = state.originalQuality)
                    if (showHint) Text("${transform.zoom.toInt()}× · " + if (transform.zoom > 1f)
                        t("Arrows Pan · OK Zoom · Back Reset", "方向键 移动 · 确定 缩放 · 返回 还原") else
                        t("← → Photos · OK Zoom · Back Controls", "← → 切换照片 · 确定 缩放 · 返回 控制栏"),
                        Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.65f)).padding(8.dp), style = MaterialTheme.typography.labelSmall)
                }
                return@Surface
            }
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp, vertical = 20.dp).testTag("tv-screen")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("PhotoHouse", "拾光相册"), fontFamily = FontFamily.Serif, style = MaterialTheme.typography.titleLarge)
                        Text(t("A little closer to home", "把回忆带回家") + " · v${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    TvButton(if (zh) "English" else "简体中文", Modifier.testTag("language")) { language = if (zh) "en" else "zh" }
                    if (state.feed != null && !state.covered) TvButton(t("Disconnect", "断开连接"), Modifier.testTag("disconnect")) { playing = false; discovery?.background(); browseStore?.disconnect() }
                }
                Spacer(Modifier.height(12.dp))
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.problem?.let { problem ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (problem == HomeError.CHANGED && discoveryState.query != null)
                            t("The library changed. Edit filters to start a fresh search.", "媒体库已更新，请修改条件后重新搜索。") else problemText(problem, zh),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                when {
                    store == null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        Text(t("Your family album, on the big screen.", "在大屏幕上，重温家里的故事。"), style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(t("Server setup needed. Ask for a TV build configured for your home photo feed.", "需要配置服务器。请获取已配置家庭照片源的电视版本。"), Modifier.testTag("setup"))
                        TvButton(if (zh) "English" else "简体中文", Modifier.focusRequester(first)) { language = if (zh) "en" else "zh" }
                    }
                    state.covered -> Column {
                        Text(t("Private content is covered.", "私人内容已隐藏。"), Modifier.testTag("covered"))
                    }
                    exploring -> TvDiscovery(discoveryState.snapshot?.options, zh, discoveryState.query ?: DiscoveryDraft(),
                        onClose = { discovery?.close(); exploring = false; restoreExplore = true },
                        onApply = if (discoveryState.snapshot != null && !discoveryState.loading && discoveryState.problem == null) {
                            { query -> discovery?.search(query); exploring = false }
                        } else null,
                        loading = discoveryState.loading, problem = discoveryState.problem,
                        onRetry = { discovery?.open() }, more = discoveryState.snapshot?.nextPages?.keys.orEmpty(),
                        onMore = { discovery?.more(it) })
                    state.feed == null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                        Text(if (state.disconnected) t("Display disconnected", "屏幕已断开") else if (state.problem != null) t("Home photos unavailable", "暂时无法查看家庭照片") else t("Connecting to your home photos…", "正在连接家庭照片…"),
                            style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("home-access-needed"))
                        Spacer(Modifier.height(16.dp))
                        Text(t("No personal sign-in is needed on this screen.", "此屏幕无需个人登录。"))
                        if (state.disconnected) TvButton(t("Reconnect", "重新连接"), Modifier.focusRequester(first)) { store.reconnect() }
                        else TvButton(t("Retry", "重试"), Modifier.focusRequester(first), !state.busy) { store.retry() }
                        if (discoveryState.query != null) {
                            TvButton(t("Edit filters", "修改条件"), Modifier.testTag("edit-search")) { exploring = true; discovery?.open() }
                            TvButton(t("Clear search", "清除搜索"), Modifier.testTag("clear-results")) { discovery?.clearResults() }
                        }
                    }
                    viewer -> {
                        val bytes = state.display
                        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black).testTag("viewer")) {
                            if (state.videoFailed) Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).focusable().padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Text(state.mediaProblem?.let { mediaProblemText(it, zh) } ?: playbackFailure?.message(zh) ?: t("Playback unavailable. Retry or choose another video. [TV-READ]", "暂时无法播放。请重试或选择其他视频。[TV-READ]"), Modifier.testTag("video-error"))
                            } else TvImage(bytes, t("Photo", "照片") + " ${state.selected ?: ""}", Modifier.fillMaxSize(), if (state.asset?.kind == AssetKind.VIDEO) t("Video", "视频") else unavailableText(state.asset?.displayUnavailable, zh), transform = transform, loading = state.busy, original = state.originalQuality)
                        }
                        if (state.mediaProblem != null && state.asset?.kind != AssetKind.VIDEO) {
                            Text(mediaProblemText(state.mediaProblem!!, zh), Modifier.testTag("media-error"))
                            TvButton(t("Retry photo", "重试照片"), Modifier.testTag("retry-photo"), !state.busy) { state.asset?.let { store.openAsset(it) } }
                        }
                        if (state.asset?.kind == AssetKind.VIDEO) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (state.asset?.video != null) TvButton(t("Open video", "打开视频"), Modifier.testTag("open-video"), enabled = !state.busy) { playing = false; playbackFailure = null; store.openVideo() }
                                if (state.asset?.video == null) Text(unavailableText(state.asset?.videoUnavailable, zh), Modifier.testTag("video-unavailable"))
                                else if (!state.videoFailed) Text(t("Video · Press Play after opening", "视频 · 打开后按播放"))
                            }
                        }
                        // Toolbar arrows move focus; immersive mode maps arrows to photos.
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TvButton(if ((state.feed?.version ?: 1) >= 2) t("Library", "媒体库") else t("Photos", "照片"), Modifier.focusRequester(first)) { back() }
                            TvButton(t("Previous", "上一张"), enabled = !state.busy && state.adjacentAsset(-1) != null) { playing = false; store.adjacentPhoto(-1) }
                            if (state.asset?.kind == AssetKind.PHOTO) TvButton(if (playing) t("Pause", "暂停") else t("Play page", "播放本页"), Modifier.testTag("slideshow").onPreviewKeyEvent {
                                if (it.nativeKeyEvent.action != AndroidKey.ACTION_DOWN) false
                                else when (it.nativeKeyEvent.keyCode) {
                                    AndroidKey.KEYCODE_MEDIA_PLAY_PAUSE -> { transform = PhotoTransform(); playing = !playing; true }
                                    else -> false
                                }
                            }, enabled = state.asset?.kind == AssetKind.PHOTO && !state.busy && state.problem == null && (playing || state.adjacentAsset(1) != null)) { transform = PhotoTransform(); playing = !playing }
                            TvButton(t("Next", "下一张"), enabled = !state.busy && state.adjacentAsset(1) != null) { playing = false; store.adjacentPhoto(1) }
                            if (state.asset?.kind == AssetKind.PHOTO) {
                            TvButton(t("Full screen", "全屏"), Modifier.testTag("fullscreen"), enabled = !state.busy && state.display != null) { immersive = true }
                            TvButton(if (transform.fill) t("Fit photo", "完整显示") else t("Fill screen", "填满画面"), Modifier.testTag("photo-fit"), enabled = !state.busy && state.display != null) {
                                playing = false; transform = PhotoTransform(fill = !transform.fill)
                            }
                            if (state.asset?.original != null) TvButton(
                                if (state.originalQuality) t("Original loaded", "已加载原图") else t("Original quality", "原图画质"),
                                Modifier.testTag("photo-original"), enabled = !state.busy && !state.originalQuality) {
                                playing = false; store.openOriginal()
                            }
                            TvButton(t("Zoom", "放大"), Modifier.testTag("photo-zoom"), enabled = state.display != null) {
                                playing = false; transform = transform.nextZoom(); immersive = true
                            }
                            }
                            TvButton(if ((state.feed?.version ?: 1) >= 2) t("Details", "信息") else t("Captions", "说明")) { captions = !captions }
                        }
                        if (!state.videoFailed) Text("${state.index + 1} / ${state.feed!!.items.size}  ·  " +
                            if (state.asset?.kind == AssetKind.PHOTO) if (state.originalQuality) t("Original file · display resolution adapts to memory", "原始文件 · 显示分辨率随内存调整") else t("Display image · 8 seconds per slide", "高清展示图 · 每张 8 秒") else if (state.asset?.video?.direct == true) t("Original video · streaming", "原始视频 · 流式播放") else t("Compatible video · streaming", "兼容视频 · 流式播放"), style = MaterialTheme.typography.labelSmall)
                    }
                    else -> {
                        val gallery = state.feed
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (discoveryState.query != null) t("Search results", "搜索结果") else gallery?.title.orEmpty(), Modifier.widthIn(max = 220.dp), maxLines = 1, style = MaterialTheme.typography.titleMedium)
                            TvButton(t("Explore", "探索"), Modifier.testTag("explore").focusRequester(exploreFocus)) { exploring = true; discovery?.open() }
                            TvButton(t("Refresh", "刷新"), if (focusAsset == null) Modifier.focusRequester(first) else Modifier, enabled = !state.busy) { store.loadPage(gallery?.page ?: 1) }
                            TvButton(t("Previous page", "上一页"), enabled = !state.busy && gallery != null && gallery.page > 1) { store.loadPage(gallery!!.page - 1) }
                            TvButton(t("Next page", "下一页"), enabled = !state.busy && gallery != null && gallery.hasMore && gallery.page < 2000) { store.loadPage(gallery!!.page + 1) }
                        }
                        if (discoveryState.query != null) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t("${discoveryState.query!!.count} filters applied", "已应用 ${discoveryState.query!!.count} 类条件"), color = Gold, modifier = Modifier.testTag("applied-search"))
                            TvButton(t("Edit filters", "修改条件"), Modifier.testTag("edit-search")) { exploring = true; discovery?.open() }
                            TvButton(t("Clear search", "清除搜索"), Modifier.testTag("clear-results")) { discovery?.clearResults() }
                        }
                        if (gallery != null && gallery.total > 50) TvButton(t("Go to page", "跳转页面"), Modifier.testTag("page-jump"), enabled = !state.busy) { pages = true }
                        if (gallery != null) Text(t("Page ${gallery.page} · ${gallery.total} assets", "第 ${gallery.page} 页 · 共 ${gallery.total} 项"), style = MaterialTheme.typography.labelSmall)
                        if (gallery != null && gallery.version >= 2 && gallery.items.any { !it.mediaReady() }) {
                            val ready = gallery.items.count { it.mediaReady() }
                            Text(t("$ready of ${gallery.items.size} ready on this page · Other media still needs server preparation.",
                                "本页 ${gallery.items.size} 项中有 $ready 项可观看 · 其余媒体仍需服务器准备。"),
                                Modifier.testTag("page-availability"), color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        if (gallery != null && gallery.items.isEmpty()) Text(if (discoveryState.query != null) t("No matching memories. Try fewer filters.", "没有匹配的回忆，试试减少筛选条件。") else t("No photos here yet.", "这里还没有照片。"))
                        if (state.gridProblems.isNotEmpty() || state.missingGrids.any { id -> gallery?.items?.any { it.id == id && it.grid != null } == true }) {
                            TvButton(t("Retry missing previews", "重试未加载的预览"), Modifier.testTag("retry-previews")) { store.retryPreviews() }
                        }
                        LazyVerticalGrid(GridCells.Adaptive(if (LocalConfiguration.current.fontScale >= 1.5f) 240.dp else 180.dp), Modifier.weight(1f).testTag("grid"), state = grid,
                            contentPadding = PaddingValues(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(gallery?.items.orEmpty(), key = { it.id }) { asset ->
                                val target = focusAsset?.id
                                var focused by remember { mutableStateOf(false) }
                                OutlinedButton({ lastAsset = asset.id; store.openAsset(asset, openPlayer = asset.kind == AssetKind.VIDEO && asset.video != null) },
                                    Modifier.fillMaxWidth().height(158.dp).then(if (asset.id == target) Modifier.focusRequester(first) else Modifier)
                                        .focusProperties { canFocus = asset.canOpen() }.onFocusChanged { focused = it.isFocused }.testTag("asset-${asset.id}"),
                                    enabled = asset.canOpen(), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Moss, contentColor = Cream),
                                    border = BorderStroke(if (focused) 3.dp else 1.dp, if (focused) Gold else Moss)) {
                                    Column(Modifier.fillMaxSize()) {
                                        TvImage(state.grids[asset.id], t("Photo", "照片") + " ${asset.id}", Modifier.fillMaxWidth().weight(1f), if (asset.id in state.gridProblems) t("Preview load failed · Retry", "预览加载失败 · 可重试") else if (asset.kind == AssetKind.VIDEO && asset.video != null) t("Ready to play", "可以播放") else unavailableText(asset.gridUnavailable, zh), maxPixels = 262144, loading = asset.grid != null && state.grids[asset.id] == null && asset.id !in state.missingGrids && asset.id !in state.gridProblems)
                                        if (asset.kind != AssetKind.PHOTO) Text(if (asset.kind == AssetKind.VIDEO) (if (asset.video != null) t("Video · Play", "视频 · 播放") else t("Video · Not ready", "视频 · 尚未就绪")) else t("Unsupported format", "不支持的格式"), Modifier.testTag("asset-kind-${asset.id}"), style = MaterialTheme.typography.labelSmall)
                                        Text(asset.caption.ifEmpty { t("Photo", "照片") + " ${asset.id}" }, maxLines = 1, style = MaterialTheme.typography.labelMedium)
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

private fun problemText(problem: HomeError, zh: Boolean): String {
    fun t(en: String, cn: String) = if (zh) cn else en
    return when (problem) {
        HomeError.DENIED -> t("Home feed is disabled or unavailable to this display.", "家庭照片源已停用或此屏幕无法访问。")
        HomeError.CHANGED -> t("Photo selection changed. Reloading…", "照片选择已更改，正在重新加载…")
        HomeError.BUSY -> t("Server is busy. Waiting before retrying…", "服务器繁忙，正在等待重试…")
        HomeError.TLS -> t("Cannot verify the secure server connection.", "无法验证服务器的安全连接。")
        HomeError.INVALID -> t("The server response could not be displayed safely.", "无法安全显示服务器响应。")
        HomeError.UNAVAILABLE, HomeError.OFFLINE -> t("Connection unavailable. Retrying automatically…", "连接不可用，正在自动重试…")
    }
}

private fun unavailableText(reason: MediaUnavailable?, zh: Boolean): String {
    fun t(en: String, cn: String) = if (zh) cn else en
    return when (reason) {
        MediaUnavailable.NOT_PREPARED -> t("Not prepared yet", "尚未准备好")
        MediaUnavailable.SOURCE_MISSING -> t("Source unavailable", "源文件不可用")
        MediaUnavailable.UNSUPPORTED -> t("Unsupported format", "不支持的格式")
        MediaUnavailable.PREPARATION_FAILED -> t("Preparation failed", "准备失败")
        null -> t("Preview unavailable", "预览不可用")
    }
}

/** Catalog entries remain visible, but unavailable media never opens an empty viewer. */
private fun HomeAsset.canOpen() = display != null || original != null || kind == AssetKind.VIDEO && video != null
private fun HomeAsset.mediaReady() = if (kind == AssetKind.VIDEO) video != null else kind == AssetKind.PHOTO && (display != null || original != null)

private fun mediaProblemText(problem: HomeError, zh: Boolean): String {
    val message = when (problem) {
        HomeError.OFFLINE -> if (zh) "媒体连接中断，请重试。" else "The media connection was interrupted. Please retry."
        HomeError.BUSY -> if (zh) "服务器繁忙，请稍后重试。" else "The server is busy. Retry shortly."
        HomeError.UNAVAILABLE -> if (zh) "服务器暂时无法提供此媒体，请重试。" else "The server could not provide this media. Please retry."
        else -> if (zh) "无法读取媒体响应，请反馈此错误码。" else "The media response could not be read. Please report this code."
    }
    return "$message [TV-READ-${problem.name}]"
}
