package dev.photohouse.connected

import android.content.*
import android.graphics.SurfaceTexture
import android.media.*
import android.os.*
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.photohouse.connected.core.VideoReader
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

internal class VideoDataSource(private val reader: VideoReader) : MediaDataSource() {
    override fun getSize() = reader.size()
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int) = reader.readAt(position, buffer, offset, size)
    // Borrowed reader: Store/NativeVideoPlayer own cancellation and final release.
    // MediaPlayer may close this adapter while rejecting setDataSource; closing
    // the owner here would suppress the error callback and leave a stuck viewer.
    override fun close() { }
}
internal data class Playback(val ready: Boolean = false, val playing: Boolean = false, val position: Int = 0,
    val duration: Int = 0, val width: Int = 16, val height: Int = 9, val seeking: Boolean = false)

/** Every platform-player operation runs on one looper; close cancels HTTP before release. */
internal class NativeVideoPlayer(context: Context, private val reader: VideoReader,
    private val changed: (Playback) -> Unit, private val failed: () -> Unit) {
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
    private val noisy = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { pause() } }
    private val app = context.applicationContext
    init {
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") app.registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        reader.onClose(::close)
    }
    private fun publish() { val value = state; main.post { if (!closed.get()) changed(value) } }
    private fun command(block: () -> Unit) { handler.post { if (!closed.get()) try { block() } catch (_: Exception) { error() } } }
    private fun error() { if (!closed.get()) main.post { if (!closed.get()) failed() } }
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
            p.setOnSeekCompleteListener { state = state.copy(seeking = false, position = p.currentPosition); publish() }
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
            player?.start(); state = state.copy(playing = true); publish()
        }
    }
    private fun pauseNow() {
        if (state.ready && state.playing) player?.pause()
        state = state.copy(playing = false); audio.abandonAudioFocusRequest(focus); publish()
    }
    fun pause() = command { pauseNow() }
    fun seek(milliseconds: Int) = command {
        if (!state.ready || state.seeking) return@command
        state = state.copy(seeking = true); publish()
        player?.seekTo(milliseconds.coerceIn(0, state.duration).toLong(), MediaPlayer.SEEK_CLOSEST)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun VideoPlayer(reader: VideoReader, zh: Boolean, close: () -> Unit, failure: () -> Unit) {
    fun t(en: String, cn: String) = if (zh) cn else en
    var state by remember(reader) { mutableStateOf(Playback()) }
    val context = LocalContext.current
    val onFailure by rememberUpdatedState(failure)
    val player = remember(reader) { NativeVideoPlayer(context, reader, { state = it }, { onFailure() }) }
    DisposableEffect(player) { onDispose { player.close() } }
    LaunchedEffect(player) { while (true) { delay(250); player.poll() } }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp).testTag("video-player"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("Video", "视频"), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = close) { Text(t("Close video", "关闭视频")) }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            AndroidView(factory = { ctx -> TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) { player.attach(texture) }
                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) { }
                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) { }
                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean { player.close(); close(); return true }
                }
            } }, modifier = Modifier.fillMaxWidth().aspectRatio(state.width.toFloat() / state.height).testTag("video-surface"))
        }
        if (!state.ready) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(t("Loading video…", "正在加载视频…")) }
        Text("${state.position / 1000} / ${state.duration / 1000} " + t("seconds", "秒"), Modifier.testTag("video-position"))
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
