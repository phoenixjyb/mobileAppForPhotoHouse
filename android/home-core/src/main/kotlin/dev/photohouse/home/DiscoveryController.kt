package dev.photohouse.home

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-domain contract; a transport must bind both revisions to its verified server contract. */
data class DiscoverySnapshot(val revision: Int, val catalogRevision: Int, val libraryId: String,
    val libraryTitle: String, val options: DiscoveryOptions, val nextPages: Map<DiscoveryField, Int> = emptyMap(),
    val binding: String? = null, val facetTotals: Map<DiscoveryField, Int> = emptyMap(),
    val facetLastIds: Map<DiscoveryField, Int> = emptyMap())
interface DiscoveryGateway {
    suspend fun load(): DiscoverySnapshot
    suspend fun more(snapshot: DiscoverySnapshot, field: DiscoveryField): DiscoverySnapshot
    fun results(snapshot: DiscoverySnapshot, draft: DiscoveryDraft): HomeApi
}
data class DiscoveryState(val snapshot: DiscoverySnapshot? = null, val loading: Boolean = false,
    val problem: HomeError? = null, val query: DiscoveryDraft? = null, val results: HomeStore? = null)

/** Owns metadata and an isolated result gallery. A failed search never falls back to all photos. */
class DiscoveryController(private val gateway: DiscoveryGateway, private val browse: HomeStore,
    private val scope: CoroutineScope, private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private val mutable = MutableStateFlow(DiscoveryState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    private var active = false
    private var notBefore = 0L
    fun open() {
        active = true
        val previous = state.value.snapshot
        load {
            val fresh = gateway.load()
            // Revalidate first; retain later pages only under the identical server metadata binding.
            if (previous?.binding != null && previous.binding == fresh.binding &&
                previous.revision == fresh.revision && previous.catalogRevision == fresh.catalogRevision &&
                previous.libraryId == fresh.libraryId && previous.facetTotals == fresh.facetTotals)
                fresh.copy(options = fresh.options.copy(people = previous.options.people, tags = previous.options.tags,
                    places = previous.options.places), nextPages = previous.nextPages, facetLastIds = previous.facetLastIds)
            else fresh
        }
    }
    fun more(field: DiscoveryField) {
        val snapshot = state.value.snapshot ?: return
        if (!active || state.value.loading || field !in snapshot.nextPages) return
        load(keepSnapshot = true) { gateway.more(snapshot, field) }
    }
    private fun load(keepSnapshot: Boolean = false, request: suspend () -> DiscoverySnapshot) {
        job?.cancel(); val g = ++generation
        if (now() < notBefore) { mutable.value = state.value.copy(loading = false, problem = HomeError.BUSY); return }
        mutable.value = state.value.copy(snapshot = if (keepSnapshot) state.value.snapshot else null, loading = true, problem = null)
        job = scope.launch {
            try {
                val snapshot = request()
                if (!active || generation != g) return@launch
                val library = browse.state.value.feed?.id
                if (library != null && library != snapshot.libraryId) throw HomeFailure(HomeError.INVALID)
                mutable.value = state.value.copy(snapshot = snapshot, loading = false, problem = null)
            } catch (e: CancellationException) { throw e }
            catch (e: HomeFailure) {
                if (!active || generation != g) return@launch
                if (e.kind == HomeError.BUSY) notBefore = maxOf(notBefore, now() + e.retryAfterMillis.coerceIn(0, Long.MAX_VALUE - now()))
                if (e.kind in setOf(HomeError.DENIED, HomeError.CHANGED, HomeError.INVALID, HomeError.TLS)) {
                    state.value.results?.background(); browse.failDiscovery(e)
                    mutable.value = DiscoveryState(problem = e.kind)
                } else mutable.value = state.value.copy(snapshot = null, loading = false, problem = e.kind)
            }
        }
    }
    fun search(draft: DiscoveryDraft) {
        val s = state.value; val snapshot = s.snapshot ?: return
        if (!active || s.loading || s.problem != null || draft.issue(snapshot.options) != null) return
        val query = draft.normalized()
        val api = gateway.results(snapshot, query)
        job?.cancel(); generation++; active = false
        s.results?.background()
        val result = HomeStore(api, scope)
        result.foreground()
        mutable.value = s.copy(query = query, results = result)
    }
    fun close() { active = false; generation++; job?.cancel(); mutable.value = state.value.copy(loading = false) }
    fun clearResults() {
        close(); state.value.results?.background()
        mutable.value = state.value.copy(query = null, results = null)
        browse.loadPage(1)
    }
    fun invalidateResults(error: HomeError) {
        if (error !in setOf(HomeError.DENIED, HomeError.INVALID, HomeError.TLS)) return
        background(); browse.failDiscovery(HomeFailure(error))
    }
    fun background() {
        close(); state.value.results?.background(); mutable.value = DiscoveryState()
    }
}
