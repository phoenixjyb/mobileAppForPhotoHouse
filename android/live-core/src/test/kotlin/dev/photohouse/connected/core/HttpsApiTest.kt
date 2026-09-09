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
                assertEquals(if (status == 429) 7000L else 0L, error.retryAfterMillis)
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
