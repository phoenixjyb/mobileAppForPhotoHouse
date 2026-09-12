package dev.photohouse.connected.core

import kotlinx.coroutines.*
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/** Memory-only, serialized random reads. The player never receives a URL or bearer.
 * No reusable chunk cache: each nonempty read is authorized by the backend again.
 */
class VideoReader internal constructor(
    fetch: suspend (Long, Int) -> VideoChunk,
    private val expiresAt: Long,
    private val now: () -> Long,
    failed: (Exception) -> Unit,
) : Closeable {
    @Volatile private var loader: (suspend (Long, Int) -> VideoChunk)? = fetch
    @Volatile private var failureCallback: ((Exception) -> Unit)? = failed
    private val lifetimeLock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val closed = AtomicBoolean(false)
    private val notified = AtomicBoolean(false)
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val readLock = Any()
    private var total = -1L
    val isClosed get() = closed.get()
    override fun toString() = "VideoReader([private])"
    fun onClose(listener: () -> Unit) {
        listeners += listener
        if (closed.get() && listeners.remove(listener)) listener()
    }
    override fun close() {
        val first = synchronized(lifetimeLock) {
            if (!closed.compareAndSet(false, true)) false
            else { loader = null; failureCallback = null; true }
        }
        if (first) {
            scope.cancel()
            listeners.toList().forEach { if (listeners.remove(it)) it() }
        }
    }
    private fun checkOpen() {
        if (closed.get()) throw IOException("Video closed")
        if (now() >= expiresAt) throw ApiFailure(FailureKind.HTTP, 401)
    }
    private fun chunk(start: Long, length: Int): VideoChunk {
        checkOpen()
        val task = scope.async { (loader ?: throw CancellationException())(start, length) }
        val part = runBlocking { task.await() }
        checkOpen()
        if (part.total > HttpsPhotoHouseApi.VIDEO_FILE_LIMIT) throw ApiFailure(FailureKind.TOO_LARGE)
        if (part.start != start || part.total <= start || (total >= 0 && total != part.total) ||
            part.bytes.size.toLong() != minOf(length.toLong(), part.total - start)) throw ApiFailure(FailureKind.INVALID_RESPONSE)
        total = part.total
        return part
    }
    private fun <T> read(action: () -> T): T = synchronized(readLock) {
        try { checkOpen(); action() }
        catch (e: Exception) {
            val report = !closed.get() && e !is CancellationException
            val callback = failureCallback
            close()
            if (report && notified.compareAndSet(false, true)) callback?.invoke(e)
            throw IOException("Video read unavailable")
        }
    }
    fun size(): Long = read {
        if (total < 0) chunk(0, 1)
        total
    }
    fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int = read {
        require(position >= 0 && offset >= 0 && size >= 0 && offset <= buffer.size && size <= buffer.size - offset)
        if (size == 0) return@read 0
        if (total < 0) chunk(0, 1)
        if (position >= total) return@read -1
        val length = minOf(size.toLong(), HttpsPhotoHouseApi.VIDEO_CHUNK_LIMIT.toLong(), total - position).toInt()
        val part = chunk(position, length)
        synchronized(lifetimeLock) { checkOpen(); part.bytes.copyInto(buffer, offset) }
        part.bytes.size
    }
}
