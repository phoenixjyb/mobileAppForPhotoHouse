package dev.photohouse.connected.core

import dev.photohouse.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Message { SIGNED_OUT_LOCAL, SIGNED_OUT_CONFIRMED, SESSION_ENDED, ACCESS_DENIED, UNAVAILABLE, TLS_ERROR, CLOSED, RATE_LIMITED, INVALID_INPUT, INVALID_RESPONSE, TOO_LARGE, MEDIA_UNAVAILABLE }
data class LiveProblem(val message: Message, val retryAtMillis: Long = 0)
/** Only the current page's IDs, never a persistent or cross-library history. */
data class PhotoNavigation(val page: Int, val assetIds: List<String>, val index: Int)
data class LiveState(
    val generation: Long = 0, val session: Session? = null, val library: String? = null,
    val gallery: Gallery? = null, val detail: Detail? = null, val captions: Captions? = null,
    val previews: Map<String, ByteArray> = emptyMap(), val busy: Boolean = false,
    val covered: Boolean = false, val problem: LiveProblem? = null,
    val photoNavigation: PhotoNavigation? = null,
    val viewingOriginal: Boolean = false, val originalPhoto: ByteArray? = null,
)

/** UI-dispatcher-confined; only the wire DTO module is shared with fixture code. */
class ConnectedStore(private val api: PhotoHouseApi, private val scope: CoroutineScope, private val now: () -> Long = System::currentTimeMillis) {
    private val mutable = MutableStateFlow(LiveState())
    val state = mutable.asStateFlow()
    private var token: Bearer? = null
    private var identity: Session? = null
    private var deadline = 0L
    private var cooldownUntil = 0L
    private fun coolingDown() = now() < cooldownUntil
    private var expiryJob: Job? = null
    private val requests = mutableSetOf<Job>()
    private var retry: (() -> Unit)? = null
    val hasSession get() = token != null
    val cachedBytes get() = state.value.previews.values.sumOf { it.size }

    private fun invalidate(keepIdentity: Boolean, cover: Boolean = false) {
        requests.toList().forEach { it.cancel() }; requests.clear(); retry = null
        if (!keepIdentity) { token = null; identity = null; deadline = 0; expiryJob?.cancel(); expiryJob = null }
        mutable.value = LiveState(generation = state.value.generation + 1, session = if (cover) null else identity, covered = cover,
            problem = if (coolingDown()) LiveProblem(Message.RATE_LIMITED, cooldownUntil) else null)
    }
    private fun active(generation: Long): Boolean {
        if (state.value.generation != generation) return false
        if (token != null && now() >= deadline) { expire(); return false }
        return true
    }
    private fun usable(): Boolean = !state.value.covered && token != null && active(state.value.generation)
    private fun allowed(): Boolean = usable() && identity?.memberships?.any { it.library_id == state.value.library && it.available } == true
    private fun launch(block: suspend (Long) -> Unit) {
        val generation = state.value.generation
        val job = scope.launch {
            try { block(generation) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (active(generation)) mutable.value = state.value.copy(busy = false, problem = problem(e))
            }
        }
        requests += job
        job.invokeOnCompletion { requests.remove(job) }
    }
    fun authenticate(phone: String, password: String, invitation: String? = null) {
        if (state.value.busy || coolingDown()) return
        invalidate(keepIdentity = false)
        mutable.value = state.value.copy(busy = true)
        launch { generation ->
            Admission.phone(phone); Admission.password(password)
            val issuedAt = now()
            val response = if (invitation == null) api.login(phone, password) else api.register(phone, password, invitation)
            if (!active(generation)) return@launch
            val credential = runCatching { Bearer.from(response) }.getOrElse { throw ApiFailure(FailureKind.INVALID_RESPONSE) }
            val session = api.session(credential)
            if (!active(generation)) return@launch
            validateSession(session)
            token = credential; identity = session; deadline = issuedAt + response.expires_in * 1000
            expiryJob = scope.launch {
                delay((deadline - now()).coerceAtLeast(0))
                if (token === credential) expire()
            }
            mutable.value = state.value.copy(session = session, busy = false)
        }
    }
    fun libraries() { if (usable()) invalidate(keepIdentity = true) }
    fun selectLibrary(library: String) {
        if (!usable() || coolingDown()) return
        invalidate(keepIdentity = true)
        if (identity?.memberships?.none { it.library_id == library && it.available } != false) {
            mutable.value = state.value.copy(problem = LiveProblem(Message.ACCESS_DENIED)); return
        }
        mutable.value = state.value.copy(library = library)
        loadPage(1)
    }
    fun loadPage(page: Int = 1) {
        if (!allowed() || coolingDown()) return
        require(page in 1..100000)
        val library = state.value.library!!; val credential = token!!
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = library, busy = true)
        launch { generation ->
            try {
                val gallery = api.gallery(credential, library, page)
                if (!active(generation)) return@launch
                validResponse(gallery.library_id == library && gallery.page == page && gallery.page_size in 1..100 && gallery.total >= 0 && gallery.items.size <= gallery.page_size)
                val unique = gallery.copy(items = gallery.items.distinctBy { it.id })
                mutable.value = state.value.copy(gallery = unique)
                var byteCount = 0
                val images = mutableMapOf<String, ByteArray>()
                for (asset in unique.items) {
                    val bytes = api.thumbnail(credential, library, asset)
                    if (!active(generation)) return@launch
                    if (bytes != null && bytes.size <= HttpsPhotoHouseApi.IMAGE_LIMIT && byteCount + bytes.size <= CACHE_LIMIT) {
                        images[asset.id] = bytes; byteCount += bytes.size
                        mutable.value = state.value.copy(previews = images.toMap())
                    }
                }
                mutable.value = state.value.copy(busy = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { readFailure(e, generation, credential) { loadPage(page) } }
        }
    }
    fun openAsset(asset: Asset) {
        val gallery = state.value.gallery
        val ids = gallery?.items?.map { it.id }.orEmpty()
        val index = ids.indexOf(asset.id)
        val navigation = if (gallery != null && index >= 0) PhotoNavigation(gallery.page, ids, index) else null
        openPhoto(asset.id, navigation)
    }
    fun adjacentPhoto(direction: Int) {
        if (state.value.busy || direction !in listOf(-1, 1)) return
        val navigation = state.value.photoNavigation ?: return
        val index = navigation.index + direction
        if (index !in navigation.assetIds.indices) return
        openPhoto(navigation.assetIds[index], navigation.copy(index = index))
    }
    fun backToPhotos() {
        loadPage(state.value.photoNavigation?.page ?: state.value.gallery?.page ?: 1)
    }
    fun openOriginalPhoto() {
        if (!allowed() || state.value.busy || state.value.viewingOriginal || coolingDown()) return
        val detail = state.value.detail ?: return
        if (!detail.originals_allowed || detail.asset.kind != "image") return
        val credential = token!!
        val navigation = state.value.photoNavigation
        retainDetail(viewingOriginal = true, busy = true)
        launch { generation ->
            try {
                val bytes = api.originalPhoto(credential, detail.library_id, detail.asset.id)
                if (!active(generation)) return@launch
                if (bytes.size > HttpsPhotoHouseApi.ORIGINAL_LIMIT) throw ApiFailure(FailureKind.TOO_LARGE)
                validResponse(bytes.isNotEmpty())
                mutable.value = state.value.copy(originalPhoto = bytes, busy = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // Retry reloads metadata/permission first; original access remains explicit.
                readFailure(e, generation, credential) { openPhoto(detail.asset.id, navigation) }
            }
        }
    }
    fun closeOriginalPhoto() {
        if (!usable()) return
        if (state.value.viewingOriginal) retainDetail(viewingOriginal = false, busy = false)
    }
    private fun retainDetail(viewingOriginal: Boolean, busy: Boolean) {
        val previous = state.value
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = previous.library, detail = previous.detail,
            captions = previous.captions, previews = previous.previews, photoNavigation = previous.photoNavigation,
            viewingOriginal = viewingOriginal, busy = busy)
    }
    private fun openPhoto(assetId: String, navigation: PhotoNavigation?) {
        if (!allowed() || coolingDown()) return
        val library = state.value.library!!; val credential = token!!
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = library, busy = true, photoNavigation = navigation)
        launch { generation ->
            try {
                val detail = api.detail(credential, library, assetId)
                if (!active(generation)) return@launch
                validResponse(detail.library_id == library && detail.asset.id == assetId)
                val captions = api.captions(credential, library, assetId)
                if (!active(generation)) return@launch
                validResponse(captions.library_id == library && captions.asset_id == assetId && captions.items.size <= 20 && captions.items.map { it.id }.distinct().size == captions.items.size)
                val bytes = api.thumbnail(credential, library, detail.asset)
                if (!active(generation)) return@launch
                validResponse(bytes == null || bytes.size <= HttpsPhotoHouseApi.IMAGE_LIMIT)
                mutable.value = state.value.copy(detail = detail, captions = captions, busy = false,
                    previews = if (bytes == null) emptyMap() else mapOf(assetId to bytes))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { readFailure(e, generation, credential) { openPhoto(assetId, navigation) } }
        }
    }
    private suspend fun readFailure(error: Exception, generation: Long, credential: Bearer, retryRead: () -> Unit) {
        if (!active(generation)) return
        mutable.value = state.value.copy(gallery = null, detail = null, captions = null, previews = emptyMap(), photoNavigation = null, originalPhoto = null, viewingOriginal = false, busy = false, problem = problem(error))
        if (error is ApiFailure && error.status == 401) {
            mutable.value = state.value.copy(busy = true)
            try {
                val session = api.session(credential)
                if (!active(generation)) return
                validateSession(session, identity?.account_id)
                identity = session
                invalidate(keepIdentity = true)
                mutable.value = state.value.copy(problem = LiveProblem(Message.ACCESS_DENIED))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (!active(generation)) return
                if (e is ApiFailure && e.status == 401) expire()
                else {
                    invalidate(keepIdentity = true, cover = true)
                    mutable.value = state.value.copy(problem = problem(e))
                    retry = { foreground() }
                }
            }
        } else if (error is ApiFailure && (error.status == 429 || error.status in 500..599 || error.kind == FailureKind.OFFLINE)) retry = retryRead
    }
    fun acceptInvitation(code: String) {
        if (!usable() || state.value.busy || coolingDown()) return
        val credential = token!!
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(busy = true)
        launch { generation ->
            try {
                api.acceptInvitation(credential, code)
                if (!active(generation)) return@launch
                val session = api.session(credential)
                if (!active(generation)) return@launch
                validateSession(session, identity?.account_id)
                identity = session
                mutable.value = state.value.copy(session = session, busy = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { readFailure(e, generation, credential) { background(); foreground() } }
        }
    }
    fun background() { invalidate(keepIdentity = true, cover = true) }
    fun foreground() {
        if (!state.value.covered || coolingDown()) return
        val credential = token
        if (credential == null) { invalidate(keepIdentity = false); return }
        if (now() >= deadline) { expire(); return }
        invalidate(keepIdentity = true, cover = true)
        mutable.value = state.value.copy(busy = true)
        launch { generation ->
            try {
                val session = api.session(credential)
                if (!active(generation)) return@launch
                validateSession(session, identity?.account_id)
                identity = session
                mutable.value = state.value.copy(session = session, covered = false, busy = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (!active(generation)) return@launch
                if (e is ApiFailure && e.status == 401) expire()
                else { mutable.value = state.value.copy(busy = false, problem = problem(e)); retry = { foreground() } }
            }
        }
    }
    fun logout() {
        val credential = token
        invalidate(keepIdentity = false)
        mutable.value = state.value.copy(problem = LiveProblem(Message.SIGNED_OUT_LOCAL, cooldownUntil))
        if (credential == null) return
        launch { generation ->
            try {
                api.logout(credential)
                if (active(generation)) mutable.value = state.value.copy(problem = LiveProblem(Message.SIGNED_OUT_CONFIRMED, cooldownUntil))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Local logout remains final even when server acknowledgement fails. */ }
        }
    }
    private fun expire() { invalidate(keepIdentity = false); mutable.value = state.value.copy(problem = LiveProblem(Message.SESSION_ENDED)) }
    fun canRetry(at: Long = now()) = retry != null && at >= cooldownUntil
    fun retry() { if (canRetry()) { val action = retry; retry = null; action?.invoke() } }
    private fun problem(error: Exception): LiveProblem {
        if (error is IllegalArgumentException) return LiveProblem(Message.INVALID_INPUT)
        if (error !is ApiFailure) return LiveProblem(Message.INVALID_RESPONSE)
        val message = when {
            error.kind == FailureKind.TOO_LARGE -> Message.TOO_LARGE
            error.status == 404 -> Message.MEDIA_UNAVAILABLE
            error.kind == FailureKind.TLS -> Message.TLS_ERROR
            error.kind == FailureKind.OFFLINE || error.status in 500..599 -> Message.UNAVAILABLE
            error.status == 401 -> Message.ACCESS_DENIED
            error.status == 403 || error.status in 300..399 -> Message.CLOSED
            error.status == 429 -> Message.RATE_LIMITED
            error.kind == FailureKind.INVALID_INPUT || error.status == 400 || error.status == 409 -> Message.INVALID_INPUT
            else -> Message.INVALID_RESPONSE
        }
        val wait = error.retryAfterMillis.coerceAtLeast(0)
        val at = if (Long.MAX_VALUE - now() < wait) Long.MAX_VALUE else now() + wait
        if (message == Message.RATE_LIMITED) cooldownUntil = maxOf(cooldownUntil, at)
        return LiveProblem(message, if (message == Message.RATE_LIMITED) cooldownUntil else 0)
    }
    private fun validateSession(session: Session, account: String? = null) {
        validResponse(session.account_id.isNotBlank() && (account == null || session.account_id == account))
        validResponse(session.memberships.all { it.revision >= 1 && it.originals in 0..1 })
        validResponse(session.memberships.map { it.library_id }.distinct().size == session.memberships.size)
    }
    private fun validResponse(condition: Boolean) { if (!condition) throw ApiFailure(FailureKind.INVALID_RESPONSE) }
    companion object { const val CACHE_LIMIT = 8 * 1024 * 1024 }
}
