package dev.photohouse.connected.core

import dev.photohouse.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProtectedStoryStoreTest {
    private val asset = Asset("1", "image", 256, 256, null, null, "/assets/1/thumbnail?library=family")
    private inner class Api : PhotoHouseApi {
        override var protectedNativeV2Enabled = true
        var available = true
        var sessionError: Exception? = null
        var storyError: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        val reads = mutableListOf<Int>()
        var wrongScope = false
        override suspend fun login(phone: String, password: String) = SessionToken(86400, "T".repeat(43), "Bearer")
        override suspend fun register(phone: String, password: String, code: String) = login(phone,password)
        override suspend fun session(token: Bearer): Session {
            sessionError?.let { throw it }
            return Session("synthetic-account", "+12025550123", listOf(Membership("family", "approved", "viewer", 1, null, 0, available)))
        }
        override suspend fun acceptInvitation(token: Bearer, code: String) {}
        override suspend fun logout(token: Bearer) {}
        override suspend fun gallery(token: Bearer, library: String, page: Int) = Gallery(library,page,50,1,false,listOf(asset))
        override suspend fun detail(token: Bearer, library: String, assetId: String) = Detail(library,false,asset.copy(id=assetId))
        override suspend fun captions(token: Bearer, library: String, assetId: String) = Captions(library,assetId,false,emptyList())
        override suspend fun thumbnail(token: Bearer, library: String, asset: Asset): ByteArray? = null
        override suspend fun videoRange(token: Bearer, library: String, assetId: String, start: Long, length: Int): VideoChunk = error("unused")
        override suspend fun originalPhoto(token: Bearer, library: String, assetId: String): ByteArray = error("unused")
        override suspend fun stories(token: Bearer, library: String, assetId: String, page: Int): ProtectedStoryPage {
            reads += page
            gate?.let { withContext(NonCancellable) { it.await() } }
            storyError?.let { throw it }
            return ProtectedStoryPage(if(wrongScope) "foreign" else library, assetId, page, false, page==1,
                if(page==1) listOf(ProtectedStory("10000000-0000-0000-0000-000000000001",assetId,"Title","家人的原文 <b>literal</b>","mixed","A family member","synthetic-account",1,100,100,false,false)) else emptyList())
        }
    }
    private fun TestScope.open(api: Api): ConnectedStore {
        val store=ConnectedStore(api,backgroundScope) { testScheduler.currentTime }
        store.authenticate("+12025550123", "12345678"); runCurrent()
        store.selectLibrary("family");runCurrent();store.openAsset(asset);runCurrent()
        assertNotNull(store.state.value.detail)
        return store
    }
    @Test fun readsOnlyWhenRequestedAndReplacesPagesWithoutPersistingHistory()=runTest {
        val api=Api();val store=open(api);assertTrue(api.reads.isEmpty())
        store.loadStories();runCurrent();assertEquals("家人的原文 <b>literal</b>",store.state.value.stories!!.result!!.items.single().text)
        store.loadStories(2);runCurrent();assertTrue(store.state.value.stories!!.result!!.items.isEmpty())
        store.loadStories(1);runCurrent();assertEquals(listOf(1,2,1),api.reads)
        store.backToPhotos();runCurrent();assertNull(store.state.value.stories)
    }
    @Test fun disabledProfileNeverRequestsStories()=runTest {
        val api=Api().apply { protectedNativeV2Enabled=false }
        val store=ConnectedStore(api,backgroundScope)
        store.authenticate("+12025550123","long-enough-password");runCurrent()
        store.selectLibrary("family");runCurrent();store.openAsset(asset);runCurrent()
        store.loadStories();runCurrent();assertTrue(api.reads.isEmpty());assertNull(store.state.value.stories)
    }
    @Test fun backgroundNavigationAndLogoutRejectUncancellableLateText()=runTest {
        for(action in listOf("background","library","logout","photo")) {
            val api=Api();val store=open(api);api.gate=CompletableDeferred()
            store.loadStories();runCurrent();assertTrue(store.state.value.stories!!.busy)
            when(action) {"background"->store.background();"library"->store.libraries();"logout"->store.logout();else->store.openAsset(asset.copy(id="2"))}
            runCurrent();api.gate!!.complete(Unit);runCurrent();assertNull(store.state.value.stories)
        }
    }
    @Test fun revocationClearsTextMediaAndUnavailableMembershipCannotRead()=runTest {
        val api=Api();val store=open(api);store.loadStories();runCurrent()
        api.storyError=ApiFailure(FailureKind.HTTP,401);api.available=false
        store.loadStories(2);runCurrent()
        assertNull(store.state.value.stories);assertNull(store.state.value.detail);assertTrue(store.state.value.previews.isEmpty())
        store.loadStories();runCurrent();assertEquals(listOf(1,2),api.reads)
    }
    @Test fun expiredSessionAnd403AlsoClearStoryData()=runTest {
        for(status in listOf(401,403)) {
            val api=Api();val store=open(api);store.loadStories();runCurrent()
            api.storyError=ApiFailure(FailureKind.HTTP,status)
            if(status==401)api.sessionError=ApiFailure(FailureKind.HTTP,401)
            store.loadStories(2);runCurrent();assertNull(store.state.value.stories);assertNull(store.state.value.detail)
            if(status==401)assertNull(store.state.value.session)
        }
    }
    @Test fun recoverableStoryFailureKeepsPhotoAndRequiresExplicitRetry()=runTest {
        val api=Api();val store=open(api);api.storyError=ApiFailure(FailureKind.OFFLINE)
        store.loadStories();runCurrent();assertNotNull(store.state.value.detail)
        assertEquals(Message.UNAVAILABLE,store.state.value.stories!!.problem!!.message);assertEquals(listOf(1),api.reads)
        api.storyError=null;store.loadStories();runCurrent();assertNotNull(store.state.value.stories!!.result)
    }
    @Test fun cooldownSuppressesRepeatRequestsAndNoWrongScopeTextAppears()=runTest {
        val api=Api();val store=open(api);api.storyError=ApiFailure(FailureKind.HTTP,429,1000)
        store.loadStories();runCurrent();store.loadStories();runCurrent();assertEquals(1,api.reads.size)
        advanceTimeBy(1001);api.storyError=null;api.wrongScope=true
        store.loadStories();runCurrent();assertEquals(2,api.reads.size)
        assertNull(store.state.value.stories!!.result);assertEquals(Message.INVALID_RESPONSE,store.state.value.stories!!.problem!!.message)
    }
    @Test fun longLivedSessionExpiryClearsAlreadyReadStories()=runTest {
        val api=Api();val store=open(api);store.loadStories();runCurrent()
        advanceTimeBy(86400001);runCurrent();assertNull(store.state.value.stories);assertNull(store.state.value.session)
    }
}
