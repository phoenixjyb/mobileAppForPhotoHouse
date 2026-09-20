package dev.photohouse.connected.core

import dev.photohouse.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GalleryMedia(val wire: String) { ALL("all"), PHOTOS("image"), VIDEOS("video"), PREPARED_VIDEOS("prepared_video");
    val assetKind get() = if (this == PREPARED_VIDEOS) "video" else wire
}
enum class Message { SESSION_STORAGE_UNAVAILABLE, SIGNED_OUT_LOCAL, SIGNED_OUT_CONFIRMED, SESSION_ENDED, ACCESS_DENIED, UNAVAILABLE, TLS_ERROR, CLOSED, RATE_LIMITED, INVALID_INPUT, INVALID_RESPONSE, TOO_LARGE, MEDIA_UNAVAILABLE, DISCOVERY_CHANGED, DISCOVERY_INPUT, VIDEO_NOT_READY, VIDEO_CHANGED, VIDEO_BUSY, PLAYBACK_UNAVAILABLE }
data class LiveProblem(val message: Message, val retryAtMillis: Long = 0, val playbackFailure: VideoPlaybackFailure? = null)
/** Only the current page's IDs, never a persistent or cross-library history. */
data class PhotoNavigation(val page: Int, val assetIds: List<String>, val index: Int, val discovery: PhoneDiscoveryState? = null, val media: GalleryMedia = GalleryMedia.ALL)
data class StoryReading(val page: Int = 1, val result: ProtectedStoryPage? = null, val busy: Boolean = false, val problem: LiveProblem? = null)
data class LiveState(
    val generation: Long = 0, val session: Session? = null, val library: String? = null,
    val gallery: Gallery? = null, val detail: Detail? = null, val captions: Captions? = null,
    val previews: Map<String, ByteArray> = emptyMap(), val busy: Boolean = false,
    val covered: Boolean = false, val problem: LiveProblem? = null,
    val photoNavigation: PhotoNavigation? = null,
    val video: VideoReader? = null,
    val viewingOriginal: Boolean = false, val originalPhoto: ByteArray? = null,
    val photoSlideshow: Boolean = false, val photoOriginalQuality: Boolean = false,
    val photoPreviewOnly: Boolean = false,
    val discovery: PhoneDiscoveryState? = null,
    val stories: StoryReading? = null,
    val media: GalleryMedia = GalleryMedia.ALL,
    val storyEditor: ProtectedStoryEditorStore? = null,
)

/** UI-dispatcher-confined; only the wire DTO module is shared with fixture code. */
class ConnectedStore(private val api: PhotoHouseApi, private val scope: CoroutineScope, private val persistence: SessionPersistence? = null, private val now: () -> Long = System::currentTimeMillis) {
    private val mutable = MutableStateFlow(LiveState())
    val state = mutable.asStateFlow()
    private var restorationAttempted = false
    private var token: Bearer? = null
    private var identity: Session? = null
    private var deadline = 0L
    private var cooldownUntil = 0L
    private fun coolingDown() = now() < cooldownUntil
    private var expiryJob: Job? = null
    private val requests = mutableSetOf<Job>()
    private var retry: (() -> Unit)? = null
    val protectedNativeV2Enabled get() = api.protectedNativeV2Enabled
    val mediaFilterEnabled get() = api.mediaFilterEnabled
    val preparedBrowseEnabled get() = api.preparedBrowseEnabled
    val canRememberSession get() = persistence != null
    val preparedVideoEnabled get() = api.preparedVideoEnabled
    val photoDeliveryEnabled get() = api.photoDeliveryEnabled
    val discoveryEnabled get() = api.discoveryEnabled
    val hasSession get() = token != null
    val cachedBytes get() = state.value.previews.values.sumOf { it.size }

    private fun invalidate(keepIdentity: Boolean, cover: Boolean = false) {
        state.value.video?.close()
        state.value.storyEditor?.close()
        requests.toList().forEach { it.cancel() }; requests.clear(); retry = null
        var storageFailed = false
        if (!keepIdentity) {
            storageFailed = runCatching { persistence?.clear() }.isFailure
            token = null; identity = null; deadline = 0; expiryJob?.cancel(); expiryJob = null }
        mutable.value = LiveState(generation = state.value.generation + 1, session = if (cover) null else identity, covered = cover,
            problem = if (storageFailed) LiveProblem(Message.SESSION_STORAGE_UNAVAILABLE) else if (coolingDown()) LiveProblem(Message.RATE_LIMITED, cooldownUntil) else null)
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
    /** Restore only a still-valid credential, then reauthorize before showing any private data. */
    fun restoreSession() {
        if (restorationAttempted || token != null || state.value.busy) return
        restorationAttempted = true
        val saved = runCatching { persistence?.load() }.getOrElse {
            mutable.value = state.value.copy(problem = LiveProblem(Message.SESSION_STORAGE_UNAVAILABLE)); return
        } ?: return
        val credential = runCatching {
            require(saved.issuedAtMillis >= 0 && saved.issuedAtMillis <= now() &&
                saved.expiresAtMillis > now() && saved.expiresAtMillis - saved.issuedAtMillis == 86400000L)
            Bearer.from(SessionToken(86400, saved.accessToken, "Bearer"))
        }.getOrElse { invalidate(keepIdentity = false); return }
        token = credential; deadline = saved.expiresAtMillis
        armExpiry(credential)
        mutable.value = state.value.copy(covered = true)
        foreground()
    }
    private fun armExpiry(credential: Bearer) {
        expiryJob?.cancel()
        expiryJob = scope.launch {
            delay((deadline - now()).coerceAtLeast(0))
            if (token === credential) expire()
        }
    }
    fun authenticate(phone: String, password: String, invitation: String? = null, name: String? = null, remember: Boolean = true) {
        if (state.value.busy || coolingDown()) return
        restorationAttempted = true
        invalidate(keepIdentity = false)
        if (state.value.problem?.message == Message.SESSION_STORAGE_UNAVAILABLE) return
        mutable.value = state.value.copy(busy = true)
        launch { generation ->
            Admission.phone(phone); Admission.password(password, protectedNativeV2 = api.protectedNativeV2Enabled, registration = invitation != null)
            val issuedAt = now()
            val response = when {
                invitation == null -> api.login(phone, password)
                api.protectedNativeV2Enabled -> api.registerNamed(phone, password, invitation, Admission.displayName(name.orEmpty()))
                else -> api.register(phone, password, invitation)
            }
            if (!active(generation)) return@launch
            val credential = runCatching { Bearer.from(response) }.getOrElse { throw ApiFailure(FailureKind.INVALID_RESPONSE) }
            val session = api.session(credential)
            if (!active(generation)) return@launch
            validateSession(session)
            val expiresAt = issuedAt + response.expires_in * 1000
            if (now() >= expiresAt) { expire(); return@launch }
            token = credential; identity = session; deadline = expiresAt
            armExpiry(credential)
            val saved = !remember || runCatching {
                persistence?.save(RememberedSession(response.access_token, issuedAt, expiresAt))
            }.isSuccess
            mutable.value = state.value.copy(session = session, busy = false,
                problem = if (!saved) LiveProblem(Message.SESSION_STORAGE_UNAVAILABLE) else state.value.problem)
        }
    }
    /** Stories stay scoped to this visible detail and never enter Home/TV state. */
    fun loadStories(page: Int = 1) {
        if (!api.protectedNativeV2Enabled || !allowed() || state.value.busy ||
            state.value.stories?.busy == true || state.value.viewingOriginal || state.value.video != null ||
            coolingDown() || state.value.storyEditor != null || page !in 1..100000) return
        val detail = state.value.detail ?: return
        val library = state.value.library!!; val credential = token!!
        mutable.value = state.value.copy(stories = StoryReading(page, busy = true))
        launch { generation ->
            try {
                val result = api.stories(credential, library, detail.asset.id, page)
                if (!active(generation)) return@launch
                validResponse(result.libraryId == library && result.assetId == detail.asset.id && result.page == page &&
                    result.items.size <= 5 && result.items.all { it.assetId == detail.asset.id } &&
                    result.items.map { it.id }.distinct().size == result.items.size)
                mutable.value = state.value.copy(stories = StoryReading(page, result))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (!active(generation)) return@launch
                if (e is ApiFailure && e.status in listOf(401, 403)) {
                    readFailure(e, generation, credential) { openPhoto(detail.asset.id, null) }
                } else mutable.value = state.value.copy(stories = StoryReading(page, problem = problem(e)))
            }
        }
    }
    fun editStory(storyId: String? = null) {
        if (!api.protectedNativeV2Enabled || !allowed() || state.value.busy || coolingDown() ||
            state.value.storyEditor != null || state.value.viewingOriginal || state.value.video != null) return
        val reading = state.value.stories?.takeIf { !it.busy }?.result ?: return
        val detail = state.value.detail ?: return
        if (reading.assetId != detail.asset.id || reading.libraryId != state.value.library) return
        val initial = if (storyId == null) null else reading.items.firstOrNull { it.id == storyId && it.canEdit } ?: return
        if (initial == null && !reading.canCreate) return
        val generation = state.value.generation
        val credential = token!!; val library = state.value.library!!; val assetId = detail.asset.id
        fun checkScope() {
            if (!active(generation) || !allowed() || state.value.detail?.asset?.id != assetId)
                throw CancellationException("Story editor closed")
        }
        val editor = ProtectedStoryEditorStore(scope, assetId, initial,
            save = { mutation ->
                checkScope()
                val result = api.saveStory(credential, library, mutation)
                checkScope()
                validResponse(result.assetId == assetId && (mutation.storyId == null || result.id == mutation.storyId))
                result
            },
            reload = {
                checkScope()
                val result = initial?.let { api.currentStory(credential, library, assetId, it.id) }
                checkScope(); result
            },
            onSaved = { _ ->
                if (active(generation)) {
                    // Show the server's current revision, including an exact retry whose result was edited later.
                    // The dialog displays the returned story; refresh page metadata after closing.
                    mutable.value = state.value.copy(stories = null)
                }
            },
            onDenied = {
                if (active(generation)) {
                    invalidate(keepIdentity = true, cover = true)
                    mutable.value = state.value.copy(problem = LiveProblem(Message.ACCESS_DENIED))
                    retry = { foreground() }
                }
            }, now = now)
        mutable.value = state.value.copy(storyEditor = editor)
    }
    fun closeStoryEditor() {
        state.value.storyEditor?.close()
        mutable.value = state.value.copy(storyEditor = null)
        loadStories(1)
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
    fun selectMedia(media: GalleryMedia) {
        if (!mediaFilterEnabled || media == GalleryMedia.PREPARED_VIDEOS && !preparedBrowseEnabled) return
        loadPage(1, media)
    }
    fun loadPage(page: Int = 1, media: GalleryMedia = state.value.media) {
        if (!allowed() || coolingDown()) return
        require(page in 1..100000 && (media == GalleryMedia.ALL || mediaFilterEnabled))
        require(media != GalleryMedia.PREPARED_VIDEOS || preparedBrowseEnabled)
        val library = state.value.library!!; val credential = token!!
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = library, busy = true, media = media)
        launch { generation ->
            try {
                // The first gallery read can lose a freshly established connection. Retry
                // exactly once, only for a transport-level OFFLINE result. The retry stays
                // inside this generation so logout/background/expiry cancellation wins.
                val gallery = galleryReadWithOfflineRecovery(credential, library, page, media, generation)
                if (!active(generation)) return@launch
                validResponse(gallery.library_id == library && gallery.page == page && gallery.page_size in 1..100 && gallery.total >= 0 && gallery.items.size <= gallery.page_size)
                validResponse(media == GalleryMedia.ALL || gallery.items.all { it.kind == media.assetKind })
                val unique = gallery.copy(items = gallery.items.distinctBy { it.id })
                mutable.value = state.value.copy(gallery = unique)
                var byteCount = 0
                val images = mutableMapOf<String, ByteArray>()
                for (asset in unique.items) {
                    val bytes = try { api.thumbnail(credential, library, asset) }
                    catch (e: ApiFailure) {
                        // A missing preview must not discard an already authorized page.
                        // Denials, TLS, invalid data and rate limits still close the view.
                        if (e.kind == FailureKind.OFFLINE || e.kind == FailureKind.HTTP &&
                            e.status in 502..504 && e.retryAfterMillis == 0L) null else throw e
                    }
                    if (!active(generation)) return@launch
                    if (bytes != null && bytes.size <= HttpsPhotoHouseApi.IMAGE_LIMIT && byteCount + bytes.size <= CACHE_LIMIT) {
                        images[asset.id] = bytes; byteCount += bytes.size
                        mutable.value = state.value.copy(previews = images.toMap())
                    }
                }
                mutable.value = state.value.copy(busy = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { readFailure(e, generation, credential) { loadPage(page, media) } }
        }
    }
    private suspend fun galleryReadWithOfflineRecovery(
        credential: Bearer,
        library: String,
        page: Int,
        media: GalleryMedia,
        generation: Long,
    ): Gallery {
        try {
            return api.gallery(credential, library, page, media)
        } catch (e: ApiFailure) {
            if (e.kind != FailureKind.OFFLINE) throw e
            delay(GALLERY_RECOVERY_DELAY_MILLIS)
            if (!active(generation)) throw CancellationException("Gallery read is no longer active")
            return api.gallery(credential, library, page, media)
        }
    }
    fun navigatePage(page: Int) { if (state.value.discovery != null) searchDiscoveryPage(page) else loadPage(page) }
    fun openDiscovery() {
        if (!api.discoveryEnabled || !allowed() || coolingDown()) return
        loadDiscoveryFacet(PhoneFacet.PEOPLE, 1, PhoneDiscoveryState())
    }
    fun editDiscovery() {
        if (!allowed() || state.value.busy) return
        val current = state.value.discovery ?: return
        val library = state.value.library
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = library, discovery = current.copy(editing = true, result = null))
    }
    fun updateDiscoveryFilters(filters: PhoneFilters) {
        if (!allowed() || state.value.busy) return
        val current = state.value.discovery ?: return
        if (!current.editing || current.snapshot == null) return
        // Only records actually shown in this scope can become selected IDs.
        for ((next, previous, field) in listOf(Triple(filters.people, current.filters.people, PhoneFacet.PEOPLE),
            Triple(filters.tags, current.filters.tags, PhoneFacet.TAGS), Triple(filters.places, current.filters.places, PhoneFacet.PLACES))) {
            val known = previous + (current.facetPage?.takeIf { it.facet == field }?.items ?: emptyList()) +
                (if (field == PhoneFacet.PEOPLE) current.snapshot.pins else emptyList())
            if (next.size > 20 || next.any { choice -> choice !in known } || next.map { it.id }.distinct().size != next.size) return
        }
        mutable.value = state.value.copy(discovery = current.copy(filters = filters, inputInvalid = false))
    }
    fun loadDiscoveryFacet(facet: PhoneFacet, page: Int = 1, context: PhoneDiscoveryState? = state.value.discovery) {
        if (!api.discoveryEnabled || !allowed() || coolingDown() || page !in 1..5000) return
        val previous = context ?: PhoneDiscoveryState()
        val snapshot = previous.snapshot
        if (page > 1 && snapshot == null) return
        val library = state.value.library!!; val credential = token!!
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = library, busy = true, discovery = previous.copy(editing = true, facetPage = null))
        launch { generation ->
            try {
                val response = api.facets(credential, library, facet, page, snapshot?.binding)
                if (!active(generation)) return@launch
                validResponse(response.snapshot.library == library && response.facet == facet && response.page == page && response.size == 50)
                validResponse(snapshot == null || response.snapshot == snapshot)
                mutable.value = state.value.copy(busy = false, discovery = previous.copy(snapshot = response.snapshot,
                    facetPage = response, editing = true, changed = false, inputInvalid = false))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { discoveryFailure(e, generation, credential, library) }
        }
    }
    fun applyDiscovery() {
        val context = state.value.discovery ?: return
        if (state.value.busy || context.snapshot == null) return
        if (runCatching { context.filters.json().keys.all { it in context.snapshot.enabled } }.getOrDefault(false)) {
            searchDiscoveryPage(1, context.copy(result = null))
        } else mutable.value = state.value.copy(discovery = context.copy(inputInvalid = true))
    }
    fun searchDiscoveryPage(page: Int, context: PhoneDiscoveryState? = state.value.discovery) {
        if (!api.discoveryEnabled || !allowed() || coolingDown() || page !in 1..100000) return
        val previous = context ?: return; val snapshot = previous.snapshot ?: return
        val fingerprint = if (page == 1) null else previous.result?.fingerprint ?: return
        val library = state.value.library!!; val credential = token!!
        if (snapshot.library != library) return
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = library, busy = true, discovery = previous.copy(editing = false))
        launch { generation ->
            try {
                val result = api.search(credential, library, snapshot.binding, previous.filters, page, fingerprint)
                if (!active(generation)) return@launch
                validResponse(result.gallery.library_id == library && result.gallery.page == page && result.gallery.page_size == 50 &&
                    result.binding == snapshot.binding && (fingerprint == null || result.fingerprint == fingerprint))
                mutable.value = state.value.copy(gallery = result.gallery, discovery = previous.copy(result = result, editing = false))
                val images = mutableMapOf<String, ByteArray>(); var byteCount = 0
                for (asset in result.gallery.items) {
                    val bytes = try { api.thumbnail(credential, library, asset) }
                    catch (e: ApiFailure) {
                        // A missing preview must not discard an already authorized page.
                        // Denials, TLS, invalid data and rate limits still close the view.
                        if (e.kind == FailureKind.OFFLINE || e.kind == FailureKind.HTTP &&
                            e.status in 502..504 && e.retryAfterMillis == 0L) null else throw e
                    }
                    if (!active(generation)) return@launch
                    if (bytes != null && bytes.size <= HttpsPhotoHouseApi.IMAGE_LIMIT && byteCount + bytes.size <= CACHE_LIMIT) {
                        images[asset.id] = bytes; byteCount += bytes.size
                        mutable.value = state.value.copy(previews = images.toMap())
                    }
                }
                mutable.value = state.value.copy(busy = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { discoveryFailure(e, generation, credential, library) }
        }
    }
    private suspend fun discoveryFailure(error: Exception, generation: Long, credential: Bearer, library: String) {
        if (!active(generation)) return
        if (error is ApiFailure && error.status == 409) {
            invalidate(keepIdentity = true)
            mutable.value = state.value.copy(library = library, discovery = PhoneDiscoveryState(changed = true), problem = LiveProblem(Message.DISCOVERY_CHANGED))
            // The user obtains fresh facets, then explicitly applies a new query.
        } else {
            readFailure(error, generation, credential) { openDiscovery() }
            if (active(generation) && error is ApiFailure && error.status == 400)
                mutable.value = state.value.copy(problem = LiveProblem(Message.DISCOVERY_INPUT))
        }
    }
    fun openAssetById(input: String) {
        val id = input.trim()
        if (!validAssetLookupId(id) || !allowed() || coolingDown()) return
        val current = state.value
        val navigation = current.gallery?.let { PhotoNavigation(it.page, listOf(id), 0, current.discovery, current.media) }
        // Detail, captions and media keep the same library/session authorization as a card tap.
        openPhoto(id, navigation)
    }
    fun openMedia(asset: Asset) = openAsset(asset, viewMedia = true)
    fun openAsset(asset: Asset, viewMedia: Boolean = false) {
        val gallery = state.value.gallery
        val ids = gallery?.items?.map { it.id }.orEmpty()
        val index = ids.indexOf(asset.id)
        val navigation = if (gallery != null && index >= 0) PhotoNavigation(gallery.page, ids, index, state.value.discovery, state.value.media) else null
        openPhoto(asset.id, navigation, mediaAfterLoad = viewMedia)
    }
    fun adjacentPhoto(direction: Int) {
        if (state.value.busy || direction !in listOf(-1, 1)) return
        val navigation = state.value.photoNavigation ?: return
        val index = navigation.index + direction
        if (index !in navigation.assetIds.indices) return
        openPhoto(navigation.assetIds[index], navigation.copy(index = index))
    }
    fun backToPhotos() {
        val navigation = state.value.photoNavigation
        val context = navigation?.discovery
        if (context != null) searchDiscoveryPage(navigation.page, context)
        else loadPage(navigation?.page ?: state.value.gallery?.page ?: 1, navigation?.media ?: state.value.media)
    }
    fun openVideo() = startVideo(prepared = api.preparedVideoEnabled)
    fun openOriginalVideo() = startVideo(prepared = false)
    private fun startVideo(prepared: Boolean) {
        if (!allowed() || state.value.busy || state.value.video != null || coolingDown()) return
        val detail = state.value.detail ?: return
        if (detail.asset.kind != "video" || (!prepared && !detail.originals_allowed)) return
        val credential = token!!
        val navigation = state.value.photoNavigation
        retainDetail(viewingOriginal = false, busy = prepared)
        val generation = state.value.generation
        fun attach(info: PreparedVideoInfo?) {
            if (!active(generation)) return
            val reader = VideoReader({ start, length ->
                if (info != null) api.preparedVideoRange(credential, detail.library_id, detail.asset.id, info, start, length)
                else api.videoRange(credential, detail.library_id, detail.asset.id, start, length)
            }, deadline, now, initialSize = info?.bytes ?: -1L, readAhead = prepared, retryTransientRead = prepared) { error ->
                scope.launch {
                    if (active(generation)) {
                        if (prepared) preparedPlaybackFailure(error, generation, credential)
                        else readFailure(error, generation, credential) { openPhoto(detail.asset.id, navigation) }
                    }
                }
            }
            mutable.value = state.value.copy(video = reader, busy = false)
        }
        if (!prepared) attach(null)
        else launch {
            try {
                val info = api.preparedVideoInfo(credential, detail.library_id, detail.asset.id)
                attach(info)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { preparedPlaybackFailure(e, generation, credential) }
        }
    }
    private suspend fun preparedPlaybackFailure(error: Exception, generation: Long, credential: Bearer) {
        if (!active(generation)) return
        if (error is ApiFailure && error.status in listOf(401, 403)) {
            readFailure(error, generation, credential) { } // Privacy handling owns denial; never fall back.
            return
        }
        retainDetail(viewingOriginal = false, busy = false)
        val generic = problem(error)
        val message = when ((error as? ApiFailure)?.status) {
            404 -> Message.VIDEO_NOT_READY
            409 -> Message.VIDEO_CHANGED
            429 -> Message.VIDEO_BUSY
            503 -> Message.PLAYBACK_UNAVAILABLE
            else -> generic.message
        }
        mutable.value = state.value.copy(problem = generic.copy(message = message))
        if (error is ApiFailure && (error.status in listOf(404, 409, 429, 503) || error.kind == FailureKind.OFFLINE))
            retry = { openVideo() } // Explicit retry repeats HEAD; no automatic loop or original fallback.
    }
    fun closeVideo(reader: VideoReader? = state.value.video) {
        if (state.value.video !== reader) return
        if (usable() && state.value.video != null) retainDetail(viewingOriginal = false, busy = false)
    }
    fun videoPlaybackFailed(reader: VideoReader, nativeFailure: Boolean = false, reason: VideoPlaybackFailure? = null) {
        if (state.value.video !== reader || !usable()) return
        // Transport failure owns its classified error and any session recheck. A native
        // failure may already have closed its source to stop reads/audio immediately.
        if (reader.hasReadFailure || reader.isClosed && !nativeFailure) return
        reader.close()
        retainDetail(viewingOriginal = false, busy = false)
        mutable.value = state.value.copy(problem = LiveProblem(Message.MEDIA_UNAVAILABLE, playbackFailure = reason))
        if (reason?.canRetry == true) retry = { openVideo() }
    }
    fun openDisplayPhoto() {
        val detail = state.value.detail ?: return
        if (!api.photoDeliveryEnabled || detail.asset.kind != "image") return
        openPhoto(detail.asset.id, state.value.photoNavigation, mediaAfterLoad = true)
    }
    /** Open only the already-authorized preview route; never infer original access. */
    fun openPreviewPhoto() {
        val detail = state.value.detail ?: return
        if (!api.protectedNativeV2Enabled || api.photoDeliveryEnabled || detail.asset.kind != "image") return
        openPhoto(detail.asset.id, state.value.photoNavigation, mediaAfterLoad = true)
    }
    fun openOriginalPhoto() {
        if (!allowed() || state.value.busy || (state.value.viewingOriginal && state.value.photoOriginalQuality) || coolingDown()) return
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
                mutable.value = state.value.copy(originalPhoto = bytes, busy = false, photoOriginalQuality = true, photoPreviewOnly = false)
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
    fun togglePhotoSlideshow() {
        if (state.value.photoSlideshow) { stopPhotoSlideshow(); return }
        val current = state.value
        val navigation = current.photoNavigation ?: return
        if (!allowed() || current.busy || !current.viewingOriginal || current.originalPhoto == null ||
            navigation.index >= navigation.assetIds.lastIndex || coolingDown()) return
        mutable.value = current.copy(photoSlideshow = true)
    }
    fun stopPhotoSlideshow() { mutable.value = state.value.copy(photoSlideshow = false) }
    /** A slideshow never wraps, crosses pages, skips denied items, or starts video audio. */
    fun advancePhotoSlideshow() {
        if (!state.value.photoSlideshow) return
        adjacentOriginalPhoto(1, fromSlideshow = true)
    }
    fun adjacentOriginalPhoto(direction: Int, fromSlideshow: Boolean = false) {
        val current = state.value
        if (!allowed() || current.busy || !current.viewingOriginal || direction !in listOf(-1, 1) ||
            (fromSlideshow && (!current.photoSlideshow || direction != 1))) return
        val navigation = current.photoNavigation ?: return
        val index = navigation.index + direction
        if (index !in navigation.assetIds.indices) { stopPhotoSlideshow(); return }
        openPhoto(navigation.assetIds[index], navigation.copy(index = index), originalAfterLoad = true, slideshow = fromSlideshow, originalQuality = current.photoOriginalQuality)
    }
    private fun retainDetail(viewingOriginal: Boolean, busy: Boolean) {
        val previous = state.value
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = previous.library, detail = previous.detail,
            captions = previous.captions, previews = previous.previews, photoNavigation = previous.photoNavigation, media = previous.media,
            viewingOriginal = viewingOriginal, busy = busy)
    }
    private fun openPhoto(assetId: String, navigation: PhotoNavigation?, originalAfterLoad: Boolean = false, slideshow: Boolean = false, mediaAfterLoad: Boolean = false, originalQuality: Boolean = false, allowTransientRetry: Boolean = true) {
        if (!allowed() || coolingDown()) return
        val library = state.value.library!!; val credential = token!!
        invalidate(keepIdentity = true)
        mutable.value = state.value.copy(library = library, busy = true, photoNavigation = navigation,
            viewingOriginal = originalAfterLoad, photoSlideshow = slideshow, media = navigation?.media ?: GalleryMedia.ALL)
        launch { generation ->
            try {
                val detail = api.detail(credential, library, assetId)
                if (!active(generation)) return@launch
                validResponse(detail.library_id == library && detail.asset.id == assetId)
                val captions = api.captions(credential, library, assetId)
                if (!active(generation)) return@launch
                validResponse(captions.library_id == library && captions.asset_id == assetId && captions.items.size <= 20 && captions.items.map { it.id }.distinct().size == captions.items.size)
                val bytes = api.detailPreview(credential, library, detail.asset)
                if (!active(generation)) return@launch
                validResponse(bytes == null || bytes.size <= HttpsPhotoHouseApi.IMAGE_LIMIT)
                val usePreview = api.protectedNativeV2Enabled && !api.photoDeliveryEnabled && !originalQuality
                val useOriginal = originalQuality || (!api.photoDeliveryEnabled && !usePreview)
                val requestedPhoto = (originalAfterLoad || mediaAfterLoad) && detail.asset.kind == "image"
                val openOriginal = requestedPhoto && (!useOriginal || detail.originals_allowed) &&
                    (!usePreview || bytes?.isNotEmpty() == true)
                mutable.value = state.value.copy(detail = detail, captions = captions, busy = openOriginal,
                    viewingOriginal = openOriginal, photoOriginalQuality = useOriginal, photoSlideshow = state.value.photoSlideshow && openOriginal,
                    photoPreviewOnly = usePreview && openOriginal,
                    problem = if (requestedPhoto && usePreview && !openOriginal) LiveProblem(Message.MEDIA_UNAVAILABLE) else null,
                    previews = if (bytes == null) emptyMap() else mapOf(assetId to bytes))
                if (openOriginal) {
                    val original = when {
                        usePreview -> bytes!!
                        useOriginal -> api.originalPhoto(credential, library, assetId)
                        else -> api.displayPhoto(credential, library, assetId)
                    }
                    if (!active(generation)) return@launch
                    if (original.size > HttpsPhotoHouseApi.ORIGINAL_LIMIT) throw ApiFailure(FailureKind.TOO_LARGE)
                    validResponse(original.isNotEmpty())
                    mutable.value = state.value.copy(originalPhoto = original, busy = false,
                        photoSlideshow = state.value.photoSlideshow && navigation != null && navigation.index < navigation.assetIds.lastIndex)
                }
                // The gallery tap requests viewing; returned detail and each byte read
                // still authorize access. Preparing a player never starts its audio.
                if (mediaAfterLoad && (api.preparedVideoEnabled || detail.originals_allowed) && detail.asset.kind == "video") openVideo()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                readFailure(e, generation, credential, allowTransientRetry = allowTransientRetry) {
                    openPhoto(assetId, navigation, originalAfterLoad, slideshow && state.value.photoSlideshow,
                        mediaAfterLoad, originalQuality, allowTransientRetry = false)
                }
            }
        }
    }
    private suspend fun readFailure(error: Exception, generation: Long, credential: Bearer, allowTransientRetry: Boolean = false, retryRead: () -> Unit) {
        if (!active(generation)) return
        // A protected photo transition is a read-only operation. Give a single
        // short-lived transport/5xx failure a bounded recovery attempt while
        // retaining the current generation and cancellation boundary. Auth
        // denial, rate limiting, malformed responses and writes remain explicit.
        if (allowTransientRetry && error is ApiFailure &&
            (error.kind == FailureKind.OFFLINE ||
                error.kind == FailureKind.HTTP && error.status in 502..504 && error.retryAfterMillis == 0L)) {
            delay(350)
            if (active(generation)) retryRead()
            return
        }
        state.value.video?.close()
        mutable.value = state.value.copy(video = null, gallery = null, detail = null, captions = null, previews = emptyMap(), photoNavigation = null, originalPhoto = null, viewingOriginal = false, photoSlideshow = false, photoPreviewOnly = false, photoOriginalQuality = false, discovery = null, stories = null, busy = false, problem = problem(error))
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
        if (!state.value.covered || state.value.busy || coolingDown()) return
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
        mutable.value = state.value.copy(problem = state.value.problem?.takeIf { it.message == Message.SESSION_STORAGE_UNAVAILABLE }
            ?: LiveProblem(Message.SIGNED_OUT_LOCAL, cooldownUntil))
        if (credential == null) return
        launch { generation ->
            try {
                api.logout(credential)
                if (active(generation)) mutable.value = state.value.copy(problem = LiveProblem(Message.SIGNED_OUT_CONFIRMED, cooldownUntil))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Local logout remains final even when server acknowledgement fails. */ }
        }
    }
    private fun expire() { invalidate(keepIdentity = false); mutable.value = state.value.copy(problem = state.value.problem?.takeIf { it.message == Message.SESSION_STORAGE_UNAVAILABLE } ?: LiveProblem(Message.SESSION_ENDED)) }
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
        val retryAfterCooldown = error.status in 502..504 && wait > 0
        if (message == Message.RATE_LIMITED || retryAfterCooldown) cooldownUntil = maxOf(cooldownUntil, at)
        return LiveProblem(message, if (message == Message.RATE_LIMITED || retryAfterCooldown) cooldownUntil else 0)
    }
    private fun validateSession(session: Session, account: String? = null) {
        validResponse(session.account_id.isNotBlank() && (account == null || session.account_id == account))
        validResponse(session.memberships.all { it.revision >= 1 && it.originals in 0..1 })
        validResponse(session.memberships.map { it.library_id }.distinct().size == session.memberships.size)
    }
    private fun validResponse(condition: Boolean) { if (!condition) throw ApiFailure(FailureKind.INVALID_RESPONSE) }
    companion object {
        const val CACHE_LIMIT = 8 * 1024 * 1024
        private const val GALLERY_RECOVERY_DELAY_MILLIS = 350L
    }
}

fun validAssetLookupId(value: String): Boolean = value.matches(Regex("[1-9][0-9]{0,18}")) && value.toLongOrNull() != null
