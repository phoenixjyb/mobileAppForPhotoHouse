package dev.photohouse.connected

import android.content.*
import android.graphics.SurfaceTexture
import android.media.*
import android.os.*
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.photohouse.connected.core.VideoReader
import dev.photohouse.connected.core.MediaViewport
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

internal interface PhonePlaybackSource : java.io.Closeable {
    fun size(): Long
    fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int
    fun onClose(listener: () -> Unit)
}
private class AccountPlaybackSource(private val reader: VideoReader) : PhonePlaybackSource {
    override fun size() = reader.size()
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int) = reader.readAt(position, buffer, offset, size)
    override fun onClose(listener: () -> Unit) = reader.onClose(listener)
    override fun close() = reader.close()
}
internal class VideoDataSource(private val reader: PhonePlaybackSource) : MediaDataSource() {
    constructor(reader: VideoReader) : this(AccountPlaybackSource(reader))
    override fun getSize() = reader.size()
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int) = reader.readAt(position, buffer, offset, size)
    // Borrowed reader: Store/NativeVideoPlayer own cancellation and final release.
    // MediaPlayer may close this adapter while rejecting setDataSource; closing
    // the owner here would suppress the error callback and leave a stuck viewer.
    override fun close() { }
}
internal data class Playback(val ready: Boolean = false, val playing: Boolean = false, val position: Int = 0,
    val duration: Int = 0, val width: Int = 16, val height: Int = 9, val seeking: Boolean = false, val buffering: Boolean = false, val audioFocusDenied: Boolean = false)

internal fun videoTime(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    val minutes = seconds / 60
    return if (minutes < 60) "$minutes:${(seconds % 60).toString().padStart(2, '0')}"
    else "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** Every platform-player operation runs on one looper; close cancels HTTP before release. */
internal class NativeVideoPlayer(context: Context, private val reader: PhonePlaybackSource,
    private val changed: (Playback) -> Unit, private val failed: () -> Unit) {
    constructor(context: Context, reader: VideoReader, changed: (Playback) -> Unit, failed: () -> Unit) :
        this(context, AccountPlaybackSource(reader), changed, failed)
    val isClosed get() = closed.get()
    private val failureSent = AtomicBoolean(false)
    private val thread = HandlerThread("PhotoHouseVideo").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean(false)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener({ if (it != AudioManager.AUDIOFOCUS_GAIN) pause() }, handler).build()
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var state = Playback()
    private var seekGeneration = 0L
    private val noisy = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { pause() } }
    private val app = context.applicationContext
    init {
        try {
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), Context.RECEIVER_NOT_EXPORTED)
            else @Suppress("UnspecifiedRegisterReceiverFlag") app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        } catch (_: Exception) { error() }
        reader.onClose(::close)
    }
    private fun publish() { val value = state; main.post { if (!closed.get()) changed(value) } }
    private fun command(block: () -> Unit) { handler.post { if (!closed.get()) try { block() } catch (_: Exception) { error() } } }
    private fun error() {
        if (!closed.get() && failureSent.compareAndSet(false, true)) {
            close()
            main.post { failed() }
        }
    }
    fun attach(texture: SurfaceTexture) = command {
        if (player != null) return@command
        surface = Surface(texture)
        val p = MediaPlayer()
        player = p
        p.also {
            p.setAudioAttributes(attributes)
            p.setSurface(surface)
            p.setOnVideoSizeChangedListener { _, width, height ->
                if (width > 0 && height > 0 && !closed.get()) {
                    state = state.copy(width = width, height = height); publish()
                }
            }
            p.setOnPreparedListener {
                if (!closed.get()) {
                    state = state.copy(ready = true, duration = p.duration.coerceAtLeast(0),
                        width = p.videoWidth.takeIf { it > 0 } ?: state.width, height = p.videoHeight.takeIf { it > 0 } ?: state.height)
                    publish() // Explicit Play; never autoplay audio after preparation.
                }
            }
            p.setOnCompletionListener { state = state.copy(playing = false, position = state.duration); audio.abandonAudioFocusRequest(focus); publish() }
            p.setOnSeekCompleteListener { seekGeneration++; state = state.copy(seeking = false, position = p.currentPosition); publish() }
            p.setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> { state = state.copy(buffering = true); publish(); true }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> { state = state.copy(buffering = false); publish(); true }
                    else -> false
                }
            }
            p.setOnErrorListener { _, _, _ -> error(); true }
            p.setDataSource(VideoDataSource(reader))
            p.prepareAsync()
            handler.postDelayed({ if (!closed.get() && !state.ready) error() }, 30000)
        }
    }
    fun playPause() = command {
        if (!state.ready || state.seeking) return@command
        if (state.playing) pauseNow()
        else if (audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player?.start(); state = state.copy(playing = true, audioFocusDenied = false); publish()
        } else { state = state.copy(audioFocusDenied = true); publish() }
    }
    private fun pauseNow() {
        if (state.ready && state.playing) player?.pause()
        state = state.copy(playing = false); audio.abandonAudioFocusRequest(focus); publish()
    }
    fun pause() = command { pauseNow() }
    fun seek(milliseconds: Int) = command {
        if (!state.ready || state.seeking) return@command
        state = state.copy(seeking = true); publish()
        val generation = ++seekGeneration
        player?.seekTo(milliseconds.coerceIn(0, state.duration).toLong(), MediaPlayer.SEEK_CLOSEST)
        handler.postDelayed({ if (!closed.get() && state.seeking && seekGeneration == generation) error() }, 30000)
    }
    fun poll() = command {
        if (state.ready && !state.seeking) { state = state.copy(position = player?.currentPosition ?: 0); publish() }
    }
    fun close() {
        if (!closed.compareAndSet(false, true)) return
        reader.close()
        runCatching { app.unregisterReceiver(noisy) }
        handler.post {
            runCatching { player?.release() }; player = null
            surface?.release(); surface = null
            audio.abandonAudioFocusRequest(focus)
            thread.quitSafely()
        }
    }
}

@Composable internal fun VideoPlayer(reader: VideoReader, zh: Boolean, close: () -> Unit, failure: () -> Unit) {
    val source = remember(reader) { AccountPlaybackSource(reader) }
    PhoneVideoPlayer(source, zh, close, failure)
}

@Composable internal fun PhoneVideoPlayer(reader: PhonePlaybackSource, zh: Boolean, close: () -> Unit, failure: () -> Unit) {
    val current by rememberUpdatedState(reader)
    val onClose by rememberUpdatedState(close)
    val onFailure by rememberUpdatedState(failure)
    key(reader) { PhoneVideoContent(reader, zh, { if (current === reader) onClose() }, { if (current === reader) onFailure() }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun PhoneVideoContent(reader: PhonePlaybackSource, zh: Boolean, close: () -> Unit, failure: () -> Unit) {
    fun t(en: String, cn: String) = if (zh) cn else en
    var state by remember(reader) { mutableStateOf(Playback()) }
    var fill by remember(reader) { mutableStateOf(false) }
    var fullScreen by remember(reader) { mutableStateOf(false) }
    val context = LocalContext.current
    val onFailure by rememberUpdatedState(failure)
    val player = remember(reader) { NativeVideoPlayer(context, reader, { state = it }, { onFailure() }) }
    DisposableEffect(player) { onDispose { player.close() } }
    MediaWindow(fullScreen, state.playing)
    BackHandler(fullScreen) { fullScreen = false }
    LaunchedEffect(player) { while (true) { delay(250); player.poll() } }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("video-player")) {
    val panelLimit = maxHeight * 0.5f
    Column(Modifier.fillMaxSize().then(if (fullScreen) Modifier else Modifier.safeDrawingPadding().padding(16.dp)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().background(Color.Black).clipToBounds().testTag("video-viewport"), contentAlignment = Alignment.Center) {
            val fitted = MediaViewport.measure(state.width.toFloat(), state.height.toFloat(), maxWidth.value, maxHeight.value, fill)
            AndroidView(factory = { ctx -> TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) { player.attach(texture) }
                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) { }
                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) { }
                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                        val expected = player.isClosed
                        player.close()
                        if (!expected) onFailure()
                        return true
                    }
                }
            } }, modifier = Modifier.requiredSize(fitted.width.dp, fitted.height.dp).testTag("video-surface"))
        }
        if (!fullScreen) Column(Modifier.fillMaxWidth().heightIn(max = panelLimit).verticalScroll(rememberScrollState()).testTag("video-controls"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = close) { Text(t("Close video", "关闭视频")) }
            TextButton(onClick = { fullScreen = true }, enabled = state.ready) { Text(t("Full screen", "全屏")) }
            TextButton(onClick = { fill = !fill }) { Text(if (fill) t("Fit video", "完整视频") else t("Fill screen", "填满屏幕")) }
        }
        Text(if (fill) t("Fill · edges cropped", "填满 · 边缘已裁切") else t("Fit · whole video", "适合 · 完整视频"), Modifier.testTag("video-fit-mode"), style = MaterialTheme.typography.labelMedium)
        if (!state.ready || state.seeking || state.buffering) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(when { !state.ready -> t("Loading video…", "正在加载视频…"); state.seeking -> t("Seeking…", "正在跳转…"); else -> t("Buffering…", "正在缓冲…") })
        }
        if (state.audioFocusDenied) Text(t("Audio is busy. Pause other audio and try Play again.", "音频被占用，请暂停其他音频后再播放。"), Modifier.testTag("video-audio-focus"))
        Text("${videoTime(state.position)} / ${videoTime(state.duration)}", Modifier.testTag("video-position"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = player::playPause, enabled = state.ready && !state.seeking) { Text(if (state.playing) t("Pause", "暂停") else t("Play", "播放")) }
            OutlinedButton(onClick = { player.seek(state.position - 10000) }, enabled = state.ready && !state.seeking) { Text(t("Back 10s", "后退 10 秒")) }
            OutlinedButton(onClick = { player.seek(state.position + 10000) }, enabled = state.ready && !state.seeking) { Text(t("Forward 10s", "前进 10 秒")) }
        }
        var scrub by remember(reader) { mutableStateOf<Float?>(null) }
        Slider(value = scrub ?: state.position.toFloat().coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
            onValueChange = { scrub = it }, onValueChangeFinished = { scrub?.let { player.seek(it.toInt()) }; scrub = null },
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.ready && !state.seeking,
            modifier = Modifier.testTag("video-seek"))
        }
    }
    if (fullScreen) FilledTonalButton(onClick = { fullScreen = false },
        modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(12.dp).testTag("video-exit-fullscreen")) {
        Text(t("Show controls", "显示控制"))
    }
    if (fullScreen && (state.seeking || state.buffering)) Surface(Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp)) {
        Text(if (state.seeking) t("Seeking…", "正在跳转…") else t("Buffering…", "正在缓冲…"), Modifier.padding(12.dp))
    }
    }
}
