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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.photohouse.connected.core.*
import dev.photohouse.protocol.*
import kotlinx.coroutines.delay

private class Words(val zh: Boolean) {
    fun t(en: String, cn: String) = if (zh) cn else en
    fun message(m: Message) = when (m) {
        Message.SIGNED_OUT_LOCAL -> t("Signed out on this phone. Server acknowledgement is unavailable.", "已在本机退出，尚未获得服务器确认。")
        Message.SIGNED_OUT_CONFIRMED -> t("Signed out. The server confirmed session revocation.", "已退出，服务器已确认会话撤销。")
        Message.SESSION_ENDED -> t("Your session ended. Please sign in again.", "会话已结束，请重新登录。")
        Message.ACCESS_DENIED -> t("Access was not granted. Check your account, invitation or library access.", "未获授权，请检查账号、邀请或资料库权限。")
        Message.UNAVAILABLE -> t("PhotoHouse is temporarily unavailable. If registration was submitted, try signing in when it returns.", "相册服务暂不可用。如果已提交注册，请在恢复后尝试登录。")
        Message.TLS_ERROR -> t("The server's secure connection could not be verified. Contact your administrator.", "无法验证服务器的安全连接，请联系管理员。")
        Message.CLOSED -> t("This request was blocked. Contact your administrator.", "请求已被阻止，请联系管理员。")
        Message.RATE_LIMITED -> t("Too many attempts. Wait before trying again.", "尝试过于频繁，请稍后重试。")
        Message.INVALID_INPUT -> t("Check your inputs. Use a country code and a password of 15–128 characters. If already registered, sign in.", "请检查输入，使用含国家码的手机号和 15–128 个字符的密码。已注册请登录。")
        Message.INVALID_RESPONSE -> t("The server response could not be displayed safely.", "无法安全显示服务器响应。")
        Message.TOO_LARGE -> t("This file is too large to display here.", "文件过大，无法在此处显示。")
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
    val scroll = rememberLazyListState()
    LaunchedEffect(state.generation) { scroll.scrollToItem(0) }
    PhotoHouseTheme {
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
                else if (state.detail != null || state.photoNavigation != null) store?.backToPhotos() else store?.libraries()
            }
            if (state.video != null && !state.covered && store != null) {
                val reader = state.video!!
                key(reader) { VideoPlayer(reader, words.zh, { store.closeVideo(reader) }) { store.videoPlaybackFailed(reader) } }
                return@Surface
            }
            if (state.viewingOriginal && !state.covered && store != null) {
                OriginalPhotoViewer(state.originalPhoto, state.busy, words.zh, store::closeOriginalPhoto)
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
                            Text(words.message(problem.message))
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
                                    if (state.session!!.memberships.isEmpty()) item { Text(t("You have no library memberships.", "尚未加入任何资料库。")) }
                                    items(state.session!!.memberships, key = { it.library_id }) { membership ->
                                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(t("Photo library", "照片资料库"), style = MaterialTheme.typography.titleLarge)
                                            Text(membership.library_id, style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(words.membership(membership), color = MaterialTheme.colorScheme.primary)
                                            Button(onClick = { store.selectLibrary(membership.library_id) }, enabled = membership.available && !state.busy) { Text(t("Open library", "打开资料库")) }
                                        } }
                                    }
                                    item { key(state.generation) { InvitationForm(store, state, words) } }
                                }
                                state.detail != null || state.photoNavigation != null -> {
                                    item { TextButton(onClick = store::backToPhotos) { Text(t("Back to Photos", "返回照片")) } }
                                    state.detail?.let { detail ->
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
                                    if (detail.asset.kind == "image" && detail.originals_allowed) item {
                                        Button(onClick = store::openOriginalPhoto, enabled = !state.busy) { Text(t("Open original photo", "打开原始照片")) }
                                    }
                                    if (detail.asset.kind == "video" && detail.originals_allowed) item {
                                        Button(onClick = store::openVideo, enabled = !state.busy) { Text(t("Open video", "打开视频")) }
                                    }
                                    item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(detail.asset.taken_at ?: t("Date unknown", "日期未知"), style = MaterialTheme.typography.titleLarge)
                                        Text(t("Source date, shown as received", "原始日期，按原文显示"),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!detail.originals_allowed) Text(t("Original access is not permitted.", "无原始文件访问权限。"))
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
                                        Text(t("Photos", "照片"), style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
                                        Text(state.library.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } }
                                    state.gallery?.let { gallery ->
                                        if (gallery.items.isEmpty()) item { Text(t("This page has no photos", "此页没有照片")) }
                                        val columns = if (config.fontScale > 1.3f || config.screenWidthDp < 360) 1 else 2
                                        items(gallery.items.chunked(columns)) { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            row.forEach { asset -> Card(onClick = { store.openAsset(asset) }, modifier = Modifier.weight(1f)) {
                                                Preview(asset, state.previews[asset.id], words)
                                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(asset.taken_at ?: t("Date unknown", "日期未知"), style = MaterialTheme.typography.titleSmall)
                                                    Text(t(if (asset.kind == "video") "Video" else "Photo", if (asset.kind == "video") "视频" else "照片"),
                                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            } }
                                            if (row.size < columns) Spacer(Modifier.weight(1f))
                                        } }
                                        item {
                                            Text(t("Page ${gallery.page} · ${gallery.total} photos", "第 ${gallery.page} 页 · ${gallery.total} 张照片"))
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { store.loadPage(gallery.page - 1) }, enabled = !state.busy && gallery.page > 1) { Text(t("Previous", "上一页")) }
                                                OutlinedButton(onClick = { store.loadPage(gallery.page + 1) }, enabled = !state.busy && gallery.page < 100000 && gallery.page.toLong() * gallery.page_size < gallery.total) { Text(t("Next", "下一页")) }
                                                OutlinedButton(onClick = { store.loadPage() }, enabled = !state.busy) { Text(t("Refresh", "刷新")) }
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
    var register by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var now by remember(state.problem) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.problem) { while (now < (state.problem?.retryAtMillis ?: 0)) { delay(500); now = System.currentTimeMillis() } }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(words.t(if (register) "Invited registration" else "Sign in", if (register) "受邀注册" else "登录"), style = MaterialTheme.typography.headlineSmall)
            Text(words.t(if (register) "Join your photo library with an invitation from its owner." else "Welcome back to your photo library.",
                if (register) "使用所有者发出的邀请，加入你的照片资料库。" else "欢迎回到你的照片资料库。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(phone, { if (it.length <= 32) phone = it }, label = { Text(words.t("Phone with country code", "含国家码的手机号")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { if (it.codePointCount(0, it.length) <= 128) password = it }, label = { Text(words.t("Password (15–128 characters)", "密码（15–128 个字符）")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false), visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            if (register) OutlinedTextField(code, { if (it.length <= 512) code = it }, label = { Text(words.t("Invitation code", "邀请码")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false), visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            val valid = runCatching { Admission.phone(phone); Admission.password(password); !register || code.isNotBlank() }.getOrDefault(false)
            Button(onClick = { store.authenticate(phone, password, if (register) code else null); phone = ""; password = ""; code = "" }, enabled = valid && !state.busy && now >= (state.problem?.retryAtMillis ?: 0), modifier = Modifier.fillMaxWidth()) { Text(words.t(if (register) "Register with invitation" else "Sign in", if (register) "使用邀请注册" else "登录")) }
            TextButton(onClick = { register = !register; phone = ""; password = ""; code = "" }, enabled = !state.busy) { Text(words.t(if (register) "Already registered? Sign in" else "Have an invitation? Register", if (register) "已有账号？登录" else "收到邀请？注册")) }
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

@Composable private fun PhotoHouseTheme(content: @Composable () -> Unit) {
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
