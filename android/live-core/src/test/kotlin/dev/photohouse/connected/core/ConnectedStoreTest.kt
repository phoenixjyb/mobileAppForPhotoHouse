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
    private inner class FakeApi : PhotoHouseApi {
        var currentSession = Session("account-a", "+12025550123", listOf(membership("family"), membership("second"), membership("closed", false)))
        var sessionReads = 0; var galleryReads = 0; var imageReads = 0; var logouts = 0; var accepts = 0; var logins = 0
        var sessionError: Exception? = null
        var galleryError: Exception? = null
        var detailError: Exception? = null
        var detailGate: CompletableDeferred<Unit>? = null
        var originalsAllowed = false
        var originalGate: CompletableDeferred<Unit>? = null
        var originalError: Exception? = null
        var originalBytes = byteArrayOf(9, 8, 7)
        var originalReads = 0
        val detailReads = mutableListOf<String>()
        var logoutError: Exception? = null
        var loginGate: CompletableDeferred<Unit>? = null
        var galleryGate: CompletableDeferred<Unit>? = null
        var sessionGate: CompletableDeferred<Unit>? = null
        var logoutGate: CompletableDeferred<Unit>? = null
        var images: ByteArray? = byteArrayOf(1, 2, 3)
        var assets = listOf(asset)
        var lastRegistration: String? = null
        override suspend fun login(phone: String, password: String): SessionToken {
            logins++; loginGate?.let { withContext(NonCancellable) { it.await() } }
            return SessionToken(86400, "T".repeat(43), "Bearer")
        }
        override suspend fun register(phone: String, password: String, code: String): SessionToken { lastRegistration = code; return login(phone, password) }
        override suspend fun session(token: Bearer): Session {
            sessionReads++; val response = currentSession
            sessionGate?.let { withContext(NonCancellable) { it.await() } }
            sessionError?.let { throw it }; return response
        }
        override suspend fun acceptInvitation(token: Bearer, code: String) { accepts++ }
        override suspend fun logout(token: Bearer) { logouts++; logoutGate?.let { withContext(NonCancellable) { it.await() } }; logoutError?.let { throw it } }
        override suspend fun gallery(token: Bearer, library: String, page: Int): Gallery {
            galleryReads++; val response = Gallery(library, page, 50, 100, false, assets)
            galleryGate?.let { withContext(NonCancellable) { it.await() } }
            galleryError?.let { throw it }; return response
        }
        override suspend fun detail(token: Bearer, library: String, assetId: String): Detail {
            detailReads += assetId
            val response = Detail(library, originalsAllowed, assets.first { it.id == assetId })
            detailGate?.let { withContext(NonCancellable) { it.await() } }
            detailError?.let { throw it }; return response
        }
        override suspend fun captions(token: Bearer, library: String, assetId: String) = Captions(library, assetId, false, listOf(Caption("c1", "<b>原文 literal</b>", false, false, null, null)))
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? { imageReads++; return images }
        override suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray {
            originalReads++; val bytes = originalBytes
            originalGate?.let { withContext(NonCancellable) { it.await() } }
            originalError?.let { throw it }; return bytes
        }
    }
    private fun TestScope.store(api: FakeApi) = ConnectedStore(api, backgroundScope) { testScheduler.currentTime }
    private fun TestScope.signIn(store: ConnectedStore) { store.authenticate("+12025550123", "synthetic-password-only"); runCurrent(); assertNotNull(store.state.value.session) }

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
    @Test fun offlineAdjacentRetryFetchesAgainAndRetainsReturnPage() = runTest {
        val api = FakeApi().apply { assets = listOf(asset, asset.copy(id = "2")) }
        val store = store(api); signIn(store); store.selectLibrary("family"); runCurrent()
        store.loadPage(2); runCurrent(); store.openAsset(asset); runCurrent()
        api.detailError = ApiFailure(FailureKind.OFFLINE)
        store.adjacentPhoto(1); runCurrent()
        assertNull(store.state.value.photoNavigation); assertNull(store.state.value.detail)
        assertTrue(store.canRetry()); assertEquals(0, store.cachedBytes)
        api.detailError = null; store.retry(); runCurrent()
        assertEquals("2", store.state.value.detail?.asset?.id)
        assertEquals(listOf("1", "2", "2"), api.detailReads)
        store.backToPhotos(); runCurrent(); assertEquals(2, store.state.value.gallery?.page)
    }
}
