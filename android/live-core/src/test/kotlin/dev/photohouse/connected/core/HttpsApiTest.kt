package dev.photohouse.connected.core

import dev.photohouse.protocol.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import okhttp3.tls.*
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class HttpsApiTest {
    private val password = "synthetic-password-only"
    private val token = Bearer.from(SessionToken(86400, "T".repeat(43), "Bearer"))
    private val asset = Asset("1", "image", null, null, null, null, "/assets/1/thumbnail?library=family")
    private val session = """{"account_id":"synthetic-account","phone_login":"+12025550123","memberships":[]}"""
    private class TlsFixture(hostname: String = "localhost") : AutoCloseable {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName(hostname).build()
        val server = MockWebServer().apply {
            useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory(), false)
            start(InetAddress.getByName("127.0.0.1"), 0)
        }
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val client = OkHttpClient.Builder().sslSocketFactory(trust.sslSocketFactory(), trust.trustManager).build()
        val origin = TrustedOrigin.parse("https://localhost:${server.port}")
        val api = HttpsPhotoHouseApi(origin, client)
        override fun close() { server.shutdown(); client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
    }
    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
    private suspend fun failure(block: suspend () -> Unit): ApiFailure {
        try { block() } catch (e: ApiFailure) { return e }
        throw AssertionError("Expected a classified failure")
    }
    private fun range(body: String = "abcd", header: String = "bytes 0-3/10") = MockResponse().setResponseCode(206)
        .setHeader("Content-Type", "video/mp4").setHeader("Content-Range", header).setBody(body)
    @Test fun preparedBrowseSendsOnlyTheOptInScopedFilter() = runBlocking {
        TlsFixture().use { f ->
            val disabled = HttpsPhotoHouseApi(f.origin, f.client, protectedNativeV2Enabled = true, mediaFilterEnabled = true)
            assertTrue(runCatching { disabled.gallery(token, "family", 1, GalleryMedia.PREPARED_VIDEOS) }.isFailure)
            assertEquals(0, f.server.requestCount)
            val api = HttpsPhotoHouseApi(f.origin, f.client, protectedNativeV2Enabled = true,
                mediaFilterEnabled = true, preparedVideoEnabled = true, preparedBrowseEnabled = true)
            f.server.enqueue(json("""{"library_id":"family","page":1,"page_size":50,"total":0,"originals_allowed":false,"items":[]}"""))
            assertEquals(0, api.gallery(token, "family", 1, GalleryMedia.PREPARED_VIDEOS).total)
            val request = f.server.takeRequest()
            assertEquals("prepared_video", request.requestUrl!!.queryParameter("media"))
            assertEquals("family", request.requestUrl!!.queryParameter("library"))
            assertEquals("Bearer " + "T".repeat(43), request.getHeader("Authorization"))
            assertEquals(1, f.server.requestCount)
        }
    }

    @Test fun absentRetryAfterAllowsTransientReadRecoveryButRetainsRateLimitDefault() = runBlocking {
        TlsFixture().use { f ->
            for (status in listOf(502, 503, 504, 429)) {
                f.server.enqueue(MockResponse().setResponseCode(status))
                val error = failure { f.api.session(token) }
                assertEquals(if (status == 429) 5000L else 0L, error.retryAfterMillis)
            }
            f.server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "invalid"))
            assertEquals(5000L, failure { f.api.session(token) }.retryAfterMillis)
            assertEquals(5, f.server.requestCount) // the transport itself never retries requests
        }
    }

    @Test fun optimizedPhotoUsesProtectedDisplayRouteAndNeverOriginalFallback() = runBlocking {
        TlsFixture().use { f ->
            val api = HttpsPhotoHouseApi(f.origin, f.client, photoDeliveryEnabled = true)
            f.server.enqueue(MockResponse().setHeader("Content-Type", "image/jpeg").setBody("synthetic"))
            assertEquals("synthetic", api.displayPhoto(token, "family", "1").toString(Charsets.UTF_8))
            val request = f.server.takeRequest()
            assertEquals("/assets/1/display?library=family", request.path)
            assertEquals("Bearer " + "T".repeat(43), request.getHeader("Authorization"))
            f.server.enqueue(MockResponse().setResponseCode(503))
            assertEquals(503, failure { api.displayPhoto(token, "family", "1") }.status)
            assertEquals(2, f.server.requestCount)
        }
    }

    @Test fun videoRangesAuthenticateEverySeekWithStrictSameOriginHeaders() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(range()); f.server.enqueue(range("ij", "bytes 8-9/10"))
            assertEquals(10L, f.api.videoRange(token, "family", "1", 0, 4).total)
            assertEquals("ij", f.api.videoRange(token, "family", "1", 8, 4).bytes.toString(Charsets.UTF_8))
            for (expected in listOf("bytes=0-3", "bytes=8-11")) {
                val request = f.server.takeRequest()
                assertEquals("/assets/1/media?library=family", request.path)
                assertEquals(expected, request.getHeader("Range"))
                assertEquals("identity", request.getHeader("Accept-Encoding"))
                assertEquals("no-store", request.getHeader("Cache-Control"))
                assertEquals(1, request.headers.values("Authorization").size)
                assertNull(request.getHeader("Cookie")); assertNull(request.getHeader("If-Range"))
            }
            for (start in listOf(-1L, HttpsPhotoHouseApi.VIDEO_FILE_LIMIT, Long.MAX_VALUE))
                assertTrue(runCatching { f.api.videoRange(token, "family", "1", start, 4) }.isFailure)
            assertTrue(runCatching { f.api.videoRange(token, "family", "1", 0, 262145) }.isFailure)
            assertEquals(2, f.server.requestCount)
        }
    }
    @Test fun videoRejectsIgnoredMalformedCompressedOversizedAndTruncatedRanges() = runBlocking {
        TlsFixture().use { f ->
            val invalid = listOf(range().setResponseCode(200), range().removeHeader("Content-Range"),
                range(header = "bytes 1-4/10"), range(header = "bytes 0-4/10"), range(header = "bytes 0-3/*"),
                range(header = "bytes 0-3/0"), range(header = "bytes 0-3/99999999999999999999"),
                range().setHeader("Content-Encoding", "gzip"), range().setHeader("Content-Type", "text/html"),
                range("abc"), range("abcde").setChunkedBody("abcde", 1))
            for (response in invalid) {
                f.server.enqueue(response)
                assertTrue(failure { f.api.videoRange(token, "family", "1", 0, 4) }.kind in setOf(FailureKind.INVALID_RESPONSE, FailureKind.TOO_LARGE))
            }
            f.server.enqueue(range(header = "bytes 0-3/${HttpsPhotoHouseApi.VIDEO_FILE_LIMIT + 1}"))
            assertEquals(FailureKind.TOO_LARGE, failure { f.api.videoRange(token, "family", "1", 0, 4) }.kind)
            f.server.enqueue(range().setHeader("Content-Length", 5))
            assertEquals(FailureKind.TOO_LARGE, failure { f.api.videoRange(token, "family", "1", 0, 4) }.kind)
        }
    }
    @Test fun multiGigabyteSeeksUseLongOffsetsWithoutDownloadingTheFile() = runBlocking {
        TlsFixture().use { f ->
            val total = 8193114694L
            for (start in listOf(0L, 4294967296L, total - 4)) {
                f.server.enqueue(range("abcd", "bytes $start-${start + 3}/$total"))
                val result = f.api.videoRange(token, "family", "1", start, 4)
                assertEquals(total, result.total); assertEquals(start, result.start)
                assertEquals(4, result.bytes.size)
                val request = f.server.takeRequest()
                assertEquals("bytes=$start-${start + 3}", request.getHeader("Range"))
                assertEquals(1, request.headers.values("Authorization").size)
            }
            assertEquals(3, f.server.requestCount)
        }
    }
    @Test fun videoDenialRateLimitRedirectAndCancellationHaveNoAutomaticRetry() = runBlocking {
        TlsFixture().use { f ->
            for (status in listOf(401, 403, 404, 416, 429, 503, 302)) {
                f.server.enqueue(MockResponse().setResponseCode(status).setHeader("Retry-After", "7").setHeader("Location", "https://other.invalid/video"))
                val error = failure { f.api.videoRange(token, "family", "1", 0, 4) }
                assertEquals(status, error.status)
                if (status == 429) assertEquals(7000L, error.retryAfterMillis)
                assertNotNull(f.server.takeRequest(5, TimeUnit.SECONDS))
            }
            assertEquals(7, f.server.requestCount)
            f.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val job = launch(Dispatchers.IO) { f.api.videoRange(token, "family", "1", 0, 4) }
            assertNotNull(f.server.takeRequest(5, TimeUnit.SECONDS)); withTimeout(2000) { job.cancelAndJoin() }
        }
    }
    @Test fun originalUsesFixedAuthenticatedNoStoreRouteAndAcceptsExactBudget() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(MockResponse().setHeader("Content-Type", "image/jpeg").setBody("x".repeat(HttpsPhotoHouseApi.ORIGINAL_LIMIT)))
            assertEquals(HttpsPhotoHouseApi.ORIGINAL_LIMIT, f.api.originalPhoto(token, "family", "1").size)
            val request = f.server.takeRequest()
            assertEquals("/assets/1/media?library=family", request.path); assertEquals("GET", request.method)
            assertEquals(listOf("Bearer ${"T".repeat(43)}"), request.headers.values("Authorization"))
            assertEquals("no-store", request.getHeader("Cache-Control"))
            assertEquals("image/jpeg, image/png, image/webp", request.getHeader("Accept"))
            assertNull(request.getHeader("Range")); assertNull(request.getHeader("Cookie"))
            assertTrue(runCatching { f.api.originalPhoto(token, "family", "../collect") }.isFailure)
            assertEquals(1, f.server.requestCount)
        }
    }
    @Test fun originalRejectsKnownAndUnknownOversizeAndIncompleteOrNonImageBodies() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(MockResponse().setBody("x").setHeader("Content-Type", "image/png")
                .setHeader("Content-Length", HttpsPhotoHouseApi.ORIGINAL_LIMIT + 1))
            assertEquals(FailureKind.TOO_LARGE, failure { f.api.originalPhoto(token, "family", "1") }.kind)
            f.server.enqueue(MockResponse().setHeader("Content-Type", "image/png")
                .setChunkedBody("x".repeat(HttpsPhotoHouseApi.ORIGINAL_LIMIT + 1), 8192))
            assertEquals(FailureKind.TOO_LARGE, failure { f.api.originalPhoto(token, "family", "1") }.kind)
            for (response in listOf(
                MockResponse().setResponseCode(206).setHeader("Content-Type", "image/png").setBody("partial"),
                MockResponse().setHeader("Content-Type", "image/svg+xml").setBody("<svg/>"),
                MockResponse().setHeader("Content-Type", "video/mp4").setBody("not a photo"),
                MockResponse().setHeader("Content-Type", "image/png").setBody(""))) {
                f.server.enqueue(response)
                assertEquals(FailureKind.INVALID_RESPONSE, failure { f.api.originalPhoto(token, "family", "1") }.kind)
            }
        }
    }
    @Test fun originalDenialAndRedirectAreNeverFollowedOrRetried() = runBlocking {
        TlsFixture().use { f ->
            for (status in listOf(401, 403, 404, 302)) {
                f.server.enqueue(MockResponse().setResponseCode(status).setHeader("Location", "https://other.invalid/collect"))
                assertEquals(status, failure { f.api.originalPhoto(token, "family", "1") }.status)
            }
            assertEquals(4, f.server.requestCount)
            f.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val task = launch(Dispatchers.Default) { f.api.originalPhoto(token, "family", "1"); fail("Cancelled original returned") }
            repeat(5) { assertNotNull(f.server.takeRequest(5, TimeUnit.SECONDS)) }
            withTimeout(2000) { task.cancelAndJoin() }; assertTrue(task.isCancelled)
        }
    }
    @Test fun nativeLoginAndRegistrationSendExactFieldsWithoutAmbientCredentials() = runBlocking {
        TlsFixture().use { f ->
            repeat(2) { f.server.enqueue(json("""{"expires_in":86400,"access_token":"${"T".repeat(43)}","token_type":"Bearer"}""").setHeader("Set-Cookie", "ambient=never")) }
            f.api.login("+1 (202) 555-0123", password)
            f.api.register("+12025550123", password, "synthetic-invitation")
            val login = f.server.takeRequest()
            assertEquals("POST", login.method); assertEquals("/auth/login", login.path)
            val body = Wire.json.parseToJsonElement(login.body.readUtf8()).jsonObject
            assertEquals(setOf("phone", "password", "transport"), body.keys)
            assertEquals("native", body.getValue("transport").jsonPrimitive.content)
            assertEquals("+12025550123", body.getValue("phone").jsonPrimitive.content)
            val register = f.server.takeRequest()
            assertEquals("/auth/register", register.path)
            assertEquals(setOf("phone", "password", "code", "transport"), Wire.json.parseToJsonElement(register.body.readUtf8()).jsonObject.keys)
            for (request in listOf(login, register)) for (header in listOf("Authorization", "Cookie", "Origin")) assertNull(request.getHeader(header))
        }
    }
    @Test fun protectedSessionUsesOneHeaderAndDoesNotPersistCookie() = runBlocking {
        TlsFixture().use { f ->
            repeat(2) { f.server.enqueue(json(session).setHeader("Set-Cookie", "secret=discard")) }
            repeat(2) { assertEquals("synthetic-account", f.api.session(token).account_id) }
            repeat(2) {
                val request = f.server.takeRequest()
                assertEquals(listOf("Bearer ${"T".repeat(43)}"), request.headers.values("Authorization"))
                assertEquals("/auth/session", request.path); assertNull(request.getHeader("Cookie"))
                assertEquals("no-store", request.getHeader("Cache-Control"))
            }
        }
    }
    @Test fun frozenBrowsingAndMutationResponsesUseExactScopedRoutes() = runBlocking {
        val cases = Wire.json.parseToJsonElement(javaClass.getResource("/fixtures.json")!!.readText()).jsonObject.getValue("cases").jsonArray
        fun body(id: String) = cases.single { it.jsonObject.getValue("id").jsonPrimitive.content == id }.jsonObject.getValue("body").toString()
        TlsFixture().use { f ->
            for (id in listOf("gallery", "detail", "captions-bilingual", "accept-second-library", "logout")) f.server.enqueue(json(body(id)))
            assertEquals(2, f.api.gallery(token, "family-a", 1).items.size)
            assertEquals("101", f.api.detail(token, "family-a", "101").asset.id)
            assertEquals("Synthetic hillside. 合成山景。", f.api.captions(token, "family-a", "101").items.single().text)
            f.api.acceptInvitation(token, "synthetic-invitation"); f.api.logout(token)
            for (path in listOf("/assets?library=family-a&page=1&page_size=50", "/assets/detail/101?library=family-a", "/assets/101/captions?library=family-a")) {
                val request = f.server.takeRequest(); assertEquals(path, request.path); assertEquals("GET", request.method)
            }
            val accept = f.server.takeRequest(); assertEquals("/auth/invitations/accept", accept.path); assertEquals("POST", accept.method)
            assertEquals("""{"code":"synthetic-invitation"}""", accept.body.readUtf8())
            val logout = f.server.takeRequest(); assertEquals("/auth/logout", logout.path); assertEquals("POST", logout.method); assertEquals("{}", logout.body.readUtf8())
        }
    }
    @Test fun redirectsNeverFollowEvenOnSameOrigin() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/collect"))
            assertEquals(302, failure { f.api.session(token) }.status)
            assertEquals(1, f.server.requestCount)
        }
    }
    @Test fun platformTrustRejectsSelfSignedServerAndHostnameValidationRemainsEnabled() = runBlocking {
        TlsFixture().use { f ->
            assertEquals(FailureKind.TLS, failure { HttpsPhotoHouseApi(f.origin).session(token) }.kind)
            assertEquals(0, f.server.requestCount)
        }
        TlsFixture("wrong.invalid").use { f ->
            assertEquals(FailureKind.TLS, failure { f.api.session(token) }.kind)
            assertEquals(0, f.server.requestCount)
        }
    }
    @Test fun statusesDoNotDependOnErrorBodyAndNeverAutoRetry() = runBlocking {
        TlsFixture().use { f ->
            for (status in listOf(401, 403, 422, 429, 503)) {
                f.server.enqueue(MockResponse().setResponseCode(status).setHeader("Retry-After", "7").setBody("private diagnostic deliberately not parsed"))
                val error = failure { f.api.session(token) }
                assertEquals(status, error.status)
                assertEquals(if (status == 429 || status in 502..504) 7000L else 0L, error.retryAfterMillis)
                assertFalse(error.toString().contains("private diagnostic"))
            }
            assertEquals(5, f.server.requestCount)
        }
    }
    @Test fun boundedStreamingAndStrictJsonRejectMalformedResponses() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(json(" ".repeat(HttpsPhotoHouseApi.JSON_LIMIT + 1)).setChunkedBody(" ".repeat(HttpsPhotoHouseApi.JSON_LIMIT + 1), 8192))
            assertEquals(FailureKind.TOO_LARGE, failure { f.api.session(token) }.kind)
            f.server.enqueue(json(session.dropLast(1) + ",\"unexpected\":true}"))
            assertEquals(FailureKind.INVALID_RESPONSE, failure { f.api.session(token) }.kind)
            f.server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody(session))
            assertEquals(FailureKind.INVALID_RESPONSE, failure { f.api.session(token) }.kind)
        }
    }
    @Test fun missingThumbnailIsPlaceholderWithNoOriginalFallback() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(MockResponse().setResponseCode(404))
            assertNull(f.api.thumbnail(token, "family", asset))
            assertEquals("/assets/1/thumbnail?library=family", f.server.takeRequest().path)
            assertEquals(1, f.server.requestCount)
        }
    }
    @Test fun arbitraryThumbnailUrlsNeverSendBearer() = runBlocking {
        TlsFixture().use { f ->
            for (url in listOf("https://other.invalid/collect", "//other.invalid/collect", "/assets/1/media?library=family", "/assets/1/thumbnail?library=other", "/assets/2/thumbnail?library=family")) {
                try { f.api.thumbnail(token, "family", asset.copy(thumbnail_url = url)); fail("Accepted $url") } catch (_: IllegalArgumentException) { }
            }
            assertEquals(0, f.server.requestCount)
        }
    }
    @Test fun thumbnailMimeAndStreamingSizeAreBounded() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(MockResponse().setHeader("Content-Type", "image/svg+xml").setBody("<svg/>"))
            assertEquals(FailureKind.INVALID_RESPONSE, failure { f.api.thumbnail(token, "family", asset) }.kind)
            f.server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setChunkedBody("x".repeat(HttpsPhotoHouseApi.IMAGE_LIMIT + 1), 8192))
            assertEquals(FailureKind.TOO_LARGE, failure { f.api.thumbnail(token, "family", asset) }.kind)
        }
    }
    @Test fun cancelStopsOutstandingTransport() = runBlocking {
        TlsFixture().use { f ->
            f.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val task = launch(Dispatchers.Default) { f.api.session(token); fail("Cancelled request returned") }
            assertNotNull(f.server.takeRequest(5, TimeUnit.SECONDS))
            withTimeout(2000) { task.cancelAndJoin() }
            assertTrue(task.isCancelled); assertEquals(1, f.server.requestCount)
        }
    }
    @Test fun requestLimitsRejectOversizedInvitationBeforeNetwork() = runBlocking {
        TlsFixture().use { f ->
            assertEquals(FailureKind.INVALID_INPUT, failure { f.api.register("+12025550123", password, "x".repeat(3000)) }.kind)
            assertEquals(0, f.server.requestCount)
        }
    }
    @Test fun originAdmissionAndOpaqueTokenValidation() {
        for (origin in listOf("http://localhost", "https://u:p@example.invalid", "https://example.invalid/path", "https://example.invalid?x=1", "https://example.invalid#x", " https://example.invalid", "https://example.invalid\\path")) {
            assertTrue(runCatching { TrustedOrigin.parse(origin) }.isFailure)
        }
        assertEquals("+12025550123", Admission.phone("+1 (202) 555-0123"))
        assertTrue(runCatching { Admission.phone("12025550123") }.isFailure)
        Admission.password("😀".repeat(15))
        assertTrue(runCatching { Admission.password("😀".repeat(14)) }.isFailure)
        assertTrue(runCatching { Bearer.from(SessionToken(86400, "F".repeat(43), "Bearer")) }.isFailure)
        assertFalse(token.toString().contains("T".repeat(43)))
        assertEquals(9000L, retryAfterMillis("9"))
        assertEquals(5000L, retryAfterMillis("invalid"))
        assertEquals(1000L, retryAfterMillis("Thu, 01 Jan 1970 00:00:01 GMT", 0))
    }
}
