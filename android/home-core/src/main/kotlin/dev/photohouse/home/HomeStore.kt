package dev.photohouse.home

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** All operations are confined to the supplied UI scope. Bytes exist only in memory. */
data class HomeState(
    val feed: HomeFeed? = null, val grids: Map<Int, ByteArray> = emptyMap(),
    val selected: Int? = null, val display: ByteArray? = null,
    val busy: Boolean = false, val covered: Boolean = true, val disconnected: Boolean = false,
    val problem: HomeError? = null, val retryAtMillis: Long = 0,
    val missingGrids: Set<Int> = emptySet(), val displayMissing: Boolean = false
) {
    val asset: HomeAsset? get() = feed?.items?.firstOrNull { it.id == selected }
    val index: Int get() = feed?.items?.indexOfFirst { it.id == selected } ?: -1
}
class HomeStore(private val api: HomeApi, private val scope: CoroutineScope,
                private val now: () -> Long = { System.nanoTime() / 1_000_000 },
                private val jitter: () -> Double = { Random.nextDouble() }) {
    private val mutable = MutableStateFlow(HomeState())
    val state = mutable.asStateFlow()
    private var visible = false
    private var paused = false
    private var generation = 0L
    private var work: Job? = null
    private var timer: Job? = null
    private var failures = 0
    private var notBefore = 0L
    private var page = 1
    private var detailJob: Job? = null
    private var detailGeneration = 0L
    private fun current(g: Long) = visible && !paused && g == generation
    private fun invalidate() { generation++; work?.cancel(); timer?.cancel(); detailJob?.cancel(); detailGeneration++ }
    fun foreground() {
        if (visible || paused) return
        visible = true; loadPage(page)
    }
    fun background() {
        visible = false; invalidate(); mutable.value = HomeState(disconnected = paused)
    }
    fun disconnect() {
        paused = true; invalidate(); mutable.value = HomeState(covered = false, disconnected = true)
    }
    fun reconnect() {
        paused = false; visible = true; loadPage(page)
    }
    fun canRetry() = visible && !paused && !state.value.busy && now() >= notBefore
    fun retry() { if (canRetry()) loadPage(page) }
    fun loadPage(target: Int = page) {
        if (!visible || paused || target !in 1..2000) return
        page = target; invalidate()
        val g = generation
        mutable.value = HomeState(busy = now() >= notBefore, covered = false)
        work = scope.launch {
            delay((notBefore - now()).coerceAtLeast(0))
            if (!current(g)) return@launch
            mutable.value = HomeState(busy = true, covered = false)
            try {
                val feed = api.feed(page)
                if (!current(g)) return@launch
                mutable.value = HomeState(feed = feed, covered = false)
                scheduleRefresh(g)
                // Sequential grid requests keep well below the server's four-read budget.
                for (asset in feed.items) {
                    val bytes = api.preview(asset, Variant.GRID, feed.revision)
                    if (!current(g)) return@launch
                    val s = state.value
                    // A page may describe 100 MiB of grids; retain at most 16 MiB.
                    if (bytes != null && s.grids.values.sumOf { it.size } + bytes.size <= GRID_CACHE_BYTES)
                        mutable.value = s.copy(grids = s.grids + (asset.id to bytes))
                    else mutable.value = s.copy(missingGrids = s.missingGrids + asset.id)
                }
                failures = 0
            } catch (e: CancellationException) { throw e }
              catch (e: HomeFailure) { if (current(g)) failed(e) }
        }
    }
    private fun scheduleRefresh(g: Long) {
        timer?.cancel()
        timer = scope.launch {
            delay(60000)
            if (!current(g)) return@launch
            try {
                val fresh = api.feed(page)
                if (!current(g)) return@launch
                val old = state.value.feed
                if (fresh != old) { loadPage(page); return@launch }
                scheduleRefresh(g)
            } catch (e: CancellationException) { throw e }
              catch (e: HomeFailure) { if (current(g)) failed(e) }
        }
    }
    private fun failed(e: HomeFailure) {
        invalidate()
        val retryable = e.kind in setOf(HomeError.OFFLINE, HomeError.UNAVAILABLE, HomeError.BUSY, HomeError.CHANGED)
        val delays = longArrayOf(2000, 5000, 15000, 30000, 60000)
        val base = delays[failures.coerceAtMost(4)]
        val wait = (base + (base * .2 * jitter().coerceIn(0.0, 1.0)).toLong()).coerceAtMost(60000)
        failures++
        val floor = e.retryAfterMillis.coerceAtMost(Long.MAX_VALUE - now())
        if (e.kind == HomeError.BUSY) notBefore = maxOf(notBefore, now() + floor)
        val retryAt = maxOf(now() + wait, notBefore)
        mutable.value = HomeState(covered = false, problem = e.kind, retryAtMillis = retryAt)
        if (retryable) {
            val g = generation
            timer = scope.launch {
                delay((retryAt - now()).coerceAtLeast(0))
                if (current(g)) loadPage(page)
            }
        }
    }
    fun openAsset(asset: HomeAsset) {
        val s = state.value; val feed = s.feed ?: return
        if (!visible || paused || s.covered || asset !in feed.items) return
        detailJob?.cancel(); val d = ++detailGeneration; val g = generation
        mutable.value = s.copy(selected = asset.id, display = null, busy = true, displayMissing = false)
        detailJob = scope.launch {
            try {
                val bytes = api.preview(asset, Variant.DISPLAY, feed.revision)
                if (current(g) && d == detailGeneration)
                    mutable.value = state.value.copy(display = bytes, busy = false, displayMissing = bytes == null)
            } catch (e: CancellationException) { throw e }
              catch (e: HomeFailure) { if (current(g) && d == detailGeneration) failed(e) }
        }
    }
    fun adjacentPhoto(delta: Int) {
        val s = state.value; val list = s.feed?.items ?: return
        if (s.busy || s.index < 0 || delta !in listOf(-1, 1)) return
        list.getOrNull(s.index + delta)?.let(::openAsset)
    }
    fun backToPhotos() {
        detailJob?.cancel(); detailGeneration++
        mutable.value = state.value.copy(selected = null, display = null, busy = false, displayMissing = false)
    }
    companion object { const val GRID_CACHE_BYTES = 16 * 1024 * 1024 }
}
