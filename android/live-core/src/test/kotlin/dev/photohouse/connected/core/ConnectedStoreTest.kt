package dev.photohouse.connected.core

import dev.photohouse.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectedStoreTest {
    private val asset = Asset("1", "image", 256, 256, null, null, "/assets/1/thumbnail?library=family")
    private fun membership(id: String, available: Boolean = true) = Membership(id, "approved", "viewer", 1, null, 0, available)
    @Test fun videoNavigationPreservesPageSkipsPhotosAndReauthorizesEachAsset() = runTest {
        val api = FakeApi().apply {
            preparedVideoEnabled = true
            assets = listOf(asset.copy(id = "1", kind = "video"), asset.copy(id = "2"), asset.copy(id = "3", kind = "video"))
        }
        val store = ConnectedStore(api, backgroundScope, now = { 0L })
        store.authenticate("+12025550123", "correct horse battery staple"); runCurrent()
        store.selectLibrary("family"); runCurrent()
        store.openAsset(api.assets.first(), viewMedia = true); runCurrent()
        assertNotNull(store.state.value.video)
        assertNull(store.adjacentVideoId(-1)); assertEquals("3", store.adjacentVideoId(1))
        val old = store.state.value.video!!
        store.state.value.videoBookmark!!.record(12000, 60000)
        store.adjacentVideo(1); runCurrent()
        assertTrue(old.isClosed); assertEquals("3", store.state.value.detail!!.asset.id)
        assertEquals(listOf("1", "3"), api.detailReads)
        store.adjacentVideo(-1); runCurrent()
        assertEquals(12000, store.state.value.videoBookmark!!.positionMillis)
        val stale = store.state.value.videoBookmark!!
        store.logout(); runCurrent(); stale.record(20000, 60000)
        store.authenticate("+12025550123", "correct horse battery staple"); runCurrent()
        store.selectLibrary("family"); runCurrent(); store.openAsset(api.assets.first(), viewMedia = true); runCurrent()
        assertEquals(0, store.state.value.videoBookmark!!.positionMillis)
    }
    @Test fun uploadEntryRequiresCurrentMembershipAndCannotSurvivePrivacyInvalidation() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled = true; uploadEnabled = true }
        val store = ConnectedStore(api, backgroundScope, now = { 0L })
        assertNull(store.openUpload())
        store.authenticate("+12025550123", "password"); runCurrent()
        val upload = store.openUpload()!!
        val source = UploadSource("photo.jpg", 1) { java.io.ByteArrayInputStream(byteArrayOf(1)) }
        upload.start(source, network = UploadNetwork.UNKNOWN)
        store.background()
        assertNull(store.state.value.upload)
        assertFalse(upload.approveNetwork())
        assertFalse(upload.start(source, network = UploadNetwork.UNMETERED))
        store.foreground(); runCurrent()
        assertNotSame(upload, store.openUpload())
        store.logout(); runCurrent()
        assertNull(store.state.value.upload); assertEquals(0, api.uploads)
    }
    @Test fun deniedUploadClearsTheAuthenticatedGalleryAndHistory() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled = true; uploadEnabled = true; uploadError = ApiFailure(FailureKind.HTTP,403) }
        val store = ConnectedStore(api,backgroundScope,now={0L})
        store.authenticate("+12025550123","password"); runCurrent()
        store.selectLibrary("family"); runCurrent(); assertNotNull(store.state.value.gallery)
        store.openUpload()!!.start(UploadSource("photo.jpg",1) { java.io.ByteArrayInputStream(byteArrayOf(1)) }, network=UploadNetwork.UNMETERED)
        runCurrent()
        assertFalse(store.hasSession); assertNull(store.state.value.gallery); assertNull(store.state.value.upload)
        assertTrue(store.state.value.previews.isEmpty()); assertEquals(Message.ACCESS_DENIED,store.state.value.problem?.message)
    }
    private inner class FakeApi : PhotoHouseApi {
        override var protectedNativeV2Enabled = false
        override var uploadEnabled = false
        var uploads = 0
        var uploadError: ApiFailure? = null
        override suspend fun uploadPhoto(token: Bearer, source: UploadSource, batch: String, onProgress: (Long) -> Unit): UploadReceipt {
            uploads++; uploadError?.let { throw it }
            return UploadReceipt("7", null, "synthetic", batch, "image", 1, 1, "a".repeat(64), source.bytes, 5)
        }
        override var preparedVideoEnabled = false
        var preparedHeads = 0
        var preparedReads = 0
        var originalVideoReads = 0
        var preparedError: ApiFailure? = null
        var preparedGate: CompletableDeferred<Unit>? = null
        override suspend fun preparedVideoInfo(token: Bearer, library: String, assetId: String): PreparedVideoInfo {
            preparedHeads++; preparedGate?.let { withContext(NonCancellable) { it.await() } }
            preparedError?.let { throw it }; return PreparedVideoInfo(100,"\""+"a".repeat(64)+"\"")
        }
        override suspend fun preparedVideoRange(token: Bearer, library: String, assetId: String, info: PreparedVideoInfo, start: Long, length: Int): VideoChunk {
            preparedReads++; preparedError?.let { throw it }
            return VideoChunk(start,100,ByteArray(minOf(length,100-start.toInt())))
        }
        override var photoDeliveryEnabled = false
        var displayReads = 0
        var displayError: Exception? = null
        var displayGate: CompletableDeferred<Unit>? = null
        override suspend fun displayPhoto(token: Bearer, library: String, assetId: String): ByteArray {
            displayReads++; displayGate?.let { withContext(NonCancellable) { it.await() } }
            displayError?.let { throw it }; return byteArrayOf(4, 5, 6)
        }
        var currentSession = Session("account-a", "+12025550123", listOf(membership("family"), membership("second"), membership("closed", false)))
        var sessionReads = 0; var galleryReads = 0; var imageReads = 0; var logouts = 0; var accepts = 0; var logins = 0
        var admissionError: Exception? = null
        var thumbnailGate: CompletableDeferred<Unit>? = null
        var sessionError: Exception? = null
        var galleryError: Exception? = null
        var galleryFailures: Int? = null
        var galleryResult: Gallery? = null
        var detailError: Exception? = null
        var detailFailures: Int? = null
        var detailGate: CompletableDeferred<Unit>? = null
        var originalsAllowed = false
        var originalGate: CompletableDeferred<Unit>? = null
        var originalError: Exception? = null
        var originalBytes = byteArrayOf(9, 8, 7)
        var originalReads = 0
        var videoError: Exception? = null
        val detailReads = mutableListOf<String>()
        var logoutError: Exception? = null
        var loginGate: CompletableDeferred<Unit>? = null
        var galleryGate: CompletableDeferred<Unit>? = null
        var sessionGate: CompletableDeferred<Unit>? = null
        var logoutGate: CompletableDeferred<Unit>? = null
        var images: ByteArray? = byteArrayOf(1, 2, 3)
        var thumbnailFailureAsset: String? = null
        var thumbnailFailure: Exception? = null
        var assets = listOf(asset)
        var lastRegistration: String? = null
        var registrationName: String? = null
        override suspend fun login(phone: String, password: String): SessionToken {
            logins++; admissionError?.let { throw it }; loginGate?.let { withContext(NonCancellable) { it.await() } }
            return SessionToken(86400, "T".repeat(43), "Bearer")
        }
        override suspend fun register(phone: String, password: String, code: String): SessionToken { lastRegistration = code; return login(phone, password) }
        override suspend fun registerNamed(phone: String, password: String, code: String, name: String): SessionToken {
            registrationName = name
            currentSession = currentSession.copy(displayName = name)
            return register(phone, password, code)
        }
        override suspend fun session(token: Bearer): Session {
            sessionReads++; val response = currentSession
            sessionGate?.let { withContext(NonCancellable) { it.await() } }
            sessionError?.let { throw it }; return response
        }
        override suspend fun acceptInvitation(token: Bearer, code: String) { accepts++ }
        override suspend fun logout(token: Bearer) { logouts++; logoutGate?.let { withContext(NonCancellable) { it.await() } }; logoutError?.let { throw it } }
        override suspend fun gallery(token: Bearer, library: String, page: Int): Gallery {
            galleryReads++; val response = galleryResult ?: Gallery(library, page, 50, 100, false, assets)
            galleryGate?.let { withContext(NonCancellable) { it.await() } }
            if (galleryFailures != null) {
                if (galleryFailures!! > 0) {
                    galleryFailures = galleryFailures!! - 1
                    galleryError?.let { throw it }
                }
            } else galleryError?.let { throw it }
            return response
        }
        override suspend fun detail(token: Bearer, library: String, assetId: String): Detail {
            detailReads += assetId
            val response = Detail(library, originalsAllowed, assets.first { it.id == assetId })
            detailGate?.let { withContext(NonCancellable) { it.await() } }
            if (detailFailures != null) {
                if (detailFailures!! > 0) {
                    detailFailures = detailFailures!! - 1
                    detailError?.let { throw it }
                }
            } else detailError?.let { throw it }
            return response
        }
        override suspend fun captions(token: Bearer, library: String, assetId: String) = Captions(library, assetId, false, listOf(Caption("c1", "<b>原文 literal</b>", false, false, null, null)))
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? {
            imageReads++
            val response = images
            thumbnailGate?.let { withContext(NonCancellable) { it.await() } }
            if (asset.id == thumbnailFailureAsset) thumbnailFailure?.let { throw it }
            return response
        }
        override suspend fun videoRange(token: Bearer, library: String, assetId: String, start: Long, length: Int): VideoChunk {
            originalVideoReads++; videoError?.let { throw it }
            return VideoChunk(start, 100, ByteArray(minOf(length, 100 - start.toInt())))
        }
        override suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray {
            originalReads++; val bytes = originalBytes
            originalGate?.let { withContext(NonCancellable) { it.await() } }
            originalError?.let { throw it }; return bytes
        }
    }
    private fun TestScope.store(api: FakeApi) = ConnectedStore(api, backgroundScope) { testScheduler.currentTime }
    private fun TestScope.signIn(store: ConnectedStore) { store.authenticate("+12025550123", "synthetic-password-only"); runCurrent(); assertNotNull(store.state.value.session) }

    @Test fun directLookupFetchesAuthorizedDetailOutsideCurrentPageAndReturnsToPage() = runTest {
        val api = FakeApi()
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.loadPage(31); runCurrent()
        api.assets = listOf(asset.copy(id = "901", kind = "video"))
        store.openAssetById("901"); runCurrent()
        assertEquals(listOf("901"), api.detailReads)
        assertEquals("901", store.state.value.detail?.asset?.id)
        assertNull(store.state.value.video) // Opening details does not autoplay.
        store.backToPhotos(); runCurrent()
        assertEquals(31, store.state.value.gallery?.page)
    }
    @Test fun directLookupRejectsMalformedIdsAndPreservesAuthorization() = runTest {
        val api = FakeApi()
        val store = store(api)
        store.openAssetById("1"); runCurrent(); assertTrue(api.detailReads.isEmpty())
        signIn(store); store.selectLibrary("family"); runCurrent()
        for (id in listOf("", "0", "-1", "01", "1/thumbnail", "9223372036854775808")) store.openAssetById(id)
        runCurrent(); assertTrue(api.detailReads.isEmpty())
        api.detailError = ApiFailure(FailureKind.HTTP, 403)
        store.openAssetById("1"); runCurrent()
        assertEquals(listOf("1"), api.detailReads)
        assertNull(store.state.value.detail)
        assertNull(store.state.value.video)
        assertEquals(0, api.preparedHeads)
    }

    @Test fun preparedViewerOpensAndSeeksWithoutOriginalGrant() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled=true; preparedVideoEnabled=true; assets=listOf(asset.copy(kind="video")) }
        val store=store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(api.assets.first()); runCurrent()
        val reader=store.state.value.video!!
        assertEquals(1,api.preparedHeads); assertEquals(100L,reader.size())
        assertEquals(4,reader.readAt(80,ByteArray(4),0,4)); assertEquals(1,api.preparedReads)
        assertEquals(0,api.originalVideoReads)
        store.closeVideo(); store.openOriginalVideo(); runCurrent(); assertNull(store.state.value.video)
        store.background(); runCurrent(); assertTrue(reader.isClosed)
    }
    @Test fun preparedReadinessErrorsKeepDetailAndManualRetryHonorsCooldown() = runTest {
        for ((code,message) in listOf(404 to Message.VIDEO_NOT_READY,409 to Message.VIDEO_CHANGED,429 to Message.VIDEO_BUSY,503 to Message.PLAYBACK_UNAVAILABLE)) {
            val api=FakeApi().apply { protectedNativeV2Enabled=true; preparedVideoEnabled=true; assets=listOf(asset.copy(kind="video")); preparedError=ApiFailure(FailureKind.HTTP,code,2000) }
            val store=store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openMedia(api.assets.first()); runCurrent()
            assertNull(store.state.value.video); assertNotNull(store.state.value.detail)
            assertEquals(message,store.state.value.problem?.message); assertEquals(0,api.originalVideoReads)
            if(code==429 || code == 503) { store.retry(); runCurrent(); assertEquals(1,api.preparedHeads); advanceTimeBy(2000) }
            api.preparedError=null; store.retry(); runCurrent()
            assertEquals(2,api.preparedHeads); assertNotNull(store.state.value.video)
            store.logout(); runCurrent()
        }
    }
    @Test fun latePreparedHeadCannotReopenAfterBackgroundOrLogout() = runTest {
        for(logout in listOf(false,true)) {
            val gate=CompletableDeferred<Unit>()
            val api=FakeApi().apply { protectedNativeV2Enabled=true; preparedVideoEnabled=true; assets=listOf(asset.copy(kind="video")); preparedGate=gate }
            val store=store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openMedia(api.assets.first()); runCurrent(); assertTrue(store.state.value.busy)
            if(logout) store.logout() else store.background()
            gate.complete(Unit); runCurrent()
            assertNull(store.state.value.video); assertNull(store.state.value.detail); assertNull(store.state.value.session)
        }
    }
    @Test fun preparedMidstreamFailureWinsDecoderRaceAndNeverFallsBack() = runTest {
        for (code in listOf(401, 409, 429)) {
            val api = FakeApi().apply { protectedNativeV2Enabled=true; preparedVideoEnabled=true; assets=listOf(asset.copy(kind="video")) }
            val store=store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openMedia(api.assets.first()); runCurrent()
            val reader=store.state.value.video!!
            assertEquals(4, reader.readAt(0, ByteArray(4), 0, 4))
            api.preparedError=ApiFailure(FailureKind.HTTP, code, 2000)
            // A repeated consumed range must authorize again, even with an admitted suffix.
            try { reader.readAt(0, ByteArray(4), 0, 4); fail("Read must fail") } catch (_: java.io.IOException) { }
            assertEquals(2, api.preparedReads)
            assertTrue(reader.isClosed)
            // Native player error can arrive before the queued transport callback.
            store.videoPlaybackFailed(reader, nativeFailure=true, reason=VideoPlaybackFailure.READ)
            runCurrent()
            assertNull(store.state.value.problem?.playbackFailure)
            assertEquals(when(code) { 401 -> Message.ACCESS_DENIED; 409 -> Message.VIDEO_CHANGED; else -> Message.VIDEO_BUSY }, store.state.value.problem?.message)
            assertNull(store.state.value.video); assertEquals(0,api.originalVideoReads)
            if(code==401) assertNull(store.state.value.detail) else assertNotNull(store.state.value.detail)
            store.logout(); runCurrent()
        }
    }
    @Test fun preparedDenialClearsPrivateDetailAndRechecksSession() = runTest {
        val api=FakeApi().apply { protectedNativeV2Enabled=true; preparedVideoEnabled=true; assets=listOf(asset.copy(kind="video")); preparedError=ApiFailure(FailureKind.HTTP,401) }
        val store=store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        val before=api.sessionReads
        store.openMedia(api.assets.first()); runCurrent()
        assertNull(store.state.value.detail); assertNull(store.state.value.video)
        assertEquals(before+1,api.sessionReads); assertEquals(Message.ACCESS_DENIED,store.state.value.problem?.message)
        assertEquals(0,api.originalVideoReads)
    }

    @Test fun protectedRegistrationRequiresNameBeforeSendingAndKeepsViewerMembership() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled = true }
        val store = store(api)
        store.authenticate("+12025550123", "12345678", "synthetic-invite"); runCurrent()
        assertEquals(0, api.logins); assertNull(store.state.value.session)
        assertEquals(Message.INVALID_INPUT, store.state.value.problem?.message)
        store.authenticate("+12025550123", "12345678", "synthetic-invite", "  小溪\u00a0 Jane "); runCurrent()
        assertEquals("小溪 Jane", api.registrationName)
        assertEquals("小溪 Jane", store.state.value.session?.displayName)
        assertEquals(0, store.state.value.session!!.memberships.first().originals)
        store.background(); assertNull(store.state.value.session)
        store.foreground(); runCurrent()
        assertEquals("小溪 Jane", store.state.value.session?.displayName)
        store.logout(); runCurrent(); assertNull(store.state.value.session)
    }

    @Test fun protectedThumbnailViewerDoesNotNeedOriginalPermissionOrDisplayService() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(asset); runCurrent()
        assertTrue(store.state.value.viewingOriginal); assertTrue(store.state.value.photoPreviewOnly)
        assertFalse(store.state.value.photoOriginalQuality); assertArrayEquals(api.images, store.state.value.originalPhoto)
        assertEquals(0, api.displayReads); assertEquals(0, api.originalReads)
        store.openOriginalPhoto(); runCurrent(); assertEquals(0, api.originalReads)
        store.closeOriginalPhoto(); runCurrent(); assertNull(store.state.value.originalPhoto)
        store.openPreviewPhoto(); runCurrent(); assertTrue(store.state.value.photoPreviewOnly)
        store.background(); runCurrent(); assertNull(store.state.value.originalPhoto); assertFalse(store.state.value.photoPreviewOnly)
    }

    @Test fun protectedPreviewNeverSilentlyUpgradesEvenWhenOriginalsAreAllowed() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled = true; originalsAllowed = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(asset); runCurrent(); assertTrue(store.state.value.photoPreviewOnly); assertEquals(0, api.originalReads)
        store.openOriginalPhoto(); runCurrent(); assertEquals(1, api.originalReads)
        assertTrue(store.state.value.photoOriginalQuality); assertFalse(store.state.value.photoPreviewOnly)
    }

    @Test fun missingProtectedPreviewIsUnavailableWithoutOriginalOrDisplayFallback() = runTest {
        for (bytes in listOf(null, byteArrayOf())) {
            val api = FakeApi().apply { protectedNativeV2Enabled = true; originalsAllowed = true; images = bytes }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openMedia(asset); runCurrent()
            assertFalse(store.state.value.viewingOriginal); assertFalse(store.state.value.busy)
            assertNull(store.state.value.originalPhoto); assertEquals(Message.MEDIA_UNAVAILABLE, store.state.value.problem?.message)
            assertEquals(0, api.originalReads); assertEquals(0, api.displayReads)
        }
    }

    @Test fun protectedPreviewPagingAndSlideshowStayWithinAuthorizedCurrentPage() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled = true; assets = listOf(asset, asset.copy(id = "2"), asset.copy(id = "3", kind = "video")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(asset); runCurrent(); store.adjacentOriginalPhoto(1); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id); assertTrue(store.state.value.photoPreviewOnly)
        store.togglePhotoSlideshow(); store.advancePhotoSlideshow(); runCurrent()
        assertEquals("3", store.state.value.detail?.asset?.id); assertFalse(store.state.value.photoSlideshow)
        assertFalse(store.state.value.viewingOriginal); assertNull(store.state.value.video)
        assertEquals(0, api.originalReads); assertEquals(0, api.displayReads)
    }

    @Test fun lateProtectedPreviewCannotReturnAfterPrivacyBoundaries() = runTest {
        for (action in listOf("background", "logout", "library")) {
            val api = FakeApi().apply { protectedNativeV2Enabled = true }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            api.thumbnailGate = CompletableDeferred(); store.openMedia(asset); runCurrent()
            when (action) { "background" -> store.background(); "logout" -> store.logout(); else -> store.libraries() }
            runCurrent(); api.thumbnailGate!!.complete(Unit); runCurrent()
            assertNull(store.state.value.originalPhoto); assertFalse(store.state.value.viewingOriginal)
            assertFalse(store.state.value.photoPreviewOnly)
        }
    }

    @Test fun protectedPreviewRechecksAuthorizationBeforeDisplayingBytes() = runTest {
        for (status in listOf(401, 403)) {
            val api = FakeApi().apply { protectedNativeV2Enabled = true }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openAsset(asset); runCurrent(); api.detailError = ApiFailure(FailureKind.HTTP, status)
            store.openPreviewPhoto(); runCurrent()
            assertNull(store.state.value.originalPhoto); assertFalse(store.state.value.viewingOriginal)
            assertTrue(store.state.value.previews.isEmpty()); assertEquals(0, api.originalReads)
        }
    }

    @Test fun onDemandViewerWorksWithoutOriginalPermissionAndDoesNotUpgradeIt() = runTest {
        val api = FakeApi().apply { photoDeliveryEnabled = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(asset); runCurrent()
        assertTrue(store.state.value.viewingOriginal); assertFalse(store.state.value.photoOriginalQuality)
        assertEquals(1, api.displayReads); assertEquals(0, api.originalReads)
        store.openOriginalPhoto(); runCurrent(); assertEquals(0, api.originalReads)
        store.logout(); runCurrent(); assertNull(store.state.value.originalPhoto)
    }
    @Test fun originalQualityRemainsExplicitAndDisplayFailureNeverFallsBackToOriginals() = runTest {
        val api = FakeApi().apply { photoDeliveryEnabled = true; originalsAllowed = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(asset); runCurrent(); assertEquals(0, api.originalReads)
        store.openOriginalPhoto(); runCurrent(); assertEquals(1, api.originalReads); assertTrue(store.state.value.photoOriginalQuality)
        api.displayError = ApiFailure(FailureKind.HTTP, 503)
        store.openMedia(asset); runCurrent(); assertEquals(1, api.originalReads); assertNull(store.state.value.originalPhoto)
    }
    @Test fun lateOnDemandResponseCannotReappearAfterLogout() = runTest {
        val api = FakeApi().apply { photoDeliveryEnabled = true; displayGate = CompletableDeferred() }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(asset); runCurrent(); store.logout(); runCurrent()
        api.displayGate!!.complete(Unit); runCurrent()
        assertNull(store.state.value.originalPhoto); assertFalse(store.state.value.viewingOriginal)
    }

    @Test fun galleryMediaTapUsesFreshKindAndPermissionAndDetailsRemainSeparate() = runTest {
        for (kind in listOf("image", "video")) for (permitted in listOf(false, true)) {
            val api = FakeApi().apply { assets = listOf(asset.copy(kind = kind)); originalsAllowed = permitted }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openMedia(asset); runCurrent() // Deliberately stale gallery kind for video.
            assertEquals(permitted && kind == "image", store.state.value.viewingOriginal)
            assertEquals(permitted && kind == "video", store.state.value.video != null)
            assertEquals(if (permitted && kind == "image") 1 else 0, api.originalReads)
            assertFalse(store.state.value.photoSlideshow)
            assertEquals(0, store.state.value.photoNavigation?.index)
            store.backToPhotos(); runCurrent(); store.openAsset(asset); runCurrent()
            assertFalse(store.state.value.viewingOriginal); assertNull(store.state.value.video)
            store.logout(); runCurrent()
        }
    }

    @Test fun lateGalleryMediaTapCannotOpenAfterLogoutLibraryChangeOrBackground() = runTest {
        for (kind in listOf("image", "video")) for (boundary in listOf("logout", "library", "background")) {
            val api = FakeApi().apply { assets = listOf(asset.copy(kind = kind)); originalsAllowed = true }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            val gate = CompletableDeferred<Unit>(); api.detailGate = gate
            store.openMedia(asset); runCurrent()
            when (boundary) { "logout" -> store.logout(); "library" -> store.selectLibrary("second"); else -> store.background() }
            runCurrent(); gate.complete(Unit); runCurrent()
            assertNull(store.state.value.video); assertFalse(store.state.value.viewingOriginal)
            assertNull(store.state.value.originalPhoto); assertEquals(0, api.originalReads)
        }
    }

    @Test fun refusedInvitationNeverCreatesSessionOrTriggersAutomaticAdmissionRetry() = runTest {
        // Invalid, already-used and wrong-phone codes intentionally have the same
        // non-enumerating server denial. The client cannot infer which one failed.
        for (code in listOf("synthetic-invalid", "synthetic-used", "synthetic-wrong-phone")) {
            val api = FakeApi().apply { admissionError = ApiFailure(FailureKind.HTTP, 401) }
            val store = store(api)
            store.authenticate("+12025550123", "synthetic-password-only", code); runCurrent()
            assertEquals(1, api.logins); assertEquals(0, api.sessionReads)
            assertFalse(store.hasSession); assertNull(store.state.value.session)
            assertEquals(Message.ACCESS_DENIED, store.state.value.problem?.message)
            store.retry(); store.selectLibrary("family"); runCurrent()
            assertEquals(1, api.logins); assertEquals(0, api.galleryReads); assertFalse(store.canRetry())
        }
    }
    @Test fun refreshedRevokedExpiredOrClosedLibraryCannotUsePreviousApproval() = runTest {
        for (status in listOf("revoked", "requested", "rejected", "approved")) {
            val api = FakeApi(); val store = store(api); signIn(store)
            store.selectLibrary("family"); runCurrent(); assertTrue(store.cachedBytes > 0)
            store.background()
            api.currentSession = api.currentSession.copy(memberships = listOf(membership("family", false).copy(status = status)))
            store.foreground(); runCurrent(); store.selectLibrary("family"); runCurrent()
            assertTrue(store.hasSession); assertEquals(1, api.galleryReads)
            assertEquals(0, store.cachedBytes); assertNull(store.state.value.gallery)
            assertEquals(Message.ACCESS_DENIED, store.state.value.problem?.message)
        }
    }
    @Test fun accountDisabledOrSessionRevokedDuringReadDropsCredentialsAndAllContent() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent(); store.openAsset(asset); runCurrent()
        api.galleryError = ApiFailure(FailureKind.HTTP, 401)
        api.sessionError = ApiFailure(FailureKind.HTTP, 401)
        store.backToPhotos(); runCurrent()
        assertEquals(2, api.sessionReads); assertFalse(store.hasSession)
        assertNull(store.state.value.session); assertNull(store.state.value.gallery)
        assertNull(store.state.value.detail); assertNull(store.state.value.captions)
        assertEquals(0, store.cachedBytes); assertFalse(store.canRetry())
        assertEquals(Message.SESSION_ENDED, store.state.value.problem?.message)
    }
    @Test fun lateThumbnailCannotRestoreAfterLogoutExpiryOrLibraryChange() = runTest {
        for (boundary in listOf("logout", "expiry", "library")) {
            val api = FakeApi().apply { thumbnailGate = CompletableDeferred() }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            val gate = api.thumbnailGate!!
            api.thumbnailGate = null; api.images = null
            when (boundary) {
                "logout" -> store.logout()
                "expiry" -> advanceTimeBy(86400_000)
                else -> store.selectLibrary("second")
            }
            runCurrent(); gate.complete(Unit); runCurrent()
            assertEquals(0, store.cachedBytes); assertTrue(store.state.value.previews.isEmpty())
            assertEquals(boundary == "library", store.hasSession)
            assertEquals(if (boundary == "library") "second" else null, store.state.value.library)
        }
    }

    @Test fun videoIsExplicitPermissionedAndClosedAtEveryPrivacyBoundary() = runTest {
        for (boundary in listOf("close", "background", "logout", "library", "expiry", "navigation")) {
            val api = FakeApi().apply { assets = listOf(asset.copy(kind = "video")) }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openAsset(api.assets.single()); runCurrent(); store.openVideo(); assertNull(store.state.value.video)
            api.originalsAllowed = true; store.openAsset(api.assets.single()); runCurrent()
            assertNull(store.state.value.video); store.openVideo()
            val reader = store.state.value.video!!; assertEquals(100L, reader.size())
            when (boundary) {
                "close" -> store.closeVideo()
                "background" -> store.background()
                "logout" -> store.logout()
                "library" -> store.libraries()
                "expiry" -> { advanceTimeBy(86400000); runCurrent() }
                else -> store.backToPhotos()
            }
            assertTrue(reader.isClosed); assertNull(store.state.value.video)
        }
    }
    @Test fun videoDenialRechecksOnceAndOldPlayerCannotCloseNewVideo() = runTest {
        val api = FakeApi().apply { originalsAllowed = true; assets = listOf(asset.copy(kind = "video")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(api.assets.single()); runCurrent(); store.openVideo()
        val old = store.state.value.video!!; store.closeVideo(); store.openVideo()
        val current = store.state.value.video!!; store.closeVideo(old); store.videoPlaybackFailed(old)
        assertSame(current, store.state.value.video)
        api.videoError = ApiFailure(FailureKind.HTTP, 401)
        assertTrue(runCatching { current.size() }.isFailure); runCurrent()
        assertEquals(2, api.sessionReads); assertNull(store.state.value.video); assertNull(store.state.value.detail)
        assertTrue(store.hasSession); assertEquals(Message.ACCESS_DENIED, store.state.value.problem?.message)
    }
    @Test fun playbackTimeoutRetainsDetailAndManualRetryRechecksPreparedHead() = runTest {
        val api = FakeApi().apply { protectedNativeV2Enabled=true; preparedVideoEnabled=true; assets=listOf(asset.copy(kind="video")) }
        val store=store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openMedia(api.assets.first()); runCurrent()
        val old=store.state.value.video!!
        old.close(); store.videoPlaybackFailed(old, nativeFailure=true, reason=VideoPlaybackFailure.BUFFER_TIMEOUT)
        assertTrue(old.isClosed); assertNull(store.state.value.video); assertNotNull(store.state.value.detail)
        assertEquals(VideoPlaybackFailure.BUFFER_TIMEOUT, store.state.value.problem?.playbackFailure)
        assertTrue(store.canRetry()); assertEquals(1,api.preparedHeads)
        advanceTimeBy(60000); runCurrent(); assertEquals(1,api.preparedHeads) // Never automatically reopen.
        store.retry(); runCurrent()
        assertEquals(2,api.preparedHeads); assertNotNull(store.state.value.video)
        assertNull(store.state.value.problem); assertEquals(0,api.originalVideoReads)
        val replacement=store.state.value.video!!
        store.videoPlaybackFailed(old,nativeFailure=true,reason=VideoPlaybackFailure.SEEK_TIMEOUT)
        assertSame(replacement,store.state.value.video)
    }

    @Test fun nativeFailureAfterSourceCloseRetainsDetailsAndRejectsLateOldReader() = runTest {
        val api = FakeApi().apply { originalsAllowed = true; assets = listOf(asset.copy(kind = "video")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(api.assets.single()); runCurrent(); store.openVideo()
        val reader = store.state.value.video!!
        reader.close(); store.videoPlaybackFailed(reader, nativeFailure = true)
        assertNull(store.state.value.video); assertNotNull(store.state.value.detail)
        assertEquals(Message.MEDIA_UNAVAILABLE, store.state.value.problem?.message)
        store.retry(); runCurrent(); store.openVideo()
        val replacement = store.state.value.video!!
        store.videoPlaybackFailed(reader, nativeFailure = true)
        assertSame(replacement, store.state.value.video); assertFalse(replacement.isClosed)
    }

    @Test fun videoOfflineRetryReloadsPermissionAndNeverRestartsPlayback() = runTest {
        val api = FakeApi().apply { originalsAllowed = true; assets = listOf(asset.copy(kind = "video")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(api.assets.single()); runCurrent(); store.openVideo()
        api.videoError = ApiFailure(FailureKind.OFFLINE)
        assertTrue(runCatching { store.state.value.video!!.size() }.isFailure); runCurrent()
        assertNull(store.state.value.video); assertTrue(store.canRetry())
        api.originalsAllowed = false; store.retry(); runCurrent(); store.openVideo()
        assertNotNull(store.state.value.detail); assertNull(store.state.value.video)
    }
    @Test fun originalRequiresExplicitImagePermissionAndNeverFallsBackFromMissingPreview() = runTest {
        val api = FakeApi().apply { images = null }; val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent(); store.openAsset(asset); runCurrent()
        store.openOriginalPhoto(); runCurrent(); assertEquals(0, api.originalReads)
        api.originalsAllowed = true
        store.openAsset(asset); runCurrent(); assertEquals(0, api.originalReads)
        store.openOriginalPhoto(); store.openOriginalPhoto(); runCurrent()
        assertEquals(1, api.originalReads); assertTrue(store.state.value.viewingOriginal)
        assertArrayEquals(api.originalBytes, store.state.value.originalPhoto)
        store.closeOriginalPhoto()
        assertNull(store.state.value.originalPhoto); assertFalse(store.state.value.viewingOriginal)
        assertEquals("1", store.state.value.detail?.asset?.id)
        api.assets = listOf(asset.copy(kind = "video"))
        store.openAsset(api.assets.single()); runCurrent(); store.openOriginalPhoto(); runCurrent()
        assertEquals(1, api.originalReads)
    }
    @Test fun closingOriginalCancelsLateBytesAndRetainsNavigation() = runTest {
        val api = FakeApi().apply { originalsAllowed = true; originalGate = CompletableDeferred() }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.loadPage(2); runCurrent(); store.openAsset(asset); runCurrent()
        store.openOriginalPhoto(); runCurrent(); store.closeOriginalPhoto()
        api.originalGate!!.complete(Unit); runCurrent()
        assertFalse(store.state.value.viewingOriginal); assertNull(store.state.value.originalPhoto)
        assertEquals("1", store.state.value.detail?.asset?.id)
        store.backToPhotos(); runCurrent(); assertEquals(2, store.state.value.gallery?.page)
    }
    @Test fun lateOriginalCannotCrossLogoutBackgroundLibraryOrExpiry() = runTest {
        for (boundary in listOf("logout", "background", "library", "expiry")) {
            val api = FakeApi().apply { originalsAllowed = true; originalGate = CompletableDeferred() }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openAsset(asset); runCurrent(); store.openOriginalPhoto(); runCurrent()
            when (boundary) {
                "logout" -> store.logout()
                "background" -> store.background()
                "library" -> store.selectLibrary("second")
                "expiry" -> advanceTimeBy(86400_000)
            }
            runCurrent(); api.originalGate!!.complete(Unit); runCurrent()
            assertNull(store.state.value.originalPhoto); assertFalse(store.state.value.viewingOriginal)
            assertNull(store.state.value.detail); assertNull(store.state.value.photoNavigation)
        }
    }
    @Test fun originalDenialRechecksOnceAndClearsAllPrivateContent() = runTest {
        val api = FakeApi().apply { originalsAllowed = true; originalError = ApiFailure(FailureKind.HTTP, 401) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent(); store.openOriginalPhoto(); runCurrent()
        assertEquals(2, api.sessionReads); assertEquals(1, api.originalReads)
        assertNull(store.state.value.detail); assertNull(store.state.value.originalPhoto)
        assertNull(store.state.value.photoNavigation); assertEquals(0, store.cachedBytes)
        assertTrue(store.hasSession); assertFalse(store.canRetry())
    }
    @Test fun originalBudgetAndMissingFileHaveExplicitErrors() = runTest {
        val api = FakeApi().apply { originalsAllowed = true; originalBytes = ByteArray(HttpsPhotoHouseApi.ORIGINAL_LIMIT + 1) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent(); store.openOriginalPhoto(); runCurrent()
        assertEquals(Message.TOO_LARGE, store.state.value.problem?.message)
        assertNull(store.state.value.originalPhoto)
        api.originalError = ApiFailure(FailureKind.HTTP, 404)
        store.openAsset(asset); runCurrent(); store.openOriginalPhoto(); runCurrent()
        assertEquals(Message.MEDIA_UNAVAILABLE, store.state.value.problem?.message)
    }
    @Test fun originalOfflineRetryRechecksMetadataBeforeAnotherExplicitOriginalRead() = runTest {
        val api = FakeApi().apply { originalsAllowed = true; originalError = ApiFailure(FailureKind.OFFLINE) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.loadPage(2); runCurrent(); store.openAsset(asset); runCurrent()
        store.openOriginalPhoto(); runCurrent()
        api.originalsAllowed = false; api.originalError = null
        store.retry(); runCurrent(); store.openOriginalPhoto(); runCurrent()
        assertEquals(1, api.originalReads); assertEquals(false, store.state.value.detail?.originals_allowed)
        store.backToPhotos(); runCurrent(); assertEquals(2, store.state.value.gallery?.page)
    }
    @Test fun signInRechecksOwnSessionAndClosedMembershipNeverReads() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store)
        assertEquals(1, api.sessionReads)
        store.selectLibrary("closed"); runCurrent()
        assertEquals(0, api.galleryReads); assertEquals(Message.ACCESS_DENIED, store.state.value.problem?.message)
        store.selectLibrary("second"); runCurrent()
        assertEquals("second", store.state.value.gallery?.library_id)
    }
    @Test fun missingThumbnailAndLiteralCaptionsAreNotOriginalOrHtmlFallbacks() = runTest {
        val api = FakeApi().apply { images = null }; val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent(); assertTrue(store.state.value.previews.isEmpty())
        store.openAsset(asset); runCurrent()
        assertEquals("<b>原文 literal</b>", store.state.value.captions?.items?.single()?.text)
        assertEquals(false, store.state.value.detail?.originals_allowed)
    }
    @Test fun galleryPagesDeduplicateAndBoundMemory() = runTest {
        val api = FakeApi().apply { assets = (1..10).map { asset.copy(id = it.toString()) } + asset; images = ByteArray(1024 * 1024) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        assertEquals(10, store.state.value.gallery?.items?.size)
        assertEquals(ConnectedStore.CACHE_LIMIT, store.cachedBytes)
        store.loadPage(2); runCurrent(); assertEquals(2, store.state.value.gallery?.page)
        store.libraries(); assertEquals(0, store.cachedBytes); assertNull(store.state.value.gallery)
    }
    @Test fun backgroundClearsPrivateStateAndRevalidatesBeforeUncovering() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.background(); assertTrue(store.state.value.covered); assertNull(store.state.value.session); assertEquals(0, store.cachedBytes)
        api.sessionGate = CompletableDeferred()
        store.foreground(); runCurrent(); assertTrue(store.state.value.covered)
        api.sessionGate!!.complete(Unit); runCurrent()
        assertFalse(store.state.value.covered); assertNull(store.state.value.gallery); assertEquals(2, api.sessionReads)
    }
    @Test fun lateGalleryCannotCrossLibraryOrLogoutGeneration() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store)
        val oldGate = CompletableDeferred<Unit>(); api.galleryGate = oldGate
        store.selectLibrary("family"); runCurrent()
        api.galleryGate = null; store.selectLibrary("second"); runCurrent()
        oldGate.complete(Unit); runCurrent(); assertEquals("second", store.state.value.gallery?.library_id)
        val logoutGate = CompletableDeferred<Unit>(); api.galleryGate = logoutGate
        store.loadPage(2); runCurrent(); store.logout(); logoutGate.complete(Unit); runCurrent()
        assertNull(store.state.value.session); assertNull(store.state.value.gallery); assertEquals(0, store.cachedBytes)
    }
    @Test fun lateAuthenticationCannotUncoverAfterBackground() = runTest {
        val api = FakeApi().apply { loginGate = CompletableDeferred() }; val store = store(api)
        store.authenticate("+12025550123", "synthetic-password-only"); runCurrent(); store.background()
        api.loginGate!!.complete(Unit); runCurrent()
        assertFalse(store.hasSession); assertNull(store.state.value.session); assertEquals(0, api.sessionReads)
    }
    @Test fun expiryClearsWithoutWaitingForAnotherUserAction() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        advanceTimeBy(86400_000); runCurrent()
        assertFalse(store.hasSession); assertEquals(0, store.cachedBytes)
        assertEquals(Message.SESSION_ENDED, store.state.value.problem?.message)
    }
    @Test fun object401RechecksOnceWithoutRetryingForeignRead() = runTest {
        val api = FakeApi().apply { galleryError = ApiFailure(FailureKind.HTTP, 401) }; val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent()
        assertEquals(2, api.sessionReads); assertEquals(1, api.galleryReads)
        assertTrue(store.hasSession); assertNull(store.state.value.library); assertFalse(store.canRetry())
    }
    @Test fun session401SignsOutAnd503KeepsCoverForExplicitRetry() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store)
        store.background(); api.sessionError = ApiFailure(FailureKind.HTTP, 503); store.foreground(); runCurrent()
        assertTrue(store.hasSession); assertTrue(store.state.value.covered); assertTrue(store.canRetry())
        api.sessionError = ApiFailure(FailureKind.HTTP, 401); store.retry(); runCurrent()
        assertFalse(store.hasSession); assertEquals(Message.SESSION_ENDED, store.state.value.problem?.message)
    }
    @Test fun rateLimitSurvivesNavigationAndRequiresExplicitRetry() = runTest {
        val api = FakeApi().apply { galleryError = ApiFailure(FailureKind.HTTP, 429, 7000) }; val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent(); assertFalse(store.canRetry())
        store.loadPage(); store.selectLibrary("second"); runCurrent(); assertEquals(1, api.galleryReads)
        advanceTimeBy(7000); runCurrent(); assertEquals(1, api.galleryReads)
        api.galleryError = null; store.retry(); runCurrent(); assertEquals(2, api.galleryReads)
    }
    @Test fun logoutFailureIsLocalAndLateAcknowledgementCannotReplaceNewAccount() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store)
        api.logoutError = ApiFailure(FailureKind.OFFLINE); store.logout(); runCurrent()
        assertEquals(Message.SIGNED_OUT_LOCAL, store.state.value.problem?.message); assertFalse(store.hasSession)
        signIn(store); api.logoutError = null; api.logoutGate = CompletableDeferred(); store.logout(); runCurrent()
        api.currentSession = api.currentSession.copy(account_id = "account-b"); signIn(store)
        api.logoutGate!!.complete(Unit); runCurrent()
        assertEquals("account-b", store.state.value.session?.account_id); assertNull(store.state.value.problem)
    }
    @Test fun invitationAcceptanceRefetchesMembershipAndDoesNotReplayMutation() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store)
        api.sessionError = ApiFailure(FailureKind.HTTP, 503)
        store.acceptInvitation("synthetic-invitation"); runCurrent(); assertEquals(1, api.accepts)
        api.sessionError = null; store.retry(); runCurrent()
        assertEquals(1, api.accepts); assertNotNull(store.state.value.session); assertFalse(store.state.value.covered)
    }
    @Test fun malformedIdentityOnForegroundRemainsCovered() = runTest {
        val api = FakeApi(); val store = store(api); signIn(store); store.background()
        api.currentSession = api.currentSession.copy(account_id = "unexpected-account")
        store.foreground(); runCurrent()
        assertTrue(store.state.value.covered); assertNull(store.state.value.session)
        assertEquals(Message.INVALID_RESPONSE, store.state.value.problem?.message)
    }
    @Test fun invitedRegistrationAndColdStartAreIndependentOfFixtureState() = runTest {
        val api = FakeApi(); val store = store(api)
        assertFalse(store.hasSession); assertNull(store.state.value.session)
        store.authenticate("+12025550123", "synthetic-password-only", "synthetic-invitation"); runCurrent()
        assertEquals("synthetic-invitation", api.lastRegistration); assertNotNull(store.state.value.session)
        val restarted = store(api); assertFalse(restarted.hasSession); assertNull(restarted.state.value.session)
    }
    @Test fun detailNavigationStaysOnCurrentPageAndFetchesEachPhoto() = runTest {
        val api = FakeApi().apply { assets = (1..3).map { asset.copy(id = it.toString()) } + asset }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.loadPage(2); runCurrent(); store.openAsset(asset); runCurrent()
        assertEquals(listOf("1", "2", "3"), store.state.value.photoNavigation?.assetIds)
        store.adjacentPhoto(-1); store.adjacentPhoto(2); runCurrent()
        assertEquals(listOf("1"), api.detailReads)
        store.adjacentPhoto(1); runCurrent(); assertEquals("2", store.state.value.detail?.asset?.id)
        store.adjacentPhoto(1); runCurrent(); store.adjacentPhoto(1); runCurrent()
        assertEquals(listOf("1", "2", "3"), api.detailReads)
        store.adjacentPhoto(-1); runCurrent(); assertEquals("2", store.state.value.detail?.asset?.id)
        store.backToPhotos(); runCurrent()
        assertEquals(2, store.state.value.gallery?.page)
        assertNull(store.state.value.photoNavigation); assertNull(store.state.value.detail)
    }
    @Test fun pendingAdjacentPhotoCannotRestoreAfterBackOrLibrarySwitch() = runTest {
        for (switchLibrary in listOf(false, true)) {
            val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.loadPage(2); runCurrent(); store.openAsset(asset); runCurrent()
            api.detailGate = CompletableDeferred(); store.adjacentPhoto(1); runCurrent()
            assertTrue(store.state.value.busy); assertNull(store.state.value.detail)
            assertEquals(0, store.cachedBytes)
            store.adjacentPhoto(-1); runCurrent(); assertEquals(2, api.detailReads.size)
            if (switchLibrary) store.selectLibrary("second") else store.backToPhotos()
            runCurrent(); api.detailGate!!.complete(Unit); runCurrent()
            assertNull(store.state.value.photoNavigation); assertNull(store.state.value.detail)
            assertEquals(if (switchLibrary) "second" else "family", store.state.value.gallery?.library_id)
            assertEquals(if (switchLibrary) 1 else 2, store.state.value.gallery?.page)
        }
    }
    @Test fun navigationContextIsClearedAtEveryPrivateStateBoundary() = runTest {
        for (boundary in listOf("background", "logout", "libraries", "expiry")) {
            val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
            store.openAsset(asset); runCurrent(); assertNotNull(store.state.value.photoNavigation)
            when (boundary) {
                "background" -> store.background()
                "logout" -> store.logout()
                "libraries" -> store.libraries()
                "expiry" -> advanceTimeBy(86400_000)
            }
            runCurrent(); store.adjacentPhoto(1); runCurrent()
            assertNull(store.state.value.photoNavigation); assertNull(store.state.value.detail)
            assertEquals(0, store.cachedBytes); assertEquals(listOf("1"), api.detailReads)
        }
    }
    @Test fun adjacentPhotoDenialClearsSequenceAndRechecksSessionOnce() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent()
        api.detailError = ApiFailure(FailureKind.HTTP, 401)
        store.adjacentPhoto(1); runCurrent(); store.adjacentPhoto(-1); runCurrent()
        assertNull(store.state.value.photoNavigation); assertNull(store.state.value.detail)
        assertEquals(0, store.cachedBytes); assertEquals(2, api.sessionReads)
        assertEquals(listOf("1", "2"), api.detailReads); assertFalse(store.canRetry())
    }
    @Test fun slideshowRequiresExplicitOriginalAndStopsAtPageEndWithoutWrapping() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2"), asset.copy(id = "3")); originalsAllowed = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.loadPage(2); runCurrent(); store.openAsset(asset); runCurrent()
        store.togglePhotoSlideshow(); assertFalse(store.state.value.photoSlideshow); assertEquals(0, api.originalReads)
        store.openOriginalPhoto(); runCurrent(); store.togglePhotoSlideshow()
        assertTrue(store.state.value.photoSlideshow)
        store.advancePhotoSlideshow(); assertNull(store.state.value.originalPhoto); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id); assertTrue(store.state.value.photoSlideshow)
        store.advancePhotoSlideshow(); advanceTimeBy(350); runCurrent()
        assertEquals("3", store.state.value.detail?.asset?.id); assertFalse(store.state.value.photoSlideshow)
        store.advancePhotoSlideshow(); runCurrent(); assertEquals(3, api.originalReads)
        assertEquals(listOf("1", "2", "3"), api.detailReads)
        store.closeOriginalPhoto(); store.backToPhotos(); runCurrent(); assertEquals(2, store.state.value.gallery?.page)
    }
    @Test fun slideshowRechecksEachOriginalGrantAndDoesNotAutoplayVideo() = runTest {
        for (video in listOf(false, true)) {
            val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2", kind = if (video) "video" else "image")); originalsAllowed = true }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent(); store.openAsset(asset); runCurrent()
            store.openOriginalPhoto(); runCurrent(); store.togglePhotoSlideshow()
            if (!video) api.originalsAllowed = false
            store.advancePhotoSlideshow(); runCurrent()
            assertEquals("2", store.state.value.detail?.asset?.id)
            assertFalse(store.state.value.viewingOriginal); assertFalse(store.state.value.photoSlideshow)
            assertNull(store.state.value.video); assertNull(store.state.value.originalPhoto); assertEquals(1, api.originalReads)
        }
    }
    @Test fun pausedSlideshowCannotRestartWhenPendingMetadataCompletes() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2"), asset.copy(id = "3")); originalsAllowed = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent(); store.openAsset(asset); runCurrent()
        store.openOriginalPhoto(); runCurrent(); store.togglePhotoSlideshow()
        api.detailGate = CompletableDeferred()
        store.advancePhotoSlideshow(); runCurrent(); store.stopPhotoSlideshow()
        api.detailGate!!.complete(Unit); runCurrent()
        assertNotNull(store.state.value.originalPhoto); assertFalse(store.state.value.photoSlideshow)
        store.advancePhotoSlideshow(); runCurrent(); assertEquals(2, api.originalReads)
    }
    @Test fun lateSlideshowOriginalCannotSurviveBackgroundLogoutOrLibrarySwitch() = runTest {
        for (boundary in listOf("background", "logout", "switch", "expiry")) {
            val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2"), asset.copy(id = "3")); originalsAllowed = true }
            val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent(); store.openAsset(asset); runCurrent()
            store.openOriginalPhoto(); runCurrent(); store.togglePhotoSlideshow()
            api.originalGate = CompletableDeferred()
            store.advancePhotoSlideshow(); runCurrent()
            when (boundary) { "background" -> store.background(); "logout" -> store.logout(); "expiry" -> { advanceTimeBy(86400000); runCurrent() }; else -> store.selectLibrary("second") }
            api.originalGate!!.complete(Unit); runCurrent()
            assertFalse(store.state.value.photoSlideshow); assertFalse(store.state.value.viewingOriginal)
            assertNull(store.state.value.originalPhoto); assertNull(store.state.value.photoNavigation)
        }
    }
    @Test fun slideshowFailureClearsPrivateStateAndManualRetryReopensPhotoWithoutResumingSlideshow() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")); originalsAllowed = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent(); store.openAsset(asset); runCurrent()
        store.openOriginalPhoto(); runCurrent(); store.togglePhotoSlideshow()
        api.originalError = ApiFailure(FailureKind.OFFLINE)
        store.advancePhotoSlideshow(); advanceTimeBy(350); runCurrent()
        assertNull(store.state.value.detail); assertNull(store.state.value.originalPhoto)
        assertFalse(store.state.value.photoSlideshow); assertEquals(0, store.cachedBytes)
        api.originalError = null; store.retry(); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id); assertTrue(store.state.value.viewingOriginal)
        assertFalse(store.state.value.photoSlideshow)
        assertEquals(4, api.originalReads)
    }
    @Test fun manualViewerNavigationStopsSlideshowAndFetchesFreshOriginal() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2"), asset.copy(id = "3")); originalsAllowed = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent(); store.openAsset(asset); runCurrent()
        store.openOriginalPhoto(); runCurrent(); store.togglePhotoSlideshow()
        store.adjacentOriginalPhoto(1); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id); assertFalse(store.state.value.photoSlideshow)
        store.adjacentOriginalPhoto(-1); runCurrent(); assertEquals("1", store.state.value.detail?.asset?.id)
        assertEquals(3, api.originalReads)
    }
    @Test fun offlineAdjacentRetryFetchesAgainAndRetainsReturnPage() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.loadPage(2); runCurrent(); store.openAsset(asset); runCurrent()
        api.detailError = ApiFailure(FailureKind.OFFLINE)
        store.adjacentPhoto(1); advanceTimeBy(350); runCurrent()
        assertNull(store.state.value.photoNavigation); assertNull(store.state.value.detail)
        assertTrue(store.canRetry()); assertEquals(0, store.cachedBytes)
        api.detailError = null; store.retry(); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id)
        assertEquals(listOf("1", "2", "2", "2"), api.detailReads)
        store.backToPhotos(); runCurrent(); assertEquals(2, store.state.value.gallery?.page)
    }

    @Test fun transientAdjacentReadRecoversOnceWithoutShowingUnavailableOrEnablingManualRetry() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent()
        api.detailError = ApiFailure(FailureKind.OFFLINE); api.detailFailures = 1
        store.adjacentPhoto(1); advanceTimeBy(350); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id)
        assertNull(store.state.value.problem)
        assertFalse(store.canRetry())
        assertEquals(listOf("1", "2", "2"), api.detailReads)
    }

    @Test fun transientAdjacentReadIsBoundedToOneAutomaticRetry() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent()
        api.detailError = ApiFailure(FailureKind.OFFLINE); api.detailFailures = 2
        store.adjacentPhoto(1); advanceTimeBy(350); runCurrent()
        assertEquals(listOf("1", "2", "2"), api.detailReads)
        assertNull(store.state.value.detail); assertEquals(Message.UNAVAILABLE, store.state.value.problem?.message)
        assertTrue(store.canRetry())
    }

    @Test fun transientAdjacentReadIsCancelledByBackgroundDuringRecoveryWait() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent()
        api.detailError = ApiFailure(FailureKind.OFFLINE); api.detailFailures = 1
        store.adjacentPhoto(1); runCurrent(); store.background(); advanceTimeBy(350); runCurrent()
        assertEquals(listOf("1", "2"), api.detailReads)
        assertNull(store.state.value.detail); assertTrue(store.state.value.covered)
    }

    @Test fun firstGalleryOfflineReadRecoversOnceWithoutShowingUnavailable() = runTest {
        val api = FakeApi().apply {
            galleryError = ApiFailure(FailureKind.OFFLINE)
            galleryFailures = 1
        }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        // selectLibrary performs the first gallery load; recovery completes after one delay.
        advanceTimeBy(350); runCurrent()
        assertEquals(2, api.galleryReads)
        assertNotNull(store.state.value.gallery)
        assertNull(store.state.value.problem)
        assertFalse(store.canRetry())
    }

    @Test fun firstGalleryOfflineReadIsBoundedToOneRetry() = runTest {
        val api = FakeApi().apply {
            galleryError = ApiFailure(FailureKind.OFFLINE)
            galleryFailures = 2
        }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        advanceTimeBy(350); runCurrent()
        assertEquals(2, api.galleryReads)
        assertNull(store.state.value.gallery)
        assertEquals(Message.UNAVAILABLE, store.state.value.problem?.message)
        assertTrue(store.canRetry())
    }

    @Test fun firstGalleryOfflineRecoveryIsCancelledByBackground() = runTest {
        val api = FakeApi().apply {
            galleryError = ApiFailure(FailureKind.OFFLINE)
            galleryFailures = 1
        }
        val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent()
        store.background(); advanceTimeBy(350); runCurrent()
        assertEquals(1, api.galleryReads)
        assertNull(store.state.value.gallery)
        assertTrue(store.state.value.covered)
    }

    @Test fun firstGalleryOfflineRecoveryIsCancelledByLogout() = runTest {
        val api = FakeApi().apply {
            galleryError = ApiFailure(FailureKind.OFFLINE)
            galleryFailures = 1
        }
        val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent()
        store.logout(); advanceTimeBy(350); runCurrent()
        assertEquals(1, api.galleryReads)
        assertNull(store.state.value.gallery)
        assertFalse(store.hasSession)
    }

    @Test fun firstGalleryOfflineRecoveryIsCancelledWhenSessionExpires() = runTest {
        val api = FakeApi().apply {
            galleryError = ApiFailure(FailureKind.OFFLINE)
            galleryFailures = 1
        }
        var clock = 0L
        val store = ConnectedStore(api, backgroundScope) { clock }
        store.authenticate("+12025550123", "synthetic-password-only"); runCurrent()
        store.selectLibrary("family"); runCurrent()
        clock = 86_400_000L
        advanceTimeBy(350); runCurrent()
        assertEquals(1, api.galleryReads)
        assertFalse(store.hasSession)
        assertEquals(Message.SESSION_ENDED, store.state.value.problem?.message)
    }

    @Test fun firstGalleryOfflineRecoveryIsCancelledWhenSelectionStartsAnotherGeneration() = runTest {
        val api = FakeApi().apply {
            galleryError = ApiFailure(FailureKind.OFFLINE)
            galleryFailures = 1
        }
        val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent()
        store.loadPage(2); runCurrent(); advanceTimeBy(350); runCurrent()
        assertEquals(2, api.galleryReads)
        assertEquals(2, store.state.value.gallery?.page)
    }

    @Test fun galleryRecoveryDoesNotRetryTlsAuthRateLimitServerOrInvalidFailures() = runTest {
        val failures = listOf(
            ApiFailure(FailureKind.TLS),
            ApiFailure(FailureKind.HTTP, 401),
            ApiFailure(FailureKind.HTTP, 429),
            ApiFailure(FailureKind.HTTP, 503),
            ApiFailure(FailureKind.INVALID_RESPONSE),
        )
        for (failure in failures) {
            val api = FakeApi().apply { galleryError = failure }
            val store = store(api); signIn(store)
            store.selectLibrary("family"); runCurrent(); advanceTimeBy(350); runCurrent()
            assertEquals(failure.toString(), 1, api.galleryReads)
            assertNull(store.state.value.gallery)
            store.logout(); runCurrent()
        }
    }

    @Test fun malformedGalleryResponseIsNotRetried() = runTest {
        val api = FakeApi().apply {
            galleryResult = Gallery("family", 2, 50, 1, false, assets)
        }
        val store = store(api); signIn(store)
        store.selectLibrary("family"); runCurrent(); advanceTimeBy(350); runCurrent()
        assertEquals(1, api.galleryReads)
        assertEquals(Message.INVALID_RESPONSE, store.state.value.problem?.message)
        assertNull(store.state.value.gallery)
    }

    @Test fun recoverableThumbnailFailureKeepsGalleryAndContinuesLaterThumbnails() = runTest {
        val first = asset
        val second = asset.copy(id = "2", thumbnail_url = "/assets/2/thumbnail?library=family")
        val api = FakeApi().apply {
            assets = listOf(first, second)
            thumbnailFailureAsset = first.id
            thumbnailFailure = ApiFailure(FailureKind.OFFLINE)
        }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        assertEquals(listOf("1", "2"), store.state.value.gallery?.items?.map { it.id })
        assertNull(store.state.value.previews[first.id])
        assertArrayEquals(byteArrayOf(1, 2, 3), store.state.value.previews[second.id])
        assertNull(store.state.value.problem)
        assertEquals(2, api.imageReads)
    }

    @Test fun thumbnailAuthorizationFailureClearsGalleryAndDoesNotContinue() = runTest {
        val second = asset.copy(id = "2", thumbnail_url = "/assets/2/thumbnail?library=family")
        val api = FakeApi().apply {
            assets = listOf(asset, second)
            thumbnailFailureAsset = asset.id
            thumbnailFailure = ApiFailure(FailureKind.HTTP, 403)
        }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        assertNull(store.state.value.gallery)
        assertEquals(Message.CLOSED, store.state.value.problem?.message)
        assertEquals(1, api.imageReads)
    }

    @Test fun retryAfterOnTransientServerReadPreventsEarlyAutomaticRetry() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent()
        api.detailError = ApiFailure(FailureKind.HTTP, 503, 1000); api.detailFailures = 1
        store.adjacentPhoto(1); runCurrent()
        assertEquals(listOf("1", "2"), api.detailReads); assertFalse(store.canRetry())
        advanceTimeBy(1000); assertTrue(store.canRetry())
        store.retry(); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id)
    }

    @Test fun automaticPhotoRecoveryPreservesOriginalViewerIntent() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")); originalsAllowed = true }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.openAsset(asset); runCurrent(); store.openOriginalPhoto(); runCurrent()
        api.detailError = ApiFailure(FailureKind.OFFLINE); api.detailFailures = 1
        store.adjacentOriginalPhoto(1); advanceTimeBy(350); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id)
        assertTrue(store.state.value.viewingOriginal); assertTrue(store.state.value.photoOriginalQuality)
        assertNotNull(store.state.value.originalPhoto)
    }
}
