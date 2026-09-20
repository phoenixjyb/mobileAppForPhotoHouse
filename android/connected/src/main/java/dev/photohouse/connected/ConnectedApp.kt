package dev.photohouse.connected

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.photohouse.connected.core.*
import dev.photohouse.protocol.*
import kotlinx.coroutines.delay

private class Words(val zh: Boolean) {
    fun t(en: String, cn: String) = if (zh) cn else en
    fun message(m: Message) = when (m) {
        Message.SESSION_STORAGE_UNAVAILABLE -> t("Secure sign-in storage is unavailable on this device. Sign in again next time; if signing out, check server revocation.", "此设备的安全登录存储不可用，下次需重新登录；退出时请确认服务器已撤销会话。")
        Message.SIGNED_OUT_LOCAL -> t("Signed out on this phone. Server acknowledgement is unavailable.", "已在本机退出，尚未获得服务器确认。")
        Message.SIGNED_OUT_CONFIRMED -> t("Signed out. The server confirmed session revocation.", "已退出，服务器已确认会话撤销。")
        Message.SESSION_ENDED -> t("Your session ended. Please sign in again.", "会话已结束，请重新登录。")
        Message.ACCESS_DENIED -> t("Access was not granted. Check your account, invitation or library access.", "未获授权，请检查账号、邀请或资料库权限。")
        Message.UNAVAILABLE -> t("PhotoHouse is temporarily unavailable. If registration was submitted, try signing in when it returns.", "相册服务暂不可用。如果已提交注册，请在恢复后尝试登录。")
        Message.TLS_ERROR -> t("The server's secure connection could not be verified. Contact your administrator.", "无法验证服务器的安全连接，请联系管理员。")
        Message.CLOSED -> t("This request was blocked. Contact your administrator.", "请求已被阻止，请联系管理员。")
        Message.RATE_LIMITED -> t("Too many attempts. Wait before trying again.", "尝试过于频繁，请稍后重试。")
        Message.INVALID_INPUT -> t("Check your inputs and the password rules shown. Use an explicit country code. If already registered, sign in.", "请检查输入和显示的密码要求，手机号应包含国家码。已注册请登录。")
        Message.INVALID_RESPONSE -> t("The server response could not be displayed safely.", "无法安全显示服务器响应。")
        Message.TOO_LARGE -> t("This file is too large to display here.", "文件过大，无法在此处显示。")
        Message.DISCOVERY_CHANGED -> t("Search information changed. Refresh options and apply your filters again.", "搜索资料已更新，请刷新选项后重新选择并应用条件。")
        Message.DISCOVERY_INPUT -> t("Check your search filters and dates.", "请检查搜索条件和日期。")
        Message.VIDEO_NOT_READY -> t("This video is not ready for playback yet. You can try again later.", "此视频尚未准备好，可稍后重试。")
        Message.VIDEO_CHANGED -> t("This video changed. Open it again to load the current version.", "此视频已更新，请重新打开。")
        Message.VIDEO_BUSY -> t("Video playback is busy. Wait a moment before trying again.", "视频播放繁忙，请稍候重试。")
        Message.PLAYBACK_UNAVAILABLE -> t("Video playback is temporarily unavailable. Your album is still available.", "视频播放暂不可用，仍可继续浏览相册。")
        Message.MEDIA_UNAVAILABLE -> t("This media is unavailable.", "此媒体不可用。")
    }
    fun membership(m: Membership) = when {
        m.available -> t("Available", "可访问")
        m.status == "requested" -> t("Awaiting approval", "等待批准")
        m.status == "rejected" -> t("Not approved", "未获批准")
        m.status == "revoked" -> t("Access revoked", "权限已撤销")
        m.expires_at != null -> t("Membership expired or unavailable", "成员资格过期或不可用")
        else -> t("Library unavailable", "资料库不可用")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ConnectedApp(store: ConnectedStore?) {
    var language by remember { mutableStateOf("system") }
    var settings by remember { mutableStateOf(false) }
    val config = LocalConfiguration.current
    val words = Words(language == "zh" || language == "system" && config.locales[0].language == "zh")
    val t = words::t
    val state = store?.state?.collectAsState()?.value ?: LiveState()
    var jumpPage by remember(state.generation) { mutableStateOf(false) }
    var lookupAsset by remember(state.generation) { mutableStateOf(false) }
    val scroll = rememberLazyListState()
    LaunchedEffect(state.generation) { scroll.scrollToItem(0) }
    PhotoHouseTheme {
        if (!state.covered && store != null) state.storyEditor?.let { editor ->
            ProtectedStoryEditorDialog(editor, onDismiss = store::closeStoryEditor, zh = words.zh)
        }
        if (lookupAsset && state.library != null && !state.covered && store != null) AssetLookupDialog(
            words.zh, { lookupAsset = false }, { id -> lookupAsset = false; store.openAssetById(id) })
        val galleryForJump = state.gallery
        if (jumpPage && galleryForJump != null && !state.covered && store != null) PhonePageJump(
            galleryForJump.page, galleryForJump.page_size, galleryForJump.total, words.zh,
            { jumpPage = false }, { target -> jumpPage = false; store.navigatePage(target) })
        if (settings) AlertDialog(
            onDismissRequest = { settings = false },
            title = { Text(t("Settings", "设置")) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(t("App language", "界面语言"), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to t("System", "系统"), "en" to "English", "zh" to "简体中文").forEach { (value, label) ->
                        FilterChip(selected = language == value, onClick = { language = value; settings = false }, label = { Text(label) })
                    }
                }
                if (state.session != null && !state.covered && store != null) {
                    HorizontalDivider()
                    TextButton(onClick = { settings = false; store.logout() }) { Text(t("Sign out", "退出登录")) }
                }
            } },
            confirmButton = { TextButton(onClick = { settings = false }) { Text(t("Done", "完成")) } }
        )
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BackHandler(store != null && state.library != null && !state.covered) {
                if (state.video != null) store?.closeVideo()
                else if (state.viewingOriginal) store?.closeOriginalPhoto()
                else if (state.detail != null || state.photoNavigation != null) store?.backToPhotos() else if (state.discovery != null) { if (state.discovery?.editing == true) store?.loadPage(1) else store?.editDiscovery() } else store?.libraries()
            }
            if (state.video != null && !state.covered && store != null) {
                val reader = state.video!!
                key(reader) { VideoPlayer(reader, words.zh, { store.closeVideo(reader) }) { store.videoPlaybackFailed(reader, nativeFailure = true, reason = it) } }
                return@Surface
            }
            if (state.viewingOriginal && !state.covered && store != null) {
                OriginalPhotoViewer(state.originalPhoto, state.busy, words.zh, store::closeOriginalPhoto,
                    state.photoNavigation, state.photoSlideshow, { store.adjacentOriginalPhoto(it) },
                    store::togglePhotoSlideshow, store::stopPhotoSlideshow, store::advancePhotoSlideshow,
                    originalQuality = state.photoOriginalQuality,
                    previewOnly = state.photoPreviewOnly,
                    onOriginal = if (state.detail?.originals_allowed == true) store::openOriginalPhoto else null)
                return@Surface
            }
            LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().testTag("connected-screen"), state = scroll,
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(t("PhotoHouse", "拾光相册"), style = MaterialTheme.typography.headlineLarge,
                                fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary)
                            Text(t("Development preview", "开发预览版"), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { settings = true }, modifier = Modifier.testTag("app-settings")) { Text(t("Settings", "设置")) }
                    }
                }
                if (store == null) {
                    item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t("Server setup needed", "需要配置服务器"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("server-not-configured"))
                        Text(t("Ask your administrator for a build configured for your PhotoHouse HTTPS server. Sign-in is unavailable until then.", "请向管理员获取已配置相册 HTTPS 服务器的版本。配置完成前无法登录。"))
                    } }
                } else {
                    if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    state.problem?.let { problem -> item {
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                            Text(problem.playbackFailure?.message(words.zh) ?: if (problem.message == Message.UNAVAILABLE && state.session != null)
                                t("Could not load this item. Check the connection and retry.", "暂时无法加载此内容，请检查网络连接后重试。")
                            else words.message(problem.message))
                            var now by remember(problem) { mutableLongStateOf(System.currentTimeMillis()) }
                            LaunchedEffect(problem) { while (now < problem.retryAtMillis) { delay(500); now = System.currentTimeMillis() } }
                            if (now < problem.retryAtMillis) Text(t("Please wait", "请稍候"))
                            if (store.canRetry(now)) TextButton(onClick = store::retry) { Text(t("Retry", "重试")) }
                        } }
                    } }
                    when {
                        state.covered -> {
                            item { Text(t("Private content is covered while your session is checked.", "检查会话期间，私人内容已遮盖。")) }
                            item { Button(onClick = store::foreground, enabled = !state.busy) { Text(t("Check session", "检查会话")) } }
                            item { Button(onClick = store::logout) { Text(t("Sign out locally", "在本机退出")) } }
                        }
                        state.session == null -> item { key(state.generation) { AdmissionForm(store, state, words) } }
                        else -> {
                            if (state.library != null && state.detail == null && state.photoNavigation == null) item {
                                TextButton(onClick = store::libraries) { Text(t("Libraries", "资料库")) }
                            }
                            when {
                                state.library == null -> {
                                    item { Text(t("Your libraries", "你的资料库"), style = MaterialTheme.typography.headlineSmall) }
                                    state.session?.displayName?.let { name -> item { Text(t("Welcome, $name", "欢迎，$name"), modifier = Modifier.testTag("account-name")) } }
                                    if (state.session!!.memberships.isEmpty()) item { Text(t("You have no library memberships.", "尚未加入任何资料库。")) }
                                    items(state.session!!.memberships, key = { it.library_id }) { membership ->
                                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(t("Photo and video library", "照片与视频资料库"), style = MaterialTheme.typography.titleLarge)
                                            Text(membership.library_id, style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(words.membership(membership), color = MaterialTheme.colorScheme.primary)
                                            Button(onClick = { store.selectLibrary(membership.library_id) }, enabled = membership.available && !state.busy) { Text(t("Open library", "打开资料库")) }
                                        } }
                                    }
                                    item { key(state.generation) { InvitationForm(store, state, words) } }
                                }
                                state.discovery?.editing == true -> phoneDiscoveryEditor(store, state, words.zh)
                                state.detail != null || state.photoNavigation != null -> {
                                    item { TextButton(onClick = store::backToPhotos) { Text(if (state.photoNavigation?.discovery != null) t("Back to results", "返回结果") else t("Back to Photos", "返回照片")) } }
                                    state.detail?.let { detail ->
                                        item { Text(t("No. ${detail.asset.id}", "编号 ${detail.asset.id}"), style = MaterialTheme.typography.titleMedium) }
                                        item { Preview(detail.asset, state.previews[detail.asset.id], words, detail = true) }
                                    }
                                    state.photoNavigation?.let { navigation ->
                                        item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(t("Photo ${navigation.index + 1} of ${navigation.assetIds.size} · Page ${navigation.page}", "第 ${navigation.page} 页 · 第 ${navigation.index + 1}/${navigation.assetIds.size} 张"))
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { store.adjacentPhoto(-1) }, enabled = !state.busy && navigation.index > 0) { Text(t("Previous photo", "上一张")) }
                                                OutlinedButton(onClick = { store.adjacentPhoto(1) }, enabled = !state.busy && navigation.index < navigation.assetIds.lastIndex) { Text(t("Next photo", "下一张")) }
                                            }
                                        } }
                                    }
                                    state.detail?.let { detail ->
                                    if (detail.asset.kind == "image" && store.photoDeliveryEnabled) item {
                                        Button(onClick = store::openDisplayPhoto, enabled = !state.busy) { Text(t("View photo", "查看照片")) }
                                    }
                                    if (detail.asset.kind == "image" && store.protectedNativeV2Enabled && !store.photoDeliveryEnabled) item {
                                        Button(onClick = store::openPreviewPhoto, enabled = !state.busy,
                                            modifier = Modifier.testTag("view-protected-preview")) { Text(t("View preview", "查看预览")) }
                                        Text(t("Preview quality. Full screen and zoom do not download the original.",
                                            "预览画质。全屏与缩放不会下载原始文件。"), style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (detail.asset.kind == "image" && detail.originals_allowed) item {
                                        Button(onClick = store::openOriginalPhoto, enabled = !state.busy) { Text(t("Open original photo", "打开原始照片")) }
                                    }
                                    if (detail.asset.kind == "video" && (store.preparedVideoEnabled || detail.originals_allowed)) item {
                                        var videoNow by remember(state.problem) { mutableLongStateOf(System.currentTimeMillis()) }
                                        LaunchedEffect(state.problem) { while (videoNow < (state.problem?.retryAtMillis ?: 0)) { delay(500); videoNow = System.currentTimeMillis() } }
                                        val videoEnabled = !state.busy && videoNow >= (state.problem?.retryAtMillis ?: 0)
                                        Button(onClick = store::openVideo, enabled = videoEnabled, modifier = Modifier.testTag("open-video")) { Text(t("Open video", "打开视频")) }
                                        if (store.preparedVideoEnabled && state.busy) Text(t("Checking video availability…", "正在检查视频是否可播放…"))
                                        if (store.preparedVideoEnabled && detail.originals_allowed)
                                            TextButton(onClick = store::openOriginalVideo, enabled = videoEnabled, modifier = Modifier.testTag("open-original-video")) { Text(t("Open original video", "打开原始视频")) }
                                    }
                                    item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(detail.asset.taken_at ?: t("Date unknown", "日期未知"), style = MaterialTheme.typography.titleLarge)
                                        Text(t("Source date, shown as received", "原始日期，按原文显示"),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!detail.originals_allowed) Text(t("Original access is not permitted.", "无原始文件访问权限。"))
                                    } }
                                    if (store.protectedNativeV2Enabled) item {
                                        ProtectedStoriesPanel(state.stories, state.busy, words,
                                            load = store::loadStories, edit = store::editStory)
                                    }
                                    item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                        Text(t("Captions · AI unless marked edited", "描述 · 未标注编辑时为 AI 内容"), style = MaterialTheme.typography.titleMedium)
                                        Text(t("Caption language information is not supplied.", "未提供描述语言信息。"),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } }
                                    if (state.captions?.items.isNullOrEmpty()) item { Text(t("No captions yet", "暂无描述")) }
                                    items(state.captions?.items.orEmpty(), key = { it.id }) { caption ->
                                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(caption.text, style = MaterialTheme.typography.bodyLarge)
                                            if (caption.user_edited) Text(t("Edited", "已编辑"))
                                            if (caption.truncated) Text(t("Text truncated", "文本已截断"))
                                        } }
                                    }
                                    if (state.captions?.has_more == true) item { Text(t("More captions exist", "还有更多描述")) }
                                    }
                                }
                                else -> {
                                    item { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (state.discovery != null) t("Search results", "搜索结果") else t("Your memories", "家庭相册"), style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
                                        Text(state.library.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } }
                                    if (store.mediaFilterEnabled && state.discovery == null) item {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            for (media in GalleryMedia.entries.filter { it != GalleryMedia.PREPARED_VIDEOS || store.preparedBrowseEnabled }) FilterChip(
                                                selected = state.media == media, onClick = { store.selectMedia(media) },
                                                modifier = Modifier.testTag("gallery-media-${media.wire}"), label = { Text(when(media) {
                                                    GalleryMedia.ALL -> t("All", "全部")
                                                    GalleryMedia.PHOTOS -> t("Photos", "照片")
                                                    GalleryMedia.VIDEOS -> t("Videos", "视频")
                                                    GalleryMedia.PREPARED_VIDEOS -> t("Prepared videos", "已准备视频")
                                                }) })
                                        }
                                    }
                                    item { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        state.gallery?.let { gallery -> Text(t("Page ${gallery.page} · ${gallery.total} items", "第 ${gallery.page} 页 · ${gallery.total} 项")) }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { lookupAsset = true }, modifier = Modifier.testTag("open-asset-lookup")) { Text(t("Open by number", "按编号打开")) }
                                            if (state.gallery != null) OutlinedButton(onClick = { jumpPage = true }) { Text(t("Go to page", "跳转页面")) }
                                        }
                                    } }
                                    if (store.discoveryEnabled) item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { if (state.discovery == null) store.openDiscovery() else store.editDiscovery() }, enabled = !state.busy,
                                            modifier = Modifier.testTag("open-discovery")) { Text(if (state.discovery == null) t("Find a memory", "寻找回忆") else t("Edit filters", "修改条件")) }
                                        if (state.discovery != null) TextButton(onClick = { store.loadPage(1) }, enabled = !state.busy) { Text(t("All photos", "全部照片")) }
                                    } }
                                    state.gallery?.let { gallery ->
                                        if (gallery.items.isEmpty()) item { Text(t("No matching items on this page", "此页暂无符合条件的内容")) }
                                        val columns = if (config.fontScale > 1.3f || config.screenWidthDp < 360) 1 else 2
                                        items(gallery.items.chunked(columns)) { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            row.forEach { asset -> Card(onClick = { store.openMedia(asset) }, modifier = Modifier.weight(1f).testTag("media-${asset.id}")) {
                                                Preview(asset, state.previews[asset.id], words)
                                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(t("No. ${asset.id}", "编号 ${asset.id}"), style = MaterialTheme.typography.labelMedium)
                                                    Text(asset.taken_at ?: t("Date unknown", "日期未知"), style = MaterialTheme.typography.titleSmall)
                                                    Text(t(if (asset.kind == "video") "Video" else "Photo", if (asset.kind == "video") "视频" else "照片"),
                                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    TextButton(onClick = { store.openAsset(asset) }, modifier = Modifier.testTag("details-${asset.id}")) {
                                                        Text(t("Details", "详情"))
                                                    }
                                                }
                                            } }
                                            if (row.size < columns) Spacer(Modifier.weight(1f))
                                        } }
                                        item {
                                            Text(t("Page ${gallery.page} · ${gallery.total} items", "第 ${gallery.page} 页 · ${gallery.total} 项"))
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { store.navigatePage(gallery.page - 1) }, enabled = !state.busy && gallery.page > 1) { Text(t("Previous", "上一页")) }
                                                OutlinedButton(onClick = { store.navigatePage(gallery.page + 1) }, enabled = !state.busy && gallery.page < 100000 && gallery.page.toLong() * gallery.page_size < gallery.total) { Text(t("Next", "下一页")) }
                                                OutlinedButton(onClick = { store.navigatePage(1) }, enabled = !state.busy) { Text(t("Refresh", "刷新")) }
                                                OutlinedButton(onClick = { jumpPage = true }, enabled = !state.busy) { Text(t("Go to page", "跳转页面")) }
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
    }
}

@Composable private fun AdmissionForm(store: ConnectedStore, state: LiveState, words: Words) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var register by remember { mutableStateOf(false) }
    var rememberSession by remember { mutableStateOf(true) }
    val defaultPhone = if (store.protectedNativeV2Enabled) "+86" else ""
    var phone by remember { mutableStateOf(defaultPhone) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var now by remember(state.problem) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.problem) { while (now < (state.problem?.retryAtMillis ?: 0)) { delay(500); now = System.currentTimeMillis() } }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(words.t(if (register) "Invited registration" else "Sign in", if (register) "受邀注册" else "登录"), style = MaterialTheme.typography.headlineSmall)
            Text(words.t(if (register) "Join your photo library with an invitation from its owner." else "Welcome back to your photo library.",
                if (register) "使用所有者发出的邀请，加入你的照片资料库。" else "欢迎回到你的照片资料库。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (register && store.protectedNativeV2Enabled) OutlinedTextField(name,
                { if (it.length <= 512) name = it }, label = { Text(words.t("Your name", "你的名字")) },
                supportingText = { Text(words.t("How your family will see you · 1–64 characters", "家人看到的名字 · 1–64 个字符")) },
                singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("registration-name"))
            OutlinedTextField(phone, { if (it.length <= 32) phone = it }, label = { Text(words.t("Phone with country code", "含国家码的手机号")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { if (it.codePointCount(0, it.length) <= 128) password = it }, label = { Text(if (store.protectedNativeV2Enabled) { if (register) words.t("Password (8–128 characters)", "密码（8–128 个字符）") else words.t("Password", "密码") } else words.t("Password (15–128 characters)", "密码（15–128 个字符）")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false), visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            if (register) OutlinedTextField(code, { if (it.length <= 512) code = it }, label = { Text(words.t("Invitation code", "邀请码")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false), visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            if (store.canRememberSession) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(rememberSession, { rememberSession = it }, enabled = !state.busy, modifier = Modifier.testTag("remember-session"))
                    Text(words.t("Remember sign-in on this device", "在此设备记住登录"))
                }
                Text(words.t("Encrypted on this device. Sign-in still expires after 24 hours; your password is never saved.",
                    "在此设备加密保存。登录仍会在 24 小时后过期，不保存密码。"), style = MaterialTheme.typography.bodySmall)
            }
            val valid = runCatching { Admission.phone(phone); Admission.password(password, protectedNativeV2 = store.protectedNativeV2Enabled, registration = register); if (register && store.protectedNativeV2Enabled) { Admission.displayName(name); Admission.invitationCode(code) }; !register || code.isNotBlank() }.getOrDefault(false)
            Button(onClick = { focus.clearFocus(); keyboard?.hide(); store.authenticate(phone, password, if (register) code else null, if (register) name else null, remember = rememberSession); phone = defaultPhone; password = ""; code = ""; name = "" }, enabled = valid && !state.busy && now >= (state.problem?.retryAtMillis ?: 0), modifier = Modifier.fillMaxWidth()) { Text(words.t(if (register) "Register with invitation" else "Sign in", if (register) "使用邀请注册" else "登录")) }
            TextButton(onClick = { focus.clearFocus(); keyboard?.hide(); register = !register; phone = defaultPhone; password = ""; code = ""; name = "" }, enabled = !state.busy) { Text(words.t(if (register) "Already registered? Sign in" else "Have an invitation? Register", if (register) "已有账号？登录" else "收到邀请？注册")) }
            Text(words.t("Phone is an unverified login label. Include your country code.", "手机号是未经验证的登录标识，请包含国家码。"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun InvitationForm(store: ConnectedStore, state: LiveState, words: Words) {
    var code by remember { mutableStateOf("") }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(words.t("Join another library", "加入其他资料库"), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(code, { if (it.length <= 512) code = it }, label = { Text(words.t("Invitation for another library", "其他资料库的邀请码")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false), modifier = Modifier.fillMaxWidth())
            Button(onClick = { store.acceptInvitation(code); code = "" }, enabled = code.isNotBlank() && !state.busy) { Text(words.t("Accept invitation", "接受邀请")) }
        }
    }
}

@Composable private fun Preview(asset: Asset, bytes: ByteArray?, words: Words, detail: Boolean = false) {
    val image = remember(bytes) {
        bytes?.let {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(it, 0, it.size, bounds)
            if (bounds.outWidth in 1..1024 && bounds.outHeight in 1..1024) BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() else null
        }
    }
    val ratio = if (detail && image != null) (image.width.toFloat() / image.height).coerceIn(0.7f, 1.8f) else if (detail) 4f / 3 else 1f
    Box(Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(if (detail) 20.dp else 0.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (image == null) Text(words.t("Preview unavailable", "预览不可用"), Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Image(image, contentDescription = if (detail) words.t("Photo ${asset.id}", "照片 ${asset.id}") else null,
            contentScale = if (detail) ContentScale.Fit else ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (asset.kind == "video") Surface(Modifier.align(Alignment.TopStart).padding(12.dp),
            shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
            Text(words.t("Video", "视频"), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable internal fun PhotoHouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF365347), onPrimary = Color.White,
            primaryContainer = Color(0xFFDEE9DF), onPrimaryContainer = Color(0xFF203A2E),
            secondary = Color(0xFF705D49), onSecondary = Color.White,
            secondaryContainer = Color(0xFFEEE5D7), onSecondaryContainer = Color(0xFF493D2E),
            background = Color(0xFFF7F5F0), onBackground = Color(0xFF252D28),
            surface = Color(0xFFFFFEFA), onSurface = Color(0xFF252D28),
            surfaceVariant = Color(0xFFE8EAE2), onSurfaceVariant = Color(0xFF505C53),
            outline = Color(0xFF737C71), outlineVariant = Color(0xFFD7DDD2)
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ProtectedStoriesPanel(reading: StoryReading?, detailBusy: Boolean, words: Words, load: (Int) -> Unit, edit: (String?) -> Unit) {
    val t = words::t
    var expanded by remember(reading?.result) { mutableStateOf<String?>(null) }
    reading?.result?.items?.firstOrNull { it.id == expanded }?.let { story ->
        Dialog(onDismissRequest = { expanded = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            PhotoHouseTheme { Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(story.title.ifBlank { t("Family story", "家人的故事") }, style = MaterialTheme.typography.titleLarge,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { expanded = null }, modifier = Modifier.testTag("story-reader-close")) {
                        Text(t("Close story", "关闭故事"))
                    }
                    val chunks = remember(story.text) {
                        buildList {
                            var offset = 0
                            while (offset < story.text.length) {
                                // Keep each scroll item shorter than a phone viewport even
                                // with large CJK text; preserve every source character.
                                var end = minOf(offset + 96, story.text.length)
                                if (end < story.text.length && story.text[end - 1].isHighSurrogate()) end--
                                add(story.text.substring(offset, end)); offset = end
                            }
                        }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("story-reader"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chunks.size) { index -> Text(chunks[index], style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            } }
        }
    }
    var now by remember(reading?.problem) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(reading?.problem) {
        while (now < (reading?.problem?.retryAtMillis ?: 0)) { delay(500); now = System.currentTimeMillis() }
    }
    Column(Modifier.fillMaxWidth().testTag("protected-stories"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text(t("Family stories", "家人的故事"), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        Text(t("Memories written by your family, kept separate from AI descriptions.", "家人亲笔记录的回忆，与 AI 描述分开呈现。"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (reading == null) {
            OutlinedButton(onClick = { load(1) }, enabled = !detailBusy, modifier = Modifier.testTag("stories-open")) {
                Text(t("Stories & memories", "故事与回忆"))
            }
        } else if (reading.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(t("Loading stories…", "正在加载故事…"))
        } else {
            reading.problem?.let { error ->
                Text(words.message(error.message), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { load(reading.page) },
                    enabled = !detailBusy && now >= error.retryAtMillis, modifier = Modifier.testTag("stories-retry")) {
                    Text(t("Try again", "重试"))
                }
            }
            reading.result?.let { result ->
                if (result.canCreate) Button(onClick = { edit(null) }, enabled = !detailBusy,
                    modifier = Modifier.testTag("story-add")) { Text(t("Add a memory", "添加回忆")) }
                if (result.items.isEmpty()) Text(t("No family stories on this page yet.", "此页暂无家人的故事。"))
                result.items.forEach { story ->
                    Card(Modifier.fillMaxWidth().testTag("story-${story.id}")) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(t("Family memory", "家人回忆"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            if (story.title.isNotEmpty()) Text(story.title, style = MaterialTheme.typography.titleMedium)
                            if (story.canEdit) TextButton(onClick = { edit(story.id) }, enabled = !detailBusy,
                                modifier = Modifier.testTag("story-edit-${story.id}")) { Text(t("Edit memory", "编辑回忆")) }
                            if (story.byline.isNotEmpty()) Text(story.byline, style = MaterialTheme.typography.labelMedium)
                            if (story.text.length <= 2000) Text(story.text, style = MaterialTheme.typography.bodyLarge)
                            else {
                                Text(story.text.take(240) + "…", style = MaterialTheme.typography.bodyLarge)
                                TextButton(onClick = { expanded = story.id }, modifier = Modifier.testTag("story-read-full")) {
                                    Text(t("Read full story", "阅读全文"))
                                }
                            }
                        }
                    }
                }
                Text(t("Story page ${result.page}", "故事第 ${result.page} 页"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { load(result.page - 1) }, enabled = !detailBusy && result.page > 1,
                        modifier = Modifier.testTag("stories-previous")) { Text(t("Previous", "上一页")) }
                    OutlinedButton(onClick = { load(result.page + 1) }, enabled = !detailBusy && result.hasMore && result.page < 100000,
                        modifier = Modifier.testTag("stories-next")) { Text(t("Next", "下一页")) }
                    TextButton(onClick = { load(1) }, enabled = !detailBusy, modifier = Modifier.testTag("stories-refresh")) {
                        Text(t("Refresh", "刷新"))
                    }
                }
                Text(t("Stories can change while you browse. Refresh to see the latest. Review your words before saving.",
                    "浏览期间故事可能更新，可刷新查看。保存前请检查你的文字。"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
